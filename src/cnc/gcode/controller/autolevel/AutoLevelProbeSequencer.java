/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cnc.gcode.controller.autolevel;

import cnc.gcode.controller.CNCCommand;
import cnc.gcode.controller.DatabaseV2;
import cnc.gcode.controller.MyException;
import cnc.gcode.controller.MySwingWorker;
import cnc.gcode.controller.Tools;
import cnc.gcode.controller.communication.ComInterruptException;
import cnc.gcode.controller.communication.Communication;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author j.beuckman
 */
public abstract class AutoLevelProbeSequencer extends MySwingWorker<String,Object> {

    private AutoLevelSystem.Point[] points;
    private boolean         hit     = false;
    private boolean         pos     = false;
    private double          hitvalue= 0;
    
    private ArrayList<CNCCommand> cmds;
    private Integer[] cmdpropeindex;
    private long maxTime;

    
    AutoLevelProbeSequencer(AutoLevelSystem.Point[] _points) {
        //Marlin makes an error (looks like rounding problem with G92 stepcount it much more resulution ...)
        //so probing 1 point twice
        this.points = (new ArrayList<AutoLevelSystem.Point>(){
            {
                AutoLevelSystem.Point[] ps = _points;
                addAll(Arrays.asList(ps));
                add(new AutoLevelSystem.Point(ps[0].getPoint().x, ps[0].getPoint().y));
            }
        }).toArray(new AutoLevelSystem.Point[0]);
    }

    
    @Override
    protected String doInBackground() throws Exception {

        progress(0, "Processing commands");
        Thread.sleep(1000);

        generateGcodes();
        
        maxTime = estimateTime();

        executeCommands();

        return buildReport();
    }

    protected void progress(int progress, String message) {}

    private boolean waitForNextSend() throws Exception {
        //Set Pos back
        while (Communication.isBussy()) 
        {
            if (this.isCancelled()) 
            {
                throw new Exception();
            }

            Thread.sleep(1);
            dopause();
        }
        return false;
    }
    
    public synchronized void zEndstopHit(double value) {
        hit = true;
        trigger();
    }

    public synchronized void addLocationString(double[] value) {
        hitvalue = value[2];
        pos = true;
        trigger();
    }

    
    private void generateGcodes() {
        cmdpropeindex = new Integer[points.length];
        cmds = new ArrayList<>(points.length * 2 + 20);

        //add start Command
        cmds.add(CNCCommand.getALStartCommand());

        //go to safe height
        cmds.add(new CNCCommand("G0 Z" + DatabaseV2.ALSAVEHEIGHT));

        AutoLevelSystem.Point aktpoint = points[0];
        Point2D aktpointPosition = null;
        Point2D lastpos = null;

        while (true)
        {
            if (this.isCancelled())
            {
                return;
            }

            aktpointPosition = aktpoint.getPoint();

            //go to Point
            if (lastpos == null || !lastpos.equals(aktpointPosition))
            {
                cmds.add(new CNCCommand("G0 X" + Tools.dtostr(aktpointPosition.getX()) + " Y" + Tools.dtostr(aktpointPosition.getY())));
            }
            lastpos = aktpointPosition;

            //Prope
            cmdpropeindex[Arrays.asList(points).indexOf(aktpoint)] = cmds.size();

            cmds.add(new CNCCommand(Communication.getProbeCommand()+" Z" + Tools.dtostr(DatabaseV2.ALZERO.getsaved()- DatabaseV2.ALMAXPROBDEPTH.getsaved()) + " F" + DatabaseV2.ALFEEDRATE));

            // --> Set Position + clearance is made after Propping

            //Get next nearest Point:
            double d    = Double.MAX_VALUE;
            AutoLevelSystem.Point newpoint=null;
            for (int i = 0; i < (points.length - 1); i++)
            {
                if (cmdpropeindex[i] == null && aktpointPosition.distance(points[i].getPoint()) < d)
                {
                    newpoint    = points[i];
                    d           = aktpointPosition.distance(points[i].getPoint());
                }
            }
            if (cmdpropeindex[points.length - 1] == null && newpoint == null)
            {
                newpoint = points[points.length - 1];
            }
            if (newpoint == null)
            {
                break;
            }
            aktpoint = newpoint;
        }

        //go to safe height
        cmds.add(new CNCCommand("G0 Z" + DatabaseV2.ALSAVEHEIGHT));        
    }
    
