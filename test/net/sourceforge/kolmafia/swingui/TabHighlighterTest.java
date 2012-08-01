package net.sourceforge.kolmafia.swingui;

import org.junit.Test;

import javax.swing.*;

public class TabHighlighterTest {
    @Test
    public void testSelectSelected() throws Exception {
        JTabbedPane pane = new JTabbedPane();

        TabHighlighter instance = new TabHighlighter( pane, -1 );

        instance.run();
    }
}
