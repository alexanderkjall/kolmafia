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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class VendingMachineRequest
	extends CoinMasterRequest
{
	public static final String master = "Vending Machine"; 

	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( VendingMachineRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( VendingMachineRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have (\\w+) fat loot tokens? to spend." );
	public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get( ItemPool.FAT_LOOT_TOKEN, 1 );

	public static final CoinmasterData VENDING_MACHINE =
		new CoinmasterData(
			VendingMachineRequest.master,
			VendingMachineRequest.class,
			"da.php?place=vendo",
			"fat loot token",
			"You don't have any fat loot tokens to spend",
			false,
			VendingMachineRequest.TOKEN_PATTERN,
			VendingMachineRequest.FAT_LOOT_TOKEN,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			null,
			null,
			"vendo",
			VendingMachineRequest.buyItems,
			VendingMachineRequest.buyPrices,
			null,
			null
			);

	public VendingMachineRequest()
	{
		super( VendingMachineRequest.VENDING_MACHINE );
	}

	public VendingMachineRequest( final String action )
	{
		super( VendingMachineRequest.VENDING_MACHINE, action );
		this.addFormField( "pwd" );
	}

	public VendingMachineRequest( final String action, final int itemId, final int quantity )
	{
		super( VendingMachineRequest.VENDING_MACHINE, action, itemId, quantity );
		this.addFormField( "pwd" );
	}

	public VendingMachineRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public VendingMachineRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	@Override
	public void processResults()
	{
		VendingMachineRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = VendingMachineRequest.VENDING_MACHINE;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "place=vendo" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
			}

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "da.php" ) )
		{
			return false;
		}

		CoinmasterData data = VendingMachineRequest.VENDING_MACHINE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
