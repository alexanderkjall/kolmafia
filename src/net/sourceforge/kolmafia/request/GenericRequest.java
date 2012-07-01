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

package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.chat.InternalMessage;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.OceanManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;

import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.NaiveSecureSocketLayer;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.RelayAgent;
import net.sourceforge.kolmafia.webui.RelayServer;

public class GenericRequest
	implements Runnable
{
	// Used in many requests. Here for convenience and non-duplication
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );
	public static final Pattern PLACE_PATTERN = Pattern.compile( "place=([^&]*)" );

	private int timeoutCount = 0;
	private static final int TIMEOUT_LIMIT = 3;

	public static final Pattern REDIRECT_PATTERN = Pattern.compile( "([^\\/]*)\\/(login\\.php.*)", Pattern.DOTALL );
	public static final Pattern JS_REDIRECT_PATTERN =
		Pattern.compile( ">\\s*top.mainpane.document.location\\s*=\\s*\"(.*?)\";" );

	public static boolean isRatQuest = false;
	public static boolean isBarrelSmash = false;
	public static boolean handlingChoices = false;
	public static boolean ascending = false;
	public static String itemMonster = null;
	public static boolean choiceHandled = true;
	private static boolean suppressUpdate = false;

	protected String encounter = "";

	public static final int MENU_FANCY = 1;
	public static final int MENU_COMPACT = 2;
	public static final int MENU_NORMAL = 3;
	public static int topMenuStyle = 0;

	public static final String[] SERVERS =
	{
		"devproxy.kingdomofloathing.com",
		"www.kingdomofloathing.com"
	};

	public static final String KOL_IP = "69.16.150.211";
	public static String KOL_HOST = GenericRequest.SERVERS[ 1 ];
	public static URL KOL_ROOT = null;
	public static URL KOL_SECURE_ROOT = null;

	private URL formURL;
	private String currentHost;
	private String formURLString;
	private String baseURLString;

	public boolean isChatRequest = false;

	protected List<String> data;
	private boolean dataChanged = true;
	private byte[] dataString = null;

	public boolean containsUpdate;

	public int responseCode;
	public String responseText;
	public HttpURLConnection formConnection;
	public String redirectLocation;

	// Per-login data

	private static String userAgent = "";
	public static String serverCookie = null;
	public static String inventoryCookie = null;
	public static String passwordHash = "";
	public static String passwordHashValue = "";

	public static void reset()
	{
		GenericRequest.setUserAgent();
		GenericRequest.serverCookie = null;
		GenericRequest.inventoryCookie = null;
		GenericRequest.passwordHash = "";
		GenericRequest.passwordHashValue = "";
	}

	public static void setPasswordHash( final String hash )
	{
		GenericRequest.passwordHash = hash;
		GenericRequest.passwordHashValue = "=" + hash;
	}

	/**
	 * static final method called when <code>GenericRequest</code> is first instantiated or whenever the settings have
	 * changed. This initializes the login server to the one stored in the user's settings, as well as initializes the
	 * user's proxy settings.
	 */

	public static void applySettings()
	{
		Properties systemProperties = System.getProperties();

		systemProperties.put( "java.net.preferIPv4Stack", "true" );
		systemProperties.put( "networkaddress.cache.ttl", "true" );

		GenericRequest.applyProxySettings();

		boolean useDevProxyServer = Preferences.getBoolean( "useDevProxyServer" );

		GenericRequest.setLoginServer( GenericRequest.SERVERS[ useDevProxyServer ? 0 : 1 ] );

		if ( Preferences.getBoolean( "allowSocketTimeout" ) )
		{
			systemProperties.put( "sun.net.client.defaultConnectTimeout", "10000" );
			systemProperties.put( "sun.net.client.defaultReadTimeout", "120000" );
		}
		else
		{
			systemProperties.remove( "sun.net.client.defaultConnectTimeout" );
			systemProperties.remove( "sun.net.client.defaultReadTimeout" );
		}

		if ( Preferences.getBoolean( "useNaiveSecureLogin" ) || Preferences.getBoolean( "connectViaAddress" ) )
		{
			NaiveSecureSocketLayer.install();
		}
		else
		{
			NaiveSecureSocketLayer.uninstall();
		}

		systemProperties.put( "http.keepAlive", "false" );

		if ( Preferences.getBoolean( "useSecureLogin" ) )
		{
			systemProperties.put( "http.referer", "https://" + GenericRequest.KOL_HOST + "/game.php" );
		}
		else
		{
			systemProperties.put( "http.referer", "http://" + GenericRequest.KOL_HOST + "/game.php" );
		}
	}

	private static void applyProxySettings()
	{
		GenericRequest.applyProxySettings( "http" );
		GenericRequest.applyProxySettings( "https" );
	}

	private static void applyProxySettings( String protocol )
	{
		if ( System.getProperty( "os.name" ).startsWith( "Mac" ) )
		{
			return;
		}

		Properties systemProperties = System.getProperties();

		String proxySet = Preferences.getString( "proxySet" );
		String proxyHost = Preferences.getString( protocol + ".proxyHost" );
		String proxyPort = Preferences.getString( protocol + ".proxyPort" );
		String proxyUser = Preferences.getString( protocol + ".proxyUser" );
		String proxyPassword = Preferences.getString( protocol + ".proxyPassword" );

		// Remove the proxy host from the system properties
		// if one isn't specified, or proxy setting is off.

		if ( proxySet.equals( "false" ) || proxyHost.equals( "" ) )
		{
			systemProperties.remove( protocol + ".proxyHost" );
			systemProperties.remove( protocol + ".proxyPort" );
		}
		else
		{
			try
			{
				proxyHost = InetAddress.getByName( proxyHost ).getHostAddress();
			}
			catch ( UnknownHostException e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Error in proxy setup" );
			}

			systemProperties.put( protocol + ".proxyHost", proxyHost );
			systemProperties.put( protocol + ".proxyPort", proxyPort );
		}

		// Remove the proxy user from the system properties
		// if one isn't specified, or proxy setting is off.

		if ( proxySet.equals( "false" ) || proxyHost.equals( "" ) || proxyUser.equals( "" ) )
		{
			systemProperties.remove( protocol + ".proxyUser" );
			systemProperties.remove( protocol + ".proxyPassword" );
		}
		else
		{
			systemProperties.put( protocol + ".proxyUser", proxyUser );
			systemProperties.put( protocol + ".proxyPassword", proxyPassword );
		}
	}

	private static boolean substringMatches( final String a, final String b )
	{
		return a.contains( b ) || b.contains( a );
	}

	/**
	 * static final method used to manually set the server to be used as the root for all requests by all KoLmafia
	 * clients running on the current JVM instance.
	 * 
	 * @param server The hostname of the server to be used.
	 */

	public static void setLoginServer( final String server )
	{
		if ( server == null )
		{
			return;
		}

		for ( int i = 0; i < GenericRequest.SERVERS.length; ++i )
		{
			if ( GenericRequest.substringMatches( server, GenericRequest.SERVERS[ i ] ) )
			{
				GenericRequest.setLoginServer( i );
				return;
			}
		}
	}

	private static void setLoginServer( final int serverIndex )
	{
		GenericRequest.KOL_HOST = GenericRequest.SERVERS[ serverIndex ];

		try
		{
			if ( Preferences.getBoolean( "connectViaAddress" ) )
			{
				GenericRequest.KOL_ROOT = new URL( "http", GenericRequest.KOL_IP, 80, "/" );
			}
			else
			{
				GenericRequest.KOL_ROOT = new URL( "http", GenericRequest.KOL_HOST, 80, "/" );
			}
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		try
		{
			if ( Preferences.getBoolean( "connectViaAddress" ) )
			{
				GenericRequest.KOL_SECURE_ROOT = new URL( "https", GenericRequest.KOL_IP, 443, "/" );
			}
			else
			{
				GenericRequest.KOL_SECURE_ROOT = new URL( "https", GenericRequest.KOL_HOST, 443, "/" );
			}
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}

		Preferences.setString( "loginServerName", GenericRequest.KOL_HOST );
	}

	/**
	 * static final method used to return the server currently used by this KoLmafia session.
	 * 
	 * @return The host name for the current server
	 */

	public static String getRootHostName()
	{
		return GenericRequest.KOL_HOST;
	}

	/**
	 * Constructs a new GenericRequest which will notify the given client of any changes and will use the given URL for
	 * data submission.
	 * 
	 * @param formURLString The form to be used in posting data
	 */

	public GenericRequest( final String newURLString, final boolean usePostMethod )
	{
        data = new ArrayList<String>();
		if ( !newURLString.equals( "" ) )
		{
            constructURLString( newURLString, usePostMethod );
		}
	}

	public GenericRequest( final String newURLString )
	{
		this( newURLString, true );
	}

	public static void suppressUpdate( final boolean suppressUpdate )
	{
		GenericRequest.suppressUpdate = suppressUpdate;
	}

	public GenericRequest cloneURLString( final GenericRequest req )
	{
		String newURLString = req.getFullURLString();
		boolean usePostMethod = !req.data.isEmpty();
		boolean encoded = true;
		return constructURLString( newURLString, usePostMethod, encoded );
	}

	public GenericRequest constructURLString( final String newURLString )
	{
		return constructURLString( newURLString, true, false );
	}

	public GenericRequest constructURLString( final String newURLString, final boolean usePostMethod )
	{
		return constructURLString( newURLString, usePostMethod, false );
	}

	public GenericRequest constructURLString( String newURLString, final boolean usePostMethod, final boolean encoded )
	{
        responseText = null;
        dataChanged = true;
        data.clear();

		String oldURLString = formURLString;

		int formSplitIndex = newURLString.indexOf( "?" );
		String queryString = null;

		if ( formSplitIndex == -1 )
		{
            baseURLString = newURLString;
		}
		else
		{
            baseURLString = GenericRequest.decodePath( newURLString.substring( 0, formSplitIndex ) );

			queryString = newURLString.substring( formSplitIndex + 1 );
		}

		while ( baseURLString.startsWith( "/" ) || baseURLString.startsWith( "." ) )
		{
            baseURLString = baseURLString.substring( 1 );
		}

		if ( queryString == null )
		{
            formURLString = baseURLString;
		}
		else if ( !usePostMethod )
		{
            formURLString = baseURLString + "?" + queryString;
		}
		else
		{
            formURLString = baseURLString;
            addFormFields( queryString, encoded );
		}

		if ( !formURLString.equals( oldURLString ) )
		{
            currentHost = GenericRequest.KOL_HOST;
            formURL = null;
		}

        isChatRequest =
                formURLString.startsWith( "chat.php" ) ||
                        formURLString.startsWith( "newchatmessages.php" ) ||
                        formURLString.startsWith( "submitnewchat.php" );

		return this;
	}

	/**
	 * Returns the location of the form being used for this URL, in case it's ever needed/forgotten.
	 */

	public String getURLString()
	{
		return data.isEmpty() ?
                formURLString :
                formURLString + "?" + getDisplayDataString();
	}

	public String getFullURLString()
	{
		return data.isEmpty() ?
                formURLString :
                formURLString + "?" + getDataString();
	}

	public String getDisplayURLString()
	{
		return data.isEmpty() ?
			StringUtilities.singleStringReplace( formURLString, GenericRequest.passwordHashValue, "" ) :
                formURLString + "?" + getDisplayDataString();
	}

	/**
	 * Clears the data fields so that the descending class can have a fresh set of data fields. This allows requests
	 * with variable numbers of parameters to be reused.
	 */

	public void clearDataFields()
	{
        data.clear();
	}

	public void addFormFields( final String fields, final boolean encoded )
	{
		if ( !fields.contains( "&" ) )
		{
            addFormField( fields, encoded );
			return;
		}

		String[] tokens = fields.split( "&" );
		for ( int i = 0; i < tokens.length; ++i )
		{
			if ( tokens[ i ].length() > 0 )
			{
                addFormField( tokens[ i ], encoded );
			}
		}
	}

	public void addFormField( final String element, final boolean encoded )
	{
		if ( encoded )
		{
            addEncodedFormField( element );
		}
		else
		{
            addFormField( element );
		}
	}

	/**
	 * Adds the given form field to the GenericRequest. Descendant classes should use this method if they plan on
	 * submitting forms to Kingdom of Loathing before a call to the <code>super.run()</code> method. Ideally, these
	 * fields can be added at construction time.
	 * 
	 * @param name The name of the field to be added
	 * @param value The value of the field to be added
	 * @param allowDuplicates true if duplicate names are OK
	 */

	public void addFormField( final String name, final String value, final boolean allowDuplicates )
	{
        dataChanged = true;

		String charset = isChatRequest ? "ISO-8859-1" : "UTF-8";

		String encodedName = name + "=";
		String encodedValue = value == null ? "" : GenericRequest.encodeURL( value, charset );

		// Make sure that when you're adding data fields, you don't
		// submit duplicate fields.

		if ( !allowDuplicates )
		{
			Iterator it = data.iterator();
			while ( it.hasNext() )
			{
				if ( ( (String) it.next() ).startsWith( encodedName ) )
				{
					it.remove();
				}
			}
		}

		// If the data did not already exist, then
		// add it to the end of the array.

        data.add( encodedName + encodedValue );
	}

	public void addFormField( final String name, final String value )
	{
        addFormField( name, value, false );
	}

	/**
	 * Adds the given form field to the GenericRequest.
	 * 
	 * @param element The field to be added
	 */

	public void addFormField( final String element )
	{
		int equalIndex = element.indexOf( "=" );
		if ( equalIndex == -1 )
		{
            addFormField( element, "", false );
			return;
		}

		String name = element.substring( 0, equalIndex ).trim();
		String value = element.substring( equalIndex + 1 ).trim();
        addFormField( name, value, true );
	}

	/**
	 * Adds an already encoded form field to the GenericRequest.
	 * 
	 * @param element The field to be added
	 */

	public void addEncodedFormField( String element )
	{
		if ( element == null || element.equals( "" ) )
		{
			return;
		}

		// Browsers are inconsistent about what, exactly, they supply.
		//
		// When you visit the crafting "Discoveries" page and select a
		// multi-step recipe, you get the following as the path:
		//
		// craft.php?mode=cook&steps[]=2262,2263&steps[]=2264,2265
		//
		// If you then confirm that you want to make that recipe, you
		// get the following as your path:
		//
		// craft.php?mode=cook&steps[]=2262,2263&steps[]=2264,2265
		//
		// and the following as your POST data:
		//
		// action=craft&steps%5B%5D=2262%2C2263&steps5B%5D=2264%2C2265&qty=1&pwd
		//
		// URL decoding the latter gives:
		//
		// action=craft&steps[]=2262,2263&steps[]=2264,2265&qty=1&pwd
		//
		// We have to recognize that the following are identical:
		//
		// steps%5B%5D=2262%2C2263
		// steps[]=2262,2263
		//
		// and not submit duplicates when we post the request. For the
		// above example, when we submit path + form fields, we want to
		// end up with:
		//
		// craft.php?mode=cook&steps[]=2262,2263&steps[]=2264,2265&action=craft&qty=1&pwd
		//
		// or, more correctly, with the data URLencoded:
		//
		// craft.php?mode=cook&steps%5B%5D=2262%2C2263&steps%5B%5D=2264%2C2265&action=craft&qty=1&pwd
		//
		// One additional wrinkle: we now see the following URL:
		//
		// craft.php?mode=combine&steps%5B%5D=118,119&steps%5B%5D=120,121
		//
		// given the following POST data:
		//
		// mode=combine&pwd=5a88021883a86d2b669654f79598101e&action=craft&steps%255B%255D=118%2C119&steps%255B%255D=120%2C121&qty=1
		//
		// Notice that the URL is actually NOT encoded and the POST
		// data IS encoded. So, %255B -> %5B

		int equalIndex = element.indexOf( "=" );
		if ( equalIndex != -1 )
		{
			String name = element.substring( 0, equalIndex ).trim();
			String value = element.substring( equalIndex + 1 ).trim();
			String charset = isChatRequest ? "ISO-8859-1" : "UTF-8";

			// The name may or may not be encoded.
			name = GenericRequest.decodeField( name, "UTF-8" );
			value = GenericRequest.decodeField( value, charset );

			// But we want to always submit value encoded.
			value = GenericRequest.encodeURL( value, charset );

			element = name + "=" + value;
		}

		Iterator it = data.iterator();
		while ( it.hasNext() )
		{
			if ( ( (String) it.next() ).equals( element ) )
			{
				return;
			}
		}

        data.add( element );
	}

	public List getFormFields()
	{
		if ( !data.isEmpty() )
		{
			return data;
		}

		int index = formURLString.indexOf( "?" );
		if ( index == -1 )
		{
			return Collections.EMPTY_LIST;
		}

		String[] tokens = formURLString.substring( index + 1 ).split( "&" );
		List<String> fields = new ArrayList<String>();
		for ( int i = 0; i < tokens.length; ++i )
		{
			fields.add( tokens[ i ] );
		}
		return fields;
	}

	public String getFormField( final String key )
	{
		return findField( getFormFields(), key );
	}

	private String findField( final List data, final String key )
	{
		for ( int i = 0; i < data.size(); ++i )
		{
			String datum = (String) data.get( i );

			int splitIndex = datum.indexOf( "=" );
			if ( splitIndex == -1 )
			{
				continue;
			}

			String name = datum.substring( 0, splitIndex );
			if ( !name.equalsIgnoreCase( key ) )
			{
				continue;
			}

			String value = datum.substring( splitIndex + 1 );

			// Chat was encoded as ISO-8859-1, so decode it that way.
			String charset = isChatRequest ? "ISO-8859-1" : "UTF-8";
			return GenericRequest.decodeField( value, charset );
		}

		return null;
	}

	public static String decodePath( final String urlString )
	{
		if ( urlString == null )
		{
			return null;
		}

		String oldURLString = null;
		String newURLString = urlString;

		try
		{
			do
			{
				oldURLString = newURLString;
				newURLString = URLDecoder.decode( oldURLString, "UTF-8" );
			}
			while ( !oldURLString.equals( newURLString ) );
		}
		catch ( IOException e )
		{
		}

		return newURLString;
	}

	public static String decodeField( final String urlString )
	{
		return GenericRequest.decodeField( urlString, "UTF-8" );
	}

	public static String decodeField( final String value, final String charset )
	{
		if ( value == null )
		{
			return null;
		}

		try
		{
			return URLDecoder.decode( value, charset );
		}
		catch ( IOException e )
		{
			return value;
		}
	}

	public static String encodeURL( final String urlString )
	{
		return GenericRequest.encodeURL( urlString, "UTF-8" );
	}

	public static String encodeURL( final String urlString, final String charset )
	{
		if ( urlString == null )
		{
			return null;
		}

		try
		{
			return URLEncoder.encode( urlString, charset );
		}
		catch ( IOException e )
		{
			return urlString;
		}
	}

	public void removeFormField( final String name )
	{
		if ( name == null )
		{
			return;
		}

        dataChanged = true;

		String encodedName = name + "=";

		Iterator it = data.iterator();
		while ( it.hasNext() )
		{
			if ( ( (String) it.next() ).startsWith( encodedName ) )
			{
				it.remove();
			}
		}
	}

	public String getPath()
	{
		return formURLString;
	}

	public String getBasePath()
	{
		String path = formURLString;
		if ( path == null )
		{
			return null;
		}
		int quest = path.indexOf( "?" );
		return quest != -1 ? path.substring( 0, quest ) : path;
	}

	public String getHashField()
	{
		return "pwd";
	}

	private String getDataString()
	{
		// This returns the data string as we will submit it to KoL: if
		// the request wants us to include the password hash, we
		// include the actual value

		StringBuilder dataBuffer = new StringBuilder();
		String hashField = getHashField();

		for ( int i = 0; i < data.size(); ++i )
		{
			String element = (String) data.get( i );

			if ( element.equals( "" ) )
			{
				continue;
			}

			if ( hashField != null && element.startsWith( hashField ) )
			{
				int index = element.indexOf( '=' );
				int length = hashField.length();

				// If this is exactly the hashfield, either
				// with or without a value, omit it.
				if ( length == ( index == -1 ? element.length() : length ) )
				{
					continue;
				}
			}

			if ( dataBuffer.length() > 0 )
			{
				dataBuffer.append( '&' );
			}

			dataBuffer.append( element );
		}

		if ( hashField != null && !GenericRequest.passwordHash.equals( "" ) )
		{
			if ( dataBuffer.length() > 0 )
			{
				dataBuffer.append( '&' );
			}

			dataBuffer.append( hashField );
			dataBuffer.append( '=' );
			dataBuffer.append( GenericRequest.passwordHash );
		}

		return dataBuffer.toString();
	}

	private String getDisplayDataString()
	{
		// This returns the data string as we will display it in the
		// logs: omitting the actual boring value of the password hash

		StringBuilder dataBuffer = new StringBuilder();

		for ( int i = 0; i < data.size(); ++i )
		{
			String element = (String) data.get( i );

			if ( element.equals( "" ) )
			{
				continue;
			}

			if ( element.startsWith( "pwd=" ) )
			{
				element = "pwd";
			}
			else if ( element.startsWith( "phash=" ) )
			{
				element = "phash";
			}
			else if ( element.startsWith( "password=" ) )
			{
				element = "password";
			}

			if ( dataBuffer.length() > 0 )
			{
				dataBuffer.append( '&' );
			}

			dataBuffer.append( element );
		}

		return dataBuffer.toString();
	}

	private boolean shouldUpdateDebugLog()
	{
		return RequestLogger.isDebugging() && !isChatRequest;
	}

	private boolean stopForCounters()
	{
		while ( true )
		{
			TurnCounter expired = TurnCounter.getExpiredCounter( this, true );
			while ( expired != null )
			{
				// Process all expiring informational counters
				// first.  This strategy has the best chance of
				// not screwing everything up totally if both
				// informational and aborting counters expire
				// on the same turn.
				KoLmafia.updateDisplay( "(" + expired.getLabel() + " counter expired)" );
                invokeCounterScript( expired );
				expired = TurnCounter.getExpiredCounter( this, true );
			}

			expired = TurnCounter.getExpiredCounter( this, false );
			if ( expired == null )
			{
				break;
			}

			int remain = expired.getTurnsRemaining();
			if ( remain < 0 )
			{
				continue;
			}

			TurnCounter also;
			while ( ( also = TurnCounter.getExpiredCounter( this, false ) ) != null )
			{
				if ( also.getTurnsRemaining() < 0 )
				{
					continue;
				}
				if ( also.getLabel().equals( "Fortune Cookie" ) )
				{
					KoLmafia.updateDisplay( "(" + expired.getLabel() + " counter discarded due to conflict)" );
					expired = also;
				}
				else
				{
					KoLmafia.updateDisplay( "(" + also.getLabel() + " counter discarded due to conflict)" );
				}
			}

			if ( invokeCounterScript( expired ) )
			{
				// Abort if between battle actions fail
				if ( !KoLmafia.permitsContinue() )
				{
					return true;
				}
				continue;
			}

			String message;
			if ( remain == 0 )
			{
				message = expired.getLabel() + " counter expired.";
			}
			else
			{
				message =
					expired.getLabel() + " counter will expire after " + remain + " more turn" + ( remain == 1 ? "." : "s." );
			}

			if ( expired.getLabel().equals( "Fortune Cookie" ) )
			{
				message += " " + EatItemRequest.lastSemirareMessage();
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return true;
		}

		return false;
	}

	private boolean invokeCounterScript( final TurnCounter expired )
	{
		String scriptName = Preferences.getString( "counterScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}

		Interpreter interpreter =
			KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter != null )
		{
			// Clear abort state so counter script and between
			// battle actions are not hindered.
			KoLmafia.forceContinue();

			String pref = Preferences.getString( "lastAdventure" );
			KoLAdventure nextLocation = AdventureDatabase.getAdventure( pref );
			int oldTurns = KoLCharacter.getCurrentRun();

			Value v = interpreter.execute( "main", new String[]
			{
				expired.getLabel(),
				String.valueOf( expired.getTurnsRemaining() )
			} );

			// If the counter script used adventures, we need to
			// run between-battle actions for the next adventure,
			// in order to maintain moods

			if ( KoLCharacter.getCurrentRun() != oldTurns )
			{
				KoLAdventure.setNextLocation( nextLocation, pref );
				RecoveryManager.runBetweenBattleChecks( true );
			}

			return v != null && v.intValue() != 0;
		}

		return false;
	}

	public static String getAction( final String urlString )
	{
		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		return matcher.find() ? GenericRequest.decodeField( matcher.group( 1 ) ) : null;
	}

	public static String getPlace( final String urlString )
	{
		Matcher matcher = GenericRequest.PLACE_PATTERN.matcher( urlString );
		return matcher.find() ? GenericRequest.decodeField( matcher.group( 1 ) ) : null;
	}

	public static final Pattern HOWMUCH_PATTERN = Pattern.compile( "howmuch=([^&]*)" );

	public static int getHowMuch( final String urlString )
	{
		Matcher matcher = GenericRequest.HOWMUCH_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			// KoL allows any old crap in the input field. It
			// strips out non-numeric characters and treats the
			// rest as an integer.
			String field = GenericRequest.decodeField( matcher.group( 1 ) );
			try
			{
				return StringUtilities.parseIntInternal2( field );
			}
			catch ( NumberFormatException e )
			{
			}
		}
		return -1;
	}

	public void reconstructFields()
	{
	}

	/**
	 * Runs the thread, which prepares the connection for output, posts the data to the Kingdom of Loathing, and
	 * prepares the input for reading. Because the Kingdom of Loathing has identical page layouts, all page reading and
	 * handling will occur through these method calls.
	 */

	public void run()
	{
		if ( GenericRequest.serverCookie == null &&
		     !( this instanceof LoginRequest ) &&
		     !( this instanceof LogoutRequest ) )
		{
			return;
		}

        timeoutCount = 0;
        containsUpdate = false;

		String location = getURLString();
		if ( StaticEntity.backtraceTrigger != null &&
                location.contains( StaticEntity.backtraceTrigger ) )
		{
			StaticEntity.printStackTrace( "Backtrace triggered by page load" );
		}

		if ( location.contains( "clan" ) )
		{
			if ( location.contains( "action=leaveclan" ) || location.contains( "action=joinclan" ) )
			{
				ClanManager.resetClanId();
			}
		}

		if ( ResponseTextParser.hasResult( formURLString ) && stopForCounters() )
		{
			return;
		}

		if ( shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( getClass() );
		}

		if ( location.startsWith( "hermit.php?auto" ) )
		{
			// auto-buying chewing gum or permits overrides the
			// setting that disables NPC purchases, since the user
			// explicitly requested the purchase.
			boolean old = Preferences.getBoolean( "autoSatisfyWithNPCs" );
			if ( !old )
			{
				Preferences.setBoolean( "autoSatisfyWithNPCs", true );
			}

			// If he wants us to automatically get a worthless item
			// in the sewer, do it.
			if ( location.contains( "autoworthless=on" ) )
			{
				InventoryManager.retrieveItem( HermitRequest.WORTHLESS_ITEM, false );
			}

			// If he wants us to automatically get a hermit permit, if needed, do it.
			// If he happens to have a hermit script, use it and obviate permits
			if ( location.contains( "autopermit=on" ) )
			{
				if ( InventoryManager.hasItem( HermitRequest.HACK_SCROLL ) )
				{
					RequestThread.postRequest( UseItemRequest.getInstance( HermitRequest.HACK_SCROLL ) );
				}
				InventoryManager.retrieveItem( ItemPool.HERMIT_PERMIT, false );
			}

			if ( !old )
			{
				Preferences.setBoolean( "autoSatisfyWithNPCs", false );
			}
		}
		else if ( location.startsWith( "mountains.php?orcs=1" ) )
		{
			InventoryManager.retrieveItem( ItemPool.BRIDGE );
		}
		else if ( location.startsWith( "casino.php" ) )
		{
			InventoryManager.retrieveItem( ItemPool.CASINO_PASS );
		}
		else if ( location.startsWith( "beach.php?action=woodencity" ) )
		{
			CreateItemRequest staff = CreateItemRequest.getInstance( ItemPool.STAFF_OF_ED );
			if ( staff != null && staff.getQuantityPossible() > 0 )
			{
				staff.setQuantityNeeded( 1 );
				staff.run();
			}
			AdventureResult hooks = ItemPool.get( ItemPool.WORM_RIDING_HOOKS, 1 );
			AdventureResult machine = ItemPool.get( ItemPool.DRUM_MACHINE, 1 );
			if ( ( KoLConstants.inventory.contains( hooks ) ||
				KoLCharacter.hasEquipped( hooks, EquipmentManager.WEAPON ) ) &&
				KoLConstants.inventory.contains( machine ) )
			{
				UseItemRequest.getInstance( machine ).run();
			}
		}
		else if ( location.startsWith( "pandamonium.php?action=mourn&whichitem=" ) )
		{
			Matcher itemMatcher = UseItemRequest.ITEMID_PATTERN.matcher( location );
			if ( !itemMatcher.find() )
			{
				return;
			}
			int comedyItemID = StringUtilities.parseInt( itemMatcher.group( 1 ) );

			String comedy;
			boolean offhand = false;
			switch ( comedyItemID )
			{
			case ItemPool.INSULT_PUPPET:
				comedy = "insult";
				offhand = true;
				break;
			case ItemPool.OBSERVATIONAL_GLASSES:
				comedy = "observe";
				break;
			case ItemPool.COMEDY_PROP:
				comedy = "prop";
				break;
			default:
				KoLmafia.updateDisplay(
					MafiaState.ABORT,
					"\"" + comedyItemID + "\" is not a comedy item number that Mafia recognizes." );
				return;
			}

			AdventureResult comedyItem = ItemPool.get( comedyItemID, 1 );

			SpecialOutfit.createImplicitCheckpoint();
			if ( KoLConstants.inventory.contains( comedyItem ) )
			{
				// Unequip any 2-handed weapon before equipping an offhand
				if ( offhand )
				{
					AdventureResult weapon = EquipmentManager.getEquipment( EquipmentManager.WEAPON );
					int hands = EquipmentDatabase.getHands( weapon.getItemId() );
					if ( hands > 1 )
					{
						new EquipmentRequest( EquipmentRequest.UNEQUIP, EquipmentManager.WEAPON ).run();
					}
				}

				new EquipmentRequest( comedyItem ).run();
			}

			String text = null;
			if ( KoLmafia.permitsContinue() && KoLCharacter.hasEquipped( comedyItem ) )
			{
				GenericRequest request = new PandamoniumRequest( comedy );
				request.run();
				text = request.responseText;
			}
			SpecialOutfit.restoreImplicitCheckpoint();

			if ( text != null )
			{
                responseText = text;
				return;
			}
		}

		// To avoid wasting turns, buy a can of hair spray before
		// climbing the tower. Also, if the person has an NG, make sure
		// to construct it first.  If there are any tower items sitting
		// in the closet or that have not been constructed, pull them
		// out.

		if ( location.startsWith( "lair4.php" ) || location.startsWith( "lair5.php" ) )
		{
			SorceressLairManager.makeGuardianItems();
		}

        execute();

		if ( responseCode != 200 )
		{
			return;
		}

		if ( responseText == null )
		{
			KoLmafia.updateDisplay(
				MafiaState.ABORT,
				"Server " + GenericRequest.KOL_HOST + " returned a blank page from " + getBasePath() + ". Complain to Jick, not us." );
			return;
		}

		// Call central dispatch method for locations that require
		// special handling

		CouncilFrame.handleQuestChange( location, responseText );

        formatResponse();
		KoLCharacter.updateStatus();
	}

	public void execute()
	{
		String urlString = getURLString();

		if ( !GenericRequest.isRatQuest )
		{
			GenericRequest.isRatQuest = urlString.startsWith( "cellar.php" );
		}

		if ( GenericRequest.isRatQuest && ResponseTextParser.hasResult( formURLString ) && !urlString.startsWith( "cellar.php" ) )
		{
			GenericRequest.isRatQuest = urlString.startsWith( "fight.php" );
		}

		if ( GenericRequest.isRatQuest )
		{
			TavernRequest.preTavernVisit( this );
		}

		if ( ResponseTextParser.hasResult( formURLString ) && GenericRequest.isBarrelSmash )
		{
			// Smash has resulted in a mimic.
			// Continue tracking throughout the combat
			GenericRequest.isBarrelSmash = urlString.startsWith( "fight.php" );
		}

		if ( urlString.startsWith( "barrel.php?" ) )
		{
			GenericRequest.isBarrelSmash = true;
			BarrelDecorator.beginSmash( urlString );
		}

		if ( ResponseTextParser.hasResult( formURLString ) )
		{
			RequestLogger.registerRequest( this, urlString );
		}

		if ( urlString.startsWith( "choice.php" ) )
		{
			GenericRequest.choiceHandled = false;
			ChoiceManager.preChoice( this );
		}

		// If you're about to fight the Naughty Sorceress,
		// clear your list of effects.

		if ( urlString.startsWith( "lair6.php" ) && urlString.contains( "place=5" ) )
		{
			KoLConstants.activeEffects.clear();
			// *** Do we retain intrinsic effects?
		}

		if ( urlString.startsWith( "ascend.php" ) && urlString.contains( "action=ascend" ) )
		{
			GenericRequest.ascending = true;
			KoLmafia.forceContinue();
			ValhallaManager.preAscension();
			GenericRequest.ascending = false;

			// If the preAscension script explicitly aborted, don't
			// jump into the gash. Let the user fix the problem.
			if ( KoLmafia.refusesContinue() )
			{
				return;
			}

			// Set preference so we call ValhallaManager.onAscension()
			// when we reach the afterlife.
			Preferences.setInteger( "lastBreakfast", 0 );
		}

		if ( urlString.startsWith( "afterlife.php" ) && Preferences.getInteger( "lastBreakfast" ) != -1 )
		{
			ValhallaManager.onAscension();
		}

		do
		{
			if ( !prepareConnection() )
			{
				break;
			}
		}
		while ( !postClientData() && !retrieveServerReply() && timeoutCount < GenericRequest.TIMEOUT_LIMIT );

		if ( !LoginRequest.isInstanceRunning() )
		{
			ConcoctionDatabase.refreshConcoctions( false );
		}
	}

	public static boolean shouldIgnore( final GenericRequest request )
	{
		String requestURL = GenericRequest.decodeField( request.formURLString );
		return requestURL == null ||
			// Disallow mall searches
                requestURL.contains( "mall.php" ) ||
                requestURL.contains( "manageprices.php" ) ||
			// Disallow anything to do with chat
			request.isChatRequest;
	}

	/**
	 * Utility method used to prepare the connection for input and output (if output is necessary). The method attempts
	 * to open the connection, and then apply the needed settings.
	 * 
	 * @return <code>true</code> if the connection was successfully prepared
	 */

	private boolean prepareConnection()
	{
		if ( shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Connecting to " + baseURLString + "..." );
		}

		// Make sure that all variables are reset before you reopen
		// the connection.

        responseCode = 0;
        responseText = null;
        redirectLocation = null;
        formConnection = null;

		try
		{
            formURL = buildURL();
            formConnection = (HttpURLConnection) formURL.openConnection();
		}
		catch ( IOException e )
		{
			if ( shouldUpdateDebugLog() )
			{
				String message = "IOException opening connection (" + getURLString() + "). Retrying...";
				StaticEntity.printStackTrace( e, message );
			}

			return false;
		}

        formConnection.setDoInput( true );

        formConnection.setDoOutput( !data.isEmpty() );
        formConnection.setUseCaches( false );
        formConnection.setInstanceFollowRedirects( false );

		if ( GenericRequest.serverCookie != null )
		{
			if ( formURLString.startsWith( "inventory" ) && GenericRequest.inventoryCookie != null )
			{
                formConnection.addRequestProperty(
                        "Cookie", GenericRequest.inventoryCookie + "; " + GenericRequest.serverCookie );
			}
			else if ( !formURLString.startsWith( "http:" ) && !formURLString.startsWith( "https:" ) )
			{
                formConnection.addRequestProperty( "Cookie", GenericRequest.serverCookie );
			}
		}

        formConnection.setRequestProperty( "User-Agent", GenericRequest.userAgent );
        formConnection.setRequestProperty( "Connection", "close" );

		if ( !data.isEmpty() )
		{
			if ( dataChanged )
			{
                dataChanged = false;
                dataString = getDataString().getBytes();
			}

            formConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
            formConnection.setRequestProperty( "Content-Length", String.valueOf( dataString.length ) );
		}

		return true;
	}

	private URL buildURL()
		throws MalformedURLException
	{
		if ( formURL != null && currentHost.equals( GenericRequest.KOL_HOST ) )
		{
			return formURL;
		}

        currentHost = GenericRequest.KOL_HOST;
		String urlString = formURLString;

		URL context = null;

		if ( !urlString.startsWith( "http:" ) && !urlString.startsWith( "https:" ) )
		{
			if ( Preferences.getBoolean( "useSecureLogin" ) && urlString.contains( "login.php" ) )
			{
				context = GenericRequest.KOL_SECURE_ROOT;
			}
			else
			{
				context = GenericRequest.KOL_ROOT;
			}
		}

		return new URL( context, urlString );
	}

	/**
	 * Utility method used to post the client's data to the Kingdom of Loathing server. The method grabs all form fields
	 * added so far and posts them using the traditional ampersand style of HTTP requests.
	 * 
	 * @return <code>true</code> if all data was successfully posted
	 */

	private boolean postClientData()
	{
		if ( shouldUpdateDebugLog() )
		{
            printRequestProperties();
		}

		// Only attempt to post something if there's actually data to
		// post - otherwise, opening an input stream should be enough

		if ( data.isEmpty() )
		{
			return false;
		}

		try
		{
            formConnection.setRequestMethod( "POST" );
			OutputStream ostream = formConnection.getOutputStream();
			ostream.write( dataString );

			ostream.flush();
			ostream.close();

			ostream = null;
			return false;
		}
		catch ( SocketTimeoutException e )
		{
			++timeoutCount;

			if ( shouldUpdateDebugLog() )
			{
				String message = "Time out during data post (" + formURLString + "). This could be bad...";
				RequestLogger.printLine( message );
			}

			return KoLmafia.refusesContinue();
		}
		catch ( IOException e )
		{
			String message = "IOException during data post (" + getURLString() + ").";

			if ( shouldUpdateDebugLog() )
			{
				StaticEntity.printStackTrace( e, message );
			}

			RequestLogger.printLine( MafiaState.ERROR, message );
            timeoutCount = TIMEOUT_LIMIT;
			return true;
		}
	}

	/**
	 * Utility method used to retrieve the server's reply. This method detects the nature of the reply via the response
	 * code provided by the server, and also detects the unusual states of server maintenance and session timeout. All
	 * data retrieved by this method is stored in the instance variables for this class.
	 * 
	 * @return <code>true</code> if the data was successfully retrieved
	 */

	private boolean retrieveServerReply()
	{
		InputStream istream = null;

		if ( shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Retrieving server reply..." );
		}

        responseText = "";
        redirectLocation = "";

		try
		{
			istream = formConnection.getInputStream();
            responseCode = formConnection.getResponseCode();
            redirectLocation = responseCode != 302 ? null : formConnection.getHeaderField( "Location" );
		}
		catch ( SocketTimeoutException e )
		{
			if ( shouldUpdateDebugLog() )
			{
				String message = "Time out retrieving server reply (" + formURLString + ").";
				RequestLogger.printLine( message );
			}

			boolean shouldRetry = retryOnTimeout();
			if ( !shouldRetry && processOnFailure() )
			{
                processResults();
			}

			GenericRequest.forceClose( istream );

			++timeoutCount;
			return !shouldRetry || KoLmafia.refusesContinue();
		}
		catch ( IOException e )
		{
            responseCode = getResponseCode();

			if ( responseCode != 0 )
			{
				String message = "Server returned response code " + responseCode + " for " + baseURLString;
				RequestLogger.printLine( MafiaState.ERROR, message );
			}

			if ( shouldUpdateDebugLog() )
			{
				String message = "IOException retrieving server reply (" + getURLString() + ").";
				StaticEntity.printStackTrace( e, message );
			}

			if ( processOnFailure() )
			{
                responseText = "";
                processResults();
			}

			GenericRequest.forceClose( istream );

            timeoutCount = TIMEOUT_LIMIT;
			return true;
		}

		if ( istream == null )
		{
            responseCode = 302;
            redirectLocation = "main.php";
			return true;
		}

		if ( shouldUpdateDebugLog() )
		{
            printHeaderFields();
		}

		boolean shouldStop = false;

		try
		{
			if ( responseCode == 200 )
			{
				shouldStop = retrieveServerReply( istream );
				istream.close();
				if ( shouldStop && responseText != null && responseText.length() < 200 )
				{
					// This may be a JavaScript redirect.
					Matcher m = GenericRequest.JS_REDIRECT_PATTERN.matcher( responseText );
					if ( m.find() )
					{
                        redirectLocation = m.group( 1 );
						shouldStop = handleServerRedirect();
					}
				}
			}
			else
			{
				// If the response code is not 200, then you've
				// read all the information you need.  Close
				// the input stream.

				istream.close();
				shouldStop = responseCode == 302 ? handleServerRedirect() : true;
			}
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
			return true;
		}

		istream = null;
		return shouldStop || KoLmafia.refusesContinue();
	}

	private int getResponseCode()
	{
		if ( formConnection != null )
		{
			try
			{
				return formConnection.getResponseCode();
			}
			catch ( IOException e )
			{
			}
		}

		return 0;
	}

	private static void forceClose( final InputStream stream)
	{
		if ( stream != null )
		{
			try
			{
				stream.close();
			}
			catch ( IOException e )
			{
			}
		}
	}

	protected boolean retryOnTimeout()
	{
		return formURLString.endsWith( ".php" ) && (data.isEmpty() || getClass() == GenericRequest.class );
	}

	protected boolean processOnFailure()
	{
		return false;
	}

	private boolean handleServerRedirect()
	{
		if ( redirectLocation == null )
		{
			return true;
		}

		if ( redirectLocation.startsWith( "maint.php" ) )
		{
			// If the request was issued from the Relay
			// Browser, follow the redirect and show the
			// user the maintenance page.

			if ( this instanceof RelayRequest )
			{
				return true;
			}

			// Otherwise, inform the user in the status
			// line and abort.

			KoLmafia.updateDisplay( MafiaState.ABORT, "Nightly maintenance. Please restart KoLmafia." );
			GenericRequest.reset();
			return true;
		}

		// Check to see if this is a login page redirect.  If it is,
		// then construct the URL string and notify the browser that it
		// should change everything.

		if ( formURLString.startsWith( "login.php" ) )
		{
			if ( redirectLocation.startsWith( "login.php" ) )
			{
                constructURLString( redirectLocation, false );
				return false;
			}

			Matcher matcher = GenericRequest.REDIRECT_PATTERN.matcher( redirectLocation );
			if ( matcher.find() )
			{
				String server = matcher.group( 1 );
				if ( !server.equals( "" ) )
				{
					RequestLogger.printLine( "Redirected to " + server + "..." );
					GenericRequest.setLoginServer( server );
				}
                constructURLString( matcher.group( 2 ), false );
				return false;
			}

			LoginRequest.processLoginRequest( this );
			return true;
		}

		if ( redirectLocation.startsWith( "fight.php" ) )
		{
			GenericRequest.checkItemRedirection( getURLString() );

			if ( this instanceof UseItemRequest )
			{
				FightRequest.INSTANCE.run();
				return !LoginRequest.isInstanceRunning();
			}
		}

		if ( redirectLocation.startsWith( "choice.php" ) )
		{
			GenericRequest.checkItemRedirection( getURLString() );
		}

		if ( redirectLocation.startsWith( "messages.php?results=Message" ) )
		{
			SendMailRequest.parseTransfer( getURLString() );
		}

		if ( redirectLocation.startsWith( "login.php" ) )
		{
			if ( this instanceof LoginRequest )
			{
                constructURLString( redirectLocation, false );
				return false;
			}

			if ( formURLString.startsWith( "logout.php" ) )
			{
				return true;
			}

			if ( LoginRequest.executeTimeInRequest( getURLString(), redirectLocation ) )
			{
                dataChanged = true;
				return false;
			}

			return true;
		}

		if ( this instanceof RelayRequest )
		{
			return true;
		}

		if ( formURLString.startsWith( "fight.php" ) )
		{
			if ( redirectLocation.startsWith( "main.php" ) )
			{
                constructURLString( redirectLocation, false );
				return false;
			}
		}

		if ( shouldFollowRedirect() )
		{
			// Re-setup this request to follow the redirect
			// desired and rerun the request.

            constructURLString( redirectLocation, false );
			if ( redirectLocation.equals( "choice.php" ) )
			{
				GenericRequest.choiceHandled = false;
				ChoiceManager.preChoice( this );
			}
			return false;
		}

		if ( redirectLocation.startsWith( "adventure.php" ) )
		{
            constructURLString( redirectLocation, false );
			return false;
		}

		if ( redirectLocation.startsWith( "fight.php" ) )
		{
			if ( LoginRequest.isInstanceRunning() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, baseURLString + ": redirected to a fight page." );
				FightRequest.initializeAfterFight();
				return true;
			}

			// You have been redirected to a fight! Here, you need
			// to complete the fight before you can continue.

			if ( this == ChoiceManager.CHOICE_HANDLER ||
				this instanceof AdventureRequest ||
				this instanceof BasementRequest ||
				this instanceof HiddenCityRequest )
			{
				int pos = redirectLocation.indexOf( "ireallymeanit=" );
				if ( pos != -1 )
				{
					FightRequest.ireallymeanit = redirectLocation.substring( pos + 14 );
				}
				FightRequest.INSTANCE.run();
				return !LoginRequest.isInstanceRunning();
			}

			// This is a request which should not have lead to a
			// fight, but it did.  Notify the user.

			KoLmafia.updateDisplay( MafiaState.ABORT, baseURLString + ": redirected to a fight page." );
			return true;
		}

		if ( redirectLocation.startsWith( "choice.php" ) )
		{
			if ( LoginRequest.isInstanceRunning() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, baseURLString + ": redirected to a choice page." );
				ChoiceManager.initializeAfterChoice();
				return true;
			}

			GenericRequest.handlingChoices = true;
			ChoiceManager.processChoiceAdventure();
			GenericRequest.handlingChoices = false;
			return !LoginRequest.isInstanceRunning();
		}

		if ( redirectLocation.startsWith( "ocean.php" ) )
		{
			OceanManager.processOceanAdventure();
			return true;
		}

		if ( formURLString.startsWith( "sellstuff" ) )
		{
			String redirect = redirectLocation;
			String newMode =
				redirect.startsWith( "sellstuff.php" ) ? "compact" :
					redirect.startsWith( "sellstuff_ugly.php" ) ? "detailed" :
						null;

			if ( newMode != null )
			{
				String message = "Autosell mode changed to " + newMode;
				KoLmafia.updateDisplay( message );
				KoLCharacter.setAutosellMode( newMode );
				return true;
			}
		}

		if ( this instanceof AdventureRequest || formURLString.startsWith( "choice.php" ) )
		{
			AdventureRequest.handleServerRedirect( redirectLocation );
			return true;
		}

		if ( shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Redirected: " + redirectLocation );
		}

		return true;
	}

	protected boolean shouldFollowRedirect()
	{
		return this != ChoiceManager.CHOICE_HANDLER && getClass() == GenericRequest.class;
	}

	private boolean retrieveServerReply( final InputStream istream )
		throws IOException
	{
		if ( shouldUpdateDebugLog() )
		{
			RequestLogger.updateDebugLog( "Retrieving server reply" );
		}
        responseText = new String( ByteBufferUtilities.read( istream ), "UTF-8" );
		if ( shouldUpdateDebugLog() )
		{
			if ( responseText == null )
			{
				RequestLogger.updateDebugLog( "ResponseText is null" );
			}
			else
			{
				RequestLogger.updateDebugLog( "ResponseText has " + responseText.length() + " characters." );
			}
		}

		if ( responseText != null )
		{
			try
			{
                processResponse();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}

		return true;
	}

	/**
	 * This method allows classes to process a raw, unfiltered server response.
	 */

	public void processResponse()
	{
		String urlString = getURLString();
		boolean hasResult = ResponseTextParser.hasResult( formURLString );

		if ( shouldUpdateDebugLog() )
		{
			String text = responseText;
			if ( !Preferences.getBoolean( "logReadableHTML" ) )
			{
				text = KoLConstants.LINE_BREAK_PATTERN.matcher( text ).replaceAll( "" );
			}
			RequestLogger.updateDebugLog( text );
		}

		if ( urlString.startsWith( "charpane.php" ) )
		{
			long responseTimestamp =
                    formConnection.getHeaderFieldDate( "Date", System.currentTimeMillis() );

			if ( !CharPaneRequest.processResults( responseTimestamp, responseText ) )
			{
                responseCode = 304;
			}

			return;
		}

		if ( !isChatRequest )
		{
			EventManager.checkForNewEvents( responseText );
		}

		if ( GenericRequest.isRatQuest )
		{
			TavernRequest.postTavernVisit( this );
			GenericRequest.isRatQuest = false;
		}

        encounter = AdventureRequest.registerEncounter( this );

		if ( urlString.startsWith( "fight.php" ) )
		{
			FightRequest.updateCombatData( urlString, encounter, responseText );
		}
		else if ( urlString.startsWith( "shore.php" ) )
		{
			AdventureRequest.handleShoreVisit( urlString, responseText );
		}
		else if ( urlString.startsWith( "lair6.php" ) && urlString.contains( "place=6" ) )
		{
			KoLCharacter.liberateKing();
		}

		if ( !GenericRequest.choiceHandled && !isChatRequest )
		{
			// Handle choices BEFORE result processing
			ChoiceManager.postChoice1( this );
		}

		int effectCount = KoLConstants.activeEffects.size();

		if ( hasResult )
		{
			int initialHP = KoLCharacter.getCurrentHP();
            parseResults();

			if ( initialHP != 0 && KoLCharacter.getCurrentHP() == 0 )
			{
				KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
				KoLConstants.activeEffects.add( KoLAdventure.BEATEN_UP );
			}

			if ( !LoginRequest.isInstanceRunning() && !( this instanceof RelayRequest ) )
			{
                showInBrowser( false );
			}
		}

		if ( urlString.startsWith( "fight.php" ) )
		{ // This has to be done after parseResults() to properly
			// deal with combat items received during combat.
			FightRequest.parseCombatItems( responseText );
			FightRequest.parseConditionalCombatSkills( responseText );
		}

		// Now let the main method of result processing for
		// each request type happen.

        processResults();

		if ( !GenericRequest.choiceHandled && !isChatRequest )
		{
			// Handle choices AFTER result processing
			GenericRequest.choiceHandled = !responseText.contains( "choice.php" );
			ChoiceManager.postChoice2( this );
		}

		// Let clover protection kick in if needed

		if ( ResultProcessor.shouldDisassembleClovers( urlString ) )
		{
			KoLmafia.protectClovers();
		}

		// Perhaps check for random donations in Fistcore
		if ( !ResultProcessor.onlyAutosellDonationsCount && KoLCharacter.inFistcore() )
		{
			ResultProcessor.handleDonations( urlString, responseText );
		}

		// Once everything is complete, decide whether or not
		// you should refresh your status.

		if ( !hasResult || GenericRequest.suppressUpdate )
		{
			return;
		}

		if ( this instanceof RelayRequest )
		{
            containsUpdate = false;
		}
		else if ( effectCount != KoLConstants.activeEffects.size() || getAdventuresUsed() > 0 )
		{
            containsUpdate = true;
		}
		else
		{
            containsUpdate |= responseText.contains( "charpane.php" );
		}

		if ( containsUpdate )
		{
			new CharPaneRequest().run();
			RelayServer.updateStatus();
		}
	}

	public void formatResponse()
	{
	}

	/**
	 * Utility method used to skip the given number of tokens within the provided <code>StringTokenizer</code>. This
	 * method is used in order to clarify what's being done, rather than calling <code>st.nextToken()</code> repeatedly.
	 * 
	 * @param st The <code>StringTokenizer</code> whose tokens are to be skipped
	 * @param tokenCount The number of tokens to skip
	 */

	public static void skipTokens( final StringTokenizer st, final int tokenCount )
	{
		for ( int i = 0; i < tokenCount; ++i )
		{
			st.nextToken();
		}
	}

	/**
	 * Utility method used to transform the next token on the given <code>StringTokenizer</code> into an integer.
	 * Because this is used repeatedly in parsing, its functionality is provided globally to all instances of
	 * <code>GenericRequest</code>.
	 * 
	 * @param st The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @return The integer token, if it exists, or 0, if the token was not a number
	 */

	public static int intToken( final StringTokenizer st )
	{
		return GenericRequest.intToken( st, 0 );
	}

	/**
	 * Utility method used to transform the next token on the given <code>StringTokenizer</code> into an integer;
	 * however, this differs in the single-argument version in that only a part of the next token is needed. Because
	 * this is also used repeatedly in parsing, its functionality is provided globally to all instances of
	 * <code>GenericRequest</code>.
	 * 
	 * @param st The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @param fromStart The index at which the integer to parse begins
	 * @return The integer token, if it exists, or 0, if the token was not a number
	 */

	public static int intToken( final StringTokenizer st, final int fromStart )
	{
		String token = st.nextToken().substring( fromStart );
		return StringUtilities.parseInt( token );
	}

	/**
	 * Utility method used to transform part of the next token on the given <code>StringTokenizer</code> into an
	 * integer. This differs from the two-argument in that part of the end of the string is expected to contain
	 * non-numeric values as well. Because this is also repeatedly in parsing, its functionality is provided globally to
	 * all instances of <code>GenericRequest</code>.
	 * 
	 * @param st The <code>StringTokenizer</code> whose next token is to be retrieved
	 * @param fromStart The index at which the integer to parse begins
	 * @param fromEnd The distance from the end at which the first non-numeric character is found
	 * @return The integer token, if it exists, or 0, if the token was not a number
	 */

	public static int intToken( final StringTokenizer st, final int fromStart, final int fromEnd )
	{
		String token = st.nextToken();
		token = token.substring( fromStart, token.length() - fromEnd );
		return StringUtilities.parseInt( token );
	}

	/**
	 * An alternative method to doing adventure calculation is determining how many adventures are used by the given
	 * request, and subtract them after the request is done. This number defaults to <code>zero</code>; overriding
	 * classes should change this value to the appropriate amount.
	 * 
	 * @return The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{
		return 0;
	}

	private void parseResults()
	{
		String urlString = getURLString();

		// If this is a lucky adventure, then remove a clover
		// from the player's inventory,
		//
		// Most places, this is signaled by the message "Your (or your)
		// ten-leaf clover disappears in a puff of smoke."
		//
		// In the Sorceress's entryway, the message is "You see a puff
		// of smoke come from your sack, and catch a whiff of burnt
		// clover"
		//
		// In the Spooky Forest's Lucky, Lucky! encounter, the message is
		// "Your ten-leaf clover disappears into the leprechaun's pocket"
		//
		// The Hippy Camp (In Disguise)'s A Case of the Baskets, the message
		// is "Like the smoke your ten-leaf clover disappears in a puff of"

		if ( responseText.contains( "clover" ) &&
			(responseText.contains( " puff of smoke" ) ||
                    responseText.contains( "into the leprechaun's pocket" ) ||
                    responseText.contains( "disappears in a puff of" )) )
		{
			ResultProcessor.processItem( ItemPool.TEN_LEAF_CLOVER, -1 );
		}

		if ( urlString.startsWith( "dungeon.php" ) &&
                responseText.contains( "key breaks off in the lock" ) )
		{
			// Unfortunately, the key breaks off in the lock.
			ResultProcessor.processItem( ItemPool.SKELETON_KEY, -1 );
		}

		if ( responseText.contains( "You break the bottle on the ground" ) )
		{
			// You break the bottle on the ground, and stomp it to powder
			ResultProcessor.processItem( ItemPool.EMPTY_AGUA_DE_VIDA_BOTTLE, -1 );
		}

		if ( responseText.contains( "FARQUAR" ) ||
                responseText.contains( "Sleeping Near the Enemy" ) )
		{
			// The password to the Dispensary is known!
			Preferences.setInteger( "lastDispensaryOpen", KoLCharacter.getAscensions() );
		}

		if ( urlString.startsWith( "mall.php" ) ||
			urlString.startsWith( "searchmall.php" ) ||
			urlString.startsWith( "account.php" ) ||
			urlString.startsWith( "records.php" ) ||
			( urlString.startsWith( "peevpee.php" ) && getFormField("lid") != null ) )
		{
			// These pages cannot possibly contain an actual item
			// drop, but may have a bogus "You acquire an item:" as
			// part of a store name, profile quote, familiar name, etc.
            containsUpdate = false;
		}
		else if ( urlString.startsWith( "bet.php" ) )
		{
			// This can either add or remove meat from inventory
			// using unique messages, in some cases. Let
			// MoneyMakingGameRequest sort it all out.
            containsUpdate = true;
		}
		else if ( urlString.startsWith( "raffle.php" ) )
		{
            containsUpdate = true;
		}
		else if ( urlString.startsWith( "mallstore.php" ) )
		{
			// Mall stores themselves can only contain processable
			// results when actually buying an item, and then only
			// at the very top of the page.
            containsUpdate =
                    getFormField( "whichitem" ) != null &&
					ResultProcessor.processResults(
						false, responseText.substring( 0, responseText.indexOf( "</table>" ) ) );
		}
		else if ( urlString.startsWith( "fight.php" ) )
		{
            containsUpdate = FightRequest.processResults( responseText );
		}
		else if ( urlString.startsWith( "adventure.php" ) )
		{
            containsUpdate = ResultProcessor.processResults( true, responseText );
		}
		else if ( urlString.startsWith( "arena.php" ) )
		{
            containsUpdate = CakeArenaRequest.parseResults( responseText );
		}
		else if ( urlString.startsWith( "afterlife.php" ) )
		{
            containsUpdate = AfterLifeRequest.parseResponse( urlString, responseText );
		}
		else
		{
            containsUpdate = ResultProcessor.processResults( false, responseText );
		}
	}

	public void processResults()
	{
		boolean externalUpdate = false;
		String path = getPath();

		if ( ResponseTextParser.hasResult( path ) && !path.startsWith( "fight.php" ) )
		{
			externalUpdate = true;
		}
		else if ( path.startsWith( "desc_" ) )
		{
			externalUpdate = true;
		}

		if ( externalUpdate )
		{
			ResponseTextParser.externalUpdate( getURLString(), responseText );
		}
	}

	/*
	 * Method to display the current request in the Fight Frame. If we are synchronizing, show all requests If we are
	 * finishing, show only exceptional requests
	 */

	public void showInBrowser( final boolean exceptional )
	{
		if ( !exceptional && !Preferences.getBoolean( "showAllRequests" ) )
		{
			return;
		}

		// Only show the request if the response code is
		// 200 (not a redirect or error).

		boolean showRequestSync =
			Preferences.getBoolean( "showAllRequests" ) ||
				exceptional && Preferences.getBoolean( "showExceptionalRequests" );

		if ( showRequestSync )
		{
			RequestSynchFrame.showRequest( this );
		}

		if ( exceptional )
		{
			RelayAgent.setErrorRequest( this );

			String linkHTML =
				"<a href=main.php target=mainpane class=error>Click here to continue in the relay browser.</a>";
			InternalMessage message = new InternalMessage( linkHTML, null );
			ChatPoller.addEntry( message );
		}
	}

	private static void checkItemRedirection( final String location )
	{
		AdventureResult item = UseItemRequest.extractItem( location );
		GenericRequest.itemMonster = null;

		if ( item == null )
		{
			return;
		}

		int itemId = item.getItemId();
		String itemName = null;
		boolean consumed = false;

		switch ( itemId )
		{
		case ItemPool.BLACK_PUDDING:
			itemName = "Black Pudding";
			consumed = true;
			break;

		case ItemPool.DRUM_MACHINE:
			itemName = "Drum Machine";
			consumed = true;
			break;

		case ItemPool.DOLPHIN_WHISTLE:
			itemName = "Dolphin Whistle";
			consumed = true;
			MonsterData m = MonsterDatabase.findMonster( "rotten dolphin thief", false );
			if ( m != null )
			{
				m.clearItems();
				String stolen = Preferences.getString( "dolphinItem" );
				if ( stolen.length() > 0 )
				{
					m.addItem( ItemPool.get( stolen, 100 << 16 | 'n' ) );
				}
				m.doneWithItems();
			}
			TurnCounter.startCounting( 10, "Dolphin Whistle cooldown loc=*", "whistle.gif" );
			Preferences.setString( "dolphinItem", "" );
			break;

		case ItemPool.CARONCH_MAP:
			itemName = "Cap'm Caronch's Map";
			break;

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			itemName = "Orcish Frathouse Blueprints";
			break;

		case ItemPool.CURSED_PIECE_OF_THIRTEEN:
			itemName = "Cursed Piece of Thirteen";
			break;

		case ItemPool.SPOOKY_PUTTY_MONSTER:
			itemName = "Spooky Putty Monster";
			Preferences.setString( "spookyPuttyMonster", "" );
			ResultProcessor.processItem( ItemPool.SPOOKY_PUTTY_SHEET, 1 );
			consumed = true;
			KoLmafia.ignoreSpecialMonsters();
			break;

		case ItemPool.RAIN_DOH_MONSTER:
			itemName = "Rain-Doh box full of monster";
			Preferences.setString( "rainDohMonster", "" );
			ResultProcessor.processItem( ItemPool.RAIN_DOH_BOX, 1 );
			consumed = true;
			KoLmafia.ignoreSpecialMonsters();
			break;

		case ItemPool.SHAKING_CAMERA:
			itemName = "Shaking 4-D Camera";
			Preferences.setString( "cameraMonster", "" );
			Preferences.setBoolean( "_cameraUsed", true );
			consumed = true;
			KoLmafia.ignoreSpecialMonsters();
			break;

		case ItemPool.PHOTOCOPIED_MONSTER:
			itemName = "photocopied monster";
			Preferences.setString( "photocopyMonster", "" );
			Preferences.setBoolean( "_photocopyUsed", true );
			consumed = true;
			KoLmafia.ignoreSpecialMonsters();
			break;

		case ItemPool.WAX_BUGBEAR:
			itemName = "wax bugbear";
			Preferences.setString( "waxMonster", "" );
			consumed = true;
			KoLmafia.ignoreSpecialMonsters();
			break;

		case ItemPool.DEPLETED_URANIUM_SEAL:
			itemName = "Infernal Seal Ritual";
			Preferences.increment( "_sealsSummoned", 1 );
			ResultProcessor.processResult( GenericRequest.sealRitualCandles( itemId ) );
			// Why do we count this?
			Preferences.increment( "_sealFigurineUses", 1 );
			break;

		case ItemPool.WRETCHED_SEAL:
		case ItemPool.CUTE_BABY_SEAL:
		case ItemPool.ARMORED_SEAL:
		case ItemPool.ANCIENT_SEAL:
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
			itemName = "Infernal Seal Ritual";
			consumed = true;
			Preferences.increment( "_sealsSummoned", 1 );
			ResultProcessor.processResult( GenericRequest.sealRitualCandles( itemId ) );
			break;

		case ItemPool.BRICKO_OOZE:
		case ItemPool.BRICKO_BAT:
		case ItemPool.BRICKO_OYSTER:
		case ItemPool.BRICKO_TURTLE:
		case ItemPool.BRICKO_ELEPHANT:
		case ItemPool.BRICKO_OCTOPUS:
		case ItemPool.BRICKO_PYTHON:
		case ItemPool.BRICKO_VACUUM_CLEANER:
		case ItemPool.BRICKO_AIRSHIP:
		case ItemPool.BRICKO_CATHEDRAL:
		case ItemPool.BRICKO_CHICKEN:
			itemName = item.getName();
			Preferences.increment( "_brickoFights", 1 );
			consumed = true;
			break;

		case ItemPool.FOSSILIZED_BAT_SKULL:
			itemName = "Fossilized Bat Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_WING, -2 );
			break;

		case ItemPool.FOSSILIZED_BABOON_SKULL:
			itemName = "Fossilized Baboon Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -4 );
			break;

		case ItemPool.FOSSILIZED_SERPENT_SKULL:
			itemName = "Fossilized Serpent Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPINE, -3 );
			break;

		case ItemPool.FOSSILIZED_WYRM_SKULL:
			itemName = "Fossilized Wyrm Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -2 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_WING, -2 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPINE, -3 );
			break;

		case ItemPool.FOSSILIZED_DEMON_SKULL:
			itemName = "Fossilized Demon Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPIKE, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -4 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_WING, -2 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPINE, -1 );
			break;

		case ItemPool.FOSSILIZED_SPIDER_SKULL:
			itemName = "Fossilized Spider Skull";
			consumed = true;
			ResultProcessor.processItem( ItemPool.FOSSILIZED_TORSO, -1 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_LIMB, -8 );
			ResultProcessor.processItem( ItemPool.FOSSILIZED_SPIKE, -8 );
			break;

		case ItemPool.RONALD_SHELTER_MAP:
			itemName = "Map to Safety Shelter Ronald Prime";
			consumed = true;
			break;

		case ItemPool.GRIMACE_SHELTER_MAP:
			itemName = "Map to Safety Shelter Grimace Prime";
			consumed = true;
			break;

		case ItemPool.D10:
			// Using a single D10 generates a monster.
			if ( item.getCount() != 1 )
			{
				return;
			}
			itemName = "d10";
			// The item IS consumed, but inv_use.php does not
			// redirect to fight.php. Instead, the response text
			// includes Javascript to request fight.php
			consumed = false;
			break;

		default:
			return;
		}

		if ( consumed )
		{
			ResultProcessor.processResult( item.getInstance( -1 ) );
		}

		KoLAdventure.lastVisitedLocation = null;
		KoLAdventure.lastLocationName = null;
		KoLAdventure.lastLocationURL = location;
		Preferences.setString( "lastAdventure", "None" );
		KoLCharacter.updateSelectedLocation( null );

		int adventure = KoLAdventure.getAdventureCount();
		RequestLogger.printLine();
		RequestLogger.printLine( "[" + adventure + "] " + itemName );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "[" + adventure + "] " + itemName );
		GenericRequest.itemMonster = itemName;
	}

	private static AdventureResult sealRitualCandles( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.WRETCHED_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -1 );
		case ItemPool.CUTE_BABY_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -5 );
		case ItemPool.ARMORED_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -10 );
		case ItemPool.ANCIENT_SEAL:
			return ItemPool.get( ItemPool.SEAL_BLUBBER_CANDLE, -3 );
		case ItemPool.SLEEK_SEAL:
		case ItemPool.SHADOWY_SEAL:
		case ItemPool.STINKING_SEAL:
		case ItemPool.CHARRED_SEAL:
		case ItemPool.COLD_SEAL:
		case ItemPool.SLIPPERY_SEAL:
		case ItemPool.DEPLETED_URANIUM_SEAL:
			return ItemPool.get( ItemPool.IMBUED_SEAL_BLUBBER_CANDLE, -1 );
		}
		return null;
	}

	public final void loadResponseFromFile( final String filename )
	{
        loadResponseFromFile( new File( filename ) );
	}

	public final void loadResponseFromFile( final File f )
	{
		BufferedReader buf = FileUtilities.getReader( f );

		try
		{
			String line;
			StringBuilder response = new StringBuilder();

			while ( ( line = buf.readLine() ) != null )
			{
				response.append( line );
			}

            responseCode = 200;
            responseText = response.toString();
		}
		catch ( IOException e )
		{
			// This means simply that there was no file from which
			// to load the data.  Given that this is run during debug
			// tests, only, we can ignore the error.
		}

		try
		{
			buf.close();
		}
		catch ( IOException e )
		{
		}
	}

	@Override
	public String toString()
	{
		return getURLString();
	}

	private static String lastUserAgent = "";

	public static void saveUserAgent( final String agent )
	{
		if ( !agent.equals( GenericRequest.lastUserAgent ) )
		{
			GenericRequest.lastUserAgent = agent;
			Preferences.setString( "lastUserAgent", agent );
		}
	}

	public static void setUserAgent()
	{
		String agent = "";
		if ( Preferences.getBoolean( "useLastUserAgent" ) )
		{
			agent = Preferences.getString( "lastUserAgent" );
		}
		if ( agent.equals( "" ) )
		{
			agent = KoLConstants.VERSION_NAME;
		}
		GenericRequest.setUserAgent( agent );
	}

	public static void setUserAgent( final String agent )
	{
		if ( !agent.equals( GenericRequest.userAgent ) )
		{
			GenericRequest.userAgent = agent;
			System.setProperty( "http.agent", GenericRequest.userAgent );
		}

		// Get rid of obsolete setting
		Preferences.setString( "userAgent", "" );
	}

	public void printRequestProperties()
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Requesting: " + formURL.getProtocol() + "://" + GenericRequest.KOL_HOST + "/" + getDisplayURLString() );

		Map requestProperties = formConnection.getRequestProperties();
		RequestLogger.updateDebugLog( requestProperties.size() + " request properties" );
		RequestLogger.updateDebugLog();

		Iterator iterator = requestProperties.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Entry entry = (Entry) iterator.next();
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}

	public void printHeaderFields()
	{
		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "Retrieved: " + formURL.getProtocol() + "://" + GenericRequest.KOL_HOST + "/" + getDisplayURLString() );
		RequestLogger.updateDebugLog();

		Map headerFields = formConnection.getHeaderFields();
		RequestLogger.updateDebugLog( headerFields.size() + " header fields" );

		Iterator iterator = headerFields.entrySet().iterator();
		while ( iterator.hasNext() )
		{
			Entry entry = (Entry) iterator.next();
			RequestLogger.updateDebugLog( "Field: " + entry.getKey() + " = " + entry.getValue() );
		}

		RequestLogger.updateDebugLog();
	}
}
