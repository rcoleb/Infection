package com.fhs.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        
        ig.population.createPeople(Constants.currPopulation, Constants.currSize[0], Constants.currSize[1], Constants.currInfected);
        frame.add(ig);
        frame.add(cp);
        frame.setVisible(true);
        
        Timer timer = new Timer("repaintTimer", true);
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
        }, 0, 40);
        
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
                    ig.startInfected = !e.isShiftDown();
                } else {
                    return;
                }
                ig.startX = e.getX();
                ig.startY = e.getY();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (ig.startX != -1 && ig.startY != -1) {
                    int dx, dy;
                    if (Math.abs(ig.startX - e.getX()) < 0.1 && Math.abs(ig.startY - e.getY()) < 0.1) {
                        Random rand = new Random();
                        int cnt = 1;
                        if (e.isAltDown()) {
                            cnt = 40;
                        }
                        for (int i = 0; i < cnt; i++) {
                            dx = rand.nextInt(32);
                            if (rand.nextBoolean()) dx *= -1;
                            dy = rand.nextInt(32);
                            if (rand.nextBoolean()) dy *= -1;
                            ig.population.createPerson(ig.startX, ig.startY, dx, dy, ig.startInfected);
                        }
                    } else {
                        dx = ig.startX - e.getX();
                        dy = ig.startY - e.getY();
                        ig.population.createPerson(ig.startX, ig.startY, dx, dy, ig.startInfected);
                    }
                }
                ig.startX = ig.startY = -1;
                ig.startInfected = false;
            }
            
        });
        
        double dTime = 0.01 / 200;
        double currTime = System.currentTimeMillis();
        double accum = 0.0;
            
        while(ig.runSim) {
            double newTime = System.currentTimeMillis();
            double frameTime = newTime - currTime;
            currTime = newTime;
            
            accum += frameTime;
            
            while (accum >= dTime && ig.runSim) {
                if (!ig.pauseSim) {
                    ig.population.update(dTime, ig.getBounds());
                }
                accum -= dTime;
            }
        }
        
        timer.cancel();
        frame.setVisible(false);
        frame.dispose();
        
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
                g2d.setColor(new Color(tempInf, 0, 0, tempInf / 3));
                int rad = Constants.currInfectRad;
                int diam = (Constants.currInfectRad * 2) + sz;
                g2d.fillOval((int) person.x - rad, (int) person.y - rad, diam, diam);
                
                if (person.incubation > 0 && this.drawIncub) {
                    g2d.setColor(Color.yellow.brighter());
                    float ratio = (person.incubation / Constants.currIncub) * (sz * 2);
                    g2d.drawOval((int) (person.x - ratio), (int) (person.y - ratio), (int) (2 * ratio) + sz, (int) (2 * ratio) + sz);
                }
                
                g2d.setColor(Color.RED);
            }
            g2d.fillOval((int) (person.x), (int) (person.y), sz, sz);
            
            if (this.drawInfectionTree && person.selected) {
                if (person.infector != null) {
                    Color c = g2d.getColor();
                    g2d.setColor(c.darker());
                    g2d.drawLine((int) person.x + (Constants.AGENT_SIZE / 2), (int) person.y + (Constants.AGENT_SIZE / 2), (int) person.infector.x + (Constants.AGENT_SIZE / 2), (int) person.infector.y + (Constants.AGENT_SIZE / 2));
                    g2d.setColor(c);
                }
                for (Agent inf : person.infected ) {
                    g2d.drawLine((int) person.x + (Constants.AGENT_SIZE / 2), (int) person.y + (Constants.AGENT_SIZE / 2), (int) inf.x + (Constants.AGENT_SIZE / 2), (int) inf.y + (Constants.AGENT_SIZE / 2));
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
    
    Population population = new Population(this.getBounds(), false, false);
    private boolean drawVeloc;
    private boolean drawIncub;
    protected boolean drawInfectionTree;
    
    public void setDrawVelocity(boolean selected) {
        this.drawVeloc = selected;
    }
    
    public void setDrawIncubation(boolean selected) {
        this.drawIncub = selected;
    }
    
    public void pausePlay(boolean sel) {
        this.pauseSim = sel;
    }

    public void restart(int pop, int szX, int szY, int infect, int infRad, int incubation, int topSpeed, boolean follow, boolean avoid) {
        Constants.currPopulation = pop;
        Constants.currSize = new int[] {szX, szY};
        Constants.currInfected = infect;
        Constants.currInfectRad = infRad;
        Constants.currIncub = incubation;
        Constants.currTopSpeed = topSpeed;
        this.population = new Population(this.getBounds(), follow, avoid);
        this.population.createPeople(pop, szX, szY, infect);
    }

}
