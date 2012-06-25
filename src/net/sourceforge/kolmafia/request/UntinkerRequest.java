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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.GenericFrame;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UntinkerRequest
	extends GenericRequest
{
	private static final GenericRequest AVAILABLE_CHECKER = new GenericRequest( "forestvillage.php?place=untinker" );

	private static boolean canUntinker;
	private static int lastUserId = -1;

	private static final AdventureResult SCREWDRIVER = ItemPool.get( ItemPool.RUSTY_SCREWDRIVER, -1 );

	private final int itemId;
	private int iterationsNeeded;
	private AdventureResult item;

	public static final void reset()
	{
		UntinkerRequest.canUntinker = false;
		UntinkerRequest.lastUserId = -1;
	}

	public UntinkerRequest( final int itemId )
	{
		this( itemId, Integer.MAX_VALUE );
	}

	public UntinkerRequest( final int itemId, final int itemCount )
	{
		super( "forestvillage.php" );

		this.addFormField( "action", "untinker" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );

		this.itemId = itemId;
		this.iterationsNeeded = 1;

		this.item = new AdventureResult( itemId, itemCount );

		if ( itemCount == Integer.MAX_VALUE )
		{
			this.item = this.item.getInstance( this.item.getCount( KoLConstants.inventory ) );
		}

		if ( itemCount > 5 || this.item.getCount( KoLConstants.inventory ) == itemCount )
		{
			this.addFormField( "untinkerall", "on" );
		}
		else
		{
			this.iterationsNeeded = itemCount;
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		// Check to see if the item can be constructed using meat
		// paste, and only execute the request if it is known to be
		// creatable through combination.

		if ( (ConcoctionDatabase.getMixingMethod( this.itemId ) & KoLConstants.CT_MASK) != KoLConstants.COMBINE )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot untinker that item." );
			return;
		}

		if ( !InventoryManager.retrieveItem( this.item ) )
		{
			return;
		}

		KoLmafia.updateDisplay( "Untinkering " + this.item + "..." );

		super.run();

		if ( this.responseText.indexOf( "You acquire" ) == -1 )
		{
			ResultProcessor.processResult( new AdventureResult( this.itemId, 1 ) );

			UntinkerRequest.AVAILABLE_CHECKER.run();

			if ( UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "<select" ) == -1 )
			{
				UntinkerRequest.canUntinker = UntinkerRequest.completeQuest();

				if ( !UntinkerRequest.canUntinker )
				{
					return;
				}

				UntinkerRequest.AVAILABLE_CHECKER.run();
			}

			super.run();
		}

		for ( int i = 1; i < this.iterationsNeeded; ++i )
		{
			super.run();
		}

		KoLmafia.updateDisplay( "Successfully untinkered " + this.item );
	}

	@Override
	public void processResults()
	{
		UntinkerRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// Either place=untinker or action=untinker

		if ( !location.startsWith( "forestvillage.php" ) || location.indexOf( "untinker" ) == -1 )
		{
			return;
		}

		// "Thanks! I'll tell ya, I'm just lost without my screwdriver. Here, lemme mark the Knoll on your map."
		if ( responseText.indexOf( "I'm just lost without my screwdriver" ) != -1 )
		{
			QuestDatabase.setQuestProgress( Quest.UNTINKER, QuestDatabase.STARTED );
		}

		// Visiting the untinker removes screwdriver from inventory.

		if ( KoLConstants.inventory.contains( UntinkerRequest.SCREWDRIVER ) )
		{
			ResultProcessor.processResult( UntinkerRequest.SCREWDRIVER );
			QuestDatabase.setQuestProgress( Quest.UNTINKER, QuestDatabase.FINISHED );
		}

		UntinkerRequest.lastUserId = KoLCharacter.getUserId();
		UntinkerRequest.canUntinker = responseText.indexOf( "you don't have anything like that" ) != -1 || responseText.indexOf( "<select name=whichitem>" ) != -1;

		if ( responseText.indexOf( "You acquire" ) != -1 )
		{
			Matcher matcher = TransferItemRequest.ITEMID_PATTERN.matcher( location );
			if ( !matcher.find() )
			{
				return;
			}

			int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
			AdventureResult result = new AdventureResult( itemId, -1 );

			if ( location.indexOf( "untinkerall=on" ) != -1 )
			{
				result = result.getInstance( 0 - result.getCount( KoLConstants.inventory ) );
			}

			ResultProcessor.processResult( result );
		}
	}

	public static final boolean canUntinker()
	{
		if ( UntinkerRequest.lastUserId == KoLCharacter.getUserId() )
		{
			return UntinkerRequest.canUntinker;
		}

		UntinkerRequest.lastUserId = KoLCharacter.getUserId();

		// If the person does not have the accomplishment, visit
		// the untinker to ensure that they get the quest.

		UntinkerRequest.AVAILABLE_CHECKER.run();

		// "I can take apart anything that's put together with meat
		// paste, but you don't have anything like that..."

		UntinkerRequest.canUntinker =
			UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "you don't have anything like that" ) != -1 || UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "<select name=whichitem>" ) != -1;

		return UntinkerRequest.canUntinker;
	}

	public static final boolean completeQuest()
	{
		// If the are in a muscle sign, this is a trivial task;
		// just have them visit Innabox.

		if ( KoLCharacter.knollAvailable() )
		{
			GenericRequest tinkVisit = new GenericRequest( "forestvillage.php" );
			tinkVisit.run();
			tinkVisit.addFormField( "action=screwquest" );
			tinkVisit.run();
			GenericRequest knollVisit = new GenericRequest( "knoll.php" );
			knollVisit.run();

			knollVisit.addFormField( "place=smith" );
			knollVisit.run();

			return true;
		}

		if ( GenericFrame.instanceExists() )
		{
			if ( !InputFieldUtilities.confirm( "KoLmafia thinks you haven't completed the screwdriver quest.  Would you like to have KoLmafia automatically complete it now?" ) )
			{
				return false;
			}
		}

		// Okay, so they don't have one yet. Complete the
		// untinkerer's quest automatically.

		GenericRequest tinkVisit = new GenericRequest( "forestvillage.php" );
		tinkVisit.run();
		tinkVisit.addFormField( "action=screwquest" );
		tinkVisit.run();

		KoLAdventure sideTripLocation = AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=18" );
		AdventureResult sideTripItem = UntinkerRequest.SCREWDRIVER.getNegation();

		String action = Preferences.getString( "battleAction" );
		if ( action.indexOf( "dictionary" ) != -1 )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "battleAction=attack" );
		}

		GoalManager.makeSideTrip( sideTripLocation, sideTripItem );

		if ( KoLmafia.refusesContinue() )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "battleAction=" + action );
			return false;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "battleAction=" + action );

		// You should now have a screwdriver in your inventory.
		// Go ahead and rerun the untinker request and you will
		// have the needed accomplishment.

		UntinkerRequest.AVAILABLE_CHECKER.run();
		return UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "Degrassi Knoll" ) == -1;
	}

	public static final void decorate( final String urlString, final StringBuffer buffer )
	{
		// We decorate simple visits to the untinker and also
		// accepting his quest
		if ( urlString.indexOf( "place=untinker" ) == -1 &&
		     urlString.indexOf( "action=screwquest" ) == -1 )
		{
			return;
		}

		// Hey, man -- is an adventurer you? I lost my screwdriver
		// somewhere near Degrassi Knoll, but every time I try to get
		// it back, the Gnolls punch me in the eye.
		//
		// You look pretty tough, though -- do you think you could get
		// it back for me?
		//
		// *** There is now a button on the page to accept his quest.
		// *** We need to find out what he says when you accept it.
		//
		// "Thanks!  I'll tell ya, I'm just lost without my
		// screwdriver.	 Here, lemme mark the Knoll on your map."

		// Initial response to accepting his quest
		String test = "Here, lemme mark the Knoll on your map.&quot;";
		int index = buffer.indexOf( test );

		// Have you had any luck finding my screwdriver? I lost it at
		// Degrassi Knoll, you'll recall.
		if ( index == -1 )
		{
			// Subsequent visits
			test = "I lost it at Degrassi Knoll, you'll recall.&quot;";
			index = buffer.indexOf( test );
		}

		if ( index == -1 )
		{
			return;
		}

		String link;
		if ( KoLCharacter.knollAvailable() )
		{
			link = "<font size=1>[<a href=\"knoll.php?place=smith\">visit Innabox</a>]</font>";
		}
		else
			link = "<font size=1>[<a href=\"adventure.php?snarfblat=18\">Degrassi Knoll</a>]</font>";
		{
		}

		buffer.insert( index + test.length(), link );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// Either place=untinker or action=untinker

		if ( !urlString.startsWith( "forestvillage.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.indexOf( "action=untinker" ) != -1 )
		{
			Matcher matcher = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				return true;
			}

			String name = ItemDatabase.getItemName( StringUtilities.parseInt( matcher.group( 1 ) ) );
			message = "untinker " + ( urlString.indexOf( "untinkerall=on" ) != -1 ? "*" : "1" ) + " " + name;
		}
		else if ( urlString.indexOf( "action=screwquest" ) != -1 )
		{
			message = "Accepting quest to find the Untinker's screwdriver";
		}
		else if ( urlString.indexOf( "place=untinker" ) != -1 )
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the Untinker";
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
