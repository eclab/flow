/***
    Copyright 2017 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package flow.gui;

import java.lang.reflect.*;

// Code for handling MacOS X specific About Menus.  Maybe also we'll handle Preferences later.
//
// Largely cribbed from https://stackoverflow.com/questions/7256230/in-order-to-macify-a-java-app-to-catch-the-about-event-do-i-have-to-implement
//
// I used the reflection version so it compiles cleanly on linux and windows as well.

public class Mac
    {
    public static void setup(Rack rack)
        {
        if (System.getProperty("os.name").contains("Mac")) 
            {
            System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            try 
                {
                Object app = Class.forName("com.apple.eawt.Application").getMethod("getApplication").invoke(null);

                Object al = Proxy.newProxyInstance(
                    Class.forName("com.apple.eawt.AboutHandler").getClassLoader(),
                    new Class[]{Class.forName("com.apple.eawt.AboutHandler")},
                    new AboutListener(rack));

                app.getClass().getMethod("setAboutHandler", Class.forName("com.apple.eawt.AboutHandler")).invoke(app, al);

                al = Proxy.newProxyInstance(
                    Class.forName("com.apple.eawt.QuitHandler").getClassLoader(),
                    new Class[]{Class.forName("com.apple.eawt.QuitHandler")},
                    new QuitListener(rack));

                app.getClass().getMethod("setQuitHandler", Class.forName("com.apple.eawt.QuitHandler")).invoke(app, al);
                }
            catch (Exception e) 
                {
                //fail quietly
                }
            }       
        }
    }
        
class AboutListener implements InvocationHandler 
    {
    Rack rack;
    public AboutListener(Rack rack) { this.rack = rack; }
    public Object invoke(Object proxy, Method method, Object[] args) 
        {
        AppMenu.doAbout();
        return null;
        }
    }

class QuitListener implements InvocationHandler 
    {
    Rack rack;
    public QuitListener(Rack rack) { this.rack = rack; }
    public Object invoke(Object proxy, Method method, Object[] args) 
        {
        rack.doQuit();
        return null;
        }
    }
