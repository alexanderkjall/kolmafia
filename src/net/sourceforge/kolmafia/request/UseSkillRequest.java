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

import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.Speculation;

import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UseSkillRequest
	extends GenericRequest
	implements Comparable
{
	private static final HashMap ALL_SKILLS = new HashMap();
	private static final Pattern SKILLID_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern BOOKID_PATTERN = Pattern.compile( "preaction=(?:summon|combine)([^&]*)" );

	private static final Pattern COUNT1_PATTERN = Pattern.compile( "bufftimes=([\\*\\d,]+)" );
	private static final Pattern COUNT2_PATTERN = Pattern.compile( "quantity=([\\*\\d,]+)" );

	// <p>1 / 50 casts used today.</td>
	private static final Pattern LIMITED_PATTERN = Pattern.compile( "<p>(\\d+) / [\\d]+ casts used today\\.</td>", Pattern.DOTALL );

	public static final String[] BREAKFAST_SKILLS =
	{
		"Advanced Cocktailcrafting",
		"Advanced Saucecrafting",
		"Pastamastery",
		"Summon Crimbo Candy",
		"Lunch Break",
	};

	public static final String[] TOME_SKILLS =
	{
		"Summon Snowcones",
		"Summon Stickers",
		"Summon Sugar Sheets",
		// Summon Clip Art requires extra parameters
		// "Summon Clip Art",
	};

	public static final String[] LIBRAM_SKILLS =
	{
		"Summon Candy Hearts",
		"Summon Party Favor",
		"Summon Love Song",
		"Summon BRICKOs",
		"Summon Dice",
		"Summon Resolutions",
	};

	public static final String[] GRIMOIRE_SKILLS =
	{
		"Summon Hilarious Objects",
		"Summon Tasteful Items",
		"Summon Alice's Army Cards",
	};

	private static final int REQUEST_SANDWICH = 82;
	private static final int OTTER_TONGUE = 1007;
	private static final int WALRUS_TONGUE = 1010;
	private static final int BANDAGES = 3009;
	private static final int COCOON = 3012;
	private static final int DISCO_NAP = 5007;
	private static final int POWER_NAP = 5011;
	private static final int ODE_TO_BOOZE = 6014;
	private static final int THINGFINDER = 6020;
	private static final int BENETTONS = 6021;
	private static final int ELRONS = 6022;
	private static final int COMPANIONSHIP = 6023;
	private static final int PRECISION = 6024;
	private static final int DONHOS = 6026;
	private static final int INIGOS = 6028;
	private static final int DEMAND_SANDWICH = 11021;

	public static String lastUpdate = "";
	public static int lastSkillUsed = -1;
	public static int lastSkillCount = 0;

	private final int skillId;
	private final String skillName;
	private String target;
	private int buffCount;
	private String countFieldId;
	private boolean isRunning;

	private int lastReduction = Integer.MAX_VALUE;
	private String lastStringForm = "";

	public static final AdventureResult[] TAMER_WEAPONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS, 1 ),
		ItemPool.get( ItemPool.CHELONIAN_MORNINGSTAR, 1 ),
		ItemPool.get( ItemPool.MACE_OF_THE_TORTOISE, 1 ),
		ItemPool.get( ItemPool.TURTLE_TOTEM, 1 )
	};
	public static final int[] TAMER_WEAPONS_BONUS = new int[] { 15, 10, 5, 0 };

	public static final AdventureResult[] SAUCE_WEAPONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.WINDSOR_PAN_OF_THE_SOURCE, 1 ),
		ItemPool.get( ItemPool.SEVENTEEN_ALARM_SAUCEPAN, 1 ),
		ItemPool.get( ItemPool.FIVE_ALARM_SAUCEPAN, 1 ),
		ItemPool.get( ItemPool.SAUCEPAN, 1 )
	};
	public static final int[] SAUCE_WEAPONS_BONUS = new int[] { 15, 10, 5, 0 };

	public static final AdventureResult[] THIEF_WEAPONS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.TRICKSTER_TRIKITIXA, 1 ),
		ItemPool.get( ItemPool.SQUEEZEBOX_OF_THE_AGES, 1 ),
		ItemPool.get( ItemPool.ROCK_N_ROLL_LEGEND, 1 ),
		ItemPool.get( ItemPool.CALAVERA_CONCERTINA, 1 ),
		ItemPool.get( ItemPool.STOLEN_ACCORDION, 1 )
	};
	public static final int[] THIEF_WEAPONS_BONUS = new int[] { 15, 10, 5, 2, 0 };

	public static final AdventureResult PLEXI_PENDANT = ItemPool.get( ItemPool.PLEXIGLASS_PENDANT, 1 );
	public static final AdventureResult BRIM_BERET = ItemPool.get( ItemPool.BRIMSTONE_BERET, 1 );
	public static final AdventureResult WIZARD_HAT = ItemPool.get( ItemPool.JEWEL_EYED_WIZARD_HAT, 1 );

	public static final AdventureResult PLEXI_WATCH = ItemPool.get( ItemPool.PLEXIGLASS_POCKETWATCH, 1 );
	public static final AdventureResult BRIM_BRACELET = ItemPool.get( ItemPool.BRIMSTONE_BRACELET, 1 );
	public static final AdventureResult SOLITAIRE = ItemPool.get( ItemPool.STAINLESS_STEEL_SOLITAIRE, 1 );

	public static final AdventureResult NAVEL_RING = ItemPool.get( ItemPool.NAVEL_RING, 1 );
	public static final AdventureResult WIRE_BRACELET = ItemPool.get( ItemPool.WOVEN_BALING_WIRE_BRACELETS, 1 );
	public static final AdventureResult BACON_BRACELET = ItemPool.get( ItemPool.BACONSTONE_BRACELET, 1 );
	public static final AdventureResult BACON_EARRING = ItemPool.get( ItemPool.BACONSTONE_EARRING, 1 );
	public static final AdventureResult SOLID_EARRING = ItemPool.get( ItemPool.SOLID_BACONSTONE_EARRING, 1 );
	public static final AdventureResult EMBLEM_AKGYXOTH = ItemPool.get( ItemPool.EMBLEM_AKGYXOTH, 1 );

	public static final AdventureResult SAUCEBLOB_BELT = ItemPool.get( ItemPool.SAUCEBLOB_BELT, 1 );
	public static final AdventureResult JUJU_MOJO_MASK = ItemPool.get( ItemPool.JUJU_MOJO_MASK, 1 );

	// The following list must contain only accessories!
	private static final AdventureResult[] AVOID_REMOVAL = new AdventureResult[]
	{
		UseSkillRequest.PLEXI_WATCH,	// -3
		UseSkillRequest.BRIM_BRACELET,	// -3
		UseSkillRequest.SOLITAIRE,		// -2
		UseSkillRequest.NAVEL_RING,		// -1
		UseSkillRequest.WIRE_BRACELET,	// -1
		UseSkillRequest.BACON_BRACELET,	// -1, discontinued item
		UseSkillRequest.BACON_EARRING,	// -1
		UseSkillRequest.SOLID_EARRING,	// -1
		UseSkillRequest.EMBLEM_AKGYXOTH,	// -1
		// Removing the following might drop an AT song
		UseSkillRequest.PLEXI_PENDANT,
		// Removing the following may lose a buff
		UseSkillRequest.JUJU_MOJO_MASK,
	};

	// The number of items at the end of AVOID_REMOVAL that are simply
	// there to avoid removal - there's no point in equipping them
	// temporarily during casting:

	private static final int AVOID_REMOVAL_ONLY = 2;

	// Other known MP cost/song count items:
	//
	// wizard hat (-1) - has to be handled specially since it's not an accessory.
	// Vile Vagrant Vestments (-5) - unlikely to be equippable during Ronin.
	// Idol of Ak'gyxoth (-1) - off-hand, would require special handling.
	// Scandalously Skimpy Bikini (4 songs) - custom accessory.
	// Sombrero de Vida (4 songs) - custom hat.

	private UseSkillRequest( final String skillName )
	{
		super( UseSkillRequest.chooseURL( skillName ) );

		this.skillId = SkillDatabase.getSkillId( skillName );
		if ( this.skillId == -1 )
		{
			RequestLogger.printLine( "Unrecognized skill: " + skillName );
			this.skillName = skillName;
		}
		else
		{
			this.skillName = SkillDatabase.getSkillName( this.skillId );
		}
		this.target = "yourself";

		this.addFormFields();
	}

	private static String chooseURL( final String skillName )
	{
		switch ( SkillDatabase.getSkillId( skillName ) )
		{
		case SkillDatabase.SNOWCONE:
		case SkillDatabase.STICKER:
		case SkillDatabase.SUGAR:
		case SkillDatabase.CLIP_ART:
		case SkillDatabase.HILARIOUS:
		case SkillDatabase.TASTEFUL:
		case SkillDatabase.CARDS:
		case SkillDatabase.CANDY_HEART:
		case SkillDatabase.PARTY_FAVOR:
		case SkillDatabase.LOVE_SONG:
		case SkillDatabase.BRICKOS:
		case SkillDatabase.DICE:
		case SkillDatabase.RESOLUTIONS:
			return "campground.php";
		}

		return "skills.php";
	}

	private void addFormFields()
	{
		switch ( this.skillId )
		{
		case SkillDatabase.SNOWCONE:
			this.addFormField( "preaction", "summonsnowcone" );
			break;

		case SkillDatabase.STICKER:
			this.addFormField( "preaction", "summonstickers" );
			break;

		case SkillDatabase.SUGAR:
			this.addFormField( "preaction", "summonsugarsheets" );
			break;

		case SkillDatabase.CLIP_ART:
			this.addFormField( "preaction", "combinecliparts" );
			break;

		case SkillDatabase.HILARIOUS:
			this.addFormField( "preaction", "summonhilariousitems" );
			break;

		case SkillDatabase.TASTEFUL:
			this.addFormField( "preaction", "summonspencersitems" );
			break;

		case SkillDatabase.CARDS:
			this.addFormField( "preaction", "summonaa" );
			break;

		case SkillDatabase.CANDY_HEART:
			this.addFormField( "preaction", "summoncandyheart" );
			break;

		case SkillDatabase.PARTY_FAVOR:
			this.addFormField( "preaction", "summonpartyfavor" );
			break;

		case SkillDatabase.LOVE_SONG:
			this.addFormField( "preaction", "summonlovesongs" );
			break;

		case SkillDatabase.BRICKOS:
			this.addFormField( "preaction", "summonbrickos" );
			break;

		case SkillDatabase.DICE:
			this.addFormField( "preaction", "summongygax" );
			break;

		case SkillDatabase.RESOLUTIONS:
			this.addFormField( "preaction", "summonresolutions" );
			break;

		default:
			this.addFormField( "action", "Skillz." );
			this.addFormField( "whichskill", String.valueOf( this.skillId ) );
			break;
		}
	}

	public void setTarget( final String target )
	{
		if ( SkillDatabase.isBuff( this.skillId ) )
		{
			this.countFieldId = "bufftimes";

			if ( target == null || target.trim().length() == 0 || target.equals( KoLCharacter.getPlayerId() ) || target.equals( KoLCharacter.getUserName() ) )
			{
				this.target = null;
				this.addFormField( "specificplayer", KoLCharacter.getPlayerId() );
			}
			else
			{
				this.target = ContactManager.getPlayerName( target );
				this.addFormField( "specificplayer", ContactManager.getPlayerId( target ) );
			}
		}
		else
		{
			this.countFieldId = "quantity";
			this.target = null;
		}
	}

	public void setBuffCount( int buffCount )
	{
		int mpCost = SkillDatabase.getMPConsumptionById( this.skillId );
		if ( mpCost == 0 )
		{
			this.buffCount = 0;
			return;
		}

		int maxPossible = 0;
		int availableMP = KoLCharacter.getCurrentMP();

		if ( SkillDatabase.isLibramSkill( this.skillId ) )
		{
			maxPossible = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			maxPossible = Math.min( this.getMaximumCast(), availableMP / mpCost );
		}

		if ( buffCount < 1 )
		{
			buffCount += maxPossible;
		}
		else if ( buffCount == Integer.MAX_VALUE )
		{
			buffCount = maxPossible;
		}

		this.buffCount = buffCount;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof UseSkillRequest ) )
		{
			return -1;
		}

		int mpDifference =
			SkillDatabase.getMPConsumptionById( this.skillId ) - SkillDatabase.getMPConsumptionById( ( (UseSkillRequest) o ).skillId );

		return mpDifference != 0 ? mpDifference : this.skillName.compareToIgnoreCase( ( (UseSkillRequest) o ).skillName );
	}

	public int getSkillId()
	{
		return this.skillId;
	}

	public String getSkillName()
	{
		return this.skillName;
	}

	public int getMaximumCast()
	{
		int maximumCast = Integer.MAX_VALUE;

		switch ( this.skillId )
		{
		// The Smile of Mr. A can be used five times per day per Golden
		// Mr. Accessory you own
		case SkillDatabase.SMILE_OF_MR_A:
			maximumCast =
				Preferences.getInteger( "goldenMrAccessories" ) * 5 -
				Preferences.getInteger( "_smilesOfMrA" );
			break;

		// Vent Rage Gland can be used once per day
		case SkillDatabase.RAGE_GLAND:
			maximumCast = Preferences.getBoolean( "rageGlandVented" ) ? 0 : 1;
			break;

		// You can take a Lunch Break once a day
		case SkillDatabase.LUNCH_BREAK:

			maximumCast = Preferences.getBoolean( "_lunchBreak" ) ? 0 : 1;
			break;

		// Summon "Boner Battalion" can be used once per day
		case SkillDatabase.SUMMON_BONERS:
			maximumCast = Preferences.getBoolean( "_bonersSummoned" ) ? 0 : 1;
			break;

		case UseSkillRequest.REQUEST_SANDWICH:
			maximumCast = Preferences.getBoolean( "_requestSandwichSucceeded" ) ? 0 : 1;
			break;

		// Tomes can be used three times per day

		case SkillDatabase.SNOWCONE:
		case SkillDatabase.STICKER:
		case SkillDatabase.SUGAR:
		case SkillDatabase.CLIP_ART:

			maximumCast = Math.max( 3 - Preferences.getInteger( "tomeSummons" ), 0 );
			break;

		// Grimoire items can only be summoned once per day.
		case SkillDatabase.HILARIOUS:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire1Summons" ), 0 );
			break;

		case SkillDatabase.TASTEFUL:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire2Summons" ), 0 );
			break;

		case SkillDatabase.CARDS:

			maximumCast = Math.max( 1 - Preferences.getInteger( "grimoire3Summons" ), 0 );
			break;

		// You can summon Crimbo candy once a day
		case SkillDatabase.CRIMBO_CANDY:

			maximumCast = Math.max( 1 - Preferences.getInteger( "_candySummons" ), 0 );
			break;

		// Rainbow Gravitation can be cast 3 times per day.  Each
		// casting consumes five elemental wads and a twinkly wad

		case SkillDatabase.RAINBOW:
			maximumCast = Math.max( 3 - Preferences.getInteger( "prismaticSummons" ), 0 );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.COLD_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.HOT_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.SLEAZE_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.SPOOKY_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.STENCH_WAD ), maximumCast );
			maximumCast = Math.min( InventoryManager.getCount( ItemPool.TWINKLY_WAD ), maximumCast );
			break;

		// Transcendental Noodlecraft affects # of summons for
		// Pastamastery

		case 3006:

			maximumCast = KoLCharacter.hasSkill( "Transcendental Noodlecraft" ) ? 5 : 3;
			maximumCast = Math.max( maximumCast - Preferences.getInteger( "noodleSummons" ), 0 );
			break;

		// Canticle of Carboloading can be cast once per day.
		case 3024:
			maximumCast = Preferences.getBoolean( "_carboLoaded" ) ? 0 : 1;
			break;

		// The Way of Sauce affects # of summons for Advanced
		// Saucecrafting. So does the Gravyskin Belt of the Sauceblob

		case 4006:

			maximumCast = KoLCharacter.hasSkill( "The Way of Sauce" ) ? 5 : 3;
			if ( KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) &&
			     ( KoLCharacter.hasEquipped( UseSkillRequest.SAUCEBLOB_BELT ) ||
			       UseSkillRequest.SAUCEBLOB_BELT.getCount( KoLConstants.inventory ) > 0 ) )
			{
				maximumCast += 3;
			}
			maximumCast = Math.max( maximumCast - Preferences.getInteger( "reagentSummons" ), 0 );
			break;

		// Superhuman Cocktailcrafting affects # of summons for
		// Advanced Cocktailcrafting

		case 5014:

			maximumCast = KoLCharacter.hasSkill( "Superhuman Cocktailcrafting" ) ? 5 : 3;
			maximumCast = Math.max( maximumCast - Preferences.getInteger( "cocktailSummons" ), 0 );
			break;

		case UseSkillRequest.THINGFINDER:
			maximumCast = Math.max( 10 - Preferences.getInteger( "_thingfinderCasts" ), 0 );
			break;

		case UseSkillRequest.BENETTONS:
			maximumCast = Math.max( 10 - Preferences.getInteger( "_benettonsCasts" ), 0 );
			break;

		case UseSkillRequest.ELRONS:
			maximumCast = Math.max( 10 - Preferences.getInteger( "_elronsCasts" ), 0 );
			break;

		case UseSkillRequest.COMPANIONSHIP:
			maximumCast = Math.max( 10 - Preferences.getInteger( "_companionshipCasts" ), 0 );
			break;

		case UseSkillRequest.PRECISION:
			maximumCast = Math.max( 10 - Preferences.getInteger( "_precisionCasts" ), 0 );
			break;

		case UseSkillRequest.DONHOS:
			maximumCast = Math.max( 50 - Preferences.getInteger( "_donhosCasts" ), 0 );
			break;

		case UseSkillRequest.INIGOS:
			maximumCast = Math.max( 5 - Preferences.getInteger( "_inigosCasts" ), 0 );
			break;

		case UseSkillRequest.DEMAND_SANDWICH:
			maximumCast = Math.max( 3 - Preferences.getInteger( "_demandSandwich" ), 0 );
			break;
		}

		return maximumCast;
	}

	@Override
	public String toString()
	{
		if ( this.lastReduction == KoLCharacter.getManaCostAdjustment() && !SkillDatabase.isLibramSkill( this.skillId ) )
		{
			return this.lastStringForm;
		}

		this.lastReduction = KoLCharacter.getManaCostAdjustment();
		this.lastStringForm = this.skillName + " (" + SkillDatabase.getMPConsumptionById( this.skillId ) + " mp)";
		return this.lastStringForm;
	}

	private static boolean canSwitchToItem( final AdventureResult item )
	{
		return !KoLCharacter.hasEquipped( item ) &&
			EquipmentManager.canEquip( item.getName() ) &&
			InventoryManager.hasItem( item, false );
	}

	public static void optimizeEquipment( final int skillId )
	{
		boolean isBuff = SkillDatabase.isBuff( skillId );

		if ( isBuff )
		{
			if ( skillId > 2000 && skillId < 3000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.TAMER_WEAPONS, skillId );
			}

			if ( skillId > 4000 && skillId < 5000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.SAUCE_WEAPONS, skillId );
			}

			if ( skillId > 6000 && skillId < 7000 )
			{
				UseSkillRequest.prepareWeapon( UseSkillRequest.THIEF_WEAPONS, skillId );
			}
		}

		if ( Preferences.getBoolean( "switchEquipmentForBuffs" ) )
		{
			UseSkillRequest.reduceManaConsumption( skillId, isBuff );
		}
	}

	private static boolean isValidSwitch( final int slotId )
	{
		AdventureResult item = EquipmentManager.getEquipment( slotId );
		if ( item.equals( EquipmentRequest.UNEQUIP ) ) return true;

		for ( int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length; ++i )
		{
			if ( item.equals( UseSkillRequest.AVOID_REMOVAL[ i ] ) )
			{
				return false;
			}
		}

		Speculation spec = new Speculation();
		spec.equip( slotId, EquipmentRequest.UNEQUIP );
		int[] predictions = spec.calculate().predict();
		if ( KoLCharacter.getCurrentMP() > predictions[ Modifiers.BUFFED_MP ] )
		{
			return false;
		}
		if ( KoLCharacter.getCurrentHP() > predictions[ Modifiers.BUFFED_HP ] )
		{
			return false;
		}

		return true;
	}

	private static int attemptSwitch( final int skillId, final AdventureResult item, final boolean slot1Allowed,
		final boolean slot2Allowed, final boolean slot3Allowed )
	{
		if ( slot3Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY3 ) ).run();
			return EquipmentManager.ACCESSORY3;
		}

		if ( slot2Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY2 ) ).run();
			return EquipmentManager.ACCESSORY2;
		}

		if ( slot1Allowed )
		{
			( new EquipmentRequest( item, EquipmentManager.ACCESSORY1 ) ).run();
			return EquipmentManager.ACCESSORY1;
		}

		return -1;
	}

	private static void reduceManaConsumption( final int skillId, final boolean isBuff )
	{
		// Never bother trying to reduce mana consumption when casting
		// ode to booze or a libram skill

		if ( skillId == UseSkillRequest.ODE_TO_BOOZE || SkillDatabase.isLibramSkill( skillId ) )
		{
			return;
		}

		if ( KoLCharacter.canInteract() )
		{
			return;
		}

		// Best switch is a PLEXI_WATCH, since it's a guaranteed -3 to
		// spell cost.

		for ( int i = 0; i < UseSkillRequest.AVOID_REMOVAL.length - AVOID_REMOVAL_ONLY; ++i )
		{
			if ( SkillDatabase.getMPConsumptionById( skillId ) == 1 ||
				KoLCharacter.currentNumericModifier( Modifiers.MANA_COST ) <= -3 )
			{
				return;
			}

			if ( !UseSkillRequest.canSwitchToItem( UseSkillRequest.AVOID_REMOVAL[ i ] ) )
			{
				continue;
			}

			// First determine which slots are available for switching in
			// MP reduction items.  This has do be done inside the loop now
			// that max HP/MP prediction is done, since two changes that are
			// individually harmless might add up to a loss of points.

			boolean slot1Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY1 );
			boolean slot2Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY2 );
			boolean slot3Allowed = UseSkillRequest.isValidSwitch( EquipmentManager.ACCESSORY3 );

			UseSkillRequest.attemptSwitch(
				skillId, UseSkillRequest.AVOID_REMOVAL[ i ], slot1Allowed, slot2Allowed, slot3Allowed );
		}

		if ( UseSkillRequest.canSwitchToItem( UseSkillRequest.WIZARD_HAT ) &&
			!KoLCharacter.hasEquipped( UseSkillRequest.BRIM_BERET ) &&
			UseSkillRequest.isValidSwitch( EquipmentManager.HAT ) )
		{
			( new EquipmentRequest( UseSkillRequest.WIZARD_HAT, EquipmentManager.HAT ) ).run();
		}
	}

	public static int songLimit()
	{
		int rv = 3;
		if ( KoLCharacter.currentBooleanModifier( Modifiers.FOUR_SONGS ) )
		{
			++rv;
		}
		if ( KoLCharacter.currentBooleanModifier( Modifiers.ADDITIONAL_SONG ) )
		{
			++rv;
		}
		return rv;
	}

	@Override
	public void run()
	{
		if ( this.isRunning )
		{
			return;
		}

		UseSkillRequest.lastUpdate = "";

		if ( this.buffCount == 0 )
		{
			// Silently do nothing
			return;
		}

		if ( !KoLCharacter.hasSkill( this.skillName ) )
		{
			UseSkillRequest.lastUpdate = "You don't know how to cast " + this.skillName + ".";
			return;
		}

		UseSkillRequest.optimizeEquipment( this.skillId );

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		this.isRunning = true;
		this.setBuffCount( Math.min( this.buffCount, this.getMaximumCast() ) );
		this.useSkillLoop();
		this.isRunning = false;
	}

	private void useSkillLoop()
	{
		if ( KoLmafia.refusesContinue() )
		{
			return;
		}

		// Before executing the skill, ensure that all necessary mana is
		// recovered in advance.

		int castsRemaining = this.buffCount;

		int maximumMP = KoLCharacter.getMaximumMP();
		int mpPerCast = SkillDatabase.getMPConsumptionById( this.skillId );
		int maximumCast = maximumMP / mpPerCast;

		// Save name so we can guarantee correct target later

		String originalTarget = this.target;

		while ( !KoLmafia.refusesContinue() && castsRemaining > 0 )
		{
			if ( SkillDatabase.isLibramSkill( this.skillId ) )
			{
				mpPerCast = SkillDatabase.getMPConsumptionById( this.skillId );
			}

			if ( maximumMP < mpPerCast )
			{
				UseSkillRequest.lastUpdate = "Your maximum mana is too low to cast " + this.skillName + ".";
				KoLmafia.updateDisplay( UseSkillRequest.lastUpdate );
				return;
			}

			// Find out how many times we can cast with current MP

			int currentCast = this.availableCasts( castsRemaining, mpPerCast );

			// If none, attempt to recover MP in order to cast;
			// take auto-recovery into account.
			// Also recover MP if an opera mask is worn, to maximize its benefit.
			// (That applies only to AT buffs, but it's unlikely that an opera mask
			// will be worn at any other time than casting one.)
			boolean needExtra = currentCast < maximumCast && currentCast < castsRemaining &&
				EquipmentManager.getEquipment( EquipmentManager.HAT ).getItemId() == ItemPool.OPERA_MASK;

			if ( currentCast == 0 || needExtra )
			{
				currentCast = Math.min( castsRemaining, maximumCast );
				int currentMP = KoLCharacter.getCurrentMP();

				int recoverMP = mpPerCast * currentCast;

				SpecialOutfit.createImplicitCheckpoint();
				if ( MoodManager.isExecuting() )
				{
					recoverMP = Math.min( Math.max( recoverMP, MoodManager.getMaintenanceCost() ), maximumMP );
				}
				RecoveryManager.recoverMP( recoverMP  );
				SpecialOutfit.restoreImplicitCheckpoint();

				// If no change occurred, that means the person
				// was unable to recover MP; abort the process.

				if ( currentMP == KoLCharacter.getCurrentMP() )
				{
					UseSkillRequest.lastUpdate = "Could not restore enough mana to cast " + this.skillName + ".";
					KoLmafia.updateDisplay( UseSkillRequest.lastUpdate );
					return;
				}

				currentCast = this.availableCasts( castsRemaining, mpPerCast );
			}

			if ( KoLmafia.refusesContinue() )
			{
				UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
				return;
			}

			// If this happens to be a health-restorative skill,
			// then there is an effective cap based on how much
			// the skill is able to restore.

			switch ( this.skillId )
			{
			case OTTER_TONGUE:
			case WALRUS_TONGUE:
			case DISCO_NAP:
			case POWER_NAP:
			case BANDAGES:
			case COCOON:

				int healthRestored = HPRestoreItemList.getHealthRestored( this.skillName );
				int maxPossible = Math.max( 1, ( KoLCharacter.getMaximumHP() - KoLCharacter.getCurrentHP() ) / healthRestored );
				castsRemaining = Math.min( castsRemaining, maxPossible );
				currentCast = Math.min( currentCast, castsRemaining );
				break;
			}

			currentCast = Math.min( currentCast, maximumCast );

			if ( currentCast > 0 )
			{
				// Attempt to cast the buff.

				this.buffCount = currentCast;
				UseSkillRequest.optimizeEquipment( this.skillId );

				if ( KoLmafia.refusesContinue() )
				{
					UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
					return;
				}

				this.setTarget( originalTarget );

				this.addFormField( this.countFieldId, String.valueOf( currentCast ), false );

				if ( this.target == null || this.target.trim().length() == 0 )
				{
					KoLmafia.updateDisplay( "Casting " + this.skillName + " " + currentCast + " times..." );
				}
				else
				{
					KoLmafia.updateDisplay( "Casting " + this.skillName + " on " + this.target + " " + currentCast + " times..." );
				}

				super.run();

				// Otherwise, you have completed the correct
				// number of casts.  Deduct it from the number
				// of casts remaining and continue.

				castsRemaining -= currentCast;
			}
		}

		if ( KoLmafia.refusesContinue() )
		{
			UseSkillRequest.lastUpdate = "Error encountered during cast attempt.";
		}
	}

	public final int availableCasts( int maxCasts, int mpPerCast )
	{
		int availableMP = KoLCharacter.getCurrentMP();
		int currentCast = 0;

		if ( SkillDatabase.isLibramSkill( this.skillId ) )
		{
			currentCast = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			currentCast = availableMP / mpPerCast;
			currentCast = Math.min( this.getMaximumCast(), currentCast );
		}

		currentCast = Math.min( maxCasts, currentCast );

		return currentCast;
	}

	public static boolean hasAccordion()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.THIEF_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.THIEF_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static boolean hasTotem()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.TAMER_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.TAMER_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static boolean hasSaucepan()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}

		for ( int i = 0; i < UseSkillRequest.SAUCE_WEAPONS.length; ++i )
		{
			if ( InventoryManager.hasItem( UseSkillRequest.SAUCE_WEAPONS[ i ], true ) )
			{
				return true;
			}
		}

		return false;
	}

	public static void prepareWeapon( final AdventureResult[] options, int skillId )
	{
		if ( KoLCharacter.canInteract() )
		{
			// The first weapon is a quest item: the reward for
			// finally defeating your Nemesis
			if ( InventoryManager.hasItem( options[ 0 ], false ) )
			{
				if ( !KoLCharacter.hasEquipped( options[ 0 ] ) )
				{
					InventoryManager.retrieveItem( options[ 0 ] );
				}

				return;
			}

			// The second weapon is a quest item: the Legendary
			// Epic Weapon of the class
			if ( InventoryManager.hasItem( options[ 1 ], false ) )
			{
				if ( !KoLCharacter.hasEquipped( options[ 1 ] ) )
				{
					InventoryManager.retrieveItem( options[ 1 ] );
				}

				return;
			}

			// The third weapon is tradeable: the Epic Weapon of
			// the class
			if ( InventoryManager.hasItem( options[ 2 ], false ) )
			{
				if ( !KoLCharacter.hasEquipped( options[ 2 ] ) )
				{
					InventoryManager.retrieveItem( options[ 2 ] );
				}

				return;
			}

			// Otherwise, obtain the Epic Weapon
			InventoryManager.retrieveItem( options[ 2 ] );
			return;
		}

		// Check for the weakest equipped item

		AdventureResult equippedItem = null;

		for ( int i = options.length - 1; i >= 0; --i )
		{
			if ( KoLCharacter.hasEquipped( options[ i ] ) )
			{
				equippedItem = options[ i ];
				break;
			}
		}

		// Check for the strongest available item

		for ( int i = 0; i < options.length; ++i )
		{
			if ( !InventoryManager.hasItem( options[ i ], false ) )
			{
				continue;
			}

			if ( equippedItem != null && options[ i ] != equippedItem )
			{
				( new EquipmentRequest( EquipmentRequest.UNEQUIP,
					EquipmentManager.WEAPON ) ).run();
			}

			if ( !KoLCharacter.hasEquipped( options[ i ] ) )
			{
				InventoryManager.retrieveItem( options[ i ] );
			}

			return;
		}

		// Nothing available, try to retrieve the weakest item

		InventoryManager.retrieveItem( options[ options.length - 1 ] );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return false;
	}

	@Override
	protected boolean processOnFailure()
	{
		return true;
	}

	@Override
	public void processResults()
	{
		UseSkillRequest.lastUpdate = "";

		boolean shouldStop = UseSkillRequest.parseResponse( this.getURLString(), this.responseText );

		if ( !UseSkillRequest.lastUpdate.equals( "" ) )
		{
			MafiaState state = shouldStop ? MafiaState.ABORT : MafiaState.CONTINUE;
			KoLmafia.updateDisplay( state, UseSkillRequest.lastUpdate );

			if ( BuffBotHome.isBuffBotActive() )
			{
				BuffBotHome.timeStampedLogEntry( BuffBotHome.ERRORCOLOR, UseSkillRequest.lastUpdate );
			}

			return;
		}

		if ( this.target == null )
		{
			KoLmafia.updateDisplay( this.skillName + " was successfully cast." );
		}
		else
		{
			KoLmafia.updateDisplay( this.skillName + " was successfully cast on " + this.target + "." );
		}
	}

	@Override
	public boolean equals( final Object o )
	{
		return o != null && o instanceof UseSkillRequest && this.getSkillName().equals(
			( (UseSkillRequest) o ).getSkillName() );
	}

	public static UseSkillRequest getInstance( final int skillId )
	{
		return UseSkillRequest.getInstance( SkillDatabase.getSkillName( skillId ) );
	}

	public static UseSkillRequest getInstance( final String skillName, final Concoction conc )
	{
		return UseSkillRequest.getInstance( skillName, KoLCharacter.getUserName(), 1, conc );
	}

	public static UseSkillRequest getInstance( final String skillName, final int buffCount )
	{
		return UseSkillRequest.getInstance( skillName, KoLCharacter.getUserName(), buffCount, null );
	}

	public static UseSkillRequest getInstance( final String skillName, final String target, final int buffCount )
	{
		return UseSkillRequest.getInstance( skillName, target, buffCount, null );
	}

	public static UseSkillRequest getInstance( final String skillName, final String target, final int buffCount, final Concoction conc )
	{
		UseSkillRequest instance = UseSkillRequest.getInstance( skillName );
		if ( instance == null )
		{
			return null;
		}

		instance.setTarget( target == null || target.equals( "" ) ? KoLCharacter.getUserName() : target );
		instance.setBuffCount( buffCount );

		// Clip Art request
		if ( conc != null )
		{
			int clip1 = ( conc.getParam() >> 16 ) & 0xFF;
			int clip2 = ( conc.getParam() >> 8  ) & 0xFF;
			int clip3 = conc.getParam() & 0xFF;

			instance.addFormField( "clip1", String.valueOf( clip1 ) );
			instance.addFormField( "clip2", String.valueOf( clip2 ) );
			instance.addFormField( "clip3", String.valueOf( clip3 ) );
		}

		return instance;
	}

	public static UseSkillRequest getUnmodifiedInstance( String skillName )
	{
		if ( skillName == null || !SkillDatabase.contains( skillName ) )
		{
			return null;
		}

		skillName = StringUtilities.getCanonicalName( skillName );
		UseSkillRequest request = (UseSkillRequest) UseSkillRequest.ALL_SKILLS.get( skillName );
		if ( request == null )
		{
			request = new UseSkillRequest( skillName );
			UseSkillRequest.ALL_SKILLS.put( skillName, request );
		}

		return request;
	}

	public static UseSkillRequest getInstance( String skillName )
	{
		if ( skillName == null || !SkillDatabase.contains( skillName ) )
		{
			return null;
		}

		skillName = StringUtilities.getCanonicalName( skillName );
		UseSkillRequest request = (UseSkillRequest) UseSkillRequest.ALL_SKILLS.get( skillName );
		if ( request == null )
		{
			request = new UseSkillRequest( skillName );
			UseSkillRequest.ALL_SKILLS.put( skillName, request );
		}

		request.setTarget( KoLCharacter.getUserName() );
		request.setBuffCount( 0 );
		return request;
	}

	public static boolean parseResponse( final String urlString, final String responseText )
	{
		int skillId = UseSkillRequest.lastSkillUsed;
		int count = UseSkillRequest.lastSkillCount;

		if ( skillId == -1 )
		{
			UseSkillRequest.lastUpdate = "Skill ID not saved.";
			return false;
		}

		UseSkillRequest.lastSkillUsed = -1;
		UseSkillRequest.lastSkillCount = 0;

		if ( responseText == null || responseText.trim().length() == 0 )
		{
			int initialMP = KoLCharacter.getCurrentMP();
			new ApiRequest().run();

			if ( initialMP == KoLCharacter.getCurrentMP() )
			{
				UseSkillRequest.lastUpdate = "Encountered lag problems.";
				return false;
			}

			UseSkillRequest.lastUpdate = "KoL sent back a blank response, but consumed MP.";
			return true;
		}

		if ( responseText.contains( "You don't have that skill" ) )
		{
			UseSkillRequest.lastUpdate = "That skill is unavailable.";
			return true;
		}

		if ( responseText.contains( "You may only use three Tome summonings each day" ) )
		{
			UseSkillRequest.lastUpdate = "You've used your Tomes enough today.";
			Preferences.setInteger( "tomeSummons", 3 );
			ConcoctionDatabase.setRefreshNeeded( true );
			return true;
		}

		// Summon Clip Art cast through the browser has two phases:
		//
		//   campground.php?preaction=summoncliparts
		//   campground.php?preaction=combinecliparts
		//
		// Only the second once consumes MP and only if it is successful.
		// Internally, we use only the second URL.
		//
		// For now, simply ignore any call on either URL that doesn't
		// result in an item, since failures just redisplay the bookshelf

		if ( skillId == SkillDatabase.CLIP_ART && !responseText.contains( "You acquire" ) )
		{
			return false;
		}

		if ( responseText.contains( "too many songs" ) )
		{
			UseSkillRequest.lastUpdate = "Selected target has the maximum number of AT buffs already.";
			return false;
		}

		if ( responseText.contains( "casts left of the Smile of Mr. A" ) )
		{
			UseSkillRequest.lastUpdate = "You cannot cast that many smiles.";
			return false;
		}

		if ( responseText.contains( "Invalid target player" ) )
		{
			UseSkillRequest.lastUpdate = "Selected target is not a valid target.";
			return true;
		}

		// You can't cast that spell on persons who are lower than
		// level 15, like <name>, who is level 13.
		if ( responseText.contains( "lower than level" ) )
		{
			UseSkillRequest.lastUpdate = "Selected target is too low level.";
			return false;
		}

		if ( responseText.contains( "busy fighting" ) )
		{
			UseSkillRequest.lastUpdate = "Selected target is busy fighting.";
			return false;
		}

		if ( responseText.contains( "receive buffs" ) )
		{
			UseSkillRequest.lastUpdate = "Selected target cannot receive buffs.";
			return false;
		}

		if ( responseText.contains( "You need" ) )
		{
			UseSkillRequest.lastUpdate = "You need special equipment to cast that buff.";
			return true;
		}

		if ( responseText.contains( "You can't remember how to use that skill" ) )
		{
			UseSkillRequest.lastUpdate = "That skill is currently unavailable.";
			return true;
		}

		if ( responseText.contains( "You can't cast this spell because you are not an Accordion Thief" ) )
		{
			UseSkillRequest.lastUpdate = "Only Accordion Thieves can use that skill.";
			return true;
		}

		// You think your stomach has had enough for one day.
		if ( responseText.contains( "enough for one day" ) )
		{
			UseSkillRequest.lastUpdate = "You can only do that once a day.";
			Preferences.setBoolean( "_carboLoaded", true );
			return false;
		}

		// You can't cast that many turns of that skill today. (You've used 5 casts today,
		// and the limit of casts per day you have is 5.)
		if ( responseText.contains( "You can't cast that many turns of that skill today" ) )
		{
			UseSkillRequest.lastUpdate = "You've reached your daily casting limit for that skill.";
			switch ( skillId )
			{
			case UseSkillRequest.THINGFINDER:
				Preferences.setInteger( "_thingfinderCasts", 10 );
				break;

			case UseSkillRequest.BENETTONS:
				Preferences.setInteger( "_benettonsCasts", 10 );
				break;

			case UseSkillRequest.ELRONS:
				Preferences.setInteger( "_elronsCasts", 10 );
				break;

			case UseSkillRequest.COMPANIONSHIP:
				Preferences.setInteger( "_companionshipCasts", 10 );
				break;

			case UseSkillRequest.PRECISION:
				Preferences.setInteger( "_precisionCasts", 10 );
				break;

			case UseSkillRequest.DONHOS:
				Preferences.setInteger( "_donhosCasts", 50 );
				break;

			case UseSkillRequest.INIGOS:
				Preferences.setInteger( "_inigosCasts", 5 );
				break;
			default:
				break;
			}
			return false;
		}

		Matcher limitedMatcher = UseSkillRequest.LIMITED_PATTERN.matcher( responseText );
		// limited-use skills
		// "Y / maxCasts casts used today."
		if ( limitedMatcher.find() )
		{
			int casts = 0;
			// parse the number of casts remaining and set the appropriate preference.

			String numString = limitedMatcher.group( 1 );

			casts = Integer.parseInt( numString );

			switch ( skillId )
			{
			case UseSkillRequest.THINGFINDER:
				Preferences.setInteger( "_thingfinderCasts", casts );
				break;

			case UseSkillRequest.BENETTONS:
				Preferences.setInteger( "_benettonsCasts", casts );
				break;

			case UseSkillRequest.ELRONS:
				Preferences.setInteger( "_elronsCasts", casts );
				break;

			case UseSkillRequest.COMPANIONSHIP:
				Preferences.setInteger( "_companionshipCasts", casts );
				break;

			case UseSkillRequest.PRECISION:
				Preferences.setInteger( "_precisionCasts", casts );
				break;

			case UseSkillRequest.DONHOS:
				Preferences.setInteger( "_donhosCasts", casts );
				break;

			case UseSkillRequest.INIGOS:
				Preferences.setInteger( "_inigosCasts", casts );
				break;
			}
		}

		if ( responseText.contains( "You don't have enough" ) )
		{
			String skillName = SkillDatabase.getSkillName( skillId );

			UseSkillRequest.lastUpdate = "Not enough mana to cast " + skillName + ".";
			new ApiRequest().run();
			return true;
		}

		// The skill was successfully cast. Deal with its effects.
		if ( responseText.contains( "tear the opera mask" ) )
		{
			EquipmentManager.breakEquipment( ItemPool.OPERA_MASK,
				"Your opera mask shattered." );
		}

		int mpCost = SkillDatabase.getMPConsumptionById( skillId ) * count;

		if ( responseText.contains( "You can only conjure" ) ||
                responseText.contains( "You can only scrounge up" ) ||
                responseText.contains( "You can only summon" ) )
		{
			UseSkillRequest.lastUpdate = "Summon limit exceeded.";

			// We're out of sync with the actual number of times
			// this skill has been cast.  Adjust the counter by 1
			// at a time.
			count = 1;
			mpCost = 0;
		}

		switch ( skillId )
		{
		case UseSkillRequest.ODE_TO_BOOZE:
			ConcoctionDatabase.getUsables().sort();
			break;

		case UseSkillRequest.OTTER_TONGUE:
		case UseSkillRequest.WALRUS_TONGUE:
		case UseSkillRequest.DISCO_NAP:
		case UseSkillRequest.POWER_NAP:
			UneffectRequest.removeEffectsWithSkill( skillId );
			break;

		case SkillDatabase.SMILE_OF_MR_A:
			Preferences.increment( "_smilesOfMrA" );
			break;

		case SkillDatabase.RAGE_GLAND:
			Preferences.setBoolean( "rageGlandVented", true );
			break;

		case SkillDatabase.RAINBOW:

			// Each cast of Rainbow Gravitation consumes five
			// elemental wads and a twinkly wad

			ResultProcessor.processResult( ItemPool.get( ItemPool.COLD_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.HOT_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.SLEAZE_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.SPOOKY_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.STENCH_WAD, -count ) );
			ResultProcessor.processResult( ItemPool.get( ItemPool.TWINKLY_WAD, -count ) );

			Preferences.increment( "prismaticSummons", count );
			break;

		case SkillDatabase.LUNCH_BREAK:
			Preferences.setBoolean( "_lunchBreak", true );
			break;

		case SkillDatabase.SUMMON_BONERS:
			Preferences.setBoolean( "_bonersSummoned", true );
			break;

		case UseSkillRequest.REQUEST_SANDWICH:
			// You take a deep breath and prepare for a Boris-style bellow. Then you remember your manners 
			// and shout, "If it's not too much trouble, I'd really like a sandwich right now! Please!" 
			// To your surprise, it works! Someone wanders by slowly and hands you a sandwich, grumbling, 
			// "well, since you asked nicely . . ."
			if ( responseText.contains( "well, since you asked nicely" ) )
			{
				Preferences.setBoolean( "_requestSandwichSucceeded", true );
			}
			break;

		case 3006:
			Preferences.increment( "noodleSummons", count );
			break;

		case 3024:
			Preferences.setBoolean( "_carboLoaded", true );
			Preferences.increment( "carboLoading", 1 );
			break;

		case 4006:
			Preferences.increment( "reagentSummons", count );
			break;

		case 5014:
			Preferences.increment( "cocktailSummons", count );
			break;

		case UseSkillRequest.DEMAND_SANDWICH:
			Preferences.increment( "_demandSandwich", count );
			break;

		case SkillDatabase.SNOWCONE:
		case SkillDatabase.STICKER:
		case SkillDatabase.SUGAR:
		case SkillDatabase.CLIP_ART:
			Preferences.increment( "tomeSummons", count );
			ConcoctionDatabase.setRefreshNeeded( false );
			break;

		case SkillDatabase.HILARIOUS:
			Preferences.increment( "grimoire1Summons", 1 );
			break;

		case SkillDatabase.TASTEFUL:
			Preferences.increment( "grimoire2Summons", 1 );
			break;

		case SkillDatabase.CARDS:
			Preferences.increment( "grimoire3Summons", 1 );
			break;

		case SkillDatabase.CRIMBO_CANDY:
			Preferences.increment( "_candySummons", 1 );
			break;

		case SkillDatabase.BRICKOS:
			if ( responseText.contains( "BRICKO eye brick" ) )
			{
				Preferences.increment( "_brickoEyeSummons", 1 );
			}
		case SkillDatabase.CANDY_HEART:
		case SkillDatabase.PARTY_FAVOR:
		case SkillDatabase.LOVE_SONG:
		case SkillDatabase.DICE:
		case SkillDatabase.RESOLUTIONS:
			int cast = Preferences.getInteger( "libramSummons" );
			mpCost = SkillDatabase.libramSkillMPConsumption( cast + 1, count );
			Preferences.increment( "libramSummons", count );
			KoLConstants.summoningSkills.sort();
			KoLConstants.usableSkills.sort();
			break;
		}

		ResultProcessor.processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );

		return false;
	}

	private static int getSkillId( final String urlString )
	{
		Matcher skillMatcher = UseSkillRequest.SKILLID_PATTERN.matcher( urlString );
		if ( skillMatcher.find() )
		{
			return StringUtilities.parseInt( skillMatcher.group( 1 ) );
		}

		skillMatcher = UseSkillRequest.BOOKID_PATTERN.matcher( urlString );
		if ( !skillMatcher.find() )
		{
			return -1;
		}

		String action = skillMatcher.group( 1 );

		if ( action.equals( "snowcone" ) )
		{
			return SkillDatabase.SNOWCONE;
		}

		if ( action.equals( "stickers" ) )
		{
			return SkillDatabase.STICKER;
		}

		if ( action.equals( "sugarsheets" ) )
		{
			return SkillDatabase.SUGAR;
		}

		if ( action.equals( "cliparts" ) )
		{
			if ( !urlString.contains( "clip3=" ) )
			{
				return -1;
			}

			return SkillDatabase.CLIP_ART;
		}

		if ( action.equals( "hilariousitems" ) )
		{
			return SkillDatabase.HILARIOUS;
		}

		if ( action.equals( "spencersitems" ) )
		{
			return	SkillDatabase.TASTEFUL;
		}

		if ( action.equals( "aa" ) )
		{
			return	SkillDatabase.CARDS;
		}

		if ( action.equals( "candyheart" ) )
		{
			return SkillDatabase.CANDY_HEART;
		}

		if ( action.equals( "partyfavor" ) )
		{
			return SkillDatabase.PARTY_FAVOR;
		}

		if ( action.equals( "lovesongs" ) )
		{
			return SkillDatabase.LOVE_SONG;
		}

		if ( action.equals( "brickos" ) )
		{
			return SkillDatabase.BRICKOS;
		}

		if ( action.equals( "gygax" ) )
		{
			return SkillDatabase.DICE;
		}

		if ( action.equals( "resolutions" ) )
		{
			return SkillDatabase.RESOLUTIONS;
		}

		return -1;
	}

	private static int getCount( final String urlString, int skillId )
	{
		Matcher countMatcher = UseSkillRequest.COUNT1_PATTERN.matcher( urlString );

		if ( !countMatcher.find() )
		{
			countMatcher = UseSkillRequest.COUNT2_PATTERN.matcher( urlString );
			if ( !countMatcher.find() )
			{
				return 1;
			}
		}

		int availableMP = KoLCharacter.getCurrentMP();
		int maxcasts;
		if ( SkillDatabase.isLibramSkill( skillId ) )
		{
			maxcasts = SkillDatabase.libramSkillCasts( availableMP );
		}
		else
		{
			int MP = SkillDatabase.getMPConsumptionById( skillId );
			maxcasts = MP == 0 ? 1 : availableMP / MP;
		}

		if ( countMatcher.group( 1 ).startsWith( "*" ) )
		{
			return maxcasts;
		}

		return Math.min( maxcasts, StringUtilities.parseInt( countMatcher.group( 1 ) ) );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) && !urlString.startsWith( "skills.php" ) )
		{
			return false;
		}

		int skillId = UseSkillRequest.getSkillId( urlString );
                // Quick skills has (select a skill) with ID = 999
		if ( skillId == -1 || skillId == 999 )
		{
			return false;
		}

		int count = UseSkillRequest.getCount( urlString, skillId );
		String skillName = SkillDatabase.getSkillName( skillId );

		UseSkillRequest.lastSkillUsed = skillId;
		UseSkillRequest.lastSkillCount = count;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "cast " + count + " " + skillName );
		
		SkillDatabase.registerCasts( skillId, count );

		return true;
	}
}
