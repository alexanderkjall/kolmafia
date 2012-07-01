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

public class Expression
	extends Value
{
	Value conditional;
	Value lhs;
	Value rhs;
	Operator oper;

	public Expression()
	{
	}

	public Expression( final Value conditional, final Value lhs, final Value rhs )
	{
		this.conditional = conditional;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Expression( final Value lhs, final Value rhs, final Operator oper )
	{
		this.lhs = lhs;
		this.rhs = rhs;
		this.oper = oper;
	}

	@Override
	public Type getType()
	{
		Type leftType = lhs.getType();

		// Unary operators have no right hand side
		if ( rhs == null )
		{
			return leftType;
		}

		// Ternary expressions have no real operator
		if ( oper == null )
		{
			return leftType;
		}

		Type rightType = rhs.getType();

		// String concatenation always yields a string
		if ( oper.equals( "+" ) && ( leftType.equals( DataTypes.TYPE_STRING ) || rightType.equals( DataTypes.TYPE_STRING ) ) )
		{
			return DataTypes.STRING_TYPE;
		}

		// If it's an integer operator, must be integers
		if ( oper.isInteger() )
		{
			return DataTypes.INT_TYPE;
		}

		// If it's a logical operator, must be both integers or both
		// booleans
		if ( oper.isLogical() )
		{
			return leftType;
		}

		// If it's not arithmetic, it's boolean
		if ( !oper.isArithmetic() )
		{
			return DataTypes.BOOLEAN_TYPE;
		}

		// Coerce int to float
		if ( leftType.equals( DataTypes.TYPE_FLOAT ) )
		{
			return DataTypes.FLOAT_TYPE;
		}

		// Otherwise result is whatever is on right
		return rightType;
	}

	public Value getLeftHandSide()
	{
		return lhs;
	}

	public Value getRightHandSide()
	{
		return rhs;
	}

	public Operator getOperator()
	{
		return oper;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( conditional != null )
		{
			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Operator: ?:" );
			}

			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Condition: " + conditional );
			}
			Value conditionResult = conditional.execute( interpreter );
			interpreter.captureValue( conditionResult );

			if ( conditionResult == null )
			{
				conditionResult = DataTypes.VOID_VALUE;
			}
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult.toQuotedString() );
			}
			interpreter.traceUnindent();

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			Value expression;
			String tag;

			if ( conditionResult.intValue() != 0 )
			{
				expression = lhs;
				tag = "True value: ";
			}
			else
			{
				expression = rhs;
				tag = "False value: ";
			}

			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( tag + expression );
			}

			Value executeResult = expression.execute( interpreter );

			if ( executeResult == null )
			{
				executeResult = DataTypes.VOID_VALUE;
			}

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + executeResult.toQuotedString() );
			}
			interpreter.traceUnindent();

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			if ( Operator.isStringLike( lhs.getType() ) != Operator.isStringLike( rhs.getType() ) )
			{
				executeResult = executeResult.toStringValue();
			}
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "<- " + executeResult );
			}
			interpreter.traceUnindent();

			return executeResult;
		}

		return oper.applyTo( interpreter, lhs, rhs );
	}

	@Override
	public String toString()
	{
		if ( conditional != null )
		{
			return "( " + conditional.toQuotedString() + " ? " + lhs.toQuotedString() + " : " + rhs.toQuotedString() + " )";
		}

		if ( rhs == null )
		{
			return oper.toString() + " " + lhs.toQuotedString();
		}

		return "( " + lhs.toQuotedString() + " " + oper.toString() + " " + rhs.toQuotedString() + " )";
	}

	@Override
	public String toQuotedString()
	{
		return toString();
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		if ( conditional != null )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<OPER ?:>" );
            conditional.print( stream, indent + 1 );
		}
		else
		{
            getOperator().print( stream, indent );
		}
        lhs.print( stream, indent + 1 );
		if ( rhs != null )
		{
            rhs.print( stream, indent + 1 );
		}
	}
}
