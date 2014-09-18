package com.fhs.main;

class Stat {
    final double time;
    final int pop;
    final int h;
    final int i;
    public Stat(double t, int p, int h, int i) {
        this.time = t;
        this.pop = p;
        this.h = h;
        this.i = i;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.time).append(",").append(this.pop).append(",").append(this.h).append(",").append(this.i);
        return builder.toString();
    }
    
}