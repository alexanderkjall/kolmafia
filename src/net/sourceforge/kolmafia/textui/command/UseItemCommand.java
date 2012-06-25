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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.DrinkItemRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class UseItemCommand
	extends AbstractCommand
{
	public UseItemCommand()
	{
		this.usage = "[?] [either] <item> [, <item>]... - use/consume items";
	}

	@Override
	public void run( String command, final String parameters )
	{
		if ( command.equals( "overdrink" ) )
		{
			DrinkItemRequest.permitOverdrink();
			command = "drink";
		}
		else if ( command.equals( "eatsilent" ) )
		{
			EatItemRequest.ignoreMilkPrompt();
			command = "eat";
		}
		SpecialOutfit.createImplicitCheckpoint();
		UseItemCommand.use( command, parameters );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public static void use( final String command, String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			return;
		}

		boolean either = parameters.startsWith( "either " );
		if ( either )
		{
			parameters = parameters.substring( 7 ).trim();
		}

		if ( command.equals( "eat" ) || command.equals( "eatsilent" ) )
		{
			if ( KoLCharacter.inBadMoon() && KitchenCommand.visit( parameters ) )
			{
				return;
			}
			if ( KoLCharacter.canadiaAvailable() && RestaurantCommand.makeChezSnooteeRequest( parameters ) )
			{
				return;
			}
		}

		if ( command.equals( "drink" ) || command.equals( "overdrink" ) )
		{
			if ( KoLCharacter.inBadMoon() && KitchenCommand.visit( parameters ) )
			{
				return;
			}
			if ( KoLCharacter.gnomadsAvailable() && RestaurantCommand.makeMicroBreweryRequest( parameters ) )
			{
				return;
			}
		}

		// Now, handle the instance where the first item is actually
		// the quantity desired, and the next is the amount to use

		if ( command.equals( "eat" ) || command.equals( "eatsilent" ) || command.equals( "ghost" ) )
		{
			ItemFinder.setMatchType( ItemFinder.FOOD_MATCH );
		}
		else if ( command.equals( "drink" ) || command.equals( "overdrink" ) || command.equals( "hobo" ) )
		{
			ItemFinder.setMatchType( ItemFinder.BOOZE_MATCH );
		}
		else if ( command.equals( "slimeling" ) )
		{
			ItemFinder.setMatchType( ItemFinder.EQUIP_MATCH );
		}
		else
		{
			ItemFinder.setMatchType( ItemFinder.USE_MATCH );
		}

		Object[] itemList = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );

		ItemFinder.setMatchType( ItemFinder.ANY_MATCH );

		for ( int level = either ? 0 : 2; level <= 2; ++level )
		{ // level=0: use only items in inventory, exit on first success
			// level=1: buy/make as needed, exit on first success
			// level=2: use all items in list, buy/make as needed
			for ( int i = 0; i < itemList.length; ++i )
			{
				AdventureResult currentMatch = (AdventureResult) itemList[ i ];
				int consumpt = ItemDatabase.getConsumptionType( currentMatch.getItemId() );

				if ( command.equals( "eat" ) && consumpt == KoLConstants.CONSUME_FOOD_HELPER )
				{ // allowed
				}
				else if ( command.equals( "eat" ) || command.equals( "ghost" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_EAT )
					{
						KoLmafia.updateDisplay(
							MafiaState.ERROR, currentMatch.getName() + " cannot be consumed." );
						return;
					}
				}

				if ( command.equals( "drink" ) && consumpt == KoLConstants.CONSUME_DRINK_HELPER )
				{ // allowed
				}
				else if ( command.equals( "drink" ) || command.equals( "hobo" ) )
				{
					if ( consumpt != KoLConstants.CONSUME_DRINK )
					{
						KoLmafia.updateDisplay(
							MafiaState.ERROR, currentMatch.getName() + " is not an alcoholic beverage." );
						return;
					}
				}

				if ( command.equals( "use" ) )
				{
					switch ( consumpt )
					{
					case KoLConstants.CONSUME_EAT:
					case KoLConstants.CONSUME_FOOD_HELPER:
						KoLmafia.updateDisplay( MafiaState.ERROR, currentMatch.getName() + " must be eaten." );
						return;
					case KoLConstants.CONSUME_DRINK:
					case KoLConstants.CONSUME_DRINK_HELPER:
						KoLmafia.updateDisplay(
							MafiaState.ERROR, currentMatch.getName() + " is an alcoholic beverage." );
						return;
					}
				}

				int have = currentMatch.getCount( KoLConstants.inventory );
				if ( level > 0 || have > 0 )
				{
					if ( level == 0 && have < currentMatch.getCount() )
					{
						currentMatch = currentMatch.getInstance( have );
					}
					if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
					{
						RequestLogger.printLine( currentMatch.toString() );
					}
					else
					{
						UseItemRequest request =
							command.equals( "hobo" ) ?
							UseItemRequest.getInstance( KoLConstants.CONSUME_HOBO, currentMatch ) :
							command.equals( "ghost" ) ?
							UseItemRequest.getInstance( KoLConstants.CONSUME_GHOST, currentMatch ) :
							command.equals( "slimeling" ) ?
							UseItemRequest.getInstance( KoLConstants.CONSUME_SLIME, currentMatch ) :
							UseItemRequest.getInstance( currentMatch );
						RequestThread.postRequest( request );
					}

					if ( level < 2 )
					{
						return;
					}
				}
			}
		}
	}
}
