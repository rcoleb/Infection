package com.fhs.main;

import java.util.ArrayList;

class Agent {
    
    public float incubation;
    // unique id
    String name;
    // 0 -> W; 0 -> H
    float x, y;
    // 
    float dx, dy;
    // 0 - 255
    float infect;
    // set for now
    int deltaInfect = 5;
    boolean selected = false;
    Agent infector = null;
    ArrayList<Agent> infected = new ArrayList<>();
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Agent)) {
            return false;
        }
        Agent other = (Agent) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }
    
}