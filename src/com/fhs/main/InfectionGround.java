package com.fhs.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

public class InfectionGround extends JPanel {
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLayout(new MigLayout("insets 0, gap rel 0", "[grow, fill][10%]", "[grow, fill]"));
        final InfectionGround ig = new InfectionGround();
        final ControlPanel cp = new ControlPanel();
        
        ig.createPeople(Constants.POPULATION, Constants.CITY_SIZE[0], Constants.CITY_SIZE[1], Constants.INFECTED_PCT);
        frame.add(ig);
        frame.add(cp);
        frame.setVisible(true);
        
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int totPop = ig.population.people.size();
                        int infect = ig.population.infected.size();
                        cp.dLblTotPop.setText("" + totPop);
                        cp.dLblInfected.setText("" + infect);
                        cp.dLblHealthy.setText("" + (totPop - infect));
                        ig.repaint();
                    }
                });
            }
        }, 0, 50);
        
        ig.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                ig.createPerson(x, y, true);
            }
        });
        
        Thread updateThread = new Thread(new Runnable() {
            double time = 0.0;
            double dTime = 0.01 / 200;
            double currTime = System.currentTimeMillis();
            double accum = 0.0;
            
            @Override
            public void run() {
                while(true) {
                    double newTime = System.currentTimeMillis();
                    double frameTime = newTime - this.currTime;
                    this.currTime = newTime;
                    
                    this.accum += frameTime;
                    
                    while (this.accum >= this.dTime) {
                        ig.population.update(this.dTime, ig.getBounds());
                        this.accum -= this.dTime;
                        this.time += this.dTime;
                    }
                }
            }
        }, "updateThread");
        updateThread.setDaemon(true);
        updateThread.start();
        
        
    }
    
    
    
    public InfectionGround() {
        this.setBackground(Color.black);
    }
    
    
    @Override
    protected void paintComponent(Graphics arg0) {
        super.paintComponent(arg0);

        Graphics2D g2d = (Graphics2D) arg0;
        
        for (int i = 0; i < this.population.people.size(); i++) {
            Agent person = this.population.people.get(i);
            
            int sz = Constants.AGENT_SIZE;
            
            if (person.infect == 0) {
                g2d.setColor(Color.GREEN);
                g2d.fillOval((int) (person.x), (int) (person.y), sz, sz);
            } else {
                int tempInf = (int) person.infect;
                g2d.setColor(new Color(tempInf, 255 - tempInf, 0, tempInf / 3));
                int rad = Constants.INFECT_RADIUS;
                int diam = (Constants.INFECT_RADIUS * 2) + sz;
                g2d.fillOval((int) person.x - rad, (int) person.y - rad, diam, diam);
                
                if (person.incubation > 0) {
                    g2d.setColor(Color.yellow.brighter());
                    float ratio = (person.incubation / Constants.INCUBATION) * (sz * 2);
                    g2d.drawOval((int) (person.x - ratio), (int) (person.y - ratio), (int) (2 * ratio) + sz, (int) (2 * ratio) + sz);
                }
                
                g2d.setColor(new Color(255, 0, 0));
                g2d.fillOval((int) (person.x), (int) (person.y), sz, sz);
            }
            Color crr = g2d.getColor();
            g2d.setColor(new Color(crr.getRed(), crr.getGreen(), crr.getBlue(), 128));
            g2d.drawLine((int) (person.x + (sz / 2.0)), (int) (person.y + (sz / 2.0)), (int) (person.x - (person.dx)), (int) (person.y - (person.dy)));
            
        }
    }
    
    Population population = new Population();
    
    public void createPeople(int cnt, int h, int w, int infect) {
        Random rand = new Random();
        for (int i = 0; i < cnt; i++) {
            Agent agent = new Agent();
            agent.name = "Agent_" + i;
            agent.x = rand.nextInt(w);
            agent.y = rand.nextInt(w);
            agent.dx = rand.nextInt(32);
            if (rand.nextBoolean()) agent.dx *= -1;
            agent.dy = rand.nextInt(32);
            if (rand.nextBoolean()) agent.dy *= -1;
            if (rand.nextInt(100) < infect)
                agent.infect = rand.nextInt(256);
            else
                agent.infect = 0;
            this.population.addPerson(agent);
            
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
        this.population.addPerson(agent);
    }
    
}

