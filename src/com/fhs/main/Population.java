package com.fhs.main;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

import com.fhs.main.ControlPanel.UpdateListener;

public class Population {
    private static final Object   GRID_LOCK       = new Object();
    private static final Object   LIST_LOCK_1     = new Object();
    private static final Object   LIST_LOCK_2     = new Object();
    volatile ArrayList<Agent>[][] agentGrid;
    volatile Rectangle            currBnds;
    volatile ArrayList<Agent>     infected        = new ArrayList<>();
    volatile ArrayList<Agent>     people          = new ArrayList<>();
    Stat                          prevStat        = null;
    
    ArrayList<Stat>               stats           = new ArrayList<>();
    
    volatile double               time            = 0.0;
    
    ArrayList<UpdateListener>     updateListeners = new ArrayList<>();
    
    boolean                       optFollowHost   = false;
    boolean                       optAvoidInfect  = false;
    
    public void addPerson(final Agent person) {
        synchronized (Population.LIST_LOCK_1) {
            this.people.add(person);
        }
        synchronized (Population.LIST_LOCK_2) {
            if (person.infect > 0) {
                this.infected.add(person);
            }
        }
    }
    
    private ArrayList<Agent> infect(final Agent host, final int indX, final int indY) {
        final ArrayList<Agent> newInfected = new ArrayList<>();
        synchronized (Population.GRID_LOCK) {
            for (final Agent person : this.agentGrid[indX][indY]) {
                // ignore already infected; later hyper-infected may increase takeover rate in less-infected people
                if (person.infect > 0) {
                    continue;
                }
                // ignore out of range people
                if ((person.x > (host.x + Constants.INFECT_RADIUS)) || (person.x <= (host.x - Constants.INFECT_RADIUS))) {
                    continue;
                }
                if ((person.y > (host.y + Constants.INFECT_RADIUS)) || (person.y <= (host.y - Constants.INFECT_RADIUS))) {
                    continue;
                }
                person.infect = person.deltaInfect;
                person.incubation = Constants.INCUBATION;
                person.infector = host;
                
                if (this.optFollowHost) {
                    double m = host.dx / host.dy;
                    double c = Math.sqrt(person.dx * person.dx + person.dy * person.dy);
                    double newY = c / Math.sqrt((m * m) + 1); 
                    double newX = m * newY;
                    if (host.dy < 0) {
                        person.dx = -1 * (float) newX;
                        person.dy = -1 * (float) newY;
                    } else {
                        person.dx = (float) newX;
                        person.dy = (float) newY;
                    }
                }
                
                host.infected.add(person);
                newInfected.add(person);
            }
        }
        
        return newInfected;
    }
    
    protected void pingUpdate() {
        for (final UpdateListener ul : this.updateListeners) {
            ul.pingUpdate();
        }
    }
    
    public void registerUpdateListener(final UpdateListener lister) {
        this.updateListeners.add(lister);
    }
    
