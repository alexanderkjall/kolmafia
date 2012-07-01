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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanLoungeRequest
	extends GenericRequest
{
	private static final int SEARCH = 0;

	public static final int KLAW = 1;
	public static final int HOTTUB = 2;
	public static final int POOL_TABLE = 3;
	public static final int CRIMBO_TREE = 4;
	public static final int LOOKING_GLASS = 5;
	public static final int FAX_MACHINE = 6;
	public static final int APRIL_SHOWER = 7;
	public static final int SWIMMING_POOL = 8;

	// Pool options
	public static final int AGGRESSIVE_STANCE = 1;
	public static final int STRATEGIC_STANCE = 2;
	public static final int STYLISH_STANCE = 3;

	// Fax options
	public static final int SEND_FAX = 1;
	public static final int RECEIVE_FAX = 2;

	// Shower options
	public static final int COLD_SHOWER = 1;
	public static final int COOL_SHOWER = 2;
	public static final int LUKEWARM_SHOWER = 3;
	public static final int WARM_SHOWER = 4;
	public static final int HOT_SHOWER = 5;

	// Swimming Pool options
	public static final int CANNONBALL = 1;
	public static final int LAPS = 2;
	public static final int SPRINTS = 3;

	private int action;
	private int option;

	private static final Pattern STANCE_PATTERN = Pattern.compile( "stance=(\\d*)" );
	private static final Pattern TREE_PATTERN = Pattern.compile( "Check back in (\\d+) day" );
	private static final Pattern FAX_PATTERN = Pattern.compile( "preaction=(.+?)fax" );
	private static final Pattern TEMPERATURE_PATTERN = Pattern.compile( "temperature=(\\d*)" );
	private static final Pattern SWIMMING_POOL_PATTERN = Pattern.compile( "subaction=([^&]+)" );
	private static final Pattern LAPS_PATTERN = Pattern.compile( "manage to swim (\\d+) before" );
	private static final Pattern SPRINTS_PATTERN = Pattern.compile( "you do (\\d+) of them" );
	private static final Pattern HOTTUB_PATTERN = Pattern.compile( "hottub(\\d).gif" );

	public static final Object [][] POOL_GAMES = new Object[][]
	{
		{
			"aggressive",
			"muscle",
			"billiards belligerence",
			IntegerPool.get( AGGRESSIVE_STANCE )
		},
		{
			"strategic",
			"mysticality",
			"mental a-cue-ity",
			IntegerPool.get( STRATEGIC_STANCE )
		},
		{
			"stylish",
			"moxie",
			"hustlin'",
			IntegerPool.get( STYLISH_STANCE )
		},
	};

	public static final Object [][] FAX_OPTIONS = new Object[][]
	{
		{
			"send",
			"put",
			IntegerPool.get( SEND_FAX )
		},
		{
			"receive",
			"get",
			IntegerPool.get( RECEIVE_FAX )
		},
	};

	public static final Object [][] SHOWER_OPTIONS = new Object[][]
	{
		{
			"cold",
			"ice",
			IntegerPool.get( COLD_SHOWER )
		},
		{
			"cool",
			"moxie",
			IntegerPool.get( COOL_SHOWER )
		},
		{
			"lukewarm",
			"mysticality",
			IntegerPool.get( LUKEWARM_SHOWER )
		},
		{
			"warm",
			"muscle",
			IntegerPool.get( WARM_SHOWER )
		},
		{
			"hot",
			"mp",
			IntegerPool.get( HOT_SHOWER )
		},
	};

	public static final Object [][] SWIMMING_OPTIONS = new Object[][]
	{
		//{
		//	"cannonball",
		//	"item",
		//	IntegerPool.get( CANNONBALL )
		//},
		{
			"laps",
			"ml",
			IntegerPool.get( LAPS )
		},
		{
			"sprints",
			"noncombat",
			IntegerPool.get( SPRINTS )
		},
	};

	public static int findPoolGame( String tag )
	{
		if ( StringUtilities.isNumeric( tag ) )
		{
			int index = StringUtilities.parseInt( tag );
			if ( index >= 1 && index <= POOL_GAMES.length )
			{
				return index;
			}
		}

		tag = tag.toLowerCase();
		for ( int i = 0; i < POOL_GAMES.length; ++i )
		{
			Object [] game = POOL_GAMES[i];
			Integer index = (Integer) game[3];
			String stance = (String) game[0];
			if ( stance.startsWith( tag ) )
			{
				return index;
			}
			String stat = (String) game[1];
			if ( stat.startsWith( tag ) )
			{
				return index;
			}
			String effect = (String) game[2];
			if ( effect.startsWith( tag ) )
			{
				return index;
			}
		}

		return 0;
	}

	public static int findFaxOption( String tag )
	{
		tag = tag.toLowerCase();
		for ( int i = 0; i < FAX_OPTIONS.length; ++i )
		{
			Object [] faxOption = FAX_OPTIONS[i];
			Integer index = (Integer) faxOption[2];
			String faxCommand0 = (String) faxOption[0];
			if ( faxCommand0.startsWith( tag ) )
			{
				return index;
			}
			String faxCommand1 = (String) faxOption[1];
			if ( faxCommand1.startsWith( tag ) )
			{
				return index;
			}
		}

		return 0;
	}

	public static int findShowerOption( String tag )
	{
		tag = tag.toLowerCase();
		for ( int i = 0; i < SHOWER_OPTIONS.length; ++i )
		{
			Object [] showerOption = SHOWER_OPTIONS[i];
			Integer index = (Integer) showerOption[2];
			String temp = (String) showerOption[0];
			if ( temp.startsWith( tag ) )
			{
				return index;
			}
			String effect = (String) showerOption[1];
			if ( effect.startsWith( tag ) )
			{
				return index;
			}
		}

		return 0;
	}

	public static int findSwimmingOption( String tag )
	{
		tag = tag.toLowerCase();
		for ( int i = 0; i < SWIMMING_OPTIONS.length; ++i )
		{
			Object [] swimmingOption = SWIMMING_OPTIONS[i];
			Integer index = (Integer) swimmingOption[2];
			String action = (String) swimmingOption[0];
			if ( action.startsWith( tag ) )
			{
				return index;
			}
			String effect = (String) swimmingOption[1];
			if ( effect.startsWith( tag ) )
			{
				return index;
			}
		}

		return 0;
	}

	public static String prettyStanceName( final int stance )
	{
		switch ( stance )
		{
		case AGGRESSIVE_STANCE:
			return "an aggressive stance";
		case STRATEGIC_STANCE:
			return "a strategic stance";
		case STYLISH_STANCE:
			return "a stylish stance";
		}
		return "an unknown stance";
	}

	public static String prettyFaxCommand( final int faxCommand )
	{
		switch ( faxCommand )
		{
		case SEND_FAX:
			return "Sending a fax.";
		case RECEIVE_FAX:
			return "Receiving a fax.";
		}
		return "Unknown fax command.";
	}

	public static String prettyTemperatureName( final int temp )
	{
		switch ( temp )
		{
		case COLD_SHOWER:
			return "a cold";
		case COOL_SHOWER:
			return "a cool";
		case LUKEWARM_SHOWER:
			return "a lukewarm";
		case WARM_SHOWER:
			return "a warm";
		case HOT_SHOWER:
			return "a hot";
		}
		return "an unknown";
	}

	public static String prettySwimmingName( final int action )
	{
		switch ( action )
		{
		case CANNONBALL:
			return "cannonball";
		case LAPS:
			return "swim laps";
		case SPRINTS:
			return "do submarine sprints";
		}
		return "do something";
	}

	/**
	 * Constructs a new <code>ClanLoungeRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	private ClanLoungeRequest()
	{
		this( SEARCH );
	}

	public ClanLoungeRequest( final int action )
	{
		super( "clan_viplounge.php" );
		this.action = action;
	}

	public ClanLoungeRequest( final int action, final int option )
	{
		super( "clan_viplounge.php" );
		this.action = action;
		this.option = option;
	}

	public static final AdventureResult VIP_KEY = ItemPool.get( ItemPool.VIP_LOUNGE_KEY, 1 );
	private static final GenericRequest VIP_KEY_REQUEST =
		new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[] { ClanLoungeRequest.VIP_KEY } );
	private static final GenericRequest VISIT_REQUEST = new ClanLoungeRequest();

	private static void pullVIPKey()
	{
		if ( VIP_KEY.getCount( KoLConstants.inventory ) > 0 )
		{
			return;
		}

		// If you have a VIP Lounge Key in storage, pull it.
		if ( VIP_KEY.getCount( KoLConstants.freepulls ) > 0 )
		{
			RequestThread.postRequest( VIP_KEY_REQUEST );
		}
	}

	public static boolean visitLounge()
	{
		// Pull a key from storage, if necessary
		ClanLoungeRequest.pullVIPKey();

		// If we have no Clan VIP Lounge key, nothing to do
		if ( VIP_KEY.getCount( KoLConstants.inventory ) == 0 )
		{
			return false;
		}

		RequestThread.postRequest( VISIT_REQUEST );

		// If you are not in a clan, KoL redirects you to
		// clan_signup.php - which we do not follow.
		return VISIT_REQUEST.redirectLocation == null;
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public static String equipmentName( final String urlString )
	{
		if ( urlString.contains( "klaw" ) )
		{
			return "Deluxe Mr. Klaw \"Skill\" Crane Game";
		}
		if ( urlString.contains( "hottub" ) )
		{
			return "Relaxing Hot Tub";
		}
		if ( urlString.contains( "pooltable" ) )
		{
			return "Pool Table";
		}
		if ( urlString.contains( "crimbotree" ) )
		{
			return "Crimbo Tree";
		}
		if ( urlString.contains( "lookingglass" ) )
		{
			return "Looking Glass";
		}
		if ( urlString.contains( "faxmachine" ) )
		{
			return "Fax Machine";
		}
		if ( urlString.contains( "action=shower" ) )
		{
			return "April Shower";
		}
		if ( urlString.contains( "action=swimmingpool" ) )
		{
			return "Swimming Pool";
		}
		return null;
	}

	private static String equipmentVisit( final String urlString )
	{
		String name = ClanLoungeRequest.equipmentName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in clan VIP lounge";
		}
		return null;
	}

	@Override
	public void run()
	{
		switch ( action )
		{
		case ClanLoungeRequest.SEARCH:
			break;

		case ClanLoungeRequest.KLAW:
            constructURLString( "clan_viplounge.php" );
            addFormField( "action", "klaw" );
			break;

		case ClanLoungeRequest.HOTTUB:
            constructURLString( "clan_viplounge.php" );
            addFormField( "action", "hottub" );
			break;

		case ClanLoungeRequest.POOL_TABLE:
			RequestLogger.printLine( "Approaching pool table with " + ClanLoungeRequest.prettyStanceName( option ) + "." );

            constructURLString( "clan_viplounge.php" );
			if ( option != 0 )
			{
                addFormField( "preaction", "poolgame" );
                addFormField( "stance", String.valueOf( option ) );
			}
			else
			{
                addFormField( "action", "pooltable" );
			}
			break;

		case ClanLoungeRequest.CRIMBO_TREE:
            constructURLString( "clan_viplounge.php" );
            addFormField( "action", "crimbotree" );
			break;

		case ClanLoungeRequest.LOOKING_GLASS:
            constructURLString( "clan_viplounge.php" );
            addFormField( "action", "lookingglass" );
			break;

		case ClanLoungeRequest.FAX_MACHINE:
            constructURLString( "clan_viplounge.php" );
			switch ( option )
			{
			case SEND_FAX:
				KoLmafia.updateDisplay( "Sending a fax." );
                addFormField( "preaction", "sendfax" );
				break;
			case RECEIVE_FAX:
				KoLmafia.updateDisplay( "Receiving a fax." );
                addFormField( "preaction", "receivefax" );
				break;
			default:
                addFormField( "action", "faxmachine" );
				break;
			}
			break;

		case ClanLoungeRequest.APRIL_SHOWER:
			RequestLogger.printLine( "Let's take " + ClanLoungeRequest.prettyTemperatureName( option ) + " shower." );

            constructURLString( "clan_viplounge.php" );
			if ( option != 0 )
			{
                addFormField( "preaction", "takeshower" );
                addFormField( "temperature", String.valueOf( option ) );
			}
			else
			{
                addFormField( "action", "shower" );
			}
			break;

		case ClanLoungeRequest.SWIMMING_POOL:
			RequestLogger.printLine( "Let's " + ClanLoungeRequest.prettySwimmingName( option ) + " in the swimming pool." );

            constructURLString( "clan_viplounge.php" );
			switch ( option )
			{
			case CANNONBALL:
                addFormField( "preaction", "goswimming" );
                addFormField( "subaction", "screwaround" );
				break;
			case LAPS:
                addFormField( "preaction", "goswimming" );
                addFormField( "subaction", "laps" );
				break;
			case SPRINTS:
                addFormField( "preaction", "goswimming" );
                addFormField( "subaction", "submarine" );
				break;
			default:
                addFormField( "action", "swimmingpool" );
				break;
			}
			break;

		default:
			break;
		}

		super.run();

		if ( redirectLocation != null && redirectLocation.equals( "clan_signup.php" ) )
		{
			RequestLogger.printLine( "You don't seem to be in a clan!" );
			return;
		}

		switch ( action )
		{
		case ClanLoungeRequest.POOL_TABLE:
			if ( responseText.contains( "You skillfully defeat" ) )
			{
				RequestLogger.printLine( "You won the pool game!" );
			}
			else if ( responseText.contains( "You play a game of pool against yourself" ) )
			{
				RequestLogger.printLine( "You beat yourself at pool. Is that a win or a loss?" );
			}
			else if ( responseText.contains( "you are unable to defeat" ) )
			{
				RequestLogger.printLine( "You lost. Boo hoo." );
			}
			else if ( responseText.contains( "kind of pooled out" ) )
			{
				RequestLogger.printLine( "You decided not to play." );
			}
			// Those things are old news.  You only care about the <i>latest</i> gadgets.
			else if ( responseText.contains( "Those things are old news" ) )
			{
				KoLmafia.updateDisplay( "Boring! Nobody plays <i>that</i> any more." );
			}
			else
			{
				RequestLogger.printLine( "Huh? Unknown response." );
			}
			break;

		case ClanLoungeRequest.FAX_MACHINE:
			if ( responseText.contains( "Your photocopy slowly slides into the machine" ) )
			{
				String monster = Preferences.getString( "photocopyMonster" );
				if ( monster != "" )
				{
					KoLmafia.updateDisplay( "You load your photocopied " + monster + " in the fax machine." );
				}
				else
				{
					KoLmafia.updateDisplay( "You load your photocopied monster in the fax machine." );
				}
			}
			else if ( responseText.contains( "just be a blank sheet of paper" ) )
			{
				KoLmafia.updateDisplay( "Your fax machine doesn't have any monster." );
			}
			else if ( responseText.contains( "top half of a document prints out" ) )
			{
				// the message is printed by parseResponse()
			}
			else if ( responseText.contains( "waiting for an important fax" ) )
			{
				KoLmafia.updateDisplay( "You already had a photocopied monster in your inventory." );
			}
			// Those things are old news.  You only care about the <i>latest</i> gadgets.
			else if ( responseText.contains( "Those things are old news" ) )
			{
				KoLmafia.updateDisplay( "The fax machine is <i>so</i> last year." );
			}
			// You approach the fax machine.  Loathing wells up
			// within you and fear clutches at your heart...  What
			// do you want to do?
			else if ( responseText.contains( "What do you want to do?" ) )
			{
				// Simple visit.
			}
			else
			{
				KoLmafia.updateDisplay( "Huh? Unknown response." );
			}
			break;

		case ClanLoungeRequest.APRIL_SHOWER:
			if ( responseText.contains( "this is way too hot" ) )
			{
				RequestLogger.printLine( "You took a hot shower." );
			}
			else if ( responseText.contains( "relaxes your muscles" ) )
			{
				RequestLogger.printLine( "You took a warm shower." );
			}
			else if ( responseText.contains( "mind expands" ) )
			{
				RequestLogger.printLine( "You took a lukewarm shower." );
			}
			else if ( responseText.contains( "your goosebumps absorb" ) )
			{
				RequestLogger.printLine( "You took a cool shower." );
			}
			else if ( responseText.contains( "shards of frosty double-ice" ) )
			{
				RequestLogger.printLine( "You took a cold shower." );
			}
			else if ( responseText.contains( "already had a shower today" ) ||
                    responseText.contains( "<table><tr><td></td></tr></table>" ) )
			{
				RequestLogger.printLine( "You already took a shower today." );
			}
			else if ( responseText.contains( "Shower!" ) )
			{
				// Simple visit.
			}
			else
			{
				RequestLogger.printLine( "Huh? Unknown response." );
			}
			break;

		case ClanLoungeRequest.SWIMMING_POOL:
			if ( responseText.contains( "Screwing Around" ) )
			{
				RequestLogger.printLine( "You start screwing around in the swimming pool." );
			}
			else if ( responseText.contains( "manage to swim" ) )
			{
				// the message is printed by parseResponse()
			}
			else if ( responseText.contains( "doing submarine sprints" ) )
			{
				// the message is printed by parseResponse()
			}
			else if ( responseText.contains( "already worked out in the pool today" ) ||
                    responseText.contains( "<table><tr><td></td></tr></table>" ) )
			{
				RequestLogger.printLine( "You already worked out in the pool today." );
			}
			else if ( responseText.contains( "change into your swimsuit" ) )
			{
				// Simple visit.
			}
			else
			{
				RequestLogger.printLine( "Huh? Unknown response." );
			}
			break;
		}
	}

	@Override
	public void processResults()
	{
		ClanLoungeRequest.parseResponse( getURLString(), responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) || responseText == null )
		{
			return;
		}
		
		Matcher hottubMatcher = HOTTUB_PATTERN.matcher( responseText );
		if ( hottubMatcher.find() )
		{
			Preferences.setInteger( "_hotTubSoaks", 5 - Integer.parseInt( hottubMatcher.group( 1 ) ) );
		}

		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// For a simple visit, look at the Crimbo tree and report on
		// whether there is a present waiting.
		if ( action == null )
		{
			if ( responseText.contains( "tree5.gif" ) )
			{
				Preferences.setInteger( "crimboTreeDays", 0 );
				Preferences.setBoolean( "_crimboTree", true );
				RequestLogger.printLine( "You have a present under the Crimbo tree in your clan's VIP lounge!" );
			}
			else if ( responseText.contains( "crimbotree" ) )
			{
				if( !Preferences.getBoolean( "_crimboTree" ) )
				{
					ClanLoungeRequest request;
					request = new ClanLoungeRequest( ClanLoungeRequest.CRIMBO_TREE );
					Preferences.setBoolean( "_crimboTree", true );
					request.run();
				}
			}
			else
			{
				Preferences.setBoolean( "_crimboTree", false );
			}
			return;
		}

		if ( action.equals( "hottub" ) )
		{
			// No action needed here because this is handled already
			return;
		}

		if ( action.equals( "klaw" ) )
		{
			// You carefully guide the claw over a prize and press
			// the button (which is mahogany inlaid with
			// mother-of-pearl -- very nice!) -- the claw slowly
			// descends...
			if ( responseText.contains( "claw slowly descends" ) )
			{
				Preferences.increment( "_deluxeKlawSummons", 1 );
			}
			// You probably shouldn't play with this machine any
			// more today -- you wouldn't want to look greedy in
			// front of the other VIPs, would you?
			else if ( responseText.contains( "you wouldn't want to look greedy" ) )
			{
				Preferences.setInteger( "_deluxeKlawSummons", 3 );
			}

			return;
		}

		if ( action.equals( "pooltable" ) )
		{
			// You've already played quite a bit of pool today, so
			// you just watch with your hands in your pockets.

			if ( responseText.contains( "hands in your pockets" ) )
			{
				Preferences.setInteger( "_poolGames", 3 );
			}

			return;
		}

		if ( action.equals( "poolgame" ) )
		{
			// You skillfully defeat (player) and take control of
			// the table. Go you!
			//
			// You play a game of pool against yourself.
			// Unsurprisingly, you win! Inevitably, you lose.
			//
			// Try as you might, you are unable to defeat
			// (player). Ah well. You gave it your best.

			if ( responseText.contains( "take control of the table" ) ||
                    responseText.contains( "play a game of pool against yourself" ) ||
                    responseText.contains( "you are unable to defeat" ) )
			{
				Preferences.increment( "_poolGames", 1, 3, false );
			}

			// You're kind of pooled out for today. Maybe you'll be
			// in the mood to play again tomorrow.
			else if ( responseText.contains( "pooled out for today" ) )
			{
				Preferences.setInteger( "_poolGames", 3 );
			}

			return;
		}

		if ( action.equals( "crimbotree" ) )
		{
			// You look under the Crimbo Tree and find a present
			// with your name on it! You excitedly tear it open.
			if ( responseText.contains( "You look under the Crimbo Tree and find a present" ) )
			{
				Preferences.setInteger( "crimboTreeDays", 7 );
			}
			else if ( responseText.contains( "Check back tomorrow" ) )
			{
				Preferences.setInteger( "crimboTreeDays", 1 );
			}
			else if ( responseText.contains( "There's nothing under the Crimbo Tree" ) )
			{
				int ctd = Preferences.getInteger( "crimboTreeDays" );
				String groupStr = "";
				Matcher m = TREE_PATTERN.matcher( responseText );
				boolean matchFound = m.find();
				if( matchFound )
				{
					for (int i=0; i<=m.groupCount(); i++)
					{
						groupStr = m.group(i);
						if( !StringUtilities.isNumeric(groupStr) ) continue;
						ctd = Integer.parseInt( groupStr );
						Preferences.setInteger( "crimboTreeDays", ctd );
						return;
					}
				}
			}

			return;
		}

		if ( action.equals( "lookingglass" ) )
		{
			Preferences.setBoolean( "_lookingGlass", true );
			return;
		}

		if ( action.equals( "faxmachine" ) )
		{
			return;
		}

		if ( action.equals( "sendfax" ) )
		{
			if ( responseText.contains( "Your photocopy slowly slides into the machine" ) )
			{
				ResultProcessor.processItem( ItemPool.PHOTOCOPIED_MONSTER, -1 );
				Preferences.setString( "photocopyMonster", "" );
				return;
			}

			return;
		}

		if ( action.equals( "receivefax" ) )
		{
			if ( responseText.contains( "top half of a document prints out" ) )
			{
				String description = DebugDatabase.rawItemDescriptionText( ItemPool.PHOTOCOPIED_MONSTER, true );
				ConsequenceManager.parseItemDesc( ItemDatabase.getDescriptionId( ItemPool.PHOTOCOPIED_MONSTER ), description );

				String monster = Preferences.getString( "photocopyMonster" );
				if ( monster.equals( "" ) )
				{
					monster = "monster";
				}
				KoLmafia.updateDisplay( "You receive a photocopied " + monster + " from the fax machine." );
			}
			return;
		}

		if ( action.equals( "shower" ) )
		{
			if ( responseText.contains( "already had a shower today" ) )
			{
				Preferences.setBoolean( "_aprilShower", true );
			}
			return;
		}

		if ( action.equals( "takeshower" ) )
		{
			if ( responseText.contains( "this is way too hot" ) ||
                    responseText.contains( "relaxes your muscles" ) ||
                    responseText.contains( "mind expands" ) ||
                    responseText.contains( "your goosebumps absorb" ) ||
                    responseText.contains( "shards of frosty double-ice" ) ||
                    responseText.contains( "already had a shower today" ) ||
                    responseText.contains( "<table><tr><td></td></tr></table>" ) )
			{
				Preferences.setBoolean( "_aprilShower", true );
				ResponseTextParser.learnRecipe( urlString, responseText );
			}
			return;
		}

		if ( action.equals( "swimmingpool" ) )
		{
			if ( responseText.contains( "already worked out in the pool today" ) )
			{
				Preferences.setBoolean( "_olympicSwimmingPool", true );
			}
			return;
		}

		if ( action.equals( "goswimming" ) )
		{
			if ( responseText.contains( "<table><tr><td></td></tr></table>" ) )
			{
				Preferences.setBoolean( "_olympicSwimmingPool", true );
				return;
			}
			
			Matcher m = LAPS_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				KoLmafia.updateDisplay( "You swam " + m.group(1) + " laps." );
				Preferences.setBoolean( "_olympicSwimmingPool", true );
				return;
			}

			m = SPRINTS_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				KoLmafia.updateDisplay( "You did " + m.group(1) + " submarine sprints." );
				Preferences.setBoolean( "_olympicSwimmingPool", true );
				return;
			}
			return;
		}
	}

	public static void getBreakfast()
	{
		// No Clan Lounge in Bad Moon
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated() )
		{
			return;
		}

		// Visit the lounge to see what furniture is available
		if ( !visitLounge() )
		{
			return;
		}

		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.
		//
		// Unlike the regular Klaw, there is no message to tell you
		// that you are done for the day except when you try one too
		// many times: "You probably shouldn't play with this machine
		// any more today -- you wouldn't want to look greedy in front
		// of the other VIPs, would you?"

		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.KLAW );
		while ( Preferences.getInteger( "_deluxeKlawSummons" ) < 3 )
		{
			request.run();
		}

		// Not every clan has a looking glass
		if ( VISIT_REQUEST.responseText.contains( "lookingglass.gif" ) &&
		     !Preferences.getBoolean( "_lookingGlass" ) )
		{
			request = new ClanLoungeRequest( ClanLoungeRequest.LOOKING_GLASS );
			request.run();
		}

		// Not every clan has a crimbo tree
		if ( VISIT_REQUEST.responseText.contains( "crimbotree" ) )
		{
			Preferences.setBoolean( "_crimboTree", true );
		}

		if ( VISIT_REQUEST.responseText.contains( "tree5.gif" ) )
		{
			// Get the crimbo gift now whenever breakfast is run, since there is
			// no reason not to anymore.
			request = new ClanLoungeRequest( ClanLoungeRequest.CRIMBO_TREE );
			request.run();
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) )
		{
			return false;
		}

		String message = ClanLoungeRequest.equipmentVisit( urlString );

		if ( message == null )
		{
			String action = GenericRequest.getAction( urlString );
			if ( action.equals( "poolgame" ) )
			{
				Matcher m = STANCE_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				int stance = StringUtilities.parseInt( m.group(1) );
				if ( stance < 1 || stance > POOL_GAMES.length )
				{
					return false;
				}
				message = "pool " + (String)POOL_GAMES[ stance - 1 ][0];
			}
			else if ( action.equals( "sendfax" ) ||
				  action.equals( "receivefax" ) )
			{
				Matcher m = FAX_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				String faxCommand = m.group(1) ;
				if ( !faxCommand.equals( "send") && !faxCommand.equals( "receive" ) )
				{
					return false;
				}
				message = "fax " + faxCommand;
			}
			else if ( action.equals( "takeshower" ) )
			{
				Matcher m = TEMPERATURE_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				int temp = StringUtilities.parseInt( m.group(1) );
				if ( temp < 1 || temp > SHOWER_OPTIONS.length )
				{
					return false;
				}
				message = "shower " + (String)SHOWER_OPTIONS[ temp - 1 ][0];
			}
			else if ( action.equals( "goswimming" ) )
			{
				Matcher m = SWIMMING_POOL_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				String poolCommand = m.group(1) ;
				if ( !poolCommand.equals( "screwaround") &&
				     !poolCommand.equals( "laps" ) &&
				     !poolCommand.equals( "submarine" ) )
				{
					return false;
				}
				message = "swimming pool " + poolCommand;
			}
			else
			{
				return false;
			}
		}
		else
		{
			RequestLogger.printLine( message );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
