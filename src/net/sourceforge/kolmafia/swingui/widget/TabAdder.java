package net.sourceforge.kolmafia.swingui.widget;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.TabbedChatFrame;
import net.sourceforge.kolmafia.swingui.panel.ChatPanel;

import javax.swing.*;

public class TabAdder implements Runnable
{
    private final String tabName;
    private TabbedChatFrame chatFrame;
    private GenericRequest genericRequest;
    private JTabbedPane tabs;

    public TabAdder( final String tabName, TabbedChatFrame chatFrame, GenericRequest genericRequest, JTabbedPane tabs )
    {
        this.tabName = tabName;
        this.chatFrame = chatFrame;
        this.genericRequest = genericRequest;
        this.tabs = tabs;
    }

    public void run()
    {
        ChatPanel createdPanel = new ChatPanel( tabName, genericRequest, chatFrame, tabs );

        int tabOrder = getTabOrder( tabName );

        int tabCount = tabs.getTabCount();
        int tabIndex = tabCount;

        for ( int i = 0; i < tabCount; ++i )
        {
            String currentTabName = tabs.getTitleAt( i ).trim();

            int currentTabOrder = getTabOrder( currentTabName );

            if ( tabOrder < currentTabOrder )
            {
                tabIndex = i;
                break;
            }
        }

        tabs.insertTab( tabName, null, createdPanel, "", tabIndex );
    }

    private int getTabOrder( final String tabName )
    {
        if ( tabName.startsWith( "[" ) )
        {
            return 2;
        }

        if ( tabName.startsWith( "/" ) )
        {
            return 0;
        }

        return 1;
    }
}
