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

import net.sourceforge.kolmafia.RequestLogger;

public class PlayerSnapshotCommand
	extends AbstractCommand
{
	public PlayerSnapshotCommand()
	{
        usage = " [status],[equipment],[effects],[<etc>.] - record data, \"log snapshot\" for all common data.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equals( "snapshot" ) )
		{
            snapshot( "moon, status, equipment, skills, effects, modifiers" );
			return;
		}

        snapshot( parameters );
	}

	private void snapshot( final String parameters )
	{
		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		RequestLogger.getDebugStream().println();
		RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		StringBuilder title = new StringBuilder( "Player Snapshot" );

		int leftIndent = ( 46 - title.length() ) / 2;
		for ( int i = 0; i < leftIndent; ++i )
		{
			title.insert( 0, ' ' );
		}

		RequestLogger.updateSessionLog( title.toString() );
		RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		RequestLogger.getDebugStream().println( title.toString() );
		RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		String[] options = parameters.split( "\\s*,\\s*" );

		for ( int i = 0; i < options.length; ++i )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( " > " + options[ i ] );

			ShowDataCommand.show( options[ i ], true );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		RequestLogger.getDebugStream().println();
		RequestLogger.getDebugStream().println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog();

		RequestLogger.getDebugStream().println();
		RequestLogger.getDebugStream().println();
	}
}
