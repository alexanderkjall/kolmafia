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

import java.util.List;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class ExtendEffectCommand
	extends AbstractCommand
{
	public ExtendEffectCommand()
	{
		this.usage = "[?] <effect> [, <effect>]... - extend duration of effects.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.contains( "," ) )
		{
			String[] effects = parameters.split( "\\s*,\\s*" );
			for ( int i = 0; i < effects.length; ++i )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeCommand( cmd, effects[ i ] );
			}

			return;
		}

		int effectId = EffectDatabase.getEffectId( parameters );
		if ( effectId != -1 )
		{
			String effect = EffectDatabase.getEffectName( effectId );
			String action = MoodManager.getDefaultAction( "lose_effect", effect );
			if ( action.equals( "" ) )
			{
				action = EffectDatabase.getActionNote( effect );
				if ( action != null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "No direct source for: " + effect );
					RequestLogger.printLine( "It may be obtainable via " + action + "." );
				}
				else
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "No booster known: " + effect );
				}
				return;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				KoLmafia.updateDisplay( effect + " &lt;= " + action );
			}
			else
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
			}
			return;
		}

		List names = EffectDatabase.getMatchingNames( parameters );
		if ( names.isEmpty() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unknown effect: " + parameters );
			return;
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "Ambiguous effect name: " + parameters );
		RequestLogger.printList( names );
	}
}
