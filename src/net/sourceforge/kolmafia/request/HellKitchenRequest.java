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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class HellKitchenRequest
	extends CafeRequest
{
	public HellKitchenRequest( final String name )
	{
		super( "Hell's Kitchen", "3" );

		int itemId = ItemDatabase.getItemId( name );
		int price = Math.max( 1, ItemDatabase.getPriceById( itemId ) ) * 3;
		this.setItem( name, itemId, price );
	}

	@Override
	public void run()
	{
		if ( KoLCharacter.inBadMoon() )
		{
			super.run();
		}
	}

	public static final boolean onMenu( final String name )
	{
		return KoLConstants.kitchenItems.contains( name );
	}

	public static final void getMenu()
	{
		KoLmafia.updateDisplay( "Visiting Hell's Kitchen..." );
		KoLConstants.kitchenItems.clear();
		CafeRequest.addMenuItem( KoLConstants.kitchenItems, "Jumbo Dr. Lucifer", 150 );
		CafeRequest.addMenuItem( KoLConstants.kitchenItems, "Brimstone Chicken Sandwich", 300 );
		CafeRequest.addMenuItem( KoLConstants.kitchenItems, "Lord of the Flies-sized fries", 300 );
		CafeRequest.addMenuItem( KoLConstants.kitchenItems, "Double Bacon Beelzeburger", 300 );
		CafeRequest.addMenuItem( KoLConstants.kitchenItems, "Imp Ale", 75 );
		ConcoctionDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	public static final void reset()
	{
		CafeRequest.reset( KoLConstants.kitchenItems );
	}
}
