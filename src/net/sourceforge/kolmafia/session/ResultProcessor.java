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

package net.sourceforge.kolmafia.session;

import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class ResultProcessor
{
	private static Pattern DISCARD_PATTERN = Pattern.compile( "You discard your (.*?)\\." );
	private static Pattern INT_PATTERN = Pattern.compile( "([\\d]+)" );
	private static Pattern TRAILING_INT_PATTERN = Pattern.compile( "(.*?)(?:\\s*\\((\\d+)\\))?" );

	private static boolean receivedClover = false;
	private static boolean receivedDisassembledClover = false;
	private static boolean autoCrafting = false;

	// This number changes every time an item is processed, and can be used
	// by other code to tell if an item is received, without necessarily
	// knowing which item it was.

	public static int itemSequenceCount = 0;

	public static boolean receivedClover()
	{
		return ResultProcessor.receivedClover;
	}

	public static boolean receivedDisassembledClover()
	{
		return ResultProcessor.receivedDisassembledClover;
	}

	public static boolean shouldDisassembleClovers( String formURLString )
	{
		return ResultProcessor.receivedClover &&
		       !GenericRequest.ascending &&
		       FightRequest.getCurrentRound() == 0 &&
		       Preferences.getBoolean( "cloverProtectActive" ) &&
		       isCloverURL( formURLString );
	}

	public static boolean disassembledClovers( String formURLString )
	{
		return ResultProcessor.receivedDisassembledClover &&
		       Preferences.getBoolean( "cloverProtectActive" ) &&
		       isCloverURL( formURLString );
	}

	private static boolean isCloverURL( String formURLString )
	{
		return formURLString.startsWith( "adventure.php" ) ||
			formURLString.startsWith( "choice.php" ) ||
			formURLString.startsWith( "hermit.php" ) ||
			formURLString.startsWith( "mallstore.php" ) ||
			formURLString.startsWith( "town_fleamarket.php" ) ||
			formURLString.startsWith( "barrel.php" ) ||
			formURLString.startsWith( "shore.php" ) ||
			// Marmot sign can give you a clover after a fight
			formURLString.startsWith( "fight.php" ) ||
			// Using a 31337 scroll
                formURLString.contains( "whichitem=553" ) ||
			// ...without in-line loading can redirect to inventory
			( formURLString.startsWith( "inventory.php" ) &&
                    formURLString.contains( "action=message" ));
	}

	public static Pattern ITEM_TABLE_PATTERN = Pattern.compile( "<table class=\"item\".*?rel=\"(.*?)\".*?title=\"(.*?)\".*?descitem\\(([\\d]*)\\).*?</table>" );
	public static Pattern BOLD_NAME_PATTERN = Pattern.compile( "<b>([^<]*)</b>" );

	public static void registerNewItems( String results )
	{
		// Results now come in like this:
		//
		// <table class="item" style="float: none" rel="id=617&s=137&q=0&d=1&g=0&t=1&n=1&m=1&u=u">
		// <tr><td><img src="http://images.kingdomofloathing.com/itemimages/rcandy.gif"
		// alt="Angry Farmer candy" title="Angry Farmer candy" class=hand onClick='descitem(893169457)'></td>
		// <td valign=center class=effect>You acquire an item: <b>Angry Farmer candy</b></td></tr></table>
		//
		// Or, in haiku:
		//
		// <table class="item" style="float: none" rel="id=83&s=5&q=0&d=1&g=0&t=1&n=1&m=0&u=.">
		// <tr><td><img src="http://images.kingdomofloathing.com/itemimages/rustyshaft.gif"
		// alt="rusty metal shaft" title="rusty metal shaft" class=hand onClick='descitem(228339790)'></td>
		// <td valign=center class=effect><b>rusty metal shaft</b><br>was once your foe's, is now yours.<br>
		// Beaters-up, keepers.</td></tr></table>
		//
		// Pre-process all such matches and register new items

		Matcher itemMatcher = ResultProcessor.ITEM_TABLE_PATTERN.matcher( results );
		while ( itemMatcher.find() )
		{
			String relString = itemMatcher.group(1);
			String itemName = itemMatcher.group(2);
			String descId = itemMatcher.group(3);

			// If we already know this descid, known item.
			if ( ItemDatabase.getItemIdFromDescription( descId ) != -1 )
			{
				continue;
			}

			Matcher boldMatcher = ResultProcessor.BOLD_NAME_PATTERN.matcher( itemMatcher.group(0) );
			String items = boldMatcher.find() ? boldMatcher.group(1) : null;
			ItemDatabase.registerItem( itemName, descId, relString, items );
		}
	}

	public static boolean processResults( boolean combatResults, String results )
	{
		return ResultProcessor.processResults( combatResults, results, null );
	}

	public static boolean processResults( boolean combatResults, String results, List<AdventureResult> data )
	{
		ResultProcessor.receivedClover = false;
		ResultProcessor.receivedDisassembledClover = false;

		if ( data == null && RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing results..." );
		}

		ResultProcessor.registerNewItems( results );

		boolean requiresRefresh = false;

		try
		{
			requiresRefresh = processNormalResults( combatResults, results, data );
		}
		finally
		{
			if ( data == null )
			{
				KoLmafia.applyEffects();
			}
		}

		return requiresRefresh;
	}

	public static boolean processNormalResults( boolean combatResults, String results, List<AdventureResult> data )
	{
		String plainTextResult = KoLConstants.ANYTAG_BUT_ITALIC_PATTERN.matcher( results ).replaceAll( KoLConstants.LINE_BREAK );

		if ( data == null )
		{
			ResultProcessor.processFamiliarWeightGain( plainTextResult );
		}

		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, KoLConstants.LINE_BREAK );
		boolean shouldRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			shouldRefresh |= ResultProcessor.processNextResult( combatResults, parsedResults, data );
		}

		return shouldRefresh;
	}

	public static boolean processFamiliarWeightGain( final String results )
	{
		if ( results.contains( "gains a pound" ) ||
		     // The following are Haiku results
                results.contains( "gained a pound" ) ||
                results.contains( "puts on weight" ) ||
                results.contains( "gaining weight" ) ||
		     // The following are Anapest results
                results.contains( "just got heavier" ) ||
                results.contains( "put on some weight" ) )
		{
			KoLCharacter.incrementFamilarWeight();

			FamiliarData familiar = KoLCharacter.getFamiliar();
			String fam1 = familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace();

			String message = "Your familiar gains a pound: " + fam1;
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message	 );
			return true;
		}

		return false;
	}

	private static boolean processNextResult( boolean combatResults, StringTokenizer parsedResults, List<AdventureResult> data )
	{
		String lastToken = parsedResults.nextToken();

		// Skip bogus lead necklace drops from the Baby Bugged Bugbear

		if ( lastToken.equals( " Parse error (function not found) in arena.php line 2225" ) )
		{
			parsedResults.nextToken();
			return false;
		}

		// Skip skill acquisition - it's followed by a boldface
		// which makes the parser think it's found an item.

		if ( lastToken.contains( "You acquire a skill" ) ||
                lastToken.contains( "You learn a skill" ) ||
                lastToken.contains( "You gain a skill" ) ||
                lastToken.contains( "You have learned a skill" ) )
		{
			return false;
		}

		String acquisition = lastToken.trim();

		if ( acquisition.startsWith( "You acquire" ) )
		{
			if ( acquisition.contains( "clan trophy" ) )
			{
				return false;
			}

			if ( !acquisition.contains( "effect" ) )
			{
				ResultProcessor.processItem( combatResults, parsedResults, acquisition, data );
				return false;
			}

			return ResultProcessor.processEffect( parsedResults, acquisition, data );
		}

		if ( acquisition.startsWith( "You lose an effect" ) || acquisition.startsWith( "You lose some of an effect" ) )
		{
			return ResultProcessor.processEffect( parsedResults, acquisition, data );
		}

		if ( lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " ) || lastToken.startsWith( "You spent " ) )
		{
			return ResultProcessor.processGainLoss( lastToken, data );
		}

		if ( lastToken.startsWith( "You discard" ) )
		{
			ResultProcessor.processDiscard( lastToken );
		}

		return false;
	}

	private static void processItem( boolean combatResults, StringTokenizer parsedResults, String acquisition, List<AdventureResult> data )
	{
		String item = parsedResults.nextToken();

		if ( acquisition.contains( "an item" ) )
		{
			AdventureResult result = ItemPool.get( item, 1 );

			if ( result.getItemId() == -1 )
			{
				RequestLogger.printLine( "Unrecognized item found: " + item );
			}

			ResultProcessor.processItem( combatResults, acquisition, result, data );
			return;
		}

		// The name of the item follows the number that appears after
		// the first index.

		int spaceIndex = item.indexOf( " " );

		String countString = "";
		String itemName;

		if ( spaceIndex != -1 )
		{
			countString = item.substring( 0, spaceIndex );
			itemName = item.substring( spaceIndex ).trim();
		}
		else
		{
			itemName = item;
		}

		if ( !StringUtilities.isNumeric( countString ) )
		{
			countString = "1";
			itemName = item;
		}

		int itemCount = StringUtilities.parseInt( countString );

		// If we got more than one, do substring matching. This might
		// allow recognition of an unknown (or changed) plural form.

		int itemId = ItemDatabase.getItemId( itemName, itemCount, itemCount > 1 );
		AdventureResult result;

		if ( itemId < 0 )
		{
			RequestLogger.printLine( "Unrecognized item found: " + item );
			result = AdventureResult.tallyItem( itemName, itemCount, false );
		}
		else
		{
			result = new AdventureResult( itemId, itemCount );
		}

		ResultProcessor.processItem( combatResults, acquisition, result, data );
	}

	public static void processItem( boolean combatResults, String acquisition, AdventureResult result, List<AdventureResult> data )
	{
		if ( data != null )
		{
			AdventureResult.addResultToList( data, result );
			return;
		}

		String message = acquisition + " " + result.toString();

		RequestLogger.printLine( message );
		if ( Preferences.getBoolean( "logAcquiredItems" ) )
		{
			RequestLogger.updateSessionLog( message );
		}

		ResultProcessor.processResult( combatResults, result );

		++ResultProcessor.itemSequenceCount;
	}

	private static boolean processEffect( StringTokenizer parsedResults, String acquisition, List<AdventureResult> data )
	{
		if ( data != null )
		{
			return false;
		}

		String effectName = parsedResults.nextToken();
		String message;

		if ( acquisition.startsWith( "You lose" ) )
		{
			message = acquisition + " " + effectName;
		}
		else
		{
			String lastToken = parsedResults.nextToken();
			message = acquisition + " " + effectName + " " + lastToken;
		}

		return ResultProcessor.processEffect( effectName, message );
	}

	public static boolean processEffect( String effectName, String message )
	{
		RequestLogger.printLine( message );

		if ( Preferences.getBoolean( "logStatusEffects" ) )
		{
			RequestLogger.updateSessionLog( message );
		}

		if ( message.startsWith( "You lose" ) )
		{
			AdventureResult result = EffectPool.get( effectName );
			AdventureResult.removeResultFromList( KoLConstants.recentEffects, result );
			AdventureResult.removeResultFromList( KoLConstants.activeEffects, result );
			// If you lose Inigo's, what you can craft changes

			if ( effectName.equals( Effect.INIGO.effectName() ) )
			{
				ConcoctionDatabase.setRefreshNeeded( true );
			}

			return true;
		}

		if ( message.contains( "duration" ) )
		{
			Matcher m = INT_PATTERN.matcher( message );
			if ( m.find() )
			{
				int duration = StringUtilities.parseInt( m.group(1) );
				return ResultProcessor.parseEffect( effectName + " (" + duration + ")" );
			}
		}

		ResultProcessor.parseEffect( effectName );
		return false;
	}

	public static boolean processGainLoss( String lastToken, final List<AdventureResult> data )
	{
		int periodIndex = lastToken.indexOf( "." );
		if ( periodIndex != -1 )
		{
			lastToken = lastToken.substring( 0, periodIndex );
		}

		int parenIndex = lastToken.indexOf( "(" );
		if ( parenIndex != -1 )
		{
			lastToken = lastToken.substring( 0, parenIndex );
		}

		lastToken = lastToken.trim();

		if ( lastToken.contains( "Meat" ) )
		{
			return ResultProcessor.processMeat( lastToken, data );
		}

		if ( data != null )
		{
			return false;
		}

		if ( lastToken.startsWith( "You gain a" ) || lastToken.startsWith( "You gain some" ) )
		{
			RequestLogger.printLine( lastToken );
			if ( Preferences.getBoolean( "logStatGains" ) )
			{
				RequestLogger.updateSessionLog( lastToken );
			}
			// Update Hatter deed since new hats may now be equippable
			PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );

			return true;
		}

		return ResultProcessor.processStatGain( lastToken, data );
	}

	private static boolean possibleMeatDrop( int drop, int bonus )
	{
		float rate = (KoLCharacter.getMeatDropPercentAdjustment() + 100 + bonus) / 100.0f;
		return Math.floor( Math.ceil( drop / rate ) * rate ) == drop;
	}

	public static boolean processMeat( String text, boolean won, boolean nunnery )
	{
		if ( won && Preferences.getBoolean( "meatDropSpading" ) )
		{
			int drop = ResultProcessor.parseResult( text ).getCount();
			if ( !ResultProcessor.possibleMeatDrop( drop, 0 ) )
			{
				StringBuilder buf = new StringBuilder( "Alert - possible unknown meat bonus:" );
				if ( KoLCharacter.currentNumericModifier( Modifiers.SPORADIC_MEATDROP ) != 0.0f )
				{
					buf.append( " (sporadic!)" );
				}
				if ( KoLCharacter.currentNumericModifier( Modifiers.MEAT_BONUS ) != 0.0f )
				{
					buf.append( " (ant tool!)" );
				}
				for ( int i = 1; i <= 100 && buf.length() < 80; ++i )
				{
					if ( ResultProcessor.possibleMeatDrop( drop, i ) )
					{
						buf.append( " +" );
						buf.append( i );
					}
					if ( ResultProcessor.possibleMeatDrop( drop, -i ) )
					{
						buf.append( " -" );
						buf.append( i );
					}
				}
				RequestLogger.updateSessionLog( "Spade " + buf );
				buf.insert( 0, "<font color=green>\u2660" );
				buf.append( "</font>" );
				RequestLogger.printLine( buf.toString() );
			}
		}

		if ( won && nunnery )
		{
			AdventureResult result = ResultProcessor.parseResult( text );
			IslandDecorator.addNunneryMeat( result );
			return false;
		}

		return ResultProcessor.processMeat( text, null );
	}

	public static boolean processMeat( String lastToken, List<AdventureResult> data )
	{
		AdventureResult result = ResultProcessor.parseResult( lastToken );

		if ( data != null )
		{
			AdventureResult.addResultToList( data, result );
			return false;
		}

		// KoL can tell you that you lose meat - Leprechaun theft,
		// chewing bug vendors, etc. - but you can only lose as much
		// meat as you actually have in inventory.

		int amount = result.getCount();
		int available = KoLCharacter.getAvailableMeat();

		if ( amount < 0 && -amount > available )
		{
			amount = -available;
			lastToken = "You lose " + String.valueOf( -amount ) + " Meat";
			result = new AdventureResult( AdventureResult.MEAT, amount );
		}

		if ( amount == 0 )
		{
			return false;
		}

		RequestLogger.printLine( lastToken );
		if ( Preferences.getBoolean( "logGainMessages" ) )
		{
			RequestLogger.updateSessionLog( lastToken );
		}

		return ResultProcessor.processResult( result );
	}

	public static boolean processStatGain( String lastToken, List<AdventureResult> data )
	{
		if ( data != null )
		{
			return false;
		}

		AdventureResult result = ResultProcessor.parseResult( lastToken );

		RequestLogger.printLine( lastToken );
		if ( Preferences.getBoolean( "logStatGains" ) )
		{
			RequestLogger.updateSessionLog( lastToken );
		}

		return ResultProcessor.processResult( result );
	}

	private static void processDiscard( String lastToken )
	{
		Matcher matcher = ResultProcessor.DISCARD_PATTERN.matcher( lastToken );
		if ( matcher.find() )
		{
			AdventureResult item = new AdventureResult( matcher.group( 1 ), -1, false );
			AdventureResult.addResultToList( KoLConstants.inventory, item );
			AdventureResult.addResultToList( KoLConstants.tally, item );
			switch ( item.getItemId() )
			{
			case ItemPool.INSTANT_KARMA:
				Preferences.increment( "bankedKarma", 11 );
				break;
			}
		}
	}

	public static AdventureResult parseResult( String result )
	{
		result = result.trim();

		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Parsing result: " + result );
		}

		try
		{
			return AdventureResult.parseResult( result );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private static boolean parseEffect( String result )
	{
		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Parsing effect: " + result );
		}

		Matcher m = ResultProcessor.TRAILING_INT_PATTERN.matcher( result );
		int count = 1;
		if ( m.matches() )
		{
			result = m.group( 1 );
			count = StringUtilities.parseInt( m.group( 2 ) );
		}

		return ResultProcessor.processResult( new AdventureResult( result, count, true ) );
	}

	/**
	 * Utility. The method used to process a result. By default, this will
	 * also add an adventure result to the tally. Use this whenever the
	 * result is already known and no additional parsing is needed.
	 *
	 * @param result Result to add to the running tally of adventure results
	 */

	public static boolean processResult( AdventureResult result )
	{
		return ResultProcessor.processResult( false, result );
	}

	public static boolean processResult( boolean combatResults, AdventureResult result )
	{
		// This should not happen, but punt if the result was null.

		if ( result == null )
		{
			return false;
		}

		if ( RequestLogger.isDebugging() )
		{
			RequestLogger.updateDebugLog( "Processing result: " + result );
		}

		String resultName = result.getName();
		boolean shouldRefresh = false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list. Otherwise, add it to the tally.

		if ( result.isItem() )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( result.isStatusEffect() )
		{
			shouldRefresh |= !KoLConstants.activeEffects.contains( result );
			AdventureResult.addResultToList( KoLConstants.recentEffects, result );
		}
		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			// Update substat delta and fullstat delta, if necessary
			int [] counts = result.getCounts();

			// Update AdventureResult.SESSION_SUBSTATS in place
			// Update AdventureResult.SESSION_FULLSTATS in place
			boolean substatChanged = false;
			boolean fullstatChanged = false;

			int count = counts[ 0 ];
			if ( count != 0 )
			{
				long stat = KoLCharacter.getTotalMuscle();
				long diff = KoLCharacter.calculateBasePoints( stat + count ) -
					    KoLCharacter.calculateBasePoints( stat );
				AdventureResult.SESSION_SUBSTATS[ 0 ] += count;
				AdventureResult.SESSION_FULLSTATS[ 0 ] += diff;
				substatChanged = true;
				fullstatChanged |= ( diff != 0 );
			}

			count = counts[ 1 ];
			if ( count != 0 )
			{
				long stat = KoLCharacter.getTotalMysticality();
				long diff = KoLCharacter.calculateBasePoints( stat + count ) -
					    KoLCharacter.calculateBasePoints( stat );
				AdventureResult.SESSION_SUBSTATS[ 1 ] += count;
				AdventureResult.SESSION_FULLSTATS[ 1 ] += diff;
				substatChanged = true;
				fullstatChanged |= ( diff != 0 );
			}

			count = counts[ 2 ];
			if ( count != 0 )
			{
				long stat = KoLCharacter.getTotalMoxie();
				long diff = KoLCharacter.calculateBasePoints( stat + count ) -
					   KoLCharacter.calculateBasePoints( stat );
				AdventureResult.SESSION_SUBSTATS[ 2 ] += count;
				AdventureResult.SESSION_FULLSTATS[ 2 ] += diff;
				substatChanged = true;
				fullstatChanged |= ( diff != 0 );
			}

			int size = KoLConstants.tally.size();
			if ( substatChanged && size > 2 )
			{
				KoLConstants.tally.fireContentsChanged( KoLConstants.tally, 2, 2 );
			}

			if ( fullstatChanged)
			{
				shouldRefresh = true;
				if ( size > 3 )
				{
					KoLConstants.tally.fireContentsChanged( KoLConstants.tally, 3, 3 );
				}
			}
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
			shouldRefresh = true;
		}
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			if ( result.getCount() < 0 )
			{
				TurnCounter.saveCounters();
				AdventureResult.addResultToList( KoLConstants.tally, result.getNegation() );
			}
		}
		else if ( resultName.equals( AdventureResult.CHOICE ) )
		{
			// Don't let ignored choices delay iteration
			KoLmafia.tookChoice = true;
		}

		ResultProcessor.tallyResult( result, true );

		if ( result.isItem() )
		{
			// Do special processing when you get certain items
			ResultProcessor.gainItem( combatResults, result );

			if ( GenericRequest.isBarrelSmash )
			{
				BarrelDecorator.gainItem( result );
			}

			if ( RequestLogger.getLastURLString().startsWith( "fight.php" ) )
			{
				int adv = KoLAdventure.lastAdventureId();
				if ( adv >= AdventurePool.WINE_CELLAR_NORTHWEST && adv <= AdventurePool.WINE_CELLAR_SOUTHEAST )
				{
					CellarDecorator.gainItem( adv, result );
				}
			}

			if ( HermitRequest.isWorthlessItem( result.getItemId() ) )
			{
				result = HermitRequest.WORTHLESS_ITEM.getInstance( result.getCount() );
			}

			return false;
		}

		GoalManager.updateProgress( result );

		return shouldRefresh;
	}

	public static boolean processItem( int itemId, int count )
	{
		return ResultProcessor.processResult( ItemPool.get( itemId, count ) );
	}

	public static void removeItem( int itemId )
	{
		AdventureResult ar = ItemPool.get( itemId, -1 );
		if ( KoLConstants.inventory.contains( ar ) )
		{
			ResultProcessor.processResult( ar );
		}
	}

	public static boolean processMeat( int amount )
	{
		return ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, amount ) );
	}

	public static void processAdventuresLeft( int amount )
	{
		if ( amount != 0 )
		{
			KoLCharacter.setAdventuresLeft( KoLCharacter.getAdventuresLeft() + amount );
		}
	}

	public static void processAdventuresUsed( int amount )
	{
		if ( amount != 0 )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.ADV, -amount ) );
		}
	}

	/**
	 * Processes a result received through adventuring. This places items
	 * inside of inventories and lots of other good stuff.
	 */

	public static void tallyResult( final AdventureResult result, final boolean updateCalculatedLists )
	{
		// Treat the result as normal from this point forward.
		// Figure out which list the skill should be added to
		// and add it to that list.

		String resultName = result.getName();
		if ( resultName == null )
		{
			return;
		}

		if ( result.isItem() )
		{
			AdventureResult.addResultToList( KoLConstants.inventory, result );

			if ( updateCalculatedLists )
			{
				EquipmentManager.processResult( result );
				ConcoctionDatabase.setRefreshNeeded( result.getItemId() );
			}
		}
		else if ( resultName.equals( AdventureResult.HP ) )
		{
			KoLCharacter.setHP(
				KoLCharacter.getCurrentHP() + result.getCount(), KoLCharacter.getMaximumHP(),
				KoLCharacter.getBaseMaxHP() );
		}
		else if ( resultName.equals( AdventureResult.MP ) )
		{
			KoLCharacter.setMP(
				KoLCharacter.getCurrentMP() + result.getCount(), KoLCharacter.getMaximumMP(),
				KoLCharacter.getBaseMaxMP() );
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			KoLCharacter.setAvailableMeat( KoLCharacter.getAvailableMeat() + result.getCount() );
			if ( updateCalculatedLists )
			{
				ConcoctionDatabase.setRefreshNeeded( false );
			}
		}
		else if ( resultName.equals( AdventureResult.ADV ) )
		{
			if ( result.getCount() < 0 )
			{
				AdventureResult[] effectsArray = new AdventureResult[ KoLConstants.activeEffects.size() ];
				KoLConstants.activeEffects.toArray( effectsArray );

				for ( int i = effectsArray.length - 1; i >= 0; --i )
				{
					AdventureResult effect = effectsArray[ i ];
					int duration = effect.getCount();
					if ( duration == Integer.MAX_VALUE )
					{
						// Intrinsic effect
					}
					else if ( duration + result.getCount() <= 0 )
					{
						KoLConstants.activeEffects.remove( i );

						// If you lose Inigo's, what you can craft changes
						if ( effect.getName().equals( Effect.INIGO.effectName() ) )
						{
							ConcoctionDatabase.setRefreshNeeded( true );
						}
					}
					else
					{
						KoLConstants.activeEffects.set( i, effect.getInstance( effect.getCount() + result.getCount() ) );
					}
				}

				KoLCharacter.setCurrentRun( KoLCharacter.getCurrentRun() - result.getCount() );
			}
		}
		else if ( resultName.equals( AdventureResult.DRUNK ) )
		{
			KoLCharacter.setInebriety( KoLCharacter.getInebriety() + result.getCount() );
		}
		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			if ( result.isMuscleGain() )
			{
				KoLCharacter.incrementTotalMuscle( result.getCount() );
			}
			else if ( result.isMysticalityGain() )
			{
				KoLCharacter.incrementTotalMysticality( result.getCount() );
			}
			else if ( result.isMoxieGain() )
			{
				KoLCharacter.incrementTotalMoxie( result.getCount() );
			}
		}
		else if ( resultName.equals( AdventureResult.PVP ) )
		{
			KoLCharacter.setAttacksLeft( KoLCharacter.getAttacksLeft() + result.getCount() );
		}
	}

	private static void gainItem( boolean combatResults, AdventureResult result )
	{
		ConcoctionDatabase.setRefreshNeeded( result.getItemId() );

		// All results, whether positive or negative, are
		// handled here.

		switch ( result.getItemId() )
		{
		case ItemPool.GG_TICKET:
		case ItemPool.SNACK_VOUCHER:
		case ItemPool.LUNAR_ISOTOPE:
		case ItemPool.WORTHLESS_TRINKET:
		case ItemPool.WORTHLESS_GEWGAW:
		case ItemPool.WORTHLESS_KNICK_KNACK:
		case ItemPool.YETI_FUR:
		case ItemPool.LUCRE:
		case ItemPool.SAND_DOLLAR:
		case ItemPool.CRIMBUCK:
		case ItemPool.BONE_CHIPS:
		case ItemPool.CRIMBCO_SCRIP:
		case ItemPool.AWOL_COMMENDATION:
		case ItemPool.MR_ACCESSORY:
		case ItemPool.FAT_LOOT_TOKEN:
		case ItemPool.FUDGECULE:
		case ItemPool.FDKOL_COMMENDATION:
		case ItemPool.TWINKLY_WAD:
			// The Traveling Trader usually wants twinkly wads
		case ItemPool.GG_TOKEN:
			// You can trade tokens for tickets
		case ItemPool.TRANSPORTER_TRANSPONDER:
			// You can go to spaaace with a transponder
			CoinmastersFrame.externalUpdate();
			break;
		}

		// From here on out, only positive results are handled.
		if ( result.getCount() < 0 )
		{
			return;
		}
		
		if ( EquipmentDatabase.isHat( result ) )
		{
			PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );
		}
		
		switch ( result.getItemId() )
		{
		case ItemPool.ROASTED_MARSHMALLOW:
			// Special Yuletide adventures
			if ( KoLAdventure.lastAdventureId() == AdventurePool.YULETIDE )
			{
				ResultProcessor.removeItem( ItemPool.MARSHMALLOW );
			}
			break;

		// Sticker weapons may have been folded from the other form
		case ItemPool.STICKER_SWORD:
			ResultProcessor.removeItem( ItemPool.STICKER_CROSSBOW );
			break;

		case ItemPool.STICKER_CROSSBOW:
			ResultProcessor.removeItem( ItemPool.STICKER_SWORD );
			break;

		case ItemPool.SOCK:
			// If you get a S.O.C.K., you lose all the Immateria
			ResultProcessor.processItem( ItemPool.TISSUE_PAPER_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.TIN_FOIL_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.GAUZE_IMMATERIA, -1 );
			ResultProcessor.processItem( ItemPool.PLASTIC_WRAP_IMMATERIA, -1 );
			break;

		case ItemPool.PIRATE_FLEDGES:
			if ( !ResultProcessor.onlyAutosellDonationsCount && KoLCharacter.inFistcore() )
			{
				// Do you make a donation? You don't get Meat,
				// but there is no message about donating it.
				KoLCharacter.makeCharitableDonation( 3000 );
			}
			QuestDatabase.setQuestProgress( Quest.PIRATE, QuestDatabase.FINISHED );
			break;

		case ItemPool.MACGUFFIN_DIARY:
			// If you get your father's MacGuffin diary, you lose
			// your forged identification documents
			ResultProcessor.processItem( ItemPool.FORGED_ID_DOCUMENTS, -1 );
			// Automatically use the diary to open zones
			RequestThread.postRequest( UseItemRequest.getInstance( result ) );
			break;

		case ItemPool.PALINDROME_BOOK:
			// If you get "I Love Me, Vol. I", you lose (some of)
			// the items you put on the shelves
			ResultProcessor.processItem( ItemPool.PHOTOGRAPH_OF_GOD, -1 );
			ResultProcessor.processItem( ItemPool.HARD_ROCK_CANDY, -1 );
			ResultProcessor.processItem( ItemPool.OSTRICH_EGG, -1 );
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step2" );
			break;

		case ItemPool.MEGA_GEM:
			// If you get the Mega Gem, you lose your wet stunt nut
			// stew
			ResultProcessor.processItem( ItemPool.WET_STUNT_NUT_STEW, -1 );
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step4" );
			break;
			
		case ItemPool.HOLY_MACGUFFIN:
			QuestDatabase.setQuestProgress( Quest.PYRAMID, QuestDatabase.FINISHED );
			break;

		case ItemPool.CONFETTI:
			// If you get the confetti, you lose the Holy MacGuffin
			if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.HOLY_MACGUFFIN, 1 ) ) )
			{
				ResultProcessor.processItem( ItemPool.HOLY_MACGUFFIN, -1 );
				QuestDatabase.setQuestProgress( Quest.PYRAMID, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.MANOR, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.WORSHIP, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.PALINDOME, QuestDatabase.FINISHED );
				QuestDatabase.setQuestProgress( Quest.MACGUFFIN, QuestDatabase.FINISHED );
			}
			break;

		case ItemPool.SPOOKYRAVEN_SPECTACLES:
			// When you get the spectacles, identify dusty bottles.
			// If you have not ascended, you need to put them on -
			// which we leave to the player - but otherwise, they
			// work from inventory.

			// Temporary bandaid until we figure out how this gets
			// out of synch. Not that it's harmful to do this.
			Preferences.setInteger( "lastDustyBottleReset", -1 );

			if ( KoLCharacter.getAscensions() > 0 )
			{
				ItemDatabase.identifyDustyBottles();
			}
			break;

		case ItemPool.MOLYBDENUM_MAGNET:
			// When you get the molybdenum magnet, tell quest handler
			IslandDecorator.startJunkyardQuest();
			break;

		case ItemPool.MOLYBDENUM_HAMMER:
		case ItemPool.MOLYBDENUM_SCREWDRIVER:
		case ItemPool.MOLYBDENUM_PLIERS:
		case ItemPool.MOLYBDENUM_WRENCH:
			// When you get a molybdenum item, tell quest handler
			IslandDecorator.resetGremlinTool();
			break;

		case ItemPool.SPOOKY_BICYCLE_CHAIN:
			if ( combatResults ) QuestDatabase.setQuestIfBetter( Quest.BUGBEAR, "step3" );
			break;

		case ItemPool.RONALD_SHELTER_MAP:
		case ItemPool.GRIMACE_SHELTER_MAP:
			QuestDatabase.setQuestIfBetter( Quest.GENERATOR, "step1" );
			break;

		case ItemPool.SPOOKY_LITTLE_GIRL:
			QuestDatabase.setQuestIfBetter( Quest.GENERATOR, "step2" );
			break;

		case ItemPool.EMU_UNIT:
			// If you get an E.M.U. Unit, you lose all the E.M.U. parts
			ResultProcessor.processItem( ItemPool.EMU_JOYSTICK, -1 );
			ResultProcessor.processItem( ItemPool.EMU_ROCKET, -1 );
			ResultProcessor.processItem( ItemPool.EMU_HELMET, -1 );
			ResultProcessor.processItem( ItemPool.EMU_HARNESS, -1 );
			QuestDatabase.setQuestIfBetter( Quest.GENERATOR, "step3" );
			break;

		case ItemPool.OVERCHARGED_POWER_SPHERE:
		case ItemPool.EL_VIBRATO_HELMET:
		case ItemPool.EL_VIBRATO_SPEAR:
		case ItemPool.EL_VIBRATO_PANTS:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.POWER_SPHERE );
			break;

		case ItemPool.BROKEN_DRONE:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.DRONE );
			break;

		case ItemPool.REPAIRED_DRONE:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.BROKEN_DRONE );
			break;

		case ItemPool.AUGMENTED_DRONE:
			if ( combatResults ) ResultProcessor.removeItem( ItemPool.REPAIRED_DRONE );
			break;

		case ItemPool.TRAPEZOID:
			ResultProcessor.removeItem( ItemPool.POWER_SPHERE );
			break;

		case ItemPool.CITADEL_SATCHEL:
			ResultProcessor.processMeat( -300 );
			break;

		case ItemPool.HAROLDS_BELL:
			ResultProcessor.processItem( ItemPool.HAROLDS_HAMMER, -1 );
			break;

		 // These update the session results for the item swapping in
		 // the Gnome's Going Postal quest.

		case ItemPool.REALLY_BIG_TINY_HOUSE:
			ResultProcessor.processItem( ItemPool.RED_PAPER_CLIP, -1 );
			break;

		case ItemPool.NONESSENTIAL_AMULET:
			ResultProcessor.processItem( ItemPool.REALLY_BIG_TINY_HOUSE, -1 );
			break;

		case ItemPool.WHITE_WINE_VINAIGRETTE:
			ResultProcessor.processItem( ItemPool.NONESSENTIAL_AMULET, -1 );
			break;
		case ItemPool.CURIOUSLY_SHINY_AX:
			ResultProcessor.processItem( ItemPool.WHITE_WINE_VINAIGRETTE, -1 );
			break;
		case ItemPool.CUP_OF_STRONG_TEA:
			ResultProcessor.processItem( ItemPool.CURIOUSLY_SHINY_AX, -1 );
			break;
		case ItemPool.MARINATED_STAKES:
			ResultProcessor.processItem( ItemPool.CUP_OF_STRONG_TEA, -1 );
			break;
		case ItemPool.KNOB_BUTTER:
			ResultProcessor.processItem( ItemPool.MARINATED_STAKES, -1 );
			break;
		case ItemPool.VIAL_OF_ECTOPLASM:
			ResultProcessor.processItem( ItemPool.KNOB_BUTTER, -1 );
			break;
		case ItemPool.BOOCK_OF_MAGIKS:
			ResultProcessor.processItem( ItemPool.VIAL_OF_ECTOPLASM, -1 );
			break;
		case ItemPool.EZ_PLAY_HARMONICA_BOOK:
			ResultProcessor.processItem( ItemPool.BOOCK_OF_MAGIKS, -1 );
			break;
		case ItemPool.FINGERLESS_HOBO_GLOVES:
			ResultProcessor.processItem( ItemPool.EZ_PLAY_HARMONICA_BOOK, -1 );
			break;
		case ItemPool.CHOMSKYS_COMICS:
			ResultProcessor.processItem( ItemPool.FINGERLESS_HOBO_GLOVES, -1 );
			break;

		case ItemPool.GNOME_DEMODULIZER:
			ResultProcessor.removeItem( ItemPool.CHOMSKYS_COMICS );
			break;

		case ItemPool.MUS_MANUAL:
		case ItemPool.MYS_MANUAL:
		case ItemPool.MOX_MANUAL:
			ResultProcessor.processItem( ItemPool.DUSTY_BOOK, -1 );
			ResultProcessor.processItem( ItemPool.FERNSWARTHYS_KEY, -1 );
			break;

		case ItemPool.FRATHOUSE_BLUEPRINTS:
			ResultProcessor.processItem( ItemPool.CARONCH_MAP, -1 );
			ResultProcessor.processItem( ItemPool.CARONCH_NASTY_BOOTY, -1 );
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step2" );
			break;
			
		case ItemPool.CARONCH_DENTURES:
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step3" );
			break;

		case ItemPool.TEN_LEAF_CLOVER:
			ResultProcessor.receivedClover = true;
			break;

		case ItemPool.DISASSEMBLED_CLOVER:
			ResultProcessor.receivedDisassembledClover = true;
			break;

		case ItemPool.BAT_BANDANA:
			QuestDatabase.setQuestProgress( Quest.BAT, "step4" );

		case ItemPool.BATSKIN_BELT:
		case ItemPool.DRAGONBONE_BELT_BUCKLE:
			ResultProcessor.autoCreate( ItemPool.BADASS_BELT );
			break;

		case ItemPool.QUANTUM_EGG:
			ResultProcessor.autoCreate( ItemPool.ROWBOAT );
			break;

		case ItemPool.HEMP_STRING:
		case ItemPool.BONERDAGON_VERTEBRA:
			ResultProcessor.autoCreate( ItemPool.BONERDAGON_NECKLACE );
			break;
			
		case ItemPool.BONERDAGON_CHEST:
			QuestDatabase.setQuestProgress( Quest.CYRPT, "step1" );
			break;

		case ItemPool.SNAKEHEAD_CHARM:
			if ( result.getCount( KoLConstants.inventory ) >= 2 )
			{
				ResultProcessor.autoCreate( ItemPool.TALISMAN );
			}
			break;
			
		case ItemPool.TALISMAN:
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step1" );
			break;

		case ItemPool.EYE_OF_ED:
			QuestDatabase.setQuestProgress( Quest.MANOR, QuestDatabase.FINISHED );
			break;
			
		case ItemPool.ANCIENT_AMULET:
			QuestDatabase.setQuestProgress( Quest.WORSHIP, QuestDatabase.FINISHED );
			break;
			
		case ItemPool.STAFF_OF_FATS:
			QuestDatabase.setQuestProgress( Quest.PALINDOME, QuestDatabase.FINISHED );
			break;
			
		case ItemPool.WORM_RIDING_MANUAL_1:
			QuestDatabase.setQuestProgress( Quest.PYRAMID, "step7" );
			break;
			
		case ItemPool.WORM_RIDING_MANUAL_2:
			QuestDatabase.setQuestProgress( Quest.PYRAMID, "step8" );
			break;
			
		case ItemPool.WORM_RIDING_MANUAL_3_15:
			QuestDatabase.setQuestProgress( Quest.PYRAMID, "step9" );
			break;

		case ItemPool.WORM_RIDING_HOOKS:
			ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_1, -1 );
			ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_2, -1 );
			ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_3_15, -1 );
			QuestDatabase.setQuestProgress( Quest.PYRAMID, "step10" );
			break;
			
		case ItemPool.CARONCH_MAP:
			QuestDatabase.setQuestProgress( Quest.PIRATE, QuestDatabase.STARTED );
			break;
			
		case ItemPool.CARONCH_NASTY_BOOTY:
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step1" );
			break;
			
		case ItemPool.STEEL_LIVER:
		case ItemPool.STEEL_STOMACH:
		case ItemPool.STEEL_SPLEEN:
			QuestDatabase.setQuestProgress( Quest.AZAZEL, QuestDatabase.FINISHED );
			break;

		case ItemPool.DAS_BOOT:
			ResultProcessor.removeItem( ItemPool.DAMP_OLD_BOOT );
			break;

		case ItemPool.PREGNANT_FLAMING_MUSHROOM:
			ResultProcessor.processItem( ItemPool.FLAMING_MUSHROOM, -1 );
			break;

		case ItemPool.PREGNANT_FROZEN_MUSHROOM:
			ResultProcessor.processItem( ItemPool.FROZEN_MUSHROOM, -1 );
			break;

		case ItemPool.PREGNANT_STINKY_MUSHROOM:
			ResultProcessor.processItem( ItemPool.STINKY_MUSHROOM, -1 );
			break;

		case ItemPool.GRANDMAS_MAP:
			ResultProcessor.processItem( ItemPool.GRANDMAS_NOTE, -1 );
			ResultProcessor.processItem( ItemPool.FUCHSIA_YARN, -1 );
			ResultProcessor.processItem( ItemPool.CHARTREUSE_YARN, -1 );
			break;

		case ItemPool.SMALL_STONE_BLOCK:
			ResultProcessor.processItem( ItemPool.IRON_KEY, -1 );
			break;

		case ItemPool.CRIMBOMINATION_CONTRAPTION:
			ResultProcessor.removeItem( ItemPool.WRENCH_HANDLE );
			ResultProcessor.removeItem( ItemPool.HEADLESS_BOLTS );
			ResultProcessor.removeItem( ItemPool.AGITPROP_INK );
			ResultProcessor.removeItem( ItemPool.HANDFUL_OF_WIRES );
			ResultProcessor.removeItem( ItemPool.CHUNK_OF_CEMENT );
			ResultProcessor.removeItem( ItemPool.PENGUIN_GRAPPLING_HOOK );
			ResultProcessor.removeItem( ItemPool.CARDBOARD_ELF_EAR );
			ResultProcessor.removeItem( ItemPool.SPIRALING_SHAPE );
			break;

		case ItemPool.HELLSEAL_DISGUISE:
			ResultProcessor.processItem( ItemPool.HELLSEAL_HIDE, -6 );
			ResultProcessor.processItem( ItemPool.HELLSEAL_BRAIN, -6 );
			ResultProcessor.processItem( ItemPool.HELLSEAL_SINEW, -6 );
			break;

		case ItemPool.DECODED_CULT_DOCUMENTS:
			ResultProcessor.processItem( ItemPool.CULT_MEMO, -5 );
			break;

		case ItemPool.PIXEL_CHAIN_WHIP:
			if ( combatResults )
			{
				// If you acquire a pixel chain whip, you lose
				// the pixel whip you were wielding and wield
				// the chain whip in its place.

				AdventureResult whip = ItemPool.get( ItemPool.PIXEL_WHIP, 1 );
				EquipmentManager.transformEquipment( whip, result );
				ResultProcessor.processItem( result.getItemId(), -1 );
			}
			break;

		case ItemPool.PIXEL_MORNING_STAR:
			if ( combatResults )
			{
				// If you acquire a pixel morning star, you
				// lose the pixel chain whip you were wielding
				// and wield the morning star in its place.

				AdventureResult chainWhip = ItemPool.get( ItemPool.PIXEL_CHAIN_WHIP, 1 );
				EquipmentManager.transformEquipment( chainWhip, result );
				ResultProcessor.processItem( result.getItemId(), -1 );
			}
			break;

		case ItemPool.REFLECTION_OF_MAP:
			if ( combatResults )
			{
				int current = Preferences.getInteger( "pendingMapReflections" );
				current = Math.max( 0, current - 1);
				Preferences.setInteger( "pendingMapReflections", current );
			}
			break;

		case ItemPool.GONG:
			if ( combatResults )
			{
				Preferences.increment( "_gongDrops", 1 );
			}
			break;

		case ItemPool.SLIME_STACK:
			{
				int dropped = Preferences.increment( "slimelingStacksDropped", 1 );
				if ( dropped > Preferences.getInteger( "slimelingStacksDue" ) )
				{
					// in case it's out of sync, nod and smile
					Preferences.setInteger( "slimelingStacksDue", dropped );
				}
			}
			break;

		case ItemPool.ABSINTHE:
			if ( combatResults )
			{
				Preferences.increment( "_absintheDrops", 1 );
			}
			break;

		case ItemPool.ASTRAL_MUSHROOM:
			if ( combatResults )
			{
				Preferences.increment( "_astralDrops", 1 );
			}
			break;

		case ItemPool.AGUA_DE_VIDA:
			if ( combatResults )
			{
				Preferences.increment( "_aguaDrops", 1 );
			}
			break;

		case ItemPool.DEVILISH_FOLIO:
			if ( combatResults )
			{
				Preferences.increment( "_kloopDrops", 1 );
			}
			break;

		case ItemPool.GROOSE_GREASE:
			if ( combatResults )
			{
				Preferences.increment( "_grooseDrops", 1 );
			}
			break;

		case ItemPool.GG_TOKEN:
			if ( combatResults )
			{
				Preferences.increment( "_tokenDrops", 1 );
			}
			// Fall through
		case ItemPool.GG_TICKET:
			// If this is the first token or ticket we've gotten
			// this ascension, visit the wrong side of the tracks
			// to unlock the arcade.
			if ( Preferences.getInteger( "lastArcadeAscension" ) < KoLCharacter.getAscensions() )
			{
				Preferences.setInteger( "lastArcadeAscension", KoLCharacter.getAscensions() );
				RequestThread.postRequest( new GenericRequest( "town_wrong.php" ) );
			}
			break;

		case ItemPool.TRANSPORTER_TRANSPONDER:
			if ( combatResults )
			{
				Preferences.increment( "_transponderDrops", 1 );
			}
			break;

		case ItemPool.LIVER_PIE:
		case ItemPool.BADASS_PIE:
		case ItemPool.FISH_PIE:
		case ItemPool.PIPING_PIE:
		case ItemPool.IGLOO_PIE:
		case ItemPool.TURNOVER:
		case ItemPool.DEAD_PIE:
		case ItemPool.THROBBING_PIE:
			if ( combatResults )
			{
				Preferences.increment( "_pieDrops", 1 );
				Preferences.setInteger( "_piePartsCount", -1 );
			}
			break;

		case ItemPool.GOOEY_PASTE:
		case ItemPool.BEASTLY_PASTE:
		case ItemPool.OILY_PASTE:
		case ItemPool.ECTOPLASMIC:
		case ItemPool.GREASY_PASTE:
		case ItemPool.BUG_PASTE:
		case ItemPool.HIPPY_PASTE:
		case ItemPool.ORC_PASTE:
		case ItemPool.DEMONIC_PASTE:
		case ItemPool.INDESCRIBABLY_HORRIBLE_PASTE:
		case ItemPool.FISHY_PASTE:
		case ItemPool.GOBLIN_PASTE:
		case ItemPool.PIRATE_PASTE:
		case ItemPool.CHLOROPHYLL_PASTE:
		case ItemPool.STRANGE_PASTE:
		case ItemPool.MER_KIN_PASTE:
		case ItemPool.SLIMY_PASTE:
		case ItemPool.PENGUIN_PASTE:
		case ItemPool.ELEMENTAL_PASTE:
		case ItemPool.COSMIC_PASTE:
		case ItemPool.HOBO_PASTE:
		case ItemPool.CRIMBO_PASTE:
			if ( combatResults )
			{
				Preferences.increment( "_pasteDrops", 1 );
			}
			break;

		case ItemPool.BEER_LENS:
			if ( combatResults )
			{
				Preferences.increment( "_beerLensDrops", 1 );
			}
			break;

		case ItemPool.COTTON_CANDY_CONDE:
		case ItemPool.COTTON_CANDY_PINCH:
		case ItemPool.COTTON_CANDY_SMIDGEN:
		case ItemPool.COTTON_CANDY_SKOSHE:
		case ItemPool.COTTON_CANDY_PLUG:
		case ItemPool.COTTON_CANDY_PILLOW:
		case ItemPool.COTTON_CANDY_BALE:
			if ( combatResults )
			{
				Preferences.increment( "_carnieCandyDrops", 1 );
			}
			break;

		case ItemPool.LESSER_GRODULATED_VIOLET:
		case ItemPool.TIN_MAGNOLIA:
		case ItemPool.BEGPWNIA:
		case ItemPool.UPSY_DAISY:
		case ItemPool.HALF_ORCHID:
			if ( combatResults )
			{
				Preferences.increment( "_mayflowerDrops", 1 );
			}
			break;

		case ItemPool.EVILOMETER:
			Preferences.setInteger( "cyrptTotalEvilness", 200 );
			Preferences.setInteger( "cyrptAlcoveEvilness", 50 );
			Preferences.setInteger( "cyrptCrannyEvilness", 50 );
			Preferences.setInteger( "cyrptNicheEvilness", 50 );
			Preferences.setInteger( "cyrptNookEvilness", 50 );
			break;

		case ItemPool.TEACHINGS_OF_THE_FIST:
			// save which location the scroll was found in.
			String setting = AdventureDatabase.fistcoreLocationToSetting( KoLAdventure.lastAdventureId() );
			if ( setting != null )
			{
				Preferences.setBoolean( setting, true );
			}
			break;

		case ItemPool.KEYOTRON:
			Preferences.setInteger( "biodataEngineering", 0 );
			Preferences.setInteger( "biodataGalley", 0 );
			Preferences.setInteger( "biodataMedbay", 0 );
			Preferences.setInteger( "biodataMorgue", 0 );
			Preferences.setInteger( "biodataNavigation", 0 );
			Preferences.setInteger( "biodataScienceLab", 0 );
			Preferences.setInteger( "biodataSonar", 0 );
			Preferences.setInteger( "biodataSpecialOps", 0 );
			Preferences.setInteger( "biodataWasteProcessing", 0 );
			Preferences.setInteger( "lastKeyotronUse", KoLCharacter.getAscensions() );
			break;
		}

		// Gaining items can achieve goals.
		GoalManager.updateProgress( result );
	}

	private static void autoCreate( final int itemId )
	{
		if ( ResultProcessor.autoCrafting || !Preferences.getBoolean( "autoCraft" ) )
		{
			return;
		}

		ConcoctionDatabase.refreshConcoctions( true );
		CreateItemRequest creator = CreateItemRequest.getInstance( itemId );

		// getQuantityPossible() should take meat paste or
		// Muscle Sign into account

		int possible = creator.getQuantityPossible();

		if ( possible > 0 )
		{
			// Make as many as you can
			ResultProcessor.autoCrafting = true;
			creator.setQuantityNeeded( possible );
			RequestThread.postRequest( creator );
			ResultProcessor.autoCrafting = false;
		}
	}

	private static Pattern HIPPY_PATTERN = Pattern.compile( "we donated (\\d+) meat" );
	public static boolean onlyAutosellDonationsCount = true;

	public static void handleDonations( final String urlString, final String responseText )
	{
		// Apparently, only autoselling items counts towards the trophy..
		if ( ResultProcessor.onlyAutosellDonationsCount )
		{
			return;
		}

		// ITEMS

		// Dolphin King's map:
		//
		// The treasure includes some Meat, but you give it away to
		// some moist orphans. They need it to buy dry clothes.

		if ( responseText.contains( "give it away to moist orphans" ) )
		{
			KoLCharacter.makeCharitableDonation( 150 );
			return;
		}

		// chest of the Bonerdagon:
		//
		// The Cola Wars Veterans Administration is really gonna
		// appreciate the huge donation you're about to make!

		if ( responseText.contains( "Cola Wars Veterans Administration" ) )
		{
			KoLCharacter.makeCharitableDonation( 3000 );
			return;
		}

		// ancient vinyl coin purse
		//
		// You head into town and give the Meat to a guy wearing thick
		// glasses and a tie. Maybe now he'll be able to afford eye
		// surgery and a new wardrobe.
		//
		// black pension check
		//
		// You head back to the Black Forest and give the proceeds to
		// one of the black widows. Any given widow is more or less the
		// same as any other widow, right?
		//
		// old coin purse
		//
		// You wander around town until you find somebody named
		// Charity, and give her the Meat.
		//
		// old leather wallet
		//
		// You take the Meat to a soup kitchen and hand it to the first
		// person you see. He smelled bad, so he was probably a
		// volunteer.
		//
		// orcish meat locker
		//
		// You unlock the Meat locker with your rusty metal key, and
		// then dump the contents directly into a charity box at a
		// nearby convenience store. Those kids with boneitis are sure
		// to appreciate the gesture.
		//
		// Penultimate Fantasy chest
		//
		// There some Meat in it, but you drop it off the side of the
		// airship. It'll probably land on someone needy.
		//
		// Warm Subject gift certificate
		//
		// Then you walk next door to the hat store, and you give the
		// hat store all of your Meat. We need all kinds of things in
		// this economy.

		// QUESTS

		// Spooky Forest quest:
		//
		// Thanks for the larva, Adventurer. We'll put this to good use.

		if ( responseText.contains( "Thanks for the larva, Adventurer" ) &&
                !responseText.contains( "You gain" ) )
		{
			KoLCharacter.makeCharitableDonation( 500 );
			return;
		}

		// Wizard of Ego: from the "Other Class in the Guild" -> place=ocg
		// Nemesis: from the "Same Class in the Guild" -> place=scg
		//
		// You take the Meat into town and drop it in the donation slot
		// at the orphanage. You know, the one next to the library.

		if ( responseText.contains( "the one next to the library" ) )
		{
			int donation =
                    urlString.contains( "place=ocg" ) ? 500 :
                            urlString.contains( "place=scg" ) ? 1000 :
				0;
			KoLCharacter.makeCharitableDonation( donation );
			return;
		}

		// Tr4pz0r quest:
		//
		// The furs you divide up between yourself and the Tr4pz0r, the
		// Meat you divide up between the Tr4pz0r and the needy.

		if ( responseText.contains( "you divide up between the Tr4pz0r and the needy" ) )
		{
			KoLCharacter.makeCharitableDonation( 5000 );
			return;
		}

		// Cap'n Caronch:
		//
		// (3000 meat with pirate fledges)

		// Post-filthworm orchard:
		//
		// Oh, hey, boss! Welcome back! Hey man, we don't want to
		// impose on your vow of poverty, so we donated 4248 meat from
		// our profits to the human fund in your honor. Thanks for
		// getting rid of those worms, man!

		Matcher matcher = ResultProcessor.HIPPY_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int donation = StringUtilities.parseInt( matcher.group( 1 ) );
			KoLCharacter.makeCharitableDonation( donation );
			return;
		}
	}
}
