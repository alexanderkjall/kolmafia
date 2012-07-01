package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.StyledChatBuffer;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.webui.RelayLoader;

import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * An action listener responsible for sending the text contained within the entry panel to the KoL chat server
 * for processing. This listener spawns a new request to the server which then handles everything that's needed.
 */

public class ChatEntryListener extends ThreadedListener
{
    private static final SimpleDateFormat MARK_TIMESTAMP = new SimpleDateFormat( "HH:mm:ss", Locale.US );

    private ChatPanel chatPanel;

    private final ArrayList<String> commandHistory;
    private int lastCommandIndex = 0;
    private String unfinishedLine;

    public ChatEntryListener(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;
        commandHistory = new ArrayList<String>();
    }

    @Override
    protected void execute()
    {
        String message = chatPanel.getEntryField().getText();

        int keyCode = getKeyCode();

        if ( isAction() || keyCode == KeyEvent.VK_ENTER )
        {
            commandHistory.add( message );
            lastCommandIndex = commandHistory.size();
            unfinishedLine = "";

            submitChat( message );
            return;
        }


        if ( keyCode == KeyEvent.VK_UP )
        {
            if ( lastCommandIndex <= 0 )
            {
                return;
            }
            if( lastCommandIndex == commandHistory.size() )
            {
                unfinishedLine = chatPanel.getEntryField().getText();
            }

            chatPanel.getEntryField().setText( commandHistory.get( --lastCommandIndex ) );
        }
        else if ( keyCode == KeyEvent.VK_DOWN )
        {
            if ( lastCommandIndex + 1 >= commandHistory.size() )
            {
                chatPanel.getEntryField().setText( unfinishedLine );
                return;
            }
            chatPanel.getEntryField().setText( commandHistory.get( ++lastCommandIndex ) );
        }
    }

    private void submitChat(String message)
    {
        if ( message.equals( "" ) )
        {
            return;
        }

        chatPanel.getEntryField().setText( "" );

        StyledChatBuffer buffer = ChatManager.getBuffer( chatPanel.getAssociatedContact() );

        if ( message.startsWith( "/clear" ) || message.startsWith( "/cls" ) || message.equals( "clear" ) || message.equals( "cls" ) )
        {
            buffer.clear();
            return;
        }

        if ( message.equals( "/m" ) || message.startsWith( "/mark" ) )
        {
            buffer.append( "<br><hr><center><font size=2>" + MARK_TIMESTAMP.format( new Date() ) + "</font></center><br>" );
            return;
        }

        if ( message.startsWith( "/?" ) || message.startsWith( "/help" ) )
        {
            RelayLoader.openSystemBrowser( "http://www.kingdomofloathing.com/doc.php?topic=chat_commands" );
            return;
        }

        ChatSender.sendMessage( chatPanel.getAssociatedContact(), message, false );
    }
}
