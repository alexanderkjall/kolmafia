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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.request.PeeVPeeRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.PvpManager;

public class PvpAttackCommand
	extends AbstractCommand
{
	public PvpAttackCommand()
	{
        usage = " <target> [, <target>...] - PvP for items or fame";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			RequestLogger.printLine( "You must specify a target." );
			return;
		}

		String[] names = parameters.split( "\\s*,\\s*" );
		ProfileRequest[] targets = new ProfileRequest[ names.length ];

		for ( int i = 0; i < names.length; ++i )
		{
			String playerId = ContactManager.getPlayerId( names[ i ] );
			if ( !playerId.equals( names[ i ] ) )
			{
				continue;
			}

			String text = KoLmafia.whoisPlayer( playerId );
			Matcher idMatcher = Pattern.compile( "\\(#(\\d+)\\)" ).matcher( text );

			if ( idMatcher.find() )
			{
				ContactManager.registerPlayerId( names[ i ], idMatcher.group( 1 ) );
			}
			else
			{
				names[ i ] = null;
			}
		}

		for ( int i = 0; i < names.length; ++i )
		{
			if ( names[ i ] == null )
			{
				continue;
			}

			KoLmafia.updateDisplay( "Retrieving player data for " + names[ i ] + "..." );
			targets[ i ] = new ProfileRequest( names[ i ] );
			targets[ i ].run();
		}

		String mission = KoLCharacter.canInteract() ? "lootwhatever" : "fame";
		PeeVPeeRequest request = new PeeVPeeRequest( "", 0, mission );
		PvpManager.executePvpRequest( targets, request );
	}
}
