// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;

public class ModulationInput extends InputOutput implements Rebuildable
    {
    // The dial (duh)
    Dial dial;

    // The JLabel which holds the current parameter data
    JLabel data;
    
    public boolean getDrawsStateDot() { return false; }
    
    public JLabel getData() { return data; }
    
    // The underlying Modulation.  This is the Modulation for Sound #0
    Modulation modulation;
        
    // Are we presently highlighting the ModulationInput (in red I guess)?
    boolean highlight;
    
    // The incoming Wire
    ModulationWire incoming;
    
    // The distance the mouse travels to go 0...1
    static final int SCALE = 256;

    JPopupMenu pop;
    
    // revises the data JLabel to reflect the current modulation value
    public void updateText()
        {
        modulation.getSound().getOutput().lock();
        try
            {
            data.setText(" " + modulation.getModulationValueDescription(number));
            }
        finally 
            {
            modulation.getSound().getOutput().unlock();
            }
        }
    
    // Returns the current modulation value
    public double getState()
        {
        double d = 0;
        modulation.getSound().getOutput().lock();
        try
            {
            d = modulation.modulate(number);
            }
        finally 
            {
            modulation.getSound().getOutput().unlock();
            }
        return d;
        }
        
    // Sets the current modulation value to a Constant.
    public void setState(double state)
        {
        // distribute to all sounds
        Output output = modulation.getSound().getOutput();
        output.lock();
        try
            {
            if (incoming != null)  // uh oh, shouldn't happen
                {
                System.err.println("ModulationInput.setState: wire wasn't null");
                disconnect();
                }

            int index = modulation.getSound().findRegistered(modulation);
            if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                {
                return;
                }
            int numSounds = output.getNumSounds();
            for(int i = 0; i < numSounds; i++)
                {
                Sound s = output.getSound(i);
                if (s.getGroup() == Output.PRIMARY_GROUP)
                    {
                    s.getRegistered(index).setModulation(new Constant(state), number);
                    }
                }
            updateText();
            }
        finally 
            {
            output.unlock();
            }
        data.paintImmediately(data.getBounds());
        dial.paintImmediately(dial.getBounds());
        }

        
    public void rebuild()
        {
        Output output = modulation.getSound().getOutput();
        Rack rack = modPanel.getRack();
        output.lock();
        try
            {
            Modulation connection = modulation.getModulation(number);
            int outModNumber = modulation.getModulationIndex(number);

            disconnect();
                        
            // now reconnect
                        
            if (connection instanceof Constant)
                {
                dial.repaint();
                updateText();
                return;  // we're done
                }

            // find output
            int index = modulation.getSound().findRegistered(connection);
            if (index == Sound.NOT_FOUND)
                {
                System.err.println("ModulationInput: Can't find connection!");
                }
            else
                {
                ModulePanel connectionPanel = rack.getModulePanel(index);
                ModulationWire wire = new ModulationWire(rack);
                ModulationOutput outputModulation = connectionPanel.findModulationOutputForIndex(outModNumber);
                wire.setStart(outputModulation);
                wire.setEnd(this);
                outputModulation.attach(this, wire);
                updateText();
                }
            }
        finally 
            {
            output.unlock();
            }
        repaint();
        }


    /** Constructor, given an owning Modulation and ModPanel, plus which unit input number we are in our owner. */
    public ModulationInput(Modulation mod, int number, ModulePanel modPanel)
        {
        modulation = mod;
        this.number = number;
        this.modPanel = modPanel;

        dial = new Dial(Style.MOD_COLOR, this);

        title = new JLabel(" " + modulation.getModulationName(number));
        title.setFont(Style.SMALL_FONT());
        title.addMouseListener(new LabelMouseAdapter());
        
        data = new JLabel(" " + modulation.getModulationValueDescription(number));
        data.setFont(Style.SMALL_FONT());
                
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        JLabel aux = modPanel.getAuxModulationInputTitle(number);
        if (aux != null)
            {
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BorderLayout());
            panel2.add(title, BorderLayout.CENTER);
            panel2.add(aux, BorderLayout.EAST);
            panel.add(panel2, BorderLayout.NORTH);
            }
        else
            {
            panel.add(title, BorderLayout.NORTH);
            }
        panel.add(data, BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
        add(dial, BorderLayout.WEST);

        String[] help = mod.wrapHelp(mod.getModulationHelp());
        if (help != null && help.length > number && help[number] != null)
            {
            dial.setToolTipText(help[number]);
            title.setToolTipText(help[number]);
            data.setToolTipText(help[number]);
            }
        }

    public static final double[] DEFAULT_CONVERSIONS = new double[] { 0.0, 1.0 / 4.0, 1.0 / 3.0, 1.0 / 2.0, 2.0 / 3.0, 3.0 / 4.0, 1.0 };
    public static final String[] DEFAULT_OPTIONS = new String[] { "0", "1/4", "1/3", "1/2", "2/3", "3/4", "1" };
    double[] conversions = DEFAULT_CONVERSIONS;
    String[] options = DEFAULT_OPTIONS;
    
    /** Returns the value corresponding to option elt in the Modulation's pop-up window.  You can override
    	this method; or you can just call setOptionsAndConversions or setOptionsWithDefaultConversions; or
    	you can simply implement the getPopupOptions() and getConversions() methods in Modulation, which
    	will be used instead. */
    public double convert(int elt) 
    	{
    	if (modulation != null)
    		{
    		double conv = modulation.getPopupConversion(number, elt);
    		if (conv != Modulation.NO_POPUP_CONVERSION_IMPLEMENTED)
    			return conv;
    		}
    	return conversions[elt]; 
    	}
    
    /** Returns the options to be displayed in the ModulationInput's pop-up window.  These must correspond to,
    	and thus have an array the same size as, the values from getConversions(), which are the actual values
    	from 0 to 1 which correspond to each option.   You can override this method to return your own options,
    	as long as you also override the method getConversions() to return a corresponding array.  Alternatively
    	you can just call setOptionsAndConversions) or setOptionsWithDefaultConversions(). 
    	Finally, if you implement the methods getPopupOptions() and getPopupConversions() in Modulation,
    	they will be used instead.  */
    public String[] getOptions() 
    	{
    	String[] opt = null;
    	if (modulation != null)
    		 {
    		 opt = modulation.getPopupOptions(number);
    		 }
		 if (opt == null)
			{
			opt = options;
			}
    	return opt; 
    	}
    	
    /*
    public double[] getConversions() 
    	{
    	double[] conv = null;
    	System.err.println(modulation);
    	if (modulation != null)
    		 {
    		 conv = modulation.getPopupConversions(number);
    		 }
		 if (conv == null)
			{
			conv = conversions;
			}
    	return conv; 
    	}
    */
    
    /** Sets the *options* (the items presented in the ModulationInput's pop-up window and their 
    	*conversions* (the numbers from 0.0 to 1.0 which are the values corresponding to them).  These arrays
    	must be the same size.  Alternatively you can override the getOptions() and getConversions() methods if you wish. 
    	This method also rebuilds the popup window.  */
    public void setOptionsAndConversions( String[] options, double[] conversions) { this.options = options; this.conversions = conversions;  pop = getPopupMenu(); }
    /** Sets the *options* (the labels presented in the ModulationInput's pop-up window) corresponding to 
    	the default conversion values from DEFAULT_CONVERSIONS.  The default conversions are 0, 1/4, 1/3, 1/2, 2/3, 3/4, and 1.
    	The options array must thus be DEFAULT_CONVERSIONS.length in size.
    	Alternatively you can override the getOptions() and getConversions() methods if you wish.  
    	This method also rebuilds the popup window. */
    public void setOptionsWithDefaultConversions(String[] options) { setOptionsAndConversions(options, DEFAULT_CONVERSIONS); }
      
    public JPopupMenu getPopupMenu()
        {
        JPopupMenu pop = new JPopupMenu();
        String[] options = getOptions();
        for(int i = 0; i < options.length; i++)
            {
            JMenuItem menu = new JMenuItem(options[i]);
            menu.setFont(Style.SMALL_FONT());
            final int _i = i;
            menu.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)      
                    {
                    double val = convert(_i);
                    if (val >= 0 && val <= 1)
                        setState(val);
                    }       
                });     
            pop.add(menu);
            }    
        return pop; 
        }  
        
    // ModulationInput uses this class rather than a Jack to be a combination Jack and Dial.
    
    class Dial extends JPanel
        {
        ModulationInput modulationInput;
        
        // What's going on?  Is the user changing the dial?
        public static final int STATUS_STATIC = 0;
        public static final int STATUS_DIAL_DYNAMIC = 1;
        int status = STATUS_STATIC;
        Color staticColor;

        // The state when the mouse was pressed 
        double startState;
        // The mouse position when the mouse was pressed 
        int startX;
        int startY;
        
        // Is the mouse pressed?  This is part of a mechanism for dealing with
        // a stupidity in Java: if you PRESS in a widget, it'll be told. But if
        // you then drag elsewhere and RELEASE, the widget is never told.
        boolean mouseDown;
        
        public Dimension getPreferredSize() { return new Dimension(Style.LABELLED_DIAL_WIDTH(), Style.LABELLED_DIAL_WIDTH()); }
        public Dimension getMinimumSize() { return new Dimension(Style.LABELLED_DIAL_WIDTH(), Style.LABELLED_DIAL_WIDTH()); }
                
        /** Returns the actual square within which the Dial's circle
            is drawn. */
        public Rectangle getDrawSquare()
            {
            Insets insets = getInsets();
            Dimension size = getSize();
            int width = size.width - insets.left - insets.right;
            int height = size.height - insets.top - insets.bottom;
                
            // How big do we draw our circle?
            if (width > height)
                {
                // base it on height
                int h = height;
                int w = h;
                int y = insets.top;
                int x = insets.left + (width - w) / 2;
                return new Rectangle(x, y, w, h);
                }
            else
                {
                // base it on width
                int w = width;
                int h = w;
                int x = insets.left;
                int y = insets.top + (height - h) / 2;
                return new Rectangle(x, y, w, h);
                }
            }
                        

        void mouseReleased(MouseEvent e)
            {                
            if (mouseDown)
                {
                mouseDown = false;
                status = STATUS_STATIC;
                repaint();
                if (releaseListener != null)
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                }
            }
 
        double getProposedState(MouseEvent e) { return getProposedState(e, null); }
                
        double getProposedState(MouseEvent e, Rectangle p)
            {
            int py = e.getY();
            if (p != null)
                py -= p.getY();
                                
            int y = -(py - startY);
            int min = 0;
            int max = 1;
            double range = (max - min);
                                        
            double multiplicand = SCALE / range;
                                        
            double proposedState = startState + y / multiplicand;
                        
            if (((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) &&
                    (((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) || 
                    ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK)))
                {
                proposedState = startState + y / multiplicand / 64;
                }
            else if ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK)
                {
                proposedState = startState + y / multiplicand / 16;
                }
            else if (((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK))
                {
                proposedState = startState + y / multiplicand / 4;
                }
            return proposedState;
            }
        
        public Dial(Color staticColor, ModulationInput modulationInput)
            {
            this.staticColor = staticColor;
            this.modulationInput = modulationInput;

            pop = getPopupMenu();

            addMouseWheelListener(new MouseWheelListener()
                {
                public void mouseWheelMoved(MouseWheelEvent e) 
                    {
                    disconnect();
                    double val = getState() - e.getWheelRotation() / 2.0;
                    if (val > 1) val = 1;
                    if (val < 0) val = 0;

                    setState(val);
                    }
                });
        
            addMouseListener(new MouseAdapter()
                {
                public void mouseClicked(MouseEvent e)
                    {
                    if (e.getClickCount() == 1)
                        {
                        disconnect();
                        }
                    else if (e.getClickCount() == 2 && (SwingUtilities.isRightMouseButton(e) || (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK))
                        {
                        final double currentState = getState();
                        final JTextField field = new JTextField("" + currentState, 20);
                        final JLabel equivalent = new JLabel(" = " + modulation.getModulationValueDescription(number));
                        field.addKeyListener(new KeyAdapter()
                            {
                            public void keyTyped(KeyEvent e)
                                {
                                boolean success = true;
                                double d = currentState;
                                try { d = Double.parseDouble(field.getText()); }
                                catch (Exception ex)
                                    {
                                    success = false;
                                    }
                                if (!success || d < 0 || d > 1 || d != d)
                                    {
                                    equivalent.setText("<html><font color=red>Value must be between 0 and 1</font></html>");
                                    }
                                else
                                    {
                                    equivalent.setText(" = " + modulation.getModulationValueDescription(number, d, true));
                                    }
                                }
                            });
                                                        
                        JPanel panel = new JPanel();
                        panel.setLayout(new BorderLayout());
                        panel.add(field, BorderLayout.CENTER);
                        panel.add(equivalent, BorderLayout.SOUTH);
                                                
                        Rack rack = modPanel.getRack();
                        rack.disableMenuBar();
                        int result = JOptionPane.showConfirmDialog(modPanel, panel, "Enter a Precise Value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
                        rack.enableMenuBar();
                        if (result == JOptionPane.OK_OPTION)
                            {
                            double d = 0;
                            try 
                                { 
                                d = Double.parseDouble(field.getText()); 
                                }
                            catch (Exception ex) 
                                { 
                                // ignore
                                }
                                
                            if (d >= 0 && d <= 1)
                                {
                                setState(d);
                                }
                            }
                        }
                    else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
                        {
                        pop.show(Dial.this, Dial.this.getBounds().x,  Dial.this.getBounds().y +  Dial.this.getBounds().height);
                        }
                    }
                        
                public void mousePressed(MouseEvent e)
                    {
                    // this shouldn't cause a race condition
                    if (!(modulation.getModulation(number) instanceof Constant))
                        disconnect();
                        
                    mouseDown = true;
                    startX = e.getX();
                    startY = e.getY();
                    startState = getState();
                    status = STATUS_DIAL_DYNAMIC;
                    repaint();

                    if (releaseListener != null)
                        Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);

                    // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                    // same component that received mouseClicked.  What the ... ? Asinine.
                    // So we create a global event listener which checks for mouseReleased and
                    // calls our own private function.  EVERYONE is going to do this.
                                
                    Toolkit.getDefaultToolkit().addAWTEventListener( releaseListener = new AWTEventListener()
                        {
                        public void eventDispatched(AWTEvent e)
                            {
                            if (e instanceof MouseEvent && e.getID() == MouseEvent.MOUSE_RELEASED)
                                {
                                Dial.this.mouseReleased((MouseEvent)e);
                                }
                            }
                        }, AWTEvent.MOUSE_EVENT_MASK);
                    }
                        
                MouseEvent lastRelease;
                public void mouseReleased(MouseEvent e)
                    {
                    if (e == lastRelease) // we just had this event because we're in the AWT Event Listener.  So we ignore it
                        return;
                    
                    status = STATUS_STATIC;
                    repaint();
                    if (releaseListener != null)
                        Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
                    lastRelease = e;
                    }
                });
                        
            addMouseMotionListener(new MouseMotionAdapter()
                {
                public void mouseDragged(MouseEvent e)
                    {
                    double proposedState = getProposedState(e);
                                        
                    // at present we're just going to use y.  It's confusing to use either y or x.
                    if (getState() != proposedState)
                        {
                        setState(proposedState);
                        }
                    }
                });

            repaint();
            }
        
        AWTEventListener releaseListener = null;
        
        /** Returns the actual square within which the Dial's circle is drawn. */
        public void paintComponent(Graphics g)
            {
            Style.prepareGraphics(g);
                
            Graphics2D graphics = (Graphics2D) g;
                
            Rectangle rect = getBounds();
            rect.x = 0;
            rect.y = 0;
            graphics.setPaint(title.getBackground());
            graphics.fill(rect);
            rect = getDrawSquare();

            // this shouldn't cause a race condition
            if (highlight || !(modulation.getModulation(number) instanceof Constant))
                {
                Ellipse2D.Double e = new Ellipse2D.Double(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH()); 
                        
                if (highlight)
                    {
                    graphics.setPaint(Color.RED);
                    graphics.fill(e);
                    }
                // this shouldn't cause a race condition
                else if (!(modulation.getModulation(number) instanceof Constant))
                    {
                    graphics.setPaint(Color.GREEN);
                    graphics.fill(e);
                    }
                                
                graphics.setPaint(Style.MOD_COLOR);
                graphics.setStroke(Style.DIAL_THIN_STROKE());
                graphics.draw(e);
                }
            else
                {
                graphics.setPaint(Style.MOD_COLOR);
                graphics.setStroke(Style.DIAL_THIN_STROKE());
                Arc2D.Double arc = new Arc2D.Double();
        
                double startAngle = 90 + (270 / 2);
                double interval = -270;
                
                arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), startAngle, interval, Arc2D.OPEN);

                graphics.draw(arc);
                graphics.setStroke(Style.DIAL_THICK_STROKE());
                arc = new Arc2D.Double();
                
                double state = getState();
                double min = 0;
                double max = 1;
                interval = -((state - min) / (double)(max - min) * 265) - 5;

                if (status == STATUS_DIAL_DYNAMIC)
                    {
                    graphics.setPaint(Style.DIAL_DYNAMIC_COLOR());
                    if (state == min)
                        {
                        interval = -5;
                        }
                    else
                        {
                        }
                    }
                else
                    {
                    graphics.setPaint(staticColor);
                    if (state == min)
                        {
                        interval = 0;
                        }
                    else
                        {
                        }
                    }

                arc.setArc(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH(), startAngle, interval, Arc2D.OPEN);            
                graphics.draw(arc);
                }
                
            if (getDrawsStateDot())
                {
                graphics.setPaint(Style.DIAL_DYNAMIC_COLOR());
                graphics.fill(new Ellipse2D.Double(
                        rect.getX() + Style.DIAL_STROKE_WIDTH() * 2, 
                        rect.getY() + Style.DIAL_STROKE_WIDTH() * 2, 
                        rect.getWidth() - Style.DIAL_STROKE_WIDTH() * 4, 
                        rect.getHeight() - Style.DIAL_STROKE_WIDTH() * 4));
                }
            }
        }

    /** Disconnects the ModulationInput from its sole incoming ModulationWire if any. */
    public void disconnect()
        {
        if (incoming != null)
            {
            // delete wire from arraylists

            incoming.getStart().outgoing.remove(incoming);
            modPanel.getRack().removeModulationWire(incoming);
            Output output = modulation.getSound().getOutput();
                
            // disconnect from all sounds
            output.lock();
            try
                {
                int index = modulation.getSound().findRegistered(modulation);
                if (index == Sound.NOT_FOUND)
                    System.err.println("ModulationInput.disconnect: modulation " + modulation + " not found!");
                else
                    {
                    int numSounds = output.getNumSounds();
                    for(int i = 0; i < numSounds; i++)
                        {
                        Sound s = output.getSound(i);
                        if (s.getGroup() == Output.PRIMARY_GROUP)
                            {
                            Modulation a = (Modulation)(s.getRegistered(index));
                            a.restoreModulation(number);
                            }
                        }
                    }
                }
            finally 
                {
                output.unlock();
                }
               
            // eliminate 
            incoming = null;
            updateText();
            modPanel.getRack().repaint();
            }
        }
    public boolean isInput() { return true; }
    public boolean isUnit() { return false; }
    }
