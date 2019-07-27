// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import org.json.*;

/**
   A Unit which shifts the frequency of all partials.  There are three
   options:
   
   <ol>
   <li>Pitch.  Shift all partials by a certain pitch (for example, up an octave).
   This multiplies their frequencies by a certain amount.
   <li>Frequency.  Add a certain amount to the frequency of all partials.
   partials based on their distance from the nearest
   <li>Partials.  Move all partials towards the frequency of the next partial.
   </ol>
   
   The degree of shifting depends on the SHIFT modulation, bounded by the
   BOUND modulation.
*/

public class Note extends Modulation implements Miscellaneous
    {
    private static final long serialVersionUID = 1;

    String text = "";
    int width = 0;
    
    public Note(Sound sound) 
        {
        super(sound);
        defineModulationOutputs(new String[] { } );  // no modulation outputs
        }
        
    public static final ImageIcon I_DOWN = iconFor("LeftArrow.png");
    public static final ImageIcon I_DOWN_PRESSED = iconFor("LeftArrowPressed.png");
    //public static final ImageIcon I_BELLY = iconFor("BellyButton.png");
    //public static final ImageIcon I_BELLY_PRESSED = iconFor("BellyButtonPressed.png");
    public static final ImageIcon I_UP = iconFor("RightArrow.png");
    public static final ImageIcon I_UP_PRESSED = iconFor("RightArrowPressed.png");

    static ImageIcon iconFor(String name)
        {
        return new ImageIcon(Note.class.getResource(name));
        }
    
    public void setData(JSONObject data, int moduleVersion, int patchVersion) throws Exception 
        {
        try
            {
            text = data.getString("text");
            }
        catch (Exception ex)
            {
            text = "";
            }

        try
            {
            width = data.getInt("width");
            }
        catch (Exception ex)
            {
            width = 0;
            }
        }  
    
    public JSONObject getData() 
        {
        JSONObject obj = new JSONObject();
        obj.put("text", text);
        obj.put("width", width);
        return obj;
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Note.this)
            {
            JTextArea area;
            
            public boolean getFillPanel() { return true; }
            
            public void updateForSave()
                {
                // this lock/unlock is unnecessary, since I know that we're already locked
                // when updateForSave is called.  But just for some cargo cult programming...
                
                getSound().getOutput().lock();
                try
                    {
                    width = area.getWidth();
                    Document doc = area.getDocument();
                    text = doc.getText(0, doc.getLength());
                    }
                catch (Exception ex)
                    {
                    text = "";
                    }
                finally 
                    {
                    getSound().getOutput().unlock();
                    }
                }
            
            public JComponent buildPanel()
                {               
                area = new JTextArea(text);
                area.setFont(flow.gui.Style.SMALL_FONT());
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                final JScrollPane pane = new JScrollPane(area);
                pane.setMinimumSize(new Dimension(Math.max(1, width), 0));
                pane.setPreferredSize(pane.getMinimumSize());
                final JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(pane, BorderLayout.CENTER);

                JButton left = new JButton(I_DOWN);
                left.setPressedIcon(I_DOWN_PRESSED);
                left.setBorder(null);
                left.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        pane.setMinimumSize(new Dimension(pane.getWidth() / 2, 0));
                        pane.setPreferredSize(pane.getMinimumSize());
                        panel.revalidate();
                        }                                       
                    });

                JButton right = new JButton(I_UP);
                right.setPressedIcon(I_UP_PRESSED);
                right.setBorder(null);
                right.addActionListener(new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        pane.setMinimumSize(new Dimension(pane.getWidth() * 2, 0));
                        pane.setPreferredSize(pane.getMinimumSize());
                        panel.revalidate();
                        }                                       
                    });
                                
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(box.createGlue());
                box.add(left);
                box.add(Strut.makeHorizontalStrut(4));
                box.add(right);
                panel.add(box, BorderLayout.SOUTH);
                return panel;
                }
            };
        }
                        
    }
