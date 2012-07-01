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

package net.sourceforge.kolmafia.textui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.Assignment;
import net.sourceforge.kolmafia.textui.parsetree.BasicScope;
import net.sourceforge.kolmafia.textui.parsetree.BasicScript;
import net.sourceforge.kolmafia.textui.parsetree.CompositeReference;
import net.sourceforge.kolmafia.textui.parsetree.Conditional;
import net.sourceforge.kolmafia.textui.parsetree.Else;
import net.sourceforge.kolmafia.textui.parsetree.ElseIf;
import net.sourceforge.kolmafia.textui.parsetree.Expression;
import net.sourceforge.kolmafia.textui.parsetree.ForEachLoop;
import net.sourceforge.kolmafia.textui.parsetree.ForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionCall;
import net.sourceforge.kolmafia.textui.parsetree.FunctionInvocation;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.FunctionReturn;
import net.sourceforge.kolmafia.textui.parsetree.If;
import net.sourceforge.kolmafia.textui.parsetree.LoopBreak;
import net.sourceforge.kolmafia.textui.parsetree.LoopContinue;
import net.sourceforge.kolmafia.textui.parsetree.Operator;
import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode;
import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNodeList;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RepeatUntilLoop;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.ScriptExit;
import net.sourceforge.kolmafia.textui.parsetree.SortBy;
import net.sourceforge.kolmafia.textui.parsetree.StaticScope;
import net.sourceforge.kolmafia.textui.parsetree.Switch;
import net.sourceforge.kolmafia.textui.parsetree.SwitchScope;
import net.sourceforge.kolmafia.textui.parsetree.Try;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.TypeDef;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.ValueList;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.textui.parsetree.VariableReferenceList;
import net.sourceforge.kolmafia.textui.parsetree.WhileLoop;

