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

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PrefTraceCommand
	extends AbstractCommand
{
	private static ArrayList audience = null;	// keeps listeners from being GC'd

	public PrefTraceCommand()
	{
        usage = " <name> [, <name>]... - watch changes to indicated preferences";
	}

	@Override
	public void run( String command, final String parameters )
	{
		if ( audience != null )
		{
			audience = null;
			RequestLogger.printLine( "Previously watched prefs have been cleared." );
		}
	
		if ( parameters.equals( "" ) )
		{
			return;
		}
		
		String[] prefList = parameters.split( "\\s*,\\s*" );
		audience = new ArrayList();
		for ( int i = 0; i < prefList.length; ++i )
		{
			audience.add( new Listener( prefList[ i ] ) );		
		}
	}
	
	private static class Listener
		implements PreferenceListener
	{
		String name;
		
		public Listener( String name )
		{
			this.name = name;
			PreferenceListenerRegistry.registerListener( name, this );
            update();
		}
		
		public void update()
		{
			String msg = "ptrace: " + name + " = " +
				Preferences.getString( name );
			RequestLogger.updateSessionLog( msg );
			if ( RequestLogger.isDebugging() )
			{
				StaticEntity.printStackTrace( msg );
				// msg also gets displayed in CLI
			}
			else 
			{
				RequestLogger.printLine( msg );
			}
		}
	}
}
