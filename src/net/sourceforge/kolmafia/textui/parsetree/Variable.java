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

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.ScriptException;

public class Variable
	extends Symbol
{
	Type type;
	Value content;
	Value expression = null;
	boolean isStatic = false;

	public Variable( final Type type )
	{
		super( null );
		this.type = type;
        content = new Value( type );
	}

	public Variable( final String name, final Type type )
	{
		super( name );
		this.type = type;
        content = new Value( type );
	}

	public Type getType()
	{
		return type;
	}

	public Type getBaseType()
	{
		return type.getBaseType();
	}

	public boolean isStatic()
	{
		return isStatic;
	}

	public void markStatic()
	{
        isStatic = true;
	}

	public Value getValue( final Interpreter interpreter )
	{
		if ( expression != null )
		{
            content = expression.execute( interpreter );
		}

		return content;
	}

	public Type getValueType( final Interpreter interpreter )
	{
		return getValue( interpreter ).getType();
	}

	public Object rawValue( final Interpreter interpreter )
	{
		return getValue( interpreter ).rawValue();
	}

	public long intValue( final Interpreter interpreter )
	{
		return getValue( interpreter ).intValue();
	}

	public Value toStringValue( final Interpreter interpreter )
	{
		return getValue( interpreter ).toStringValue();
	}

	public double floatValue( final Interpreter interpreter )
	{
		return getValue( interpreter ).floatValue();
	}

	public void setExpression( final Value targetExpression )
	{
        expression = targetExpression;
	}

	public void forceValue( final Value targetValue )
	{
        content = targetValue;
        expression = null;
	}

	public void setValue( Interpreter interpreter, final Value targetValue )
	{
		if ( getBaseType().equals( targetValue.getType() ) )
		{
            content = targetValue;
            expression = null;
		}
		else if ( getBaseType().equals( DataTypes.TYPE_STRICT_STRING ) || getBaseType().equals( DataTypes.TYPE_STRING ) )
		{
            content = targetValue.toStringValue();
            expression = null;
		}
		else if ( getBaseType().equals( DataTypes.TYPE_INT ) && targetValue.getType().equals( DataTypes.TYPE_FLOAT ) )
		{
            content = targetValue.toIntValue();
            expression = null;
		}
		else if ( getBaseType().equals( DataTypes.TYPE_FLOAT ) && targetValue.getType().equals( DataTypes.TYPE_INT ) )
		{
            content = targetValue.toFloatValue();
            expression = null;
		}
		else
		{
			throw new ScriptException(
				"Internal error: Cannot assign " + targetValue.getType() + " to " + getType() );
		}
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		return getValue( interpreter );
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<VAR " + getType() + " " + getName() + ">" );
	}
}
