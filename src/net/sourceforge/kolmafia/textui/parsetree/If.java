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

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class If
	extends Conditional
{
	private final ArrayList elseLoops;

	public If( final Scope scope, final Value condition )
	{
		super( scope, condition );
        elseLoops = new ArrayList();
	}

	public Iterator getElseLoops()
	{
		return elseLoops.iterator();
	}

	public void addElseLoop( final Conditional elseLoop )
	{
        elseLoops.add( elseLoop );
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		Value result = super.execute( interpreter );
		if ( interpreter.getState() != Interpreter.STATE_NORMAL || result == DataTypes.TRUE_VALUE )
		{
			return result;
		}

		// Conditional failed. Move to else clauses

		Iterator it = elseLoops.iterator();
		Conditional elseLoop;

		while ( it.hasNext() )
		{
			elseLoop = (Conditional) it.next();
			result = elseLoop.execute( interpreter );

			if ( interpreter.getState() != Interpreter.STATE_NORMAL || result == DataTypes.TRUE_VALUE )
			{
				return result;
			}
		}

		return DataTypes.FALSE_VALUE;
	}

	@Override
	public String toString()
	{
		return "if";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<IF>" );

        getCondition().print( stream, indent + 1 );
        getScope().print( stream, indent + 1 );

		Iterator it = getElseLoops();
		while ( it.hasNext() )
		{
			Conditional currentElse = (Conditional) it.next();
			currentElse.print( stream, indent );
		}
	}
	
	@Override
	public boolean assertBarrier()
	{	// Summary: an If returns if every contained block of code
		// returns, and the final block is an Else (not an ElseIf).
		if ( !getScope().assertBarrier() )
		{
			return false;
		}
		
		Iterator it = elseLoops.iterator();
		Conditional elseLoop = null;

		while ( it.hasNext() )
		{
			elseLoop = (Conditional) it.next();
			if ( !elseLoop.getScope().assertBarrier() )
			{
				return false;
			}
		}
	
		return elseLoop instanceof Else;
	}
	
	@Override
	public boolean assertBreakable()
	{
		if ( getScope().assertBreakable() )
		{
			return true;
		}
		
		Iterator it = elseLoops.iterator();

		while ( it.hasNext() )
		{
			Conditional elseLoop = (Conditional) it.next();
			if ( elseLoop.getScope().assertBreakable() )
			{
				return true;
			}
		}
	
		return false;
	}
}
