/*
 * Copyright (C) 2011 Nicolas Peransin. All rights reserved.
 * Use is subject to license terms.
 */
package com.psycho.deploy;

import org.mypsycho.swing.app.ApplicationListener;
import org.mypsycho.swing.app.SingleFrameApplication;


/**
 * @author Peransin Nicolas
 */
public abstract class CommonApplication extends SingleFrameApplication {

    protected UiCommon texts = new UiCommon(this);
    
    protected CommonApplication() {
        addApplicationListener(ApplicationListener.console);
    }
    
    
    @Override
    protected void initialize(String[] args) {
        getContext().getResourceManager().inject(texts, getLocale());
    }
    
}
