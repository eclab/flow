/***
    Copyright 2017 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;


/**
   A simple button with some useful features.  
   
   1. If you construct the button as new PushButton(text), it's just a button 
   which calls perform() when pressed.  Override perform() as you see fit.
      
   2. If you construct the button as new PushButton(text, String[]), pressing
   the button will pop up a menu with the various String[] options, and call
   perform(int) if one of the options is selected.
      
   3. If you construct the button as new PushButton(text, JMenuItem[]), pressing
   the button will pop up a menu with those JMenuItem[] options. 

   @author Sean Luke
*/

public class PushButton extends JPanel
{
    JButton button;
    JPopupMenu pop;
    
    public Insets getInsets() { return new Insets(0,0,0,0); }
    
    /** Returns the underlying JButton. */
    public JButton getButton() { return button; }
    
    /** Creates a PushButton which just calls perform() when pushed. */
    public PushButton(final String text)
    {
        button = new JButton(text);
        button.putClientProperty("JComponent.sizeVariant", "small");
        button.setFont(Style.SMALL_FONT());
        button.setHorizontalAlignment(SwingConstants.CENTER);
                
        button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    _perform();
                }
            });
        setOpaque(false);
        setLayout(new BorderLayout());
        add(button, BorderLayout.CENTER);
    }
    
    /** Creates a PushButton which pops up a menu of options, then calls perform(option)
        when one is selected. */
    public PushButton(String text, String[] options)
    {
        this(text);
        pop = new JPopupMenu();
        for(int i = 0; i < options.length; i++)
            {
                if (options[i] == null)
                    {
                        pop.addSeparator();
                    }
                else
                    {
                        JMenuItem menu = new JMenuItem(options[i]);
                        menu.setFont(Style.SMALL_FONT());
                        final int _i = i;
                        menu.addActionListener(new ActionListener()
                            {
                                public void actionPerformed(ActionEvent e)      
                                {
                                    perform(_i);
                                }       
                            });     
                        pop.add(menu);
                    }
            }
    }
    
    /** Creates a PushButton which pops up a menu of menu items, then calls perform(option)
        when one is selected. */
    public PushButton(String text, JMenuItem[] menuItems)
    {
        this(text);
        pop = new JPopupMenu();
        for(int i = 0; i < menuItems.length; i++)
            {
                if (menuItems[i] == null)
                    pop.addSeparator();
                else
                    pop.add(menuItems[i]);
            }
    }
    
    void _perform()
    {
        if (pop != null)
            {
                button.add(pop);
                if (Style.isMac())
                    {
                        // Mac buttons have strange insets, and only the top and bottom match the
                        // actual border.
                        Insets insets = button.getInsets();
                        pop.show(button, button.getBounds().x + insets.top, button.getBounds().y + button.getBounds().height - insets.bottom);
                    }
                else
                    {
                        pop.show(button, button.getBounds().x, button.getBounds().y + button.getBounds().height);
                    }
                button.remove(pop);
            }
        else
            {
                perform();
            }
    }
    
    /** Called from basic PushButtons when they are pushed.  Override this to respond. */
    public void perform()
    {
    }
        
    /** Called from Popup Menu style PushButtons when a menu option is selected.  Override this to respond. */
    public void perform(int i)
    {
    }
}
