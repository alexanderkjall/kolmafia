package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.chat.StyledChatBuffer;
import net.sourceforge.kolmafia.swingui.panel.ChatPanel;
import net.sourceforge.kolmafia.webui.RelayLoader;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * An action listener responsible for sending the text contained within the entry panel to the KoL chat server
 * for processing. This listener spawns a new request to the server which then handles everything that's needed.
 */

public class ChatEntryListener extends ThreadedListener
{
    private static final SimpleDateFormat MARK_TIMESTAMP = new SimpleDateFormat( "HH:mm:ss", Locale.US );

    private ChatPanel chatPanel;
    private JTabbedPane tabs;

    private final ArrayList<String> commandHistory;
    private int lastCommandIndex = 0;
    private String unfinishedLine;
    private Map<Integer, Integer> key2tab;

    public ChatEntryListener(ChatPanel chatPanel, JTabbedPane tabs)
    {
        this.chatPanel = chatPanel;
        this.tabs = tabs;
        commandHistory = new ArrayList<String>();

        key2tab = new TreeMap<Integer, Integer>();
        key2tab.put(KeyEvent.VK_1, 0);
        key2tab.put(KeyEvent.VK_2, 1);
        key2tab.put(KeyEvent.VK_3, 2);
        key2tab.put(KeyEvent.VK_4, 3);
        key2tab.put(KeyEvent.VK_5, 4);
        key2tab.put(KeyEvent.VK_6, 5);
        key2tab.put(KeyEvent.VK_7, 6);
        key2tab.put(KeyEvent.VK_8, 7);
        key2tab.put(KeyEvent.VK_9, 8);
        key2tab.put(KeyEvent.VK_0, 9);
    }

    private void selectTab(int keyCode)
    {
        if(hasCtrlModifier() && key2tab.containsKey(keyCode))
        {
            if( tabs.getTabCount() > key2tab.get(keyCode) )
            {
                tabs.setSelectedIndex( key2tab.get(keyCode) );
            }
        }
        else if(hasCtrlModifier() && hasShiftModifier() && keyCode == KeyEvent.VK_TAB)
        {
            int selected = tabs.getSelectedIndex();

            if( selected > 0 )
            {
                tabs.setSelectedIndex( selected - 1 );
            }
            else
            {
                tabs.setSelectedIndex( tabs.getTabCount() - 1 );
            }
        }
        else if(hasCtrlModifier() && keyCode == KeyEvent.VK_TAB)
        {
            int selected = tabs.getSelectedIndex();
            if( tabs.getTabCount() - 1 > selected )
            {
                tabs.setSelectedIndex( selected + 1 );
            }
            else
            {
                tabs.setSelectedIndex( 0 );
            }
        }
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

        selectTab(keyCode);

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
