/**
 * Copyright (c) 2005-2012, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.chat;

import java.awt.Toolkit;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatParser
{
	private static final Pattern TABLECELL_PATTERN = Pattern.compile( "</?[tc].*?>" );
	private static final Pattern PARENTHESIS_PATTERN = Pattern.compile( " \\(.*?\\)" );

	private static final Pattern TITLE_PATTERN =
		Pattern.compile( "<center><b>([^<]+)</b></center>" );

	private static final Pattern WHO_PATTERN =
		Pattern.compile( "<font color='?#?(\\w+)'?[^>]*>(.*?)</font></a>" );

	private static final Pattern PLAYERID_PATTERN =
		Pattern.compile( "showplayer\\.php\\?who\\=([-\\d]+)[\'\"][^>]*?>(.*?)</a>" );

	private static final Pattern SENDER_PATTERN =
		Pattern.compile( "(?:<b>)+<a target=mainpane href=\"showplayer\\.php\\?who=([\\-\\d]+)\">([^<]+)</a>\\:?</b>\\:? (.*)(?:</b>)?" );

	private static final Pattern CHANNEL_LISTEN_PATTERN = Pattern.compile( "&nbsp;&nbsp;(.*?)<br>" );

	public static void parseChannelList( final List chatMessages, final String content )
	{
		Matcher channelMatcher = ChatParser.CHANNEL_LISTEN_PATTERN.matcher( content );

		while ( channelMatcher.find() )
		{
			String channel = channelMatcher.group( 1 );

			boolean isCurrentChannel = false;

			if ( channel.contains( "<b" ) )
			{
				isCurrentChannel = true;
			}

			channel = "/" + KoLConstants.ANYTAG_PATTERN.matcher( channel ).replaceAll( "" ).trim();

			if ( isCurrentChannel )
			{
				chatMessages.add( new EnableMessage( channel, true ) );
			}
			else
			{
				chatMessages.add( new EnableMessage( channel, false ) );
			}
		}
	}

	public static void parseContacts( final List chatMessages, final String content )
	{
		Matcher titleMatcher = TITLE_PATTERN.matcher( content );

		String title = titleMatcher.find() ? titleMatcher.group( 1 ) : "Contacts Online";

		Map contacts = new TreeMap();

		Matcher whoMatcher = WHO_PATTERN.matcher( content );

		while ( whoMatcher.find() )
		{
			String playerName = whoMatcher.group( 2 );
			String color = whoMatcher.group( 1 );
			boolean inChat = color.equals( "black" ) || color.equals( "blue" );

			contacts.put( playerName, inChat ? Boolean.TRUE : Boolean.FALSE );
		}

		String noTableContent = ChatParser.TABLECELL_PATTERN.matcher( content ).replaceAll( "" );
		String spacedContent = StringUtilities.singleStringReplace( noTableContent, "</b>", "</b>&nbsp;" );

		WhoMessage message = new WhoMessage( contacts, spacedContent );
		message.setHidden( Preferences.getBoolean( "useContactsFrame" ) );

		chatMessages.add( message );

		ContactManager.updateContactList( title, contacts );
	}

	public static void parseChannel( final List chatMessages, final String content )
	{
		int startIndex = content.indexOf( ":" ) + 2;
		int dotIndex = content.indexOf( "." );

		String channel = content.substring( startIndex, dotIndex == -1 ? content.length() : dotIndex );

		channel = "/" + KoLConstants.ANYTAG_PATTERN.matcher( channel ).replaceAll( "" );

		if ( content.contains( "You are now talking in channel: " ) )
		{
			String currentChannel = ChatManager.getCurrentChannel();

			chatMessages.add( new EnableMessage( channel, true ) );
			chatMessages.add( new DisableMessage( currentChannel, true ) );
		}
	}

	public static void parseSwitch( final List chatMessages, final String content )
	{
		int startIndex = content.indexOf( ":" ) + 2;
		int dotIndex = content.indexOf( "." );

		String channel = content.substring( startIndex, dotIndex == -1 ? content.length() : dotIndex );

		channel = "/" + KoLConstants.ANYTAG_PATTERN.matcher( channel ).replaceAll( "" );

		if ( content.contains( "You are now talking in channel: " ) )
		{
			chatMessages.add( new EnableMessage( channel, true ) );
		}
	}

	public static void parseListen( final List chatMessages, final String content )
	{
		int startIndex = content.indexOf( ":" ) + 2;
		int dotIndex = content.indexOf( "." );

		String channel = content.substring( startIndex, dotIndex == -1 ? content.length() : dotIndex );

		channel = "/" + KoLConstants.ANYTAG_PATTERN.matcher( channel ).replaceAll( "" );

		ChatMessage message = null;

		if ( content.contains( "Now listening to channel: " ) )
		{
			message = new EnableMessage( channel, false );
		}
		else if ( content.contains( "No longer listening to channel: " ) )
		{
			message = new DisableMessage( channel, false );
		}

		if ( message != null )
		{
			chatMessages.add( message );
		}
	}

	public static void parseLines( final List chatMessages, final String content )
	{
		ChatParser.parsePlayerIds( content );

		// There are no updates if there was a timeout.

		if ( content == null || content.length() == 0 )
		{
			return;
		}

		String[] lines = ChatFormatter.formatInternalMessage( content ).split( "<br>" );

		// Check for /haiku messages.

		int nextLine = 0;

		for ( int i = 0; i < lines.length; i = nextLine )
		{
			if ( lines[ i ] == null || lines[ i ].length() == 0 )
			{
				++nextLine;
				continue;
			}

			StringBuilder currentLineBuilder = new StringBuilder( lines[ i ] );

			while ( ++nextLine < lines.length && !lines[nextLine].contains( "<a" ) )
			{
				if ( lines[ nextLine ] != null && lines[ nextLine ].length() > 0 )
				{
					currentLineBuilder.append( "<br>" + lines[ nextLine ] );
				}
			}

			ChatParser.parseLine( chatMessages, currentLineBuilder.toString().trim() );
		}
	}

	public static void parseLine( final List chatMessages, String line )
	{
		// Empty messages do not need to be processed; therefore,
		// return if one was retrieved.

		if ( line == null )
		{
			return;
		}

		line = StringUtilities.globalStringDelete( line, "Invalid password submitted." ).trim();

		if ( line.length() == 0 )
		{
			return;
		}

		if ( ChatParser.parseChannelMessage( chatMessages, line ) )
		{
			return;
		}

		if ( line.contains( "(private):<" ) )
		{
			ChatParser.parsePrivateReceiveMessage( chatMessages, line );
			return;
		}

		if ( line.contains( "<b>private to" ) )
		{
			ChatParser.parsePrivateSendMessage( chatMessages, line );
			return;
		}

		ChatMessage message = new EventMessage( line, "green" );
		chatMessages.add( message );
	}

	private static boolean parseChannelMessage( final List chatMessages, final String line )
	{
		String channel = null;
		String content = line;

		if ( line.startsWith( "[" ) )
		{
			channel = "/" + line.substring( 1, line.indexOf( "]" ) );
			content = line.substring( channel.length() + 2 );
		}
		else
		{
			channel = ChatManager.getCurrentChannel();
		}

		boolean isAction = false;

		int italicIndex = content.indexOf( "<i>" );
		int spaceIndex = content.indexOf( "> " ) + 1;

		if ( italicIndex != -1 && italicIndex < spaceIndex )
		{
			isAction = true;
			content = StringUtilities.singleStringDelete( content, "<i>" );
			content = content.substring( 0, content.length() - 4 );
		}

		Matcher senderMatcher = ChatParser.SENDER_PATTERN.matcher( content );

		if ( !senderMatcher.lookingAt() )
		{
			return false;
		}

		String playerId = senderMatcher.group( 1 ).trim();
		String playerName = senderMatcher.group( 2 ).trim();
		content = senderMatcher.group( 3 );

		ChatMessage message;

		if ( playerName.equals( "Mod Warning" ) || playerName.equals( "Mod Announcement" ) || playerName.equals( "System Message" ) )
		{
			message = new ModeratorMessage( channel, playerName, playerId, content );
		}
		else
		{
			ContactManager.registerPlayerId( playerName, playerId );
			message = new ChatMessage( playerName, channel, content, isAction );
		}

		chatMessages.add( message );
		return true;
	}

	private static void parsePrivateReceiveMessage( final List chatMessages, final String line )
	{
		String sender = line.substring( 0, line.indexOf( " (" ) );
		sender = KoLConstants.ANYTAG_PATTERN.matcher( sender ).replaceAll( "" );

		String recipient = KoLCharacter.getUserName();

		String content = line.substring( line.indexOf( ":" ) + 9 ).trim();
		if( Preferences.getBoolean( "chatBeep" ) )
		{
			Toolkit.getDefaultToolkit().beep();
		}

		ChatMessage message = new ChatMessage( sender, recipient, content, false );
		chatMessages.add( message );
	}

	private static void parsePrivateSendMessage( final List chatMessages, final String line )
	{
		String sender = KoLCharacter.getUserName();

		String recipient = line.substring( 0, line.indexOf( ":" ) );
		recipient = KoLConstants.ANYTAG_PATTERN.matcher( recipient ).replaceAll( "" ).substring( 11 );

		String content = line.substring( line.indexOf( ":" ) + 1 ).trim();

		ChatMessage message = new ChatMessage( sender, recipient, content, false );
		chatMessages.add( message );
	}

	public static void parsePlayerIds( final String content )
	{
		if ( content == null )
		{
			return;
		}

		Matcher playerMatcher = ChatParser.PLAYERID_PATTERN.matcher( content );

		String playerName, playerId;

		while ( playerMatcher.find() )
		{
			playerName = KoLConstants.ANYTAG_PATTERN.matcher( playerMatcher.group( 2 ) ).replaceAll( "" );
			playerName = ChatParser.PARENTHESIS_PATTERN.matcher( playerName ).replaceAll( "" );
			playerName = playerName.replaceAll( ":", "" );

			playerId = playerMatcher.group( 1 );

			// Handle the new player profile links -- in
			// this case, ignore the registration.

			if ( !playerName.startsWith( "&" ) )
			{
				ContactManager.registerPlayerId( playerName, playerId );
			}
		}
	}
}
