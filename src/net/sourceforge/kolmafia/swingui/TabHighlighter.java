package net.sourceforge.kolmafia.swingui;

import com.sun.java.forums.CloseableTabbedPane;
import tab.CloseTabbedPane;

import javax.swing.*;

public class TabHighlighter implements Runnable
{
    private final int tabIndex;
    public JTabbedPane tabs;

    public TabHighlighter( JTabbedPane tabs, final int tabIndex )
    {
        this.tabs = tabs;
        this.tabIndex = tabIndex;
    }

    public void run()
    {
        if ( tabs.getSelectedIndex() == this.tabIndex )
        {
            return;
        }

        if ( tabs instanceof CloseTabbedPane )
        {
            ( (CloseTabbedPane) tabs ).highlightTab( this.tabIndex );
        }
        else
        {
            ( (CloseableTabbedPane) tabs ).highlightTab( this.tabIndex );
        }
    }
}
