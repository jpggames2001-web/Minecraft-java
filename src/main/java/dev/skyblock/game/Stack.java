package dev.skyblock.game;

public record Stack(Item item,int count,int damage) {
    public Stack {
        if(item==null || count<1 || count>item.limit || damage<0 || damage>item.durability) throw new IllegalArgumentException("Invalid stack");
    }
    public Stack(Item item,int count) { this(item,count,0); }
    public Stack withCount(int n) { return n==0 ? null : new Stack(item,n,damage); }
    public boolean merges(Stack other) { return other!=null && item==other.item && damage==other.damage; }
    public Stack wear() { return item.durability==0 ? this : damage+1>=item.durability ? null : new Stack(item,count,damage+1); }
}
