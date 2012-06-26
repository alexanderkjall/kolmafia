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

package net.sourceforge.kolmafia.request;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.LoginManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.WumpusManager;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.HobopolisDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;
import net.sourceforge.kolmafia.webui.NemesisDecorator;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentToken;
import org.htmlcleaner.ContentToken;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class FightRequest
	extends GenericRequest
{
	// Character-class permissions
	private static boolean canSteal = false;
	private static boolean canSummon = false;

	private static final PauseObject PAUSER = new PauseObject();
	public static final FightRequest INSTANCE = new FightRequest();

	private static final AdventureResult AMNESIA = new AdventureResult( "Amnesia", 1, true );
	private static final AdventureResult CUNCTATITIS = new AdventureResult( "Cunctatitis", 1, true );
	public static final AdventureResult ONTHETRAIL = new AdventureResult( "On the Trail", 1, true );
	public static final AdventureResult BIRDFORM = new AdventureResult( "Form of...Bird!", 1, true );
	public static final AdventureResult MOLEFORM = new AdventureResult( "Shape of...Mole!", 1, true );
	public static final AdventureResult INFERNO = new AdventureResult( "Taste the Inferno", 1, true );

	public static final AdventureResult DICTIONARY1 = ItemPool.get( ItemPool.DICTIONARY, 1 );
	public static final AdventureResult DICTIONARY2 = ItemPool.get( ItemPool.FACSIMILE_DICTIONARY, 1 );
	private static final AdventureResult TEQUILA = ItemPool.get( ItemPool.TEQUILA, -1 );

	public static AdventureResult haikuEffect = EffectPool.get( Effect.HAIKU_STATE_OF_MIND );
	public static AdventureResult anapestEffect = EffectPool.get( Effect.JUST_THE_BEST_ANAPESTS );

	private static final int HEALTH = 0;
	private static final int ATTACK = 1;
	private static final int DEFENSE = 2;

	private static int lastUserId = 0;
	private static String lostInitiativeMessage = "";
	private static String wonInitiativeMessage = "";
	public static String lastMacroUsed = "";

	private static int preparatoryRounds = 0;
	private static String consultScriptThatDidNothing = null;
	public static boolean waitingForSpecial;
	public static String ireallymeanit = null;
	public static boolean ignoreSpecialEncounter = false;

	public static String lastResponseText = "";
	private static boolean isTrackingFights = false;
	private static boolean foundNextRound = false;
	private static boolean haveFought = false;
	private static boolean shouldRefresh = false;
	private static boolean initializeAfterFight = false;

	private static boolean isAutomatingFight = false;
	private static boolean isUsingConsultScript = false;

	private static final Pattern COMBATITEM_PATTERN = Pattern.compile( "<option[^>]*?value=(\\d+)[^>]*?>[^>]*?\\((\\d+)\\)</option>" );
	private static final Pattern CONDITIONAL_COMBATSKILL_PATTERN = Pattern.compile( "<option[^>]*?value=\"(7\\d{3,3})[^>]*?>[^>]*?\\((\\d+)[^<]*</option>" );

	public static final Pattern SKILL_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern ITEM1_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ITEM2_PATTERN = Pattern.compile( "whichitem2=(\\d+)" );
	private static final Pattern CLEESH_PATTERN =
		Pattern.compile( "newpic\\(\".*?\", \"(.*?)\".*?\\)" );
	private static final Pattern WORN_STICKER_PATTERN =
		Pattern.compile( "A sticker falls off your weapon, faded and torn" );
	private static final Pattern BEEP_PATTERN =
		Pattern.compile( "Your Evilometer beeps (\\d+) times" );
	private static final Pattern BALLROOM_SONG_PATTERN =
		Pattern.compile( "You hear strains of (?:(lively)|(mellow)|(lovely)) music in the distance" );
	private static final Pattern WHICHMACRO_PATTERN =
		Pattern.compile( "whichmacro=(\\d+)" );
	private static final Pattern MACRO_PATTERN =
		Pattern.compile( "\\G.*?<!-- macroaction: *(\\w+) ++(\\d+)?,? *(\\d+)?.*?(?=$|<!-- macroaction)", Pattern.DOTALL );
	private static final Pattern FULLPAGE_PATTERN =
		Pattern.compile( "^.*$", Pattern.DOTALL );
	private static final Pattern MACRO_COMPACT_PATTERN =
		Pattern.compile( "(?:#.*?)?([;\\n])[\\s;\\n]*" );

	private static final Pattern NS_ML_PATTERN =
		Pattern.compile( "The Sorceress pauses for a moment\\, mutters some words under her breath\\, and straightens out her dress\\. Her skin seems to shimmer for a moment\\." );

	private static final Pattern DETECTIVE_PATTERN =
		Pattern.compile( "I deduce that this monster has approximately (\\d+) hit points" );
	private static final Pattern SPACE_HELMET_PATTERN =
		Pattern.compile( "Opponent HP: (\\d+)" );
	private static final Pattern SLIMED_PATTERN =
		Pattern.compile( "it blasts you with a massive loogie that sticks to your (.*?), pulls it off of you" );
	private static final Pattern MULTIFIGHT_PATTERN =
		Pattern.compile( "href=\"?/?fight.php" );
	
	private static final Pattern KEYOTRON_PATTERN =
		Pattern.compile( "key-o-tron emits (\\d) short" );

	private static final AdventureResult TOOTH = ItemPool.get( ItemPool.SEAL_TOOTH, 1);
	private static final AdventureResult SPICES = ItemPool.get( ItemPool.SPICES, 1);
	private static final AdventureResult MERCENARY = ItemPool.get( ItemPool.TOY_MERCENARY, 1);
	private static final AdventureResult STOMPER = ItemPool.get( ItemPool.MINIBORG_STOMPER, 1);
	private static final AdventureResult LASER = ItemPool.get( ItemPool.MINIBORG_LASER, 1);
	private static final AdventureResult DESTROYER = ItemPool.get( ItemPool.MINIBORG_DESTROYOBOT, 1);
	public static final AdventureResult ANTIDOTE = ItemPool.get( ItemPool.ANTIDOTE, 1);
	private static final AdventureResult EXTRACTOR = ItemPool.get( ItemPool.ODOR_EXTRACTOR, 1);
	private static final AdventureResult PUTTY_SHEET = ItemPool.get( ItemPool.SPOOKY_PUTTY_SHEET, 1);
	private static final AdventureResult RAINDOH_BOX = ItemPool.get( ItemPool.RAIN_DOH_BOX, 1);
	private static final AdventureResult CAMERA = ItemPool.get( ItemPool.CAMERA, 1);
	private static final AdventureResult SHAKING_CAMERA = ItemPool.get( ItemPool.SHAKING_CAMERA, 1);
	private static final AdventureResult PHOTOCOPIER = ItemPool.get( ItemPool.PHOTOCOPIER, 1);
	private static final AdventureResult PHOTOCOPIED_MONSTER = ItemPool.get( ItemPool.PHOTOCOPIED_MONSTER, 1);

	private static final String TOOTH_ACTION = "item" + ItemPool.SEAL_TOOTH;
	private static final String SPICES_ACTION = "item" + ItemPool.SPICES;
	private static final String MERCENARY_ACTION = "item" + ItemPool.TOY_MERCENARY;
	private static final String STOMPER_ACTION = "item" + ItemPool.MINIBORG_STOMPER;
	private static final String LASER_ACTION = "item" + ItemPool.MINIBORG_LASER;
	private static final String DESTROYER_ACTION = "item" + ItemPool.MINIBORG_DESTROYOBOT;
	private static final String OLFACTION_ACTION = "skill" + SkillDatabase.OLFACTION;

	private static boolean castNoodles = false;
	private static boolean castCleesh = false;
	private static boolean jiggledChefstaff = false;
	private static boolean squeezedStressBall = false;
	private static boolean canOlfact = true;
	private static boolean canStomp = false;
	private static boolean summonedGhost = false;
	public static boolean haiku = false;
	public static boolean anapest = false;
	private static int currentRound = 0;
	private static boolean inMultiFight = false;

	private static String nextAction = null;

	private static AdventureResult desiredScroll = null;

	private static final AdventureResult SCROLL_334 = ItemPool.get( ItemPool.SCROLL_334, 1);
	private static final AdventureResult SCROLL_668 = ItemPool.get( ItemPool.SCROLL_668, 1);
	private static final AdventureResult SCROLL_30669 = ItemPool.get( ItemPool.SCROLL_30669, 1);
	private static final AdventureResult SCROLL_33398 = ItemPool.get( ItemPool.SCROLL_33398, 1);
	private static final AdventureResult SCROLL_64067 = ItemPool.get( ItemPool.SCROLL_64067, 1);
	private static final AdventureResult SCROLL_64735 = ItemPool.get( ItemPool.GATES_SCROLL, 1);
	private static final AdventureResult SCROLL_31337 = ItemPool.get( ItemPool.ELITE_SCROLL, 1);

	private static final Object[][] NEMESIS_WEAPONS =
	{	// class, LEW, ULEW
		{
			KoLCharacter.SEAL_CLUBBER,
			ItemPool.get( ItemPool.HAMMER_OF_SMITING, 1 ),
			ItemPool.get( ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR, 1 )
		},
		{
			KoLCharacter.TURTLE_TAMER,
			ItemPool.get( ItemPool.CHELONIAN_MORNINGSTAR, 1 ),
			ItemPool.get( ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS, 1 )
		},
		{
			KoLCharacter.PASTAMANCER,
			ItemPool.get( ItemPool.GREEK_PASTA_OF_PERIL, 1 ),
			ItemPool.get( ItemPool.WRATH_OF_THE_PASTALORDS, 1 )
		},
		{
			KoLCharacter.SAUCEROR,
			ItemPool.get( ItemPool.SEVENTEEN_ALARM_SAUCEPAN, 1 ),
			ItemPool.get( ItemPool.WINDSOR_PAN_OF_THE_SOURCE, 1 )
		},
		{
			KoLCharacter.DISCO_BANDIT,
			ItemPool.get( ItemPool.SHAGADELIC_DISCO_BANJO, 1 ),
			ItemPool.get( ItemPool.SEEGERS_BANJO, 1 )
		},
		{
			KoLCharacter.ACCORDION_THIEF,
			ItemPool.get( ItemPool.SQUEEZEBOX_OF_THE_AGES, 1 ),
			ItemPool.get( ItemPool.TRICKSTER_TRIKITIXA, 1 )
		},
	};

	// Ultra-rare monsters
	private static final String[] RARE_MONSTERS =
	{
		"baiowulf",
		"count bakula",
		"crazy bastard",
		"hockey elemental",
		"hypnotist of hey deze",
		"infinite meat bug",
		"knott slanding",
		"master of thieves",
		"temporal bandit"
	};

	// Skills which cannot be used with a ranged weapon
	private static final HashSet<String> INVALID_WITH_RANGED_ATTACK = new HashSet<String>();
	static
	{
		INVALID_WITH_RANGED_ATTACK.add( "1003" );
		INVALID_WITH_RANGED_ATTACK.add( "skill thrust-smack" );
		INVALID_WITH_RANGED_ATTACK.add( "1005" );
		INVALID_WITH_RANGED_ATTACK.add( "skill lunging thrust-smack" );
		INVALID_WITH_RANGED_ATTACK.add( "2003" );
		INVALID_WITH_RANGED_ATTACK.add( "skill headbutt" );
		INVALID_WITH_RANGED_ATTACK.add( "2005" );
		INVALID_WITH_RANGED_ATTACK.add( "skill shieldbutt" );
		INVALID_WITH_RANGED_ATTACK.add( "2015" );
		INVALID_WITH_RANGED_ATTACK.add( "skill kneebutt" );
		INVALID_WITH_RANGED_ATTACK.add( "2103" );
		INVALID_WITH_RANGED_ATTACK.add( "skill head + knee combo" );
		INVALID_WITH_RANGED_ATTACK.add( "2105" );
		INVALID_WITH_RANGED_ATTACK.add( "skill head + shield combo" );
		INVALID_WITH_RANGED_ATTACK.add( "2106" );
		INVALID_WITH_RANGED_ATTACK.add( "skill knee + shield combo" );
		INVALID_WITH_RANGED_ATTACK.add( "2107" );
		INVALID_WITH_RANGED_ATTACK.add( "skill head + knee + shield combo" );
	}

	// Skills which require a shield
	private static final HashSet<String> INVALID_WITHOUT_SHIELD = new HashSet<String>();
	static
	{
		INVALID_WITHOUT_SHIELD.add( "2005" );
		INVALID_WITHOUT_SHIELD.add( "skill shieldbutt" );
		INVALID_WITHOUT_SHIELD.add( "2105" );
		INVALID_WITHOUT_SHIELD.add( "skill head + shield combo" );
		INVALID_WITHOUT_SHIELD.add( "2106" );
		INVALID_WITHOUT_SHIELD.add( "skill knee + shield combo" );
		INVALID_WITHOUT_SHIELD.add( "2107" );
		INVALID_WITHOUT_SHIELD.add( "skill head + knee + shield combo" );
	}

	private static final HashSet<String> INVALID_OUT_OF_WATER = new HashSet<String>();
	static
	{
		INVALID_OUT_OF_WATER.add( "2024" );
		INVALID_OUT_OF_WATER.add( "skill summon leviatuga" );
	}

	private static final String[][] EVIL_ZONES =
	{
		{
			"defiled alcove",
			"cyrptAlcoveEvilness",
		},
		{
			"defiled cranny",
			"cyrptCrannyEvilness",
		},
		{
			"defiled niche",
			"cyrptNicheEvilness",
		},
		{
			"defiled nook",
			"cyrptNookEvilness",
		},
	};
	
	private static final String[][] BUGBEAR_BIODATA =
	{
		{
			"hypodermic bugbear",
			"biodataMedbay"
		},
		{
			"scavenger bugbear",
			"biodataWasteProcessing"
		},
		{
			"batbugbear",
			"biodataSonar"
		},
		{
			"bugbear scientist",
			"biodataScienceLab"
		},
		{
			"bugaboo",
			"biodataMorgue"
		},
		{
			"black ops bugbear",
			"biodataSpecialOps"
		},
		{
			"battlesuit bugbear type",
			"biodataEngineering"
		},
		{
			"ancient unspeakable bugbear",
			"biodataNavigation"
		},
		{
			"trendy bugbear chef",
			"biodataGalley"
		}
	};

	// Make an HTML cleaner
	private static HtmlCleaner cleaner = new HtmlCleaner();

	static
	{
		CleanerProperties props = FightRequest.cleaner.getProperties();
		props.setTranslateSpecialEntities( false );
		props.setRecognizeUnicodeChars( false );
		props.setOmitXmlDeclaration( true );
	}

	/**
	 * Constructs a new <code>FightRequest</code>. User settings will be
	 * used to determine the kind of action to be taken during the battle.
	 */

	private FightRequest()
	{
		super( "fight.php" );
	}

	public static void initialize()
	{
		FightRequest.canSteal = KoLCharacter.isMoxieClass();
		FightRequest.canSummon = KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	private static final Pattern CAN_STEAL_PATTERN =
		Pattern.compile( "value=\"(Pick (?:His|Her|Their|Its) Pocket(?: Again)?|Look for Shiny Objects)\"" );

	public static boolean canStillSteal()
	{
		// Return true if you can still steal during this battle.

		// Must be a Moxie class character or any character in Birdform
		// or have a tiny black hole equipped.in the offhand slot
		if ( !( FightRequest.canSteal ||
			KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) ||
			KoLCharacter.hasEquipped( ItemPool.TINY_BLACK_HOLE, EquipmentManager.OFFHAND ) ) )
		{
			return false;
		}

		// Look for buttons that allow you to pickpocket
		String responseText = FightRequest.lastResponseText;
		Matcher matcher = FightRequest.CAN_STEAL_PATTERN.matcher( responseText );
		return matcher.find();
	}

	public static boolean canCastNoodles()
	{
		return !FightRequest.castNoodles ||
			MonsterStatusTracker.getLastMonsterName().equals( "spaghetti demon" );
	}

	public static boolean canOlfact()
	{
		return FightRequest.canOlfact && !KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL );

	}

	public static void initializeAfterFight()
	{
		FightRequest.initializeAfterFight = true;
	}

	public static boolean initializingAfterFight()
	{
		return FightRequest.initializeAfterFight;
	}

	public static boolean canStillSummon()
	{
		// Return true if you can still summon during this battle.

		// Must be a Pastamancer
		if ( !FightRequest.canSummon )
		{
			return false;
		}

		// Look for active buttons that allow you to summon
		// <input type=hidden name=action value="summon">

		String responseText = FightRequest.lastResponseText;
		return responseText.contains( "<input type=hidden name=action value=\"summon\">" );
	}

	public static boolean wonInitiative()
	{
		return	FightRequest.currentRound == 1 &&
			FightRequest.wonInitiative( FightRequest.lastResponseText );
	}

	public static boolean wonInitiative( String text )
	{
		// Regular encounter
		if ( text.contains( "You get the jump" ) )
			return true;

		// Can Has Cyborger
		if ( text.contains( "The Jump: " ) )
			return true;

		// Blavious Kloop

		// You leap into combat, as quick as a wink,
		// attacking the monster before he can blink!

		if ( text.contains( "as quick as a wink" ) )
			return true;

		// Who got the jump? Oh please, who, tell me, who?
		// It wasn't your foe, so it must have been you!

		if ( text.contains( "It wasn't your foe, so it must have been you" ) )
			return true;

		// Your foe is so slow! So slow is your foe!
		// Much slower than you, who are ready to go!

		if ( text.contains( "Your foe is so slow" ) )
			return true;

		// Haiku dungeon

		//    Before he sees you,
		//    you're already attacking.
		//    You're sneaky like that.

		if ( text.contains( "You're sneaky like that." ) )
			return true;

		//    You leap at your foe,
		//    throwing caution to the wind,
		//    and get the first strike.

		if ( text.contains( "and get the first strike." ) )
			return true;

		//    You jump at your foe
		//    and strike before he's ready.
		//    Nice and sportsmanlike.

		if ( text.contains( "Nice and sportsmanlike." ) )
			return true;

		return false;
	}

	public void nextRound( String desiredAction )
	{
		if ( KoLmafia.refusesContinue() )
		{
			FightRequest.nextAction = "abort";
			return;
		}

		// First round, KoLmafia does not decide the action.
		// Update accordingly.

		if ( FightRequest.currentRound == 0 )
		{
			String macro = Macrofier.macrofy();

			FightRequest.nextAction = null;

			if ( FightRequest.ireallymeanit != null )
			{
				this.addFormField( "ireallymeanit", FightRequest.ireallymeanit );
				FightRequest.ireallymeanit = null;
			}

			if ( macro != null && macro.length() > 0 && (macro.contains( "\n" ) || macro.contains( ";" )) )
			{
				this.handleMacroAction( macro );
			}

			return;
		}

		// Always let the user see rare monsters

		for ( int i = 0; i < FightRequest.RARE_MONSTERS.length; ++i )
		{
			if ( MonsterStatusTracker.getLastMonsterName().contains( FightRequest.RARE_MONSTERS[i] ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "You have encountered the " + this.encounter );
				FightRequest.nextAction = "abort";
				return;
			}
		}

		// Desired action overrides any internal logic not related to macros

		if ( desiredAction != null && desiredAction.length() > 0 )
		{
			if ( CombatActionManager.isMacroAction( desiredAction ) )
			{
				this.handleMacroAction( desiredAction );
				return;
			}

			FightRequest.nextAction =
				CombatActionManager.getShortCombatOptionName( desiredAction );
		}
		else
		{
			// Fight automation is still considered automation.
			// If the player drops below the threshold, then go
			// ahead and halt the battle.

			if ( !RecoveryManager.runThresholdChecks() )
			{
				FightRequest.nextAction = "abort";
				return;
			}

			FightRequest.nextAction = null;

			String macro = Macrofier.macrofy();

			if ( macro != null && macro.length() > 0 )
			{
				if ( macro.contains( "\n" ) || macro.contains( ";" ) )
				{
					this.handleMacroAction( macro );
					return;
				}

				FightRequest.nextAction = CombatActionManager.getShortCombatOptionName( macro );
			}

			// Added emergency break for hulking construct

			else if ( problemFamiliar() &&
				 MonsterStatusTracker.getLastMonsterName().equals( "hulking construct" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Aborting combat automation due to Familiar that can stop automatic item usage." );
				return;
			}

			// Adding machine should override custom combat scripts as well,
			// since it's conditions-driven.

			else if ( MonsterStatusTracker.getLastMonsterName().equals( "rampaging adding machine" )
				&& !KoLConstants.activeEffects.contains( FightRequest.BIRDFORM )
				&& !FightRequest.waitingForSpecial )
			{
				this.handleAddingMachine();
			}

			// Hulking Constructs also require special handling

			else if ( MonsterStatusTracker.getLastMonsterName().equals( "hulking construct" ) )
			{
				this.handleHulkingConstruct();
			}

			if ( FightRequest.nextAction == null )
			{
				String combatAction = CombatActionManager.getCombatAction(
					MonsterStatusTracker.getLastMonsterName(), FightRequest.getRoundIndex(), false );

				FightRequest.nextAction =
					CombatActionManager.getShortCombatOptionName( combatAction );
			}
		}

		// If the person wants to use their own script,
		// then this is where it happens.

		if ( FightRequest.nextAction.startsWith( "consult" ) )
		{
			FightRequest.isUsingConsultScript = true;
			String scriptName = FightRequest.nextAction.substring( "consult".length() ).trim();

			Interpreter consultInterpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
			if ( consultInterpreter != null )
			{
				int initialRound = FightRequest.currentRound;

				String[] parameters = new String[3];
				parameters[0] = String.valueOf( FightRequest.currentRound );
				parameters[1] = MonsterStatusTracker.getLastMonsterName();
				parameters[2] = FightRequest.lastResponseText;

				consultInterpreter.execute( "main", parameters );

				if ( KoLmafia.refusesContinue() )
				{
					FightRequest.nextAction = "abort";
				}
				else if ( initialRound == FightRequest.currentRound )
				{
					if ( FightRequest.nextAction.equals( FightRequest.consultScriptThatDidNothing ) )
					{
						FightRequest.nextAction = "abort";
					}
					else
					{
						FightRequest.consultScriptThatDidNothing = FightRequest.nextAction;
					}
				}
				if ( FightRequest.currentRound != 0 )
				{
					// don't adjust round # if fight is over!
					FightRequest.preparatoryRounds +=
						FightRequest.currentRound - initialRound - 1;
				}

				// Continue running after the consult script
				this.responseCode = 200;
				return;
			}

			KoLmafia.updateDisplay( MafiaState.ABORT, "Consult script '" + scriptName + "' not found." );
			FightRequest.nextAction = "abort";
			return;
		}

		// Let the de-level action figure out what
		// should be done, and then re-process.

		if ( FightRequest.nextAction.startsWith( "delevel" ) )
		{
			FightRequest.nextAction = this.getMonsterWeakenAction();
		}

		this.updateCurrentAction();
	}

	private void handleMacroAction( String macro )
	{
		FightRequest.nextAction = "macro";

		this.addFormField( "action", "macro" );

		// In case the player continues the script from the relay browser,
		// insert a jump to the next restart point.

		if ( macro.contains( "#mafiarestart" ) )
		{
			String label = "mafiaskip" + macro.length();

			StringUtilities.singleStringReplace( macro, "#mafiarestart", "mark " + label );
			StringUtilities.singleStringReplace( macro, "#mafiaheader", "#mafiaheader\ngoto " + label );
		}

		this.addFormField( "macrotext", FightRequest.MACRO_COMPACT_PATTERN.matcher( macro ).replaceAll( "$1" ) );
	}

	public static String getCurrentKey()
	{
		return CombatActionManager.encounterKey( MonsterStatusTracker.getLastMonsterName() );
	}

	private void updateCurrentAction()
	{
		if ( FightRequest.shouldUseAntidote() )
		{
			FightRequest.nextAction = String.valueOf( ItemPool.ANTIDOTE );
			++FightRequest.preparatoryRounds;
		}

		if ( FightRequest.nextAction.equals( "special" ) )
		{
			FightRequest.waitingForSpecial = false;
			String specialAction = FightRequest.getSpecialAction();

			if ( GenericRequest.passwordHash.equals( "" ) || specialAction == null )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}

			FightRequest.nextAction = specialAction;
		}

		if ( FightRequest.nextAction.equals( "abort" ) )
		{
			// If the user has chosen to abort combat, flag it.
			--FightRequest.preparatoryRounds;
			return;
		}

		if ( FightRequest.nextAction.equals( "abort after" ) )
		{
			KoLmafia.abortAfter( "Aborted by CCS request" );
			--FightRequest.preparatoryRounds;
			this.nextRound( null );
			return;
		}

		if ( FightRequest.nextAction.equals( "skip" ) )
		{
			--FightRequest.preparatoryRounds;
			this.nextRound( null );
			return;
		}

		// User wants to run away
		if ( FightRequest.nextAction.contains( "run" ) && FightRequest.nextAction.contains( "away" ) )
		{
			Matcher runAwayMatcher = CombatActionManager.TRY_TO_RUN_AWAY_PATTERN.matcher( FightRequest.nextAction );

			int runaway = 0;

			if ( runAwayMatcher.find() )
			{
				runaway = StringUtilities.parseInt( runAwayMatcher.group( 1 ) );
			}

			FightRequest.nextAction = "runaway";

			if ( runaway > FightRequest.freeRunawayChance() )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}

			this.addFormField( "action", FightRequest.nextAction );
			return;
		}

		// User wants a regular attack
		if ( FightRequest.nextAction.startsWith( "attack" ) )
		{
			FightRequest.nextAction = "attack";
			this.addFormField( "action", FightRequest.nextAction );
			return;
		}

		if ( FightRequest.nextAction.startsWith( "twiddle" ) )
		{
			--FightRequest.preparatoryRounds;
			return;
		}

		if ( KoLConstants.activeEffects.contains( FightRequest.AMNESIA ) )
		{
			if ( !MonsterStatusTracker.willUsuallyMiss() )
			{
				FightRequest.nextAction = "attack";
				this.addFormField( "action", FightRequest.nextAction );
				return;
			}

			FightRequest.nextAction = "abort";
			return;
		}

		// Actually steal if the action says to steal

		if ( FightRequest.nextAction.contains( "steal" ) &&
                !FightRequest.nextAction.contains( "stealth" ) )
		{
			if ( FightRequest.canStillSteal() && MonsterStatusTracker.shouldSteal() )
			{
				FightRequest.nextAction = "steal";
				this.addFormField( "action", "steal" );
				return;
			}

			--FightRequest.preparatoryRounds;
			this.nextRound( null );
			return;
		}

		// Summon a ghost if requested.

		if ( FightRequest.nextAction.equals( "summon ghost" ) )
		{
			if ( FightRequest.canStillSummon() )
			{
				this.addFormField( "action", "summon" );
				return;
			}

			--FightRequest.preparatoryRounds;
			this.nextRound( null );
			return;
		}

		// Jiggle chefstaff if the action says to jiggle and we're
		// wielding a chefstaff. Otherwise, skip this action.

		if ( FightRequest.nextAction.startsWith( "jiggle" ) )
		{
			if ( !FightRequest.jiggledChefstaff &&
			     EquipmentManager.usingChefstaff() )
			{
				this.addFormField( "action", "chefstaff" );
				return;
			}

			// You can only jiggle once per round.
			--FightRequest.preparatoryRounds;
			this.nextRound( null );
			return;
		}

		// Handle DB combos.

		if ( FightRequest.nextAction.startsWith( "combo " ) )
		{
			String name = FightRequest.nextAction.substring( 6 );
			int[] combo = DiscoCombatHelper.getCombo( name );
			if ( combo == null )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Invalid combo '" + name + "' requested" );
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}

			StringBuffer macro = new StringBuffer();

			Macrofier.macroCommon( macro );
			Macrofier.macroCombo( macro, combo );

			this.addFormField( "action", "macro" );
			this.addFormField( "macrotext", macro.toString() );

			FightRequest.preparatoryRounds += combo.length - 1;

			return;
		}

		// If the player wants to use an item, make sure he has one
		if ( !FightRequest.nextAction.startsWith( "skill" ) )
		{
			if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
			{	// Can't use items in Birdform
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
			int item1, item2;

			int commaIndex = FightRequest.nextAction.indexOf( "," );
			if ( commaIndex != -1 )
			{
				item1 = StringUtilities.parseInt( FightRequest.nextAction.substring( 0, commaIndex ) );
				item2 = StringUtilities.parseInt( FightRequest.nextAction.substring( commaIndex + 1 ) );
			}
			else
			{
				item1 = StringUtilities.parseInt( FightRequest.nextAction );
				item2 = -1;
			}

			int itemCount = ( new AdventureResult( item1, 1 ) ).getCount( KoLConstants.inventory );

			if ( itemCount == 0 && item2 != -1 )
			{
				item1 = item2;
				item2 = -1;

				itemCount = ( new AdventureResult( item1, 1 ) ).getCount( KoLConstants.inventory );
			}

			if ( itemCount == 0 )
			{
				KoLmafia.updateDisplay(
					MafiaState.ABORT, "You don't have enough " + ItemDatabase.getItemName( item1 ) );
				FightRequest.nextAction = "abort";
				return;
			}

			if ( item1 == ItemPool.DICTIONARY || item1 == ItemPool.FACSIMILE_DICTIONARY )
			{
				if ( itemCount < 1 )
				{
					KoLmafia.updateDisplay( MafiaState.ABORT, "You don't have a dictionary." );
					FightRequest.nextAction = "abort";
					return;
				}

				if ( MonsterStatusTracker.getLastMonsterName().equals( "rampaging adding machine" ) )
				{
					FightRequest.nextAction = "attack";
					this.addFormField( "action", FightRequest.nextAction );
					return;
				}
			}

			this.addFormField( "action", "useitem" );
			this.addFormField( "whichitem", String.valueOf( item1 ) );

			if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				return;
			}

			if ( item2 != -1 )
			{
				itemCount = ( new AdventureResult( item2, 1 ) ).getCount( KoLConstants.inventory );

				if ( itemCount > 1 || item1 != item2 && itemCount > 0 )
				{
					FightRequest.nextAction += "," + String.valueOf( item2 );
					this.addFormField( "whichitem2", String.valueOf( item2 ) );
				}
				else
				{
					KoLmafia.updateDisplay(
						MafiaState.ABORT, "You don't have enough " + ItemDatabase.getItemName( item2 ) );
					FightRequest.nextAction = "abort";
				}

				return;
			}

			if ( singleUseCombatItem( item1 ) )
			{
				if ( KoLConstants.inventory.contains( FightRequest.MERCENARY ) )
				{
					FightRequest.nextAction += "," + FightRequest.MERCENARY_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.MERCENARY.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.DESTROYER ) )
				{
					FightRequest.nextAction += "," + FightRequest.DESTROYER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.DESTROYER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.LASER ) )
				{
					FightRequest.nextAction += "," + FightRequest.LASER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.LASER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.STOMPER ) )
				{
					FightRequest.nextAction += "," + FightRequest.STOMPER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.STOMPER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.TOOTH ) )
				{
					FightRequest.nextAction += "," + FightRequest.TOOTH_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.TOOTH.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.SPICES ) )
				{
					FightRequest.nextAction += "," + FightRequest.SPICES_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.SPICES.getItemId() ) );
				}
			}
			else if ( itemCount >= 2 && !soloUseCombatItem( item1 ))
			{
				FightRequest.nextAction += "," + FightRequest.nextAction;
				this.addFormField( "whichitem2", String.valueOf( item1 ) );
			}

			return;
		}

		// We do not verify that the character actually knows the skill
		// or that it is currently available. It can be complicated:
		// birdform skills are available only in birdform., but some
		// are available only if you've prepped them by eating the
		// appropriate kind of bug.

		// We do ensure that it is a combat skill.

		String skillIdString = FightRequest.nextAction.substring( 5 );
		int skillId = StringUtilities.parseInt( skillIdString );
		String skillName = SkillDatabase.getSkillName( skillId );

		if ( skillName == null || !SkillDatabase.isCombat( skillId ) )
		{
			if ( this.isAcceptable( 0, 0 ) )
			{
				FightRequest.nextAction = "attack";
				this.addFormField( "action", FightRequest.nextAction );
				return;
			}

			FightRequest.nextAction = "abort";
			return;
		}

		if ( skillName.equals( "Transcendent Olfaction" ) )
		{
			// You can't sniff if you are already on the trail.

			// You can't sniff in Bad Moon, even though the skill
			// shows up on the char sheet, unless you've recalled
			// your skills.

			if ( ( KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled() ) ||
			     KoLConstants.activeEffects.contains( EffectPool.get( Effect.ON_THE_TRAIL ) ) )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Consume Burrowgrub" ) )
		{
			// You can only consume 3 burrowgrubs per day

			if ( Preferences.getInteger( "burrowgrubSummonsRemaining" ) <= 0 )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Entangling Noodles" ) )
		{
			// You can only use this skill once per combat
			if ( FightRequest.castNoodles )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Squeeze Stress Ball" ) )
		{
			// You can only use this skill once per combat
			if ( FightRequest.squeezedStressBall )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Fire a badly romantic arrow" ) )
		{
			// You can only shoot 1 badly romantic arrow per day

			if ( Preferences.getInteger( "_badlyRomanticArrows" ) >= 1 ||
			     KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Fire a boxing-glove arrow" ) )
		{
			// You can only shoot 5 boxing-glove arrows per day

			if ( Preferences.getInteger( "_boxingGloveArrows" ) >= 5 ||
			     KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Fire a poison arrow" ) )
		{
			// You can only shoot 10 poison arrows per day

			if ( Preferences.getInteger( "_poisonArrows" ) >= 1 ||
			     KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Fire a fingertrap arrow" ) )
		{
			// You can only shoot 10 fingertrap arrows per day

			if ( Preferences.getInteger( "_fingertrapArrows" ) >= 10 ||
			     KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Release the Boots" ) )
		{
			// You can only release the boots 7 times per day

			if ( !FightRequest.canStomp )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}
		else if ( skillName.equals( "Nuclear Breath" ) )
		{
			// You can only use this skill if you have the Taste the Inferno effect

			if ( !KoLConstants.activeEffects.contains( FightRequest.INFERNO ) )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}
		}

		// Skills use MP. Make sure the character has enough.
		if ( KoLCharacter.getCurrentMP() < FightRequest.getActionCost() && !GenericRequest.passwordHash.equals( "" ) )
		{
			if ( !Preferences.getBoolean( "autoManaRestore" ) )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound( null );
				return;
			}

			if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
			{
				FightRequest.nextAction = "abort";
				return;
			}

			for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			{
				if ( MPRestoreItemList.CONFIGURES[ i ].isCombatUsable() && KoLConstants.inventory.contains( MPRestoreItemList.CONFIGURES[ i ].getItem() ) )
				{
					FightRequest.nextAction = String.valueOf( MPRestoreItemList.CONFIGURES[ i ].getItem().getItemId() );

					++FightRequest.preparatoryRounds;
					this.updateCurrentAction();
					return;
				}
			}

			FightRequest.nextAction = "abort";
			return;
		}

		if ( skillName.equals( "CLEESH" ) )
		{
			if ( FightRequest.castCleesh )
			{
				FightRequest.nextAction = "attack";
				this.addFormField( "action", FightRequest.nextAction );
				return;
			}

			FightRequest.castCleesh = true;
		}

		if ( FightRequest.isInvalidAttack( FightRequest.nextAction ) )
		{
			FightRequest.nextAction = "abort";
			return;
		}

		this.addFormField( "action", "skill" );
		this.addFormField( "whichskill", skillIdString );
	}

	private static boolean problemFamiliar()
	{
		return ( KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.BLACK_CAT ||
			 KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.OAF ) &&
			!KoLCharacter.hasEquipped( ItemPool.get( ItemPool.TINY_COSTUME_WARDROBE, 1 ) );

	}

	private boolean singleUseCombatItem( int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_SINGLE );
	}

	private boolean soloUseCombatItem( int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_SOLO );
	}

	public static boolean isInvalidRangedAttack( final String action )
	{
		if ( !INVALID_WITH_RANGED_ATTACK.contains( action.toLowerCase() ) )
		{
			return false;
		}

		int weaponId = EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId();

		if ( EquipmentDatabase.getWeaponType( weaponId ) == KoLConstants.RANGED )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "This skill is useless with ranged weapons." );
			return true;
		}

		return false;
	}

	public static boolean isInvalidShieldlessAttack( final String action )
	{
		if ( !INVALID_WITHOUT_SHIELD.contains( action.toLowerCase() ) )
		{
			return false;
		}

		if ( !EquipmentManager.usingShield() )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "This skill is useless without a shield." );
			return true;
		}

		return false;
	}

	public static boolean isInvalidLocationAttack( final String action )
	{
		if ( !INVALID_OUT_OF_WATER.contains( action.toLowerCase() ) )
		{
			return false;
		}

		KoLAdventure location = KoLAdventure.lastVisitedLocation();
		String zone = location != null ? location.getZone() : null;

		if ( zone != null && !zone.equals( "The Sea" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "This skill is useless out of water." );
			return true;
		}

		return false;
	}

	public static boolean isInvalidAttack( final String action )
	{
		return FightRequest.isInvalidRangedAttack( action ) ||
		       FightRequest.isInvalidShieldlessAttack( action ) ||
		       FightRequest.isInvalidLocationAttack( action );
	}

	public void runOnce( final String desiredAction )
	{
		this.clearDataFields();

		FightRequest.nextAction = null;
		FightRequest.isUsingConsultScript = false;

		if ( !KoLmafia.refusesContinue() )
		{
			this.nextRound( desiredAction );
		}

		if ( !FightRequest.isUsingConsultScript )
		{
			if ( FightRequest.currentRound == 0 || ( FightRequest.nextAction != null && !FightRequest.nextAction.equals( "abort" ) ) )
			{
				super.run();
			}
		}

		if ( FightRequest.nextAction != null && FightRequest.nextAction.equals( "abort" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "You're on your own, partner." );
		}
	}

	@Override
	public void run()
	{
		this.constructURLString( "fight.php" );

		FightRequest.isAutomatingFight = true;

		do
		{
			this.runOnce( null );
		}
		while ( this.responseCode == 200 && FightRequest.currentRound != 0 && !KoLmafia.refusesContinue() );

		if ( this.responseCode == 302 )
		{
			FightRequest.clearInstanceData();
		}

		if ( KoLmafia.refusesContinue() && FightRequest.currentRound != 0
			&& !FightRequest.isTrackingFights() )
		{
			this.showInBrowser( true );
		}

		FightRequest.isAutomatingFight = false;
	}

	public static boolean processResults( final String responseText )
	{
		return FightRequest.shouldRefresh;
	}

	private boolean isAcceptable( final int offenseModifier, final int defenseModifier )
	{
		if ( MonsterStatusTracker.willUsuallyMiss( defenseModifier ) ||
		     MonsterStatusTracker.willUsuallyDodge( offenseModifier ) )
		{
			return false;
		}

		return RecoveryManager.getRestoreCount() == 0;
	}

	private void handleAddingMachine()
	{
		int action = Preferences.getInteger( "addingScrolls" );
		// 0: show in browser
		// 1: create goal scrolls only
		// 2: create goal & 668
		// 3: create goal, 31337, 668
		if ( action == 0 )
		{
			FightRequest.nextAction = "abort";
			return;
		}
		else if ( FightRequest.desiredScroll != null )
		{
			this.createAddingScroll( FightRequest.desiredScroll );
		}
		else if ( GoalManager.hasGoal( FightRequest.SCROLL_64735 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_64735 );
		}
		else if ( GoalManager.hasGoal( FightRequest.SCROLL_64067 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_64067 );
		}
		else if ( GoalManager.hasGoal( FightRequest.SCROLL_31337 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_31337 );
		}
		else if ( GoalManager.hasGoal( FightRequest.SCROLL_668 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_668 );
		}
		else if ( action >= 3 )
		{
			this.createAddingScroll( FightRequest.SCROLL_31337 );
		}
		else if ( action >= 2 )
		{
			this.createAddingScroll( FightRequest.SCROLL_668 );
		}
	}

	private boolean createAddingScroll( final AdventureResult scroll )
	{
		// If the familiar can break automation, skip creation
		if ( problemFamiliar() )
		{
			return false;
		}

		AdventureResult part1 = null;
		AdventureResult part2 = null;

		if ( scroll == FightRequest.SCROLL_64735 )
		{
			part2 = FightRequest.SCROLL_64067;
			part1 = FightRequest.SCROLL_668;
		}
		else if ( scroll == FightRequest.SCROLL_64067 )
		{
			if ( !GoalManager.hasGoal( FightRequest.SCROLL_64067 ) && KoLConstants.inventory.contains( FightRequest.SCROLL_64067 ) )
			{
				return false;
			}

			part1 = FightRequest.SCROLL_30669;
			part2 = FightRequest.SCROLL_33398;
		}
		else if ( scroll == FightRequest.SCROLL_668 )
		{
			part1 = FightRequest.SCROLL_334;
			part2 = FightRequest.SCROLL_334;
		}
		else if ( scroll == FightRequest.SCROLL_31337 )
		{
			part1 = FightRequest.SCROLL_30669;
			part2 = FightRequest.SCROLL_668;
		}
		else
		{
			return false;
		}

		if ( FightRequest.desiredScroll != null )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.nextAction = String.valueOf( part2.getItemId() );

			FightRequest.desiredScroll = null;
			return true;
		}

		if ( part1 == part2 && part1.getCount( KoLConstants.inventory ) < 2 )
		{
			return false;
		}

		if ( !KoLConstants.inventory.contains( part1 ) )
		{
			return this.createAddingScroll( part1 ) || this.createAddingScroll( part2 );
		}

		if ( !KoLConstants.inventory.contains( part2 ) )
		{
			return this.createAddingScroll( part2 );
		}

		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.nextAction = String.valueOf( part1.getItemId() );

			FightRequest.desiredScroll = scroll;
			return true;
		}

		++FightRequest.preparatoryRounds;
		FightRequest.nextAction = part1.getItemId() + "," + part2.getItemId();
		return true;
	}

	private void handleHulkingConstruct()
	{
		if ( FightRequest.currentRound > 1 )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.nextAction = "3155";
			return;
		}

		AdventureResult card1 = ItemPool.get( ItemPool.PUNCHCARD_ATTACK, 1 );
		AdventureResult card2 = ItemPool.get( ItemPool.PUNCHCARD_WALL, 1 );

		if ( !KoLConstants.inventory.contains( card1 ) ||
		     !KoLConstants.inventory.contains( card2 ) )
		{
			FightRequest.nextAction = "runaway";
			return;
		}

		++FightRequest.preparatoryRounds;
		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			FightRequest.nextAction = "3146";
		}
		else
		{
			FightRequest.nextAction = "3146,3155";
		}
	}

	private String getMonsterWeakenAction()
	{
		if ( this.isAcceptable( 0, 0 ) )
		{
			return "attack";
		}

		int desiredSkill = 0;
		boolean isAcceptable = false;

		// Disco Eye-Poke
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Eye-Poke" ) )
		{
			desiredSkill = 5003;
			isAcceptable = this.isAcceptable( -1, -1 );
		}

		// Disco Dance of Doom
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance of Doom" ) )
		{
			desiredSkill = 5005;
			isAcceptable = this.isAcceptable( -3, -3 );
		}

		// Disco Dance II: Electric Boogaloo
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance II: Electric Boogaloo" ) )
		{
			desiredSkill = 5008;
			isAcceptable = this.isAcceptable( -5, -5 );
		}

		// Tango of Terror
		if ( !isAcceptable && KoLCharacter.hasSkill( "Tango of Terror" ) )
		{
			desiredSkill = 5019;
			isAcceptable = this.isAcceptable( -6, -6 );
		}

		// Disco Face Stab
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Face Stab" ) )
		{
			desiredSkill = 5012;
			isAcceptable = this.isAcceptable( -7, -7 );
		}

		return desiredSkill == 0 ? "attack" : "skill" + desiredSkill;
	}

	private static boolean checkForInitiative( final String responseText )
	{
		if ( FightRequest.isAutomatingFight )
		{
			String action = Preferences.getString( "battleAction" );

			if ( action.startsWith( "custom" ) )
			{
				String file = Preferences.getBoolean( "debugPathnames" ) ? CombatActionManager.getStrategyLookupFile().getAbsolutePath() : CombatActionManager.getStrategyLookupName();
				action = file + " [" + CombatActionManager.getEncounterKey( MonsterStatusTracker.getLastMonsterName() ) + "]";
			}

			RequestLogger.printLine( "Strategy: " + action );
		}

		if ( FightRequest.lastUserId != KoLCharacter.getUserId() )
		{
			FightRequest.lastUserId = KoLCharacter.getUserId();
			FightRequest.lostInitiativeMessage = "Round 0: " + KoLCharacter.getUserName() + " loses initiative!";
			FightRequest.wonInitiativeMessage = "Round 0: " + KoLCharacter.getUserName() + " wins initiative!";
		}

		boolean shouldLogAction = Preferences.getBoolean( "logBattleAction" );

		// The response tells you if you won initiative.

		if ( !FightRequest.wonInitiative( responseText ) )
		{
			// If you lose initiative, there's nothing very
			// interesting to print to the session log.

			if ( shouldLogAction )
			{
				RequestLogger.printLine( FightRequest.lostInitiativeMessage );
				RequestLogger.updateSessionLog( FightRequest.lostInitiativeMessage );
			}

			return false;
		}

		// Now that you've won initiative, figure out what actually
		// happened in that first round based on player settings.

		if ( shouldLogAction )
		{
			RequestLogger.printLine( FightRequest.wonInitiativeMessage );
			RequestLogger.updateSessionLog( FightRequest.wonInitiativeMessage );
		}

		int autoAttackAction = KoLCharacter.getAutoAttackAction();

		// If no default action is made by the player, then the round
		// remains the same.  Simply report winning/losing initiative.
		// Same applies for autoattack macros, we detect their action elsewhere.

		if ( autoAttackAction == 0 || String.valueOf( autoAttackAction ).startsWith( "99" ) ||
                responseText.contains( "<!-- macroaction:" ) )
		{
			return false;
		}

		if ( autoAttackAction == 1 )
		{
			FightRequest.registerRequest( false, "fight.php?[AA]attack" );
		}
		else if ( autoAttackAction == 3 )
		{
			FightRequest.registerRequest( false, "fight.php?[AA]steal" );
		}
		else
		{
			FightRequest.registerRequest( false, "fight.php?[AA]whichskill=" + autoAttackAction );
		}

		return true;
	}

	public static final Pattern ONTURN_PATTERN = Pattern.compile( "onturn = (\\d+)" );

	public static void updateCombatData( final String location, String encounter, final String responseText )
	{
		FightRequest.lastResponseText = responseText;

		boolean autoAttacked = false;

		if ( FightRequest.currentRound == 0 )
		{
			int adventure = KoLAdventure.lastAdventureId();

			// Adventuring in the Haiku Dungeon
			// Currently have Haiku State of Mind
			// Acquiring Haiku State of Mind can happen in the middle of a macro
			// combat, so is detected elsewhere.
			FightRequest.haiku =
				adventure == AdventurePool.HAIKU_DUNGEON ||
				KoLConstants.activeEffects.contains( FightRequest.haikuEffect );

			// Adventuring in the Suburbs of Dis
			// Currently have Just the Best Anapests
			FightRequest.anapest =
				adventure == AdventurePool.CLUMSINESS_GROVE ||
				adventure == AdventurePool.MAELSTROM_OF_LOVERS ||
				adventure == AdventurePool.GLACIER_OF_JERKS ||
				KoLConstants.activeEffects.contains( FightRequest.anapestEffect );

			KoLCharacter.getFamiliar().recognizeCombatUse();

			FightRequest.haveFought = true;

			if ( responseText.contains( "There is a blinding flash of light, and a chorus of heavenly voices rises in counterpoint to the ominous organ music." ) )
			{
				FightRequest.transmogrifyNemesisWeapon( false );
			}

			if ( responseText.contains( "stomps in place restlessly" ) )
			{
				FightRequest.canStomp = true;
			}

			if ( responseText.contains( "hear a wolf whistle" ) )
			{
				KoLmafia.ignoreSpecialMonsters();
				Preferences.increment( "_romanticFightsLeft", -1 );
				TurnCounter.stopCounting( "Romantic Monster window begin" );
				TurnCounter.stopCounting( "Romantic Monster window end" );
				if ( Preferences.getInteger( "_romanticFightsLeft" ) > 0 )
				{
					TurnCounter.startCounting( 15, "Romantic Monster window begin loc=*", "lparen.gif" );
					TurnCounter.startCounting( 25, "Romantic Monster window end loc=*", "rparen.gif" );
				}
				else
				{
					Preferences.setString( "romanticTarget", "" );
				}
			}

			// Increment stinky cheese counter
			int stinkyCount = EquipmentManager.getStinkyCheeseLevel();
			if ( stinkyCount > 0 )
			{
				Preferences.increment( "_stinkyCheeseCount", stinkyCount );
			}

			// If this is the first round, then register the
			// opponent you are fighting against.

			encounter = ConsequenceManager.disambiguateMonster( encounter, responseText );

			if ( encounter.equalsIgnoreCase( "Ancient Protector Spirit" ) )
			{
				HiddenCityRequest.addHiddenCityLocation( 'P' );

			}
			else if ( encounter.equalsIgnoreCase( "giant octopus" ) )
			{
				if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.GRAPPLING_HOOK, 1 ) ) )
				{
					ResultProcessor.processItem( ItemPool.GRAPPLING_HOOK, -1 );
				}
			}
			else if ( !KoLmafia.ignoreSpecialMonsters &&
				  ( encounter.equalsIgnoreCase( "angry bassist" ) ||
				    encounter.equalsIgnoreCase( "blue-haired girl" ) ||
				    encounter.equalsIgnoreCase( "evil ex-girlfriend" ) ||
				    encounter.equalsIgnoreCase( "peeved roommate" ) ||
				    encounter.equalsIgnoreCase( "random scenester" ) ||
				    encounter.toLowerCase().startsWith( "black crayon" ) ) )
			{
				Preferences.increment( "_hipsterAdv", 1 );
			}
			else if ( !KoLmafia.ignoreSpecialMonsters &&
				  KoLCharacter.inBeecore() &&
				  ( encounter.equalsIgnoreCase( "beebee gunners" ) ||
				    encounter.equalsIgnoreCase( "moneybee" ) ||
				    encounter.equalsIgnoreCase( "mumblebee" ) ||
				    encounter.equalsIgnoreCase( "beebee queue" ) ||
				    encounter.equalsIgnoreCase( "bee swarm" ) ||
				    encounter.equalsIgnoreCase( "buzzerker" ) ||
				    encounter.equalsIgnoreCase( "beebee king" ) ||
				    encounter.equalsIgnoreCase( "bee thoven" ) ||
				    encounter.equalsIgnoreCase( "Queen Bee" ) ) )
			{
				Preferences.setInteger( "beeCounter", KoLCharacter.getCurrentRun() + 1 );
				TurnCounter.stopCounting( "Bee window begin" );
				TurnCounter.stopCounting( "Bee window end" );
				TurnCounter.startCounting( 15, "Bee window begin loc=*", "lparen.gif" );
				TurnCounter.startCounting( 20, "Bee window end loc=*", "rparen.gif" );
			}
			else if ( !KoLmafia.ignoreSpecialMonsters &&
				  ( encounter.equalsIgnoreCase( "Candied Yam Golem" ) ||
					encounter.equalsIgnoreCase( "Malevolent Tofurkey" ) ||
					encounter.equalsIgnoreCase( "Possessed Can of Cranberry Sauce" ) ||
					encounter.equalsIgnoreCase( "Stuffing Golem" ) ||
					encounter.equalsIgnoreCase( "Hammered Yam Golem" ) ||
					encounter.equalsIgnoreCase( "Inebriated Tofurkey" ) ||
					encounter.equalsIgnoreCase( "Plastered Can of Cranberry Sauce" ) ||
					encounter.equalsIgnoreCase( "Soused Stuffing Golem" ) ||
					encounter.equalsIgnoreCase( "El Novio Cad&aacute;ver" ) ||
					encounter.equalsIgnoreCase( "El Padre Cad&aacute;ver" ) ||
					encounter.equalsIgnoreCase( "La Novia Cad&aacute;ver" ) ||
					encounter.equalsIgnoreCase( "La Persona Inocente Cad&aacute;ver" ) ||
					encounter.equalsIgnoreCase( "Four-shadowed mime" ) ) )
			{
				TurnCounter.stopCounting( "Holiday Monster window begin" );
				TurnCounter.stopCounting( "Holiday Monster window end" );
				TurnCounter.startCounting( 25, "Holiday Monster window begin loc=*", "lparen.gif" );
				TurnCounter.startCounting( 35, "Holiday Monster window end loc=*", "rparen.gif" );
			}

			MonsterStatusTracker.setNextMonsterName( CombatActionManager.encounterKey( encounter ) );

			FightRequest.isTrackingFights = false;
			FightRequest.waitingForSpecial = false;
			for ( int i = 0; i < 10; ++i )
			{
				if ( CombatActionManager.getShortCombatOptionName(
					CombatActionManager.getCombatAction(
						MonsterStatusTracker.getLastMonsterName(), i, false ) ).equals( "special" ) )
				{
					FightRequest.waitingForSpecial = true;
					break;
				}
			}

			autoAttacked = FightRequest.checkForInitiative( responseText );
			KoLmafia.ignoreSpecialMonsters = false;
		}

		// Figure out various things by examining the responseText. Ideally,
		// these could be done while walking the HTML parse tree.

		FightRequest.parseBangPotion( responseText );
		FightRequest.parseStoneSphere( responseText );
		FightRequest.parsePirateInsult( responseText );
		FightRequest.parseGrubUsage( location, responseText );
		FightRequest.parseGhostSummoning( location, responseText );
		FightRequest.parseFlyerUsage( location, responseText );

		Matcher macroMatcher = FightRequest.MACRO_PATTERN.matcher( responseText );
		if ( macroMatcher.find() )
		{
			FightRequest.registerMacroAction( macroMatcher );
		}
		else	// no macro results
		{	// replace with dummy matcher that matches the full page
			macroMatcher = FightRequest.FULLPAGE_PATTERN.matcher( responseText );
			macroMatcher.find();
		}

		// We've started a new round
		++FightRequest.currentRound;

		// Sanity check: compare our round with what KoL claims it is
		Matcher m = ONTURN_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			int round = StringUtilities.parseInt( m.group(1) );
			if ( round != FightRequest.currentRound )
			{
				RequestLogger.printLine( "KoLmafia thinks it is round " + FightRequest.currentRound + " but KoL thinks it is round " + round );
			}
		}

		// *** This doesn't seem right, but is currently necessary for
		// *** CCS scripts to behave correctly. FIX
		if ( autoAttacked )
		{
			++FightRequest.preparatoryRounds;
			++FightRequest.currentRound;
		}

		// Assume this response does not warrant a refresh
		FightRequest.shouldRefresh = false;

		// Preprocess results and register new items
		ResultProcessor.registerNewItems( responseText );

		// Clean HTML and process it
		FightRequest.processNormalResults( responseText, macroMatcher );

		// Perform other processing for the final round
		FightRequest.updateRoundData( macroMatcher );

		if ( responseText.contains( "Macro Abort" ) ||
                responseText.contains( "Macro abort" ) ||
                responseText.contains( "macro abort" ) ||
                responseText.contains( "Could not match item(s) for use" ) )
		{
			FightRequest.nextAction = "abort";
		}

		FightRequest.foundNextRound = true;
	}

	// This performs checks that have to be applied to a single round of
	// combat results, and that aren't (yet) part of the
	// processNormalResults loop.  responseText will be a fragment of the
	// page; anything that needs to check something outside of the round
	// (such as looking at the action menus to see if an item or skill is
	// still available) should use FightRequest.lastResponseText instead.
	private static void updateRoundData( final Matcher macroMatcher )
	{
		String responseText;
		try
		{
			responseText = macroMatcher.group();
		}
		catch ( IllegalStateException e )
		{	// page structure is botched - should have already been reported
			return;
		}
		boolean finalRound = macroMatcher.end() ==
			FightRequest.lastResponseText.length();

		// Look for special effects
		FightRequest.updateMonsterHealth( responseText );

		// Spend MP and consume items
		FightRequest.payActionCost( responseText );

		// Track disco skill sequences
		DiscoCombatHelper.parseFightRound( FightRequest.nextAction, responseText );

		// Check for equipment breakage.

		if ( responseText.contains( "Your antique helmet, weakened" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_HELMET,
				"Your antique helmet broke." );
		}

		if ( responseText.contains( "sunders your antique spear" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_SPEAR,
				"Your antique spear broke." );
		}

		if ( responseText.contains( "Your antique shield, weakened" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_SHIELD,
				"Your antique shield broke." );
		}

		if ( responseText.contains( "Your antique greaves, weakened" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_GREAVES,
				"Your antique greaves broke." );
		}

		// You try to unlock your cyber-mattock, but the battery's
		// dead.  Since the charger won't be invented for several
		// hundred years, you chuck the useless hunk of plastic as far
		// from you as you can.

		if ( responseText.contains( "You try to unlock your cyber-mattock" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.CYBER_MATTOCK,
				"Your cyber-mattock broke." );
		}

		// "You sigh and discard the belt in a nearby trash can."
		if ( responseText.contains( "You sigh and discard the belt" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.CHEAP_STUDDED_BELT,
				"Your cheap studded belt broke." );
		}

		// "The adhesive on the fake piercing comes loose and it falls
		// off. Looks like those things weren't meant to withstand as
		// much sweat as your eyebrow is capable of producing."
		if ( responseText.contains( "The adhesive on the fake piercing comes loose" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.STICK_ON_EYEBROW_PIERCING,
				"Your stick-on eyebrow piercing broke." );
		}

		if ( responseText.contains( "Your sugar chapeau slides" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_CHAPEAU,
				"Your sugar chapeau shattered." );
		}

		if ( responseText.contains( "your sugar shank handle" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHANK,
				"Your sugar shank shattered." );
		}

		if ( responseText.contains( "drop something as sticky as the sugar shield" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHIELD,
				"Your sugar shield shattered." );
		}

		if ( responseText.contains( "Your sugar shillelagh absorbs the shock" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHILLELAGH,
				"Your sugar shillelagh shattered." );
		}

		if ( responseText.contains( "Your sugar shirt falls apart" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHIRT,
				"Your sugar shirt shattered." );
		}

		if ( responseText.contains( "Your sugar shotgun falls apart" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHOTGUN,
				"Your sugar shotgun shattered." );
		}

		if ( responseText.contains( "Your sugar shorts crack" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHORTS,
				"Your sugar shorts shattered." );
		}

		// "The Slime draws back and shudders, as if it's about to sneeze.
		// Then it blasts you with a massive loogie that sticks to your
		// rusty grave robbing shovel, pulls it off of you, and absorbs
		// it back into the mass."

		Matcher m = FightRequest.SLIMED_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			int id = ItemDatabase.getItemId( m.group( 1 ) );
			if ( id > 0 )
			{
				EquipmentManager.discardEquipment( id );
				KoLmafia.updateDisplay( MafiaState.PENDING, "Your " +
					m.group( 1 ) + " got slimed." );
			}
		}

		if ( responseText.contains( "Axel screams, and lets go of your hand" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.SPOOKY_LITTLE_GIRL );
			KoLmafia.updateDisplay( MafiaState.PENDING, "Your Spooky little girl ran off." );
		}

		// "[slimeling] leaps on your opponent, sliming it for XX damage.  It's inspiring!"
		if ( responseText.contains( "leaps on your opponent" ) )
		{
			float fullness = Math.max( Preferences.getFloat( "slimelingFullness" ) - 1.0F, 0.0F );
			Preferences.setFloat("slimelingFullness", fullness );
		}

		// "As you're trying to get away, you sink in the silty muck on
		// the sea floor. You manage to get yourself unmired, but your
		// greaves seem to have gotten instantly rusty in the process..."
		if ( responseText.contains( "have gotten instantly rusty" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.ANTIQUE_GREAVES );
			KoLmafia.updateDisplay( MafiaState.PENDING, "Your antique greaves got rusted." );
		}

		// Check for familiar actions

		if ( responseText.contains( "shimmers briefly, and you feel it getting earlier." ) )
		{
			Preferences.increment( "_riftletAdv", 1 );
		}

		if ( responseText.contains( "sees that you're about to get attacked and trips it before it can attack you." )
			|| responseText.contains( "does the Time Warp, then does the Time Warp again. Clearly, madness has taken its toll on him." )
			|| responseText.contains( "The air shimmers around you." ) )
		{
			Preferences.increment( "_timeHelmetAdv", 1 );
		}

		if ( responseText.contains( "into last week. It saves you some time, because you already beat" ) )
		{
			Preferences.increment( "_vmaskAdv", 1 );
		}

		if ( responseText.contains( "stomps your opponent into paste" )
			|| responseText.contains( "stomps your opponents into paste" )
			|| responseText.contains( "shuffles its heels, gets a running start, then leaps on" ) )
		{
			Preferences.setBoolean( "bootsCharged", false );
		}
		// <name> rubs its soles together, then stomps in place
		// restlessly. Clearly, the violence it's done so far is
		// only making it ache for some quality stomping.
		if ( responseText.contains( "making it ache for some quality stomping" ) )
		{
			Preferences.setBoolean( "bootsCharged", true );
		}

		int blindIndex = responseText.indexOf( "... something.</div>" );
		while ( blindIndex != -1 )
		{
			RequestLogger.printLine( "You acquire... something." );
			if ( Preferences.getBoolean( "logAcquiredItems" ) )
			{
				RequestLogger.updateSessionLog( "You acquire... something." );
			}

			blindIndex = responseText.indexOf( "... something.</div>", blindIndex + 1 );
		}

		switch ( KoLAdventure.lastAdventureId() )
		{
		case AdventurePool.JUNKYARD_BARREL:
		case AdventurePool.JUNKYARD_REFRIGERATOR:
		case AdventurePool.JUNKYARD_TIRES:
		case AdventurePool.JUNKYARD_CAR:
			// Quest gremlins might have a tool.
			IslandDecorator.handleGremlin( responseText );
			break;

		case AdventurePool.FRAT_UNIFORM_BATTLEFIELD:
		case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD:
			IslandDecorator.handleBattlefield( responseText );
			break;

		case AdventurePool.HOBOPOLIS_TOWN_SQUARE:
			HobopolisDecorator.handleTownSquare( responseText );
			break;
		}

		// Reset round information if the battle is complete.
		if ( !finalRound )
		{
			return;
		}

		boolean won = responseText.contains( "<!--WINWINWIN-->" );

		if ( won )
		{
			KoLCharacter.getFamiliar().addCombatExperience( responseText );
		}

		// If we won, the fight is over for sure. It might be over
		// anyway. We can detect this in one of two ways: if you have
		// the CAB enabled, there will be no link to the old combat
		// form. Otherwise, a link to fight.php indicates that the
		// fight is continuing

		if ( !won &&
                responseText.contains( Preferences.getBoolean( "serverAddsCustomCombat" ) ?
                        "(show old combat form)" :
                        "fight.php" ) )
		{
			return;
		}

		// The turtle blinks at you with gratitude for freeing it from
		// its brainwashing, and trudges off over the horizon.
		// ...Eventually.
		if ( responseText.contains( "freeing it from its brainwashing" ) )
		{
			int free = Preferences.increment( "guardTurtlesFreed" );
			String message = "Freed guard turtle #" + free;
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		if ( responseText.contains( "your Epic Weapon reverts to its original form in a puff of failure" ) )
		{
			FightRequest.transmogrifyNemesisWeapon( true );
		}

		// Check for bounty item not dropping from a monster
		// that is known to drop the item.

		int bountyItemId = Preferences.getInteger( "currentBountyItem" );
		if ( MonsterStatusTracker.dropsItem( bountyItemId ) )
		{
			AdventureResult bountyItem = ItemPool.get( bountyItemId, 1 );
			String bountyItemName = bountyItem.getName();

			if ( !responseText.contains( bountyItemName ) && !problemFamiliar() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "Bounty item failed to drop from expected monster." );
			}
		}

		// Check for GMoB defeat
		if ( responseText.contains( "guy made of bee pollen" ) )
		{
			// Record that we beat the guy made of bees.
			Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
		}

		// Check for runaways. Only a free runaway decreases chance
		if ( responseText.contains( "shimmers as you quickly float away" ) ||
                responseText.contains( "your pants suddenly activate" ) )
		{
			Preferences.increment( "_navelRunaways", 1 );
		}

		if ( responseText.contains( "his back, and flooms away" ) ||
                responseText.contains( "speed your escape.  Thanks" ) )
		{
			Preferences.increment( "_banderRunaways", 1 );
		}

		// Vivala mask skill Creepy Grin used
		if ( responseText.contains( "an even creepier grin" ) )
		{
			Preferences.setBoolean( "_vmaskBanisherUsed", true );
		}

		// stinky cheese eye Give Your Opponent the Stinkeye skill used
		if ( responseText.contains( "You fix an extremely disdainful eye" ) )
		{
			Preferences.setBoolean( "_stinkyCheeseBanisherUsed", true );
		}

		// The first part is for a hobo underling being summoned
		// The second part is from using a dinged-up triangle to summon it
		if ( responseText.contains( "A hobo runs up to you" ) &&
			 !responseText.contains( "You give the triangle a vigorous ringing." ) )
		{
			Preferences.increment( "_hoboUnderlingSummons", 1 );
		}

		if ( responseText.contains( "Use of undefined constant itemprocess" ) )
		{
			Preferences.setBoolean( "_softwareGlitchTurnReceived", true );
		}

		// Increment Organ Grinder combat counter
		if ( responseText.contains( "picking a few choice bits to put in his grinder" ) ||
                responseText.contains( "picks some organs out of your opponent and tosses them into his grinder." ) ||
                responseText.contains( "squealing something about burning his Longers and Lingers." ) ||
                responseText.contains( "chattering about how the upper story on his Gregory is standing up." ) ||
                responseText.contains( "My Hampton has a funny feeling about this one, mate" ) ||
                responseText.contains( "shivers as he rummages for grindable organs" ) ||
                responseText.contains( "squelches around in its body for some grinder fodder" ) ||
                responseText.contains( "and harvests a few choice bits for his grinder." ) )
		{
			Preferences.increment( "_piePartsCount", 1 );
		}

		// Check for worn-out stickers
		int count = 0;
		m = WORN_STICKER_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			++count;
		}
		if ( count > 0 )
		{
			KoLmafia.updateDisplay( (count == 1 ? "A sticker" : count + " stickers") +
				" fell off your weapon." );
			EquipmentManager.stickersExpired( count );
		}

		// Check for ballroom song hint
		m = BALLROOM_SONG_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			Preferences.setInteger( "lastQuartetAscension", KoLCharacter.getAscensions() );
			Preferences.setInteger( "lastQuartetRequest", m.start( 1 ) != -1 ? 1 :
				m.start( 2 ) != -1 ? 2 : 3 );
		}

		// Check for special familiar actions
		FamiliarData familiar = KoLCharacter.getEffectiveFamiliar();
		switch ( familiar.getId() )
		{
		case FamiliarPool.HARE:
			// <name> pulls an oversized pocketwatch out of his
			// waistcoat and winds it. "Two days slow, that's what
			// it is," he says.
			if ( responseText.contains( "oversized pocketwatch" ) )
			{
				Preferences.increment( "extraRolloverAdventures", 1 );
				Preferences.increment( "_hareAdv", 1 );
			}

			// The dormouse emerges groggily from <names>'s
			// waistcoat and gives the watch another turn. He
			// vanishes back into the pocket with a sleepy 'feed
			// your head.'
			break;

		case FamiliarPool.GIBBERER:
			// <name> mutters dark secrets under his breath, and
			// you feel time slow down.
			if ( responseText.contains( "you feel time slow down" ) )
			{
				Preferences.increment( "extraRolloverAdventures", 1 );
				Preferences.increment( "_gibbererAdv", 1 );
			}
			break;

		case FamiliarPool.STOCKING_MIMIC:
			// <name> reaches deep inside himself and pulls out a
			// big bag of candy. Cool!
			if ( responseText.contains( "pulls out a big bag of candy" ) )
			{
				AdventureResult item = ItemPool.get( ItemPool.BAG_OF_MANY_CONFECTIONS, 1 );
				// The Stocking Mimic will do this once a day
				Preferences.setBoolean( "_bagOfCandy", true );
				// Add bag of many confections to inventory
				ResultProcessor.processItem( ItemPool.BAG_OF_MANY_CONFECTIONS, 1 );
				// Equip familiar with it
				familiar.setItem( item );
			}

			// <name> gorges himself on candy from his bag.
			if ( responseText.contains( "gorges himself on candy from his bag" ) )
			{
				familiar.addNonCombatExperience( 1 );
			}
			break;

		case FamiliarPool.HIPSTER:
			//  The words POWER UP appear above <name>'s head as he
			//  instantly grows a stupid-looking moustache.
			if ( responseText.contains( "instantly grows a stupid-looking moustache" ) )
			{
				AdventureResult item = ItemPool.get( ItemPool.IRONIC_MOUSTACHE, 1 );
				// The Mini-Hipster will do this once a day
				Preferences.setBoolean( "_ironicMoustache", true );
				// Add ironic moustache to inventory
				ResultProcessor.processItem( ItemPool.IRONIC_MOUSTACHE, 1 );
				// Equip familiar with it
				familiar.setItem( item );
			}
			break;
		}

		// Check for weapon-specific cases
		if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.LEAFBLOWER, 1 ) ) )
		{
			Preferences.setInteger( "_leafblowerML", Math.min( 25, 1 +
				Preferences.getInteger( "_leafblowerML" ) ) );
		}

		if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.HATSEAT, 1 ) ) &&
                responseText.contains( "gains 1 Experience" ) )
		{
			KoLCharacter.getEnthroned().addNonCombatExperience( 1 );
		}

		// Cancel any combat modifiers
		Modifiers.overrideModifier( "fightMods", null );

		if ( won )
		{
			String monster = MonsterStatusTracker.getLastMonsterName();

			if ( monster.equalsIgnoreCase( "Black Pudding" ) )
			{
				Preferences.increment( "blackPuddingsDefeated", 1 );
			}
			else if ( monster.equalsIgnoreCase( "Wu Tang the Betrayer" ) )
			{
				Preferences.setInteger( "lastWuTangDefeated", KoLCharacter.getAscensions() );
			}
			else if ( monster.equalsIgnoreCase( "Ancient Protector Spirit" ) )
			{
				HiddenCityRequest.addHiddenCityLocation( 'D' );
			}
			else if ( monster.equalsIgnoreCase( "Baron Von Ratsworth" ) )
			{
				TavernRequest.addTavernLocation( '6' );
			}
			else if ( monster.equalsIgnoreCase( "Wumpus" ) )
			{
				WumpusManager.reset();
			}
			else if ( monster.equalsIgnoreCase( "Dirty Thieving Brigand" ) )
			{
				// "Well," you say, "it would really help the war effort if
				// your convent could serve as a hospital for our wounded
				// troops."
				if ( responseText.contains( "could serve as a hospital" ) )
				{
					Preferences.setString( "sidequestNunsCompleted", "hippy" );
				}
				else if ( responseText.contains( "could serve as a massage parlor" ) )
				{
					Preferences.setString( "sidequestNunsCompleted", "fratboy" );
				}
			}
			else if ( !FightRequest.castCleesh &&
				Preferences.getString( "lastAdventure" ).equalsIgnoreCase(
					"A Maze of Sewer Tunnels" ) )
			{
				AdventureResult result = AdventureResult.tallyItem(
					"sewer tunnel explorations", false );
				AdventureResult.addResultToList( KoLConstants.tally, result );
			}

			// Give your summoned combat entity some experience
			if ( FightRequest.summonedGhost )
			{
				// The Angel Hair Wisp can leave the battle
				// before you win. We'll check if the summoned
				// entity is still present by looking for its
				// image.

				int guardian = KoLCharacter.findGuardianByImage( responseText );
				if ( guardian != -1 )
				{
					// Legendary Regalia of the Pasta Master
					int exp = EquipmentManager.isWearingOutfit( 66 ) ? 2 : 1;
					Preferences.increment( "pastamancerGhostExperience", exp );
				}
			}
		}

		// "You pull out your personal massager and use it to work the
		// kinks out of your neck and your back. You stop there,
		// though, as nothing below that point is feeling particularly
		// kinky. Unfortunately, it looks like the batteries in the
		// thing were only good for that one use."

		if ( responseText.contains( "You pull out your personal massager" ) )
		{
			ResultProcessor.processItem( ItemPool.PERSONAL_MASSAGER, -1 );
			KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
		}

		FightRequest.clearInstanceData();
		FightRequest.inMultiFight = won &&
			FightRequest.MULTIFIGHT_PATTERN.matcher( responseText ).find();
	}

	public static String getSpecialAction()
	{
		ArrayList<String> items = new ArrayList<String>();

		boolean haveSkill, haveItem;
		String pref = Preferences.getString( "autoOlfact" );
		if ( !pref.equals( "" ) && !KoLConstants.activeEffects.contains( EffectPool.get( Effect.ON_THE_TRAIL ) ) )
		{
			haveSkill = KoLCharacter.hasSkill( "Transcendent Olfaction" ) &&
				( Preferences.getBoolean( "autoManaRestore" ) || KoLCharacter.getCurrentMP() >= SkillDatabase.getMPConsumptionById( SkillDatabase.OLFACTION ) );
			haveItem = KoLConstants.inventory.contains( FightRequest.EXTRACTOR );
			if ( (haveSkill | haveItem) && shouldTag( pref, "autoOlfact triggered" ) )
			{
				if ( haveSkill )
				{
					return OLFACTION_ACTION;
				}

				items.add( String.valueOf( ItemPool.ODOR_EXTRACTOR ) );
			}
		}

		pref = Preferences.getString( "autoPutty" );
		if ( !pref.equals( "" ) )
		{
			int totalCopies = Preferences.getInteger( "spookyPuttyCopiesMade" ) + Preferences.getInteger( "_raindohCopiesMade" );
			haveItem = KoLConstants.inventory.contains( FightRequest.PUTTY_SHEET ) &&
				Preferences.getInteger( "spookyPuttyCopiesMade" ) < 5 && totalCopies < 6;
			boolean haveItem2 = KoLConstants.inventory.contains( FightRequest.RAINDOH_BOX ) &&
				Preferences.getInteger( "_raindohCopiesMade" ) < 5 && totalCopies < 6;
			boolean haveItem3 = KoLConstants.inventory.contains( FightRequest.CAMERA ) &&
				!KoLConstants.inventory.contains( FightRequest.SHAKING_CAMERA );
			boolean haveItem4 = KoLConstants.inventory.contains( FightRequest.PHOTOCOPIER ) &&
				!KoLConstants.inventory.contains( FightRequest.PHOTOCOPIED_MONSTER );
			if ( (haveItem || haveItem2 || haveItem3 || haveItem4 ) && shouldTag( pref, "autoPutty triggered" ) )
			{
				if ( haveItem )
				{
					items.add( String.valueOf( ItemPool.SPOOKY_PUTTY_SHEET ) );
				}
				else if ( haveItem2 )
				{
					items.add( String.valueOf( ItemPool.RAIN_DOH_BOX ) );
				}
				else if ( haveItem3 )
				{
					items.add( String.valueOf( ItemPool.CAMERA ) );
				}
				else
				{
					items.add( String.valueOf( ItemPool.PHOTOCOPIER ) );
				}
			}
		}

		if ( Preferences.getBoolean( "autoSphereID" ) )
		{
			ItemPool.suggestIdentify( items, 2174, 2177, "lastStoneSphere" );
		}
		if ( Preferences.getBoolean( "autoPotionID" ) )
		{
			ItemPool.suggestIdentify( items, 819, 827, "lastBangPotion" );
		}

		int itemsSize = items.size();
		boolean haveFunkslinging = KoLCharacter.hasSkill( "Ambidextrous Funkslinging" );
		if ( itemsSize == 0 )
		{
			return null;
		}
		else if ( itemsSize == 1 || !haveFunkslinging )
		{
			return (String) items.get( 0 );
		}
		else
		{
			return (String) items.get( 0 ) + "," + (String) items.get( 1 );
		}
	}

	private static boolean shouldTag( String pref, String msg )
	{
		boolean isAbort = false, isMonster = false, rv;
		List items = null;

		if ( pref.endsWith( " abort" ) )
		{
			isAbort = true;
			pref = pref.substring( 0, pref.length() - 6 ).trim();
		}

		if ( pref.equals( "goals" ) )
		{
			items = GoalManager.getGoals();
		}
		else if ( pref.startsWith( "monster " ) )
		{
			isMonster = true;
			pref = pref.substring( 8 ).trim();
		}
		else {
			if ( pref.startsWith( "item " ) )
			{
				pref = pref.substring( 5 );
			}
			Object[] temp = ItemFinder.getMatchingItemList(
				KoLConstants.inventory, pref );
			if ( temp == null )
			{
				return false;
			}
			items = Arrays.asList( temp );
		}

		if ( isMonster )
		{
			rv = MonsterStatusTracker.getLastMonsterName().contains( pref );
		}
		else
		{
			rv = MonsterStatusTracker.dropsItems( items );
		}

		if ( rv && isAbort )
		{
			KoLmafia.abortAfter( msg );
		}
		return rv;
	}

	private static void transmogrifyNemesisWeapon( boolean reverse )
	{
		for ( int i = 0; i < FightRequest.NEMESIS_WEAPONS.length; ++i )
		{
			Object[] data = FightRequest.NEMESIS_WEAPONS[ i ];
			if ( KoLCharacter.getClassType().equals( data[ 0 ] ) )
			{
				EquipmentManager.transformEquipment(
					(AdventureResult) data[ reverse ? 2 : 1 ],
					(AdventureResult) data[ reverse ? 1 : 2 ] );
				return;
			}
		}
	}

	private static final Pattern BANG_POTION_PATTERN =
		Pattern.compile( "You throw the (.*?) potion at your opponent.?.  It shatters against .*?[,\\.] (.*?)\\." );

	private static void parseBangPotion( final String responseText )
	{
		if ( FightRequest.anapest )
		{
			return;
		}

		Matcher bangMatcher = FightRequest.BANG_POTION_PATTERN.matcher( responseText );
		while ( bangMatcher.find() )
		{
			int potionId = ItemDatabase.getItemId( bangMatcher.group( 1 ) + " potion" );

			String effectText = bangMatcher.group( 2 );
			String[][] strings = ItemPool.bangPotionStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( effectText.contains( strings[i][1] ) )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						potionId,
						819, 827,
						"lastBangPotion", " of " ) )
					{
						KoLmafia.updateDisplay( "All bang potions have been identified!" );
					}
					break;
				}
			}
		}
	}

	// You hold the rough stone sphere up in the air.
	private static final Pattern STONE_SPHERE_PATTERN =
		Pattern.compile( "You hold the (.*?) stone sphere up in the air.*?It radiates a (.*?)," );

	private static void parseStoneSphere( final String responseText )
	{
		if ( FightRequest.anapest )
		{
			return;
		}

		Matcher sphereMatcher = FightRequest.STONE_SPHERE_PATTERN.matcher( responseText );
		while ( sphereMatcher.find() )
		{
			int sphereId = ItemDatabase.getItemId( sphereMatcher.group( 1 ) + " stone sphere" );

			if ( sphereId == -1 )
			{
				continue;
			}

			String effectText = sphereMatcher.group( 2 );
			String[][] strings = ItemPool.stoneSphereStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( effectText.contains( strings[i][1] ) )
				{
					FightRequest.identifyStoneSphere( i, sphereId );
					break;
				}
			}
		}
	}

	private static void identifyStoneSphere( final int sphere, final int sphereId )
	{
		if ( ItemPool.eliminationProcessor( ItemPool.stoneSphereStrings, sphere, sphereId, 2174, 2177, "lastStoneSphere", " of " ) )
		{
			KoLmafia.updateDisplay( "All stone spheres have been identified!" );
		}
	}

	private static boolean isStoneSphere( final int sphereId )
	{
		return sphereId >= 2174 && sphereId <= 2177;
	}

	public static String stoneSphereEffectToId( final String effect )
	{
		for ( int i = 2174; i <= 2177; ++i )
		{
			String itemId = String.valueOf( i );
			String value = Preferences.getString( "lastStoneSphere" + itemId );

			if ( value.equals( "plants" ) )
			{
				value = "nature";
			}

			if ( effect.equals( value ) )
			{
				return itemId;
			}
		}

		return null;
	}

	// The pirate sneers at you and replies &quot;<insult>&quot;

	private static final Pattern PIRATE_INSULT_PATTERN =
		Pattern.compile( "The pirate sneers \\w+ you and replies &quot;(.*?)&quot;" );

	private static void parsePirateInsult( final String responseText )
	{
		if ( FightRequest.anapest )
		{
			return;
		}

		Matcher insultMatcher = FightRequest.PIRATE_INSULT_PATTERN.matcher( responseText );

		if ( !insultMatcher.find() )
		{
			return;
		}

		int insult = BeerPongRequest.findPirateRetort( insultMatcher.group( 1 ) );
		if ( insult <= 0 )
		{
			return;
		}

		KoLCharacter.ensureUpdatedPirateInsults();
		if ( !Preferences.getBoolean( "lastPirateInsult" + insult ) )
		{	// it's a new one
			Preferences.setBoolean( "lastPirateInsult" + insult, true );
			AdventureResult result = AdventureResult.tallyItem( "pirate insult", false );
			AdventureResult.addResultToList( KoLConstants.tally, result );
			GoalManager.updateProgress( result );
			int count = BeerPongRequest.countPirateInsults();
			float odds = BeerPongRequest.pirateInsultOdds( count ) * 100.0f;
			RequestLogger.printLine( "Pirate insults known: " +
						 count + " (" + KoLConstants.FLOAT_FORMAT.format( odds ) +
						 "%)" );
		}
	}

	private static void parseGrubUsage( final String location, final String responseText )
	{
		// URL no longer a meaningful test of whether a grub was used
		if ( false )	//location.indexOf( "7074" ) == -1 )
		{
			return;
		}

		// You concentrate on one of the burrowgrubs digging its way
		// through your body, and absorb it into your bloodstream.
		// It's refreshingly disgusting!

		int pos = responseText.indexOf( "refreshingly disgusting" );
		if ( pos != -1 )
		{
			int uses = Preferences.getInteger( "burrowgrubSummonsRemaining" ) - 1;

			while ( (pos = responseText.indexOf( "refreshingly disgusting", pos + 23 )) != -1 )
			{
				--uses;
			}

			// We have used our burrowgrub hive today
			Preferences.setBoolean( "burrowgrubHiveUsed", true );

			// <option value="7074" picurl="nopic" selected>Consume
			// Burrowgrub (0 Mojo Points)</option>

			if ( !responseText.contains( "option value=\"7074\"" ) )
			{
				// We can't actually conclude anything from the lack of an
				// option to consume another one - it's possible that the
				// combat finished with no further user input.
				uses = Math.max( 0, uses );
			}
			else
			{	// At least one more use today

				uses = Math.max( 1, uses );
			}

			Preferences.setInteger( "burrowgrubSummonsRemaining", uses );
		}
	}

	private static void parseFlyerUsage( final String location, final String responseText )
	{
		// URL check not working for macro'ed flyering
		if ( false )	//location.indexOf( "240" ) == -1 )
		{	// jam band flyers=2404, rock band flyers=2405
			return;
		}

		// You slap a flyer up on your opponent. It enrages it.

		if ( responseText.contains( "You slap a flyer" ) )
		{
			int ML = Math.max( 0, MonsterStatusTracker.getMonsterBaseAttack() );
			Preferences.increment( "flyeredML", ML );
			AdventureResult result = AdventureResult.tallyItem( "Arena flyer ML", ML, false );
			AdventureResult.addResultToList( KoLConstants.tally, result );
			GoalManager.updateProgress( result );
		}
	}

	private static void parseGhostSummoning( final String location, final String responseText )
	{
		// URL check not working for macro'ed summoning
		if ( false )	//location.indexOf( "summon" ) == -1 )
		{
			return;
		}

		String name = null;
		String type = null;

		KoLCharacter.ensureUpdatedPastaGuardians();
		for ( int i = 0; i < KoLCharacter.PASTA_GUARDIANS.length; ++ i )
		{
			Object [] entity = KoLCharacter.PASTA_GUARDIANS[i];
			Pattern pattern = (Pattern)entity[3];
			Matcher matcher = pattern.matcher( responseText );
			if ( matcher.find() )
			{
				name = matcher.group(1);
				type = (String)entity[0];
				break;
			}
		}

		if ( name == null )
		{
			return;
		}

		FightRequest.summonedGhost = true;

		// If we have a new Pasta Guardian, reset type and experience
		if ( !type.equals( Preferences.getString( "pastamancerGhostType" ) ) )
		{
			Preferences.setString( "pastamancerGhostType", type );
			Preferences.setInteger( "pastamancerGhostExperience", 0 );
		}

		// Spaghetti Elementals change their name as they grow
		if ( !name.equals( Preferences.getString( "pastamancerGhostName" ) ) )
		{
			Preferences.setString( "pastamancerGhostName", name );
		}

		int uses = Preferences.getInteger( "pastamancerGhostSummons" );
		int limit = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPAGHETTI_BANDOLIER, 1 ) ) ? 15 : 10;

		// You are mentally exhausted by the effort of summoning <name>.
		if ( responseText.contains( "You are mentally exhausted" ) )
		{
			uses = limit;
		}

		// Your brain feels tired.
		else if ( responseText.contains( "Your brain feels tired" ) && uses < limit - 2 )
		{
			uses = limit - 2;
		}
		else
		{
			++uses;
		}

		Preferences.setInteger( "pastamancerGhostSummons", uses );
	}

	public static void parseCombatItems( String responseText )
	{
		// The current round will be zero when the fight is over.
		// If you run with the WOWbar, the combat item dropdown will
		// still be on the page. Don't look at it.
		if ( FightRequest.currentRound < 1 )
		{
			return;
		}

		int startIndex = responseText.indexOf( "<select name=whichitem>" );
		if ( startIndex == -1 )
		{
			return;
		}
		int endIndex = responseText.indexOf( "</select>", startIndex );
		if ( endIndex == -1 )
		{
			return;
		}
		Matcher m = FightRequest.COMBATITEM_PATTERN.matcher( responseText.substring( startIndex, endIndex ) );
		while ( m.find() )
		{
			int itemId = StringUtilities.parseInt( m.group( 1 ) );
			if ( itemId <= 0 )
			{
				continue;
			}

			// KoL has a bug: if you initiate combat by using a
			// d10, the number of d10s in the combat item dropdown
			// will be incorrect. Therefore, don't believe it.
			if ( itemId == ItemPool.D10 )
			{
				continue;
			}

			int actualQty = StringUtilities.parseInt( m.group( 2 ) );
			AdventureResult ar = ItemPool.get( itemId, 1 );
			int currentQty = ar.getCount( KoLConstants.inventory );
			if ( actualQty != currentQty )
			{
				ar = ar.getInstance( actualQty - currentQty );
				ResultProcessor.processResult( ar );
				RequestLogger.updateSessionLog( "Adjusted combat item count: " + ar );
			}
		}
	}

	public static void parseConditionalCombatSkills( String responseText )
	{
		// The current round will be zero when the fight is over.
		// If you run with the WOWbar, the skills dropdown will
		// still be on the page. Don't look at it.
		if ( FightRequest.currentRound < 1 )
		{
			return;
		}

		int startIndex = responseText.indexOf( "<select name=whichskill>" );
		if ( startIndex == -1 )
		{
			return;
		}
		int endIndex = responseText.indexOf( "</select>", startIndex );
		if ( endIndex == -1 )
		{
			return;
		}

		KoLConstants.availableConditionalSkills.clear();
		KoLConstants.availableConditionalSkillsMap.clear();

		Matcher m = FightRequest.CONDITIONAL_COMBATSKILL_PATTERN.matcher( responseText.substring( startIndex, endIndex ) );
		while ( m.find() )
		{
			int skillId = StringUtilities.parseInt( m.group( 1 ) );
			KoLCharacter.addAvailableConditionalSkill( SkillDatabase.getSkillName( skillId ) );
		}
	}

	private static void getRound( final StringBuffer action )
	{
		action.setLength( 0 );
		if ( FightRequest.currentRound == 0 )
		{
			action.append( "After Battle: " );
		}
		else
		{
			action.append( "Round " );
			action.append( FightRequest.currentRound );
			action.append( ": " );
		}
	}

	private static void updateMonsterHealth( final String responseText )
	{
		StringBuffer action = new StringBuffer();

		Matcher m = FightRequest.NS_ML_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			MonsterStatusTracker.resetAttackAndDefense();
			if ( Preferences.getBoolean( "logMonsterHealth" ) )
			{
				action.append( MonsterStatusTracker.getLastMonsterName() );
				action.append( " resets her attack power and defense modifiers!" );
			}
		}

		if ( !Preferences.getBoolean( "logMonsterHealth" ) )
		{
			return;
		}

		Matcher detectiveMatcher = FightRequest.DETECTIVE_PATTERN.matcher( responseText );
		if ( detectiveMatcher.find() )
		{
			FightRequest.getRound( action );
			action.append( MonsterStatusTracker.getLastMonsterName() );
			action.append( " shows detective skull health estimate of " );
			action.append( detectiveMatcher.group( 1 ) );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		Matcher helmetMatcher = FightRequest.SPACE_HELMET_PATTERN.matcher( responseText );
		if ( helmetMatcher.find() )
		{
			FightRequest.getRound( action );
			action.append( MonsterStatusTracker.getLastMonsterName() );
			action.append( " shows toy space helmet health estimate of " );
			action.append( helmetMatcher.group( 1 ) );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		int hp = DwarfFactoryRequest.deduceHP( responseText );
		if ( hp > 0 )
		{
			FightRequest.getRound( action );
			action.append( MonsterStatusTracker.getLastMonsterName() );
			action.append( " shows dwarvish war mattock health estimate of " );
			action.append( hp );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		int attack = DwarfFactoryRequest.deduceAttack( responseText );
		if ( attack > 0 )
		{
			FightRequest.getRound( action );
			action.append( MonsterStatusTracker.getLastMonsterName() );
			action.append( " shows dwarvish war helmet attack rating of " );
			action.append( attack );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		int defense = DwarfFactoryRequest.deduceDefense( responseText );
		if ( defense > 0 )
		{
			FightRequest.getRound( action );
			action.append( MonsterStatusTracker.getLastMonsterName() );
			action.append( " shows dwarvish war kilt defense rating of " );
			action.append( defense );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}
	}

	private static void logMonsterAttribute( final StringBuffer action, final int damage, final int type )
	{
		if ( damage == 0 )
		{
			return;
		}

		FightRequest.getRound( action );
		action.append( MonsterStatusTracker.getLastMonsterName() );

		if ( damage > 0 )
		{
			action.append( type == HEALTH ? " takes " : " drops " );
			action.append( damage );
			action.append( type == ATTACK ? " attack power." :
				       type == DEFENSE ? " defense." :
				       " damage." );
		}
		else
		{
			action.append( type == HEALTH ? " heals " : " raises " );
			action.append( -1 * damage );
			action.append( type == ATTACK ? " attack power." :
				       type == DEFENSE ? " defense." :
				       " hit points." );
		}

		String message = action.toString();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	// NOTE: All of the non-empty patterns that can match in the first group
	// imply that the entire expression should be ignored.	If you add one
	// and this is not the case, then correct the use of this Pattern below.

	private static final Pattern PHYSICAL_PATTERN =
		Pattern.compile( "(your blood, to the tune of|stabs you for|sown|You lose|You gain|strain your neck|approximately|roughly|)\\s*#?(\\d[\\d,]*) (\\([^.]*\\) |)((?:[^\\s]+ ){0,3})(?:\"?damage|points?|bullets|hollow|notch(?:es)?|to your opponent|to the foul demon|force damage|tiny holes|like this, it's bound)" );

	private static final Pattern ELEMENTAL_PATTERN =
		Pattern.compile( "(sown|) \\+?([\\d,]+) (\\([^.]*\\) |)(?:months worth of concentrated palm sweat|(?:slimy, (?:clammy|gross) |hotsy-totsy |)damage|points|HP worth)" );

	private static final Pattern SECONDARY_PATTERN = Pattern.compile( "\\+([\\d,]+)" );

	private static int parseNormalDamage( final String text )
	{
		if ( text.equals( "" ) )
		{
			return 0;
		}

		int damage = 0;

		Matcher m = FightRequest.PHYSICAL_PATTERN.matcher( text );
		if ( m.find() )
		{
			// Currently, all of the explicit attack messages that
			// preceed the number all imply that this is not damage
			// against the monster or is damage that should not
			// count (reap/sow X damage.)

			if ( !m.group( 1 ).equals( "" ) )
			{
				return 0;
			}

			// "shambles up to your opponent" following a number is
			// most likely a familiar naming problem, so it should
			// not count.
			//
			// Similarly, using a number scroll on something other
			// than an adding machine does not do physical damage:
			//
			// You hand the 33398 scroll to your opponent. It
			// unrolls it, reads it, and looks slightly confused by
			// it. Then it tears it up and throws the bits into the
			// wind.

			if ( m.group( 4 ).equals( "shambles up " ) || m.group( 4 ).equals( "scroll " ) )
			{
				return 0;
			}

			damage += StringUtilities.parseInt( m.group( 2 ) );

			// The last string contains all of the extra damage
			// from dual-wielding or elemental damage, e.g. "(+3)
			// (+10)".

			Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher( m.group( 3 ) );
			while ( secondaryMatcher.find() )
			{
				damage += StringUtilities.parseInt( secondaryMatcher.group( 1 ) );
			}

			return damage;
		}

		m = FightRequest.ELEMENTAL_PATTERN.matcher( text );
		if ( m.find() )
		{
			if ( !m.group( 1 ).equals( "" ) )
			{
				return 0;
			}

			damage += StringUtilities.parseInt( m.group( 2 ) );

			Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher( m.group( 3 ) );
			while ( secondaryMatcher.find() )
			{
				damage += StringUtilities.parseInt( secondaryMatcher.group( 1 ) );
			}
			return damage;
		}

		return 0;
	}

	private static final Pattern HAIKU_DAMAGE2_PATTERN =
		Pattern.compile( "<b>(?:<font color=[\"]?(\\w+)[\"]?>)?([\\d,]+)(?:</font>)?</b> damage" );

	public static Pattern HAIKU_PATTERN = Pattern.compile( "<td valign=center[^>]*>(.*?)</td>" );

	private static Pattern INT_PATTERN = Pattern.compile( "\\d+" );

	private static Pattern EFF_PATTERN = Pattern.compile( "eff\\(['\"](.*?)['\"]" );

	private static int parseHaikuDamage( final String text )
	{
		if ( !text.contains( "damage" ) && !text.contains( "from you to your foe" ) )
		{
			return 0;
		}

		Matcher damageMatcher = FightRequest.INT_PATTERN.matcher( text );
		if ( damageMatcher.find() )
		{
			return StringUtilities.parseInt( damageMatcher.group() );
		}
		return 0;
	}

	private static boolean extractVerse( final TagNode node, final StringBuffer buffer, final String tag )
	{
		boolean hasTag = false;
		String nodeName = node.getName(); 

		if ( nodeName.equals( "br" ) )
		{
			buffer.append( " / " );
		}

		if ( tag != null && nodeName.equals( tag ) )
		{
			hasTag = true;
		}

		Iterator it = node.getChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof ContentToken )
			{
				buffer.append( ((ContentToken) child).getContent() );
			}
			else if ( child instanceof TagNode )
			{
				hasTag |= FightRequest.extractVerse( (TagNode) child, buffer, tag );
			}
		}

		return hasTag;
	}

	private static void processHaikuResult( final TagNode node, final TagNode inode, final String image, final TagStatus status )
	{
		if ( image.equals( status.familiar ) || image.equals( status.enthroned ) )
		{
			FightRequest.processFamiliarAction( node, inode, status );
			return;
		}

		StringBuffer action = status.action;
		action.setLength( 0 );

		boolean hasBold = FightRequest.extractVerse( node, action, "b" );
		String haiku = action.toString();

		if ( FightRequest.foundVerseDamage( inode, action, status ) )
		{
			return;
		}

		Matcher m = INT_PATTERN.matcher( haiku );
		if ( !m.find() )
		{
			if ( image.equals( "strboost.gif" ) && hasBold )
			{
				String message = "You gain a Muscle point!";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}

			if ( image.equals( "snowflakes.gif" ) && hasBold )
			{
				String message = "You gain a Mysticality point!";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}

			if ( image.equals( "wink.gif" ) && hasBold )
			{
				String message = "You gain a Moxie point!";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}
			return;
		}

		String points = m.group();

		if ( image.equals( "meat.gif" ) )
		{
			String message = "You gain " + points + " Meat";
			ResultProcessor.processMeat( message, status.won, status.nunnery );
			status.shouldRefresh = true;
			return;
		}

		if ( image.equals( "hp.gif" ) )
		{
			// Gained or lost HP

			String gain = "lose";

			// Your wounds fly away
			// on a refreshing spring breeze.
			// You gain <b>X</b> hit points.

			// When <b><font color=black>XX</font></b> hit points<
			// are restored to your body,
			// you make an "ahhhhhhhh" sound.

			// You're feeling better --
			// <b><font color=black>XXX</font></b> hit points better -
			// than you were before.

			if ( haiku.contains( "Your wounds fly away" ) ||
                    haiku.contains( "restored to your body" ) ||
                    haiku.contains( "You're feeling better" ) )
			{
				gain = "gain";
			}

			String message = "You " + gain + " " + points + " hit points";
			status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			return;
		}

		if ( image.equals( "mp.gif" ) )
		{
			// Gained or lost MP

			String gain = "lose";

			// You feel quite refreshed,
			// like a frog drinking soda,
			// gaining <b>X</b> MP.

			// A contented belch.
			// Ripples in a mystic pond.
			// You gain <b>X</b> MP.

			// Like that wimp Dexter,
			// you have become ENERGIZED,
			// with <b>XX</b> MP.

			// <b>XX</b> magic points
			// fall upon you like spring rain.
			// Mana from heaven.

			// Spring rain falls within
			// metaphorically, I mean.
			// <b>XXX</b> mp.

			// <b>XXX</b> MP.
			// Like that sports drink commercial --
			// is it in you?  Yes.

			if ( haiku.contains( "You feel quite refreshed" ) ||
                    haiku.contains( "A contented belch" ) ||
                    haiku.contains( "ENERGIZED" ) ||
                    haiku.contains( "Mana from heaven" ) ||
                    haiku.contains( "Spring rain falls within" ) ||
                    haiku.contains( "sports drink" ) )
			{
				gain = "gain";
			}
			String message = "You " + gain + " " + points + " Mojo points";

			status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			return;
		}

		if ( image.equals( "strboost.gif" ) )
		{
			String message = "You gain " + points + " Strongness";
			status.shouldRefresh |= ResultProcessor.processStatGain( message, null );
			return;
		}

		if ( image.equals( "snowflakes.gif" ) )
		{
			String message = "You gain " + points + " Magicalness";
			status.shouldRefresh |= ResultProcessor.processStatGain( message, null );
			return;
		}

		if ( image.equals( "wink.gif" ) )
		{
			String message = "You gain " + points + " Roguishness";
			status.shouldRefresh |= ResultProcessor.processStatGain( message, null );
			return;
		}

		if ( haiku.contains( "damage" ) )
		{
			// Using a combat item
			int damage = StringUtilities.parseInt( points );
			if ( status.logMonsterHealth )
			{
				FightRequest.logMonsterAttribute( action, damage, HEALTH );
			}
			MonsterStatusTracker.damageMonster( damage );
			return;
		}
	}

	private static int parseVerseDamage( final TagNode inode )
	{
		if ( inode == null )
		{
			return 0;
		}

		// Look for Damage: title in the image
		String title = inode.getAttributeByName( "title" );
		if ( title == null || !title.startsWith( "Damage: " ) )
		{
			return 0;
		}

		int damage = 0;

		String[] pieces = title.substring( 8 ).split( "[^\\d,]+" );
		for ( int i = 0; i < pieces.length; ++i )
		{
			damage += StringUtilities.parseInt( pieces[ i ] );
		}

		return damage;
	}

	private static boolean foundVerseDamage( final TagNode inode, final StringBuffer action, final TagStatus status )
	{
		int damage = parseVerseDamage( inode );
		if ( damage == 0 )
		{
			return false;
		}

		if ( status.logMonsterHealth )
		{
			FightRequest.logMonsterAttribute( action, damage, HEALTH );
		}

		MonsterStatusTracker.damageMonster( damage );

		return true;
	}

	private static void processAnapestResult( final TagNode node, final TagNode inode, final String image, final TagStatus status )
	{
		if ( image.equals( status.familiar ) || image.equals( status.enthroned ) )
		{
			FightRequest.processFamiliarAction( node, inode, status );
			return;
		}

		StringBuffer action = status.action;
		action.setLength( 0 );

		boolean hasFont = FightRequest.extractVerse( node, action, "font" );
		String verse = action.toString();

		if ( FightRequest.foundVerseDamage( inode, action, status ) )
		{
			return;
		}

		Matcher m = INT_PATTERN.matcher( verse );
		if ( !m.find() )
		{
			if ( image.equals( "strboost.gif" ) )
			{
				String message = "You gain a Muscle point!";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}

			if ( image.equals( "snowflakes.gif" ) )
			{
				String message = "You gain a Mysticality point!";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}

			if ( image.equals( "wink.gif" ) )
			{
				String message = "You gain a Moxie point!";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}
			return;
		}

		String points = m.group();

		if ( image.equals( "meat.gif" ) )
		{
			String message = "You gain " + points + " Meat";
			ResultProcessor.processMeat( message, status.won, status.nunnery );
			status.shouldRefresh = true;
			return;
		}

		if ( image.equals( "hp.gif" ) )
		{
			// Gained or lost HP

			String gain = "lose";

			// You heal <b><font color=black>4</font></b> hit points, which may not be a lot,
			// But let's face it, right now, it's the best that you've got.

			// You've been beat up and beat down and beat sideways, too,
			// But you heal 7 hitpoints, and you feel less blue.

			// You've gained 7 hitpoints, and feel a lot better.
			// Go out there and get 'em, you going go-getter!

			// With <b><font color=black>21</font></b> hitpoints added onto your score,
			// you're raring to go and you're ready for more!

			// You apply a fresh bandage to stop blood from spurting,
			// and regenerate 21 points worth of hurting.

			// You've got 12 more hit points than you had before!
			// Now go out and fight and get off of the floor!

			// You were starting to feel a bit down in the dumps,
			// but those 7 HP should help clear up the lumps.

			// You heal a few damage -- not much, I'll admit,
			// but the hitpoints you've added will help you a bit.

			if ( verse.contains( "You heal" ) ||
                    verse.contains( "you heal" ) ||
                    verse.contains( "feel a lot better" ) ||
                    verse.contains( "added onto your score" ) ||
                    verse.contains( "regenerate" ) ||
                    verse.contains( "more hit points" ) ||
                    verse.contains( "help clear up the lumps" ) )
			{
				gain = "gain";
			}

			String message = "You " + gain + " " + points + " hit points";
			status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );

			if ( gain.equals( "gain" ) )
			{
				if ( status.mosquito )
				{
					status.mosquito = false;

					int damage = StringUtilities.parseInt( points );
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterAttribute( action, damage, HEALTH );
					}

					MonsterStatusTracker.damageMonster( damage );
				}
				else if ( FightRequest.isStoneSphere( status.lastCombatItem ) )
				{
					FightRequest.identifyStoneSphere( ItemPool.SPHERE_OF_WATER, status.lastCombatItem );
					status.lastCombatItem = -1;
				}
			}

			return;
		}

		if ( image.equals( "mp.gif" ) )
		{
			// Gained or lost MP

			String gain = "lose";

			// Magical energy floods into your veins!
			// Not a whole lot -- 10 points -- but it's good for your brains.

			// You regain 15 MP of mystical fuel,
			// and prepare to escort some more monsters to school.

			// You've just gotten 10 of your MP restored!
			// Now you can show all those creeps what you've learned!

			// Your MP's restored, it's now 16 points higher.
			// Now you can set some more monsters on fire!

			// 17 MP should add spring to your step,
			// and lift up your spirits with gusto and pep!

			// Your MP just went up by 11 quarts!
			// (I'm not sure how you measure amounts of this sort.)

			if ( verse.contains( "Magical energy floods into your veins" ) ||
                    verse.contains( "mystical fuel" ) ||
                    verse.contains( "your MP restored" ) ||
                    verse.contains( "set some more monsters on fire" ) ||
                    verse.contains( "add spring to your step" ) ||
                    verse.contains( "Your MP just went up" ) )
			{
				gain = "gain";
			}

			String message = "You " + gain + " " + points + " Mojo points";

			status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			return;
		}

		if ( image.equals( "strboost.gif" ) )
		{
			String message = "You gain " + points + " Strongness";
			status.shouldRefresh |= ResultProcessor.processStatGain( message, null );
			return;
		}

		if ( image.equals( "snowflakes.gif" ) )
		{
			String message = "You gain " + points + " Magicalness";
			status.shouldRefresh |= ResultProcessor.processStatGain( message, null );
			return;
		}

		if ( image.equals( "wink.gif" ) )
		{
			String message = "You gain " + points + " Roguishness";
			status.shouldRefresh |= ResultProcessor.processStatGain( message, null );
			return;
		}

		// You hurl a thing that you took from your pack,
		// and deal <b>1</b> points with a thingly attack.

		if ( verse.contains( "thingly attack" ) )
		{
			// Using a combat item
			int damage = StringUtilities.parseInt( points );
			if ( status.logMonsterHealth )
			{
				FightRequest.logMonsterAttribute( action, damage, HEALTH );
			}
			MonsterStatusTracker.damageMonster( damage );

			if ( FightRequest.isStoneSphere( status.lastCombatItem ) )
			{
				int sphere = hasFont ? ItemPool.SPHERE_OF_FIRE : ItemPool.SPHERE_OF_LIGHTNING;
				FightRequest.identifyStoneSphere( sphere, status.lastCombatItem );
				status.lastCombatItem = -1;
			}

			return;
		}
	}

	public static class TagStatus
	{
		public String familiar;
		public String enthroned;
		public final boolean doppel;
		public String diceMessage;
		public final String ghost;
		public final boolean logFamiliar;
		public final boolean logMonsterHealth;
		public final StringBuffer action;
		public boolean shouldRefresh;
		public boolean famaction = false;
		public boolean mosquito = false;
		public boolean dice = false;
		public boolean nunnery = false;
		public boolean won = false;
		public Matcher macroMatcher;
		public int lastCombatItem = -1;

		public TagStatus()
		{
			FamiliarData current = KoLCharacter.getFamiliar();
			this.familiar = current.getImageLocation();
			this.doppel =
				( current.getId() == FamiliarPool.DOPPEL ) ||
				KoLCharacter.hasEquipped( ItemPool.TINY_COSTUME_WARDROBE, EquipmentManager.FAMILIAR );

			this.diceMessage = ( current.getId() == FamiliarPool.DICE ) ? ( current.getName() + " begins to roll." ) : null;


			FamiliarData enthroned = KoLCharacter.getEnthroned();
			this.enthroned = enthroned.getImageLocation();
			this.logFamiliar = Preferences.getBoolean( "logFamiliarActions" );
			this.logMonsterHealth = Preferences.getBoolean( "logMonsterHealth" );
			this.action = new StringBuffer();

			this.shouldRefresh = false;

			// Note if we are fighting The Themthar Hills
			this.nunnery = MonsterStatusTracker.getLastMonsterName().equals( "dirty thieving brigand" );

			if ( KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER )
			{
				String name = Preferences.getString( "pastamancerGhostName" );
				this.ghost = name.equals( "" ) ? null : name;
			}
			else
			{
				this.ghost = null;
			}
		}

		public void setFamiliar( final String image )
		{
			FamiliarData current = KoLCharacter.getFamiliar();
			int id = FamiliarDatabase.getFamiliarByImageLocation( image );
			if ( id == current.getId() )
			{
				KoLCharacter.resetEffectiveFamiliar();
			}
			else
			{
				KoLCharacter.setEffectiveFamiliar( new FamiliarData( id ) );
			}
			FamiliarData effective = KoLCharacter.getEffectiveFamiliar();
			this.familiar = image;
			this.diceMessage = ( effective.getId() == FamiliarPool.DICE ) ? ( current.getName() + " begins to roll." ) : null;
		}
	}

	private static void processNormalResults( final String text, final Matcher macroMatcher )
	{
		TagNode fight = parseFightHTML( text, true );
		if ( fight == null )
		{
			// Do normal result processing and hope for the best.
			FightRequest.shouldRefresh = ResultProcessor.processResults( true, text );
			return;
		}

		if ( RequestLogger.isDebugging() && Preferences.getBoolean( "logCleanedHTML" ) )
		{
			FightRequest.logHTML( fight );
		}

		TagStatus status = new TagStatus();
		status.macroMatcher = macroMatcher;

		FightRequest.processNode( fight, status );

		FightRequest.shouldRefresh = status.shouldRefresh;
	}

	public static void parseFightHTML( final String text )
	{
		FightRequest.logHTML( parseFightHTML( text, false ) );
	}

	private static TagNode parseFightHTML( String text, boolean logIt )
	{
		// Clean the HTML on the Fight page
		TagNode node = cleanFightHTML( text );
		if ( node == null )
		{
			if ( logIt )
			{
				RequestLogger.printLine( "HTML cleaning failed." );
			}
			return null;
		}

		// Find the monster tag
		TagNode mon = findMonsterTag( node );
		if ( mon == null )
		{
			if ( logIt )
			{
				RequestLogger.printLine( "Cannot find monster." );
			}
			return null;
		}

		// Find the top of the parse tree
		TagNode fight = findFightNode( mon );
		if ( fight == null )
		{
			if ( logIt )
			{
				RequestLogger.printLine( "Cannot find combat results." );
			}
			return null;
		}
		return fight;
	}

	private static TagNode cleanFightHTML( final String text )
	{
		try
		{
			// Clean the HTML on this fight response page
			return cleaner.clean( text );
		}
		catch ( IOException e )
		{
			StaticEntity.printStackTrace( e );
		}
		return null;
	}

	private static TagNode findMonsterTag( final TagNode node )
	{
		if ( node == null )
		{
			return null;
		}

		// Look first for 'monpic' image.
		// All haiku monsters and most normal monsters have that.
		TagNode mon = node.findElementByAttValue( "id", "monpic", true, false );

		// If that fails, look for 'monname' span.
		if ( mon == null )
		{
			mon = node.findElementByAttValue( "id", "monname", true, false );
		}
		return mon;
	}

	private static TagNode findFightNode( final TagNode mon )
	{
		// Walk up the tree and find <center>
		//
		// The parent of that node has everything interesting about the
		// fight.
		TagNode fight = mon;
		while ( fight != null )
		{
			fight = fight.getParent();
			if ( fight != null && fight.getName().equals( "center" ) )
			{
				// One more level
				return fight.getParent();
			}
		}
		return null;
	}

	private static Pattern FUMBLE_PATTERN =
		Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static final Pattern MOSQUITO_PATTERN =
		Pattern.compile( "sucks some blood out of your opponent and injects it into you." );
	private static final Pattern ADORABLE_SEAL_PATTERN =
		Pattern.compile( "greedily sucks the vital juices from the wound" );
	private static Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static Pattern CARBS_PATTERN = Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );

	private static void specialFamiliarDamage( final StringBuffer text, TagStatus status )
	{
		int familiarId = KoLCharacter.getEffectiveFamiliar().getId();

		if ( FightRequest.anapest || FightRequest.haiku )
		{
			switch ( familiarId )
			{
			case FamiliarPool.MOSQUITO:
			case FamiliarPool.ADORABLE_SEAL_LARVA:
				status.mosquito = true;
				break;
			}

			return;
		}

		// Mosquito can muck with the monster's HP, but doesn't have
		// normal text.

		switch ( familiarId )
		{
		case FamiliarPool.MOSQUITO:
		{
			Matcher m = FightRequest.MOSQUITO_PATTERN.matcher( text );
			if ( m.find() )
			{
				status.mosquito = true;
			}
			break;
		}

		case FamiliarPool.ADORABLE_SEAL_LARVA:
		{
			Matcher m = FightRequest.ADORABLE_SEAL_PATTERN.matcher( text );

			if ( m.find() )
			{
				status.mosquito = true;
			}
			break;
		}

		case FamiliarPool.STAB_BAT:
		{
			Matcher m = FightRequest.STABBAT_PATTERN.matcher( text );

			if ( m.find() )
			{
				String message = "You lose " + m.group( 1 ) + " hit points";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}
			break;
		}

		case FamiliarPool.ORB:
		{
			Matcher m = FightRequest.CARBS_PATTERN.matcher( text );

			if ( m.find() )
			{
				String message = "You lose " + m.group( 1 ) + " hit points";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}
			break;
		}
		}
	}

	private static void processNode( final TagNode node, final TagStatus status )
	{
		String name = node.getName();
		StringBuffer action = status.action;

		// Skip scripts, forms, buttons, and html links
		if ( name.equals( "form" ) ||
		     name.equals( "input" ) ||
		     name.equals( "a" ) ||
		     name.equals( "div" ) )
		{
			return;
		}

		/// node-specific processing
		if ( name.equals( "script" ) )
		{
			Matcher m = CLEESH_PATTERN.matcher( node.getText() );
			if ( m.find() )
			{
				String newMonster = m.group( 1 );
				MonsterStatusTracker.setNextMonsterName( CombatActionManager.encounterKey( newMonster ) );
				FightRequest.logText( "your opponent becomes " + newMonster + "!", status );
			}

			return;
		}

		if ( name.equals( "hr" ) )
		{
			FightRequest.updateRoundData( status.macroMatcher );
			if ( status.macroMatcher.find() )
			{
				FightRequest.registerMacroAction( status.macroMatcher );
				++FightRequest.currentRound;
			}
			else
			{
				FightRequest.logText( "unspecified macro action?", status );
			}
		}
		else if ( name.equals( "table" ) )
		{
			// Items have "rel" strings.
			String cl = node.getAttributeByName( "class" );
			String rel = node.getAttributeByName( "rel" );
			if ( cl != null && cl.equals( "item" ) && rel != null )
			{
				AdventureResult result = ItemDatabase.itemFromRelString( rel );
				ResultProcessor.processItem( true, "You acquire an item:", result, (List) null );
				return;
			}

			// Tables often appear in fight results to hold images.
			TagNode inode = node.findElementByName( "img", true );

			if ( status.famaction )
			{
				FightRequest.processFamiliarAction( node, inode, status );
				status.famaction = false;
				return;
			}

			StringBuffer text = node.getText();
			String str = text.toString();

			if ( inode == null )
			{
				// No image. Parse combat damage.
				int damage = ( FightRequest.haiku || FightRequest.anapest ) ?
					FightRequest.parseHaikuDamage( str ) :
					FightRequest.parseNormalDamage( str );
				if ( damage != 0 )
				{
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterAttribute( action, damage, HEALTH );
					}
					MonsterStatusTracker.damageMonster( damage );
				}

				// If it's not combat damage, perhaps it's a stat gain
				else if ( str.startsWith( "You gain" ) )
				{
					status.shouldRefresh |= ResultProcessor.processGainLoss( str, null );
				}

				// The tootlers tootle! The singers all sing!
				// You've accomplished a wonderful, glorious thing!
				// Come raise a glass high, and come join in the revel!
				// We're all celebrating! You went up a level!

				else if ( FightRequest.anapest && str.contains( "You went up a level" ) )
				{
					String msg = "You gain a Level!";
					status.shouldRefresh |= ResultProcessor.processGainLoss( msg, null );
				}

				return;
			}

			// Look for items and effects first
			String onclick = inode.getAttributeByName( "onclick" );
			if ( onclick != null )
			{
				if ( onclick.startsWith( "descitem" ) &&
                        !str.contains( "An item drops:" ) )
				{
					Matcher m = INT_PATTERN.matcher( onclick );
					if ( !m.find() )
					{
						return;
					}

					int itemId = ItemDatabase.getItemIdFromDescription( m.group() );
					AdventureResult result = ItemPool.get( itemId, 1 );
					ResultProcessor.processItem( true, "You acquire an item:", result, (List) null );
					if ( str.contains( "Item unequipped:" ) )
					{	// Item removed by Zombo
						EquipmentManager.discardEquipment( itemId );
					}
					return;
				}

				Matcher m = EFF_PATTERN.matcher( onclick );
				if ( m.find() )
				{
					// Gain/loss of effect
					status.shouldRefresh = true;
					String effect = EffectDatabase.getEffectName( m.group( 1 ) );
					if ( effect == null )
					{
						return;
					}
					// For prettiness
					String munged = StringUtilities.singleStringReplace( str, "(", " (" );
					if ( FightRequest.haiku || FightRequest.anapest )
					{	// the haiku doesn't name the effect
						munged = "You acquire an effect: " + effect;
					}
					ResultProcessor.processEffect( effect, munged );
					if ( effect.equalsIgnoreCase( Effect.HAIKU_STATE_OF_MIND.effectName() ) )
					{
						FightRequest.haiku = true;
						if ( status.logMonsterHealth )
						{
							FightRequest.logMonsterAttribute( action, 17, HEALTH );
						}
						MonsterStatusTracker.damageMonster( 17 );
					}
					else if ( effect.equalsIgnoreCase( Effect.JUST_THE_BEST_ANAPESTS.effectName() ) )
					{
						FightRequest.anapest = true;
					}
					return;
				}
			}

			String src = inode.getAttributeByName( "src" );

			if ( src == null ) return;

			String image = src.substring( src.lastIndexOf( "/" ) + 1 );

			// Attempt to identify combat items
			String itemName = inode.getAttributeByName( "title" );
			int itemId = ItemDatabase.getItemId( itemName );
			if ( itemId != -1 && image.equals( ItemDatabase.getImage( itemId ) ) )
			{
				status.lastCombatItem = itemId;
			}

			if ( image.equals( "hp.gif" ) &&
			     (str.contains( "regains" ) ||
                         str.contains( "She looks about" )) )
			{
				// The monster heals itself
				Matcher m = INT_PATTERN.matcher( str );
				int healAmount = m.find() ? StringUtilities.parseInt( m.group() ) : 0;
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterAttribute( action, -healAmount, HEALTH );
				}
				MonsterStatusTracker.healMonster( healAmount );

				status.shouldRefresh = true;
				return;
			}

			if ( image.equals( "nicesword.gif" ) )
			{
				// You modify monster attack power
				Matcher m = INT_PATTERN.matcher( str );
				int damage = m.find() ? StringUtilities.parseInt( m.group() ) : 0;
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterAttribute( action, damage, ATTACK );
				}
				MonsterStatusTracker.lowerMonsterAttack( damage );
				return;
			}

			if ( image.equals( "whiteshield.gif" ) )
			{
				// You modify monster defense
				Matcher m = INT_PATTERN.matcher( str );
				int damage = m.find() ? StringUtilities.parseInt( m.group() ) : 0;
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterAttribute( action, damage, DEFENSE );
				}
				MonsterStatusTracker.lowerMonsterDefense( damage );
				return;
			}

			// If you have Just the Best Anapests and go to the
			// haiku dungeon, you see ... anapests!

			if ( FightRequest.anapest )
			{
				FightRequest.processAnapestResult( node, inode, image, status );
				return;
			}

			if ( FightRequest.haiku )
			{
				FightRequest.processHaikuResult( node, inode, image, status );
				return;
			}

			if ( image.equals( "meat.gif" ) )
			{
				// Adjust for Can Has Cyborger
				str = StringUtilities.singleStringReplace( str, "gets", "gain" );
				str = StringUtilities.singleStringReplace( str, "Meets", "Meat" );

				// Adjust for The Sea
				str = StringUtilities.singleStringReplace( str, "manage to grab", "gain" );

				// If we are in The Themthar Hills and we have
				// seen the "you won" comment, the nuns take
				// the meat.

				ResultProcessor.processMeat( str, status.won, status.nunnery );
				status.shouldRefresh = true;
				return;
			}

			if ( image.equals( "hp.gif" ) ||
			     image.equals( "mp.gif" ) )
			{
				// You gain HP or MP
				if ( status.mosquito )
				{
					status.mosquito = false;
					Matcher m = INT_PATTERN.matcher( str );
					int damage = m.find() ? StringUtilities.parseInt( m.group() ) : 0;
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterAttribute( action, damage, HEALTH );
					}
					MonsterStatusTracker.damageMonster( damage );
				}

				status.shouldRefresh |= ResultProcessor.processGainLoss( str, null );

				return;
			}

			if ( image.equals( status.familiar ) || image.equals( status.enthroned ) )
			{
				FightRequest.processFamiliarAction( node, inode, status );
				return;
			}

			if ( image.equals( "hkatana.gif" ) )
			{
				// You struck with your haiku katana. Pull the
				// damage out of the img tag if we can
				if ( FightRequest.foundVerseDamage( inode, action, status ) )
				{

					return;
				}
			}

			if ( image.equals( "realdolphin_r.gif" ) )
			{
				// You are slowed too much by the water, and a
				// stupid dolphin swims up and snags a seaweed
				// before you can grab it.

				// Inside this table is another table with
				// another image of the stolen dolphin item.

				TagNode tnode = node.findElementByName( "table", true );
				if ( tnode == null )
				{
					return;
				}

				TagNode inode2 = tnode.findElementByName( "img", true );
				if ( inode2 == null )
				{
					return;
				}

				String onclick2 = inode2.getAttributeByName( "onclick" );
				if ( onclick2 == null || !onclick2.startsWith( "descitem" ) )
				{
					return;
				}

				Matcher m = INT_PATTERN.matcher( onclick2 );
				String descid = m.find() ? m.group() : null;

				if ( descid == null )
				{
					return;
				}

				itemId = ItemDatabase.getItemIdFromDescription( descid );
				if ( itemId == -1 )
				{
					return;
				}

				AdventureResult result = ItemPool.get( itemId, 1 );
				RequestLogger.printLine( "A dolphin stole: " + result );
				Preferences.setString( "dolphinItem", result.getName() );
				return;
			}

			// Combat item usage: process the children of this node
			// to pick up damage to the monster and stat gains
		}
		else if ( name.equals( "p" ) )
		{
			StringBuffer text = node.getText();
			String str = text.toString();

			if ( FightRequest.handleFuzzyDice( str, status ) )
			{
				return;
			}

			if ( FightRequest.processFumble( str, status ) )
			{
				return;
			}

			if ( FightRequest.handleEvilometer( str, status ) )
			{
				return;
			}
			
			if ( FightRequest.handleKeyotron( str, status ) )
			{
				return;
			}

			boolean ghostAction = status.ghost != null && str.contains( status.ghost );
			if ( ghostAction && status.logFamiliar )
			{
				// Pastamancer ghost action
				FightRequest.logText( text, status );
			}

			int damage = FightRequest.parseNormalDamage( str );
			if ( damage != 0 )
			{
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterAttribute( action, damage, HEALTH );
				}
				MonsterStatusTracker.damageMonster( damage );
				FightRequest.processComments( node, status );
				return;
			}

			if ( ghostAction )
			{
				return;
			}
		}

		Iterator it = node.getChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof CommentToken )
			{
				CommentToken object = (CommentToken) child;
				FightRequest.processComment( object, status );
				continue;
			}

			if ( child instanceof ContentToken )
			{
				ContentToken object = (ContentToken) child;
				String text = object.getContent().trim();

				if ( text.equals( "" ) )
				{
					continue;
				}

				if ( FightRequest.handleFuzzyDice( text, status ) )
				{
					continue;
				}

				if ( FightRequest.processFumble( text, status ) )
				{
					continue;
				}

				if ( text.contains( "you feel all warm and fuzzy" ) )
				{
					if ( status.logFamiliar )
					{
						FightRequest.logText( "A freed guard turtle returns.", status );
					}
					continue;
				}

				boolean ghostAction = status.ghost != null && text.contains( status.ghost );
				if ( ghostAction && status.logFamiliar )
				{
					// Pastamancer ghost action
					FightRequest.logText( text, status );
				}

				int damage = FightRequest.parseNormalDamage( text );
				if ( damage != 0 )
				{
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterAttribute( action, damage, HEALTH );
					}
					MonsterStatusTracker.damageMonster( damage );
					continue;
				}

				if ( text.startsWith( "You acquire a skill" ) )
				{
					TagNode bnode = node.findElementByName( "b", true );
					if ( bnode != null )
					{
						String skill = bnode.getText().toString();
						ResponseTextParser.learnSkill( skill );
					}
					continue;
				}

				if ( text.startsWith( "You gain" ) )
				{
					status.shouldRefresh |= ResultProcessor.processGainLoss( text, null );
					continue;
				}

				if ( text.startsWith( "You can has" ) )
				{
					// Adjust for Can Has Cyborger
					text = StringUtilities.singleStringReplace( text, "can has", "gain" );
					ResultProcessor.processGainLoss( text, null );
					continue;
				}
				continue;
			}

			if ( child instanceof TagNode )
			{
				TagNode object = (TagNode) child;
				FightRequest.processNode( object, status );
				continue;
			}
		}
	}

	private static void processComments( TagNode node, TagStatus status )
	{
		Iterator it = node.getChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof CommentToken )
			{
				CommentToken object = (CommentToken) child;
				FightRequest.processComment( object, status );
				continue;
			}
		}
	}

	private static void processComment( CommentToken object, TagStatus status )
	{
		String content = object.getContent();
		if ( content.equals( "familiarmessage" ) )
		{
			status.famaction = true;
		}
		else if ( content.equals( "WINWINWIN" ) )
		{
			FightRequest.logText( KoLCharacter.getUserName() + " wins the fight!", status );
			status.won = true;
			FightRequest.currentRound = 0;
		}
		// macroaction: comment handled elsewhere
	}

	private static void processFamiliarAction( TagNode node, TagNode inode, TagStatus status )
	{
		StringBuffer action = status.action;

		// <img src="http://images.kingdomofloathing.com/itemimages/familiar6.gif" width=30 height=30></td><td valign=center>Jiggly Grrl disappears into the wardrobe, and emerges dressed as a pair of Fuzzy Dice.

		// If you have a tiny costume wardrobe or a doppelshifter, it
		// can change its image mid-battle.
		if ( status.doppel )
		{
			String src = inode != null ? inode.getAttributeByName( "src" ) : null;
			if ( src != null )
			{
				String image = src.substring( src.lastIndexOf( "/" ) + 1 );
				status.setFamiliar( image );
			}
		}

		// Preprocess this node: remove tables and process them later.
		// This will also remove the table text from the node text and
		// thus improve the message we log.

		TagNode [] tables = node.getElementsByName( "table", true );
		for ( int i = 0; i < tables.length; ++i )
		{
			TagNode table = tables[i];
			table.getParent().removeChild( table );
		}

		// Always separate multiple lines with slashes
		StringBuffer text = new StringBuffer();
		FightRequest.extractVerse( node, text, null );
		String str = text.toString();

		if ( !str.equals( "" ) && !ResultProcessor.processFamiliarWeightGain( str ) )
		{
			// Familiar combat action?
			if ( status.logFamiliar )
			{
				FightRequest.logText( text, status );
			}

			int damage = FightRequest.parseVerseDamage( inode );
			if ( damage == 0 )
			{
				damage = FightRequest.parseNormalDamage( str );
			}

			if ( damage != 0 )
			{
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterAttribute( action, damage, HEALTH );
				}
				MonsterStatusTracker.damageMonster( damage );
			}

			FightRequest.specialFamiliarDamage( text, status );
		}

		// Now process additional familiar actions
		for ( int i = 0; i < tables.length; ++i )
		{
			TagNode table = tables[i];
			FightRequest.processNode( table, status );
		}
	}

	private static boolean handleFuzzyDice( String content, TagStatus status )
	{
		if ( status.diceMessage == null )
		{
			return false;
		}

		if ( content.startsWith( status.diceMessage ) )
		{
			status.dice = true;
			return true;
		}

		if ( !status.dice )
		{
			return false;
		}

		if ( content.equals( "&nbsp;&nbsp;&nbsp;&nbsp;" ) ||
		     content.equals( "" ) )
		{
			return true;
		}

		// We finally have the whole message.
		StringBuffer action = status.action;
		action.setLength( 0 );
		action.append( status.diceMessage );
		action.append( " " );
		action.append( " " );
		action.append( content );

		if ( status.logFamiliar )
		{
			FightRequest.logText( action, status );
		}

		// No longer accumulating fuzzy dice message
		status.dice = false;

		// Fuzzy dice can do damage. Account for it.
		int damage = FightRequest.parseNormalDamage( content );
		if ( damage != 0 )
		{
			if ( status.logMonsterHealth )
			{
				FightRequest.logMonsterAttribute( action, damage, HEALTH );
			}
			MonsterStatusTracker.damageMonster( damage );
		}

		return true;
	}

	private static boolean processFumble( String text, TagStatus status )
	{
		Matcher m = FightRequest.FUMBLE_PATTERN.matcher( text );

		if ( m.find() )
		{
			String message = "You lose " + m.group( 1 ) + " hit points";
			status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			return true;
		}

		return false;
	}

	private static boolean handleEvilometer( String text, TagStatus status )
	{
		if ( !text.contains( "Evilometer" ) )
		{
			return false;
		}

		FightRequest.logText( text, status );

		String setting = null;

		MonsterData monster = MonsterDatabase.findMonster( MonsterStatusTracker.getLastMonsterName(), false );
		for ( int i = 0; i < FightRequest.EVIL_ZONES.length; ++i )
		{
			String[] data = FightRequest.EVIL_ZONES[ i ];
			KoLAdventure adventure = AdventureDatabase.getAdventure( data[ 0 ] );
			AreaCombatData area = adventure.getAreaSummary();
			if ( area.hasMonster( monster ) )
			{
				setting = data[ 1 ];
				break;
			}
		}

		if ( setting == null )
		{
			return false;
		}

		int evilness =
                text.contains( "a single beep" ) ? 1 :
                        text.contains( "beeps three times" ) ? 3 :
                                text.contains( "three quick beeps" ) ? 3 :
                                        text.contains( "five quick beeps" ) ? 5 :
                                                text.contains( "loud" ) ? Preferences.getInteger( setting ) :
			0;

		if ( evilness == 0 )
		{
			Matcher m = BEEP_PATTERN.matcher( text );
			if ( !m.find() )
			{
				return false;
			}
			evilness = StringUtilities.parseInt( m.group(1) );
		}

		Preferences.increment( setting, -evilness );
		Preferences.increment( "cyrptTotalEvilness", -evilness );
		return true;
	}
	
	private static boolean handleKeyotron( String text, TagStatus status )
	{
		if ( !text.contains( "key-o-tron" ) )
		{
			return false;
		}

		// Your key-o-tron emits 2 short tones, indicating that it has successfully processed biometric data from this subject.
		// Your key-o-tron emits a short buzz, indicating that it has already collected enough biometric data of this type.
		
		if ( text.contains( "already collected" ) )
		{
			return true;
		}
		
		FightRequest.logText( text, status );

		String setting = null;
		String monster = MonsterStatusTracker.getLastMonsterName();
		for ( int i = 0; i < FightRequest.BUGBEAR_BIODATA.length; ++i )
		{
			if ( monster.equals( BUGBEAR_BIODATA[i][0] ) )
			{
				setting = BUGBEAR_BIODATA[i][1];
				break;
			}
		}

		if ( setting == null )
		{
			return false;
		}

		Matcher matcher = FightRequest.KEYOTRON_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			Preferences.setInteger( setting, StringUtilities.parseInt( matcher.group( 1 ) ) );
		}
		
		return true;
	}

	private static void logText( StringBuffer buffer, final TagStatus status )
	{
		FightRequest.logText( buffer.toString(), status );
	}

	private static void logText( String text, final TagStatus status )
	{
		if ( text.equals( "" ) )
		{
			return;
		}

		text = StringUtilities.globalStringReplace( text, "<br>", " / " );
		text = KoLConstants.ANYTAG_PATTERN.matcher( text ).replaceAll( " " );

		StringBuffer action = status.action;
		FightRequest.getRound( action );
		action.append( text );
		String message = action.toString();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	private static void clearInstanceData()
	{
		KoLCharacter.resetEffectiveFamiliar();
		IslandDecorator.startFight();
		FightRequest.castNoodles = false;
		FightRequest.castCleesh = false;
		FightRequest.canOlfact = true;
		FightRequest.jiggledChefstaff = false;
		FightRequest.squeezedStressBall = false;
		FightRequest.summonedGhost = false;
		FightRequest.canStomp = false;
		FightRequest.desiredScroll = null;

		// Do not clear the following, since they are looked at after combat finishes.
		// FightRequest.haiku = false;
		// FightRequest.anapest = false;

		MonsterStatusTracker.reset();

		FightRequest.nextAction = null;

		FightRequest.currentRound = 0;
		FightRequest.preparatoryRounds = 0;
		FightRequest.consultScriptThatDidNothing = null;

		if ( FightRequest.initializeAfterFight )
		{
			Runnable initializeRunner = new Runnable()
			{
				public void run()
				{
					LoginManager.login( KoLCharacter.getUserName() );
					FightRequest.initializeAfterFight = false;
				}
			};

			RequestThread.runInParallel( initializeRunner );
		}
	}

	private static int getActionCost()
	{
		if ( FightRequest.nextAction.startsWith( "skill" ) )
		{
			String skillId = FightRequest.nextAction.substring( 5 );
			return SkillDatabase.getMPConsumptionById( StringUtilities.parseInt( skillId ) );
		}

		return 0;
	}

	public static void addItemActionsWithNoCost()
	{
		KoLCharacter.battleSkillNames.add( "item seal tooth" );
		KoLCharacter.battleSkillNames.add( "item spices" );

		KoLCharacter.battleSkillNames.add( "item dictionary" );
		KoLCharacter.battleSkillNames.add( "item jam band flyers" );
		KoLCharacter.battleSkillNames.add( "item rock band flyers" );

		KoLCharacter.battleSkillNames.add( "item toy soldier" );
		KoLCharacter.battleSkillNames.add( "item toy mercenary" );

		KoLCharacter.battleSkillNames.add( "item Miniborg stomper" );
		KoLCharacter.battleSkillNames.add( "item Miniborg laser" );
		KoLCharacter.battleSkillNames.add( "item Miniborg Destroy-O-Bot" );

		KoLCharacter.battleSkillNames.add( "item naughty paper shuriken" );
		KoLCharacter.battleSkillNames.add( "item bottle of G&uuml;-Gone" );
	}

	private static boolean isItemConsumed( final int itemId, final String responseText )
	{
		if ( itemId == ItemPool.EMPTY_EYE )
		{
			// You hold Zombo's eye out toward your opponent,
			// whose gaze is transfixed by it. (success)
			//   or
			// You hold Zombo's eye out toward your opponent,
			// but nothing happens. (failure)
			if ( responseText.contains( "You hold Zombo's eye out toward your opponent, whose gaze is transfixed by it." ) )
			{
				Preferences.setInteger( "_lastZomboEye", KoLAdventure.getAdventureCount() );
				// "Safe" interval between uses is 50 turns
				TurnCounter.stopCounting( "Zombo's Empty Eye" );
				TurnCounter.startCounting( 50, "Zombo's Empty Eye loc=*", "zomboeye.gif" );
			}
		}

		if ( itemId == ItemPool.ICEBALL )
		{
			// First use:
			// You hurl the iceball at your opponent, dealing X damage.
			// Then you pick up the iceball and stick it back in your sack.
			// It feels a little softer than it did before.

			// Second use:
			// You hurl the iceball at your opponent, dealing X damage.
			// When you retrieve it this time, it feels pretty slushy.

			// Third use:
			// You hurl the iceball at your opponent, dealing X damage.
			// Unfortunately, the iceball completely disintegrates on impact.

			if ( responseText.contains( "back in your sack" ) )
			{
				Preferences.setInteger( "_iceballUses", 1 );
			}
			else if ( responseText.contains( "pretty slushy" ) )
			{
				Preferences.setInteger( "_iceballUses", 2 );
			}
			else if ( responseText.contains( "completely disintegrates" ) )
			{
				Preferences.setInteger( "_iceballUses", 3 );
				return true;
			}
		}

		if ( ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_COMBAT_REUSABLE ) )
		{
			return false;
		}

		switch ( itemId )
		{
		case ItemPool.COMMUNICATIONS_WINDCHIMES:

			// Only record usage in battle if you got some sort of
			// response.
			//
			// You bang out a series of chimes, (success)
			//   or
			// A nearby hippy soldier sees you about to start
			// ringing your windchimes (failure)
			if ( responseText.contains( "bang out a series of chimes" ) ||
                    responseText.contains( "ringing your windchimes" ) )
			{
				IslandDecorator.ensureUpdatedBigIsland();
				Preferences.setInteger( "lastHippyCall", KoLAdventure.getAdventureCount() );
				// "Safe" interval between uses is 10 turns
				// http://alliancefromhell.com/forum/viewtopic.php?t=1398
				TurnCounter.stopCounting( "Communications Windchimes" );
				TurnCounter.startCounting( 10, "Communications Windchimes loc=*", "chimes.gif" );
			}

			// Then he takes your windchimes and wanders off.
			if ( responseText.contains( "he takes your windchimes" ) )
			{
				return true;
			}
			return false;

		case ItemPool.PADL_PHONE:

			// Only record usage in battle if you got some sort of
			// response.
			//
			// You punch a few buttons on the phone, (success)
			//   or
			// A nearby frat soldier sees you about to send a
			// message to HQ (failure)
			if ( responseText.contains( "punch a few buttons on the phone" ) ||
                    responseText.contains( "send a message to HQ" ) )
			{
				IslandDecorator.ensureUpdatedBigIsland();
				Preferences.setInteger( "lastFratboyCall", KoLAdventure.getAdventureCount() );
				// "Safe" interval between uses is 10 turns
				// http://alliancefromhell.com/forum/viewtopic.php?t=1398
				TurnCounter.stopCounting( "PADL Phone" );
				TurnCounter.startCounting( 10, "PADL Phone loc=*", "padl.gif" );
			}

			// Then he takes your phone and wanders off.
			if ( responseText.contains( "he takes your phone" ) )
			{
				return true;
			}
			return false;

		case ItemPool.HAROLDS_BELL:

			TurnCounter.startCounting( 20, "Harold's Bell loc=*", "bell.gif" );
			return true;

		case ItemPool.SPOOKY_PUTTY_SHEET:

			// You press the sheet of spooky putty against
			// him/her/it and make a perfect copy, which you shove
			// into your sack. He doesn't seem to appreciate it too
			// much...

			if ( responseText.contains( "make a perfect copy" ) )
			{
				Preferences.increment( "spookyPuttyCopiesMade", 1 );
				Preferences.setString( "spookyPuttyMonster", MonsterStatusTracker.getLastMonsterName() );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			if ( responseText.contains( "too scared to copy any more monsters today" ) )
			{
				Preferences.setInteger( "spookyPuttyCopiesMade", 5 );
			}
			return false;

		case ItemPool.RAIN_DOH_BOX:

			// You push the button on the side of the box.
			// It makes a scary noise, and a tiny, ghostly image
			// of your opponent appears inside it. 

			if ( responseText.contains( "ghostly image of your opponent" ) )
			{
				Preferences.increment( "_raindohCopiesMade", 1 );
				Preferences.setString( "rainDohMonster", MonsterStatusTracker.getLastMonsterName() );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			if ( responseText.contains( "too scared to use this box anymore today" ) )
			{
				Preferences.setInteger( "_raindohCopiesMade", 5 );
			}
			return false;

		case ItemPool.CAMERA:

			// With a flash of light and an accompanying old-timey
			// -POOF- noise, you take snap a picture of him. Your
			// camera begins to shake, rattle and roll.

			if ( responseText.contains( "old-timey <i>-POOF-</i> noise" ) )
			{
				Preferences.setString( "cameraMonster", MonsterStatusTracker.getLastMonsterName() );
				Preferences.increment( "camerasUsed" );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			return false;

		case ItemPool.PHOTOCOPIER:

			// You open the lid of the photocopier, press it
			// against your opponent, and press the COPY button. He
			// is enraged, and smashes the copier to pieces, but
			// not before it produces a sheet of paper.

			if ( responseText.contains( "press the COPY button" ) )
			{
				Preferences.setString( "photocopyMonster", MonsterStatusTracker.getLastMonsterName() );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			return false;

		case ItemPool.CRAYON_SHAVINGS:

			// You toss the shavings at the bugbear, and when they hit it,
			// something strange happens -- they begin to move of their own
			// accord, melting, running down the bugbear's body in tiny streams.
			// The streams converge on the ground, forming a puddle. After a
			// moment, the puddle gathers itself up into a perfect wax replica
			// of the bugbear. You pick it up for later investigation.

			if ( responseText.contains( "You toss the shavings" ) )
			{
				Preferences.setString( "waxMonster", MonsterStatusTracker.getLastMonsterName() );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			// You throw the handful of wax shavings at your opponent, gumming up
			// all of his bits and making him smell like a Kindergarten classroom.
			else if ( responseText.contains( "You throw the handful" ) )
			{
				return true;
			}
			return false;

		case ItemPool.ANTIDOTE: // Anti-Anti-Antidote

			// You quickly quaff the anti-anti-antidote. You feel
			// better.

			return responseText.contains( "You quickly quaff" );

		case ItemPool.GLOB_OF_BLANK_OUT:

			// As you're moseying, you notice that the last of the Blank-Out
			// is gone, and that your hand is finally clean. Yay!

			if ( responseText.contains( "your hand is finally clean" ) )
			{
				Preferences.setInteger( "blankOutUsed", 0 );
				return true;
			}
			Preferences.increment( "blankOutUsed" );
			return false;

		case ItemPool.MERKIN_PINKSLIP:

			// You hand him the pinkslip. He reads it, frowns, and
			// swims sulkily away.

			return responseText.contains( "swims sulkily away" );

		case ItemPool.PUMPKIN_BOMB:

			// You light the fuse and toss the pumpkin at your
			// opponent.  After it goes off in a flash of dazzling
			// yellow and flying pumpkin guts, there's nothing left
			// of her but a stain on the ground.

			return responseText.contains( "toss the pumpkin" );

		case ItemPool.PEPPERMINT_PARASOL:

			// You hold up the parasol, and a sudden freak gust of wind
			// sends you hurtling through the air to safety.

			if ( responseText.contains( "sudden freak gust" ) )
			{
				Preferences.increment( "_navelRunaways" );
				Preferences.increment( "parasolUsed" );
			}

			// Man. That last gust was more than your parasol could handle.
			// You throw away the shredded (but delicious-smelling) wreck
			// that was once a useful tool.

			if ( responseText.contains( "You throw away the shredded" ) )
			{
				Preferences.setInteger( "parasolUsed", 0 );
				return true;
			}
			return false;

		default:

			return true;
		}
	}

	private static boolean shouldUseAntidote()
	{
		if ( !KoLConstants.inventory.contains( FightRequest.ANTIDOTE ) )
		{
			return false;
		}
		if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{
			return false;	// can't use items!
		}
		int minLevel = Preferences.getInteger( "autoAntidote" );
		for ( int i = 0; i < KoLConstants.activeEffects.size(); ++i )
		{
			if ( EffectDatabase.getPoisonLevel( ( (AdventureResult) KoLConstants.activeEffects.get( i ) ).getName() ) <= minLevel )
			{
				return true;
			}
		}
		return false;
	}

	private static void payActionCost( final String responseText )
	{
		// If we don't know what we tried, punt now.
		if ( FightRequest.nextAction == null || FightRequest.nextAction.equals( "" ) )
		{
			return;
		}

		switch ( KoLCharacter.getEffectiveFamiliar().getId() )
		{
		case FamiliarPool.BLACK_CAT:
			// If we are adventuring with a Black Cat, she might
			// prevent skill and item use during combat.

			// <Name> jumps onto the keyboard and causes you to
			// accidentally hit the Attack button instead of using
			// that skill.

			if ( responseText.contains( "jumps onto the keyboard" ) )
			{
				FightRequest.nextAction = "attack";
				return;
			}

			// Just as you're about to use that item, <name> bats
			// it out of your hand, and you have to spend the next
			// couple of minutes fishing it out from underneath a
			// couch. It's as adorable as it is annoying.

			if ( responseText.contains( "bats it out of your hand" ) )
			{
				return;
			}
			break;

		case FamiliarPool.OAF:
			// If we are adventuring with a O.A.F., it might
			// prevent skill and item use during combat.

			// Use of that skill has been calculated to be
			// sub-optimal. I recommend that you attack with your
			// weapon, instead.

			// Use of that item has been calculated to be
			// sub-optimal. I recommend that you attack with your
			// weapon, instead.

			if ( responseText.contains( "calculated to be sub-optimal" ) )
			{
				FightRequest.nextAction = "attack";
				return;
			}

			break;
		}

		if ( FightRequest.nextAction.equals( "attack" ) ||
		     FightRequest.nextAction.equals( "runaway" ) ||
		     FightRequest.nextAction.equals( "abort" ) ||
		     FightRequest.nextAction.equals( "steal" ) ||
		     FightRequest.nextAction.equals( "summon ghost" ) ||
		     // If we have Cunctatitis and decide to procrastinate,
		     // we did nothing
		     ( KoLConstants.activeEffects.contains( FightRequest.CUNCTATITIS ) &&
                     responseText.contains( "You decide to" ))
		     )
		{
			return;
		}

		if ( FightRequest.nextAction.equals( "jiggle" ) )
		{
			FightRequest.jiggledChefstaff = true;
			return;
		}

		if ( !FightRequest.nextAction.startsWith( "skill" ) )
		{
			// In Beecore, using a B-item in combat fails. Even if
			// funkslinging with a non-B item, neither item is
			// consumed.

			if ( KoLCharacter.inBeecore() &&
                    responseText.contains( "You are too scared of Bs" ) )
			{
				FightRequest.nextAction = "abort";
				return;
			}

			String item1 = FightRequest.nextAction;
			String item2 = null;

			int commaIndex = item1.indexOf( "," );

			if ( commaIndex != -1 )
			{
				item1 = FightRequest.nextAction.substring( 0, commaIndex );
				item2 = FightRequest.nextAction.substring( commaIndex + 1 );
			}

			FightRequest.payItemCost( StringUtilities.parseInt( item1 ), responseText );

			if ( item2 != null )
			{
				FightRequest.payItemCost( StringUtilities.parseInt( item2 ), responseText );
			}

			return;
		}

		if ( responseText.contains( "You don't have that skill" ) )
		{
			return;
		}

		int skillId = StringUtilities.parseInt( FightRequest.nextAction.substring( 5 ) );
		int mpCost = SkillDatabase.getMPConsumptionById( skillId );

		if ( mpCost > 0 )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );
		}
		SkillDatabase.registerCasts( skillId, 1 );

		// As you're preparing to use that skill, The Bonerdagon
		// suddenly starts furiously beating its wings. You're knocked
		// over by the gust of wind it creates, and lose track of what
		// you're doing.

		if ( responseText.contains( "Bonerdagon suddenly starts furiously beating its wings" ) )
		{
			return;
		}

		switch ( skillId )
		{
		case 49:   // Gothy Handwave
			NemesisDecorator.useGothyHandwave( MonsterStatusTracker.getLastMonsterName(), responseText );
			break;

		case 55:   // Volcanometeor Showeruption
			ResultProcessor.processItem( ItemPool.VOLCANIC_ASH, -1 );
			break;

		case 3004:
			FightRequest.castNoodles = true;
			return;

		case 7024:	// Summon Mayfly Swarm
			if ( responseText.contains( "mayfly bait and swing it" ) )
			{
				Preferences.increment( "_mayflySummons", 1 );
				Preferences.increment( "mayflyExperience",
                        responseText.contains( "mayfly aphrodisiac" ) ? 2 : 1 );
			}
			break;

		case 7038: // Vicious Talon Slash
		case 7039: // All-You-Can-Beat Wing Buffet
			Preferences.increment( "birdformRoc", 1 );
			break;

		case 7040: // Tunnel Upwards
			Preferences.increment( "moleTunnelLevel", 1 );
			break;

		case 7041: // Tunnel Downwards
			Preferences.increment( "moleTunnelLevel", -1 );
			break;

		case 7042: // Rise From Your Ashes
			Preferences.increment( "birdformHot", 1 );
			break;

		case 7043: // Antarctic Flap
			Preferences.increment( "birdformCold", 1 );
			break;

		case 7044: // The Statue Treatment
			Preferences.increment( "birdformStench", 1 );
			break;

		case 7045: // Feast on Carrion
			Preferences.increment( "birdformSpooky", 1 );
			break;

		case 7046: // Give Your Opponent "The Bird"
			Preferences.increment( "birdformSleaze", 1 );
			break;

		case 7050: // Ask the hobo to tell you a joke
			Modifiers.overrideModifier( "fightMods", "Meat Drop: +100" );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
			break;

		case 7051: // Ask the hobo to dance for you
			Modifiers.overrideModifier( "fightMods", "Item Drop: +100" );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
			break;

		case 7108: // Fire a badly romantic arrow
			Preferences.increment( "_badlyRomanticArrows", 1 );
			break;

		case 7109: // Fire a boxing-glove arrow
			Preferences.increment( "_boxingGloveArrows", 1 );
			break;

		case 7110: // Fire a poison arrow
			Preferences.increment( "_poisonArrows", 1 );
			break;

		case 7111: // Fire a fingertrap arrow
			Preferences.increment( "_fingertrapArrows", 1 );
			break;

		case 7113: // Squeeze Stress Ball
			FightRequest.squeezedStressBall = true;
			Preferences.increment( "_stressBallSqueezes", 1 );
			return;

		case 7115: // Release the Boots
			FightRequest.canStomp = false;
			Preferences.increment( "_bootStomps", 1 );
			break;
		case 7117: // Siphon Spirits
			Preferences.increment( "_mediumSiphons", 1 );
			break;
		}
	}

	public static void payItemCost( final int itemId, final String responseText )
	{
		if ( itemId <= 0 )
		{
			return;
		}

		switch ( itemId )
		{
		default:
			break;

		case ItemPool.TOY_SOLDIER:
			// A toy soldier consumes tequila.

			if ( KoLConstants.inventory.contains( FightRequest.TEQUILA ) )
			{
				ResultProcessor.processResult( FightRequest.TEQUILA );
			}
			break;

		case ItemPool.TOY_MERCENARY:
			// A toy mercenary consumes 5-10 meat

			// A sidepane refresh at the end of the battle will
			// re-synch everything.
			break;

		case ItemPool.SHRINKING_POWDER:
			if ( responseText.contains( "gets smaller and angrier" ) )
			{
				MonsterStatusTracker.damageMonster( MonsterStatusTracker.getMonsterHealth() / 2 );
			}
			break;

		case 819: case 820: case 821: case 822: case 823:
		case 824: case 825: case 826: case 827:
			if ( AdventureResult.bangPotionName( itemId ).contains( "healing" ) )
			{
				MonsterStatusTracker.healMonster( 16 );
			}
			break;
		}

		if ( FightRequest.isItemConsumed( itemId, responseText ) )
		{
			ResultProcessor.processResult( new AdventureResult( itemId, -1 ) );
			return;
		}
	}

	@Override
	public int getAdventuresUsed()
	{
		return 0;
	}

	public static String getNextTrackedRound()
	{
		while ( FightRequest.isTrackingFights && !FightRequest.foundNextRound && !KoLmafia.refusesContinue() )
		{
			PAUSER.pause( 200 );
		}

		if ( !FightRequest.foundNextRound || KoLmafia.refusesContinue() )
		{
			FightRequest.isTrackingFights = false;
		}
		else if ( FightRequest.isTrackingFights )
		{
			FightRequest.isTrackingFights = FightRequest.currentRound != 0;
		}

		FightRequest.foundNextRound = false;

		String location = FightRequest.isTrackingFights ? "fight.php?action=script" : "fight.php?action=done";
		String responseText = FightRequest.lastResponseText;
		return RequestEditorKit.getFeatureRichHTML( location, responseText );
	}

	public static boolean inMultiFight()
	{
		return FightRequest.inMultiFight;
	}

	public static int getCurrentRound()
	{
		return FightRequest.currentRound;
	}

	public static int getRoundIndex()
	{
		return FightRequest.currentRound - 1 - FightRequest.preparatoryRounds;
	}

	public static boolean alreadyJiggled()
	{
		return FightRequest.jiggledChefstaff;
	}

	public static void beginTrackingFights()
	{
		FightRequest.isTrackingFights = true;
		FightRequest.foundNextRound = false;
	}

	public static void stopTrackingFights()
	{
		FightRequest.isTrackingFights = false;
		FightRequest.foundNextRound = false;
	}

	public static boolean isTrackingFights()
	{
		return FightRequest.isTrackingFights;
	}

	public static boolean haveFought()
	{
		boolean rv = FightRequest.haveFought;
		FightRequest.haveFought = false;
		return rv;
	}

	public static String getLastMonsterName()
	{
		return MonsterStatusTracker.getLastMonsterName();
	}

	public static int freeRunawayChance()
	{
		// Bandersnatch + Ode = weight/5 free runaways
		if ( KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.BANDER &&
			KoLConstants.activeEffects.contains( ItemDatabase.ODE ) )
		{
			if ( !FightRequest.castCleesh &&
				KoLCharacter.getFamiliar().getModifiedWeight() / 5 >
				Preferences.getInteger( "_banderRunaways" ) )
			{
				return 100;
			}
		}
		// Pair of Stomping Boots = weight/5 free runaways, on the same counter as the Bandersnatch
		else if ( KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.BOOTS )
		{
			if ( KoLCharacter.getFamiliar().getModifiedWeight() / 5 >
			     Preferences.getInteger( "_banderRunaways" ) )
			{
				return 100;
			}
		}
		else if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.NAVEL_RING, 1 ) ) ||
			  KoLCharacter.hasEquipped( ItemPool.get( ItemPool.GREAT_PANTS, 1 ) ) )
		{
			int navelRunaways = Preferences.getInteger( "_navelRunaways" );

			return
				navelRunaways < 3  ? 100 :
					navelRunaways < 6 ? 80 :
						navelRunaways < 9 ? 50 : 20;
		}

		return 0;
	}

	private static void registerMacroAction( Matcher m )
	{	// In the interests of keeping action logging centralized, turn the
		// macro action (indicated via a macroaction: HTML comment) into a
		// fake fight.php URL and call registerRequest on it.

		String action = m.group( 1 );
		if ( action.equals( "attack" ) )
		{
			FightRequest.registerRequest( false, "fight.php?attack" );
		}
		else if ( action.equals( "runaway" ) )
		{
			FightRequest.registerRequest( false, "fight.php?runaway" );
		}
		else if ( action.equals( "steal" ) )
		{
			FightRequest.registerRequest( false, "fight.php?steal" );
		}
		else if ( action.startsWith( "summon" ) )
		{
			FightRequest.registerRequest( false, "fight.php?summon" );
		}
		else if ( action.equals( "chefstaff" ) )
		{
			FightRequest.registerRequest( false, "fight.php?chefstaff" );
		}
		else if ( action.equals( "skill" ) )
		{
			FightRequest.registerRequest( false, "fight.php?whichskill=" + m.group( 2 ) );
		}
		else if ( action.equals( "use" ) )
		{
			String item1 = m.group( 2 );
			String item2 = m.group( 3 );
			if ( item2 == null )
			{
				FightRequest.registerRequest( false, "fight.php?whichitem=" + item1 );
			}
			else
			{
				FightRequest.registerRequest( false, "fight.php?whichitem=" + item1  + "&whichitem2=" + item2 );
			}
		}
		else
		{
			System.out.println( "unrecognized macroaction: " + action );
		}
	}

	public static boolean registerRequest( final boolean isExternal, final String urlString )
	{
		if ( !urlString.startsWith( "fight.php" ) )
		{
			return false;
		}

		FightRequest.nextAction = null;

		if ( urlString.equals( "fight.php" ) || urlString.contains( "ireallymeanit=" ) )
		{
			return true;
		}

		boolean shouldLogAction = Preferences.getBoolean( "logBattleAction" );
		StringBuilder action = new StringBuilder();

		// Begin logging all the different combat actions and storing
		// relevant data for post-processing.

		if ( shouldLogAction )
		{
			action.append( "Round " );
			action.append( FightRequest.currentRound );
			action.append( ": " );
			action.append( KoLCharacter.getUserName() );
			action.append( " " );
		}

		if ( urlString.contains( "macro" ) )
		{
			Matcher m = FightRequest.WHICHMACRO_PATTERN.matcher( urlString );
			if ( m.find() )
			{
				FightRequest.lastMacroUsed = m.group( 1 );
			}
			FightRequest.nextAction = "";
			if ( shouldLogAction )
			{
				action.append( "executes a macro!" );
			}
		}
		else if ( urlString.contains( "runaway" ) )
		{
			FightRequest.nextAction = "runaway";
			if ( shouldLogAction )
			{
				action.append( "casts RETURN!" );
			}
		}
		else if ( urlString.contains( "steal" ) )
		{
			FightRequest.nextAction = "steal";
			if ( shouldLogAction )
			{
				action.append( "tries to steal an item!" );
			}
		}
		else if ( urlString.contains( "attack" ) )
		{
			FightRequest.nextAction = "attack";
			if ( shouldLogAction )
			{
				action.append( "attacks!" );
			}
		}
		else if ( urlString.contains( "summon" ) )
		{
			FightRequest.nextAction = "summon ghost";
			if ( shouldLogAction )
			{
				action.append( "summons " );
				action.append( Preferences.getString( "pastamancerGhostName" ) );
				action.append( " the " );
				action.append( Preferences.getString( "pastamancerGhostType" ) );
				action.append( "!" );
			}
		}
		else if ( urlString.contains( "chefstaff" ) )
		{
			FightRequest.nextAction = "jiggle";
			if ( shouldLogAction )
			{
				action.append( "jiggles the " );
				action.append( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName() );
			}
		}
		else
		{
			Matcher skillMatcher = FightRequest.SKILL_PATTERN.matcher( urlString );
			if ( skillMatcher.find() )
			{
				String skillId = skillMatcher.group( 1 );
				if ( FightRequest.isInvalidAttack( skillId ) )
				{
					return true;
				}

				String skill = SkillDatabase.getSkillName( StringUtilities.parseInt( skillId ) );
				if ( skill == null )
				{
					if ( shouldLogAction )
					{
						action.append( "casts CHANCE!" );
					}
				}
				else
				{
					if ( skillId.equals( "19" ) )
					{
						if ( !KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
						{
							Preferences.setString( "olfactedMonster", MonsterStatusTracker.getLastMonsterName() );
							Preferences.setString( "autoOlfact", "" );
							FightRequest.canOlfact = false;
						}
					}
					else if ( skillId.equals( "7108" ) )
					{
						boolean hasQuake = EquipmentManager.getFamiliarItem().getItemId() == ItemPool.QUAKE_OF_ARROWS;
						int fights = hasQuake ? 3 : 2;
						Preferences.setInteger( "_romanticFightsLeft", fights );
						Preferences.setString( "romanticTarget", MonsterStatusTracker.getLastMonsterName() );

						TurnCounter.stopCounting( "Romantic Monster window begin" );
						TurnCounter.stopCounting( "Romantic Monster window end" );
						TurnCounter.startCounting( 16, "Romantic Monster window begin loc=*", "lparen.gif" );
						TurnCounter.startCounting( 26, "Romantic Monster window end loc=*", "rparen.gif" );
					}
					else if ( skillId.equals( "11020" ) )
					{
						String pref = MonsterStatusTracker.getLastMonsterName();
						String[] monsters = Preferences.getString( "banishingShoutMonsters" ).split( "\\|" );
						for ( int i = 0; i < monsters.length && i < 2; ++i )
						{
							if ( monsters[ i ].length() > 0 )
							{
								pref += "|" + monsters[ i ];
							}
						}
						Preferences.setString( "banishingShoutMonsters", pref );
					}

					FightRequest.nextAction = CombatActionManager.getShortCombatOptionName( "skill " + skill );
					if ( shouldLogAction )
					{
						action.append( "casts " + skill.toUpperCase() + "!" );
					}
				}
			}
			else
			{
				Matcher itemMatcher = FightRequest.ITEM1_PATTERN.matcher( urlString );
				if ( itemMatcher.find() )
				{
					int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
					String item = ItemDatabase.getItemName( itemId );
					if ( item == null )
					{
						if ( shouldLogAction )
						{
							action.append( "plays Garin's Harp" );
						}
					}
					else
					{
						if ( item.equalsIgnoreCase( "odor extractor" ) &&
							!KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
						{
							Preferences.setString( "olfactedMonster",
								MonsterStatusTracker.getLastMonsterName() );
							Preferences.setString( "autoOlfact", "" );
							FightRequest.canOlfact = false;
						}
						FightRequest.nextAction = String.valueOf( itemId );
						if ( shouldLogAction )
						{
							action.append( "uses the " + item );
						}
					}

					itemMatcher = FightRequest.ITEM2_PATTERN.matcher( urlString );
					if ( itemMatcher.find() )
					{
						itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
						item = ItemDatabase.getItemName( itemId );
						if ( item != null )
						{
							if ( item.equalsIgnoreCase( "odor extractor" ) &&
								!KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
							{
								Preferences.setString( "olfactedMonster",
									MonsterStatusTracker.getLastMonsterName() );
								Preferences.setString( "autoOlfact", "" );
							}

							FightRequest.nextAction += "," + String.valueOf( itemId );
							if ( shouldLogAction )
							{
								action.append( " and uses the " + item );
							}
						}
					}

					if ( shouldLogAction )
					{
						action.append( "!" );
					}
				}
				else
				{
					System.out.println( "unable to parse " + urlString );
				}
			}
		}

		if ( shouldLogAction )
		{
			if ( urlString.contains( "[AA]" ) )
			{	// pseudo-parameter for parsing an autoattack
				action.append( " (auto-attack)" );
			}
			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}

	// Log cleaned HTML

	private static void logHTML( final TagNode node )
	{
		if ( node != null )
		{
			StringBuffer buffer = new StringBuffer();
			FightRequest.logHTML( node, buffer, 0 );
		}
	}

	private static void logHTML( final TagNode node, final StringBuffer buffer, int level )
	{
		String name = node.getName();

		// Skip scripts, forms, buttons, and html links
		if ( name.equals( "script" ) ||
		     name.equals( "form" ) ||
		     name.equals( "input" ) ||
		     name.equals( "a" ) ||
		     name.equals( "div" ) )
		{
			return;
		}

		FightRequest.indent( buffer, level );
		FightRequest.printTag( buffer, node );
		RequestLogger.updateDebugLog( buffer.toString() );

		Iterator it = node.getChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof CommentToken )
			{
				CommentToken object = (CommentToken) child;
				String content = object.getContent();
				FightRequest.indent( buffer, level + 1 );
				buffer.append( "<!--" );
				buffer.append( content );
				buffer.append( "-->" );
				RequestLogger.updateDebugLog( buffer.toString() );
				continue;
			}

			if ( child instanceof ContentToken )
			{
				ContentToken object = (ContentToken) child;
				String content = object.getContent().trim();
				if ( content.equals( "" ) )
				{
					continue;
				}

				FightRequest.indent( buffer, level + 1 );
				buffer.append( content );
				RequestLogger.updateDebugLog( buffer.toString() );
				continue;
			}

			if ( child instanceof TagNode )
			{
				TagNode object = (TagNode) child;
				FightRequest.logHTML( object, buffer, level + 1 );
				continue;
			}
		}
	}

	private static void indent( final StringBuffer buffer, int level )
	{
		buffer.setLength( 0 );
		for ( int i = 0; i < level; ++i )
		{
			buffer.append( " " );
			buffer.append( " " );
		}
	}

	private static void printTag( final StringBuffer buffer, TagNode node )
	{
		String name = node.getName();
		Map attributes = node.getAttributes();

		buffer.append( "<" );
		buffer.append( name );

		if ( !attributes.isEmpty() )
		{
			Iterator it = attributes.keySet().iterator();
			while ( it.hasNext() )
			{
				String key = (String) it.next();
				buffer.append( " " );
				buffer.append( key );
				buffer.append( "=\"" );
				buffer.append( (String) attributes.get( key ) );
				buffer.append( "\"" );
			}
		}
		buffer.append( ">" );
	}
}
