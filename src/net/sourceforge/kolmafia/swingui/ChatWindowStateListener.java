package net.sourceforge.kolmafia.swingui;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ChatWindowStateListener extends WindowAdapter
{
    private JTabbedPane pane;

    public ChatWindowStateListener(JTabbedPane pane)
    {
        this.pane = pane;
    }

    @Override
    public void windowGainedFocus(WindowEvent e)
    {
        if( e.getID() == WindowEvent.WINDOW_GAINED_FOCUS )
        {
            int index = pane.getSelectedIndex();
            if( index != -1 )
            {
                pane.getComponentAt( index ).requestFocusInWindow();
            }
        }

        super.windowGainedFocus( e );
    }
}
