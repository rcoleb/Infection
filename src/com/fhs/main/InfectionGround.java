package com.fhs.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
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
    
    volatile boolean runSim = true;
    volatile boolean pauseSim = false;
    protected int startX = -1;
    protected int startY = -1;
    protected boolean startInfected = true;

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1000, 1000);
        frame.setLayout(new MigLayout("insets 0, gap rel 0", "[grow, fill][10%]", "[grow, fill]"));
        final InfectionGround ig = new InfectionGround();
        final ControlPanel cp = new ControlPanel(ig);
        
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
                
                if (e.getButton() == 1) { // left click
                    Agent clickedAgent = ig.population.retrieve(x, y);
                    cp.clickedAgent(clickedAgent);
                }
                
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == 2) {
                    ig.startInfected = false;
                } else if (e.getButton() == 3){
                    ig.startInfected = true;
                } else {
                    return;
                }
                ig.startX = e.getX();
                ig.startY = e.getY();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (ig.startX != -1 && ig.startY != -1)
                ig.createPerson(e.getX(), e.getY());
            }
            
        });
        
        Thread updateThread = new Thread(new Runnable() {
            double dTime = 0.01 / 200;
            double currTime = System.currentTimeMillis();
            double accum = 0.0;
            
            @Override
            public void run() {
                while(ig.runSim) {
                    if (ig.pauseSim) {
                        this.currTime = System.currentTimeMillis();
                        continue;
                    }
                    double newTime = System.currentTimeMillis();
                    double frameTime = newTime - this.currTime;
                    this.currTime = newTime;
                    
                    this.accum += frameTime;
                    
                    while (this.accum >= this.dTime) {
                        if (!ig.pauseSim) {
                            ig.population.update(this.dTime, ig.getBounds());
                        }
                        this.accum -= this.dTime;
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
                
            } else {
                int tempInf = (int) person.infect;
                g2d.setColor(new Color(tempInf, 255 - tempInf, 0, tempInf / 3));
                int rad = Constants.INFECT_RADIUS;
                int diam = (Constants.INFECT_RADIUS * 2) + sz;
                g2d.fillOval((int) person.x - rad, (int) person.y - rad, diam, diam);
                
                if (person.incubation > 0 && this.drawIncub) {
                    g2d.setColor(Color.yellow.brighter());
                    float ratio = (person.incubation / Constants.INCUBATION) * (sz * 2);
                    g2d.drawOval((int) (person.x - ratio), (int) (person.y - ratio), (int) (2 * ratio) + sz, (int) (2 * ratio) + sz);
                }
                
                g2d.setColor(Color.RED);
            }
            g2d.fillOval((int) (person.x), (int) (person.y), sz, sz);
            
            if (this.drawInfectionTree && person.selected) {
                if (person.infector != null) {
                    Color c = g2d.getColor();
                    g2d.setColor(c.darker());
                    g2d.drawLine((int) person.x, (int) person.y, (int) person.infector.x, (int) person.infector.y);
                    g2d.setColor(c);
                }
                for (Agent inf : person.infected ) {
                    g2d.drawLine((int) person.x, (int) person.y, (int) inf.x, (int) inf.y);
                }
            }
            
            if (this.drawVeloc) {
                Color crr = g2d.getColor();
                g2d.setColor(new Color(crr.getRed(), crr.getGreen(), crr.getBlue(), 128));
                g2d.drawLine((int) (person.x + (sz / 2.0)), (int) (person.y + (sz / 2.0)), (int) (person.x - (person.dx)), (int) (person.y - (person.dy)));
            }
            
            if (person.selected) {
                g2d.setColor(Color.white);
                g2d.drawRect((int)person.x - Constants.SELECT_RADIUS, (int)person.y - Constants.SELECT_RADIUS, Constants.SELECT_RADIUS * 2 + Constants.AGENT_SIZE, Constants.SELECT_RADIUS * 2 + Constants.AGENT_SIZE);
            }
            
        }
        
        if (this.startX != -1 && this.startY != -1) {
            g2d.setColor(this.startInfected ? Color.RED : Color.GREEN);
            Point pt = MouseInfo.getPointerInfo().getLocation();
            int x = (int) (pt.getX() - this.getLocationOnScreen().getX());
            int y = (int) (pt.getY() - this.getLocationOnScreen().getY());
            g2d.drawLine(this.startX, this.startY, x, y);
        }
        
    }
    
    Population population = new Population();
    private boolean drawVeloc;
    private boolean drawIncub;
    protected boolean drawInfectionTree;
    
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

    protected void createPerson(int x, int y) {
        Agent agent = new Agent();
        agent.name = "Agent_custom_" + this.startX + "," + this.startY;
        agent.x = this.startX;
        agent.y = this.startY;
        if (Math.abs(this.startX - x) < 0.1 && Math.abs(this.startY - y) < 0.1) {
            Random rand = new Random();
            agent.dx = rand.nextInt(32);
            if (rand.nextBoolean()) agent.dx *= -1;
            agent.dy = rand.nextInt(32);
            if (rand.nextBoolean()) agent.dy *= -1;
        } else {
            agent.dx = this.startX - x;
            agent.dy = this.startY - y;
        }
        agent.infect = this.startInfected ? 255 : 0;
        this.startX = this.startY = -1;
        this.startInfected = false;
        this.population.addPerson(agent);
    }



    public void setDrawVelocity(boolean selected) {
        this.drawVeloc = selected;
    }
    
    public void setDrawIncubation(boolean selected) {
        this.drawIncub = selected;
    }
    
    public void pausePlay(boolean sel) {
        this.pauseSim = sel;
    }

}

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

class Constants {
    static final int POPULATION = 200;
    static final int[] CITY_SIZE = {800, 800};
    static final int INFECTED_PCT = 0;
    
    static final int INFECT_RADIUS = 8;
    static final int INCUBATION = 10;
    static final int AGENT_SIZE = 4;
    static final int AGENT_GRID_CELLS = 11;
    static final int TOP_GEN_SPEED = 46;
    static final int SELECT_RADIUS = 10;
}
