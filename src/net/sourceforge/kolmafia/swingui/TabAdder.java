package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.request.GenericRequest;

import javax.swing.*;

public class TabAdder implements Runnable
{
    private final String tabName;
    private ChatFrame chatFrame;
    private GenericRequest genericRequest;
    private JTabbedPane jTabbedPane;

    public TabAdder( final String tabName, ChatFrame chatFrame, GenericRequest genericRequest, JTabbedPane jTabbedPane )
    {
        this.tabName = tabName;
        this.chatFrame = chatFrame;
        this.genericRequest = genericRequest;
        this.jTabbedPane = jTabbedPane;
    }

    public void run()
    {
        ChatPanel createdPanel = new ChatPanel( tabName, genericRequest, chatFrame );

        int tabOrder = getTabOrder( tabName );

        int tabCount = jTabbedPane.getTabCount();
        int tabIndex = tabCount;

        for ( int i = 0; i < tabCount; ++i )
        {
            String currentTabName = jTabbedPane.getTitleAt( i ).trim();

            int currentTabOrder = getTabOrder( currentTabName );

            if ( tabOrder < currentTabOrder )
            {
                tabIndex = i;
                break;
            }
        }

        jTabbedPane.insertTab( tabName, null, createdPanel, "", tabIndex );
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
