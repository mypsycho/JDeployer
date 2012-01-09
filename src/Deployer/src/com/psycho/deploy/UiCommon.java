/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import java.awt.Color;
import java.awt.Component;

import org.mypsycho.text.Localized;
import org.mypsycho.text.TextMap;


/**
 * @author Peransin Nicolas
 */
public class UiCommon extends TextMap {

    private static final Command.State[] STATES = Command.State.values();
    
    Color[] colors = new Color[STATES.length];
    
    
    public UiCommon(Component parent) {
        super(parent);
        setEnumPrefix(EnumPrefix.simple);
    }
    
    public UiCommon(Localized parent) {
        super(parent);
        setEnumPrefix(EnumPrefix.simple);
    }
    
    public String getState(Command cmd) {
        return get(cmd.getState(), cmd.getResult());
    }
    
    public Color getColor(Command cmd) {
        return colors[cmd.getState().ordinal()];
    }
    
    public Color[] getColors() {
        return colors;
    }
    
//  public static final Color ERROR_COLOR       = Color.ORANGE;
//  public static final Color INTERRUPTED_COLOR = Color.RED;
//  public static final Color IDLE_COLOR        = Color.DARK_GRAY;
//  public static final Color RUNNING_COLOR     = new Color(0, 128, 0); // DarkGreen
//  public static final Color FINISHED_COLOR    = null; // Default ?
    
//  public String getStateString() {
//  return getStateString(state, result);
//}
//public static String getStateString(int s, int r) {
//  switch (s) {
//      case IDLE_STATE :
//          return Language.get("Command.Idle");
//      case RUNNING_STATE :
//          return Language.get("Command.Running");
//      case ERROR_STATE :
//          return Language.get("Command.Error");
//      case FINISHED_STATE :
//          return Language.get("Command.Finished") +" (" + r + ")";
//      case INTERRUPTED_STATE :
//          return Language.get("Command.Interrupted");
//  }
//  return "??? : " + s + " : " + r;
//}
//
//public Color getStateColor() {
//  return getStateColor(state);
//}
//public static Color getStateColor(int s) {
//      switch (s) {
//      case IDLE_STATE :        return IDLE_COLOR;
//      case RUNNING_STATE :     return RUNNING_COLOR;
//      case ERROR_STATE :       return ERROR_COLOR;
//      case FINISHED_STATE :    return FINISHED_COLOR;
//      case INTERRUPTED_STATE : return INTERRUPTED_COLOR;
//  }
//  return ERROR_COLOR;
//}

}
