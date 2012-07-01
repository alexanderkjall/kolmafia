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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;

public class CraftRequest
	extends GenericRequest
{
	private int mixingMethod;
	private int quantity;
	private AdventureResult item1;
	private AdventureResult item2;
	private int remaining;
	private int created;

	public CraftRequest( final String mode, final int quantity, final int itemId1, final int itemId2 )
	{
		super( "craft.php" );

        addFormField( "action", "craft" );
        setMixingMethod( mode );
        addFormField( "a", String.valueOf( itemId1 ) );
        addFormField( "b", String.valueOf( itemId2 ) );

		this.quantity = quantity;
        remaining = quantity;
        item1 = ItemPool.get( itemId1, quantity );
        item2 = ItemPool.get( itemId2, quantity );
	}

	private void setMixingMethod( final String mode )
	{
		if ( mode.equals( "combine" ) )
		{
            mixingMethod = KoLConstants.COMBINE;
		}
		else if ( mode.equals( "cocktail" ) )
		{
            mixingMethod = KoLConstants.MIX;
		}
		else if ( mode.equals( "cook" ) )
		{
            mixingMethod = KoLConstants.COOK;
		}
		else if ( mode.equals( "smith" ) )
		{
            mixingMethod = KoLConstants.SMITH;
		}
		else if ( mode.equals( "jewelry" ) )
		{
            mixingMethod = KoLConstants.JEWELRY;
		}
		else
		{
            mixingMethod = KoLConstants.NOCREATE;
			return;
		}

        addFormField( "mode", mode );
	}

	public int created()
	{
		return quantity - remaining;
	}

	@Override
	public void run()
	{
		if ( mixingMethod == KoLConstants.NOCREATE ||
                quantity <= 0 ||
		     !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Get all the ingredients up front

		if ( !InventoryManager.retrieveItem( item1 ) ||
		     !InventoryManager.retrieveItem( item2 ) )
		{
			return;
		}

        remaining = quantity;

		while ( remaining > 0 && KoLmafia.permitsContinue() )
		{
			if ( !CreateItemRequest.autoRepairBoxServant( mixingMethod ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Auto-repair was unsuccessful." );
				return;
			}

            addFormField( "qty", String.valueOf( remaining ) );
            created = 0;

			super.run();

			if ( responseCode == 302 && redirectLocation.startsWith( "inventory" ) )
			{
				CreateItemRequest.REDIRECT_REQUEST.constructURLString( redirectLocation ).run();
			}

			if ( created == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Creation failed, no results detected." );
				return;
			}

            remaining -= created;
		}
	}

	@Override
	public void processResults()
	{
        created = CreateItemRequest.parseCrafting( getURLString(), responseText );

		if ( responseText.contains( "Smoke" ) )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );
		}
	}
}
