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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.ChatRequest;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatSender
{
	private static final Pattern PRIVATE_MESSAGE_PATTERN =
		Pattern.compile( "^/(?:msg|whisper|w|tell)\\s+(\\S+)\\s+", Pattern.CASE_INSENSITIVE );

	private static boolean scriptedMessagesEnabled = true;

	private static final ArrayList CHANNEL_COMMANDS = new ArrayList();

	static
	{
		ChatSender.CHANNEL_COMMANDS.add( "/em" );
		ChatSender.CHANNEL_COMMANDS.add( "/me" );
		ChatSender.CHANNEL_COMMANDS.add( "/ann" );
	}

	public static void executeMacro( String macro )
	{
		if ( !ChatSender.scriptedMessagesEnabled || !ChatManager.chatLiterate() )
		{
			return;
		}

		ChatRequest request = new ChatRequest( macro );

		List accumulatedMessages = new LinkedList();

		accumulatedMessages.addAll( sendRequest( request ) );

		ChatPoller.addSentEntry( request.responseText, false );

		Iterator messageIterator = accumulatedMessages.iterator();

		while ( messageIterator.hasNext() && ChatSender.scriptedMessagesEnabled )
		{
			ChatMessage message = (ChatMessage) messageIterator.next();

			String recipient = message.getRecipient();

			ChatSender.scriptedMessagesEnabled = recipient == null || recipient.equals( "" ) || recipient.equals( "/clan" ) || recipient.equals( "/hobopolis" ) || recipient.equals( "/slimetube" ) || recipient.equals( "/hauntedhouse" );
		}
	}

	public static void sendMessage( String contact, String message, boolean channelRestricted )
	{
		if ( !ChatManager.chatLiterate() )
		{
			return;
		}

		List grafs = getGrafs( contact, message );

		if ( grafs == null )
		{
			return;
		}

		Iterator grafIterator = grafs.iterator();

		List accumulatedMessages = new LinkedList();

		while ( grafIterator.hasNext() )
		{
			String graf = (String) grafIterator.next();

			String responseText = ChatSender.sendMessage( accumulatedMessages, graf, false, channelRestricted );

			ChatPoller.addSentEntry( responseText, false );
		}
	}

	public static String sendMessage( List accumulatedMessages, String graf, boolean isRelayRequest, boolean channelRestricted )
	{
		if ( !ChatManager.chatLiterate() )
		{
			return "";
		}

		if ( channelRestricted && !ChatSender.scriptedMessagesEnabled )
		{
			return "";
		}

		if ( ChatSender.executeCommand( graf ) )
		{
			return "";
		}

		if ( graf.startsWith( "/examine" ) )
		{
			String item = graf.substring( graf.indexOf( " " ) ).trim();

			AdventureResult result = ItemFinder.getFirstMatchingItem( item, ItemFinder.ANY_MATCH, false );

			if ( result != null )
			{
				ShowDescriptionList.showGameDescription( result );
			}
			else
			{
				EventMessage message = new EventMessage( "Unable to find a unique match for " + item, "green" );
				ChatManager.broadcastEvent( message );
			}

			return "";
		}

		ChatRequest request = new ChatRequest( graf );

		accumulatedMessages.addAll( sendRequest( request ) );

		if ( channelRestricted )
		{
			Iterator messageIterator = accumulatedMessages.iterator();

			while ( messageIterator.hasNext() && ChatSender.scriptedMessagesEnabled )
			{
				ChatMessage message = (ChatMessage) messageIterator.next();

				String recipient = message.getRecipient();

				ChatSender.scriptedMessagesEnabled = recipient == null || recipient.equals( "/clan" ) || recipient.equals( "/hobopolis" ) || recipient.equals( "/slimetube" ) || recipient.equals( "/hauntedhouse" );
			}
		}

		return request.responseText;
	}

	public static List sendRequest( ChatRequest request )
	{
		if ( !ChatManager.chatLiterate() )
		{
			return Collections.EMPTY_LIST;
		}

		RequestThread.postRequest( request );

		if ( request.responseText == null )
		{
			return Collections.EMPTY_LIST;
		}

		List newMessages = new LinkedList();

		String graf = request.getGraf();

		if ( graf.equals( "/listen" ) )
		{
			ChatParser.parseChannelList( newMessages, request.responseText );
		}
		else if ( graf.startsWith( "/l " ) || graf.startsWith( "/listen " ) )
		{
			ChatParser.parseListen( newMessages, request.responseText );
		}
		else if ( graf.startsWith( "/c " ) || graf.startsWith( "/channel " ) )
		{
			ChatParser.parseChannel( newMessages, request.responseText );
		}
		else if ( graf.startsWith( "/s " ) || graf.startsWith( "/switch " ) )
		{
			ChatParser.parseSwitch( newMessages, request.responseText );
		}
		else if ( graf.startsWith( "/who " ) || graf.equals( "/f" ) || graf.equals( "/friends" ) || graf.equals( "/romans" ) || graf.equals( "/clannies" ) )
		{
			ChatParser.parseContacts( newMessages, request.responseText );
		}
		else
		{
			ChatParser.parseLines( newMessages, request.responseText );
		}

		ChatManager.processMessages( newMessages );

		return newMessages;
	}

	private static List getGrafs( String contact, String message )
	{
		List grafs = new LinkedList();

		if ( message.startsWith( "/do " ) || message.startsWith( "/run " ) || message.startsWith( "/cli " ) )
		{
			grafs.add( message );

			return grafs;
		}

		Matcher privateMessageMatcher = ChatSender.PRIVATE_MESSAGE_PATTERN.matcher( message );

		if ( privateMessageMatcher.find() )
		{
			contact = privateMessageMatcher.group( 1 ).trim();
			message = message.substring( privateMessageMatcher.end() ).trim();
		}

		if ( message.length() <= 256 || contact == null || contact.equals( "/clan" ) || message.contains( " && " ) )
		{
			String graf = ChatSender.getGraf( contact, message );

			if ( graf != null )
			{
				grafs.add( graf );
			}

			return grafs;
		}

		// If the message is too long for one message, then
		// divide it into its component pieces.

		String command = "";
		String splitter = " ";
		String prefix = "... ";
		String suffix = " ...";

		if ( message.startsWith( "/" ) )
		{
			int spaceIndex = message.indexOf( " " );

			if ( spaceIndex != -1 )
			{
				command = message.substring( 0, spaceIndex ).trim();
				message = message.substring( spaceIndex ).trim();
			}
			else
			{
				command = message.trim();
				message = "";
			}
		}

		String graf;

		int maxPiece = 255 - command.length() - suffix.length();

		while ( message.length() > maxPiece )
		{
			int splitPos = message.lastIndexOf( splitter, maxPiece );
			if ( splitPos <= prefix.length() )
			{
				splitPos = maxPiece;
			}

			graf = ChatSender.getGraf( contact, command + " " + message.substring( 0, splitPos ) + suffix );

			if ( graf != null )
			{
				grafs.add( graf );
			}

			message = prefix + message.substring( splitPos + splitter.length() );
		}

		graf = ChatSender.getGraf( contact, command + " " + message );

		if ( graf != null )
		{
			grafs.add( graf );
		}

		return grafs;
	}

	private static String getGraf( String contact, String message )
	{
		String contactId = "[none]";

		if ( contact != null )
		{
			contactId = ContactManager.getPlayerId( contact );
			contactId = StringUtilities.globalStringReplace( contactId, " ", "_" ).trim();
		}

		String graf = message == null ? "" : message.trim();

		if ( graf.startsWith( "/exit" ) )
		{
			// Exiting chat should dispose.  KoLmafia should send the
			// message to be server-friendly.

			if ( ChatManager.isRunning() )
			{
				ChatManager.dispose();
			}

			return null;
		}

		if ( contactId.startsWith( "[" ) )
		{
			// This is a message coming from an aggregated window, so
			// leave it as is.
		}
		else if ( !contact.startsWith( "/" ) && !graf.startsWith( "/" ) )
		{
			// Implied requests for a private message should be wrapped
			// in a /msg block.

			graf = "/msg " + contactId + " " + graf;
		}
		else if ( !graf.startsWith( "/" ) )
		{
			// All non-command messages are directed to a channel.  Append the
			// name of the channel to the beginning of the message so you
			// ensure the message gets there.

			graf = contact + " " + graf;
		}
		else
		{
			int spaceIndex = graf.indexOf( " " );
			String baseCommand = spaceIndex == -1 ? graf.toLowerCase() : graf.substring( 0, spaceIndex ).toLowerCase();

			if ( graf.equals( "/c" ) || graf.equals( "/channel" ) )
			{
				graf = "/channel " + contact.substring( 1 );
			}
			else if ( graf.equals( "/l" ) || graf.equals( "/listen" ) )
			{
				graf = "/listen " + contact.substring( 1 );
			}
			else if ( graf.equals( "/s") || graf.equals( "/switch" ) )
			{
				graf = "/switch " + contact.substring( 1 );
			}
			else if ( graf.equals( "/w" ) || graf.equals( "/who" ) )
			{
				// Attempts to view the /who list use the name of the channel
				// when the user doesn't specify the channel.

				graf = "/who " + contact.substring( 1 );
			}
			else if ( graf.equals( "/whois" ) || graf.equals( "/friend" ) || graf.equals( "/baleet" ) )
			{
				graf = graf + " " + contact;
			}
			else if ( ChatSender.CHANNEL_COMMANDS.contains( baseCommand ) )
			{
				if ( contact.startsWith( "/" ) )
				{
					// Direct the message to a channel by appending the name
					// of the channel to the beginning of the message.
					graf = contact + " " + graf;
				}
				else
				{
					// And if it's a private message
					graf = "/msg " + contact + " " + graf;
				}
			}
		}

		if ( graf.startsWith( "/l " ) || graf.startsWith( "/listen " ) )
		{
			String currentChannel = ChatManager.getCurrentChannel();

			if ( currentChannel != null && graf.endsWith( currentChannel ) )
			{
				return null;
			}
		}

		return graf;
	}

	private static boolean executeCommand( String graf )
	{
		if ( graf == null )
		{
			return false;
		}

		if ( graf.equalsIgnoreCase( "/trivia" ) )
		{
			ChatManager.startTriviaGame();
			return true;
		}

		if ( graf.equalsIgnoreCase( "/endtrivia" ) || graf.equalsIgnoreCase( "/stoptrivia" ) )
		{
			ChatManager.stopTriviaGame();
			return true;
		}

		if ( !graf.startsWith( "/do " ) && !graf.startsWith( "/run " ) && !graf.startsWith( "/cli " ) )
		{
			return false;
		}

		int spaceIndex = graf.indexOf( " " );

		String command = graf.substring( spaceIndex ).trim();
		CommandDisplayFrame.executeCommand( command );
		return true;
	}
}
