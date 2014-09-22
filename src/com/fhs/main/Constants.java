package com.fhs.main;

class Constants {
    static final int POPULATION = 400;
    static final int[] CITY_SIZE = {800, 800};
    static final int INFECTED = 0;
    
    static final int INFECT_RADIUS = 8;
    static final int INCUBATION = 10;
    static final int AGENT_SIZE = 4;
    static final int AGENT_GRID_CELLS = 11;
    static final int TOP_GEN_SPEED = 46;
    static final int SELECT_RADIUS = 10;
    
    static int currPopulation = POPULATION;
    static int[] currSize = CITY_SIZE;
    static int currInfected = Math.min(INFECTED, POPULATION);
    static int currInfectRad = INFECT_RADIUS;
    static int currIncub = INCUBATION;
    static int currTopSpeed = TOP_GEN_SPEED;
}