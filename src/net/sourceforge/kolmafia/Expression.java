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

import java.util.ArrayList;
import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BasementRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Expression
{
	private static final Pattern NUM_PATTERN = Pattern.compile( "([+-]?[\\d.]+)(.*)" );
	private static final int STACK_SIZE = 128;

	protected String name;
	protected String text;

	protected char[] bytecode;	// Compiled expression
	protected ArrayList literals;	// Strings & floats needed by expression
	protected AdventureResult effect;

	private static float[] cachedStack;

	protected synchronized static float[] stackFactory( float[] recycle )
	{
		if ( recycle != null )
		{	// Reuse this stack for the next evaluation.
			cachedStack = recycle;
			return null;
		}
		else if ( cachedStack != null )
		{	// We have a stack handy; it's yours now.
			float[] rv = cachedStack;
			cachedStack = null;
			return rv;
		}
		else
		{	// We're all out of stacks.
			return new float[ STACK_SIZE ];
		}
	}

	public Expression( String text, String name )
	{
		this.name = name;
		this.text = text;

		// Let subclass initialize variables needed for
		// compilation and evaluation 
		this.initialize();

		// Compile the expression into byte code
		this.bytecode = this.validBytecodes().toCharArray();
		Arrays.sort( this.bytecode );
		String compiled = this.expr() + "r";
		//if ( name.length() > 0 && name.equalsIgnoreCase(
		//	Preferences.getString( "debugEval" ) ) )
		//{
		//	compiled = compiled.replaceAll( ".", "?$0" );
		//}
		this.bytecode = compiled.toCharArray();
		if ( this.text.length() > 0 )
		{
			KoLmafia.updateDisplay( "Expression syntax error for '" + name + "': expected end, found " + this.text );
		}
		this.text = null;
	}

	protected void initialize()
	{
	}

	public float eval()
	{
		try
		{
			return this.evalInternal();
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			KoLmafia.updateDisplay( "Unreasonably complex expression for " + 
				this.name + ": " + e );
			return 0.0f;
		}
	}

	private float evalInternal()
	{
		float[] s = stackFactory( null );
		int sp = 0;
		int pc = 0;
		float v = 0.0f;

		while ( true )
		{
			char inst = this.bytecode[ pc++ ];
			switch ( inst )
			{
// 			case '?':	// temporary instrumentation
// 				KoLmafia.updateDisplay( "\u2326 Eval " + this.name + " from " +
// 					Thread.currentThread().getName() );
// 				StringBuffer b = new StringBuffer(); 
// 				if ( pc == 1 )
// 				{
// 					b.append( "\u2326 Bytecode=" );
// 					b.append( this.bytecode );
// 					for ( int i = 1; i < this.bytecode.length; i += 2 )
// 					{
// 						b.append( ' ' );
// 						b.append( Integer.toHexString( this.bytecode[ i ] ) );
// 					}
// 					KoLmafia.updateDisplay( b.toString() );
// 					b.setLength( 0 );
// 				}
// 				b.append( "\u2326 PC=" );
// 				b.append( pc );
// 				b.append( " Stack=" );
// 				if ( sp < 0 )
// 				{
// 					b.append( sp );
// 				}
// 				else
// 				{
// 					for ( int i = 0; i < sp && i < INITIAL_STACK; ++i )
// 					{
// 						b.append( ' ' );
// 						b.append( s[ i ] );
// 					}
// 				}
// 				KoLmafia.updateDisplay( b.toString() );
// 				continue;

			case 'r':
				v = s[ --sp ];
				stackFactory( s );	// recycle this stack
				return v;

			case '^':
				v = (float) Math.pow( s[ --sp ], s[ --sp ] );
				break;

			case '*':
				v = s[ --sp ] * s[ --sp ];
				break;
			case '/':
				v = s[ --sp ] / s[ --sp ];
				break;
				
			case '+':
				v = s[ --sp ] + s[ --sp ];
				break;
			case '-':
				v = s[ --sp ] - s[ --sp ];
				break;

			case 'c':
				v = (float) Math.ceil( s[ --sp ] );
				break;
			case 'f':
				v = (float) Math.floor( s[ --sp ] );
				break;
			case 's':
				v = (float) Math.sqrt( s[ --sp ] );
				break;
			case 'p':
				v = StringUtilities.parseFloat( Preferences.getString( (String) this.literals.get( (int) s[ --sp ] ) ) );
				break;
			case 'm':
				v = Math.min( s[ --sp ], s[ --sp ] );
				break;
			case 'x':
				v = Math.max( s[ --sp ], s[ --sp ] );
				break;

			case '#':
				v = (Float) this.literals.get( (int) s[--sp] );
				break;
				
			// Valid with ModifierExpression:
			case 'l':
				v = Modifiers.currentLocation.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0f : 1.0f;
				break;
			case 'z':
				v = Modifiers.currentZone.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0f : 1.0f;
				break;
			case 'w':
				v = Modifiers.currentFamiliar.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0f : 1.0f;
				break;
			case 'h':
				v = Modifiers.mainhandClass.indexOf( (String) this.literals.get( (int) s[ --sp ] ) ) == -1 ? 0.0f : 1.0f;
				break;
			case 'e':
				AdventureResult eff = new AdventureResult( (String) this.literals.get( (int) s[ --sp ] ), 1, true );
				v = eff == null ? 0.0f :
					Math.max( 0, eff.getCount( KoLConstants.activeEffects ) );
				break;
			case 'b':
				String elem = (String) this.literals.get( (int) s[ --sp ] );
				int element = elem.equals( "cold" ) ? Modifiers.COLD_RESISTANCE :
							  elem.equals( "hot" ) ? Modifiers.HOT_RESISTANCE :
							  elem.equals( "sleaze" ) ? Modifiers.SLEAZE_RESISTANCE :
							  elem.equals( "spooky" ) ? Modifiers.SPOOKY_RESISTANCE :
							  elem.equals( "stench" ) ? Modifiers.STENCH_RESISTANCE :
							  elem.equals( "slime" ) ? Modifiers.SLIME_RESISTANCE :
							  -1;
				v = KoLCharacter.currentNumericModifier( element );
				break;
			case 'A':
				v = KoLCharacter.getAscensions();
				break;
			case 'B':
				v = HolidayDatabase.getBloodEffect();
				break;
			case 'C':
				v = KoLCharacter.getMinstrelLevel();
				break;
			case 'D':
				v = KoLCharacter.getInebriety();
				break;
			case 'E':
			{
				int size = KoLConstants.activeEffects.size();
				AdventureResult[] effectsArray = new AdventureResult[ size ];
				KoLConstants.activeEffects.toArray( effectsArray );

				v = 0;
				for ( int i = 0; i < size; i++ )
				{
					AdventureResult effect = effectsArray[ i ];
					int duration = effect.getCount();
					if ( duration != Integer.MAX_VALUE )
					{
						v++;
					}
				}
				break;
			}
			case 'F':
				v = KoLCharacter.getFullness();
				break;
			case 'G':
				v = HolidayDatabase.getGrimaciteEffect() / 10.0f;
				break;
			case 'H':
				v = Modifiers.hoboPower;
				break;
			case 'J':
				v = HolidayDatabase.getHoliday().equals( "Festival of Jarlsberg" ) ? 1.0f : 0.0f;
				break;
			case 'L':
				v = KoLCharacter.getLevel();
				break;
			case 'M':
				v = HolidayDatabase.getMoonlight();
				break;
			case 'R':
				v = KoLCharacter.getReagentPotionDuration();
				break;
			case 'S':
				v = KoLCharacter.getSpleenUse();
				break;
			case 'T':
				v = this.effect == null ? 0.0f :
					Math.max( 1, this.effect.getCount( KoLConstants.activeEffects ) );
				break;
			case 'U':
				v = KoLCharacter.getTelescopeUpgrades();
				break;
			case 'W':
				v = Modifiers.currentWeight;
				break;
			case 'X':
				v = KoLCharacter.getGender();
				break;
			
			// Valid with MonsterExpression:
			case '\u0080':
				v = KoLCharacter.getAdjustedMuscle();
				break;
			case '\u0081':
				v = KoLCharacter.getAdjustedMysticality();
				break;
			case '\u0082':
				v = KoLCharacter.getAdjustedMoxie();
				break;
			case '\u0083':
				v = KoLCharacter.getMonsterLevelAdjustment();
				break;
			case '\u0084':
				v = KoLCharacter.getMindControlLevel();
				break;
			case '\u0085':
				v = KoLCharacter.getMaximumHP();
				break;
			case '\u0086':
				v = BasementRequest.getBasementLevel();
				break;
					
			default:
				if ( inst > '\u00FF' )
				{
					v = inst - 0x8000;
					break;
				}
				KoLmafia.updateDisplay( "Evaluator bytecode invalid at " +
							(pc - 1) + ": " + String.valueOf( this.bytecode ) );
				return 0.0f;
			}
			s[ sp++ ] = v;
		}
	}

	protected String validBytecodes()
	{	// Allowed operations in the A-Z range.
		return "";
	}

	protected void expect( String token )
	{
		if ( this.text.startsWith( token ) )
		{
			this.text = this.text.substring( token.length() );
			return;
		}
		KoLmafia.updateDisplay( "Evaluator syntax error: expected " + token +
					", found " + this.text );
	}

	protected String until( String token )
	{
		int pos = this.text.indexOf( token );
		if ( pos == -1 )
		{
			KoLmafia.updateDisplay( "Evaluator syntax error: expected " + token +
						", found " + this.text );
			return "";
		}
		String rv = this.text.substring( 0, pos );
		this.text = this.text.substring( pos + token.length() );
		return rv;
	}

	protected boolean optional( String token )
	{
		if ( this.text.startsWith( token ) )
		{
			this.text = this.text.substring( token.length() );
			return true;
		}
		return false;
	}

	protected char optional( String token1, String token2 )
	{
		if ( this.text.startsWith( token1 ) )
		{
			this.text = this.text.substring( token1.length() );
			return token1.charAt( 0 );
		}
		if ( this.text.startsWith( token2 ) )
		{
			this.text = this.text.substring( token2.length() );
			return token2.charAt( 0 );
		}
		return '\0';
	}

	protected String literal( Object value, char op )
	{
		if ( this.literals == null )
		{
			this.literals = new ArrayList();
		}
		this.literals.add( value == null ? "" : value );
		return String.valueOf( (char)( this.literals.size() - 1 + 0x8000 ) ) + op;
	}

	protected String expr()
	{
		String rv = this.term();
		while ( true )
		{
			switch ( this.optional( "+", "-" ) )
			{
			case '+':
				rv = this.term() + rv + "+";
				break;
			case '-':
				rv = this.term() + rv + "-";
				break;
			default:
				return rv;
			}
		}
	}

	protected String term()
	{
		String rv = this.factor();
		while ( true )
		{
			switch ( this.optional( "*", "/" ) )
			{
			case '*':
				rv = this.factor() + rv + "*";
				break;
			case '/':
				rv = this.factor() + rv + "/";
				break;
			default:
				return rv;
			}
		}
	}

	protected String factor()
	{
		String rv = this.value();
		while ( this.optional( "^", "**" ) != '\0' )
		{
			rv = this.value() + rv + "^";
		}
		return rv;
	}

	protected String value()
	{
		String rv;
		if ( this.optional( "(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv;
		}
		if ( this.optional( "ceil(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "c";
		}
		if ( this.optional( "floor(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "f";
		}
		if ( this.optional( "sqrt(" ) )
		{
			rv = this.expr();
			this.expect( ")" );
			return rv + "s";
		}
		if ( this.optional( "min(" ) )
		{
			rv = this.expr();
			this.expect( "," );
			rv = rv + this.expr() + "m";
			this.expect( ")" );
			return rv;
		}
		if ( this.optional( "max(" ) )
		{
			rv = this.expr();
			this.expect( "," );
			rv = rv + this.expr() + "x";
			this.expect( ")" );
			return rv;
		}
		if ( this.optional( "pref(" ) )
		{
			return this.literal( this.until( ")" ), 'p' );
		}

		rv = this.function();
		if ( rv != null )
		{
			return rv;
		}

		if ( this.text.length() == 0 )
		{
			KoLmafia.updateDisplay( "Evaluator syntax error: unexpected end of expr" );
			return "\u8000";	
		}
		rv = this.text.substring( 0, 1 );
		if ( rv.charAt( 0 ) >= 'A' && rv.charAt( 0 ) <= 'Z' )
		{
			this.text = this.text.substring( 1 );
			if ( Arrays.binarySearch( this.bytecode, rv.charAt( 0 ) ) < 0 )
			{
				KoLmafia.updateDisplay( "Evaluator syntax error: '" + rv +
					"' is not valid in this context" );
				return "\u8000";
			}
			return rv;
		}
		Matcher m = NUM_PATTERN.matcher( this.text );
		if ( m.matches() )
		{
			float v = Float.parseFloat( m.group( 1 ) );
			this.text = m.group( 2 );
			if ( v % 1.0f == 0.0f && v >= -0x7F00 && v < 0x8000 )
			{
				return String.valueOf( (char)((int)v + 0x8000) );
			}
			else
			{
				return this.literal( v, '#' );
			}
		}
		if ( this.optional( "-" ) )
		{
			return this.value() + "\u8000-";
		}
		KoLmafia.updateDisplay( "Evaluator syntax error: can't understand " + this.text );
		this.text = "";
		return "\u8000";
	}

	protected String function()
	{
		return null;
	}
}