    public Agent retrieve(final int x, final int y) {
        final int indX = (int) Math.floor((Constants.AGENT_GRID_CELLS * x) / this.currBnds.width);
        final int indY = (int) Math.floor((Constants.AGENT_GRID_CELLS * y) / this.currBnds.height);
        
        final TreeMap<Double, Agent> distMap = new TreeMap<>();
        synchronized (Population.GRID_LOCK) {
            final ArrayList<Agent> currCell = this.agentGrid[indX][indY];
            
            // dist = sqrt((x2 - x1)^2 + (y2 - y1)^2)
            for (final Agent agent : currCell) {
                distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
            }
        }
        
        final int indN = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (y - Constants.SELECT_RADIUS)) / this.currBnds.height));
        final boolean checkN = (indN < indY) && (indN >= 0);
        final int indS = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (y + Constants.SELECT_RADIUS)) / this.currBnds.height));
        final boolean checkS = (indS > indY) && (indS < Constants.AGENT_GRID_CELLS);
        final int indW = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (x - Constants.SELECT_RADIUS)) / this.currBnds.width));
        final boolean checkW = (indW < indX) && (indW >= 0);
        final int indE = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (x + Constants.SELECT_RADIUS)) / this.currBnds.width));
        final boolean checkE = (indE > indX) && (indE < Constants.AGENT_GRID_CELLS);
        
        synchronized (Population.GRID_LOCK) {
            if (checkN) {
                for (final Agent agent : this.agentGrid[indX][indN]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkS) {
                for (final Agent agent : this.agentGrid[indX][indS]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkE) {
                for (final Agent agent : this.agentGrid[indE][indY]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkW) {
                for (final Agent agent : this.agentGrid[indW][indY]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkN && checkE) {
                for (final Agent agent : this.agentGrid[indE][indN]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkN && checkW) {
                for (final Agent agent : this.agentGrid[indW][indN]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkS && checkE) {
                for (final Agent agent : this.agentGrid[indE][indS]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
            
            if (checkS && checkW) {
                for (final Agent agent : this.agentGrid[indW][indS]) {
                    distMap.put(Double.valueOf(Math.sqrt(((agent.x - x) * (agent.x - x)) + ((agent.y - y) * (agent.y - y)))), agent);
                }
            }
        }
        
        return (!distMap.isEmpty()) && (distMap.firstKey().doubleValue() < Constants.SELECT_RADIUS) ? distMap.firstEntry().getValue() : null;
    }
    
    public void update(final double timeDelta, final Rectangle bnds) {
        this.currBnds = bnds;
        synchronized (Population.LIST_LOCK_1) {
            for (int i = 0; i < this.people.size(); i++) {
                final Agent person = this.people.get(i);
                
                person.x += timeDelta * person.dx;
                person.y += timeDelta * person.dy;
                
                if (person.x <= 0.0) {
                    person.x = 0;
                    if (person.dx < 0.0) {
                        person.dx *= -1.0;
                    }
                }
                if (person.x >= bnds.width) {
                    person.x = bnds.width;
                    if (person.dx > 0.0) {
                        person.dx *= -1.0;
                    }
                }
                if (person.y <= 0.0) {
                    person.y = 0;
                    if (person.dy < 0.0) {
                        person.dy *= -1.0;
                    }
                }
                if (person.y >= bnds.height) {
                    person.y = bnds.height;
                    if (person.dy > 0.0) {
                        person.dy *= -1.0;
                    }
                }
                
                if (person.incubation > 0) {
                    person.incubation -= timeDelta;
                } else {
                    if (person.infect > 0) {
                        person.infect = (float) Math.min(255, person.infect + (timeDelta * person.deltaInfect));
                    }
                }
                
            }
            
            synchronized (Population.GRID_LOCK) {
                this.agentGrid = new ArrayList[Constants.AGENT_GRID_CELLS + 1][Constants.AGENT_GRID_CELLS + 1];
                for (int i = 0; i < (Constants.AGENT_GRID_CELLS + 1); i++) {
                    for (int j = 0; j < (Constants.AGENT_GRID_CELLS + 1); j++) {
                        this.agentGrid[i][j] = new ArrayList<>();
                    }
                }
                final int stepW = bnds.width / Constants.AGENT_GRID_CELLS;
                final int stepH = bnds.height / Constants.AGENT_GRID_CELLS;
                for (final Agent person : this.people) {
                    final int indX = (int) Math.floor((Constants.AGENT_GRID_CELLS * person.x) / bnds.width);
                    final int indY = (int) Math.floor((Constants.AGENT_GRID_CELLS * person.y) / bnds.height);
                    this.agentGrid[indX][indY].add(person);
                }
            }
        }
        
        final ArrayList<Agent> newInfected = new ArrayList<>();
        synchronized (Population.LIST_LOCK_2) {
            for (final Agent host : this.infected) {
                if (host.incubation > 0) {
                    continue;
                }
                final int hostX = (int) Math.floor((Constants.AGENT_GRID_CELLS * host.x) / bnds.width);
                final int hostY = (int) Math.floor((Constants.AGENT_GRID_CELLS * host.y) / bnds.height);
                // check my bucket:
                newInfected.addAll(this.infect(host, hostX, hostY));
                // check if we need to look at adjacent buckets:
                final int indN = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (host.y - Constants.INFECT_RADIUS)) / bnds.height));
                final boolean checkN = (indN < hostY) && (indN >= 0);
                final int indS = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (host.y + Constants.INFECT_RADIUS)) / bnds.height));
                final boolean checkS = (indS > hostY) && (indS < Constants.AGENT_GRID_CELLS);
                final int indW = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (host.x - Constants.INFECT_RADIUS)) / bnds.width));
                final boolean checkW = (indW < hostX) && (indW >= 0);
                final int indE = ((int) Math.floor((Constants.AGENT_GRID_CELLS * (host.x + Constants.INFECT_RADIUS)) / bnds.width));
                final boolean checkE = (indE > hostX) && (indE < Constants.AGENT_GRID_CELLS);
                
                if (checkN) {
                    newInfected.addAll(this.infect(host, hostX, indN));
                }
                if (checkE) {
                    newInfected.addAll(this.infect(host, indE, hostY));
                }
                if (checkS) {
                    newInfected.addAll(this.infect(host, hostX, indS));
                }
                if (checkW) {
                    newInfected.addAll(this.infect(host, indW, hostY));
                }
                if (checkN && checkE) {
                    newInfected.addAll(this.infect(host, indE, indN));
                }
                if (checkN && checkW) {
                    newInfected.addAll(this.infect(host, indW, indN));
                }
                if (checkS && checkE) {
                    newInfected.addAll(this.infect(host, indE, indS));
                }
                if (checkS && checkW) {
                    newInfected.addAll(this.infect(host, indW, indS));
                }
                
            }
            this.infected.addAll(newInfected);
        }
        
        this.time += timeDelta;
        
        this.updateStats();
    }
    
    private void updateStats() {
        final Stat newStat = new Stat(this.time, this.people.size(), this.people.size() - this.infected.size(), this.infected.size());
        // uncomment to only include changes in stat; this breaks naive graphing though!
        // if (this.prevStat == null || this.prevStat.pop != newStat.pop || this.prevStat.h != newStat.h || this.prevStat.i != newStat.i) {
        this.stats.add(newStat);
        // this.prevStat = newStat;
        this.pingUpdate();
        // }
    }

    public void createPeople(int cnt, int h, int w, int infect) {
        Random rand = new Random();
        for (int i = 0; i < cnt; i++) {
            Agent agent = new Agent();
            agent.name = "Agent_" + i;
            agent.x = rand.nextInt(w);
            agent.y = rand.nextInt(w);
            agent.dx = rand.nextInt(Constants.TOP_GEN_SPEED);
            if (rand.nextBoolean()) agent.dx *= -1;
            agent.dy = rand.nextInt(Constants.TOP_GEN_SPEED);
            if (rand.nextBoolean()) agent.dy *= -1;
            if (rand.nextInt(100) < infect)
                agent.infect = rand.nextInt(256);
            else
                agent.infect = 0;
            addPerson(agent);
            
        }
        
    }

    public void createPerson(int x, int y, boolean infected) {
        Random rand = new Random();
        Agent agent = new Agent();
        agent.name = "Agent_custom_" + x + "," + y;
        agent.x = x;
        agent.y = y;
        agent.dx = rand.nextInt(32);
        if (rand.nextBoolean()) agent.dx *= -1;
        agent.dy = rand.nextInt(32);
        if (rand.nextBoolean()) agent.dy *= -1;
        if (infected)
            agent.infect = rand.nextInt(256);
        else
            agent.infect = 0;
        this.addPerson(agent);
    }

    protected void createPerson(int x, int y, int dx, int dy, boolean startInfected) {
        Agent agent = new Agent();
        agent.name = "Agent_custom_" + x + "," + y;
        agent.x = x;
        agent.y = y;
        agent.dx = dx;
        agent.dy = dy;
        agent.infect = startInfected ? 255 : 0;
        this.addPerson(agent);
    }
    
}
