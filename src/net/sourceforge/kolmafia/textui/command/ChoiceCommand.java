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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.ChoiceManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChoiceCommand
	extends AbstractCommand
{
	public ChoiceCommand()
	{
		this.usage = " [<number>|<text>] - list or choose choice adventure options.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( GenericRequest.choiceHandled || ChoiceManager.lastResponseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You aren't in a choice adventure." );
		}
		if ( parameters.equals( "" ) )
		{
			ChoiceCommand.printChoices();
			return;
		}
		int decision = 0;
		TreeMap choices = ChoiceCommand.parseChoices();
		if ( StringUtilities.isNumeric( parameters ) )
		{
			decision = StringUtilities.parseInt( parameters );
		}
		else
		{
			Iterator i = choices.entrySet().iterator();
			while ( i.hasNext() )
			{
				Map.Entry e = (Map.Entry) i.next();
				if ( ((String) e.getValue()).toLowerCase().indexOf( parameters.toLowerCase() ) != -1 )
				{
					decision = ((Integer) e.getKey()).intValue();
					break;
				}
			}
		}
		
		if ( !choices.containsKey( IntegerPool.get( decision ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That isn't one of your choices." );
		}
		
		ChoiceManager.processChoiceAdventure( decision );
	}
	
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<form(?=.*?name=option value=(\\d+)).*?class=button.*?value=\"([^\"]+)\".*?</form>", Pattern.DOTALL );

	public static TreeMap parseChoices()
	{
		TreeMap rv = new TreeMap();
		if ( GenericRequest.choiceHandled || ChoiceManager.lastResponseText == null )
		{
			return rv;
		}
		String[][] possibleDecisions = ChoiceManager.choiceSpoilers( ChoiceManager.lastChoice );
		if ( possibleDecisions == null )
		{
			possibleDecisions = new String[][] { null, null, {} };
		}
		
		Matcher m = OPTION_PATTERN.matcher( ChoiceManager.lastResponseText );
		while ( m.find() )
		{
			int decision = Integer.parseInt( m.group( 1 ) );
			String text = m.group( 2 );
			if ( decision > 0 && decision <= possibleDecisions[ 2 ].length )
			{
				text = text + " (" + possibleDecisions[ 2 ][ decision - 1 ] + ")";
			}
			rv.put( IntegerPool.get( decision ), text );
		}
		return rv;
	}
	
	public static void printChoices()
	{
		TreeMap choices = ChoiceCommand.parseChoices();
		Iterator i = choices.entrySet().iterator();
		while ( i.hasNext() )
		{
			Map.Entry e = (Map.Entry) i.next();
			RequestLogger.printLine( "<b>choice " + e.getKey() +
				"</b>: " + e.getValue() );
		}
	}
}
