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

package net.sourceforge.kolmafia;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class ModifierExpression
	extends Expression
{
	public ModifierExpression( String text, String name )
	{
		super( text, name );
	}

	@Override
	protected void initialize()
	{
		// The first check also matches "[zone(The Slime Tube)]"
		// Hence the second check.
		if ( text.contains( "T" ) && EffectDatabase.contains( name ) )
		{
            effect = new AdventureResult( name, 0, true );
		}
	}

	@Override
	protected String validBytecodes()
	{
		return super.validBytecodes() + "ABCDEFGHJLMRSTUWX";
	}

	@Override
	protected String function()
	{
		if ( optional( "loc(" ) )
		{
			return literal( until( ")" ).toLowerCase(), 'l' );
		}
		if ( optional( "zone(" ) )
		{
			return literal( until( ")" ).toLowerCase(), 'z' );
		}
		if ( optional( "fam(" ) )
		{
			return literal( until( ")" ).toLowerCase(), 'w' );
		}
		if ( optional( "mainhand(" ) )
		{
			return literal( until( ")" ).toLowerCase(), 'h' );
		}
		if ( optional( "effect(" ) )
		{
			return literal( until( ")" ).toLowerCase(), 'e' );
		}
		if ( optional( "res(" ) )
		{
			return literal( until( ")" ).toLowerCase(), 'b' );
		}

		return null;
	}
}
