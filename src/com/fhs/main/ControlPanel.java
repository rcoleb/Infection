package com.fhs.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

public class ControlPanel extends JPanel {
    
    interface UpdateListener {
        void pingUpdate();
    }
    
    class PopGraph extends JPanel implements UpdateListener {
        
        private static final int NS_INSET = 3;
        private static final int EW_INSET = 0; 
        InfectionGround ig;
        
        public PopGraph(InfectionGround scape) {
            this.ig = scape;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            Rectangle bnds = g2d.getClipBounds();
            g2d.setColor(Color.black);
            // draw base line:
            g2d.drawLine(EW_INSET, bnds.height - NS_INSET, bnds.width - EW_INSET, bnds.height - NS_INSET);
            g2d.drawLine(EW_INSET, NS_INSET, EW_INSET, bnds.height - NS_INSET);
            
            ArrayList<Stat> stats = (ArrayList<Stat>) this.ig.population.stats.clone();
            if (stats.isEmpty()) return;
            float dispHght = bnds.height - NS_INSET - NS_INSET;
            float dispWdth = bnds.width - EW_INSET - EW_INSET;
            
            int maxPop = stats.get(0).pop;
            for (Stat stat : stats) {
                if (stat == null) continue;
                if (stat.pop > maxPop) maxPop = stat.pop;
            }
            int sampleSkip = stats.size() > bnds.width ? stats.size() / bnds.width : 1;
            float tickWidth = stats.size() > bnds.width ? 1 : bnds.width / stats.size();
            draw_healthy : {
                g2d.setColor(Color.GREEN.darker());
                double tickXStart = EW_INSET + 1;
                double tickXEnd = tickXStart + tickWidth;
                double tickYStart = NS_INSET + (((Constants.POPULATION - (Constants.INFECTED_PCT * Constants.POPULATION)) / Constants.POPULATION));
                double tickYEnd = tickYStart;
                for (int i = 0; i < stats.size(); i += sampleSkip) {
                    Stat stat = stats.get(i);
                    double ratio = stat.h / (double)maxPop;
                    
                    tickYEnd = (NS_INSET + (dispHght - ((dispHght - 1) * ratio)));
                    g2d.drawLine((int) tickXStart, (int) tickYStart, (int) tickXEnd, (int) tickYEnd);
                    
                    tickXStart = tickXEnd;
                    tickXEnd += tickWidth;
                    tickYStart = tickYEnd;
                }
            }
            draw_infected : {
                g2d.setColor(Color.RED.darker());
                float tickXStart = EW_INSET + 1;
                float tickXEnd = tickXStart + tickWidth;
                float tickYStart = NS_INSET + (dispHght - (Constants.INFECTED_PCT * Constants.POPULATION));
                float tickYEnd = tickYStart;
                for (int i = 0; i < stats.size(); i += sampleSkip) {
                    Stat stat = stats.get(i);
                    float ratio = stat.i / (float)maxPop;
                    
                    tickYEnd = (NS_INSET + (dispHght - ((dispHght - 1) * ratio)));
                    g2d.drawLine((int) tickXStart, (int) tickYStart, (int) tickXEnd, (int) tickYEnd);
                    
                    tickXStart = tickXEnd;
                    tickXEnd += tickWidth;
                    tickYStart = tickYEnd;
                }
            }
            
        }

        @Override
        public void pingUpdate() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    PopGraph.this.repaint();
                }
            });
        }
        
    }
    
    public JLabel dLblTotPop;
    public JLabel dLblHealthy;
    public JLabel dLblInfected;
    Agent selectedAgent = null;
    PopGraph popGraph;
    JCheckBox chkbxDrawIncub;
    JCheckBox chkbxAvoidInfect;
    JCheckBox chkbxInfectFollow;
    JCheckBox chkbxShowInfectTree;
    JCheckBox chkbxShowVel;
    JToggleButton tglbtnPlayPause;
    JButton btnReset;
    private JButton btnExit;

    /**
     * Create the panel.
     */
    public ControlPanel(final InfectionGround scape) {
        /////                                0 1 2 3  4    5 6 7 8   9     10   1112131415  16     171819
        setLayout(new MigLayout("", "[][]", "[][][][][30px][][][][][grow][16.00][][][][][][grow 10][][][]"));
        
        JSeparator separator = new JSeparator();
        add(separator, "flowx,cell 0 0 2 1,growx");
        
        JLabel lblStats = new JLabel("Stats:");
        add(lblStats, "cell 0 0 2 1");
        
        JSeparator separator_1 = new JSeparator();
        add(separator_1, "cell 0 0 2 1,growx");
        
        JLabel lblPopulation = new JLabel("Population:");
        add(lblPopulation, "cell 0 1,alignx right");
        
        this.dLblTotPop = new JLabel("");
        add(this.dLblTotPop, "cell 1 1,alignx right");
        
        JLabel lblHealthy = new JLabel("Healthy:");
        add(lblHealthy, "cell 0 2,alignx right");
        
        this.dLblHealthy = new JLabel("");
        add(this.dLblHealthy, "cell 1 2,alignx right");
        
        JLabel lblInfected = new JLabel("Infected:");
        add(lblInfected, "cell 0 3,alignx right");
        
        this.dLblInfected = new JLabel("");
        add(this.dLblInfected, "cell 1 3,alignx right");
        
        this.chkbxDrawIncub = new JCheckBox("Show Incubation");
        this.chkbxDrawIncub.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        scape.setDrawIncubation(ControlPanel.this.chkbxDrawIncub.isSelected());                        
                    }
                });
            }
        });
        
        this.popGraph = new PopGraph(scape);
        scape.population.registerUpdateListener(this.popGraph);
