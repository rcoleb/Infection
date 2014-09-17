package com.fhs.main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.JSeparator;
import javax.swing.JRadioButton;

public class ControlPanel extends JPanel {
    
    interface UpdateListener {
        void pingUpdate();
    }
    
    enum GRAPH_TYPE {
        HEALTHY,
        INFECTED
    }
        
    class PopGraph extends JPanel implements UpdateListener {
        
        private static final int NS_INSET = 3;
        private static final int EW_INSET = 0; 
        InfectionGround population;
        GRAPH_TYPE type = GRAPH_TYPE.HEALTHY;
        
        public PopGraph(InfectionGround pop) {
            this.population = pop;
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
            
            ArrayList<Stat> stats = (ArrayList<Stat>) this.population.population.stats.clone();
            
            if (stats.isEmpty()) return;
            g2d.setColor(this.type == GRAPH_TYPE.HEALTHY ? Color.GREEN.darker() : Color.RED.darker());
            float dispHght = bnds.height - NS_INSET - NS_INSET;
            float dispWdth = bnds.width - EW_INSET - EW_INSET;
            
            int maxPop = stats.get(0).pop;
            for (Stat stat : stats) {
                if (stat == null) continue;
                if (stat.pop > maxPop) maxPop = stat.pop;
            }
            int sampleSkip = stats.size() > bnds.width ? stats.size() / bnds.width : 1;
            float tickWidth = stats.size() > bnds.width ? 1 : bnds.width / stats.size();
            float tickXStart = EW_INSET + 1;
            float tickXEnd = tickXStart + tickWidth;
            float tickYStart = NS_INSET + NS_INSET;
            float tickYEnd = tickYStart;
            for (int i = 0; i < stats.size(); i += sampleSkip) {
                Stat stat = stats.get(i);
                float ratio = (this.type == GRAPH_TYPE.HEALTHY ? stat.h : stat.i) / (float)maxPop;
                
                tickYEnd = (NS_INSET + (dispHght - ((dispHght - 1) * ratio)));
                g2d.drawLine((int) tickXStart, (int) tickYStart, (int) tickXEnd, (int) tickYEnd);
                
                tickXStart = tickXEnd;
                tickXEnd += tickWidth;
                tickYStart = tickYEnd;
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
    private Agent selectedAgent = null;

    /**
     * Create the panel.
     */
    public ControlPanel(final InfectionGround scape) {
        setLayout(new MigLayout("", "[][]", "[][][][][30px][][][grow][16.00][][][][][][grow 20]"));
        
        JSeparator separator = new JSeparator();
        add(separator, "flowx,cell 0 0 2 1,growx");
        
        JLabel lblStats = new JLabel("Stats:");
        add(lblStats, "cell 0 0 2 1");
        
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
        
        final JCheckBox chkbxDrawIncub = new JCheckBox("Show Incubation");
        chkbxDrawIncub.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        scape.setDrawIncubation(chkbxDrawIncub.isSelected());                        
                    }
                });
            }
        });
        
        final PopGraph panel = new PopGraph(scape);
        scape.population.registerUpdateListener(panel);
//        JPanel panel = new JPanel();
        add(panel, "cell 0 4 2 1,grow");
        
        JRadioButton rdbtnHealthy = new JRadioButton("Healthy");
        rdbtnHealthy.setSelected(true);
        add(rdbtnHealthy, "cell 0 5");
        
        JRadioButton rdbtnInfected = new JRadioButton("Infected");
        add(rdbtnInfected, "cell 1 5");
        
        rdbtnHealthy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                panel.type = GRAPH_TYPE.HEALTHY;
            }
        });
        rdbtnInfected.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                panel.type = GRAPH_TYPE.INFECTED;
            }
        });
        
        ButtonGroup bg = new ButtonGroup();
        bg.add(rdbtnHealthy);
        bg.add(rdbtnInfected);
        
        JSeparator separator_2 = new JSeparator();
        add(separator_2, "flowx,cell 0 8 2 1,growx");
        
        JLabel lblControls = new JLabel("Controls:");
        add(lblControls, "cell 0 8 2 1");
        
        JCheckBox chkbxAvoidInfect = new JCheckBox("Avoid Infected");
        
        final JCheckBox chkbxInfectFollow = new JCheckBox("Follow Infector");
        chkbxInfectFollow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scape.population.optFollowHost = chkbxInfectFollow.isSelected();
            }
        });
        
        add(chkbxAvoidInfect, "cell 0 9 2 1");
        add(chkbxInfectFollow, "cell 0 10 2 1");
        add(chkbxDrawIncub, "cell 0 11 2 1");
        
        final JToggleButton tglbtnPlayPause = new JToggleButton("Pause");
        tglbtnPlayPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        boolean sel = tglbtnPlayPause.isSelected();
                        scape.pausePlay(sel);
                        tglbtnPlayPause.setText(sel ? "Play" : "Pause");
                    }
                });
            }
        });
        
        final JCheckBox chkbxShowVel = new JCheckBox("Show Velocity Trails");
        chkbxShowVel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        scape.setDrawVelocity(chkbxShowVel.isSelected());                        
                    }
                });
            }
        });
        add(chkbxShowVel, "cell 0 12 2 1");
        add(tglbtnPlayPause, "cell 0 13 2 1,growx");
        
        JSeparator separator_1 = new JSeparator();
        add(separator_1, "cell 0 0 2 1,growx");
        
        JSeparator separator_3 = new JSeparator();
        add(separator_3, "cell 0 8 2 1,growx");
        
    }

    public void clickedAgent(Agent clickedAgent) {
        if (this.selectedAgent != null) {
            this.selectedAgent.selected = false;
            
        }
        if (clickedAgent == null || (this.selectedAgent != null && this.selectedAgent.equals(clickedAgent))) {
            this.selectedAgent = null;
            return;
        }
        this.selectedAgent = clickedAgent;
        clickedAgent.selected = !clickedAgent.selected;
        
    }
    
}
