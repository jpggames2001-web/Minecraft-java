package dev.skyblock.game;

import java.util.Arrays;

public final class Inventory {
    private final Stack[] slots;
    public Inventory(int size) { if(size<1 || size>128) throw new IllegalArgumentException("Invalid inventory size"); slots=new Stack[size]; }
    public int size() { return slots.length; }
    public Stack get(int index) { return slots[index]; }
    public void set(int index,Stack stack) { slots[index]=stack; }
    public void clear() { Arrays.fill(slots,null); }
    /** Returns any remainder; caller keeps ownership rather than deleting overflow. */
    public Stack add(Stack stack) {
        if(stack==null) return null;
        int count=stack.count();
        for(int i=0;i<slots.length && count>0;i++) {
            Stack s=slots[i];
            if(s!=null && s.merges(stack)) {
                int n=Math.min(count,s.item().limit-s.count());
                slots[i]=s.withCount(s.count()+n); count-=n;
            }
        }
        for(int i=0;i<slots.length && count>0;i++) if(slots[i]==null) {
            int n=Math.min(count,stack.item().limit); slots[i]=new Stack(stack.item(),n,stack.damage()); count-=n;
        }
        return count==0 ? null : new Stack(stack.item(),count,stack.damage());
    }
    public Stack take(int slot,int count) {
        Stack s=get(slot); if(s==null) return null;
        int n=Math.min(s.count(),count); set(slot,s.withCount(s.count()-n)); return new Stack(s.item(),n,s.damage());
    }
}
