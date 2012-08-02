package net.sourceforge.kolmafia.swingui.panel;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.listener.ChatEntryListener;
import net.sourceforge.kolmafia.swingui.listener.ChatLinkClickedListener;
import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Utility method for creating a single panel containing the chat display and the entry area. Note that calling this
 * method changes the <code>RequestPane</code> returned by calling the <code>getChatDisplay()</code> method.
 */

public class ChatPanel
        extends JPanel
        implements FocusListener
{
    private final JTextField entryField;
    private final RequestPane chatDisplay;
    private final String associatedContact;

    public ChatPanel( final String associatedContact, GenericRequest profiler, ChatFrame chatFrame, JTabbedPane tabs )
    {
        super( new BorderLayout() );
        chatDisplay = new RequestPane();
        chatDisplay.addHyperlinkListener( new ChatLinkClickedListener( profiler, chatFrame ) );

        this.associatedContact = associatedContact;

        ChatEntryListener listener = new ChatEntryListener( this, tabs );

        JPanel entryPanel = new JPanel( new BorderLayout() );
        entryField = new JTextField();
        entryField.addKeyListener( listener );

        JButton entryButton = new JButton( "chat" );
        entryButton.addActionListener( listener );

        entryPanel.add( entryField, BorderLayout.CENTER );
        entryPanel.add( entryButton, BorderLayout.EAST );

        ChatBuffer buffer = ChatManager.getBuffer( associatedContact );
        JScrollPane scroller = buffer.addDisplay( chatDisplay );
        scroller.getVerticalScrollBar().addAdjustmentListener( new StickyListener( buffer, chatDisplay, 200 ) );
        add( scroller, BorderLayout.CENTER );

        add( entryPanel, BorderLayout.SOUTH );
        setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( entryField ) );

        addFocusListener( this );
    }

    public void focusGained( FocusEvent e )
    {
        entryField.requestFocusInWindow();
    }

    public void focusLost( FocusEvent e )
    {
    }

    public String getAssociatedContact()
    {
        return associatedContact;
    }

    @Override
    public boolean hasFocus()
    {
        if ( entryField == null || chatDisplay == null )
        {
            return false;
        }

        return entryField.hasFocus() || chatDisplay.hasFocus();
    }

    public JTextField getEntryField() {
        return entryField;
    }
}
