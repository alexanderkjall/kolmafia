package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.Test;

import java.awt.event.KeyEvent;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ChatEntryListenerTest {

    @Test
    public void testChatHistoryBasic() throws InterruptedException {
        String associatedContact = "test";
        String line1 = "line1";
        String line2 = "line2";
        GenericRequest profiler = mock(GenericRequest.class);
        ChatFrame chatFrame = mock(ChatFrame.class);
        ChatPanel panel = new ChatPanel(associatedContact, profiler, chatFrame);

        ChatEntryListener instance = new ChatEntryListener(panel);

        panel.getEntryField().setText(line1);
        instance.keyReleased( new KeyEvent( panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED ) );
        Thread.sleep(200);
        panel.getEntryField().setText( line2 );
        instance.keyReleased( new KeyEvent( panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED ) );
        Thread.sleep(200);

        String result = panel.getEntryField().getText();
        assertEquals("check that the enter event clears current line of text", "", result);

        instance.keyReleased( new KeyEvent(panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        Thread.sleep(200);

        result = panel.getEntryField().getText();
        assertEquals("check that the key up event restores one line back from history", line2, result);

        instance.keyReleased( new KeyEvent(panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        Thread.sleep(200);

        result = panel.getEntryField().getText();
        assertEquals("check that the key up event restores one line back from history", line1, result);
    }

    @Test
    public void testChatHistory() throws InterruptedException {
        String associatedContact = "test";
        String line1 = "line1";
        GenericRequest profiler = mock(GenericRequest.class);
        ChatFrame chatFrame = mock(ChatFrame.class);
        ChatPanel panel = new ChatPanel(associatedContact, profiler, chatFrame);

        ChatEntryListener instance = new ChatEntryListener(panel);

        panel.getEntryField().setText(line1);
        instance.keyReleased( new KeyEvent( panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED ) );
        Thread.sleep(200);

        String result = panel.getEntryField().getText();
        assertEquals("check that the enter event clears current line of text", "", result);

        instance.keyReleased( new KeyEvent(panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        Thread.sleep(200);

        result = panel.getEntryField().getText();
        assertEquals("check that the key up event restores one line back from history", line1, result);

        instance.keyReleased( new KeyEvent(panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
        Thread.sleep(200);

        result = panel.getEntryField().getText();
        assertEquals("check that the key up event restores one line back from history", line1, result);

        instance.keyReleased( new KeyEvent(panel.getEntryField(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
        Thread.sleep(200);

        result = panel.getEntryField().getText();
        assertEquals("check that the key down event empties the line when the history is over", "", result);
    }
}
