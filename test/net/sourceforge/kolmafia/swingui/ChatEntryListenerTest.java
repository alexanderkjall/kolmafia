package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.KeyEvent;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ChatEntryListenerTest {

    private ChatPanel panel;
    private ChatEntryListener instance;

    @Before
    public void setup()
    {
        GenericRequest profiler = mock(GenericRequest.class);
        ChatFrame chatFrame = mock(ChatFrame.class);
        panel = new ChatPanel("test", profiler, chatFrame);

        instance = new ChatEntryListener(panel);
    }

    @Test
    public void testChatHistoryBasic() throws InterruptedException {
        String line1 = "line1";
        String line2 = "line2";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER ), instance );
        panel.getEntryField().setText( line2 );
        sendEvent( getEvent( KeyEvent.VK_ENTER ), instance );

        assertEquals("check that the enter event clears current line of text", "", panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP ), instance );

        assertEquals("check that the key up event restores one line back from history", line2, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP ), instance );

        assertEquals("check that the key up event restores one line back from history", line1, panel.getEntryField().getText());
    }

    @Test
    public void testChatHistory() throws InterruptedException {
        String line1 = "line1";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER ), instance );

        assertEquals( "check that the enter event clears current line of text", "", panel.getEntryField().getText() );

        sendEvent( getEvent( KeyEvent.VK_UP ), instance );

        assertEquals("check that the key up event restores one line back from history", line1, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP ), instance );

        assertEquals("going above the top of history should do nothing", line1, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_DOWN ), instance );

        assertEquals("check that the key down event empties the line when the history is over", "", panel.getEntryField().getText());
    }

    @Test
    public void testChatHistorytwoLines() throws InterruptedException {
        String line1 = "line1";
        String line2 = "line2";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER ), instance );
        panel.getEntryField().setText(line2);
        sendEvent( getEvent( KeyEvent.VK_ENTER ), instance );

        sendEvent( getEvent( KeyEvent.VK_UP ), instance );
        assertEquals("check that the key up event restores one line back from history", line2, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_UP ), instance );
        assertEquals("second up should give first line", line1, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_DOWN ), instance );
        assertEquals("down gives second line again", line2, panel.getEntryField().getText());

        sendEvent( getEvent( KeyEvent.VK_DOWN ), instance );
        assertEquals("check that the key down event empties the line when the history is over", "", panel.getEntryField().getText());
    }

    @Test
    public void testChatHistoryRestoreUnsentMessage() throws InterruptedException {
        String line1 = "line1";
        String unsentLine = "unsentLine";

        panel.getEntryField().setText(line1);
        sendEvent( getEvent( KeyEvent.VK_ENTER ), instance );
        panel.getEntryField().setText( unsentLine );
        sendEvent( getEvent( KeyEvent.VK_UP ), instance );
        sendEvent( getEvent( KeyEvent.VK_DOWN ), instance );

        assertEquals("check that the key down event restores the not sent line when", unsentLine, panel.getEntryField().getText());
    }

    private void sendEvent(KeyEvent event, ChatEntryListener instance) throws InterruptedException {
        instance.keyReleased( event );
        Thread.sleep( 200 );
    }

    private KeyEvent getEvent( int key )
    {
        return new KeyEvent( panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED );
    }
}
