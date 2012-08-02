package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.*;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;

/**
 * Action listener responsible for displaying private message window when a username is clicked, or opening the page
 * in a browser if you're clicking something other than the username.
 */

public class ChatLinkClickedListener extends HyperlinkAdapter
{
    private GenericRequest profiler;
    private ChatFrame chatFrame;

    public ChatLinkClickedListener( GenericRequest profiler, ChatFrame chatFrame )
    {
        this.profiler = profiler;
        this.chatFrame = chatFrame;
    }

    @Override
    public void handleInternalLink( final String location )
    {
        if ( location.startsWith( "makeoffer" ) || location.startsWith( "counteroffer" ) || location.startsWith( "bet" ) || location.startsWith( "messages" ) )
        {
            RelayLoader.openSystemBrowser( location, true );
            return;
        }

        int equalsIndex = location.indexOf( "=" );

        if ( equalsIndex == -1 )
        {
            RelayLoader.openSystemBrowser( location, true );
            return;
        }

        String[] locationSplit = new String[ 2 ];
        locationSplit[ 0 ] = location.substring( 0, equalsIndex );
        locationSplit[ 1 ] = location.substring( equalsIndex + 1 );

        // First, determine the parameters inside of the
        // location which will be passed to frame classes.

        String playerId = locationSplit[ 1 ];
        String playerName = ContactManager.getPlayerName( playerId );

        // Next, determine the option which had been
        // selected in the link-click.

        int linkOption = chatFrame.getNameClickSelect() != null ?
                chatFrame.getNameClickSelect().getSelectedIndex(): 0;

        String urlString = null;

        switch ( linkOption )
        {
            case 1:
                String bufferKey = ChatManager.getBufferKey( playerName );
                ChatManager.openWindow( bufferKey, false );
                return;

            case 2:

                Object[] parameters = new Object[]
                        { playerName
                        };

                GenericFrame.createDisplay( SendMessageFrame.class, parameters );
                return;

            case 3:
                urlString = "makeoffer.php?towho=" + playerId;
                break;

            case 4:
                urlString = "displaycollection.php?who=" + playerId;
                break;

            case 5:
                urlString = "ascensionhistory.php?who=" + playerId;
                break;

            case 6:
                GenericFrame.createDisplay( MallSearchFrame.class );
                MallSearchFrame.searchMall( new MallSearchRequest( StringUtilities.parseInt( playerId ) ) );
                return;

            case 7:
                ChatSender.sendMessage( playerName, "/whois", false );
                return;

            case 8:
                ChatSender.sendMessage( playerName, "/friend", false );
                return;

            case 9:
                ChatSender.sendMessage( playerName, "/baleet", false );
                return;

            default:
                urlString = "showplayer.php?who=" + playerId;
                break;
        }

        if ( Preferences.getBoolean( "chatLinksUseRelay" ) || !urlString.startsWith( "show" ) )
        {
            RelayLoader.openSystemBrowser( urlString );
        }
        else
        {
            ProfileFrame.showRequest( profiler.constructURLString( urlString ) );
        }
    }
}
