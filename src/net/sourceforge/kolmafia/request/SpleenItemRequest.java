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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class SpleenItemRequest
	extends UseItemRequest
{
	public SpleenItemRequest( final AdventureResult item )
	{
		super( UseItemRequest.getConsumptionType( item ), item );
	}

	@Override
	public int getAdventuresUsed()
	{
		return 0;
	}

	public static int maximumUses( final int itemId, final String itemName, final int spleenHit )
	{
		// Some spleen items also heal HP or MP
		int restorationMaximum = UseItemRequest.getRestorationMaximum( itemName );

		UseItemRequest.limiter = ( restorationMaximum < Integer.MAX_VALUE ) ?
			"needed restoration or spleen" : "spleen";

		int limit = KoLCharacter.getSpleenLimit();
		int spleenLeft = limit - KoLCharacter.getSpleenUse();

		return Math.min( restorationMaximum, spleenLeft / spleenHit );
	}

	@Override
	public void run()
	{
		if ( !ItemDatabase.meetsLevelRequirement( itemUsed.getName() ) )
		{
			UseItemRequest.lastUpdate = "Insufficient level to consume " + itemUsed;
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );
			return;
		}

		int itemId = itemUsed.getItemId();
		UseItemRequest.lastUpdate = "";

		int maximumUses = UseItemRequest.maximumUses( itemId );
		if ( maximumUses < itemUsed.getCount() )
		{
			KoLmafia.updateDisplay( "(usable quantity of " + itemUsed +
				" is limited to " + maximumUses + " by " +
				UseItemRequest.limiter + ")" );
            itemUsed = itemUsed.getInstance( maximumUses );
		}

		if ( itemUsed.getCount() < 1 )
		{
			return;
		}

		int iterations = 1;
		int origCount = itemUsed.getCount();

		if ( origCount > 1 && singleConsume( itemId ) )
		{
			iterations = origCount;
            itemUsed = itemUsed.getInstance( 1 );
		}

		String originalURLString = getURLString();

		for ( int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i )
		{
            constructURLString( originalURLString );
            useOnce( i, iterations, "Using" );
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Finished using " + origCount + " " + itemUsed.getName() + "." );
		}
	}

	@Override
	public void useOnce( final int currentIteration, final int totalIterations, String useTypeAsString )
	{
		UseItemRequest.lastUpdate = "";

		// Check to make sure the character has the item in their
		// inventory first - if not, report the error message and
		// return from the method.

		if ( !InventoryManager.retrieveItem( itemUsed ) )
		{
			UseItemRequest.lastUpdate = "Insufficient items to use.";
			return;
		}

		switch ( consumptionType )
		{
		case KoLConstants.CONSUME_MULTIPLE:
		case KoLConstants.HP_RESTORE:
		case KoLConstants.MP_RESTORE:
		case KoLConstants.HPMP_RESTORE:
			if ( itemUsed.getCount() > 1 )
			{
                addFormField( "action", "useitem" );
                addFormField( "quantity", String.valueOf( itemUsed.getCount() ) );
			}
			// Fall through
		default:
            addFormField( "ajax", "1" );
			break;
		}

		super.runOneIteration( currentIteration, totalIterations, useTypeAsString );
	}

	private boolean singleConsume( final int itemId )
	{
		return consumptionType == KoLConstants.CONSUME_USE;
	}

	public static void parseConsumption( final AdventureResult item, final AdventureResult helper, final String responseText )
	{
		if ( responseText.contains( "That item isn't usable in quantity" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Internal data error: item incorrectly flagged as multi-usable." );
			return;
		}

		int spleenHit = ItemDatabase.getSpleenHit( item.getName() );
		int count = item.getCount();
		int spleenUse = spleenHit * count;

		if ( responseText.contains( "rupture" ) )
		{
			UseItemRequest.lastUpdate = "Your spleen might go kablooie.";
			KoLmafia.updateDisplay( MafiaState.ERROR, UseItemRequest.lastUpdate );

			// If we have no spleen data for this item, we can't tell what,
			// if anything, consumption did to our spleen.
			if ( spleenHit == 0 )
			{
				return;
			}

			int maxSpleen = KoLCharacter.getSpleenLimit();
			int currentSpleen = KoLCharacter.getSpleenUse();

			int estimatedSpleen = maxSpleen - spleenUse + 1;

			if ( estimatedSpleen > currentSpleen )
			{
				Preferences.setInteger( "currentSpleenUse", estimatedSpleen );
			}

			KoLCharacter.updateStatus();

			return;
		}

		// The spleen item was consumed successfully
		Preferences.increment( "currentSpleenUse", spleenUse );

		ResultProcessor.processResult( item.getNegation() );
		KoLCharacter.updateStatus();

		// Re-sort consumables list if needed
		if ( Preferences.getBoolean( "sortByRoom" ) )
		{
			ConcoctionDatabase.getUsables().sort();
		}

		// Perform item-specific processing

		switch ( item.getItemId() )
		{
		case ItemPool.STEEL_SPLEEN:
			if ( responseText.contains( "You acquire a skill" ) )
			{
				ResponseTextParser.learnSkill( "Spleen of Steel" );
			}
			return;
		}
	}

	public static boolean registerRequest()
	{
		AdventureResult item = UseItemRequest.lastItemUsed;
		int count = item.getCount();
		String name = item.getName();

		String useString = "use " + count + " " + name ;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( useString );
		return true;
	}
}
