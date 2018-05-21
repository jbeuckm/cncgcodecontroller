/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller;

import cnc.gcode.controller.communication.Communication;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author patrick
 */
public final class MainForm extends javax.swing.JFrame implements IGUIEvent, ICNCCommandSend{

    private IEvent GUIEvent = null;
    
    /**
     * Creates new form MainForm
     */
    public MainForm() {
        
        //Load Database
        if(DatabaseV2.load(null) == false)
        {
            JOptionPane.showMessageDialog(null,"Could not load settings!");
        }
        
        initComponents();
        JPanelSimpleControls basicControlView = new cnc.gcode.controller.JPanelSimpleControls();
        
        
        //Settings scroll speed
        jSscrollPaneSettings.getVerticalScrollBar().setUnitIncrement(14);
        //GuiUpdateHandler
        final IGUIEvent[] panels = new IGUIEvent[]{this,jPanelAdvancedControl,jPanelAutoLevel1,jPanelCNCMilling,jPanelCommunication,jPanelSettings, jPanelBasicControls, jPanelArt};
        IEvent updateGUI = new IEvent() {
            @Override
            public void fired() {
                boolean running=false;
                for(IGUIEvent panel:panels)
                {
                    if(panel.isRunning()){
                        running=true;
                        break;
                    }
                }
                for(IGUIEvent panel:panels)
                    panel.updateGUI(Communication.isConnected(), running);
            }
        };
        for(IGUIEvent panel:panels)
        {
            panel.setGUIEvent(updateGUI);
            
            if(panel instanceof ICNCCommandSender){
                ((ICNCCommandSender)panel).getCNCCommandSenderResource(this);
            }
        }
        
        
        
        //Show Comports/Speeds avilable (Can take secounds to load!)
        (new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<String> ports = Communication.getPortsNames();
                if(ports.isEmpty())
                {
                    ports.add("No serial port found!");
                }
                
                final ArrayList<Integer> speeds= Communication.getPortsSpeeds();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        jCBPort.setModel(new DefaultComboBoxModel(ports.toArray(new String[0])));
                        jCBSpeed.setModel(new DefaultComboBoxModel(speeds.toArray(new Integer[0])));

                        int index = 0;
                        //Load last Comport   
                        for (String port:ports) 
                        {
                            if(port.equals(DatabaseV2.PORT.get()))
                            {
                                jCBPort.setSelectedIndex(index);
                                break;
                            }
                            index++;
                        }
                        
                        //Load last Speed   
                        index = 0;
                        for (Integer speed:speeds) 
                        {
                            if(speed.toString().equals(DatabaseV2.SPEED.get()))
                            {
                                jCBSpeed.setSelectedIndex(index);
                                break;
                            }
                            index++;
                        }
                        
                        jLStatus.setText(Communication.getStatus());
                    }
                });
            }
        })).start();
        
        Communication.addChangedEvent(new IEvent() {
            @Override
            public void fired() {
                jLStatus.setText(Communication.getStatus());

                fireUpdateGUI();
            }
        });
       
        //First GUI update
        fireUpdateGUI();

    }
    
    @Override
    public void setGUIEvent(IEvent event) {
        GUIEvent    = event;
    }

    @Override
    public void updateGUI(boolean serial, boolean isworking) {
        //Controll      
        jCBPort.setEnabled(!serial);
        jCBSpeed.setEnabled(!serial);
        jBConnect.setEnabled(!isworking || !serial);
        jBConnect.setText(serial?"Disconnect":"Connect");
    }
    
    private void fireUpdateGUI()
    {
        if(GUIEvent == null)
        {
            throw new RuntimeException("GUI EVENT NOT USED!");
        }
        GUIEvent.fired();
    }
    
    @Override
    public void sendCNCSommands(CNCCommand[] commands) {
        SwingUtilities.invokeLater(() -> {
            jTabbedPane.setSelectedComponent(jPanelCNCMilling);
            jPanelCNCMilling.sendCNCCommands(commands);
        });
    }

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane = new javax.swing.JTabbedPane();
        jPanelBasicControls = new cnc.gcode.controller.JPanelSimpleControls();
        jPanelAdvancedControl = new cnc.gcode.controller.JPanelAdvancedControl();
        jPanelAutoLevel1 = new cnc.gcode.controller.autolevel.JPanelAutoLevel();
        jPanelArt = new cnc.gcode.controller.JPanelArt();
        jPanelCNCMilling = new cnc.gcode.controller.JPanelCNCMilling();
        jPanelCommunication = new cnc.gcode.controller.JPanelCommunication();
        jSscrollPaneSettings = new javax.swing.JScrollPane();
        jPanelSettings = new cnc.gcode.controller.JPanelSettings();
        jBConnect = new javax.swing.JButton();
        jLStatus = new javax.swing.JLabel();
        jCBPort = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        jCBSpeed = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CNC-GCode-Controller");
        setName("jframe"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jTabbedPane.setName("JSimpleControlPane"); // NOI18N
        jTabbedPane.addTab("Simple Controls", jPanelBasicControls);
        jTabbedPane.addTab("Advanced Controls", jPanelAdvancedControl);
        jTabbedPane.addTab("Auto Level", jPanelAutoLevel1);
        jTabbedPane.addTab("Art", jPanelArt);
        jTabbedPane.addTab("CNC Milling", jPanelCNCMilling);
        jTabbedPane.addTab("Communication", jPanelCommunication);

        jSscrollPaneSettings.setViewportView(jPanelSettings);

        jTabbedPane.addTab("Settings", jSscrollPaneSettings);

        jBConnect.setText("Connect");
        jBConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBConnectActionPerformed(evt);
            }
        });

        jLStatus.setText("Loading...");

        jLabel6.setText("@");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jCBPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBConnect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1054, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBConnect)
                    .addComponent(jLStatus)
                    .addComponent(jCBPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jCBSpeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        //close Connection
        if(Communication.isConnected())
        {
            Communication.disconnect();
        }
        
        //Save Database
        if(DatabaseV2.save(null) == false)
        {
            JOptionPane.showMessageDialog(this,"Could not save settings!");
        }
        
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    private void jBConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBConnectActionPerformed
        if(Communication.isConnected())
        {
            Communication.disconnect();
            return;
        }

        Communication.connect((String)jCBPort.getModel().getSelectedItem(), (Integer)jCBSpeed.getSelectedItem());
        DatabaseV2.PORT.set((String)jCBPort.getModel().getSelectedItem());
        DatabaseV2.SPEED.set(((Integer)jCBSpeed.getSelectedItem()).toString());
    }//GEN-LAST:event_jBConnectActionPerformed
   

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBConnect;
    private javax.swing.JComboBox jCBPort;
    private javax.swing.JComboBox jCBSpeed;
    private javax.swing.JLabel jLStatus;
    private javax.swing.JLabel jLabel6;
    private cnc.gcode.controller.JPanelAdvancedControl jPanelAdvancedControl;
    private cnc.gcode.controller.JPanelArt jPanelArt;
    private cnc.gcode.controller.autolevel.JPanelAutoLevel jPanelAutoLevel1;
    private cnc.gcode.controller.JPanelSimpleControls jPanelBasicControls;
    private cnc.gcode.controller.JPanelCNCMilling jPanelCNCMilling;
    private cnc.gcode.controller.JPanelCommunication jPanelCommunication;
    private cnc.gcode.controller.JPanelSettings jPanelSettings;
    private javax.swing.JScrollPane jSscrollPaneSettings;
    private javax.swing.JTabbedPane jTabbedPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public boolean isRunning() {
        return false;
    }


    

}
