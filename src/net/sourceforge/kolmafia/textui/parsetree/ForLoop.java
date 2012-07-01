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

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public class ForLoop
	extends Loop
{
	private final VariableReference variable;
	private final Value initial;
	private final Value last;
	private final Value increment;
	private final int direction;
	private final String fileName;
	private final int lineNumber;

	public ForLoop( final Scope scope, final VariableReference variable,
		final Value initial, final Value last, final Value increment,
		final int direction, final Parser parser )
	{
		super( scope );
		this.variable = variable;
		this.initial = initial;
		this.last = last;
		this.increment = increment;
		this.direction = direction;
        fileName = parser.getShortFileName();
        lineNumber = parser.getLineNumber();
	}

	public VariableReference getVariable()
	{
		return variable;
	}

	public Value getInitial()
	{
		return initial;
	}

	public Value getLast()
	{
		return last;
	}

	public Value getIncrement()
	{
		return increment;
	}

	public int getDirection()
	{
		return direction;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		interpreter.traceIndent();

		if ( interpreter.isTracing() )
		{
			interpreter.trace( toString() );
			interpreter.trace( "Initial: " + initial );
		}

		// Get the initial value
		Value initialValue = initial.execute( interpreter );
		interpreter.captureValue( initialValue );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + initialValue );
		}

		if ( initialValue == null )
		{
			interpreter.traceUnindent();
			return null;
		}

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Last: " + last );
		}

		// Get the final value
		Value lastValue = last.execute( interpreter );
		interpreter.captureValue( lastValue );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + lastValue );
		}

		if ( lastValue == null )
		{
			interpreter.traceUnindent();
			return null;
		}

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Increment: " + increment );
		}

		// Get the increment
		Value incrementValue = increment.execute( interpreter );
		interpreter.captureValue( incrementValue );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + incrementValue );
		}

		if ( incrementValue == null )
		{
			interpreter.traceUnindent();
			return null;
		}

		long current = initialValue.intValue();
		long increment = incrementValue.intValue();
		long end = lastValue.intValue();

		boolean up = false;

		if ( direction > 0 )
		{
			up = true;
		}
		else if ( direction < 0 )
		{
			up = false;
		}
		else
		{
			up = current <= end;
		}

		if ( up && increment < 0 || !up && increment > 0 )
		{
			increment = -increment;
		}

		// Make sure the loop will eventually terminate

		if ( current != end && increment == 0 )
		{
			throw interpreter.runtimeException( "Start not equal to end and increment equals 0", fileName, lineNumber );
		}

		while ( up && current <= end || !up && current >= end )
		{
			// Bind variable to current value
            variable.setValue( interpreter, new Value( current ) );

			// Execute the scope
			Value result = super.execute( interpreter );

			if ( interpreter.getState() == Interpreter.STATE_BREAK )
			{
				interpreter.setState( Interpreter.STATE_NORMAL );
				interpreter.traceUnindent();
				return DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() != Interpreter.STATE_NORMAL )
			{
				interpreter.traceUnindent();
				return result;
			}

			// Calculate next value
			current += increment;
		}

		interpreter.traceUnindent();
		return DataTypes.VOID_VALUE;
	}

	@Override
	public String toString()
	{
		return "for";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		int direction = getDirection();
		stream.println( "<FOR " + ( direction < 0 ? "downto" : direction > 0 ? "upto" : "to" ) + " >" );
        getVariable().print( stream, indent + 1 );
        getInitial().print( stream, indent + 1 );
        getLast().print( stream, indent + 1 );
        getIncrement().print( stream, indent + 1 );
        getScope().print( stream, indent + 1 );
	}
}
