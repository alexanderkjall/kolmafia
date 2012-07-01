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

package net.sourceforge.kolmafia.webui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

import net.sourceforge.kolmafia.session.ActionBarManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LeafletManager;
import net.sourceforge.kolmafia.session.ValhallaManager;

import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RelayAgent
	extends Thread
{
	private static final RelayAutoCombatThread COMBAT_THREAD = new RelayAutoCombatThread();
	private static final Map lastModified = new HashMap();

	private static GenericRequest errorRequest = null;
	private static String errorRequestPath = null;

	public static void reset()
	{
		RelayAgent.lastModified.clear();
	}

	public static void setErrorRequest( GenericRequest errorRequest )
	{
		RelayAgent.errorRequest = errorRequest;
		RelayAgent.errorRequestPath = "/" + errorRequest.getPath();
	}

	private final char[] data = new char[ 8192 ];
	private final StringBuffer buffer = new StringBuffer();
	private final PauseObject pauser = new PauseObject();

	private Socket socket = null;
	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private String requestMethod;
	private boolean isCheckingModified;
	private final RelayRequest request;

	public RelayAgent( final int id )
	{
		super( "LocalRelayAgent" + id );
        request = new RelayRequest( true );
	}

	public boolean isWaiting()
	{
		return socket == null;
	}

	public void setSocket( final Socket socket )
	{
		this.socket = socket;
        pauser.unpause();
	}

	@Override
	public void run()
	{
		while ( true )
		{
			if ( socket == null )
			{
                pauser.pause();
			}

			try
			{
                performRelay();
			}
			finally
			{
                closeRelay();
			}
		}
	}

	public void performRelay()
	{
		if ( socket == null )
		{
			return;
		}

        path = null;
        reader = null;
        writer = null;

		try
		{
			if ( !readBrowserRequest() )
			{
				return;
			}

            readServerResponse();
            sendServerResponse();
		}
		catch ( IOException e )
		{
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Horrible relay failure" );
		}
	}

	public boolean readBrowserRequest()
		throws IOException
	{
		boolean debugging = RequestLogger.isDebugging() && Preferences.getBoolean( "logBrowserInteractions" );

        reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

		String requestLine = reader.readLine();

		if ( debugging )
		{
			RequestLogger.updateDebugLog( "-----From Browser-----" );
			RequestLogger.updateDebugLog( requestLine );
		}

		if ( requestLine == null )
		{
			return false;
		}

		int spaceIndex = requestLine.indexOf( " " );

        requestMethod = requestLine.substring( 0, spaceIndex );
		boolean usePostMethod = requestMethod.equals( "POST" );
        path = requestLine.substring( spaceIndex + 1, requestLine.lastIndexOf( " " ) );

		if ( path.startsWith( "//" ) )
		{
			// A current KoL bug causes URLs to gain an unnecessary
			// leading slash after certain chat right-click
			// commands are used.
            path = path.substring( 1 );
		}

        request.constructURLString( path, usePostMethod );

		String currentLine;
		int contentLength = 0;

		String host = null;
		String referer = null;

		while ( ( currentLine = reader.readLine() ) != null && !currentLine.equals( "" ) )
		{
			if ( debugging )
			{
				RequestLogger.updateDebugLog( currentLine );
			}

			if ( currentLine.startsWith( "Host: " ) )
			{
				host = currentLine.substring( 6 );
				continue;
			}

			if ( currentLine.startsWith( "Referer: " ) )
			{
				referer = currentLine.substring( 9 );
				continue;
			}

			if ( currentLine.startsWith( "If-Modified-Since" ) )
			{
                isCheckingModified = true;
				continue;
			}

			if ( currentLine.startsWith( "Content-Length" ) )
			{
				contentLength = StringUtilities.parseInt( currentLine.substring( 16 ) );
				continue;
			}

			if ( currentLine.startsWith( "User-Agent" ) )
			{
				GenericRequest.saveUserAgent( currentLine.substring( 12 ) );
				continue;
			}

			if ( currentLine.startsWith( "Cookie" ) )
			{
				if ( path.startsWith( "/inventory" ) )
				{
					String[] cookieList = currentLine.substring( 8 ).split( "\\s*;\\s*" );
					for ( int i = 0; i < cookieList.length; ++i )
					{
						if ( cookieList[ i ].startsWith( "inventory" ) )
						{
							GenericRequest.inventoryCookie = cookieList[ i ];
						}
					}
				}
				continue;
			}
		}

		if ( !isValidReferer( host, referer ) )
		{
			RequestLogger.printLine( "Request from bogus referer ignored" );
			RequestLogger.printLine( "Path: \"" + path + "\"" );
			RequestLogger.printLine( "Host: \"" + host + "\"" );
			RequestLogger.printLine( "Referer: \"" + referer + "\"" );

			return false;
		}

		if ( requestMethod.equals( "POST" ) )
		{
			int remaining = contentLength;

			while ( remaining > 0 )
			{
				int current = reader.read( data );
                buffer.append( data, 0, current );
				remaining -= current;
			}

			String fields = buffer.toString();
            buffer.setLength( 0 );

			if ( debugging )
			{
				RequestLogger.updateDebugLog( fields );
			}

            request.addFormFields( fields, true );
		}

		if ( debugging )
		{
			RequestLogger.updateDebugLog( "----------" );
		}

		// Validate supplied password hashes
		String pwd = request.getFormField( "pwd" );
		if ( pwd == null )
		{
			// KoLmafia internal pages use only "pwd"
			if ( path.startsWith( "/KoLmafia" ) )
			{
				RequestLogger.printLine( "Missing password hash" );
				RequestLogger.printLine( "Path: \"" + path + "\"" );
				return false;
			}
			pwd = request.getFormField( "phash" );
		}

		// All other pages need either no password hash
		// or a valid password hash.
		if ( pwd != null && !pwd.equals( GenericRequest.passwordHash ) )
		{
			RequestLogger.printLine( "Password hash mismatch" );
			RequestLogger.printLine( "Path: \"" + path + "\"" );
			return false;
		}

		return true;
	}

	private boolean isValidReferer( String host, String referer )
	{
		if ( host != null )
		{
			validRefererHosts.add( host );
		}

		if ( referer == null || referer.equals( "" ) )
		{
			return true;
		}

		if ( !referer.startsWith( "http://" ) )
		{
			return false;
		}

		int endHostIndex = referer.indexOf( '/', 7 );

		if ( endHostIndex == -1 )
		{
			endHostIndex = referer.length();
		}

		String refererHost = referer.substring( 7, endHostIndex );

		if ( validRefererHosts.contains( refererHost ) )
		{
			return true;
		}

		if ( invalidRefererHosts.contains( refererHost ) )
		{
			return false;
		}

		InetAddress refererAddress = null;

		int endNameIndex = refererHost.indexOf( ':' );

		if ( endNameIndex == -1 )
		{
			endNameIndex = refererHost.length();
		}

		String refererName = refererHost.substring( 0, endNameIndex );

		try
		{
			refererAddress = InetAddress.getByName( refererName );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		if ( refererAddress != null && refererAddress.isLoopbackAddress() )
		{
			validRefererHosts.add( refererHost );
			return true;
		}
		else
		{
			invalidRefererHosts.add( refererHost );
			return false;
		}
	}

	private boolean shouldSendNotModified()
	{
		if ( path.startsWith( "/images" ) )
		{
			return true;
		}

		if ( path.contains( "?" ) )
		{
			return false;
		}

		if ( !path.endsWith( ".js" ) && !path.endsWith( ".html" ) )
		{
			return false;
		}

		if ( RelayAgent.lastModified.containsKey( path ) )
		{
			return true;
		}

		RelayAgent.lastModified.put( path, Boolean.TRUE );
		return false;
	}

	private void readServerResponse()
		throws IOException
	{
		// If not requesting a server-side page, then it is safe
		// to assume that no changes have been made (save time).

		if ( isCheckingModified && shouldSendNotModified() )
		{
            request.pseudoResponse( "HTTP/1.1 304 Not Modified", "" );
            request.responseCode = 304;
            request.rawByteBuffer = request.responseText.getBytes( "UTF-8" );

			return;
		}

		if ( errorRequest != null )
		{
			if ( path.startsWith( "/main.php" ) )
			{
                request.pseudoResponse( "HTTP/1.1 302 Found", RelayAgent.errorRequestPath );
				return;
			}

			if ( path.equals( RelayAgent.errorRequestPath ) )
			{
                request.pseudoResponse( "HTTP/1.1 200 OK", errorRequest.responseText );
                request.formatResponse();

				RelayAgent.errorRequest = null;
				RelayAgent.errorRequestPath = null;

				return;
			}
		}

		if ( path.equals( "/fight.php?action=custom" ) )
		{
			RelayAgent.COMBAT_THREAD.wake( null );
            request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( path.equals( "/fight.php?action=script" ) )
		{
			String fightResponse = FightRequest.getNextTrackedRound();
			if ( FightRequest.isTrackingFights() )
			{
				fightResponse = KoLConstants.SCRIPT_PATTERN.matcher( fightResponse ).replaceAll( "" );
                request.headers.add( "Refresh: 1" );
			}
            request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
			RelayRequest.executeAfterAdventureScript();
		}
		else if ( path.equals( "/fight.php?action=abort" ) )
		{
			FightRequest.stopTrackingFights();
            request.pseudoResponse( "HTTP/1.1 200 OK", FightRequest.getNextTrackedRound() );
			RelayRequest.executeAfterAdventureScript();
		}
		else if ( path.startsWith( "/fight.php?hotkey=" ) )
		{
			String hotkey = request.getFormField( "hotkey" );

			if ( hotkey.equals( "11" ) )
			{
				RelayAgent.COMBAT_THREAD.wake( null );
			}
			else
			{
				RelayAgent.COMBAT_THREAD.wake( Preferences.getString( "combatHotkey" + hotkey ) );
			}

            request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( path.equals( "/choice.php?action=auto" ) )
		{
			ChoiceManager.processChoiceAdventure( request, ChoiceManager.lastResponseText );
		}
		else if ( path.equals( "/leaflet.php?action=auto" ) )
		{
            request.pseudoResponse( "HTTP/1.1 200 OK", LeafletManager.leafletWithMagic() );
		}
		else if ( path.startsWith( "/loggedout.php" ) )
		{
            request.pseudoResponse( "HTTP/1.1 200 OK", LogoutRequest.getLastResponse() );
		}
		else if ( path.startsWith( "/actionbar.php" ) )
		{
			ActionBarManager.updateJSONString( request );
		}
		else
		{
			RequestThread.postRequest( request );

			if ( path.startsWith( "/afterlife.php" ) && request.responseCode == 302 )
			{
				if ( path.contains( "asctype=1" ) )
				{
					KoLmafia.resetCounters();
				}
				else
				{
					ValhallaManager.postAscension();
				}
			}
		}
	}

	private void sendServerResponse()
		throws IOException
	{
		if ( request.rawByteBuffer == null )
		{
			if ( request.responseText == null )
			{
				return;
			}

            request.rawByteBuffer = request.responseText.getBytes( "UTF-8" );
		}

        writer = new PrintStream( socket.getOutputStream(), false );
        writer.println( request.statusLine );
        request.printHeaders( writer );
        writer.println();
        writer.write( request.rawByteBuffer );
        writer.flush();

		if ( !RequestLogger.isDebugging() )
		{
			return;
		}

		boolean interactions = Preferences.getBoolean( "logBrowserInteractions" );

		if ( interactions )
		{
			RequestLogger.updateDebugLog( "-----To Browser-----" );
			RequestLogger.updateDebugLog( request.statusLine );
            request.printHeaders( RequestLogger.getDebugStream() );
		}

		if ( Preferences.getBoolean( "logDecoratedResponses" ) )
		{
			String text = request.responseText;
			if ( !Preferences.getBoolean( "logReadableHTML" ) )
			{
				text = KoLConstants.LINE_BREAK_PATTERN.matcher( text ).replaceAll( "" );
			}
			RequestLogger.updateDebugLog( text );
		}

		if ( interactions )
		{
			RequestLogger.updateDebugLog( "----------" );
		}
	}

	private void closeRelay()
	{
		try
		{
			if ( reader != null )
			{
                reader.close();
                reader = null;
			}
		}
		catch ( IOException e )
		{
			// The only time this happens is if the
			// input is already closed.  Ignore.
		}

		if ( writer != null )
		{
            writer.close();
            writer = null;
		}

		try
		{
			if ( socket != null )
			{
                socket.close();
                socket = null;
			}
		}
		catch ( IOException e )
		{
			// The only time this happens is if the
			// socket is already closed.  Ignore.
		}
	}

	private static Set validRefererHosts = new HashSet();
	private static Set invalidRefererHosts = new HashSet();
}
