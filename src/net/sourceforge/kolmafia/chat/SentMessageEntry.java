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

package net.sourceforge.kolmafia.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.GenericRequest;


public class SentMessageEntry
	extends HistoryEntry
{
	private static final Pattern DOJAX_PATTERN =
		Pattern.compile( "<!--js\\(\\s*dojax\\(\\s*['\"](.*?)['\"]\\s*\\)\\s*;?\\s*\\)-->" );

	private static final GenericRequest DOJAX_VISITOR = new GenericRequest( "" );

	private final boolean isRelayRequest;

	public SentMessageEntry( final String responseText, final long localLastSeen, boolean isRelayRequest )
	{
		super( responseText, localLastSeen );

		this.isRelayRequest = isRelayRequest;
	}

	public boolean isRelayRequest()
	{
		return isRelayRequest;
	}

	public void executeAjaxCommand()
	{
		if ( isRelayRequest )
		{
			return;
		}

		String content = getContent();

		if ( content == null )
		{
			return;
		}

		Matcher dojax = SentMessageEntry.DOJAX_PATTERN.matcher( content );

		GenericRequest request = SentMessageEntry.DOJAX_VISITOR;
		while ( dojax.find() )
		{
			// Force a GET, just like the Browser
			request.constructURLString( dojax.group( 1 ) );
			request.constructURLString( request.getFullURLString(), false );

			RequestThread.postRequest( SentMessageEntry.DOJAX_VISITOR );

			if ( request.responseText == null )
			{
				continue;
			}
		}

		dojax.reset();

		content = dojax.replaceAll( "" );

        setContent( content );
	}
}
