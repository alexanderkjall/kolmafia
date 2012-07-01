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
import java.util.Stack;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.Interpreter;

public class UserDefinedFunction
	extends Function
{
	private Scope scope;
	private final Stack callStack;

	public UserDefinedFunction( final String name, final Type type,
		final VariableReferenceList variableReferences )
	{
		super( name, type, variableReferences );

        scope = null;
        callStack = new Stack();
	}

	public void setScope( final Scope s )
	{
        scope = s;
	}

	public Scope getScope()
	{
		return scope;
	}

	@Override
	public void saveBindings( Interpreter interpreter )
	{
		if ( scope == null )
		{
			return;
		}
		
		ArrayList values = new ArrayList();

		Iterator scopes = scope.getScopes();
		while ( scopes.hasNext() )
		{
			Iterator variables = ((BasicScope) scopes.next()).getVariables();
	
			while ( variables.hasNext() )
			{
				Variable current = (Variable) variables.next();
				if ( !current.isStatic() )
				{
					values.add( current.getValue( interpreter ) );
				}
			}
		}
        callStack.push( values );
	}

	@Override
	public void restoreBindings( Interpreter interpreter )
	{
		if ( scope == null )
		{
			return;
		}

		ArrayList values = (ArrayList) callStack.pop();
		int i = 0;

		Iterator scopes = scope.getScopes();
		while ( scopes.hasNext() )
		{
			Iterator variables = ((BasicScope) scopes.next()).getVariables();
	
			while ( variables.hasNext() )
			{
				Variable current = (Variable) variables.next();
				if ( !current.isStatic() )
				{
					current.forceValue( (Value) values.get( i++ ) );
				}
			}
		}
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( StaticEntity.isDisabled( getName() ) )
		{
            printDisabledMessage( interpreter );
			return getType().initialValue();
		}

		if ( scope == null )
		{
			throw interpreter.runtimeException( "Calling undefined user function: " + getName() );
		}

		Value result = scope.execute( interpreter );

		if ( result.getType().equals( type.getBaseType() ) )
		{
			return result;
		}

		return getType().initialValue();
	}

	@Override
	public boolean assertBarrier()
	{
		return scope.assertBarrier();
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		super.print( stream, indent );
		if ( scope != null )
		{
            scope.print( stream, indent + 1 );
		}
	}
}
