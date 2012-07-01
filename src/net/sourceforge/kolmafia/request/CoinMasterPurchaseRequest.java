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
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class CoinMasterPurchaseRequest
	extends PurchaseRequest
{
	private CoinmasterData data;
	private CoinMasterRequest request;
	private String priceString;
	private AdventureResult cost;

	/**
	 * Constructs a new <code>CoinMasterPurchaseRequest</code> which retrieves things from Coin Masters.
	 */

	public CoinMasterPurchaseRequest( final CoinmasterData data, final int itemId, final int price, final int quantity )
	{
		super( "" );		// We do not run this request itself

		this.data = data;

        isMallStore = false;
        item = new AdventureResult( itemId, 1 );

        shopName = data.getMaster();

		this.quantity = quantity;
		this.price = price;

		AdventureResult item = data.getItem();
		String token = item != null ? item.getName() : data.getToken();
		String name = ( price != 1 ) ? data.getPluralToken() : token;
        priceString = KoLConstants.COMMA_FORMAT.format( this.price ) + " " + name;
        cost = AdventureResult.tallyItem( token, price, true );

        limit = this.quantity;
        canPurchase = true;

        timestamp = 0L;

        request = data.getRequest( data.getBuyAction(), this.item );
	}

	public CoinmasterData getData()
	{
		return data;
	}

	@Override
	public String getPriceString()
	{
		return priceString;
	}

	@Override
	public AdventureResult getCost()
	{
		return cost;
	}

	@Override
	public String getCurrency( final int count )
	{
		String name = ( count != 1 ) ? data.getPluralToken() : data.getToken();
		return name;
	}

	public int getTokenItemId()
	{
		AdventureResult item = data.getItem();
		return item != null ? item.getItemId() : -1;
	}

	@Override
	public int affordableCount()
	{
		int tokens = data.affordableTokens();
		int price = this.price;
		return tokens / price;
	}

	@Override
	public boolean canPurchase()
	{
		return canPurchase && affordableCount() > 0;
	}

	@Override
	public void setCanPurchase()
	{
        setCanPurchase( data.isAccessible() && affordableCount() > 0 );
	}

	@Override
	public boolean isAccessible()
	{
		return data.isAccessible();
	}

	@Override
	public void run()
	{
		if ( request == null )
		{
			return;
		}

		if ( limit < 1 )
		{
			return;
		}

		// Make sure we have enough tokens to buy what we want.
		if ( data.availableTokens() < limit * price )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't afford that." );
			return;
		}

		// Make sure the Coin Master is accessible
		String message = data.accessible();
		if ( message != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return;
		}

		// Now that we're ready, make the purchase!

		KoLmafia.updateDisplay( "Purchasing " + item.getName() + " (" + KoLConstants.COMMA_FORMAT.format( limit ) + " @ " + getPriceString() + ")..." );

        initialCount = item.getCount( KoLConstants.inventory );
        request.setQuantity( limit );

		RequestThread.postRequest( request );
	}
}
