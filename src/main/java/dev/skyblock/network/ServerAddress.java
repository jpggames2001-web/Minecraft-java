package dev.skyblock.network;
public record ServerAddress(String host,int port) {
    public ServerAddress { if(host==null || host.isBlank() || host.chars().anyMatch(Character::isWhitespace) || port<1 || port>65535) throw new IllegalArgumentException("Invalid server address"); }
    public static ServerAddress parse(String input) {
        input=input.strip(); String host=input; int port=25565;
        if(input.startsWith("[")) { int end=input.indexOf(']'); if(end<0) throw new IllegalArgumentException("Invalid IPv6 address"); host=input.substring(1,end); if(end+1<input.length()) { if(input.charAt(end+1)!=':') throw new IllegalArgumentException("Invalid server port"); port=Integer.parseInt(input.substring(end+2)); } }
        else if(input.indexOf(':')==input.lastIndexOf(':') && input.contains(":")) { int colon=input.indexOf(':'); host=input.substring(0,colon); port=Integer.parseInt(input.substring(colon+1)); }
        return new ServerAddress(host,port);
    }
}
