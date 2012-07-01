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

package net.sourceforge.kolmafia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.Iterator;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.HermitRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmasterData
	implements Comparable
{
	private final String master;
	private final Class requestClass;
	private final String URL;
	private String token;
	public String plural;
	private final String tokenTest;
	private final boolean positiveTest;
	private final Pattern tokenPattern;
	private AdventureResult item;
	private final String property;
	private final String itemField;
	private final Pattern itemPattern;
	private final String countField;
	private final Pattern countPattern;
	private final String buyAction;
	private final LockableListModel buyItems;
	private final Map buyPrices;
	private final String sellAction;
	private final Map sellPrices;
	private final String storageAction;
	private final String tradeAllAction;
	private final boolean canPurchase;

	public CoinmasterData( 
		final String master,
		final Class requestClass,
		final String URL,
		final String token,
		final String tokenTest,
		final boolean positiveTest,
		final Pattern tokenPattern,
		final AdventureResult item,
		final String property,
		final String itemField,
		final Pattern itemPattern,
		final String countField,
		final Pattern countPattern,
		final String buyAction,
		final LockableListModel buyItems,
		final Map buyPrices,
		final String sellAction,
		final Map sellPrices,
		final String storageAction,
		final String tradeAllAction,
		final boolean canPurchase )
	{
		this.master = master;
		this.requestClass = requestClass;
		this.URL = URL;
		this.token = token;
		this.tokenTest = tokenTest;
		this.positiveTest = positiveTest;
		this.tokenPattern = tokenPattern;
		this.item = item;
		this.property = property;
		this.itemField = itemField;
		this.itemPattern = itemPattern;
		this.countField = countField;
		this.countPattern = countPattern;
		this.buyAction = buyAction;
		this.buyItems = buyItems;
		this.buyPrices = buyPrices;
		this.sellAction = sellAction;
		this.sellPrices = sellPrices;
		this.storageAction = storageAction;
		this.tradeAllAction = tradeAllAction;
		this.canPurchase = canPurchase;

		// Derived fields
        plural = item != null ? ItemDatabase.getPluralName( token ) : token + "s";
	}

	public CoinmasterData( 
		final String master,
		final Class requestClass,
		final String URL,
		final String token,
		final String tokenTest,
		final boolean positiveTest,
		final Pattern tokenPattern,
		final AdventureResult item,
		final String property,
		final String itemField,
		final Pattern itemPattern,
		final String countField,
		final Pattern countPattern,
		final String buyAction,
		final LockableListModel buyItems,
		final Map buyPrices,
		final String sellAction,
		final Map sellPrices,
		final String storageAction,
		final String tradeAllAction )
	{
		this( master, requestClass, URL,
		      token, tokenTest, positiveTest, tokenPattern,
		      item, property,
		      itemField, itemPattern, countField, countPattern,
		      buyAction, buyItems, buyPrices,
		      sellAction, sellPrices,
		      storageAction, tradeAllAction,
		      ( buyItems != null ) );
	}

	public CoinmasterData( 
		final String master,
		final Class requestClass,
		final String URL,
		final String token,
		final String tokenTest,
		final boolean positiveTest,
		final Pattern tokenPattern,
		final AdventureResult item,
		final String property,
		final String itemField,
		final Pattern itemPattern,
		final String countField,
		final Pattern countPattern,
		final String buyAction,
		final LockableListModel buyItems,
		final Map buyPrices,
		final String sellAction,
		final Map sellPrices )
	{
		this( master, requestClass, URL,
		      token, tokenTest, positiveTest, tokenPattern,
		      item, property,
		      itemField, itemPattern, countField, countPattern,
		      buyAction, buyItems, buyPrices,
		      sellAction, sellPrices,
		      null, null );
	}

	public final String getMaster()
	{
		return master;
	}

	public final Class getRequestClass()
	{
		return requestClass;
	}

	public final String getURL()
	{
		return URL;
	}

	public final String getToken()
	{
		return token;
	}

	public final String getPluralToken()
	{
		return plural;
	}

	public final void setToken( final String token )
	{
		this.token = token;
	}

	public final int availableTokens()
	{
		AdventureResult item = this.item;
		String property = this.property;
		int count =
			item != null ? (
				item.getItemId() == ItemPool.WORTHLESS_ITEM ?
				HermitRequest.getWorthlessItemCount() :
				item.getCount( KoLConstants.inventory ) ) :
			property != null ? Preferences.getInteger( property ) :
			0;
		return count;
	}

	public final int availableStorageTokens()
	{
		return storageAction != null ? item.getCount( KoLConstants.storage ) : 0;
	}

	public final int affordableTokens()
	{
		// Special handling for acquiring worthless items
		if ( item != null && item.getItemId() == ItemPool.WORTHLESS_ITEM )
		{
			return HermitRequest.getAcquirableWorthlessItemCount();
		}
		return availableTokens();
	}

	public final String getTokenTest()
	{
		return tokenTest;
	}

	public final boolean getPositiveTest()
	{
		return positiveTest;
	}

	public final Pattern getTokenPattern()
	{
		return tokenPattern;
	}

	public final AdventureResult getItem()
	{
		return item;
	}

	public final void setItem( final AdventureResult item )
	{
		this.item = item;
	}

	public final String getProperty()
	{
		return property;
	}

	public final String getItemField()
	{
		return itemField;
	}

	public final Pattern getItemPattern()
	{
		return itemPattern;
	}

	public final Matcher getItemMatcher( final String string )
	{
		return itemPattern == null ? null : itemPattern.matcher( string );
	}

	public final String getCountField()
	{
		return countField;
	}

	public final Pattern getCountPattern()
	{
		return countPattern;
	}

	public final Matcher getCountMatcher( final String string )
	{
		return countPattern == null ? null : countPattern.matcher( string );
	}

	public final String getBuyAction()
	{
		return buyAction;
	}

	public final LockableListModel getBuyItems()
	{
		return buyItems;
	}

	public final Map getBuyPrices()
	{
		return buyPrices;
	}

	public final boolean canBuyItem( final String itemName )
	{
		if ( buyItems == null )
		{
			return false;
		}

		AdventureResult item = new AdventureResult( itemName, 1, false );
		return item.getCount( buyItems ) > 0;
	}

	public final int getBuyPrice( final String itemName )
	{
		if ( buyPrices != null )
		{
			String name = StringUtilities.getCanonicalName( itemName );
			Integer price = (Integer) buyPrices.get( name );
			return price != null ? price : 0;
		}
		return 0;
	}

	public final int getBuyPrice( final int itemId )
	{
		return getBuyPrice( ItemDatabase.getItemName( itemId ) );
	}

	public final String getSellAction()
	{
		return sellAction;
	}

	public final Map getSellPrices()
	{
		return sellPrices;
	}

	public final boolean canSellItem( final String itemName )
	{
		if ( sellPrices != null )
		{
			String name = StringUtilities.getCanonicalName( itemName );
			return sellPrices.containsKey( name );
		}
		return false;
	}

	public final int getSellPrice( final String itemName )
	{
		if ( sellPrices != null )
		{
			String name = StringUtilities.getCanonicalName( itemName );
			Integer price = (Integer) sellPrices.get( name );
			return price != null ? price : 0;
		}
		return 0;
	}

	public final String getStorageAction()
	{
		return storageAction;
	}

	public final String getTradeAllAction()
	{
		return tradeAllAction;
	}

	public void registerPurchaseRequests()
	{
		// If this Coin Master doesn't sell anything that goes into
		// your inventory, nothing to register
		if ( !canPurchase )
		{
			return;
		}

		// Clear existing purchase requests
		CoinmastersDatabase.clearPurchaseRequests( this );

		// For each item you can buy from this Coin Master, create a purchase request
		Iterator it = buyItems.iterator();
		while ( it.hasNext() )
		{
			AdventureResult item = (AdventureResult) it.next();
			int itemId = item.getItemId();
			String itemName = item.getName();
			int price = CoinmastersDatabase.getPrice( itemName, buyPrices );
			int quantity = item.getCount();
			CoinmastersDatabase.registerPurchaseRequest( this, itemId, price, quantity );
		}
	}

	@Override
	public String toString()
	{
		return master;
	}

	@Override
	public boolean equals( final Object o )
	{
		return o != null && o instanceof CoinmasterData && master == ( (CoinmasterData) o ).master;
	}

	public int compareTo( final Object o )
	{
		return o == null || !( o instanceof CoinmasterData ) ? 1 : compareTo( (CoinmasterData) o );
	}

	public int compareTo( final CoinmasterData cd )
	{
		return master.compareToIgnoreCase( cd.master );
	}

	public CoinMasterRequest getRequest()
	{
		Class requestClass = getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Constructor constructor = requestClass.getConstructor( parameters );
			Object [] initargs = new Object[ 0 ];
			return (CoinMasterRequest) constructor.newInstance( initargs );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public CoinMasterRequest getRequest( final String action, final AdventureResult it )
	{
		Class requestClass = getRequestClass();
		Class [] parameters = new Class[ 2 ] ;
		parameters[ 0 ] = String.class;
		parameters[ 1 ] = AdventureResult.class;

		try
		{
			Constructor constructor = requestClass.getConstructor( parameters );
			Object [] initargs = new Object[ 2 ];
			initargs[ 0 ] = action;
			initargs[ 1 ] = it;
			return (CoinMasterRequest) constructor.newInstance( initargs );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public boolean isAccessible()
	{
		return accessible() == null;
	}

	public String accessible()
	{
		// Returns an error reason or null

		Class requestClass = getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Method method = requestClass.getMethod( "accessible", parameters );
			Object [] args = new Object[ 0 ];
			return (String) method.invoke( null, args );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public String canSell()
	{
		// Returns an error reason or null

		Class requestClass = getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Method method = requestClass.getMethod( "canSell", parameters );
			Object [] args = new Object[ 0 ];
			return (String) method.invoke( null, args );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public String canBuy()
	{
		// Returns an error reason or null

		Class requestClass = getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Method method = requestClass.getMethod( "canBuy", parameters );
			Object [] args = new Object[ 0 ];
			return (String) method.invoke( null, args );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
}
