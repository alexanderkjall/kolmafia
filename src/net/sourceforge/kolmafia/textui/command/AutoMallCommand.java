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

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.AutoMallRequest;

public class AutoMallCommand
	extends AbstractCommand
{
	public AutoMallCommand()
	{
		this.usage = " - dump all profitable, non-memento items into the Mall.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		AutoMallCommand.automall();
	}

	public static void automall()
	{
		// Now you've got all the items used up, go ahead and prepare to
		// sell anything that's left.

		int itemCount;

		AdventureResult currentItem;
		Object[] items = KoLConstants.profitableList.toArray();

		ArrayList sellList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[ i ];

			if ( KoLConstants.mementoList.contains( currentItem ) )
			{
				continue;
			}

			if ( currentItem.getItemId() == ItemPool.MEAT_PASTE || currentItem.getItemId() == ItemPool.MEAT_STACK || currentItem.getItemId() == ItemPool.DENSE_STACK )
			{
				continue;
			}

			itemCount = currentItem.getCount( KoLConstants.inventory );

			if ( itemCount > 0 )
			{
				sellList.add( currentItem.getInstance( itemCount ) );
			}
		}

		if ( !sellList.isEmpty() )
		{
			RequestThread.postRequest( new AutoMallRequest( sellList.toArray() ) );
		}
	}
}