    private long estimateTime() throws MyException {
        //calc time
        CNCCommand.Calchelper c = new CNCCommand.Calchelper();
        for (int i = 0; i < cmds.size(); i++)
        {
            if (this.isCancelled())
            {
                return -1;
            }

            cmds.get(i).calcCommand(c);

            //second command go to save high so no x and y are known => warning can be ignored! 
            if ((cmds.get(i).getState() == CNCCommand.State.ERROR || cmds.get(i).getState() == CNCCommand.State.WARNING) && i > 1 ) 
            {
                throw new MyException("Error or warning state reported. Should not happen :-(");
            }

            //Simulate Clearancemove
            if (Arrays.asList(cmdpropeindex).contains(i))
            {
                double clearanceHeight = DatabaseV2.ALZERO.getsaved() - DatabaseV2.ALMAXPROBDEPTH.getsaved() + DatabaseV2.ALCLEARANCE.getsaved();
                (new CNCCommand("G0 Z" + Tools.dtostr(clearanceHeight))).calcCommand(c);
            }

        }

        return (long)c.seconds;
    }
    
    private void executeCommands() throws Exception {
        //Execute the Commands
        progress(0, Tools.formatDuration(maxTime));

        for (int i=0; i < cmds.size(); i++)
        {
            CNCCommand cmd = cmds.get(i);

            setProgress(100 * i / cmds.size(), "~" + Tools.formatDuration(maxTime - cmd.getSecounds()));

            for (String execute:cmd.execute(new CNCCommand.Transform(0, 0, 0, false, false), false, false))
            {
                while (true)
                {
                    waitForNextSend();
                    hit = false;
                    pos = false;
                    try {
                        Communication.send(execute);
                    }
                    catch (ComInterruptException ex){
                        continue;
                    }
                    break;
                }                            
            }

            if (Arrays.asList(cmdpropeindex).contains(i))
            {
                Communication.send("G30");
                
                //Probing Done waiting for hit:
                if (Communication.isSimulation() == false)
                {
                    waitForTrigger(1000 * 60 * 10); //10min max
                }
                else {
                    waitForTrigger(10);
                    zEndstopHit(0);
                }

                if (hit == false)
                {
                    throw new MyException("Timeout: No end stop hit!");
                }

                //Read Pos
                while (true)
                {
                    waitForNextSend();
                    try {
                        Communication.send(Communication.getReadPostionCommand());
                    }
                    catch (ComInterruptException ex){
                        continue;
                    }
                    break;
                }                            

                if (Communication.isSimulation() == false) {
                    waitForTrigger(1000); //1s max
                } else {
                    double zPosition = (new Random()).nextDouble();
                    double[] positionReport = { 0, 0, zPosition };
                    addLocationString(positionReport);
                }

                if (pos == false)
                {
                    throw new MyException("Timeout: No position report!");
                }

                double thitValue = hitvalue;

                //Save pos
                points[Arrays.asList(cmdpropeindex).indexOf(i)].setValue(thitValue);

                //Reset Z position
                while (true)
                {
                    waitForNextSend();
                    try{
                        // set internal z position
                        Communication.send("G92 Z" + Tools.dtostr(thitValue));
                    }
                    catch(ComInterruptException ex){
                        continue;
                    }
                    break;
                }                            

                //Clearence
                while (true){
                    waitForNextSend();
                    try {
                        String clearance = Tools.dtostr(thitValue + DatabaseV2.ALCLEARANCE.getsaved());
                        Communication.send("G0 Z" + clearance + " F" + DatabaseV2.GOFEEDRATE);
                    }
                    catch (ComInterruptException ex)
                    {
                        continue;
                    }
                    break;
                }                            

                publish(null);                            

            }

        }
    }
    
    private String buildReport() {
        double max      = -Double.MAX_VALUE;
        double min      = Double.MAX_VALUE;
        double sum      = 0;

        double error    = points[0].getValue() - points[points.length - 1].getValue();
        double errorStep = (error / (points.length - 2));

        if (points.length > 2)
        {
            //error correction reconstruct oder
            Integer[] keys = Arrays.copyOf(cmdpropeindex, cmdpropeindex.length); 
            Arrays.sort(keys, 0, keys.length); 
            for (int i = 0; i < points.length - 1; i++)
            {
                int index = Arrays.asList(cmdpropeindex).indexOf(keys[i]);
                points[index].setValue(points[index].getValue() + i * errorStep);
            }
        }

        for (int i = 0; i < points.length - 1; i++)
        {
            double value = points[i].getValue();
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;    
        }

        String message = "Autoleveling Done!"
                        +"\n    average: " + Tools.dtostr(sum/points.length)
                        +"\n    max: "  + Tools.dtostr(max)
                        +"\n    min: "  + Tools.dtostr(min)
                        +"\n    error: " + Tools.dtostr(error);

        return message;
    }

};
