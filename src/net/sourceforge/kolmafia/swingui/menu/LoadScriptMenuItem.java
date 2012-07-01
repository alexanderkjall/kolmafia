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

package net.sourceforge.kolmafia.swingui.menu;

import java.io.IOException;

import javax.swing.JFileChooser;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this internal class is used
 * to process the request for loading a script.
 */

public class LoadScriptMenuItem
	extends ThreadedMenuItem
{
	public LoadScriptMenuItem()
	{
		this( "Load script...", null );
	}

	public LoadScriptMenuItem( final String scriptName, final String scriptPath )
	{
		super( scriptName, new LoadScriptListener( scriptPath ) );
	}

	private static class LoadScriptListener
		extends ThreadedListener
	{
		private final String scriptPath;

		public LoadScriptListener( String scriptPath )
		{
			this.scriptPath = scriptPath;
		}

		@Override
		protected void execute()
		{
			String executePath = scriptPath;

			try
			{
				if ( scriptPath == null )
				{
					JFileChooser chooser = new JFileChooser( KoLConstants.SCRIPT_LOCATION.getCanonicalPath() );
					int returnVal = chooser.showOpenDialog( null );

					if ( chooser.getSelectedFile() == null )
					{
						return;
					}

					if ( returnVal == JFileChooser.APPROVE_OPTION )
					{
						executePath = chooser.getSelectedFile().getCanonicalPath();
					}
				}
			}
			catch ( IOException e )
			{

			}

			if ( executePath == null )
			{
				return;
			}

			KoLmafia.forceContinue();

			if ( hasShiftModifier() )
			{
				CommandDisplayFrame.executeCommand( "edit " + executePath );
			}
			else
			{
				CommandDisplayFrame.executeCommand( "call " + executePath );
			}
		}
	}
}
