package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.TabbedChatFrame;
import net.sourceforge.kolmafia.swingui.panel.ChatPanel;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.KeyEvent;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ChatEntryListenerTest {

    private ChatPanel panel;
    private ChatEntryListener instance;
    private JTabbedPane tabs;

    @Before
    public void setup()
    {
        GenericRequest profiler = mock(GenericRequest.class);
        TabbedChatFrame chatFrame = mock(TabbedChatFrame.class);
        tabs = new JTabbedPane();

        panel = new ChatPanel("first", profiler, chatFrame, chatFrame.getTabbedPane());
        tabs.insertTab("first", null, panel, "", 0);

        panel = new ChatPanel("second", profiler, chatFrame, chatFrame.getTabbedPane());
        tabs.insertTab("second", null, panel, "", 0);

        instance = new ChatEntryListener(panel, tabs );
    }

    @Test
    public void testChatHistoryBasic() throws InterruptedException {
        String line1 = "line1";
        String line2 = "line2";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER, false, false ), instance );
        panel.getEntryField().setText( line2 );
        sendEvent( getEvent( KeyEvent.VK_ENTER, false, false ), instance );

        assertEquals("check that the enter event clears current line of text", "", panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );

        assertEquals("check that the key up event restores one line back from history", line2, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );

        assertEquals("check that the key up event restores one line back from history", line1, panel.getEntryField().getText());
    }

    @Test
    public void testChatHistory() throws InterruptedException {
        String line1 = "line1";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER, false, false ), instance );

        assertEquals( "check that the enter event clears current line of text", "", panel.getEntryField().getText() );

        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );

        assertEquals("check that the key up event restores one line back from history", line1, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );

        assertEquals("going above the top of history should do nothing", line1, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_DOWN, false, false ), instance );

        assertEquals("check that the key down event empties the line when the history is over", "", panel.getEntryField().getText());
    }

    @Test
    public void testChatHistorytwoLines() throws InterruptedException {
        String line1 = "line1";
        String line2 = "line2";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER, false, false ), instance );
        panel.getEntryField().setText(line2);
        sendEvent( getEvent( KeyEvent.VK_ENTER, false, false ), instance );

        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );
        assertEquals("check that the key up event restores one line back from history", line2, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );
        assertEquals("second up should give first line", line1, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_DOWN, false, false ), instance );
        assertEquals("down gives second line again", line2, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_DOWN, false, false ), instance );
        assertEquals("check that the key down event empties the line when the history is over", "", panel.getEntryField().getText());
    }

    @Test
    public void testChatHistoryRestoreUnsentMessage() throws InterruptedException {
        String line1 = "line1";
        String unsentLine = "unsentLine";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER, false, false ), instance );
        panel.getEntryField().setText( unsentLine );
        sendEvent( getEvent( KeyEvent.VK_UP, false, false ), instance );
        sendEvent( getEvent( KeyEvent.VK_DOWN, false, false ), instance );

        assertEquals("check that the key down event restores the not sent line when", unsentLine, panel.getEntryField().getText());
    }

    @Test
    public void testCtrl1WithTwoTabs() throws InterruptedException {
        tabs.setSelectedIndex(1);
        assertEquals("check that the second tab is selected", 1, tabs.getSelectedIndex());

        sendEvent( getEvent( KeyEvent.VK_1, true, false ), instance );

        assertEquals("check that the first tab is selected", 0, tabs.getSelectedIndex());
    }

    @Test
    public void testCtrl2WithTwoTabs() throws InterruptedException {
        tabs.setSelectedIndex(0);
        assertEquals("check that the first tab is selected", 0, tabs.getSelectedIndex());

        sendEvent( getEvent( KeyEvent.VK_2, true, false ), instance );

        assertEquals("check that the second tab is selected", 1, tabs.getSelectedIndex());
    }

    @Test
    public void testCtrlTabWithTwoTabs() throws InterruptedException {
        tabs.setSelectedIndex(0);
        assertEquals("check that the first tab is selected", 0, tabs.getSelectedIndex());

        sendEvent( getEvent( KeyEvent.VK_TAB, true, false ), instance );

        assertEquals("check that the second tab is selected", 1, tabs.getSelectedIndex());
    }

    @Test
    public void testCtrlShiftTabWithTwoTabs() throws InterruptedException {
        tabs.setSelectedIndex(1);
        assertEquals("check that the second tab is selected", 1, tabs.getSelectedIndex());

        sendEvent( getEvent( KeyEvent.VK_TAB, true, true ), instance );

        assertEquals("check that the first tab is selected", 0, tabs.getSelectedIndex());
    }

    @Test
    public void testCtrlTabWithTwoTabsAroundTheCorner() throws InterruptedException {
        tabs.setSelectedIndex(1);
        assertEquals("check that the second tab is selected", 1, tabs.getSelectedIndex());

        sendEvent( getEvent( KeyEvent.VK_TAB, true, false ), instance );

        assertEquals("check that the first tab is selected", 0, tabs.getSelectedIndex());
    }

    @Test
    public void testCtrlShiftTabWithTwoTabsAroundTheCorner() throws InterruptedException {
        tabs.setSelectedIndex(0);
        assertEquals("check that the first tab is selected", 0, tabs.getSelectedIndex());

        sendEvent( getEvent( KeyEvent.VK_TAB, true, true ), instance );

        assertEquals("check that the second tab is selected", 1, tabs.getSelectedIndex());
    }

    private void sendEvent(KeyEvent event, ChatEntryListener instance) throws InterruptedException {
        instance.keyReleased( event );
        Thread.sleep( 200 );
    }

    private KeyEvent getEvent( int key, boolean ctrl, boolean shift )
    {
        int modifier = 0;
        if(ctrl)
            modifier |= KeyEvent.CTRL_DOWN_MASK;
        if(shift)
            modifier |= KeyEvent.SHIFT_DOWN_MASK;

        return new KeyEvent( panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), modifier, key, KeyEvent.CHAR_UNDEFINED );
    }
}
