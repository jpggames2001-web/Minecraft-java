package dev.skyblock.network;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
/** Real pre-1.4 server-list ping. This class does not pretend to implement multiplayer gameplay. */
public final class LegacyServerProbe implements AutoCloseable {
    public record Result(boolean reachable,String message,int online,int maximum) {}
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"legacy-server-probe"); t.setDaemon(true); return t;});
    private volatile Socket socket;
    private volatile boolean closed;
    private final Future<Result> future;
    public LegacyServerProbe(ServerAddress address) { future=executor.submit(()->ping(address)); }
    public Result result() {
        if(!future.isDone()) return null;
        try { return future.get(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); return new Result(false,"Cancelled",0,0); }
        catch(ExecutionException | CancellationException e) { return new Result(false,e.getCause()==null?e.toString():e.getCause().getMessage(),0,0); }
    }
    private Result ping(ServerAddress address) {
        try(Socket connection=new Socket()) {
            socket=connection; if(closed) return new Result(false,"Cancelled",0,0);
            connection.connect(new InetSocketAddress(address.host(),address.port()),3000); connection.setSoTimeout(3000);
            connection.getOutputStream().write(0xfe); connection.getOutputStream().flush();
            DataInputStream in=new DataInputStream(connection.getInputStream());
            if(in.readUnsignedByte()!=0xff) return new Result(false,"Not a legacy Minecraft ping response",0,0);
            int length=in.readUnsignedShort(); if(length>4096) return new Result(false,"Invalid server response",0,0);
            byte[] bytes=in.readNBytes(length*2); if(bytes.length!=length*2) throw new EOFException("Truncated server response");
            String text=new String(bytes,java.nio.charset.StandardCharsets.UTF_16BE);
            String[] parts=text.split("\u00a7",-1); if(parts.length<3) return new Result(true,text,0,0);
            int online=Integer.parseInt(parts[parts.length-2]),max=Integer.parseInt(parts[parts.length-1]);
            return new Result(true,String.join("\u00a7",java.util.Arrays.copyOf(parts,parts.length-2)),online,max);
        } catch(IOException | IllegalArgumentException e) { return new Result(false,e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(),0,0); }
        finally { socket=null; }
    }
    @Override public void close() { closed=true; future.cancel(true); Socket s=socket; if(s!=null) try { s.close(); } catch(IOException ignored) {} executor.shutdownNow(); }
}