import net.sourceforge.kolmafia.utilities.ByteArrayStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Parser
{
	// Variables used during parsing

	private String fileName;
	private String shortFileName;
	private String scriptName;
	private InputStream istream;

	private LineNumberReader commandStream;
	private String currentLine;
	private String nextLine;
	private String currentToken;
	private int lineNumber;

	private String fullLine;

	private TreeMap imports;
	private Function mainMethod = null;
	private String notifyRecipient = null;

	public Parser()
	{
		this( null, null, null );
	}

	public Parser( final File scriptFile, final InputStream stream, final TreeMap imports )
	{
		this.imports = ( imports != null ) ? imports : new TreeMap();

		if ( scriptFile != null )
		{
            fileName = scriptFile.getPath();
            shortFileName = fileName.substring( fileName.lastIndexOf( File.separator ) + 1 );
            istream = DataUtilities.getInputStream( scriptFile );
		}
		else if ( stream != null )
		{
            fileName = null;
            shortFileName = null;
            istream = stream;
		}
		else
		{
            fileName = null;
            shortFileName = null;
            istream = null;
			return;
		}

		try
		{
            commandStream = new LineNumberReader( new InputStreamReader( istream ) );
            currentLine = getNextLine();
            lineNumber = commandStream.getLineNumber();
            nextLine = getNextLine();
		}
		catch ( Exception e )
		{
			// If any part of the initialization fails,
			// then throw an exception.

			throw parseException( fileName + " could not be accessed" );
		}
	}

	private void disconnect()
	{
		try
		{
            commandStream = null;
            istream.close();
		}
		catch ( IOException e )
		{
		}
	}

	public Scope parse()
	{
		Scope scope = null;

		try
		{
			scope = parseScope( null, null, new VariableList(), Parser.getExistingFunctionScope(), false, false );

			if ( currentLine != null )
			{
				throw parseException( "Script parsing error" );
			}
		}
		finally
		{
            disconnect();
		}

		return scope;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getShortFileName()
	{
		return shortFileName;
	}

	public String getScriptName()
	{
		if ( scriptName != null ) return scriptName;
		return shortFileName;
	}

	public int getLineNumber()
	{
		return lineNumber;
	}

	public TreeMap getImports()
	{
		return imports;
	}

	public Function getMainMethod()
	{
		return mainMethod;
	}

	public String getNotifyRecipient()
	{
		return notifyRecipient;
	}

	public static Scope getExistingFunctionScope()
	{
		return new Scope( RuntimeLibrary.functions, null, DataTypes.simpleTypes );
	}

	// **************** Parser *****************

	private static final HashSet<String> multiCharTokens = new HashSet<String>();
	private static final HashSet<String> reservedWords = new HashSet<String>();

	static
	{
		// Tokens
		multiCharTokens.add( "==" );
		multiCharTokens.add( "!=" );
		multiCharTokens.add( "<=" );
		multiCharTokens.add( ">=" );
		multiCharTokens.add( "||" );
		multiCharTokens.add( "&&" );
		multiCharTokens.add( "//" );
		multiCharTokens.add( "/*" );
		multiCharTokens.add( "<<" );
		multiCharTokens.add( ">>" );
		multiCharTokens.add( ">>>" );
		multiCharTokens.add( "**" );
		multiCharTokens.add( "+=" );
		multiCharTokens.add( "-=" );
		multiCharTokens.add( "*=" );
		multiCharTokens.add( "/=" );
		multiCharTokens.add( "%=" );
		multiCharTokens.add( "**=" );
		multiCharTokens.add( "&=" );
		multiCharTokens.add( "^=" );
		multiCharTokens.add( "|=" );
		multiCharTokens.add( "<<=" );
		multiCharTokens.add( ">>=" );
		multiCharTokens.add( ">>>=" );

		// Constants
		reservedWords.add( "true" );
		reservedWords.add( "false" );

		// Operators
		reservedWords.add( "contains" );
		reservedWords.add( "remove" );
		reservedWords.add( "new" );

		// Control flow
		reservedWords.add( "if" );
		reservedWords.add( "else" );
		reservedWords.add( "foreach" );
		reservedWords.add( "in" );
		reservedWords.add( "for" );
		reservedWords.add( "from" );
		reservedWords.add( "upto" );
		reservedWords.add( "downto" );
		reservedWords.add( "by" );
		reservedWords.add( "while" );
		reservedWords.add( "repeat" );
		reservedWords.add( "until" );
		reservedWords.add( "break" );
		reservedWords.add( "continue" );
		reservedWords.add( "return" );
		reservedWords.add( "exit" );
		reservedWords.add( "switch" );
		reservedWords.add( "case" );
		reservedWords.add( "default" );
		reservedWords.add( "try" );
		reservedWords.add( "finally" );
		reservedWords.add( "static" );

		// Data types
		reservedWords.add( "void" );
		reservedWords.add( "boolean" );
		reservedWords.add( "int" );
		reservedWords.add( "float" );
		reservedWords.add( "string" );
		reservedWords.add( "buffer" );
		reservedWords.add( "matcher" );
		reservedWords.add( "aggregate" );

		reservedWords.add( "item" );
		reservedWords.add( "location" );
		reservedWords.add( "class" );
		reservedWords.add( "stat" );
		reservedWords.add( "skill" );
		reservedWords.add( "effect" );
		reservedWords.add( "familiar" );
		reservedWords.add( "slot" );
		reservedWords.add( "monster" );
		reservedWords.add( "element" );
		reservedWords.add( "coinmaster" );

		reservedWords.add( "record" );
		reservedWords.add( "typedef" );
	}

	private static boolean isReservedWord( final String name )
	{
		return Parser.reservedWords.contains( name.toLowerCase() );
	}

	public Scope importFile( final String fileName, final Scope scope )
	{
		File scriptFile = KoLmafiaCLI.findScriptFile( fileName );
		if ( scriptFile == null )
		{
			throw parseException( fileName + " could not be found" );
		}

		if ( imports.containsKey( scriptFile ) )
		{
			return scope;
		}

		Scope result = scope;
		Parser parser = null;

		try
		{
			parser = new Parser( scriptFile, null, imports );
			result = parser.parseScope( scope, null, new VariableList(), scope.getParentScope(), false, false );
			if ( parser.currentLine != null )
			{
				throw parseException( "Script parsing error" );
			}
		}
		finally
		{
			if ( parser != null )
			{
				parser.disconnect();
			}
		}

        imports.put( scriptFile, scriptFile.lastModified() );
        mainMethod = null;

		return result;
	}

	private Scope parseCommandOrDeclaration( final Scope result, final Type expectedType )
	{
		Type t = parseType( result, true, true );

		// If there is no data type, it's a command of some sort
		if ( t == null )
		{
			ParseTreeNode c = parseCommand( expectedType, result, false, false, false );
			if ( c == null )
			{
				throw parseException( "command or declaration required" );
			}

			result.addCommand( c, this );
			return result;
		}

		if ( parseVariables( t, result ) )
		{
			if ( !currentToken().equals( ";" ) )
			{
				throw parseException( ";", currentToken() );
			}

            readToken(); //read ;
			return result;
		}

		//Found a type but no function or variable to tie it to
		throw parseException( "Type given but not used to declare anything" );
	}

	private Scope parseScope( final Scope startScope,
				  final Type expectedType,
				  final VariableList variables,
				  final BasicScope parentScope,
				  final boolean allowBreak,
				  final boolean allowContinue )
	{
		Scope result;
		String importString;

		result = startScope == null ? new Scope( variables, parentScope ) : startScope;
        parseScriptName();
        parseNotify();

		while ( ( importString = parseImport() ) != null )
		{
			result = importFile( importString, result );
		}

		while ( true )
		{
			if ( parseTypedef( result ) )
			{
				if ( !currentToken().equals( ";" ) )
				{
					throw parseException( ";", currentToken() );
				}

                readToken(); //read ;
				continue;
			}

			Type t = parseType( result, true, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ParseTreeNode c = parseCommand( expectedType, result, false, allowBreak, allowContinue );
				if ( c != null )
				{
					result.addCommand( c, this );

					continue;
				}

				// No type and no command -> done.
				break;
			}

			// If this is a new record definition, enter it
			if ( t.getType() == DataTypes.TYPE_RECORD && currentToken() != null && currentToken().equals(
				";" ) )
			{
                readToken(); // read ;
				continue;
			}

			Function f = parseFunction( t, result );
			if ( f != null )
			{
				if ( f.getName().equalsIgnoreCase( "main" ) )
				{
					if ( parentScope.getParentScope() != null )
					{
						throw parseException( "main method must appear at top level" );
					}
                    mainMethod = f;
				}

				continue;
			}

			if ( parseVariables( t, result ) )
			{
				if ( !currentToken().equals( ";" ) )
				{
					throw parseException( ";", currentToken() );
				}

                readToken(); //read ;
				continue;
			}

			//Found a type but no function or variable to tie it to
			throw parseException( "Type given but not used to declare anything" );
		}

		return result;
	}

	private Type parseRecord( final BasicScope parentScope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "record" ) )
		{
			return null;
		}

        readToken(); // read record

		if ( currentToken() == null )
		{
			throw parseException( "Record name expected" );
		}

		// Allow anonymous records
		String recordName = null;

		if ( !currentToken().equals( "{" ) )
		{
			// Named record
			recordName = currentToken();

			if ( !parseIdentifier( recordName ) )
			{
				throw parseException( "Invalid record name '" + recordName + "'" );
			}

			if ( Parser.isReservedWord( recordName ) )
			{
				throw parseException( "Reserved word '" + recordName + "' cannot be a record name" );
			}

			if ( parentScope.findType( recordName ) != null )
			{
				throw parseException( "Record name '" + recordName + "' is already defined" );
			}

            readToken(); // read name
		}

		if ( currentToken() == null || !currentToken().equals( "{" ) )
		{
			throw parseException( "{", currentToken() );
		}

        readToken(); // read {

		// Loop collecting fields
		ArrayList<Type> fieldTypes = new ArrayList<Type>();
		ArrayList<String> fieldNames = new ArrayList<String>();

		while ( true )
		{
			// Get the field type
			Type fieldType = parseType( parentScope, true, true );
			if ( fieldType == null )
			{
				throw parseException( "Type name expected" );
			}

			// Get the field name
			String fieldName = currentToken();
			if ( fieldName == null )
			{
				throw parseException( "Field name expected" );
			}

			if ( !parseIdentifier( fieldName ) )
			{
				throw parseException( "Invalid field name '" + fieldName + "'" );
			}

			if ( Parser.isReservedWord( fieldName ) )
			{
				throw parseException( "Reserved word '" + fieldName + "' cannot be used as a field name" );
			}

			if ( fieldNames.contains( fieldName ) )
			{
				throw parseException( "Field name '" + fieldName + "' is already defined" );
			}

            readToken(); // read name

			if ( currentToken() == null || !currentToken().equals( ";" ) )
			{
				throw parseException( ";", currentToken() );
			}

            readToken(); // read ;

			fieldTypes.add( fieldType );
			fieldNames.add( fieldName.toLowerCase() );

			if ( currentToken() == null )
			{
				throw parseException( "}", "EOF" );
			}

			if ( currentToken().equals( "}" ) )
			{
				break;
			}
		}

        readToken(); // read }

		String[] fieldNameArray = new String[ fieldNames.size() ];
		Type[] fieldTypeArray = new Type[ fieldTypes.size() ];
		fieldNames.toArray( fieldNameArray );
		fieldTypes.toArray( fieldTypeArray );

		RecordType rec =
			new RecordType(
				recordName != null ? recordName :
					( "(anonymous record " + Integer.toHexString( fieldNameArray.hashCode() ) + ")" ),
				fieldNameArray, fieldTypeArray );

		if ( recordName != null )
		{
			// Enter into type table
			parentScope.addType( rec );
		}

		return rec;
	}

	private Function parseFunction( final Type functionType, final Scope parentScope )
	{
		if ( !parseIdentifier( currentToken() ) )
		{
			return null;
		}

		if ( nextToken() == null || !nextToken().equals( "(" ) )
		{
			return null;
		}

		String functionName = currentToken();

		if ( Parser.isReservedWord( functionName ) )
		{
			throw parseException( "Reserved word '" + functionName + "' cannot be used as a function name" );
		}

        readToken(); //read Function name
        readToken(); //read (

		VariableList paramList = new VariableList();
		VariableReferenceList variableReferences = new VariableReferenceList();

		while ( !currentToken().equals( ")" ) )
		{
			Type paramType = parseType( parentScope, true, false );
			if ( paramType == null )
			{
				throw parseException( ")", currentToken() );
			}

			Variable param = parseVariable( paramType, null );
			if ( param == null )
			{
				throw parseException( "identifier", currentToken() );
			}

			if ( !paramList.add( param ) )
			{
				throw parseException( "Variable " + param.getName() + " is already defined" );
			}

			if ( !currentToken().equals( ")" ) )
			{
				if ( !currentToken().equals( "," ) )
				{
					throw parseException( ")", currentToken() );
				}

                readToken(); //read comma
			}

			variableReferences.add( new VariableReference( param ) );
		}

        readToken(); //read )

		// Add the function to the parent scope before we parse the
		// function scope to allow recursion.

		UserDefinedFunction f = new UserDefinedFunction( functionName, functionType, variableReferences );
		UserDefinedFunction existing = parentScope.findFunction( f );

		if ( existing != null && existing.getScope() != null )
		{
			throw multiplyDefinedFunctionException( functionName, variableReferences );
		}

		// Add new function or replace existing forward reference

		UserDefinedFunction result = parentScope.replaceFunction( existing, f );

		if ( currentToken() != null && currentToken().equals( ";" ) )
		{
			// Return forward reference
            readToken(); // ;
			return result;
		}

		Scope scope;
		if ( currentToken() != null && currentToken().equals( "{" ) )
		{
			// Scope is a block

            readToken(); // {

			scope = parseScope( null, functionType, paramList, parentScope, false, false );
			if ( currentToken() == null || !currentToken().equals( "}" ) )
			{
				throw parseException( "}", currentToken() );
			}

            readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new Scope( paramList, parentScope );
			ParseTreeNode cmd = parseCommand( functionType, parentScope, false, false, false );
			if ( cmd == null )
			{
				throw parseException( "Function with no body" );
			}
			scope.addCommand( cmd, this );
		}

		result.setScope( scope );
		if ( !result.assertBarrier() && !functionType.equals( DataTypes.TYPE_VOID ) )
		{
			if ( functionType.equals( DataTypes.TYPE_BOOLEAN ) )
			{
                warning( "Missing return values in boolean functions will soon become an error" );
			}
			else
			{
				throw parseException( "Missing return value" );
			}
		}

		return result;
	}

	private boolean parseVariables( final Type t, final BasicScope parentScope )
	{
		while ( true )
		{
			Variable v = parseVariable( t, parentScope );
			if ( v == null )
			{
				return false;
			}

			if ( currentToken().equals( "," ) )
			{
                readToken(); //read ,
				continue;
			}

			return true;
		}
	}

	private Variable parseVariable( final Type t, final BasicScope scope )
	{
		if ( !parseIdentifier( currentToken() ) )
		{
			return null;
		}

		String variableName = currentToken();
		if ( Parser.isReservedWord( variableName ) )
		{
			throw parseException( "Reserved word '" + variableName + "' cannot be a variable name" );
		}

		if ( scope != null && scope.findVariable( variableName ) != null )
		{
			throw parseException( "Variable " + variableName + " is already defined" );
		}

		Variable result = new Variable( variableName, t );

        readToken(); // If parsing of Identifier succeeded, go to next token.
		// If we are parsing a parameter declaration, we are done
		if ( scope == null )
		{
			if ( currentToken().equals( "=" ) )
			{
				throw parseException( "Cannot initialize parameter " + variableName );
			}
			return result;
		}

		// Otherwise, we must initialize the variable.

		Type ltype = t.getBaseType();
		Value rhs;

		if ( currentToken().equals( "=" ) )
		{
            readToken(); // Eat the equals sign
			rhs = parseExpression( scope );

			if ( rhs == null )
			{
				throw parseException( "Expression expected" );
			}

			if ( !Parser.validCoercion( ltype, rhs.getType(), "assign" ) )
			{
				throw parseException(
					"Cannot store " + rhs.getType() + " in " + variableName + " of type " + ltype );
			}
		}
		else
		{
			rhs = null;
		}

		scope.addVariable( result );
		VariableReference lhs = new VariableReference( variableName, scope );
		scope.addCommand( new Assignment( lhs, rhs ), this );
		return result;
	}

	private boolean parseTypedef( final Scope parentScope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "typedef" ) )
		{
			return false;
		}
        readToken(); // read typedef

		Type t = parseType( parentScope, true, true );
		if ( t == null )
		{
			throw parseException( "Missing data type for typedef" );
		}

		String typeName = currentToken();

		if ( !parseIdentifier( typeName ) )
		{
			throw parseException( "Invalid type name '" + typeName + "'" );
		}

		if ( Parser.isReservedWord( typeName ) )
		{
			throw parseException( "Reserved word '" + typeName + "' cannot be a type name" );
		}

		if ( parentScope.findType( typeName ) != null )
		{
			throw parseException( "Type name '" + typeName + "' is already defined" );
		}

        readToken(); // read name

		// Add the type to the type table
		TypeDef type = new TypeDef( typeName, t );
		parentScope.addType( type );

		return true;
	}

	private ParseTreeNode parseCommand( final Type functionType, final BasicScope scope, final boolean noElse, boolean allowBreak, boolean allowContinue )
	{
		ParseTreeNode result;

		if ( currentToken() == null )
		{
			return null;
		}

		if ( currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !allowBreak )
			{
				throw parseException( "Encountered 'break' outside of loop" );
			}

			result = new LoopBreak();
            readToken(); //break
		}

		else if ( currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !allowContinue )
			{
				throw parseException( "Encountered 'continue' outside of loop" );
			}

			result = new LoopContinue();
            readToken(); //continue
		}

		else if ( currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptExit();
            readToken(); //exit
		}

		else if ( ( result = parseReturn( functionType, scope ) ) != null )
		{
			;
		}
		else if ( ( result = parseBasicScript() ) != null )
		{
			// basic_script doesn't have a ; token
			return result;
		}
		else if ( ( result = parseWhile( functionType, scope ) ) != null )
		{
			// while doesn't have a ; token
			return result;
		}
		else if ( ( result = parseForeach( functionType, scope ) ) != null )
		{
			// foreach doesn't have a ; token
			return result;
		}
		else if ( ( result = parseFor( functionType, scope ) ) != null )
		{
			// for doesn't have a ; token
			return result;
		}
		else if ( ( result = parseRepeat( functionType, scope ) ) != null )
		{
			;
		}
		else if ( ( result = parseSwitch( functionType, scope, allowContinue ) ) != null )
		{
			// switch doesn't have a ; token
			return result;
		}
		else if ( ( result = parseConditional( functionType, scope, noElse, allowBreak, allowContinue ) ) != null )
		{
			// loop doesn't have a ; token
			return result;
		}
		else if ( ( result = parseTry( functionType, scope, allowBreak, allowContinue ) ) != null )
		{
			// try doesn't have a ; token
			return result;
		}
		else if ( ( result = parseStatic( functionType, scope ) ) != null )
		{
			// try doesn't have a ; token
			return result;
		}
		else if ( ( result = parseSort( scope ) ) != null )
		{
			;
		}
		else if ( ( result = parseAssignment( scope ) ) != null )
		{
			;
		}
		else if ( ( result = parseRemove( scope ) ) != null )
		{
			;
		}
		else if ( ( result = parseValue( scope ) ) != null )
		{
			;
		}
		else
		{
			return null;
		}

		if ( currentToken() == null || !currentToken().equals( ";" ) )
		{
			throw parseException( ";", currentToken() );
		}

        readToken(); // ;
		return result;
	}

	private Type parseType( final BasicScope scope, final boolean aggregates, final boolean records )
	{
		if ( currentToken() == null )
		{
			return null;
		}

		Type valType = scope.findType( currentToken() );
		if ( valType == null )
		{
			if ( records && currentToken().equalsIgnoreCase( "record" ) )
			{
				valType = parseRecord( scope );

				if ( valType == null )
				{
					return null;
				}

				if ( aggregates && currentToken().equals( "[" ) )
				{
					return parseAggregateType( valType, scope );
				}

				return valType;
			}

			return null;
		}

        readToken();

		if ( aggregates && currentToken().equals( "[" ) )
		{
			return parseAggregateType( valType, scope );
		}

		return valType;
	}

	private Type parseAggregateType( final Type dataType, final BasicScope scope )
	{
        readToken(); // [ or ,
		if ( currentToken() == null )
		{
			throw parseException( "Missing index token" );
		}

		if ( readIntegerToken( currentToken() ) )
		{
			int size = StringUtilities.parseInt( currentToken() );
            readToken(); // integer

			if ( currentToken() == null )
			{
				throw parseException( "]", currentToken() );
			}

			if ( currentToken().equals( "]" ) )
			{
                readToken(); // ]

				if ( currentToken().equals( "[" ) )
				{
					return new AggregateType( parseAggregateType( dataType, scope ), size );
				}

				return new AggregateType( dataType, size );
			}

			if ( currentToken().equals( "," ) )
			{
				return new AggregateType( parseAggregateType( dataType, scope ), size );
			}

			throw parseException( "]", currentToken() );
		}

		Type indexType = scope.findType( currentToken() );
		if ( indexType == null )
		{
			throw parseException( "Invalid type name '" + currentToken() + "'" );
		}

		if ( !indexType.isPrimitive() )
		{
			throw parseException( "Index type '" + currentToken() + "' is not a primitive type" );
		}

        readToken(); // type name
		if ( currentToken() == null )
		{
			throw parseException( "]", currentToken() );
		}

		if ( currentToken().equals( "]" ) )
		{
            readToken(); // ]

			if ( currentToken().equals( "[" ) )
			{
				return new AggregateType( parseAggregateType( dataType, scope ), indexType );
			}

			return new AggregateType( dataType, indexType );
		}

		if ( currentToken().equals( "," ) )
		{
			return new AggregateType( parseAggregateType( dataType, scope ), indexType );
		}

		throw parseException( ", or ]", currentToken() );
	}

	private boolean parseIdentifier( final String identifier )
	{
		if ( !Character.isLetter( identifier.charAt( 0 ) ) && identifier.charAt( 0 ) != '_' )
		{
			return false;
		}

		for ( int i = 1; i < identifier.length(); ++i )
		{
			if ( !Character.isLetterOrDigit( identifier.charAt( i ) ) && identifier.charAt( i ) != '_' )
			{
				return false;
			}
		}

		return true;
	}

	private FunctionReturn parseReturn( final Type expectedType, final BasicScope parentScope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "return" ) )
		{
			return null;
		}

        readToken(); //return

		if ( currentToken() != null && currentToken().equals( ";" ) )
		{
			if ( expectedType != null && expectedType.equals( DataTypes.TYPE_VOID ) )
			{
				return new FunctionReturn( null, DataTypes.VOID_TYPE );
			}

			throw parseException( "Return needs " + expectedType + " value" );
		}
		else
		{
			if ( expectedType != null && expectedType.equals( DataTypes.TYPE_VOID ) )
			{
				throw parseException( "Cannot return a value from a void function" );
			}

			Value value = parseExpression( parentScope );

			if ( value == null )
			{
				throw parseException( "Expression expected" );
			}

			if ( expectedType != null &&
				!Parser.validCoercion( expectedType, value.getType(), "return" ) )
			{
				throw parseException( "Cannot return " + value.getType() + " value from " + expectedType + " function");
			}

			return new FunctionReturn( value, expectedType );
		}
	}

	private Scope parseSingleCommandScope( final Type functionType, final BasicScope parentScope, final boolean noElse, boolean allowBreak, boolean allowContinue )
	{
		ParseTreeNode command = parseCommand( functionType, parentScope, noElse, allowBreak, allowContinue );
		if ( command == null )
		{
			if ( currentToken() == null || !currentToken().equals( ";" ) )
			{
				throw parseException( ";", currentToken() );
			}

            readToken(); // ;
			return new Scope( parentScope );
		}
		return new Scope( command, parentScope );
	}

	private Conditional parseConditional( final Type functionType,
					      final BasicScope parentScope,
					      boolean noElse,
					      final boolean allowBreak,
					      final boolean allowContinue )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "if" ) )
		{
			return null;
		}

		if ( nextToken() == null || !nextToken().equals( "(" ) )
		{
			throw parseException( "(", nextToken() );
		}

        readToken(); // if
        readToken(); // (

		Value condition = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
		{
			throw parseException( ")", currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw parseException( "\"if\" requires a boolean conditional expression" );
		}

        readToken(); // )

		If result = null;
		boolean elseFound = false;
		boolean finalElse = false;

		do
		{
			Scope scope;

			if ( currentToken() == null || !currentToken().equals( "{" ) ) //Scope is a single call
			{
				scope = parseSingleCommandScope( functionType, parentScope, !elseFound, allowBreak, allowContinue );
			}
			else
			{
                readToken(); //read {
				scope = parseScope( null, functionType, null, parentScope, allowBreak, allowContinue );

				if ( currentToken() == null || !currentToken().equals( "}" ) )
				{
					throw parseException( "}", currentToken() );
				}

                readToken(); //read }
			}

			if ( result == null )
			{
				result = new If( scope, condition );
			}
			else if ( finalElse )
			{
				result.addElseLoop( new Else( scope, condition ) );
			}
			else
			{
				result.addElseLoop( new ElseIf( scope, condition ) );
			}

			if ( !noElse && currentToken() != null && currentToken().equalsIgnoreCase( "else" ) )
			{
				if ( finalElse )
				{
					throw parseException( "Else without if" );
				}

				if ( nextToken() != null && nextToken().equalsIgnoreCase( "if" ) )
				{
                    readToken(); //else
                    readToken(); //if

					if ( currentToken() == null || !currentToken().equals( "(" ) )
					{
						throw parseException( "(", currentToken() );
					}

                    readToken(); //(
					condition = parseExpression( parentScope );

					if ( currentToken() == null || !currentToken().equals( ")" ) )
					{
						throw parseException( ")", currentToken() );
					}

					if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
					{
						throw parseException( "\"if\" requires a boolean conditional expression" );
					}

                    readToken(); // )
				}
				else
				//else without condition
				{
                    readToken(); //else
					condition = DataTypes.TRUE_VALUE;
					finalElse = true;
				}

				elseFound = true;
				continue;
			}

			elseFound = false;
		}
		while ( elseFound );

		return result;
	}

	private BasicScript parseBasicScript()
	{
		if ( currentToken() == null )
		{
			return null;
		}

		if ( !currentToken().equalsIgnoreCase( "cli_execute" ) )
		{
			return null;
		}

		if ( nextToken() == null || !nextToken().equals( "{" ) )
		{
			return null;
		}

        readToken(); // while
        readToken(); // {

		ByteArrayStream ostream = new ByteArrayStream();

		while ( currentToken() != null && !currentToken().equals( "}" ) )
		{
			try
			{
				ostream.write( currentLine.getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}
			catch ( Exception e )
			{
				// Byte array output streams do not throw errors,
				// other than out of memory errors.

				StaticEntity.printStackTrace( e );
			}

            currentLine = "";
            fixLines();
		}

		if ( currentToken() == null )
		{
			throw parseException( "}", currentToken() );
		}

        readToken(); // }

		return new BasicScript( ostream );
	}

	private WhileLoop parseWhile( final Type functionType, final BasicScope parentScope )
	{
		if ( currentToken() == null )
		{
			return null;
		}

		if ( !currentToken().equalsIgnoreCase( "while" ) )
		{
			return null;
		}

		if ( nextToken() == null || !nextToken().equals( "(" ) )
		{
			throw parseException( "(", nextToken() );
		}

        readToken(); // while
        readToken(); // (

		Value condition = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
		{
			throw parseException( ")", currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw parseException( "\"while\" requires a boolean conditional expression" );
		}

        readToken(); // )

		Scope scope = parseLoopScope( functionType, null, parentScope );

		return new WhileLoop( scope, condition );
	}

	private RepeatUntilLoop parseRepeat( final Type functionType, final BasicScope parentScope )
	{
		if ( currentToken() == null )
		{
			return null;
		}

		if ( !currentToken().equalsIgnoreCase( "repeat" ) )
		{
			return null;
		}

        readToken(); // repeat

		Scope scope = parseLoopScope( functionType, null, parentScope );
		if ( currentToken() == null || !currentToken().equals( "until" ) )
		{
			throw parseException( "until", currentToken() );
		}

		if ( nextToken() == null || !nextToken().equals( "(" ) )
		{
			throw parseException( "(", nextToken() );
		}

        readToken(); // until
        readToken(); // (

		Value condition = parseExpression( parentScope );
		if ( currentToken() == null || !currentToken().equals( ")" ) )
		{
			throw parseException( ")", currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw parseException( "\"repeat\" requires a boolean conditional expression" );
		}

        readToken(); // )

		return new RepeatUntilLoop( scope, condition );
	}

	private Switch parseSwitch( final Type functionType, final BasicScope parentScope, final boolean allowContinue )
	{
		if ( currentToken() == null ||
		     !currentToken().equalsIgnoreCase( "switch" ) )
		{
			return null;
		}

		if ( nextToken() == null ||
		     ( !nextToken().equals( "(" ) && !nextToken().equals( "{" ) ) )
		{
			throw parseException( "( or {", nextToken() );
		}

        readToken(); // switch

		Value condition = DataTypes.TRUE_VALUE;
		if ( currentToken().equals( "(" ) )
		{
            readToken(); // (

			condition = parseExpression( parentScope );
			if ( currentToken() == null || !currentToken().equals( ")" ) )
			{
				throw parseException( ")", currentToken() );
			}

            readToken(); // )
		}

		Type type = condition.getType();

		if ( currentToken() == null || !currentToken().equals( "{" ) )
		{
			throw parseException( "{", currentToken() );
		}

        readToken(); // {

		ArrayList tests = new ArrayList();
		ArrayList indices = new ArrayList();
		int defaultIndex = -1;

		SwitchScope scope = new SwitchScope( parentScope );
		int currentIndex = 0;
		Integer currentInteger = null;

		TreeMap labels = new TreeMap();
		boolean constantLabels = true;

		while ( true )
		{
			if ( currentToken().equals( "case" ) )
			{
                readToken(); // case

				Value test = parseExpression( parentScope );

				if ( currentToken() == null || !currentToken().equals( ":" ) )
				{
					throw parseException( ":", currentToken() );
				}

				if ( !test.getType().equals( type ) )
				{
					throw parseException( "Switch conditional has type " + type + " but label expression has type " + test.getType() );
				}

                readToken(); // :

				if ( currentInteger == null )
				{
					currentInteger = IntegerPool.get( currentIndex );
				}

				if ( test.getClass() == Value.class )
				{
					if ( labels.get( test ) != null )
					{
						throw parseException( "Duplicate case label: " + test );
					}
					labels.put( test, currentInteger );
				}
				else
				{
					constantLabels = false;
				}


				tests.add( test );
				indices.add( currentInteger );
				scope.resetBarrier();

				continue;
			}

			if ( currentToken().equals( "default" ) )
			{
                readToken(); // default

				if ( currentToken() == null || !currentToken().equals( ":" ) )
				{
					throw parseException( ":", currentToken() );
				}

				if ( defaultIndex != -1 )
				{
					throw parseException( "Only one default label allowed in a switch statement" );
				}

                readToken(); // :

				defaultIndex = currentIndex;
				scope.resetBarrier();

				continue;
			}

			Type t = parseType( scope, true, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ParseTreeNode c = parseCommand( functionType, scope, false, true, allowContinue );
				if ( c != null )
				{
					scope.addCommand( c, this );
					currentIndex = scope.commandCount();
					currentInteger = null;
					continue;
				}

				// No type and no command -> done.
				break;
			}

			if ( parseVariables( t, scope ) )
			{
				if ( !currentToken().equals( ";" ) )
				{
					throw parseException( ";", currentToken() );
				}

                readToken(); //read ;
				currentIndex = scope.commandCount();
				currentInteger = null;
				continue;
			}

			//Found a type but no function or variable to tie it to
			throw parseException( "Type given but not used to declare anything" );
		}

		if ( currentToken() == null || !currentToken().equals( "}" ) )
		{
			throw parseException( "}", currentToken() );
		}

        readToken(); // }

		return new Switch( condition, tests, indices, defaultIndex, scope,
				   constantLabels ? labels : null );
	}

	private Try parseTry( final Type functionType,
		final BasicScope parentScope,
		final boolean allowBreak, final boolean allowContinue )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "try" ) )
		{
			return null;
		}

        readToken(); // try

		Scope body;
		if ( currentToken() == null || !currentToken().equals( "{" ) ) // body is a single call
		{
			body = parseSingleCommandScope( functionType, parentScope, false, allowBreak, allowContinue );
		}
		else
		{
            readToken(); //read {
			body = parseScope( null, functionType, null, parentScope, allowBreak, allowContinue );

			if ( currentToken() == null || !currentToken().equals( "}" ) )
			{
				throw parseException( "}", currentToken() );
			}

            readToken(); //read }
		}
		
		// catch clauses would be parsed here
		
		if ( currentToken() == null || !currentToken().equals( "finally" ) )
		{	// this would not be an error if at least one catch was present
			throw parseException( "\"try\" without \"finally\" is pointless" );
		}
        readToken(); //read finally

		Scope finalClause;
		if ( currentToken() == null || !currentToken().equals( "{" ) ) // finally is a single call
		{
			finalClause = parseSingleCommandScope( functionType, body, false, allowBreak, allowContinue );
		}
		else
		{
            readToken(); //read {
			finalClause = parseScope( null, functionType, null, body, allowBreak, allowContinue );

			if ( currentToken() == null || !currentToken().equals( "}" ) )
			{
				throw parseException( "}", currentToken() );
			}

            readToken(); //read }
		}

		return new Try( body, finalClause );
	}

	private Scope parseStatic( final Type functionType, final BasicScope parentScope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "static" ) )
		{
			return null;
		}

        readToken(); // final

		Scope result = new StaticScope( parentScope );

		if ( currentToken() == null || !currentToken().equals( "{" ) ) // body is a single call
		{
			return parseCommandOrDeclaration( result, functionType );
		}

        readToken(); //read {

        parseScope( result, functionType, null, parentScope, false, false );

		if ( currentToken() == null || !currentToken().equals( "}" ) )
		{
			throw parseException( "}", currentToken() );
		}

        readToken(); //read }

		return result;
	}

	private SortBy parseSort( final BasicScope parentScope )
	{
		// sort aggregate by expr

		if ( currentToken() == null )
		{
			return null;
		}

		if ( !currentToken().equalsIgnoreCase( "sort" ) )
		{
			return null;
		}

		if ( nextToken() == null || nextToken().equals( "(" )
			|| nextToken().equals( "=" ) )
		{	// it's a call to a function named sort(), or an assigment to
			// a variable named sort, not the sort statement.
			return null;
		}

        readToken(); // sort

		// Get an aggregate reference
		Value aggregate = parseVariableReference( parentScope );

		if ( aggregate == null || !( aggregate instanceof VariableReference ) || !( aggregate.getType().getBaseType() instanceof AggregateType ) )
		{
			throw parseException( "Aggregate reference expected" );
		}

		if ( currentToken() == null ||
			!currentToken().equalsIgnoreCase( "by" ) )
		{
			throw parseException( "by", currentToken() );
		}
        readToken();	// by

		// Define key variables of appropriate type
		VariableList varList = new VariableList();
		AggregateType type = (AggregateType) aggregate.getType().getBaseType();
		Variable valuevar = new Variable( "value", type.getDataType() );
		varList.add( valuevar );
		Variable indexvar = new Variable( "index", type.getIndexType() );
		varList.add( indexvar );

		// Parse the key expression in a new scope containing 'index' and 'value'
		Scope scope = new Scope( varList, parentScope );
		Value expr = parseExpression( scope );

		return new SortBy( (VariableReference) aggregate, indexvar, valuevar, expr );
	}

	private ForEachLoop parseForeach( final Type functionType, final BasicScope parentScope )
	{
		// foreach key [, key ... ] in aggregate { scope }

		if ( currentToken() == null )
		{
			return null;
		}

		if ( !currentToken().equalsIgnoreCase( "foreach" ) )
		{
			return null;
		}

        readToken(); // foreach

		ArrayList<String> names = new ArrayList<String>();

		while ( true )
		{
			String name = currentToken();

			if ( !parseIdentifier( name ) )
			{
				throw parseException( "Key variable name expected" );
			}

			if ( names.contains( name ) )
			{
				throw parseException( "Key variable '" + name + "' is already defined" );
			}

			names.add( name );
            readToken(); // name

			if ( currentToken() != null )
			{
				if ( currentToken().equals( "," ) )
				{
                    readToken(); // ,
					continue;
				}

				if ( currentToken().equalsIgnoreCase( "in" ) )
				{
                    readToken(); // in
					break;
				}
			}

			throw parseException( "in", currentToken() );
		}

		// Get an aggregate reference
		Value aggregate = parseValue( parentScope );

		if ( aggregate == null || !( aggregate.getType().getBaseType() instanceof AggregateType ) )
		{
			throw parseException( "Aggregate reference expected" );
		}

		// Define key variables of appropriate type
		VariableList varList = new VariableList();
		VariableReferenceList variableReferences = new VariableReferenceList();
		Type type = aggregate.getType().getBaseType();

		for ( String name : names )
		{
			Type itype;
			if ( type == null )
			{
				throw parseException( "Too many key variables specified" );
			}
			else if ( !( type instanceof AggregateType ) )
			{	// Variable after all key vars holds the value instead
				itype = type;
				type = null;
			}
			else
			{
				itype = ( (AggregateType) type ).getIndexType();
				type = ( (AggregateType) type ).getDataType();
			}

			Variable keyvar = new Variable( name, itype );
			varList.add( keyvar );
			variableReferences.add( new VariableReference( keyvar ) );
		}

		// Parse the scope with the list of keyVars
		Scope scope = parseLoopScope( functionType, varList, parentScope );

		// Add the foreach node with the list of varRefs
		return new ForEachLoop( scope, variableReferences, aggregate, this );
	}

	private ForLoop parseFor( final Type functionType, final BasicScope parentScope )
	{
		// foreach key in aggregate {scope }

		if ( currentToken() == null )
		{
			return null;
		}

		if ( !currentToken().equalsIgnoreCase( "for" ) )
		{
			return null;
		}

		String name = nextToken();

		if ( !parseIdentifier( name ) )
		{
			return null;
		}

		if ( parentScope.findVariable( name ) != null )
		{
			throw parseException( "Index variable '" + name + "' is already defined" );
		}

        readToken(); // for
        readToken(); // name

		if ( !currentToken().equalsIgnoreCase( "from" ) )
		{
			throw parseException( "from", currentToken() );
		}

        readToken(); // from

		Value initial = parseExpression( parentScope );

		int direction = 0;

		if ( currentToken().equalsIgnoreCase( "upto" ) )
		{
			direction = 1;
		}
		else if ( currentToken().equalsIgnoreCase( "downto" ) )
		{
			direction = -1;
		}
		else if ( currentToken().equalsIgnoreCase( "to" ) )
		{
			direction = 0;
		}
		else
		{
			throw parseException( "to, upto, or downto", currentToken() );
		}

        readToken(); // upto/downto

		Value last = parseExpression( parentScope );

		Value increment = DataTypes.ONE_VALUE;
		if ( currentToken().equalsIgnoreCase( "by" ) )
		{
            readToken(); // by
			increment = parseExpression( parentScope );
		}

		// Create integer index variable
		Variable indexvar = new Variable( name, DataTypes.INT_TYPE );

		// Put index variable onto a list
		VariableList varList = new VariableList();
		varList.add( indexvar );

		Scope scope = parseLoopScope( functionType, varList, parentScope );

		return new ForLoop( scope, new VariableReference( indexvar ), initial, last, increment, direction, this );
	}

	private Scope parseLoopScope( final Type functionType, final VariableList varList, final BasicScope parentScope )
	{
		Scope scope;

		if ( currentToken() != null && currentToken().equals( "{" ) )
		{
			// Scope is a block

            readToken(); // {

			scope = parseScope( null, functionType, varList, parentScope, true, true );
			if ( currentToken() == null || !currentToken().equals( "}" ) )
			{
				throw parseException( "}", currentToken() );
			}

            readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new Scope( varList, parentScope );
			ParseTreeNode command = parseCommand( functionType, scope, false, true, true );
			if ( command == null )
			{
				if ( currentToken() == null || !currentToken().equals( ";" ) )
				{
					throw parseException( ";", currentToken() );
				}

                readToken(); // ;
			}
			else
			{
				scope.addCommand( command, this );
			}
		}

		return scope;
	}

	private Value parseNewRecord( final BasicScope scope )
	{
		if ( !parseIdentifier( currentToken() ) )
		{
			return null;
		}

		String name = currentToken();
		Type type = scope.findType( name );

		if ( type == null || !( type instanceof RecordType ) )
		{
			throw parseException( "'" + name + "' is not a record type" );
		}

		RecordType target = (RecordType) type;

        readToken(); //name

		ValueList params = new ValueList();
		String [] names = target.getFieldNames();
		Type [] types = target.getFieldTypes();
		int param = 0;

		if ( currentToken() != null && currentToken().equals( "(" ) )
		{
            readToken(); //(

			while ( currentToken() != null && !currentToken().equals( ")" ) )
			{
				Value val;

				if ( currentToken().equals( "," ) )
				{
					val = DataTypes.VOID_VALUE;
				}
				else
				{
					val = parseExpression( scope );
				}

				if ( val == null )
				{
					throw parseException( "Expression expected for field #" + ( param + 1 ) + " (" + names[param] + ")" );
				}

				if ( val != DataTypes.VOID_VALUE )
				{
					Type expected = types[param];
					Type given = val.getType();
					if ( !expected.equals( given ) )
					{
						throw parseException( given + " found when " + expected + " expected for field #" + ( param + 1 ) + " (" + names[param] + ")" );
					}
				}

				params.add( val );
				param++;

				if ( currentToken().equals( "," ) )
				{
					if ( param == names.length )
					{
						throw parseException( "Too many field initializers for record " + name );
					}

                    readToken(); // ,
				}
			}

			if ( currentToken() == null )
			{
				throw parseException( ")", currentToken() );
			}

            readToken(); // )
		}

		return target.initialValueExpression( params );
	}

	private Value parseCall( final BasicScope scope )
	{
		return parseCall( scope, null );
	}

	private Value parseCall( final BasicScope scope, final Value firstParam )
	{
		if ( nextToken() == null || !nextToken().equals( "(" ) )
		{
			return null;
		}

		if ( !parseIdentifier( currentToken() ) )
		{
			return null;
		}

		String name = currentToken();
        readToken(); //name

		ValueList params = parseParameters( scope, firstParam );
		Function target = findFunction( scope, name, params );

		if ( target == null )
		{
			throw undefinedFunctionException( name, params );
		}

		FunctionCall call = new FunctionCall( target, params, this );

		return parsePostCall( scope, call );
	}

	private ValueList parseParameters( final BasicScope scope, final Value firstParam )
	{
		if ( !currentToken().equals( "(" ) )
		{
			return null;
		}

        readToken(); //(

		ValueList params = new ValueList();
		if ( firstParam != null )
		{
			params.add( firstParam );
		}

		while ( currentToken() != null && !currentToken().equals( ")" ) )
		{
			Value val = parseExpression( scope );
			if ( val != null )
			{
				params.add( val );
			}

			if ( currentToken() == null )
			{
				throw parseException( ")", "end of file" );
			}

			if ( !currentToken().equals( "," ) )
			{
				if ( !currentToken().equals( ")" ) )
				{
					throw parseException( ")", currentToken() );
				}
				continue;
			}

            readToken(); // ,

			if ( currentToken() == null )
			{
				throw parseException( ")", "end of file" );
			}

			if ( currentToken().equals( ")" ) )
			{
				throw parseException( "parameter", currentToken() );
			}
		}

		if ( currentToken() == null )
		{
			throw parseException( ")", "end of file" );
		}

		if ( !currentToken().equals( ")" ) )
		{
			throw parseException( ")", currentToken() );
		}

        readToken(); // )

		return params;
	}

	private Value parsePostCall( final BasicScope scope, FunctionCall call )
	{
		Value result = call;
		while ( result != null && currentToken() != null && currentToken().equals( "." ) )
		{
			Variable current = new Variable( result.getType() );
			current.setExpression( result );

			result = parseVariableReference( scope, current );
		}

		return result;
	}

	private Value parseInvoke( final BasicScope scope )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "call" ) )
		{
			return null;
		}

        readToken(); // call

		Type type = parseType( scope, true, false );

		// You can omit the type, but then this function invocation
		// cannot be used in an expression

		if ( type == null )
		{
			type = DataTypes.VOID_TYPE;
		}

		String current = currentToken();
		Value name = null;

		if ( current.equals( "(" ) )
		{
			name = parseExpression( scope );
			if ( name == null || !name.getType().equals( DataTypes.STRING_TYPE ) )
			{
				throw parseException( "String expression expected for function name" );
			}
		}
		else
		{
			if ( !parseIdentifier( current ) )
			{
				throw parseException( "Variable reference expected for function name" );
			}

			name = parseVariableReference( scope );

			if ( name == null || !( name instanceof VariableReference ) )
			{
				throw parseException( "Variable reference expected for function name" );
			}
		}

		ValueList params = parseParameters( scope, null );

		FunctionInvocation call = new FunctionInvocation( scope, type, name, params, this );

		return parsePostCall( scope, call );
	}

        private static final String [] COMPAT_FUNCTIONS = {
                "to_string",
                "to_boolean",
                "to_int",
                "to_float",
                "to_item",
                "to_class",
                "to_stat",
                "to_skill",
                "to_effect",
                "to_location",
                "to_familiar",
                "to_monster",
                "to_slot",
                "to_element",
                "to_coinmaster",
                "to_url",
        };

	private Function findFunction( final BasicScope scope, final String name, final ValueList params )
	{
		Function result = findFunction( scope, scope.getFunctionList(), name, params, true );
		if ( result != null )
		{
			return result;
		}

		result = findFunction( scope, RuntimeLibrary.functions, name, params, true );
		if ( result != null )
		{
			return result;
		}

		result = findFunction( scope, scope.getFunctionList(), name, params, false );
		if ( result != null )
		{
			return result;
		}

		result = findFunction( scope, RuntimeLibrary.functions, name, params, false );
		if ( result != null )
		{
			return result;
		}

		// Just in case some people didn't edit their scripts to use
		// the new function format, check for the old versions as well.

		for ( int i = 0; i < COMPAT_FUNCTIONS.length; ++i )
		{
			String fname = COMPAT_FUNCTIONS[i];
			if ( !name.equals( fname ) && name.endsWith( fname ) )
			{
				return findFunction( scope, fname, params );
			}
		}

		return null;
	}

	private Function findFunction( BasicScope scope, final FunctionList source,
					     final String name, final ValueList params,
					     boolean isExactMatch )
	{
		if ( params == null )
		{
			return null;
		}

		Function[] functions = source.findFunctions( name );

		// First, try to find an exact match on parameter types.
		// This allows strict matches to take precedence.

		for ( int i = 0; i < functions.length; ++i )
		{
			Iterator refIterator = functions[ i ].getReferences();
			Iterator valIterator = params.iterator();
			boolean matched = true;

			while ( refIterator.hasNext() && valIterator.hasNext() )
			{
				VariableReference currentParam = (VariableReference) refIterator.next();
				Type paramType = currentParam.getType();
				Value currentValue = (Value) valIterator.next();
				Type valueType = currentValue.getType();

				if ( isExactMatch )
				{
					if ( paramType != valueType )
					{
						matched = false;
						break;
					}
				}
				else if ( !Parser.validCoercion( paramType, valueType, "parameter" ) )
				{
					matched = false;
					break;
				}
			}

			if ( refIterator.hasNext() || valIterator.hasNext() )
			{
				matched = false;
			}

			if ( matched )
			{
				return functions[ i ];
			}
		}

		if ( isExactMatch || source == RuntimeLibrary.functions )
		{
			return null;
		}

		if ( scope.getParentScope() != null )
		{
			return findFunction( scope.getParentScope(), name, params );
		}

		return null;
	}

	private ParseTreeNode parseAssignment( final BasicScope scope )
	{
		if ( nextToken() == null )
		{
			return null;
		}

		if ( !nextToken().equals( "=" ) &&
		     !nextToken().equals( "[" ) &&
		     !nextToken().equals( "." ) &&
		     !nextToken().equals( "+=" ) &&
		     !nextToken().equals( "-=" ) &&
		     !nextToken().equals( "*=" ) &&
		     !nextToken().equals( "/=" ) &&
		     !nextToken().equals( "%=" ) &&
		     !nextToken().equals( "**=" ) &&
		     !nextToken().equals( "&=" ) &&
		     !nextToken().equals( "^=" ) &&
		     !nextToken().equals( "|=" ) &&
		     !nextToken().equals( "<<=" ) &&
		     !nextToken().equals( ">>=" ) &&
		     !nextToken().equals( ">>>=" ) )
		{
			return null;
		}

		if ( !parseIdentifier( currentToken() ) )
		{
			return null;
		}

		Value lhs = parseVariableReference( scope );
		if ( lhs instanceof FunctionCall )
		{
			return lhs;
		}

		if ( lhs == null || !( lhs instanceof VariableReference ) )
		{
			throw parseException( "Variable reference expected" );
		}

		String operStr = currentToken();
		if ( !operStr.equals( "=" ) &&
		     !operStr.equals( "+=" ) &&
		     !operStr.equals( "-=" ) &&
		     !operStr.equals( "*=" ) &&
		     !operStr.equals( "/=" ) &&
		     !operStr.equals( "%=" ) &&
		     !operStr.equals( "**=" ) &&
		     !operStr.equals( "&=" ) &&
		     !operStr.equals( "^=" ) &&
		     !operStr.equals( "|=" ) &&
		     !operStr.equals( "<<=" ) &&
		     !operStr.equals( ">>=" ) &&
		     !operStr.equals( ">>>=" ) )
		{
			return null;
		}

		Operator oper = new Operator( operStr, this );

        readToken(); // oper

		Value rhs = parseExpression( scope );

		if ( rhs == null )
		{
			throw parseException( "Internal error" );
		}

		if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), oper ) )
		{
			String error =
				oper.isLogical() ?
				( oper + " requires an integer or boolean expression and an integer or boolean variable reference" ) :
				oper.isInteger() ?
				( oper + " requires an integer expression and an integer variable reference" ) :
				( "Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
			throw parseException( error );
		}

		Operator op = operStr.equals( "=" ) ? null : new Operator( operStr.substring( 0, operStr.length() - 1 ), this );

		return new Assignment( (VariableReference) lhs, rhs, op );
	}

	private Value parseRemove( final BasicScope scope )
	{
		if ( currentToken() == null || !currentToken().equals( "remove" ) )
		{
			return null;
		}

		Value lhs = parseExpression( scope );

		if ( lhs == null )
		{
			throw parseException( "Bad 'remove' statement" );
		}

		return lhs;
	}

	private Value parseExpression( final BasicScope scope )
	{
		return parseExpression( scope, null );
	}

	private Value parseExpression( final BasicScope scope, final Operator previousOper )
	{
		if ( currentToken() == null )
		{
			return null;
		}

		Value lhs = null;
		Value rhs = null;
		Operator oper = null;

		if ( currentToken().equals( "!" ) )
		{
			String operator = currentToken();
            readToken(); // !
			if ( ( lhs = parseValue( scope ) ) == null )
			{
				throw parseException( "Value expected" );
			}

			lhs = new Expression( lhs, null, new Operator( operator, this ) );
			if ( lhs.getType() != DataTypes.BOOLEAN_TYPE )
			{
				throw parseException( "\"!\" operator requires a boolean value" );
			}
		}
		else if ( currentToken().equals( "~" ) )
		{
			String operator = currentToken();
            readToken(); // ~
			if ( ( lhs = parseValue( scope ) ) == null )
			{
				throw parseException( "Value expected" );
			}

			lhs = new Expression( lhs, null, new Operator( operator, this ) );
			if ( lhs.getType() != DataTypes.INT_TYPE && lhs.getType() != DataTypes.BOOLEAN_TYPE )
			{
				throw parseException( "\"~\" operator requires an integer or boolean value" );
			}
		}
		else if ( currentToken().equals( "-" ) )
		{
			// See if it's a negative numeric constant
			if ( ( lhs = parseValue( scope ) ) == null )
			{
				// Nope. Unary minus.
				String operator = currentToken();
                readToken(); // -
				if ( ( lhs = parseValue( scope ) ) == null )
				{
					throw parseException( "Value expected" );
				}

				lhs = new Expression( lhs, null, new Operator( operator, this ) );
			}
		}
		else if ( currentToken().equals( "remove" ) )
		{
			String operator = currentToken();
            readToken(); // remove

			lhs = parseVariableReference( scope );
			if ( lhs == null || !( lhs instanceof CompositeReference ) )
			{
				throw parseException( "Aggregate reference expected" );
			}

			lhs = new Expression( lhs, null, new Operator( operator, this ) );
		}
		else if ( ( lhs = parseValue( scope ) ) == null )
		{
			return null;
		}

		do
		{
			oper = parseOperator( currentToken() );

			if ( oper == null )
			{
				return lhs;
			}

			if ( previousOper != null && !oper.precedes( previousOper ) )
			{
				return lhs;
			}

			if ( currentToken().equals( ":" ) )
			{
				return lhs;
			}

			if ( currentToken().equals( "?" ) )
			{
                readToken(); // ?

				Value conditional = lhs;

				if ( conditional.getType() != DataTypes.BOOLEAN_TYPE )
				{
					throw parseException(
						"Non-boolean expression " + conditional + " (" + conditional.getType() + ")" );
				}

				if ( ( lhs = parseExpression( scope, null ) ) == null )
				{
					throw parseException( "Value expected in left hand side" );
				}

				if ( !currentToken().equals( ":" ) )
				{
					throw parseException( "\":\" expected" );
				}

                readToken(); // :

				if ( ( rhs = parseExpression( scope, null ) ) == null )
				{
					throw parseException( "Value expected" );
				}

				if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), oper ) )
				{
					throw parseException(
						"Cannot choose between " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
				}

				lhs = new Expression( conditional, lhs, rhs );
			}
			else
			{
                readToken(); //operator

				if ( ( rhs = parseExpression( scope, oper ) ) == null )
				{
					throw parseException( "Value expected" );
				}

				if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), oper ) )
				{
					throw parseException(
						"Cannot apply operator " + oper + " to " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
				}

				lhs = new Expression( lhs, rhs, oper );
			}
		}
		while ( true );
	}

	private Value parseValue( final BasicScope scope )
	{
		if ( currentToken() == null )
		{
			return null;
		}

		Value result = null;

		// Parse parenthesized expressions
		if ( currentToken().equals( "(" ) )
		{
            readToken(); // (

			result = parseExpression( scope );
			if ( currentToken() == null || !currentToken().equals( ")" ) )
			{
				throw parseException( ")", currentToken() );
			}

            readToken(); // )
		}

		// Parse constant values
		// true and false are reserved words

		else if ( currentToken().equalsIgnoreCase( "true" ) )
		{
            readToken();
			result = DataTypes.TRUE_VALUE;
		}

		else if ( currentToken().equalsIgnoreCase( "false" ) )
		{
            readToken();
			result = DataTypes.FALSE_VALUE;
		}

		else if ( currentToken().equals( "__FILE__" ) )
		{
            readToken();
			result = new Value( String.valueOf( shortFileName ) );
		}

		// numbers
		else if ( ( result = parseNumber() ) != null )
		{
			;
		}

		else if ( currentToken().equals( "\"" ) || currentToken().equals( "\'" ) )
		{
			result = parseString( null );
		}

		else if ( currentToken().equals( "$" ) )
		{
			result = parseTypedConstant( scope );
		}

		else if ( currentToken().equalsIgnoreCase( "new" ) )
		{
            readToken();
			result = parseNewRecord( scope );
		}

		else if ( ( result = parseInvoke( scope ) ) != null )
		{
			;
		}

		else if ( ( result = parseCall( scope, null ) ) != null )
		{
			;
		}

		else if ( ( result = parseVariableReference( scope ) ) != null )
		{
			;
		}

		Variable current;

		while ( result != null && currentToken() != null && (currentToken().equals( "." ) || currentToken().equals( "[" ) ) )
		{
			current = new Variable( result.getType() );
			current.setExpression( result );

			result = parseVariableReference( scope, current );
		}

		return result;
	}

	private Value parseNumber()
	{
		if ( currentToken() == null )
		{
			return null;
		}

		int sign = 1;

		if ( currentToken().equals( "-" ) )
		{
			String next = nextToken();

			if ( next == null )
			{
				return null;
			}

			if ( !next.equals( "." ) && !readIntegerToken( next ) )
			{
				// Unary minus
				return null;
			}

			sign = -1;
            readToken(); // Read -
		}

		if ( currentToken().equals( "." ) )
		{
            readToken();
			String fraction = currentToken();

			if ( !readIntegerToken( fraction ) )
			{
				throw parseException( "numeric value", fraction );
			}

            readToken(); // integer
			return new Value( sign * StringUtilities.parseDouble( "0." + fraction ) );
		}

		String integer = currentToken();
		if ( !readIntegerToken( integer ) )
		{
			return null;
		}

        readToken(); // integer

		if ( currentToken().equals( "." ) )
		{
			String fraction = nextToken();
			if ( !readIntegerToken( fraction ) )
			{
				return new Value( sign * StringUtilities.parseLong( integer ) );
			}

            readToken(); // .
            readToken(); // fraction

			return new Value( sign * StringUtilities.parseDouble( integer + "." + fraction ) );
		}

		return new Value( sign * StringUtilities.parseLong( integer ) );
	}

	private boolean readIntegerToken( final String token )
	{
		if ( token == null )
		{
			return false;
		}

		for ( int i = 0; i < token.length(); ++i )
		{
			if ( !Character.isDigit( token.charAt( i ) ) )
			{
				return false;
			}
		}

		return true;
	}

	private Value parseString( Type type )
	{
		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		StringBuilder resultString = new StringBuilder();
		char stopCharacter, ch;
		ArrayList<Value> list = null;
		if ( type == null )
		{	// Plain string constant
			stopCharacter = currentLine.charAt( 0 );
		}
		else
		{	// Typed plural constant - handled by same code as plain strings
			// so that they can share escape character processing
			stopCharacter = ']';
			list = new ArrayList<Value>();
		}

		for ( int i = 1; ; ++i )
		{
			if ( i == currentLine.length() )
			{
				if ( type == null )
				{	// Plain strings can't span lines
					throw parseException( "No closing \" found" );
				}
                currentLine = "";
                fixLines();
				i = 0;
				if ( currentLine == null )
				{
					throw parseException( "No closing ] found" );
				}
			}

			ch = currentLine.charAt( i );
			if ( ch == '\\' )
			{
				ch = currentLine.charAt( ++i );

				switch ( ch )
				{
				case 'n':
					resultString.append( '\n' );
					break;

				case 'r':
					resultString.append( '\r' );
					break;

				case 't':
					resultString.append( '\t' );
					break;

				case 'x':
					try
					{
						int hex08 = Integer.parseInt( currentLine.substring( i + 1, i + 3 ), 16 );
						resultString.append( (char) hex08 );
						i += 2;
					}
					catch ( NumberFormatException e )
					{
						throw parseException( "Hexadecimal character escape requires 2 digits" );
					}
					break;

				case 'u':
					try
					{
						int hex16 = Integer.parseInt( currentLine.substring( i + 1, i + 5 ), 16 );
						resultString.append( (char) hex16 );
						i += 4;
					}
					catch ( NumberFormatException e )
					{
						throw parseException( "Unicode character escape requires 4 digits" );
					}
					break;

				default:
					if ( Character.isDigit( ch ) )
					{
						try
						{
							int octal = Integer.parseInt( currentLine.substring( i, i + 3 ), 8 );
							resultString.append( (char) octal );
							i += 2;
							break;
						}
						catch ( NumberFormatException e )
						{
							throw parseException( "Octal character escape requires 3 digits" );
						}
					}
					resultString.append( ch );
				}
			}
			else if ( ch == stopCharacter ||
				( type != null && ch == ',' ) )
			{
				if ( type == null )
				{	// Plain string
                    currentLine = currentLine.substring( i + 1 ); //+ 1 to get rid of '"' token
                    currentToken = null;
					return new Value( resultString.toString() );
				}
				String element = resultString.toString().trim();
				resultString.setLength( 0 );
				if ( element.length() != 0 )
				{
					Value value = DataTypes.parseValue( type, element, false );
					if ( value == null )
					{
						throw parseException( "Bad " + type.toString() + " value: \"" + element + "\"" );
					}
					list.add( value );
				}
				if ( ch == ']' )
				{
                    currentLine = currentLine.substring( i + 1 );
                    currentToken = null;
					if ( list.size() == 0 )
					{	// Empty list - caller will interpret this specially
						return null;
					}
					return new PluralValue( type, list );
				}
			}
			else
			{
				resultString.append( currentLine.charAt( i ) );
			}
		}
	}

	private Value parseTypedConstant( final BasicScope scope )
	{
        readToken(); // read $

		String name = currentToken();
		Type type = parseType( scope, false, false );
		boolean plurals = false;

		if ( type == null )
		{
			StringBuilder buf = new StringBuilder( currentLine );
			int length = name.length();

			if ( name.endsWith( "es" ) )
			{
				buf.delete( length - 2, length );
			}
			else if ( name.endsWith( "s" ) )
			{
				buf.deleteCharAt( length - 1 );
			}
			else if ( name.endsWith( "a" ) )
			{
				buf.deleteCharAt( length - 1 );
				buf.insert( length - 1, "um" );
			}
			else
			{
				throw parseException( "Unknown type " + name );
			}

            currentLine = buf.toString();
            currentToken = null;
			type = parseType( scope, false, false );

			plurals = true;
		}

		if ( type == null )
		{
			throw parseException( "Unknown type " + name );
		}

		if ( !type.isPrimitive() )
		{
			throw parseException( "Non-primitive type " + name );
		}

		if ( !currentToken().equals( "[" ) )
		{
			throw parseException( "[", currentToken() );
		}

		if ( plurals )
		{
			Value value = parseString( type );
			if ( value != null )
			{
				return value;	// explicit list of values
			}
			value = type.allValues();
			if ( value != null )
			{
				return value;	// implicit enumeration
			}
			throw parseException( "Can't enumerate all " + name );
		}

		StringBuilder resultString = new StringBuilder();

		for ( int i = 1;; ++i )
		{
			if ( i == currentLine.length() )
			{
				throw parseException( "No closing ] found" );
			}
			else if ( currentLine.charAt( i ) == '\\' )
			{
				resultString.append( currentLine.charAt( ++i ) );
			}
			else if ( currentLine.charAt( i ) == ']' )
			{
                currentLine = currentLine.substring( i + 1 ); //+1 to get rid of ']' token
                currentToken = null;
				String input = resultString.toString().trim();

				// Make sure that only ASCII characters appear
				// in the string
				if ( !input.matches( "^\\p{ASCII}*$" ) )
				{
					throw parseException( "Typed constant $" + type.toString() + "[" + input + "] contains non-ASCII characters" );
				}

				Value value = DataTypes.parseValue( type, input, false );
				if ( value == null )
				{
					throw parseException( "Bad " + type.toString() + " value: \"" + input + "\"" );
				}
				return value;
			}
			else
			{
				resultString.append( currentLine.charAt( i ) );
			}
		}
	}

	private Operator parseOperator( final String oper )
	{
		if ( oper == null || !isOperator( oper ) )
		{
			return null;
		}

		return new Operator( oper, this );
	}

	private boolean isOperator( final String oper )
	{
		return oper.equals( "!" ) ||
			oper.equals( "?" ) ||
			oper.equals( ":" ) ||
			oper.equals( "*" ) ||
			oper.equals( "**" ) ||
			oper.equals( "/" ) ||
			oper.equals( "%" ) ||
			oper.equals( "+" ) ||
			oper.equals( "-" ) ||
			oper.equals( "&" ) ||
			oper.equals( "^" ) ||
			oper.equals( "|" ) ||
			oper.equals( "~" ) ||
			oper.equals( "<<" ) ||
			oper.equals( ">>" ) ||
			oper.equals( ">>>" ) ||
			oper.equals( "<" ) ||
			oper.equals( ">" ) ||
			oper.equals( "<=" ) ||
			oper.equals( ">=" ) ||
			oper.equals( "==" ) ||
			oper.equals( "!=" ) ||
			oper.equals( "||" ) ||
			oper.equals( "&&" ) ||
			oper.equals( "contains" ) ||
			oper.equals( "remove" );
	}

	private Value parseVariableReference( final BasicScope scope )
	{
		if ( currentToken() == null || !parseIdentifier( currentToken() ) )
		{
			return null;
		}

		String name = currentToken();
		Variable var = scope.findVariable( name, true );

		if ( var == null )
		{
			throw parseException( "Unknown variable '" + name + "'" );
		}

        readToken(); // read name

		if ( currentToken() == null || !currentToken().equals( "[" ) && !currentToken().equals( "." ) )
		{
			return new VariableReference( var );
		}

		return parseVariableReference( scope, var );
	}

	private Value parseVariableReference( final BasicScope scope, final Variable var )
	{
		Type type = var.getType();
		ValueList indices = new ValueList();

		boolean parseAggregate = currentToken().equals( "[" );

		while ( currentToken() != null && (currentToken().equals( "[" ) || currentToken().equals( "." ) || parseAggregate && currentToken().equals(
			"," ) ) )
		{
			Value index;

			type = type.getBaseType();

			if ( currentToken().equals( "[" ) || currentToken().equals( "," ) )
			{
                readToken(); // read [ or ,
				parseAggregate = true;

				if ( !( type instanceof AggregateType ) )
				{
					if ( indices.isEmpty() )
					{
						throw parseException( "Variable '" + var.getName() + "' cannot be indexed" );
					}
					else
					{
						throw parseException( "Too many keys for '" + var.getName() + "'" );
					}
				}

				AggregateType atype = (AggregateType) type;
				index = parseExpression( scope );
				if ( index == null )
				{
					throw parseException( "Index for '" + var.getName() + "' expected" );
				}

				if ( !index.getType().getBaseType().equals( atype.getIndexType().getBaseType() ) )
				{
					throw parseException(
						"Index for '" + var.getName() + "' has wrong data type " + "(expected " + atype.getIndexType() + ", got " + index.getType() + ")" );
				}

				type = atype.getDataType();
			}
			else
			{
                readToken(); // read .

				// Maybe it's a function call with an implied "this" parameter.

				if ( nextToken().equals( "(" ) )
				{
					return parseCall(
						scope, indices.isEmpty() ? new VariableReference( var ) : new CompositeReference( var, indices, this ) );
				}

				type = type.asProxy();
				if ( !( type instanceof RecordType ) )
				{
					throw parseException( "Record expected" );
				}

				RecordType rtype = (RecordType) type;

				String field = currentToken();
				if ( field == null || !parseIdentifier( field ) )
				{
					throw parseException( "Field name expected" );
				}

				index = rtype.getFieldIndex( field );
				if ( index == null )
				{
					throw parseException( "Invalid field name '" + field + "'" );
				}
                readToken(); // read name
				type = rtype.getDataType( index );
			}

			indices.add( index );

			if ( parseAggregate && currentToken() != null )
			{
				if ( currentToken().equals( "]" ) )
				{
                    readToken(); // read ]
					parseAggregate = false;
				}
			}
		}

		if ( parseAggregate )
		{
			throw parseException( currentToken(), "]" );
		}

		return new CompositeReference( var, indices, this );
	}

	private String parseDirective( final String directive )
	{
		if ( currentToken() == null || !currentToken().equalsIgnoreCase( directive ) )
		{
			return null;
		}

        readToken(); //directive

		if ( currentToken() == null )
		{
			throw parseException( "<", currentToken() );
		}

		int directiveEndIndex = currentLine.indexOf( ";" );
		if ( directiveEndIndex == -1 )
		{
			directiveEndIndex = currentLine.length();
		}
		String resultString = currentLine.substring( 0, directiveEndIndex );

		int startIndex = resultString.indexOf( "<" );
		int endIndex = resultString.indexOf( ">" );

		if ( startIndex != -1 && endIndex == -1 )
		{
			throw parseException( "No closing > found" );
		}

		if ( startIndex == -1 )
		{
			startIndex = resultString.indexOf( "\"" );
			endIndex = resultString.indexOf( "\"", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				throw parseException( "No closing \" found" );
			}
		}

		if ( startIndex == -1 )
		{
			startIndex = resultString.indexOf( "\'" );
			endIndex = resultString.indexOf( "\'", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				throw parseException( "No closing \' found" );
			}
		}

		if ( endIndex == -1 )
		{
			endIndex = resultString.indexOf( ";" );
			if ( endIndex == -1 )
			{
				endIndex = resultString.length();
			}
		}

		resultString = resultString.substring( startIndex + 1, endIndex );
        currentLine = currentLine.substring( endIndex );
        currentToken = null;

		if ( currentToken().equals( ">" ) || currentToken().equals( "\"" ) || currentToken().equals(
			"\'" ) )
		{
            readToken(); //get rid of '>' or '"' token
		}

		if ( currentToken().equals( ";" ) )
		{
            readToken(); //read ;
		}

		return resultString;
	}

	private void parseScriptName()
	{
		String resultString = parseDirective( "script" );
		if ( scriptName == null )
		{
            scriptName = resultString;
		}
	}

	private void parseNotify()
	{
		String resultString = parseDirective( "notify" );
		if ( notifyRecipient == null )
		{
            notifyRecipient = resultString;
		}
	}

	private String parseImport()
	{
		return parseDirective( "import" );
	}

	public static boolean validCoercion( Type lhs, Type rhs, final Operator oper )
	{
		int ltype = lhs.getBaseType().getType();
		int rtype = rhs.getBaseType().getType();

		if ( oper.isInteger() )
		{
			return ( ltype == DataTypes.TYPE_INT && rtype == DataTypes.TYPE_INT );
		}
		if ( oper.isLogical() )
		{
			return ltype == rtype && ( ltype == DataTypes.TYPE_INT || ltype == DataTypes.TYPE_BOOLEAN );
		}
		return Parser.validCoercion( lhs, rhs, oper.toString() );
	}

	public static boolean validCoercion( Type lhs, Type rhs, final String oper )
	{
		// Resolve aliases

		lhs = lhs.getBaseType();
		rhs = rhs.getBaseType();

		if ( oper == null )
		{
			return lhs.getType() == rhs.getType();
		}

		// "oper" is either a standard operator or is a special name:
		//
		// "parameter" - value used as a function parameter
		//	lhs = parameter type, rhs = expression type
		//
		// "return" - value returned as function value
		//	lhs = function return type, rhs = expression type
		//
		// "assign" - value
		//	lhs = variable type, rhs = expression type

		// The "contains" operator requires an aggregate on the left
		// and the correct index type on the right.

		if ( oper.equals( "contains" ) )
		{
			return lhs.getType() == DataTypes.TYPE_AGGREGATE && ( (AggregateType) lhs ).getIndexType().equals( rhs );
		}

		// If the types are equal, no coercion is necessary
		if ( lhs.equals( rhs ) )
		{
			return true;
		}

		// Noncoercible strings only accept strings
		if ( lhs.equals( DataTypes.STRICT_STRING_TYPE ) )
		{
			return rhs.equals( DataTypes.TYPE_STRING ) || rhs.equals( DataTypes.TYPE_BUFFER );
		}

		// Anything coerces to a string
		if ( lhs.equals( DataTypes.TYPE_STRING ) )
		{
			return true;
		}

		// Anything coerces to a string for concatenation
		if ( oper.equals( "+" ) && rhs.equals( DataTypes.TYPE_STRING ) )
		{
			return true;
		}

		// Int coerces to float
		if ( lhs.equals( DataTypes.TYPE_INT ) && rhs.equals( DataTypes.TYPE_FLOAT ) )
		{
			return true;
		}

		if ( lhs.equals( DataTypes.TYPE_FLOAT ) && rhs.equals( DataTypes.TYPE_INT ) )
		{
			return true;
		}

		return false;
	}

	// **************** Tokenizer *****************

	private String getNextLine()
	{
		try
		{
			do
			{
				// Read a line from input, and break out of the
				// do-while loop when you've read a valid line

                fullLine = commandStream.readLine();

				// Return null at end of file
				if ( fullLine == null )
				{
					return null;
				}

				// Remove whitespace at front and end
                fullLine = fullLine.trim();
			}
			while ( fullLine.length() == 0 );

			// Found valid currentLine - return it

			return fullLine;
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private String currentToken()
	{
		// Repeat until we get a token
		while ( true )
		{
			// If we've already parsed a token, return it
			if ( currentToken != null )
			{
				return currentToken;
			}

			// Locate next token
            fixLines();
			if ( currentLine == null )
			{
				return null;
			}

			// "#" starts a whole-line comment
			if ( currentLine.startsWith( "#" ) )
			{
				// Skip the comment
                currentLine = "";
				continue;
			}

			// Get the next token for consideration
            currentToken = currentLine.substring( 0, tokenLength( currentLine ) );

			// "//" starts a comment which consumes the rest of the line
			if ( currentToken.equals( "//" ) )
			{
				// Skip the comment
                currentToken = null;
                currentLine = "";
				continue;
			}

			// "/*" starts a comment which is terminated by "*/"
			if ( !currentToken.equals( "/*" ) )
			{
				return currentToken;
			}

			while ( currentLine != null )
			{
				int end = currentLine.indexOf( "*/" );
				if ( end == -1 )
				{
					// Skip entire line
                    currentLine = "";
                    fixLines();
					continue;
				}

                currentLine = currentLine.substring( end + 2 );
                currentToken = null;
				break;
			}
		}
	}

	private String nextToken()
	{
        fixLines();

		if ( currentLine == null )
		{
			return null;
		}

		if ( tokenLength( currentLine ) >= currentLine.length() )
		{
			if ( nextLine == null )
			{
				return null;
			}

			return nextLine.substring( 0, tokenLength( nextLine ) ).trim();
		}

		String result = currentLine.substring( tokenLength( currentLine ) ).trim();

		if ( result.equals( "" ) )
		{
			if ( nextLine == null )
			{
				return null;
			}

			return nextLine.substring( 0, tokenLength( nextLine ) );
		}

		return result.substring( 0, tokenLength( result ) );
	}

	private void readToken()
	{
        fixLines();

		if ( currentLine == null )
		{
			return;
		}

        currentLine = currentLine.substring( tokenLength( currentLine ) );
	}

	private int tokenLength( final String s )
	{
		int result;
		if ( s == null )
		{
			return 0;
		}

		for ( result = 0; result < s.length(); result++ )
		{
			if ( result + 3 < s.length() && tokenString( s.substring( result, result + 4 ) ) )
			{
				return result == 0 ? 4 : result;
			}

			if ( result + 2 < s.length() && tokenString( s.substring( result, result + 3 ) ) )
			{
				return result == 0 ? 3 : result;
			}

			if ( result + 1 < s.length() && tokenString( s.substring( result, result + 2 ) ) )
			{
				return result == 0 ? 2 : result;
			}

			if ( result < s.length() && tokenChar( s.charAt( result ) ) )
			{
				return result == 0 ? 1 : result;
			}
		}

		return result; //== s.length()
	}

	private void fixLines()
	{
        currentToken = null;
		if ( currentLine == null )
		{
			return;
		}

		while ( currentLine.equals( "" ) )
		{
            currentLine = nextLine;
            lineNumber = commandStream.getLineNumber();
            nextLine = getNextLine();

			if ( currentLine == null )
			{
				return;
			}
		}

        currentLine = currentLine.trim();

		if ( nextLine == null )
		{
			return;
		}

		while ( nextLine.equals( "" ) )
		{
            nextLine = getNextLine();
			if ( nextLine == null )
			{
				return;
			}
		}

        nextLine = nextLine.trim();
	}

	private boolean tokenChar( char ch )
	{
		switch ( ch )
		{
		case ' ':
		case '\t':
		case '.':
		case ',':
		case '{':
		case '}':
		case '(':
		case ')':
		case '$':
		case '!':
		case '~':
		case '+':
		case '-':
		case '=':
		case '"':
		case '\'':
		case '*':
		case '/':
		case '%':
		case '|':
		case '^':
		case '&':
		case '[':
		case ']':
		case ';':
		case '<':
		case '>':
		case '?':
		case ':':
			return true;
		}
		return false;
	}

	private boolean tokenString( final String s )
	{
		return Parser.multiCharTokens.contains( s );
	}

	// **************** Parse errors *****************

	private ScriptException parseException( final String expected, final String actual )
	{
		return parseException( "Expected " + expected + ", found " + actual );
	}

	private ScriptException parseException( final String message )
	{
		return new ScriptException( message + " " + getLineAndFile() );
	}

	private ScriptException undefinedFunctionException( final String name, final ValueList params )
	{
		return parseException( undefinedFunctionMessage( name, params ) );
	}

	private ScriptException multiplyDefinedFunctionException( final String name, final VariableReferenceList params )
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append( "Function '" );
		Parser.appendFunction( buffer, name, params );
		buffer.append( "' defined multiple times" );
		return parseException( buffer.toString() );
	}

	public static String undefinedFunctionMessage( final String name, final ValueList params )
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append( "Function '" );
		Parser.appendFunction( buffer, name, params );
		buffer.append( "' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts." );
		return buffer.toString();
	}
	
	public final void warning( final String msg )
	{
		RequestLogger.printLine( "WARNING: " + msg + " " + getLineAndFile() );
	}

	private static void appendFunction(  final StringBuffer buffer, final String name, final ParseTreeNodeList params )
	{
		buffer.append( name );
		buffer.append( "(" );

		Iterator it = params.iterator();
		boolean first = true;
		while ( it.hasNext() )
		{
			if ( !first )
			{
				buffer.append( ", " );
			}
			else
			{
				buffer.append( " " );
				first = false;
			}
			Value current = (Value) it.next();
			Type type = current.getType();
			buffer.append( type );
		}

		buffer.append( " )" );
	}

	private String getLineAndFile()
	{
		return Parser.getLineAndFile( shortFileName, lineNumber );
	}

	public static String getLineAndFile( final String fileName, final int lineNumber )
	{
		if ( fileName == null )
		{
			return "(" + Preferences.getString( "commandLineNamespace" ) + ")";
		}

		return "(" + fileName + ", line " + lineNumber + ")";
	}

	public static void printIndices( final ValueList indices, final PrintStream stream, final int indent )
	{
		if ( indices == null )
		{
			return;
		}

		Iterator it = indices.iterator();
		while ( it.hasNext() )
		{
			Value current = (Value) it.next();
			Interpreter.indentLine( stream, indent );
			stream.println( "<KEY>" );
			current.print( stream, indent + 1 );
		}
	}
}