//        JPanel panel = new JPanel();
        add(this.popGraph, "cell 0 4 2 1,grow");
        
        JSeparator separator_2 = new JSeparator();
        add(separator_2, "flowx,cell 0 10 2 1,growx");
        
        JLabel lblControls = new JLabel("Controls:");
        add(lblControls, "cell 0 10 2 1");
        
        JSeparator separator_3 = new JSeparator();
        add(separator_3, "cell 0 10 2 1,growx");
        
        this.chkbxAvoidInfect = new JCheckBox("Avoid Infected");
        
        this.chkbxInfectFollow = new JCheckBox("Infected Follow");
        this.chkbxInfectFollow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scape.population.optFollowHost = ControlPanel.this.chkbxInfectFollow.isSelected();
            }
        });
        
        this.chkbxShowInfectTree = new JCheckBox("Show Infection Tree");
        this.chkbxShowInfectTree.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scape.drawInfectionTree = ControlPanel.this.chkbxShowInfectTree.isSelected();
            }
        });
        add(this.chkbxShowInfectTree, "cell 0 11 2 1");
        add(this.chkbxAvoidInfect, "cell 0 12 2 1");
        add(this.chkbxInfectFollow, "cell 0 13 2 1");
        add(this.chkbxDrawIncub, "cell 0 14 2 1");
        
        this.chkbxShowVel = new JCheckBox("Show Velocity Trails");
        this.chkbxShowVel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        scape.setDrawVelocity(ControlPanel.this.chkbxShowVel.isSelected());                        
                    }
                });
            }
        });
        add(this.chkbxShowVel, "cell 0 15 2 1");
        
        this.tglbtnPlayPause = new JToggleButton("Pause");
        this.tglbtnPlayPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        boolean sel = ControlPanel.this.tglbtnPlayPause.isSelected();
                        scape.pausePlay(sel);
                        ControlPanel.this.tglbtnPlayPause.setText(sel ? "Play" : "Pause");
                    }
                });
            }
        });
        add(this.tglbtnPlayPause, "cell 0 17 2 1,growx");
        
        this.btnReset = new JButton("Restart");
        this.btnReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ControlPanel.this.selectedAgent = null;
                ControlPanel.this.chkbxAvoidInfect.setSelected(false);
                ControlPanel.this.chkbxDrawIncub.setSelected(false);
                ControlPanel.this.chkbxInfectFollow.setSelected(false);
                ControlPanel.this.chkbxShowInfectTree.setSelected(false);
                ControlPanel.this.chkbxShowVel.setSelected(false);
                ControlPanel.this.selectedAgent = null;
                scape.restart();
                scape.population.registerUpdateListener(ControlPanel.this.popGraph);
                scape.population.pingUpdate();
            }
        });
        add(this.btnReset, "cell 0 18 2 1, growx");
        
        
        this.btnExit = new JButton("Exit");
        this.btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scape.runSim = false;
            }
        });
        add(this.btnExit, "cell 0 19 2 1, growx");
        
    }

    public void clickedAgent(Agent clickedAgent) {
        if (this.selectedAgent != null) {
            this.selectedAgent.selected = false;
        }
        if (clickedAgent == null || (this.selectedAgent != null && this.selectedAgent.equals(clickedAgent))) {
            this.selectedAgent = clickedAgent;
            return;
        }
        this.selectedAgent = clickedAgent;
        clickedAgent.selected = !clickedAgent.selected;
    }
    
}
