// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import org.json.*;
import flow.utilities.*;

/** 
    A Joystick of width and height 1.0.  Each corner is associated with a modulation output, 
    and the modulation value is set to 1.0 minus the maximum of the x or y distance of the joystick
    from that corner.  The center thus 0.5 for all four modulation outputs.  You can click on to move
    the joystick, or drag it.  You can also record a drag sequence, which is then replayed whenever
    you press a note.
*/

public class Joy extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_GATE_TR = 0;
    //public static final int MOD_REL_TR = 1;


    // X, Y, and time of a movement of the joystick
    class Spot
        {
        public Spot(double x, double y, long time) { this.x = x; this.y = y; this.time = time; }
        public final double x;
        public final double y;
        public final long time;
        public String toString() { return "<" + x + ", " + y + ", " + time + ">"; }
        }
                
    // A sequence (or "recording") of movements.  If null, there is no recording at present.
    Spot[] trajectory = null;
        
    // Where we presently are in playing back a recording of the trajectory
    int pos;
        
    // A temporary variable holding the initial time when the trajectory was recorded or is being
    // played back. This is used to normalize all the timestamps of the Spots in the trajectory 
    // so that the first timestamp is set to 0.
    long startTime;
        
    // Is the note being held down?
    boolean running;
        
    void doGate()
        {
        startTime = getSyncTick(true);          // reset the start time so we record/play properly
        running = true;
        pos = 0;
        looped = false;
        }
                
    public void gate() 
        { 
        super.gate();
        if (isModulationConstant(MOD_GATE_TR))
            {
            doGate();
            }
        }
        
    /*
      void doRelease()
      {
      running = false;
      }
        
      public void release()
      {
      super.release();
      if (isModulationConstant(MOD_REL_TR))
      {
      doRelease();
      }
      }

      public void restart()
      {
      super.restart();
      running = false;
      } 

      public void reset() 
      { 
      super.reset();
      running = false;
      }
    */
        
    boolean loop;
    public boolean getLoop() { return loop; }
    public void setLoop(boolean val) { loop = val; }
    boolean looped;
    
    public static final int OPTION_LOOP = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_LOOP: return getLoop() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_LOOP: setLoop(value != 0); break;
            default: throw new RuntimeException("No such option " + option);
            }
            
        // gotta update the labels for the dials
        ModulePanel pan = getModulePanel();
        if (pan != null) SwingUtilities.invokeLater(new Runnable() { public void run() { pan.repaint(); } });
        }


    public void go()
        {
        super.go();
                
        if (isTriggered(MOD_GATE_TR))
            {
            doGate();
            }
//        else if (isTriggered(MOD_REL_TR))
//            {
//            doRelease();
//            }
            
        // We only play back the recording if we're running and if there IS a recording
        if (running && trajectory != null)
            {
            long currentTime = getSyncTick(true) - startTime;
                
            // Find the most recent trajectory position and advance to there
            while(pos < trajectory.length && trajectory[pos].time < currentTime)
                {
                pos++;
                }
                        
            if (pos >= trajectory.length && loop)
                {
                // loop back around!  We just go to zero rather than trying to find the right delta time difference; it probably won't matter much.
                startTime = getSyncTick(true);
                pos = 0;
                currentTime = 0;
                looped = true;
                }
                                
            // pos now contains the current position, or it is over the length
            if (pos < trajectory.length)
                {
                Spot spot = trajectory[pos];
                if (pos == 0 && !looped)
                    {
                    updateModulation(spot.x, spot.y);
                    }
                else if (pos == 0 && looped)
                    {
                    // let's update with a bit of linear interpolation
                    Spot prevspot = trajectory[trajectory.length - 1];
                    double alpha = (currentTime - prevspot.time) / (double)(spot.time - prevspot.time);
                    updateModulation(spot.x * alpha  + prevspot.x * (1.0 - alpha),
                        spot.y * alpha  + prevspot.y * (1.0 - alpha));
                    }
                else
                    {
                    // let's update with a bit of linear interpolation
                    Spot prevspot = trajectory[pos-1];
                    double alpha = (currentTime - prevspot.time) / (double)(spot.time - prevspot.time);
                    updateModulation(spot.x * alpha  + prevspot.x * (1.0 - alpha),
                        spot.y * alpha  + prevspot.y * (1.0 - alpha));
                    }
                }
            else if (trajectory.length > 0)
                {
                Spot spot = trajectory[trajectory.length - 1];
                updateModulation(spot.x, spot.y);
                running = false;
                }
            }
        }

    public Object clone()
        {
        Joy obj = (Joy)(super.clone());
        if (trajectory != null)
            {
            obj.trajectory = (Spot[])(trajectory.clone());
            }
        return obj;
        }
        
    public Joy(Sound sound) 
        {
        super(sound);
        defineOptions(new String[] { "Loop" }, new String[][] { { "Loop" } } );
        defineModulationOutputs(new String[] { 
                //"\u2192", "\u2191",
                "\u2194", "\u2195", 
                //"\u21C5", "\u21C6",
                "\u2196", "\u2197", "\u2199", "\u2198" }); 
        defineModulations(new Constant[] { new Constant(0) }, new String[] { "On Tr" });
        updateModulation(0,0);
        }
    
    public static final String[] KEYS = { "H", "V", "TL", "TR", "BL", "BR" }; 
    public String getKeyForModulationOutput(int output) 
        { 
        return KEYS[output];
        }

    public static String getName() { return "Joystick"; }

    // Given a joystick position (the joystick runs -1 ... +1 in each direction)
    // set the appropriate modulation values
    public void updateModulation(double xPos, double yPos)
        {
        xPos = (xPos + 1) / 2.0;
        yPos = (yPos + 1) / 2.0;

        setModulationOutput(0, xPos);
        setModulationOutput(1, 1.0 - yPos);
        setModulationOutput(2, Math.min(1.0 - xPos, 1.0 - yPos));
        setModulationOutput(3, Math.min(xPos, 1.0 - yPos));
        setModulationOutput(4, Math.min(1.0 - xPos, yPos));
        setModulationOutput(5, Math.min(xPos, yPos));
        }
    
    // Distribute the provided joystick values (-1 ... +1), updating the modulations on
    // all of the sounds
    void distributeJoystickValues(double xPos, double yPos)
        {
        int index = sound.findRegistered(this);
        Output output = sound.getOutput();
        int numSounds = output.getNumSounds();

        for(int i = 0; i < numSounds; i++)
            {
            Sound s = output.getSound(i);
            if (s.getGroup() == Output.PRIMARY_GROUP)
                {
                Joy joy = (Joy)(s.getRegistered(index));
                joy.updateModulation(xPos, yPos);
                }
            }
        }

    // Distribute the provided trajectory to all the sounds.  The trajectory can be null.
    void distributeTrajectory(Spot[] array)
        {
        int index = sound.findRegistered(this);
        Output output = sound.getOutput();
        int numSounds = output.getNumSounds();

        for(int i = 0; i < numSounds; i++)
            {
            Sound s = output.getSound(i);
            if (s.getGroup() == Output.PRIMARY_GROUP)
                {
                Joy joy = (Joy)(s.getRegistered(index));
                joy.trajectory = array;
                }
            }
        }



    /// A subclass of PushButton which handles recording, changing its internal recording and arming
    /// state appropriately.  The RecordButton has to be here because of stupid Java scoping rules. 
    /// It has backpointers to the joystick and the module panel which must be set before it can be
    /// used, but not in the constructor. Dumb.
    class RecordButton extends PushButton
        {
        public static final int STATE_STOPPED = 0;
        public static final int STATE_WAITING = 1;
        public static final int STATE_RECORDING = 2;
        public int state = STATE_STOPPED;
        public Joystick stick;
        public ModulePanel modulePanel;
                
        public RecordButton()
            {
            super("");
            updateText();
            }
                        
        public void updateText()
            {
            if (state == STATE_STOPPED)
                {
                if (trajectory == null) getButton().setText("Record");
                else getButton().setText("Clear");
                }
            else if (state == STATE_WAITING)
                {
                getButton().setText("Cancel");
                }
            else if (state == STATE_RECORDING)
                {
                getButton().setText("Recording");
                }
            }
                        
        public void perform()
            {
            if (state == STATE_STOPPED)
                {
                if (trajectory == null)
                    {
                    state = STATE_WAITING;
                    stick.unsetColor = Color.GREEN.darker();
                    stick.repaint();
                    }
                else                            // CLEAR
                    {
                    modulePanel.getRack().disableMenuBar();
                    boolean ret = (JOptionPane.showConfirmDialog(modulePanel.getRack(), "Clear the recording?", "Clear Recording",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.OK_OPTION);
                    modulePanel.getRack().enableMenuBar();
                    if (ret)
                        {
                        trajectory = null;
                        distributeTrajectory(trajectory);
                        }
                    }
                }
            else if (state == STATE_WAITING || 
                state == STATE_RECORDING)               // this shouldn't be possible to press
                {
                state = STATE_STOPPED;
                stick.unsetColor = Color.BLUE;
                stick.repaint();
                }
            updateText();
            }
        }


    public ModulePanel getPanel()
        {       
        final RecordButton recordButton = new RecordButton();
        ArrayList<Spot> trajectoryBuilder = new ArrayList<Spot>();
        
        final Joystick joystick = new Joystick(Joy.this)
            {
            public Dimension getPreferredSize() { return new Dimension(300, 300); }

            long firstTick = 0;
            public void updatePosition()
                {
                super.updatePosition();
                
                if (recordButton.state == RecordButton.STATE_WAITING)  /// If we're ARMED and ready to go
                    {
                    // clear out previous trajectory
                    trajectoryBuilder.clear();
                    distributeTrajectory(null);
                        
                    // Start recording
                    recordButton.state = RecordButton.STATE_RECORDING;
                    firstTick = getSyncTick(true);
                    recordButton.updateText();
                        
                    // Add initial joystick position
                    trajectoryBuilder.add(new Spot(xPos, yPos, 0));
                    xPositions = new double[] { xPos, xPos };
                    yPositions = new double[] { yPos, yPos };
                    colors = new Color[] { new Color(0,0,0,0), Color.GREEN.darker() };
                    strings = new String[] { "", "Start" };
                    }
                else if (recordButton.state == RecordButton.STATE_RECORDING)  /// If we're RECORDING
                    {
                    // Add joystick position only if we're at a new tick
                    long currentTick = getSyncTick(true) - firstTick;
                    Spot lastSpot = trajectoryBuilder.get(trajectoryBuilder.size() - 1);
                    if (currentTick > lastSpot.time)          // we have something new
                        trajectoryBuilder.add(new Spot(xPos, yPos, currentTick));
                    }
                else  /// If NOT RECORDING NOR ARMED
                    {
                    // Don't do anything -- we'll just change the modulation below
                    }

                // Update the modulation for EVERYONE
                distributeJoystickValues(xPos, yPos);
                }
            
            
            public void mouseUp()
                {
                if (recordButton.state == RecordButton.STATE_RECORDING)  /// If we're RECORDING and now want to STOP
                    {
                    xPositions = new double[0];
                    yPositions = new double[0];

                    // Update final joystick position if we're at a new tick
                    long currentTick = getSyncTick(true) - firstTick;
                    Spot lastSpot = trajectoryBuilder.get(trajectoryBuilder.size() - 1);
                    if (currentTick > lastSpot.time)          // we have something new
                        trajectoryBuilder.add(new Spot(xPos, yPos, currentTick));
                    else      // revise final spot
                        trajectoryBuilder.set(trajectoryBuilder.size() - 1, new Spot(xPos, yPos, currentTick));

                    // Stop
                    recordButton.state = RecordButton.STATE_STOPPED;
                    recordButton.updateText();
                        
                    // Build and distribute the trajectory
                    trajectory = trajectoryBuilder.toArray(new Spot[0]);
                    distributeTrajectory(trajectory);
                        
                    // Clean up
                    recordButton.updateText();
                    unsetColor = Color.BLUE;
                    repaint();
                    }
                }
            };
            
        recordButton.stick = joystick;
 
       
        ModulePanel p = new ModulePanel(Joy.this)
            {
            public JComponent buildPanel()
                {
                Joy joy = (Joy)(getModulation());
                final JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                                
                JPanel joystickDisclosure = new JPanel();
                joystickDisclosure.setLayout(new BorderLayout());
                joystickDisclosure.add(joystick, BorderLayout.CENTER);                                
                JPanel inner = new JPanel();
                inner.setLayout(new BorderLayout());
                inner.add(recordButton, BorderLayout.WEST);
                recordButton.modulePanel = this;
                joystickDisclosure.add(inner, BorderLayout.SOUTH);

                JLabel disclosureLabel = new JLabel("Show  ");
                disclosureLabel.setFont(Style.SMALL_FONT());
                DisclosurePanel disclosure = new DisclosurePanel(disclosureLabel, joystickDisclosure, null);
                panel.add(disclosure, BorderLayout.CENTER);

                Box vert = new Box(BoxLayout.Y_AXIS);
                Box horiz = new Box(BoxLayout.X_AXIS);
                horiz.add(new ModulationOutput(joy, 0, this));
                horiz.add(Strut.makeHorizontalStrut(8));
                horiz.add(new ModulationOutput(joy, 1, this));
                vert.add(horiz);
                vert.add(Strut.makeVerticalStrut(8));
                horiz = new Box(BoxLayout.X_AXIS);
                horiz.add(new ModulationOutput(joy, 2, this));
                horiz.add(Strut.makeHorizontalStrut(8));
                horiz.add(new ModulationOutput(joy, 3, this));
                vert.add(horiz);
                horiz = new Box(BoxLayout.X_AXIS);
                horiz.add(new ModulationOutput(joy, 4, this));
                horiz.add(Strut.makeHorizontalStrut(8));
                horiz.add(new ModulationOutput(joy, 5, this));
                vert.add(horiz);

                JPanel vertPanel = new JPanel();
                vertPanel.setLayout(new BorderLayout());
                vertPanel.add(vert, BorderLayout.EAST);
                
                Box vert2 = new Box(BoxLayout.Y_AXIS);
                vert2.add(new ModulationInput(joy, 0, this));
//                vert2.add(new ModulationInput(joy, 1, this));
                vert2.add(new OptionsChooser(joy, 0));
                JPanel vertPanel2 = new JPanel();
                vertPanel2.setLayout(new BorderLayout());
                vertPanel2.add(vert2, BorderLayout.WEST);
                
                Box vert3 = new Box(BoxLayout.Y_AXIS);
                vert3.add(vertPanel);
                vert3.add(vertPanel2);

                panel.add(vert3, BorderLayout.SOUTH);
                return panel;
                }
            };

        return p;
        }


    //// SERIALIZATION STUFF
    //// We need to write out the trajectory.  We assume that a ZERO LENGTH trajectory is really a NULL trajectory.

    public JSONObject getData()
        {
        JSONObject obj = new JSONObject();
        JSONArray trajx = new JSONArray();
        JSONArray trajy = new JSONArray();
        JSONArray trajtime = new JSONArray();
        if (trajectory != null)
            {
            for(int i = 0; i < trajectory.length; i++)
                {
                trajx.put(i, trajectory[i].x);
                trajy.put(i, trajectory[i].y);
                trajtime.put(i, trajectory[i].time);
                }
            }
        obj.put("trajx", trajx);
        obj.put("trajy", trajy);
        obj.put("trajtime", trajtime);
        return obj;
        }
        
    public void setData(JSONObject data, int moduleVersion, int patchVersion)
        {
        if (data != null)
            {
            JSONArray trajx = data.getJSONArray("trajx");
            JSONArray trajy = data.getJSONArray("trajy");
            JSONArray trajtime = data.getJSONArray("trajtime");
            if (trajx.length() == 0)
                {
                trajectory = null;
                }
            else
                {
                trajectory = new Spot[trajx.length()];
                for(int i = 0; i < trajectory.length; i++)
                    {
                    trajectory[i] = new Spot(trajx.optDouble(i, 0), trajy.optDouble(i, 0), trajtime.optLong(i, 0));
                    }
                }
            }
        } 
    }
    
    
    
    
    
    
    
