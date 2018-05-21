/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.autolevel;

import cnc.gcode.controller.DatabaseV2;
import cnc.gcode.controller.IEvent;
import cnc.gcode.controller.IGUIEvent;
import cnc.gcode.controller.JPPaintableEvent;
import cnc.gcode.controller.NumberFieldManipulator;
import cnc.gcode.controller.Tools;
import cnc.gcode.controller.TriggertSwingWorker;
import cnc.gcode.controller.communication.Communication;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author patrick
 */
public class JPanelAutoLevel extends javax.swing.JPanel implements IGUIEvent {

    private IEvent GUIEvent = null;
    
    public AutoLevelSystem al = new AutoLevelSystem();
    
    NumberFieldManipulator[][] axes;
    private AutoLevelProbeSequencer worker   = null;
    private BufferedImage image;
    private AffineTransform trans   = new AffineTransform();
    
    
    private final TriggertSwingWorker<BufferedImage> painter = new TriggertSwingWorker<BufferedImage>() {
        class GetDataSyncedHelper
        {
            private int paintableWidth;
            private int paintableHeight;
            private AutoLevelSystem al;
        }

        @Override
        protected BufferedImage doJob() throws Exception {

            //Load Parameter:
            final GetDataSyncedHelper data = new GetDataSyncedHelper();
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    data.paintableWidth    = jPPaint.getWidth();
                    data.paintableHeight    = jPPaint.getHeight();
                    data.al     = al;
                }
            });        
            
            return AutoLevelPainter.paint(data.al, data.paintableWidth, data.paintableHeight, trans);
        }

        @Override
        protected void process(BufferedImage chunk) {
            image = chunk;

            jPPaint.repaint();
        }

    };


    /**
     * Creates new form JPanelAutoLevel
     */
    public JPanelAutoLevel() {
        initComponents();
        
        NumberFieldManipulator.IChangeEvent axesevent = (NumberFieldManipulator axis) -> {
            double value;
            try {
                value = axis.getd();
            } catch (ParseException ex) {
                axis.popUpToolTip(ex.toString());
                axis.setFocus();
                return;
            }
            
            //Write back Value
            axis.set(value);
            
            //Check Values
            for(int i = 0; i < 2; i++)
            {
                for(NumberFieldManipulator n:axes[i])
                {
                    double v = n.getdsave();
                    if(v < 0)
                    {
                        n.set(0.0);
                        n.setFocus();
                        n.popUpToolTip("Value must be bigger than zero");
                    }
                    if(v > DatabaseV2.getWorkspace(i).getsaved())
                    {
                        n.set(DatabaseV2.getWorkspace(i).getsaved());
                        n.setFocus();
                        n.popUpToolTip("Value must be smaller than " + DatabaseV2.getWorkspace(i));
                    }
                }
                if(axes[i][0].getdsave()>axes[i][1].getdsave())
                {
                    axes[i][1].set(axes[i][0].getdsave());
                    axes[i][1].setFocus();
                    axes[i][1].popUpToolTip("Value must be bigger than start value");
                }
            }

            makeNewAl();
        };
        
        axes = new NumberFieldManipulator[][] {
                                /*0 START*/                                             /*0 END*/
            /*0 X*/ { new NumberFieldManipulator(jTFStartX, axesevent), new NumberFieldManipulator(jTFEndX, axesevent), },
            /*1 Y*/ { new NumberFieldManipulator(jTFStartY, axesevent), new NumberFieldManipulator(jTFEndY, axesevent), },
        };
        
        double autolevelDistance = DatabaseV2.ALDISTANCE.getsaved();
        for (int i = 0; i < 2; i++)
        {
                axes[i][0].set(autolevelDistance / 2);
                axes[i][1].set(DatabaseV2.getWorkspace(i).getsaved() - autolevelDistance / 2);
        }
        makeNewAl();

        
        jPPaint.addPaintEventListener((JPPaintableEvent evt) -> {
            if(image != null)
            {
                evt.getGaraphics().drawImage(image, 0, 0, null);
            }
        });
        
        Communication.addZEndstopHitEvent((double value) -> {
            if(worker != null)
            {
                worker.zEndstopHit(value);
            }
        });        

        Communication.addLoacationStringEvent((double[] value) -> {
            if(worker != null)
            {
                worker.addLocationString(value);
            }
        });        
        
    }

    @Override
    public void setGUIEvent(IEvent event) {
        GUIEvent = event;
    }

    @Override
    public void updateGUI(boolean serial, boolean isworking) 
    {
        boolean rectFieldsEnabled = !isRunning() && !al.isLeveled();
        
        jTFStartX.setEnabled(rectFieldsEnabled);
        jTFStartY.setEnabled(rectFieldsEnabled);
        jTFEndX.setEnabled(rectFieldsEnabled);
        jTFEndY.setEnabled(rectFieldsEnabled);

                             //START                  ABORT           CLEAR
        jBAction.setEnabled((!isworking && serial) || isRunning() || (isLeveled() && !isworking));
        if(isLeveled())
        {
            jBAction.setText("Clear");
        }
        else if(isRunning())
        {
            jBAction.setText("Abort");
        }
        else
        {
            jBAction.setText("Start");
        }
        
        jBPause.setEnabled(isRunning());
        jBPause.setText((isRunning() && worker.isPaused())? "Resume" : "Pause");

        if(isRunning() == false)
        {
            jPBar.setValue(0);
            jPBar.setString("");
        }
        
        jBImport.setEnabled(!isworking);
        jBExport.setEnabled(isLeveled());
        
        painter.trigger();
    }
    
    private void fireupdateGUI()
    {
        if(GUIEvent == null)
        {
            throw new RuntimeException("GUI EVENT NOT USED!");
        }
        GUIEvent.fired();
    }
    
    public boolean isLeveled()
    {
        return al.isLeveled();
    }

    
    @Override
    public boolean isRunning()
    {
        return worker != null && worker.isDone() == false;
    }

    private void makeNewAl() {
        if(isRunning() == false)
        {
            boolean guiupdate = AutoLevelSystem.leveled();
            
            al = new AutoLevelSystem(axes[0][0].getdsave(),
                                    axes[1][0].getdsave(),
                                    axes[0][1].getdsave(),
                                    axes[1][1].getdsave());
            AutoLevelSystem.publish(null);
            
            if(guiupdate)
            {
                fireupdateGUI();
            }
            else
            {
                painter.trigger();
            }
        }
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTFStartX = new javax.swing.JTextField();
        jTFStartY = new javax.swing.JTextField();
        jTFEndX = new javax.swing.JTextField();
        jTFEndY = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jPBar = new javax.swing.JProgressBar();
        jBAction = new javax.swing.JButton();
        jBPause = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPPaint = new cnc.gcode.controller.JPPaintable();
        jPanel4 = new javax.swing.JPanel();
        jBImport = new javax.swing.JButton();
        jBExport = new javax.swing.JButton();

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Positions"));

        jLabel1.setText("X");

        jLabel2.setText("Y");

        jLabel3.setText("Start");

        jLabel4.setText("End");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel3)
                    .addComponent(jTFStartX, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFStartY, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel4)
                    .addComponent(jTFEndX, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFEndY, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTFStartX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jTFEndX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTFStartY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFEndY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Process"));

        jPBar.setString("");
        jPBar.setStringPainted(true);

        jBAction.setText("Start");
        jBAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBActionActionPerformed(evt);
            }
        });

        jBPause.setText("Pause");
        jBPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBPauseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPBar, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jBAction)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jBPause)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPBar, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBAction)
                    .addComponent(jBPause))
                .addGap(0, 17, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));

        jPPaint.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPPaintMouseClicked(evt);
            }
        });
        jPPaint.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                jPPaintComponentResized(evt);
            }
        });

        javax.swing.GroupLayout jPPaintLayout = new javax.swing.GroupLayout(jPPaint);
        jPPaint.setLayout(jPPaintLayout);
        jPPaintLayout.setHorizontalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPPaintLayout.setVerticalGroup(
            jPPaintLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 108, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPPaint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPPaint, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Save Measurement"));

        jBImport.setText("Import");
        jBImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBImportActionPerformed(evt);
            }
        });

        jBExport.setText("Export");
        jBExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBExportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jBImport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jBExport)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jBImport)
                .addComponent(jBExport))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jPPaintComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPPaintComponentResized
        painter.trigger();
    }//GEN-LAST:event_jPPaintComponentResized

    private void jPPaintMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPPaintMouseClicked
        Point2D pos = trans.transform(new Point2D.Double(evt.getX(), evt.getY()), null);
        if(al.isLeveled())
        {
            JOptionPane.showMessageDialog(this, Tools.dtostr(al.getdZ(pos)));
        }
        else
        {
            AutoLevelSystem.Point sp = null;
            double d = Double.MAX_VALUE;
            for(AutoLevelSystem.Point p:al.getPoints())
            {
                if(d > p.getPoint().distance(pos))
                {
                    sp  = p;
                    d   = p.getPoint().distance(pos);
                }
            }
            if(sp != null)
            {
                JOptionPane.showMessageDialog(this, sp.toString());
            }
        }
    }//GEN-LAST:event_jPPaintMouseClicked

    private void jBActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBActionActionPerformed
        if(AutoLevelSystem.leveled())
        {
            makeNewAl();
        }
        else if(isRunning())
        {
            worker.cancel();
            fireupdateGUI();
        }
        else
        {
            //calc commands:
            final AutoLevelSystem al = JPanelAutoLevel.this.al;
            if (al.getPoints().length == 0)
            {
                JOptionPane.showMessageDialog(JPanelAutoLevel.this, "No points to level");
                return;
            }

            worker = new AutoLevelProbeSequencer(al.getPoints()) {
                @Override
                protected void progress(int progress, String message) 
                {
                    jPBar.setValue(progress);
                    jPBar.setString(message);
                }

                @Override
                protected void process(List chunks) {
                    painter.trigger();
                }


                @Override
                protected void done(String rvalue, Exception ex, boolean canceled) {
                    String message = rvalue;

                    if (canceled)
                    {
                        message = "Canceled!";
                    }
                    else if (ex != null)
                    {
                        message = "Error: " + ex.toString();
                        ex.printStackTrace();
                    }

                    JOptionPane.showMessageDialog(JPanelAutoLevel.this, message);
                    AutoLevelSystem.publish(al);

                    fireupdateGUI();
                }
            };
            
            worker.execute();

            fireupdateGUI();
        }
    }//GEN-LAST:event_jBActionActionPerformed

    private void jBPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBPauseActionPerformed
        if(worker != null)
        {
            worker.pause(worker.isPaused() == false);
        }

        fireupdateGUI();
    }//GEN-LAST:event_jBPauseActionPerformed

    private void jBImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBImportActionPerformed
        JFileChooser fc = DatabaseV2.getFileChooser();
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".alf")||f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Autoleveling files (*.alf)";
            }
        });
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);

        if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        
        try {
            try (ObjectInput in = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) 
            {
                al = (AutoLevelSystem)in.readObject();
            }
        }
        catch(Exception e){
            JOptionPane.showMessageDialog(this, "Cannot import file! (" + e.getMessage() + ")");
        }

        AutoLevelSystem.publish(al);

        fireupdateGUI();
    }//GEN-LAST:event_jBImportActionPerformed

    
    private void jBExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBExportActionPerformed
        JFileChooser fc = DatabaseV2.getFileChooser();
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".alf")||f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Autoleveling files (*.alf)";
            }
        });
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);

        if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        
        File f = fc.getSelectedFile();
        if (f.getName().lastIndexOf('.') == -1)
        {
            f = new File(f.getPath() + ".alf");
        }

        
        try {
            if (f.exists() == false)
            {
                f.createNewFile();
            }            
            try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(f))) 
            {
                out.writeObject(al);
            }
        }
        catch (Exception e){
            JOptionPane.showMessageDialog(this, "Cannot export file! (" + e.getMessage() + ")");
        }

    }//GEN-LAST:event_jBExportActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBAction;
    private javax.swing.JButton jBExport;
    private javax.swing.JButton jBImport;
    private javax.swing.JButton jBPause;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JProgressBar jPBar;
    private cnc.gcode.controller.JPPaintable jPPaint;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField jTFEndX;
    private javax.swing.JTextField jTFEndY;
    private javax.swing.JTextField jTFStartX;
    private javax.swing.JTextField jTFStartY;
    // End of variables declaration//GEN-END:variables
}