class Population {
    ArrayList<Agent> people = new ArrayList<>();
    ArrayList<Agent> infected = new ArrayList<>();
    ArrayList<Agent>[][] agentGrid;
    volatile boolean updating = false;
    
    public void addPerson(Agent person) {
        while(this.updating) {};
        this.people.add(person);
        if (person.infect > 0) this.infected.add(person);
    }
    
    public void update(double timeDelta, Rectangle bnds) {
        this.updating = true;
        for (int i = 0; i < this.people.size(); i++) {
            Agent person = this.people.get(i);
            
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
        
        int cells = 11;
        this.agentGrid = new ArrayList[cells+1][cells+1];
        for (int i = 0; i < cells + 1; i++) {
            for (int j = 0; j < cells + 1; j++) {
                this.agentGrid[i][j] = new ArrayList<>();
            }
        }
        int stepW = bnds.width / cells;
        int stepH = bnds.height / cells;
        for (Agent person : this.people) {
            int indX = (int) Math.floor((cells * person.x) / bnds.width);
            int indY = (int) Math.floor((cells * person.y) / bnds.height);
            this.agentGrid[indX][indY].add(person);
        }

        ArrayList<Agent> newInfected = new ArrayList<>();
        for (Agent host : this.infected) {
            if (host.incubation > 0) continue;
            int hostX = (int) Math.floor((cells * host.x) / bnds.width);
            int hostY = (int) Math.floor((cells * host.y) / bnds.height);
            // check my bucket:
            newInfected.addAll(infect(host, hostX, hostY, this.agentGrid));
            // check if we need to look at adjacent buckets:
            int indN = ((int) Math.floor((cells * (host.y - Constants.INFECT_RADIUS)) / bnds.height));
            boolean checkN = indN < hostY && indN >= 0;
            int indS = ((int) Math.floor((cells * (host.y + Constants.INFECT_RADIUS)) / bnds.height));
            boolean checkS = indS > hostY && indS < cells;
            int indW = ((int) Math.floor((cells * (host.x - Constants.INFECT_RADIUS)) / bnds.width));
            boolean checkW = indW < hostX && indW >= 0;
            int indE = ((int) Math.floor((cells * (host.x + Constants.INFECT_RADIUS)) / bnds.width));
            boolean checkE = indE > hostX && indE < cells;
            
            if (checkN) { newInfected.addAll(infect(host, hostX, indN, this.agentGrid)); }
            if (checkE) { newInfected.addAll(infect(host, indE, hostY, this.agentGrid)); }
            if (checkS) { newInfected.addAll(infect(host, hostX, indS, this.agentGrid)); }
            if (checkW) { newInfected.addAll(infect(host, indW, hostY, this.agentGrid)); }
            if (checkN && checkE) { newInfected.addAll(infect(host, indE, indN, this.agentGrid)); }
            if (checkN && checkW) { newInfected.addAll(infect(host, indW, indN, this.agentGrid)); }
            if (checkS && checkE) { newInfected.addAll(infect(host, indE, indS, this.agentGrid)); }
            if (checkS && checkW) { newInfected.addAll(infect(host, indW, indS, this.agentGrid)); }
            
        }
        
        this.infected.addAll(newInfected);
        this.updating = false;
    }
    
    private ArrayList<Agent> infect(Agent host, int indX, int indY, ArrayList<Agent>[][] agentGrid) {
        ArrayList<Agent> newInfected = new ArrayList<>();
        for (Agent person : agentGrid[indX][indY]) {
            // ignore already infected;  later hyper-infected may increase takeover rate in less-infected people
            if (person.infect > 0) continue;
            // ignore out of range people
            if (person.x > host.x + Constants.INFECT_RADIUS || person.x <= host.x - Constants.INFECT_RADIUS) continue;
            if (person.y > host.y + Constants.INFECT_RADIUS || person.y <= host.y - Constants.INFECT_RADIUS) continue;
            person.infect = person.deltaInfect;
            person.incubation = Constants.INCUBATION;
            newInfected.add(person);
        }
        return newInfected;
    }
    
}

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

class Constants {
    static final int POPULATION = 200;
    static final int[] CITY_SIZE = {800, 800};
    static final int INFECTED_PCT = 0;
    
    static final int INFECT_RADIUS = 8;
    static final int INCUBATION = 10;
    static final int AGENT_SIZE = 4;
}
