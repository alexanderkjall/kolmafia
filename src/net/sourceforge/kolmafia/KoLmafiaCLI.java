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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.LinkedList;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.chat.ChatSender;

import net.sourceforge.kolmafia.persistence.Aliases;

import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.request.LoginRequest;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.textui.command.*;

import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.PrefixMap;

public class KoLmafiaCLI
	extends KoLmafia
{
	public static final KoLmafiaCLI DEFAULT_SHELL = new KoLmafiaCLI( System.in );

	private static final Pattern HTMLTAG_PATTERN = Pattern.compile( "<.*?>", Pattern.DOTALL );
	private static final Pattern HEAD_PATTERN = Pattern.compile( "<head>.*?</head>", Pattern.DOTALL );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );

	private LinkedList queuedLines = new LinkedList();
	private CommandRetrieverThread retriever = new CommandRetrieverThread();
	private CommandProcessorThread processor = new CommandProcessorThread();

	public String previousLine = null;
	public String currentLine = null;
	private BufferedReader commandStream;
	private boolean isGUI = true;
	private boolean elseValid = false;
	private boolean elseRuns = false;

	public static boolean isExecutingCheckOnlyCommand = false;

	// Flag values for Commands:
	public static int FULL_LINE_CMD = 1;
	public static int FLOW_CONTROL_CMD = 2;

	public static void initialize()
	{
		System.out.println();
		System.out.println( StaticEntity.getVersion() );
		System.out.println( KoLConstants.VERSION_DATE );
		System.out.println();

		try
		{
			System.out.println( "Currently Running on " + System.getProperty( "os.name" ) );
			System.out.println( "Local Directory is " + KoLConstants.ROOT_LOCATION.getCanonicalPath() );
			System.out.println( "Using Java " + System.getProperty( "java.version" ) );
			System.out.println();
		}
		catch ( IOException e )
		{

		}

		StaticEntity.setClient( KoLmafiaCLI.DEFAULT_SHELL );
		RequestLogger.openStandard();
	}

	/**
	 * Constructs a new <code>KoLmafiaCLI</code> object. All data fields are initialized to their default values, the
	 * global settings are loaded from disk.
	 */

	public KoLmafiaCLI( final InputStream inputStream )
	{
		try
		{
            commandStream = FileUtilities.getReader( inputStream );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error opening input stream." );
		}
	}

	/**
	 * Utility method used to prompt the user for their login and password. Later on, when profiles are added, prompting
	 * for the user will automatically look up a password.
	 */

	public void attemptLogin( String username )
	{
		try
		{
			if ( username == null || username.length() == 0 )
			{
				System.out.println();
				System.out.print( "username: " );
				username = commandStream.readLine();
			}

			if ( username == null || username.length() == 0 )
			{
				System.out.println( "Invalid login." );
				return;
			}

			if ( username.startsWith( "login " ) )
			{
				username = username.substring( 6 ).trim();
			}

			String password = KoLmafia.getSaveState( username );

			if ( password == null )
			{
				System.out.print( "password: " );
				password = commandStream.readLine();
			}

			if ( password == null || password.length() == 0 )
			{
				System.out.println( "Invalid password." );
				return;
			}

			System.out.println();
			RequestThread.postRequest( new LoginRequest( username, password ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error in login attempt" );
		}
	}

	private class CommandRetrieverThread
		extends Thread
	{
		public void run()
		{
			String line;

			try
			{
				while ( ( line = commandStream.readLine() ) != null )
				{
					if ( line.equals( "--" ) )
					{
						RequestThread.declareWorldPeace();

						synchronized (queuedLines)
						{
                            queuedLines.clear();
						}
					}
					else if ( line.length() == 0 || line.startsWith( "#" ) || line.startsWith( "//" ) || line.startsWith( "\'" ) )
					{
						synchronized (queuedLines)
						{
                            queuedLines.addLast( null );
						}
					}
					else
					{
						synchronized (queuedLines)
						{
                            queuedLines.addLast( line );
						}
					}
				}

                commandStream.close();
                currentLine = null;
			}
			catch ( IOException e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}
	}

	private class CommandProcessorThread
		extends Thread
	{
		public void run()
		{
			String line;

			do
			{
				line = getNextLine( " > " );

				if ( StaticEntity.getClient() == KoLmafiaCLI.this )
				{
					KoLmafia.forceContinue();
				}

				if ( line != null )
				{
                    isGUI = false;
                    executeLine( line );
                    isGUI = true;
				}

				if ( StaticEntity.getClient() == KoLmafiaCLI.this )
				{
					KoLmafia.forceContinue();
				}
			}
			while ( line != null && KoLmafia.permitsContinue() );
		}
	}

	public String getNextLine( String message )
	{
		String line = null;

		while ( line == null )
		{
			if ( message != null && queuedLines.isEmpty() && StaticEntity.getClient() == KoLmafiaCLI.this && retriever.isAlive() )
			{
				RequestLogger.printLine();

				System.out.print( message );
			}

			if ( queuedLines.isEmpty() && retriever.isAlive() )
			{
				PauseObject pauser = new PauseObject();

				do
				{
					pauser.pause( 100 );
				}
				while ( queuedLines.isEmpty() && retriever.isAlive() );
			}

			if ( queuedLines.isEmpty() )
			{
				return null;
			}

			if ( StaticEntity.getClient() == KoLmafiaCLI.this )
			{
				RequestLogger.printLine();
			}

			synchronized (queuedLines)
			{
				line = (String) queuedLines.removeFirst();
			}
		}

		return line;
	}

	/**
	 * A utility method which waits for commands from the user, then executing each command as it arrives.
	 */

	public void listenForCommands()
	{
		KoLmafia.forceContinue();

		if ( StaticEntity.getClient() == KoLmafiaCLI.this )
		{
            retriever.start();
            processor.start();
		}
		else
		{
            retriever.run();
            processor.run();
		}
	}

	public void executeLine( String line )
	{
        executeLine( line, null );
	}

	public void executeLine( String line, final Interpreter interpreter )
	{
		if ( line == null || KoLmafia.refusesContinue() )
		{
			return;
		}

		String origLine = line;

		line = line.replaceAll( "[ \t]+", " " ).trim();
		if ( line.length() == 0 )
		{
			return;
		}

		// Pass through escaped character entities to ASH
		if ( !line.startsWith( "ash" ) )
		{
			line = CharacterEntities.unescape( line );
		}

		// First, handle all the aliasing that may be
		// defined by the user.

        currentLine = line = Aliases.apply( line );
		if ( !line.startsWith( "repeat" ) )
		{
            previousLine = line;
		}

		while ( KoLmafia.permitsContinue() && line.length() > 0 )
		{
			line = line.trim();

			int splitIndex = line.indexOf( ";" );
			String parameters;

			if ( splitIndex != -1 )
			{
				parameters = line.substring( 0, splitIndex );
				line = line.substring( splitIndex + 1 );
			}
			else
			{
				parameters = line;
				line = "";
			}

			// At this point, "parameters" has no leading
			// spaces. It may have trailing spaces.

			String trimmed = parameters.trim();
			if ( trimmed.length() == 0 )
			{
				continue;
			}

			if ( trimmed.startsWith( "/" ) )
			{
				ChatSender.executeMacro( trimmed );
				continue;
			}

			// "trimmed" has no leading or trailing spaces. Its
			// first word is the command.

			splitIndex = trimmed.indexOf( " " );

			String lcommand = trimmed.toLowerCase();
			String command;

			if ( splitIndex == -1 )
			{
				// Single word command. No parameters.
				command = trimmed;
				trimmed = "";
			}
			else if ( AbstractCommand.lookup.get( lcommand ) != null && AbstractCommand.lookup.getKeyType( lcommand ) == PrefixMap.EXACT_KEY )
			{
				// Multiword command
				command = lcommand;
				trimmed = "";
			}
			else
			{
				command = trimmed.substring( 0, splitIndex );
				lcommand = command.toLowerCase();
				parameters = parameters.substring( splitIndex + 1 );
				trimmed = parameters.trim();
			}

			// "parameters" has no leading spaces. It may have
			// trailing spaces.
			// "trimmed" has no leading or trailing spaces.

			if ( command.endsWith( "?" ) )
			{
				KoLmafiaCLI.isExecutingCheckOnlyCommand = true;
				int length = command.length();
				command = command.substring( 0, length - 1 );
				lcommand = lcommand.substring( 0, length - 1 );
			}

			AbstractCommand handler = (AbstractCommand) AbstractCommand.lookup.get( lcommand );
			int flags = handler == null ? 0 : handler.flags;
			if ( flags == KoLmafiaCLI.FULL_LINE_CMD && !line.equals( "" ) )
			{
				// parameters are un-trimmed original
				// parameters + rest of line
				trimmed = parameters + ";" + line;
				line = "";
			}

			if ( flags == KoLmafiaCLI.FLOW_CONTROL_CMD )
			{
				String continuation = getContinuation( line );
				if ( !KoLmafia.permitsContinue() )
				{
					return;
				}
				handler.continuation = continuation;
				handler.CLI = this;
				handler.run( lcommand, trimmed );
				handler.CLI = null;
				KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
                previousLine = command + " " + trimmed + ";" + continuation;
				return;
			}

            executeCommand( command, trimmed, interpreter );
			KoLmafiaCLI.isExecutingCheckOnlyCommand = false;
		}

		if ( KoLmafia.permitsContinue() )
		{
			// Notify user-entered Daily Deeds that the command was
			// successful.
			PreferenceListenerRegistry.firePreferenceChanged( origLine );
		}
	}

	private String getContinuation( String line )
	{
		line = line.trim();

		StringBuilder block = new StringBuilder( line );
		boolean seenCmd = false, needAnotherCmd = false;
		while ( true )
		{
			while ( line.length() > 0 )
			{
				String command;
				int splitIndex = line.indexOf( ";" );
				if ( splitIndex == -1 )
				{
					command = line.toLowerCase();
					line = "";
				}
				else
				{
					command = line.substring( 0, splitIndex ).toLowerCase();
					line = line.substring( splitIndex + 1 ).trim();
				}
				if ( command.equals( "" ) )
				{
					continue;
				}
				seenCmd = true;
				needAnotherCmd = false;
				command = command.split( " " )[ 0 ];
				if ( command.endsWith( "?" ) )
				{
					command = command.substring( 0, command.length() - 1 );
				}
				AbstractCommand handler = (AbstractCommand) AbstractCommand.lookup.get( command );
				int flags = handler == null ? 0 : handler.flags;
				if ( flags == KoLmafiaCLI.FULL_LINE_CMD )
				{
					line = "";
					break;
				}
				if ( flags == KoLmafiaCLI.FLOW_CONTROL_CMD )
				{
					needAnotherCmd = true;
				}
			}
			if ( seenCmd && !needAnotherCmd )
			{
				return block.toString();
			}

			// We need another line to complete the command.  However, if the
			// original command didn't come from the input stream (the gCLI
			// entry field, perhaps), trying to read a line would just hang.
			if ( isGUI )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Multi-line statements cannot be used from the gCLI." );
				return "";
			}

			line = getNextLine( "+> " );

			if ( line == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Unterminated conditional statement." );
				return "";
			}
			block.append( ";" );
			block.append( line );
		}
	}

	/**
	 * A utility command which decides, based on the command to be executed, what to be done with it. It can either
	 * delegate this to other functions, or do it itself.
	 */

	public void executeCommand( String command, String parameters )
	{
        executeCommand( command, parameters, null );
	}

	private void executeCommand( String command, String parameters, Interpreter interpreter )
	{
		Integer requestId = RequestThread.openRequestSequence();

		try
		{
            doExecuteCommand( command, parameters, interpreter );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
		finally
		{
			RequestThread.closeRequestSequence( requestId );
		}
	}

	private void doExecuteCommand( String command, String parameters, Interpreter interpreter )
	{
		String lcommand = command.toLowerCase();

		// If the command has already been disabled, then return
		// from this function.

		if ( StaticEntity.isDisabled( lcommand ) )
		{
			RequestLogger.printLine( "Called disabled command: " + lcommand + " " + parameters );
			return;
		}

		if ( parameters.equals( "refresh" ) )
		{
			parameters = lcommand;
			lcommand = command = "refresh";
		}

		AbstractCommand handler = (AbstractCommand) AbstractCommand.lookup.get( lcommand );

		if ( handler == null )
		{
			handler = AbstractCommand.getSubstringMatch( lcommand );
		}

		if ( handler != null )
		{
			if ( lcommand.endsWith( "*" ) )
			{
				RequestLogger.printLine( "(A * after a command name indicates that it can be " + "typed in a longer form.  There's no need to type the asterisk!)" );
			}

			handler.CLI = this;
			handler.interpreter = interpreter;
			handler.run( lcommand, parameters );
			handler.CLI = null;
			handler.interpreter = null;

			return;
		}

		// If all else fails, then assume that the
		// person was trying to call a script.

		CallScriptCommand.call( "call", command + " " + parameters, interpreter );
	}

	public void elseRuns( final boolean shouldRun )
	{
        elseRuns = shouldRun;
        elseValid = true;
	}

	public boolean elseValid()
	{
		return elseValid;
	}

	/**
	 * Indicates that a following "else" command is not valid here.
	 */
	public void elseInvalid()
	{
        elseValid = false;
	}


	/**
	 * Tests whether a "else" command should be executed, and mark further
	 * "else"s as invalid. If this "else" is invalid, generate an error and
	 * return false.
	 */

	public boolean elseRuns()
	{
		if ( !elseValid )
		{
			KoLmafia.updateDisplay(
				MafiaState.ERROR,
				"'else' must follow a conditional command, and both must be at the outermost level." );
			return false;
		}
        elseValid = false;
		return elseRuns;
	}

	static
	{
		new AbortCommand().register( "abort" );
		new AcquireCommand().register( "find" ).register( "acquire" ).register( "retrieve" );
		new AdventureCommand().registerPrefix( "adv" );
		new AliasCommand().register( "alias" );
		new AreaSummaryCommand().register( "safe" );
		new AshMultiLineCommand().register( "<inline-ash-script>" );
		new AshRefCommand().register( "ashref" );
		new AshSingleLineCommand().register( "ash" ).register( "ashq" );
		new AutoAttackCommand().register( "aa" ).register( "autoattack" );
		new AutoMallCommand().register( "automall" );
		new AutoSellCommand().register( "sell" ).register( "autosell" );
		new BacktraceCommand().register( "backtrace" );
		new BadMoonCommand().register( "badmoon" );
		new BallPitCommand().register( "ballpit" );
		new BangPotionsCommand().register( "!" ).register( "bang" ).register( "vials" );
		new BasementCommand().register( "basement" );
		new BreakfastCommand().register( "breakfast" );
		new BudgetCommand().register( "budget" );
		new BuffbotCommand().register( "buffbot" );
		new BurnMpCommand().register( "burn" );
		new BuyCommand().register( "buy" ).register( "mallbuy" );
		new CallScriptCommand().register( "verify" ).register( "validate" ).register( "check" ).register( "call" ).register(
			"run" ).registerPrefix( "exec" ).register( "load" ).register( "start" ).register( "profile" );
		new CampgroundCommand().registerPrefix( "camp" );
		new ChangeCombatScriptCommand().register( "ccs" );
		new CheckDataCommand().register( "newdata" ).
			register( "checkconsumables" ).
			register( "checkconsumption" ).
			register( "checkeffects" ).
			register( "checkfamiliars" ).
			register( "checkitems" ).
			register( "checkmodifiers" ).
			register( "checkplurals" ).
			register( "checkpotions" ).
			register( "checkpowers" ).
			register( "checkprofile" ).
			register( "checkpulverization" ).
			register( "checkshields" ).
			register( "checkzapgroups" );
		new ChessCommand().register( "chess" );
		new ChoiceCommand().register( "choice" );
		new ChipsCommand().register( "chips" );
		new ClanCommand().register( "clan" );
		new ClanSofaCommand().register( "sofa" ).register( "sleep" );
		new ClanStashCommand().register( "stash" );
		new CleanupJunkRequest().register( "junk" ).register( "cleanup" );
		new ClearBufferCommand().register( "clear" ).register( "cls" ).register( "reset" );
		new CliRefCommand().register( "help" ).register( "which" );
		new ClosetCommand().register( "closet" );
		new ColorEchoCommand().register( "colorecho" ).register( "cecho" );
		new ComparisonShopCommand().register( "cheapest" ).register( "expensive" );
		new CompleteQuestCommand().register( "maze" ).
			registerPrefix( "hedge" ).
			register( "tower" ).
			register( "guardians" ).
			register( "chamber" ).
			register( "guild" ).
			register( "gourd" ).
			register( "tavern" ).
			register( "baron" ).
			register( "dvorak" ).
			register( "choice-goal" ).
			register( "sven" );
		new ConcertCommand().register( "concert" );
		new ConditionsCommand().registerPrefix( "goal" ).registerPrefix( "condition" ).registerPrefix( "objective" );
		new CondRefCommand().register( "condref" );
		new CouncilCommand().register( "council" );
		new CountersCommand().register( "counters" );
		new CreateItemCommand().register( "create" ).register( "make" ).register( "bake" ).register( "mix" ).register(
			"smith" ).register( "tinker" ).register( "ply" );
		new CrimboTreeCommand().register( "crimbotree" );
		new DebugCreateCommand().register( "debugcreate" );
		new DebugRequestCommand().register( "debug" );
		new DemonNamesCommand().register( "demons" );
		new DisplayCaseCommand().register( "display" );
		new DustyBottlesCommand().register( "dusty" );
		new DwarfFactoryCommand().register( "factory" );
		new EchoCommand().register( "echo" ).register( "print" );
		new EditCommand().register( "edit" );
		new EditMoodCommand().registerPrefix( "trigger" );
		new ElseIfStatement().register( "elseif" );
		new ElseStatement().register( "else" );
		new EnableCommand().register( "enable" ).register( "disable" );
		new EnthroneCommand().register( "enthrone" );
		new EquipCommand().register( "equip" ).register( "wear" ).register( "wield" );
		new EventsCommand().register( "events" );
		new ExitCommand().register( "exit" ).register( "quit" );
		new ExtendEffectCommand().register( "up" );
		new FakeAddItemCommand().register( "fakeitem" );
		new FakeRemoveItemCommand().register( "removeitem" );
		new FamiliarCommand().register( "familiar" );
		new FaxCommand().register( "fax" );
		new FaxbotCommand().register( "faxbot" );
		new FullEchoCommand().register( "fecho" ).register( "fprint" );
		new FlowerHuntCommand().register( "flowers" ).register( "swagger" );
		new FoldItemCommand().register( "fold" ).register( "squeeze" );
		new ForumCommand().registerPrefix( "forum" );
		new FriarBlessingCommand().register( "friars" );
		new GalaktikCommand().register( "galaktik" );
		new GapCommand().register("gap");
		new GardenCommand().register( "garden" );
		new GongCommand().register( "gong" );
		new GrandpaCommand().register( "grandpa" );
		new HallOfLegendsCommand().register( "donate" );
		new HatterCommand().register( "hatter" );
		new HermitCommand().register( "hermit" );
		new HiddenCityCommand().register( "hiddencity" );
		new HotTubCommand().register( "hottub" ).register( "soak" );
		new IfStatement().register( "if" );
		new ItemTraceCommand().register( "itrace" );
		new JukeboxCommand().register( "jukebox" );
		new KitchenCommand().registerSubstring( "kitchen" );
		new LeafletCommand().register( "leaflet" );
		new LogEchoCommand().register( "logecho" ).register( "logprint" );
		new LoginCommand().register( "login" );
		new LogoutCommand().register( "logout" );
		new MacroTestCommand().register( "macrotest" );
		new MallRepriceCommand().register( "reprice" ).register( "undercut" );
		new MallSellCommand().register( "mallsell" );
		new ManaRestoreCountCommand().register( "mpitems" );
		new MemoryCleanupCommand().register( "gc" );
		new MirrorLogCommand().registerSubstring( "mirror" );
		new ModifierListCommand().register( "modifies" );
		new ModifierMaximizeCommand().register( "maximize" );
		new ModifierTraceCommand().register( "modtrace" );
		new ModRefCommand().register( "modref" );
		new MoleRefCommand().register( "moleref" );
		new MonsterDataCommand().register( "monsters" );
		new MonsterLevelCommand().register( "mind-control" ).register( "mcd" );
		new MoodCommand().registerPrefix( "mood" );
		new MushroomFieldCommand().register( "field" );
		new NamespaceAddCommand().register( "using" );
		new NamespaceListCommand().register( "namespace" );
		new NemesisCommand().register( "nemesis" );
		new NewEffectCommand().register( "neweffect" );
		new NunneryCommand().register( "nuns" );
		new OlfactionCommand().registerPrefix( "olfact" ).register( "putty" );
		new OutfitCheckpointCommand().register( "checkpoint" );
		new OutfitCommand().register( "outfit" );
		new PandaCommand().register( "panda" );
		new PastamancerEntityCommand().register( "entity" ).register( "guardian" );
		new PirateInsultsCommand().register( "insults" );
		new PlayerSnapshotCommand().register( "log" );
		new PoolCommand().register( "pool" );
		new PrefTraceCommand().register( "ptrace" );
		new PripheaCommand().register( "priphea" );
		new PulverizeCommand().register( "smash" ).register( "pulverize" );
		new PvpAttackCommand().register( "attack" );
		new PvpStealCommand().register( "pvp" ).register( "steal" );
		new QuarkCommand().register( "quark" );
		new RaffleCommand().register( "raffle" );
		new RecipeCommand().register( "recipe" ).register( "ingredients" );
		new RecoverCommand().registerPrefix( "restore" ).registerPrefix( "recover" ).registerPrefix( "check" );
		new RefreshStatusCommand().register( "refresh" );
		new RegisterAdventureCommand().register( "location" );
		new RelayBrowserCommand().register( "relay" );
		new RepeatLineCommand().register( "repeat" );
		new RestaurantCommand().register( "restaurant" ).registerSubstring( "brewery" );
		new SaveAsMoodCommand().register( "save as mood" );
		new SearchMallCommand().register( "searchmall" );
		new SendMessageCommand().register( "send" ).register( "kmail" ).register( "csend" );
		new SetHolidayCommand().register( "holiday" );
		new SetPreferencesCommand().register( "get" ).register( "set" );
		new ShopCommand().register( "shop" );
		new ShowDataCommand().registerPrefix( "inv" ).register( "storage" ).register( "session" ).register( "summary" ).register(
			"effects" ).register( "status" ).register( "skills" ).register( "locations" ).register( "encounters" ).registerPrefix(
			"moon" );
		new ShowerCommand().register( "shower" );
		new SkateParkCommand().register( "skate" );
		new SkeeballCommand().register( "skeeball" );
		new SlimeStackCommand().registerPrefix( "slime-stack");
		new SorceressEntrywayCommand().register( "entryway" );
		new SpeculateCommand().register( "speculate" ).register( "whatif" );
		new StickersCommand().registerPrefix( "sticker" );
		new StorageCommand().register( "hagnk" ).register( "pull" );
		new GrayGUICommand().register( "graygui" ).register( "greygui" ).register( "jstack" );
		new HeapDumpCommand().register( "jmap" ).register( "heapdump" );
		new StyxPixieCommand().register( "styx" );
		new SubmitSpadeDataCommand().register( "spade" );
		new SummonDemonCommand().register( "summon" );
		new SwimmingPoolCommand().register( "swim" );
		new TelescopeCommand().register( "telescope" );
		new TestCommand().register( "test" );
		new ThrowItemCommand().register( "throw" );
		new TrainFamiliarCommand().register( "train" );
		new TryStatement().register( "try" );
		new UnaliasCommand().register( "unalias" );
		new UneffectCommand().register( "shrug" ).register( "uneffect" ).register( "remedy" );
		new UnequipCommand().register( "unequip" ).register( "remove" );
		new UntinkerCommand().register( "untinker" );
		new UpdateDataCommand().register( "update" );
		new UseItemCommand().register( "eat" ).register( "drink" ).register( "use" ).register( "chew" )
			.register( "eatsilent" ).register( "overdrink" )
			.register( "hobo" ).register( "ghost" ).register( "slimeling" );
		new UseSkillCommand().register( "cast" ).register( "skill" );
		new VersionCommand().register( "version" );
		new VisitURLCommand().register( "text" ).registerPrefix( "http://" ).registerSubstring( ".php" );
		new VolcanoCommand().register( "volcano" );
		new WaitCommand().register( "wait" ).register( "waitq" ).register( "pause" );
		new WhileStatement().register( "while" );
		new WikiLookupCommand().register( "lookup" );
		new WikiMafiaSearchCommand().register( "ashwiki" );
		new WikiSearchCommand().register( "wiki" );
		new WindowOpenCommand().register( "chat" ).register( "mail" ).registerPrefix( "opt" ).register( "item" ).register(
			"gear" ).register( "radio" );
		new WinGameCommand().register( "win game" );
		new WumpusCommand().register( "wumpus" );
		new ZapCommand().register( "zap" );

		new CommandAlias( "campground", "rest" ).register( "rest" );
		new CommandAlias( "equip", "off-hand" ).register( "second" ).register( "hold" ).register( "dualwield" );
		new CommandAlias( "skills", "buff" ).registerPrefix( "buff" );
		new CommandAlias( "skills", "passive" ).registerPrefix( "pass" );
		new CommandAlias( "skills", "self" ).registerPrefix( "self" );
		new CommandAlias( "skills", "combat" ).registerPrefix( "combat" );
	}

	@Override
	public void showHTML( final String location, final String text )
	{
		// Remove HTML header and comments.
		String displayText = KoLmafiaCLI.HEAD_PATTERN.matcher( text ).replaceAll( "" );
		displayText = KoLmafiaCLI.COMMENT_PATTERN.matcher( displayText ).replaceAll( "" );

		// Strip out all the new lines found in the source
		// so you don't accidentally add more new lines than
		// necessary.

		displayText = displayText.replaceAll( "[\r\n]+", "" );

		// Replace all things symbolizing paragraph breaks
		// with actual new lines.

		displayText = displayText.replaceAll( "<(br|tr)[^>]*>", "\n" ).replaceAll( "<(p|blockquote)[^>]*>", "\n\n" );

		// Replace HTML character entities with something
		// which is more readily printable.

		displayText = KoLmafiaCLI.HTMLTAG_PATTERN.matcher( displayText ).replaceAll( "" );
		displayText = displayText.replaceAll( "&nbsp;", " " );
		displayText = displayText.replaceAll( "&trade;", " [tm]" );
		displayText = displayText.replaceAll( "&ntilde;", "n" );
		displayText = displayText.replaceAll( "&quot;", "" );

		// Allow only one new line at a time in the HTML
		// that is printed.

		displayText = displayText.replaceAll( "\n\n\n+", "\n\n" );

		RequestLogger.printLine( displayText.trim() );
	}

	public static File findScriptFile( final String filename )
	{
		File scriptFile = new File( filename );
		if ( scriptFile.exists() )
		{
			return scriptFile.isDirectory() ? null : scriptFile;
		}

		scriptFile = new File( UtilityConstants.ROOT_LOCATION, filename );
		if ( scriptFile.exists() )
		{
			return scriptFile.isDirectory() ? null : scriptFile;
		}

		if ( KoLConstants.SCRIPT_LOCATION.exists() )
		{
			scriptFile = KoLmafiaCLI.findScriptFile( KoLConstants.SCRIPT_LOCATION, filename, false );
			if ( scriptFile != null )
			{
				return scriptFile.isDirectory() ? null : scriptFile;
			}
		}

		if ( KoLConstants.PLOTS_LOCATION.exists() )
		{
			scriptFile = new File( KoLConstants.PLOTS_LOCATION, filename );
			if ( scriptFile.exists() )
			{
				return scriptFile.isDirectory() ? null : scriptFile;
			}
		}

		if ( KoLConstants.RELAY_LOCATION.exists() )
		{
			scriptFile = KoLmafiaCLI.findScriptFile( KoLConstants.RELAY_LOCATION, filename, false );
			if ( scriptFile != null )
			{
				return scriptFile.isDirectory() ? null : scriptFile;
			}
		}

		return null;
	}

	public static File findScriptFile( final File directory, final String filename )
	{
		return KoLmafiaCLI.findScriptFile( directory, filename, false );
	}

	private static File findScriptFile( final File directory, final String filename, final boolean isFallback )
	{
		File scriptFile = new File( directory, filename );

		if ( scriptFile.exists() )
		{
			return scriptFile;
		}

		if ( !isFallback )
		{
			scriptFile = KoLmafiaCLI.findScriptFile( directory, filename + ".cli", true );
			if ( scriptFile != null )
			{
				return scriptFile;
			}

			scriptFile = KoLmafiaCLI.findScriptFile( directory, filename + ".txt", true );
			if ( scriptFile != null )
			{
				return scriptFile;
			}

			scriptFile = KoLmafiaCLI.findScriptFile( directory, filename + ".ash", true );
			if ( scriptFile != null )
			{
				return scriptFile;
			}
		}

		File[] contents = DataUtilities.listFiles( directory );
		for ( int i = 0; i < contents.length; ++i )
		{
			if ( contents[ i ].isDirectory() )
			{
				scriptFile = KoLmafiaCLI.findScriptFile( contents[ i ], filename, false );
				if ( scriptFile != null )
				{
					return scriptFile;
				}
			}
		}

		return null;
	}

	public static String buildRelayScriptMenu()
	{
		boolean any = false;
		StringBuilder buf = new StringBuilder();
		buf.append( "<select onchange='if (this.selectedIndex>0) { top.mainpane.location=this.options[this.selectedIndex].value; this.options[0].selected=true;}'><option>-run script-</option>" );
		File[] files = DataUtilities.listFiles( KoLConstants.RELAY_LOCATION );
		for ( int i = 0; i < files.length; ++i )
		{
			String name = files[ i ].getName();
			if ( name.startsWith( "relay_" ) && name.endsWith( ".ash" ) )
			{
				any = true;
				buf.append( "<option value='" );
				buf.append( name );
				buf.append( "'>" );
				buf.append( name.substring( 6, name.length() - 4 ) );
				buf.append( "</option>" );
			}
		}

		if ( any )
		{
			buf.append( "</select>&nbsp;" );
			return buf.toString();
		}
		return "";
	}
}
