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
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CurseRequest
	extends GenericRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern PLAYER_PATTERN = Pattern.compile( "(?=.*action=use).*targetplayer=([^&]*)" );
	private static final Pattern QTY_PATTERN = Pattern.compile( "You have ([\\d,]+) more |You don't have any more " );

	private AdventureResult itemUsed;

	public CurseRequest( final AdventureResult item )
	{
		this( item, KoLCharacter.getPlayerId(), "" );
	}

	public CurseRequest( final AdventureResult item, final String target, final String message )
	{
		super( "curse.php" );
        itemUsed = item;
        addFormField( "action", "use" );
        addFormField( "whichitem", String.valueOf( item.getItemId() ) );
        addFormField( "targetplayer", target );
	}

	@Override
	public void run()
	{
		InventoryManager.retrieveItem( itemUsed );

		for ( int i = itemUsed.getCount(); KoLmafia.permitsContinue() && i > 0; --i )
		{
			KoLmafia.updateDisplay( "Throwing " + itemUsed.getName() +
				" at " + getFormField( "targetplayer" ) + "..." );
			super.run();
		}
	}

	@Override
	public void processResults()
	{
		CurseRequest.parseResponse( getURLString(), responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "curse.php" ) )
		{
			return;
		}

		Matcher m = CurseRequest.ITEM_PATTERN.matcher( location );
		if ( !m.find() )
		{
			return;
		}
		AdventureResult item = ItemPool.get( StringUtilities.parseInt( m.group( 1 ) ), 1 );

		m = CurseRequest.QTY_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			return;
		}
		int qty = m.group( 1 ) == null ? 0
			: StringUtilities.parseInt( m.group( 1 ) );
		qty = item.getCount( KoLConstants.inventory ) - qty;
		if ( qty != 0 )
		{
			item = item.getInstance( qty );
			ResultProcessor.processResult( item.getNegation() );
		}

		m = CurseRequest.PLAYER_PATTERN.matcher( location );
		if ( !m.find() )
		{
			return;
		}

		if ( responseText.contains( "You don't have that item" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Have not, throw not." );
			return;
		}

		if ( responseText.contains( "No message?" ) ||
                responseText.contains( "no message" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That item requires a message." );
			return;
		}

		if ( responseText.contains( "That player could not be found" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, m.group( 1 ) + " evaded your thrown item by the unusual strategy of being nonexistent." );
			return;
		}

		if ( responseText.contains( "try again later" ) ||
                responseText.contains( "cannot be used" ) ||
                responseText.contains( "can't use this item" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Can't use the item on that player at the moment." );
			return;
		}

		RequestLogger.updateSessionLog( "throw " + item +
			" at " + m.group( 1 ) );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "curse.php" ) )
		{
			return false;
		}

		return true;
	}
}
