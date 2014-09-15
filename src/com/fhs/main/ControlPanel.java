package com.fhs.main;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;

public class ControlPanel extends JPanel {
    
    public JLabel dLblTotPop;
    public JLabel dLblHealthy;
    public JLabel dLblInfected;

    /**
     * Create the panel.
     */
    public ControlPanel() {
        setLayout(new MigLayout("", "[][]", "[][][]"));
        
        JLabel lblPopulation = new JLabel("Population:");
        add(lblPopulation, "cell 0 0,alignx right");
        
        this.dLblTotPop = new JLabel("");
        add(this.dLblTotPop, "cell 1 0,alignx right");
        
        JLabel lblHealthy = new JLabel("Healthy:");
        add(lblHealthy, "cell 0 1,alignx right");
        
        this.dLblHealthy = new JLabel("");
        add(this.dLblHealthy, "cell 1 1,alignx right");
        
        JLabel lblInfected = new JLabel("Infected:");
        add(lblInfected, "cell 0 2,alignx right");
        
        this.dLblInfected = new JLabel("");
        add(this.dLblInfected, "cell 1 2,alignx right");
        
    }
    
}
