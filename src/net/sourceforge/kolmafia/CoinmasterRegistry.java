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

import java.util.List;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.DollHawkerRequest;
import net.sourceforge.kolmafia.request.FDKOLRequest;
import net.sourceforge.kolmafia.request.FreeSnackRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.LunarLunchRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.TicketCounterRequest;
import net.sourceforge.kolmafia.request.Tr4pz0rRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.VendingMachineRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class CoinmasterRegistry
{
	public static final CoinmasterData [] COINMASTERS = new CoinmasterData[]
	{
		AltarOfBonesRequest.ALTAR_OF_BONES,
		AWOLQuartermasterRequest.AWOL,
		BigBrotherRequest.BIG_BROTHER,
		BountyHunterHunterRequest.BHH,
		BURTRequest.BURT,
		CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP,
		Crimbo11Request.CRIMBO11,
		CrimboCartelRequest.CRIMBO_CARTEL,
		DimemasterRequest.HIPPY,
		DollHawkerRequest.DOLLHAWKER,
		FDKOLRequest.FDKOL,
		FreeSnackRequest.FREESNACKS,
		FudgeWandRequest.FUDGEWAND,
		GameShoppeRequest.GAMESHOPPE,
		HermitRequest.HERMIT,
		IsotopeSmitheryRequest.ISOTOPE_SMITHERY,
		LunarLunchRequest.LUNAR_LUNCH,
		MrStoreRequest.MR_STORE,
		QuartersmasterRequest.FRATBOY,
		SwaggerShopRequest.SWAGGER_SHOP,
		TicketCounterRequest.TICKET_COUNTER,
		Tr4pz0rRequest.L33T_TR4PZ0R,
		TravelingTraderRequest.TRAVELER,
		VendingMachineRequest.VENDING_MACHINE,
	};

	public static final String [] MASTERS = new String[ COINMASTERS.length ];
	static
	{
		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			MASTERS[ i ] = StringUtilities.getCanonicalName( COINMASTERS[ i ].getMaster() );
			COINMASTERS[ i ].registerPurchaseRequests();
		}
	};

	public static void reset()
	{
		// Nothing to do, but calling this will run the static
		// initialization the first time this class is accessed.
	}

	public static CoinmasterData findCoinmaster( final String master )
	{
		List matchingNames = StringUtilities.getMatchingNames( MASTERS, master );

		int size = matchingNames.size();

		if ( size == 0 )
		{
			return null;
		}

		String match = ( size == 1 ) ?
			(String) matchingNames.get( 0 ) :
			StringUtilities.getCanonicalName( master ).trim();

		for ( int i = 0; i < MASTERS.length; ++i )
		{
			if ( match.equals( MASTERS[ i ] ) )
			{
				return COINMASTERS[ i ];
			}
		}

		return null;
	}

	public static CoinmasterData findBuyer( final String itemName )
	{
		if ( itemName == null )
		{
			return null;
		}

		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			CoinmasterData data = COINMASTERS[ i ];
			if ( data.canSellItem( itemName ) )
			{
				return data;
			}
		}

		return null;
	}

	public static CoinmasterData findSeller( final String itemName )
	{
		for ( int i = 0; i < COINMASTERS.length; ++i )
		{
			CoinmasterData data = COINMASTERS[ i ];
			if ( data.canBuyItem( itemName ) )
			{
				return data;
			}
		}
		return null;
	}
}
