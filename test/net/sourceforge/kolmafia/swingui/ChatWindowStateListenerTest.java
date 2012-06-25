package net.sourceforge.kolmafia.swingui;

import org.junit.Test;

import javax.swing.*;

import java.awt.*;
import java.awt.event.WindowEvent;

import static org.mockito.Mockito.*;

public class ChatWindowStateListenerTest {
    @Test
    public void testWindowGainedFocus() throws Exception {
        JTabbedPane pane = new JTabbedPane();

        JPanel first = mock( JPanel.class );
        pane.addTab("first", first);

        JPanel second = mock(JPanel.class);
        pane.addTab("second", second);

        ChatWindowStateListener instance = new ChatWindowStateListener(pane);

        WindowEvent e = new WindowEvent(mock(Window.class), WindowEvent.WINDOW_GAINED_FOCUS);
        instance.windowGainedFocus(e);

        verify(first, times(1)).requestFocusInWindow();
        verify(second, never()).requestFocusInWindow();
    }
}
