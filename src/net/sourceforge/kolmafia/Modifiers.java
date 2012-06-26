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
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.swingui.MaximizerFrame;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Modifiers
	extends KoLDatabase
{
	private static final HashMap modifiersByName = new HashMap();
	private static final HashMap<String,String> familiarEffectByName = new HashMap<String,String>();
	private static final ArrayList<UseSkillRequest> passiveSkills = new ArrayList<UseSkillRequest>();
	private static final ArrayList synergies = new ArrayList();
	private static final ArrayList<String> mutexes = new ArrayList<String>();
	public static String currentLocation = "";
	public static String currentZone = "";
	public static float currentML = 4.0f;
	public static String currentFamiliar = "";
	public static String mainhandClass = "";
	public static float hoboPower = 0.0f;
	public static float currentWeight = 0.0f;
	public static boolean unarmed = false;

	private static final Pattern FAMILIAR_EFFECT_PATTERN =
		Pattern.compile( "Familiar Effect: \"(.*?)\"" );
	private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN =
		Pattern.compile( "([\\d.]+)\\s*x\\s*(Volley|Somb|Lep|Fairy)" );
	private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT = "$2: $1 ";
	private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN2 =
		Pattern.compile( "cap ([\\d.]+)" );
	private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 = "Familiar Weight Cap: $1 ";

	public static final int FAMILIAR_WEIGHT = 0;
	public static final int MONSTER_LEVEL = 1;
	public static final int COMBAT_RATE = 2;
	public static final int INITIATIVE = 3;
	public static final int EXPERIENCE = 4;
	public static final int ITEMDROP = 5;
	public static final int MEATDROP = 6;
	public static final int DAMAGE_ABSORPTION = 7;
	public static final int DAMAGE_REDUCTION = 8;
	public static final int COLD_RESISTANCE = 9;
	public static final int HOT_RESISTANCE = 10;
	public static final int SLEAZE_RESISTANCE = 11;
	public static final int SPOOKY_RESISTANCE = 12;
	public static final int STENCH_RESISTANCE = 13;
	public static final int MANA_COST = 14;
	public static final int MOX = 15;
	public static final int MOX_PCT = 16;
	public static final int MUS = 17;
	public static final int MUS_PCT = 18;
	public static final int MYS = 19;
	public static final int MYS_PCT = 20;
	public static final int HP = 21;
	public static final int HP_PCT = 22;
	public static final int MP = 23;
	public static final int MP_PCT = 24;
	public static final int WEAPON_DAMAGE = 25;
	public static final int RANGED_DAMAGE = 26;
	public static final int SPELL_DAMAGE = 27;
	public static final int SPELL_DAMAGE_PCT = 28;
	public static final int COLD_DAMAGE = 29;
	public static final int HOT_DAMAGE = 30;
	public static final int SLEAZE_DAMAGE = 31;
	public static final int SPOOKY_DAMAGE = 32;
	public static final int STENCH_DAMAGE = 33;
	public static final int COLD_SPELL_DAMAGE = 34;
	public static final int HOT_SPELL_DAMAGE = 35;
	public static final int SLEAZE_SPELL_DAMAGE = 36;
	public static final int SPOOKY_SPELL_DAMAGE = 37;
	public static final int STENCH_SPELL_DAMAGE = 38;
	public static final int CRITICAL = 39;
	public static final int FUMBLE = 40;
	public static final int HP_REGEN_MIN = 41;
	public static final int HP_REGEN_MAX = 42;
	public static final int MP_REGEN_MIN = 43;
	public static final int MP_REGEN_MAX = 44;
	public static final int ADVENTURES = 45;
	public static final int FAMILIAR_WEIGHT_PCT = 46;
	public static final int WEAPON_DAMAGE_PCT = 47;
	public static final int RANGED_DAMAGE_PCT = 48;
	public static final int STACKABLE_MANA_COST = 49;
	public static final int HOBO_POWER = 50;
	public static final int BASE_RESTING_HP = 51;
	public static final int RESTING_HP_PCT = 52;
	public static final int BONUS_RESTING_HP = 53;
	public static final int BASE_RESTING_MP = 54;
	public static final int RESTING_MP_PCT = 55;
	public static final int BONUS_RESTING_MP = 56;
	public static final int CRITICAL_PCT = 57;
	public static final int PVP_FIGHTS = 58;
	public static final int VOLLEYBALL_WEIGHT = 59;
	public static final int SOMBRERO_WEIGHT = 60;
	public static final int LEPRECHAUN_WEIGHT = 61;
	public static final int FAIRY_WEIGHT = 62;
	public static final int MEATDROP_PENALTY = 63;
	public static final int HIDDEN_FAMILIAR_WEIGHT = 64;
	public static final int ITEMDROP_PENALTY = 65;
	public static final int INITIATIVE_PENALTY = 66;
	public static final int FOODDROP = 67;
	public static final int BOOZEDROP = 68;
	public static final int HATDROP = 69;
	public static final int WEAPONDROP = 70;
	public static final int OFFHANDDROP = 71;
	public static final int SHIRTDROP = 72;
	public static final int PANTSDROP = 73;
	public static final int ACCESSORYDROP = 74;
	public static final int VOLLEYBALL_EFFECTIVENESS = 75;
	public static final int SOMBRERO_EFFECTIVENESS = 76;
	public static final int LEPRECHAUN_EFFECTIVENESS = 77;
	public static final int FAIRY_EFFECTIVENESS = 78;
	public static final int FAMILIAR_WEIGHT_CAP = 79;
	public static final int SLIME_RESISTANCE = 80;
	public static final int SLIME_HATES_IT = 81;
	public static final int SPELL_CRITICAL_PCT = 82;
	public static final int MUS_EXPERIENCE = 83;
	public static final int MYS_EXPERIENCE = 84;
	public static final int MOX_EXPERIENCE = 85;
	public static final int EFFECT_DURATION = 86;
	public static final int CANDYDROP = 87;
	public static final int DB_COMBAT_DAMAGE = 88;
	public static final int SOMBRERO_BONUS = 89;
	public static final int FAMILIAR_EXP = 90;
	public static final int SPORADIC_MEATDROP = 91;
	public static final int SPORADIC_ITEMDROP = 92;
	public static final int MEAT_BONUS = 93;
	public static final int PICKPOCKET_CHANCE = 94;
	public static final int COMBAT_MANA_COST = 95;
	public static final int MUS_EXPERIENCE_PCT = 96;
	public static final int MYS_EXPERIENCE_PCT = 97;
	public static final int MOX_EXPERIENCE_PCT = 98;
	public static final int MINSTREL_LEVEL = 99;
	public static final int UNDERWATER_COMBAT_RATE = 100;

	public static final String EXPR = "(?:([-+]?[\\d.]+)|\\[([^]]+)\\])";

	private static final Object[][] floatModifiers =
	{
		{ "Familiar Weight",
		  Pattern.compile( "([+-]\\d+) to Familiar Weight" ),
		  Pattern.compile( "Familiar Weight: " + EXPR )
		},
		{ "Monster Level",
		  Pattern.compile( "([+-]\\d+) to Monster Level" ),
		  Pattern.compile( "Monster Level: " + EXPR )
		},
		{ "Combat Rate",
		  null,
		  Pattern.compile( "Combat Rate: " + EXPR )
		},
		{ "Initiative",
		  new Object[] {
			Pattern.compile( "Combat Initiative ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Combat Initiative" ),
		  },
		  Pattern.compile( "Initiative: " + EXPR )
		},
		{ "Experience",
		  Pattern.compile( "([+-]\\d+) Stat.*Per Fight" ),
		  Pattern.compile( "Experience: " + EXPR )
		},
		{ "Item Drop",
		  Pattern.compile( "([+-]\\d+)% Item Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Item Drop: " + EXPR )
		},
		{ "Meat Drop",
		  Pattern.compile( "([+-]\\d+)% Meat from Monsters" ),
		  Pattern.compile( "Meat Drop: " + EXPR )
		},
		{ "Damage Absorption",
		  Pattern.compile( "Damage Absorption ([+-]\\d+)" ),
		  Pattern.compile( "Damage Absorption: " + EXPR )
		},
		{ "Damage Reduction",
		  Pattern.compile( "Damage Reduction: ([+-]?\\d+)" ),
		  Pattern.compile( "Damage Reduction: " + EXPR )
		},
		{ "Cold Resistance",
		  null,
		  Pattern.compile( "Cold Resistance: " + EXPR )
		},
		{ "Hot Resistance",
		  null,
		  Pattern.compile( "Hot Resistance: " + EXPR )
		},
		{ "Sleaze Resistance",
		  null,
		  Pattern.compile( "Sleaze Resistance: " + EXPR )
		},
		{ "Spooky Resistance",
		  null,
		  Pattern.compile( "Spooky Resistance: " + EXPR )
		},
		{ "Stench Resistance",
		  null,
		  Pattern.compile( "Stench Resistance: " + EXPR )
		},
		{ "Mana Cost",
		  Pattern.compile( "([+-]\\d+) MP to use Skills$" ),
		  Pattern.compile( "Mana Cost: " + EXPR )
		},
		{ "Moxie",
		  new Object[] {
			Pattern.compile( "Moxie ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Moxie$" ),
		  },
		  Pattern.compile( "Moxie: " + EXPR )
		},
		{ "Moxie Percent",
		  new Object[] {
			Pattern.compile( "Moxie ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Moxie" ),
		  },
		  Pattern.compile( "Moxie Percent: " + EXPR )
		},
		{ "Muscle",
		  new Object[] {
			Pattern.compile( "Muscle ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Muscle$" ),
		  },
		  Pattern.compile( "Muscle: " + EXPR )
		},
		{ "Muscle Percent",
		  new Object[] {
			Pattern.compile( "Muscle ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Muscle" ),
		  },
		  Pattern.compile( "Muscle Percent: " + EXPR )
		},
		{ "Mysticality",
		  new Object[] {
			Pattern.compile( "Mysticality ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Mysticality$" ),
		  },
		  Pattern.compile( "Mysticality: " + EXPR )
		},
		{ "Mysticality Percent",
		  new Object[] {
			Pattern.compile( "Mysticality ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Mysticality" ),
		  },
		  Pattern.compile( "Mysticality Percent: " + EXPR )
		},
		{ "Maximum HP",
		  Pattern.compile( "Maximum HP ([+-]\\d+)$" ),
		  Pattern.compile( "Maximum HP: " + EXPR )
		},
		{ "Maximum HP Percent",
		  null,
		  Pattern.compile( "Maximum HP Percent: " + EXPR )
		},
		{ "Maximum MP",
		  Pattern.compile( "Maximum MP ([+-]\\d+)$" ),
		  Pattern.compile( "Maximum MP: " + EXPR )
		},
		{ "Maximum MP Percent",
		  null,
		  Pattern.compile( "Maximum MP Percent: " + EXPR )
		},
		{ "Weapon Damage",
		  new Object[] {
			Pattern.compile( "Weapon Damage ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Weapon Damage" ),
		  },
		  Pattern.compile( "Weapon Damage: " + EXPR )
		},
		{ "Ranged Damage",
		  new Object[] {
			Pattern.compile( "Ranged Damage ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Ranged Damage" ),
		  },
		  Pattern.compile( "Ranged Damage: " + EXPR )
		},
		{ "Spell Damage",
		  new Object[] {
			Pattern.compile( "Spell Damage ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Spell Damage" ),
		  },
		  Pattern.compile( "(?:^|, )Spell Damage: " + EXPR )
		},
		{ "Spell Damage Percent",
		  new Object[] {
			Pattern.compile( "Spell Damage ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Spell Damage" ),
		  },
		  Pattern.compile( "Spell Damage Percent: " + EXPR )
		},
		{ "Cold Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=blue>Cold Damage<" ),
		  Pattern.compile( "Cold Damage: " + EXPR )
		},
		{ "Hot Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=red>Hot Damage<" ),
		  Pattern.compile( "Hot Damage: " + EXPR )
		},
		{ "Sleaze Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=blueviolet>Sleaze Damage<" ),
		  Pattern.compile( "Sleaze Damage: " + EXPR )
		},
		{ "Spooky Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=gray>Spooky Damage<" ),
		  Pattern.compile( "Spooky Damage: " + EXPR )
		},
		{ "Stench Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=green>Stench Damage<" ),
		  Pattern.compile( "Stench Damage: " + EXPR )
		},
		{ "Cold Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=blue>Cold Spells</font>" ),
		  Pattern.compile( "Cold Spell Damage: " + EXPR )
		},
		{ "Hot Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=red>Hot Spells</font>" ),
		  Pattern.compile( "Hot Spell Damage: " + EXPR )
		},
		{ "Sleaze Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=blueviolet>Sleaze Spells</font>" ),
		  Pattern.compile( "Sleaze Spell Damage: " + EXPR )
		},
		{ "Spooky Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=gray>Spooky Spells</font>" ),
		  Pattern.compile( "Spooky Spell Damage: " + EXPR )
		},
		{ "Stench Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=green>Stench Spells</font>" ),
		  Pattern.compile( "Stench Spell Damage: " + EXPR )
		},
		{ "Critical",
		  Pattern.compile( "(\\d+)x chance of Critical Hit" ),
		  Pattern.compile( "Critical: " + EXPR )
		},
		{ "Fumble",
		  Pattern.compile( "(\\d+)x chance of Fumble" ),
		  Pattern.compile( "Fumble: " + EXPR )
		},
		{ "HP Regen Min",
		  null,
		  Pattern.compile( "HP Regen Min: " + EXPR )
		},
		{ "HP Regen Max",
		  null,
		  Pattern.compile( "HP Regen Max: " + EXPR )
		},
		{ "MP Regen Min",
		  null,
		  Pattern.compile( "MP Regen Min: " + EXPR )
		},
		{ "MP Regen Max",
		  null,
		  Pattern.compile( "MP Regen Max: " + EXPR )
		},
		{ "Adventures",
		  Pattern.compile( "([+-]\\d+) Adventure\\(s\\) per day when equipped" ),
		  Pattern.compile( "Adventures: " + EXPR )
		},
		{ "Familiar Weight Percent",
		  Pattern.compile( "([+-]\\d+)% Familiar Weight" ),
		  Pattern.compile( "Familiar Weight Percent: " + EXPR )
		},
		{ "Weapon Damage Percent",
		  Pattern.compile( "Weapon Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Weapon Damage Percent: " + EXPR )
		},
		{ "Ranged Damage Percent",
		  Pattern.compile( "Ranged Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Ranged Damage Percent: " + EXPR )
		},
		{ "Stackable Mana Cost",
		  Pattern.compile( "([+-]\\d+) MP to use Skills$" ),
		  Pattern.compile( "Mana Cost \\(stackable\\): " + EXPR )
		},
		{ "Hobo Power",
		  Pattern.compile( "([+-]\\d+) Hobo Power" ),
		  Pattern.compile( "Hobo Power: " + EXPR )
		},
		{ "Base Resting HP",
		  null,
		  Pattern.compile( "Base Resting HP: " + EXPR )
		},
		{ "Resting HP Percent",
		  null,
		  Pattern.compile( "Resting HP Percent: " + EXPR )
		},
		{ "Bonus Resting HP",
		  null,
		  Pattern.compile( "Bonus Resting HP: " + EXPR )
		},
		{ "Base Resting MP",
		  null,
		  Pattern.compile( "Base Resting MP: " + EXPR )
		},
		{ "Resting MP Percent",
		  null,
		  Pattern.compile( "Resting MP Percent: " + EXPR )
		},
		{ "Bonus Resting MP",
		  null,
		  Pattern.compile( "Bonus Resting MP: " + EXPR )
		},
		{ "Critical Hit Percent",
		  Pattern.compile( "([+-]\\d+)% chance of Critical Hit" ),
		  Pattern.compile( "Critical Hit Percent: " + EXPR )
		},
		{ "PvP Fights",
		  Pattern.compile( "([+-]\\d+) PvP fight\\(s\\) per day when equipped" ),
		  Pattern.compile( "PvP Fights: " + EXPR )
		},
		{ "Volleyball",
		  null,
		  Pattern.compile( "Volley(?:ball)?: " + EXPR )
		},
		{ "Sombrero",
		  null,
		  Pattern.compile( "Somb(?:rero)?: " + EXPR )
		},
		{ "Leprechaun",
		  null,
		  Pattern.compile( "Lep(?:rechaun)?: " + EXPR )
		},
		{ "Fairy",
		  null,
		  Pattern.compile( "Fairy: " + EXPR )
		},
		{ "Meat Drop Penalty",
		  null,
		  Pattern.compile( "Meat Drop Penalty: " + EXPR )
		},
		{ "Hidden Familiar Weight",
		  null,
		  Pattern.compile( "Familiar Weight \\(hidden\\): " + EXPR )
		},
		{ "Item Drop Penalty",
		  null,
		  Pattern.compile( "Item Drop Penalty: " + EXPR )
		},
		{ "Initiative Penalty",
		  null,
		  Pattern.compile( "Initiative Penalty: " + EXPR )
		},
		{ "Food Drop",
		  Pattern.compile( "([+-]\\d+)% Food Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Food Drop: " + EXPR )
		},
		{ "Booze Drop",
		  Pattern.compile( "([+-]\\d+)% Booze Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Booze Drop: " + EXPR )
		},
		{ "Hat Drop",
		  Pattern.compile( "([+-]\\d+)% Hat(?:/Pants)? Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Hat Drop: " + EXPR )
		},
		{ "Weapon Drop",
		  Pattern.compile( "([+-]\\d+)% Weapon Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Weapon Drop: " + EXPR )
		},
		{ "Offhand Drop",
		  Pattern.compile( "([+-]\\d+)% Off-[Hh]and Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Offhand Drop: " + EXPR )
		},
		{ "Shirt Drop",
		  Pattern.compile( "([+-]\\d+)% Shirt Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Shirt Drop: " + EXPR )
		},
		{ "Pants Drop",
		  Pattern.compile( "([+-]\\d+)% (?:Hat/)?Pants Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Pants Drop: " + EXPR )
		},
		{ "Accessory Drop",
		  Pattern.compile( "([+-]\\d+)% Accessory Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Accessory Drop: " + EXPR )
		},
		{ "Volleyball Effectiveness",
		  null,
		  Pattern.compile( "Volleyball Effectiveness: " + EXPR )
		},
		{ "Sombrero Effectiveness",
		  null,
		  Pattern.compile( "Sombrero Effectiveness: " + EXPR )
		},
		{ "Leprechaun Effectiveness",
		  null,
		  Pattern.compile( "Leprechaun Effectiveness: " + EXPR )
		},
		{ "Fairy Effectiveness",
		  null,
		  Pattern.compile( "Fairy Effectiveness: " + EXPR )
		},
		{ "Familiar Weight Cap",
		  null,
		  Pattern.compile( "Familiar Weight Cap: " + EXPR )
		},
		{ "Slime Resistance",
		  null,
		  Pattern.compile( "Slime Resistance: " + EXPR )
		},
		{ "Slime Hates It",
		  Pattern.compile( "Slime( Really)? Hates (It|You)" ),
		  Pattern.compile( "Slime Hates It: " + EXPR )
		},
		{ "Spell Critical Percent",
		  Pattern.compile( "([+-]\\d+)% chance of Spell Critical Hit" ),
		  Pattern.compile( "Spell Critical Percent: " + EXPR )
		},
		{ "Muscle Experience",
		  Pattern.compile( "([+-]\\d+) Muscle Stat.*Per Fight" ),
		  Pattern.compile( "Experience \\(Muscle\\): " + EXPR ),
		  "Experience (Muscle)"
		},
		{ "Mysticality Experience",
		  Pattern.compile( "([+-]\\d+) Mysticality Stat.*Per Fight" ),
		  Pattern.compile( "Experience \\(Mysticality\\): " + EXPR ),
		  "Experience (Mysticality)"
		},
		{ "Moxie Experience",
		  Pattern.compile( "([+-]\\d+) Moxie Stat.*Per Fight" ),
		  Pattern.compile( "Experience \\(Moxie\\): " + EXPR ),
		  "Experience (Moxie)"
		},
		{ "Effect Duration",
		  null,
		  Pattern.compile( "Effect Duration: " + EXPR )
		},
		{ "Candy Drop",
		  Pattern.compile( "([+-]\\d+)% Candy Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Candy Drop: " + EXPR )
		},
		{ "DB Combat Damage",
		  Pattern.compile( "([+-]\\d+) damage to Disco Bandit Combat Skills" ),
		  Pattern.compile( "DB Combat Damage: " + EXPR )
		},
		{ "Sombrero Bonus",
		  Pattern.compile( "([+-]\\d+) lbs?\\. of Sombrero" ),
		  Pattern.compile( "Sombrero Bonus: " + EXPR )
		},
		{ "Familiar Experience",
		  Pattern.compile( "([+-]\\d+) Familiar Experience" ),
		  Pattern.compile( "Experience \\(familiar\\): " + EXPR ),
		  "Experience (familiar)"
		},
		{ "Sporadic Meat Drop",
		  null,
		  Pattern.compile( "Meat Drop \\(sporadic\\): " + EXPR ),
		  "Meat Drop (sporadic)"
		},
		{ "Sporadic Item Drop",
		  null,
		  Pattern.compile( "Item Drop \\(sporadic\\): " + EXPR ),
		  "Item Drop (sporadic)"
		},
		{ "Meat Bonus",
		  null,
		  Pattern.compile( "Meat Bonus: " + EXPR )
		},
		{ "Pickpocket Chance",
		  Pattern.compile( "([+-]\\d+)% Pickpocket Chance" ),
		  Pattern.compile( "Pickpocket Chance: " + EXPR )
		},
		{ "Combat Mana Cost",
		  Pattern.compile( "([+-]\\d+) MP to use Skills \\(in-combat only\\)" ),
		  Pattern.compile( "Mana Cost \\(combat\\): " + EXPR )
		},
		{ "Muscle Experience Percent",
		  Pattern.compile( "([+-]\\d+)% to all Muscle Gains" ),
		  Pattern.compile( "Experience Percent \\(Muscle\\): " + EXPR ),
		  "Experience Percent (Muscle)"
		},
		{ "Mysticality Experience Percent",
		  Pattern.compile( "([+-]\\d+)% to all Mysticality Gains" ),
		  Pattern.compile( "Experience Percent \\(Mysticality\\): " + EXPR ),
		  "Experience Percent (Mysticality)"
		},
		{ "Moxie Experience Percent",
		  Pattern.compile( "([+-]\\d+)% to all Moxie Gains" ),
		  Pattern.compile( "Experience Percent \\(Moxie\\): " + EXPR ),
		  "Experience Percent (Moxie)"
		},
		{ "Minstrel Level",
		  Pattern.compile( "([+-]\\d+) to Minstrel Level" ),
		  Pattern.compile( "Minstrel Level: " + EXPR )
		},
		{ "Underwater Combat Rate",
		  null,
		  Pattern.compile( "Combat Rate \\(Underwater\\): " + EXPR )
		},
	};

	public static final int FLOAT_MODIFIERS = Modifiers.floatModifiers.length;

	public static final int BOOLEANS = 0;
	public static final int CLOWNOSITY = 1;
	public static final int BRIMSTONE = 2;
	public static final int SYNERGETIC = 3;
	public static final int RAVEOSITY = 4;
	public static final int MUTEX = 5;
	public static final int MUTEX_VIOLATIONS = 6;

	private static final Object[][] bitmapModifiers =
	{
		{ "(booleans)",
		  null,
		  null
		},
		{ "Clownosity",
		  null,
		  Pattern.compile( "Clownosity: (\\+?\\d+)" )
		},
		{ "Brimstone",
		  null,
		  Pattern.compile( "Brimstone" )
		},
		{ "Synergetic",
		  null,
		  Pattern.compile( "Synergetic" )
		},
		{ "Raveosity",
		  null,
		  Pattern.compile( "Raveosity: (\\+?\\d+)" )
		},
		{ "Mutually Exclusive",
		  null,
		  null
		},
		{ "Mutex Violations",
		  null,
		  null
		},
	};

	public static final int BITMAP_MODIFIERS = Modifiers.bitmapModifiers.length;
	private static final int[] bitmapMasks = new int[ BITMAP_MODIFIERS ];
	static { Arrays.fill( bitmapMasks, 1 ); };

	public static final int SOFTCORE = 0;
	public static final int SINGLE = 1;
	public static final int NEVER_FUMBLE = 2;
	public static final int WEAKENS = 3;
	public static final int FREE_PULL = 4;
	public static final int VARIABLE = 5;
	public static final int NONSTACKABLE_WATCH = 6;
	public static final int COLD_IMMUNITY = 7;
	public static final int HOT_IMMUNITY = 8;
	public static final int SLEAZE_IMMUNITY = 9;
	public static final int SPOOKY_IMMUNITY = 10;
	public static final int STENCH_IMMUNITY = 11;
	public static final int COLD_VULNERABILITY = 12;
	public static final int HOT_VULNERABILITY = 13;
	public static final int SLEAZE_VULNERABILITY = 14;
	public static final int SPOOKY_VULNERABILITY = 15;
	public static final int STENCH_VULNERABILITY = 16;
	public static final int MOXIE_CONTROLS_MP = 17;
	public static final int MOXIE_MAY_CONTROL_MP = 18;
	public static final int FOUR_SONGS = 19;
	public static final int ADDITIONAL_SONG = 20;
	public static final int ADVENTURE_UNDERWATER = 21;
	public static final int UNDERWATER_FAMILIAR = 22;
	public static final int GENERIC = 23;
	public static final int UNARMED = 24;

	private static final Object[][] booleanModifiers =
	{
		{ "Softcore Only",
		  Pattern.compile( "This item cannot be equipped while in Hardcore" ),
		  Pattern.compile( "Softcore Only" )
		},
		{ "Single Equip",
		  null,
		  Pattern.compile( "Single Equip" )
		},
		{ "Never Fumble",
		  Pattern.compile( "Never Fumble" ),
		  Pattern.compile( "Never Fumble" )
		},
		{ "Weakens Monster",
		  Pattern.compile( "Successful hit weakens opponent" ),
		  Pattern.compile( "Weakens Monster" )
		},
		{ "Free Pull",
		  null,
		  Pattern.compile( "Free Pull" )
		},
		{ "Variable",
		  null,
		  null
		},
		{ "Nonstackable Watch",
		  null,
		  Pattern.compile( "Nonstackable Watch" )
		},
		{ "Cold Immunity",
		  null,
		  Pattern.compile( "Cold Immunity" )
		},
		{ "Hot Immunity",
		  null,
		  Pattern.compile( "Hot Immunity" )
		},
		{ "Sleaze Immunity",
		  null,
		  Pattern.compile( "Sleaze Immunity" )
		},
		{ "Spooky Immunity",
		  null,
		  Pattern.compile( "Spooky Immunity" )
		},
		{ "Stench Immunity",
		  null,
		  Pattern.compile( "Stench Immunity" )
		},
		{ "Cold Vulnerability",
		  null,
		  Pattern.compile( "Cold Vulnerability" )
		},
		{ "Hot Vulnerability",
		  null,
		  Pattern.compile( "Hot Vulnerability" )
		},
		{ "Sleaze Vulnerability",
		  null,
		  Pattern.compile( "Sleaze Vulnerability" )
		},
		{ "Spooky Vulnerability",
		  null,
		  Pattern.compile( "Spooky Vulnerability" )
		},
		{ "Stench Vulnerability",
		  null,
		  Pattern.compile( "Stench Vulnerability" )
		},
		{ "Moxie Controls MP",
		  null,
		  Pattern.compile( "Moxie Controls MP" )
		},
		{ "Moxie May Control MP",
		  null,
		  Pattern.compile( "Moxie May Control MP" )
		},
		{ "Four Songs",
		  Pattern.compile( "Allows you to keep 4 songs in your head instead of 3" ),
		  Pattern.compile( "Four Songs" )
		},
		{ "Additional Song",
		  Pattern.compile( "Keep 1 additional song in your head" ),
		  Pattern.compile( "Additional Song" )
		},
		{ "Adventure Underwater",
		  Pattern.compile( "Lets you breathe underwater" ),
		  Pattern.compile( "Adventure Underwater" )
		},
		{ "Underwater Familiar",
		  null,
		  Pattern.compile( "Underwater Familiar" )
		},
		{ "Generic",
		  null,
		  Pattern.compile( "Generic" )
		},
		{ "Unarmed",
		  Pattern.compile( "Bonus&nbsp;for&nbsp;Unarmed&nbsp;Characters&nbsp;only" ),
		  Pattern.compile( "Unarmed" )
		},
	};

	public static final int BOOLEAN_MODIFIERS = Modifiers.booleanModifiers.length;
	static
	{
		if ( BOOLEAN_MODIFIERS > 32 )
		{
			KoLmafia.updateDisplay( "Too many boolean modifiers to fit into bitmaps[0].  Will have to store bitmaps as longs, or use two bitmaps to hold the booleans." );
		}
	}

	public static final int CLASS = 0;
	public static final int INTRINSIC_EFFECT = 1;
	public static final int EQUALIZE = 2;
	public static final int WIKI_NAME = 3;
	public static final int MODIFIERS = 4;
	public static final int OUTFIT = 5;
	public static final int STAT_TUNING = 6;
	public static final int FAMILIAR_TUNING = 7;
	public static final int EFFECT = 8;
	public static final int EQUIPS_ON = 9;

	private static final Object[][] stringModifiers =
	{
		{ "Class",
		  null,
		  Pattern.compile( "Class: \"(.*?)\"" )
		},
		{ "Intrinsic Effect",
		  Pattern.compile( "Intrinsic Effect: <a.*?><font color=blue>(.*)</font></a>" ),
		  Pattern.compile( "Intrinsic Effect: \"(.*?)\"" )
		},
		{ "Equalize",
		  null,
		  Pattern.compile( "Equalize: \"(.*?)\"" )
		},
		{ "Wiki Name",
		  null,
		  Pattern.compile( "Wiki Name: \"(.*?)\"" )
		},
		{ "Modifiers",
		  null,
		  Pattern.compile( "^(none)$" )
		},
		{ "Outfit",
		  null,
		  null
		},
		{ "Stat Tuning",
		  null,
		  Pattern.compile( "Stat Tuning: \"(.*?)\"" )
		},
		{ "Familiar Tuning",
		  null,
		  Pattern.compile( "Familiar Tuning: \"(.*?)\"" )
		},
		{ "Effect",
		  null,
		  Pattern.compile( "(?:^|, )Effect: \"(.*?)\"" )
		},
		{ "Equips On",
		  null,
		  Pattern.compile( "Equips On: \"(.*?)\"" )
		},
		{ "Familiar Effect",
		  null,
		  Pattern.compile( "Familiar Effect: \"(.*?)\"" )
		},
	};

	public static final int STRING_MODIFIERS = Modifiers.stringModifiers.length;

	// Indexes for array returned by predict():
	public static final int BUFFED_MUS = 0;
	public static final int BUFFED_MYS = 1;
	public static final int BUFFED_MOX = 2;
	public static final int BUFFED_HP = 3;
	public static final int BUFFED_MP = 4;

	private static final Object[][] derivedModifiers =
	{
		{ "Buffed Muscle" },
		{ "Buffed Mysticality" },
		{ "Buffed Moxie" },
		{ "Buffed HP Maximum" },
		{ "Buffed MP Maximum" },
	};

	public static final int DERIVED_MODIFIERS = Modifiers.derivedModifiers.length;

	public int[] predict()
	{
		int[] rv = new int[ Modifiers.DERIVED_MODIFIERS ];

		int mus = KoLCharacter.getBaseMuscle();
		int mys = KoLCharacter.getBaseMysticality();
		int mox = KoLCharacter.getBaseMoxie();
		String equalize = this.getString( Modifiers.EQUALIZE );
		if ( equalize.startsWith( "Mus" ) )
		{
			mys = mox = mus;
		}
		else if ( equalize.startsWith( "Mys" ) )
		{
			mus = mox = mys;
		}
		else if ( equalize.startsWith( "Mox" ) )
		{
			mus = mys = mox;
		}

		rv[ Modifiers.BUFFED_MUS ] = mus + (int) this.get( Modifiers.MUS ) +
			(int) Math.ceil( this.get( Modifiers.MUS_PCT ) * mus / 100.0 );
		rv[ Modifiers.BUFFED_MYS ] = mys + (int) this.get( Modifiers.MYS ) +
			(int) Math.ceil( this.get( Modifiers.MYS_PCT ) * mys / 100.0 );
		rv[ Modifiers.BUFFED_MOX ] = mox + (int) this.get( Modifiers.MOX ) +
			(int) Math.ceil( this.get( Modifiers.MOX_PCT ) * mox / 100.0f);

		int hpbase = rv[ Modifiers.BUFFED_MUS ];
		double C = KoLCharacter.isMuscleClass() ? 1.5 : 1.0;
		int hp = (int) Math.ceil( (hpbase + 3) * ( C + this.get( Modifiers.HP_PCT ) / 100.0f ) ) + (int) this.get( Modifiers.HP );
		rv[ Modifiers.BUFFED_HP ] = Math.max( hp, mus );

		int mpbase = (int) rv[ Modifiers.BUFFED_MYS ];
		if ( this.getBoolean( Modifiers.MOXIE_CONTROLS_MP ) ||
			(this.getBoolean( Modifiers.MOXIE_MAY_CONTROL_MP ) && 
				(int) rv[ Modifiers.BUFFED_MOX ] > mpbase) )
		{
			mpbase = (int) rv[ Modifiers.BUFFED_MOX ];
		}
		C = KoLCharacter.isMysticalityClass() ? 1.5 : 1.0;
		int mp = (int) Math.ceil( mpbase * ( C + this.get( Modifiers.MP_PCT ) / 100.0f ) ) + (int) this.get( Modifiers.MP );
		rv[ Modifiers.BUFFED_MP ] = Math.max( mp, mys );

		return rv;
	}

	public static Iterator getAllModifiers()
	{
		return Modifiers.modifiersByName.keySet().iterator();
	}

	public static void overrideModifier( String name, Object value )
	{
		name = StringUtilities.getCanonicalName( name );
		if ( value != null )
		{
			Modifiers.modifiersByName.put( name, value );
		}
		else
		{
			Modifiers.modifiersByName.remove( name );
		}
	}

	public static String getModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.floatModifiers, index );
	}

	public static String getBitmapModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.bitmapModifiers, index );
	}

	public static String getBooleanModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.booleanModifiers, index );
	}

	public static String getStringModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.stringModifiers, index );
	}

	public static String getDerivedModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.derivedModifiers, index );
	}

	private static String modifierName( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return (String) table[ index ][ 0 ];
	};

	private static Object modifierDescPattern( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return table[ index ][ 1 ];
	};

	private static Pattern modifierTagPattern( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return (Pattern) table[ index ][ 2 ];
	};

	private static String modifierTag( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return table[ index ].length > 3 ?
			(String) table[ index ][ 3 ] :
			(String) table[ index ][ 0 ];
	};

	private static final String COLD =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.COLD_RESISTANCE ) + ": ";
	private static final String HOT =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.HOT_RESISTANCE ) + ": ";
	private static final String SLEAZE =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.SLEAZE_RESISTANCE ) + ": ";
	private static final String SPOOKY =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.SPOOKY_RESISTANCE ) + ": ";
	private static final String STENCH =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.STENCH_RESISTANCE ) + ": ";

	private static final String MOXIE = Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MOX ) + ": ";
	private static final String MUSCLE = Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MUS ) + ": ";
	private static final String MYSTICALITY = Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MYS ) + ": ";

	private static final String MOXIE_PCT =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MOX_PCT ) + ": ";
	private static final String MUSCLE_PCT =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MUS_PCT ) + ": ";
	private static final String MYSTICALITY_PCT =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MYS_PCT ) + ": ";

	private static final String HP_TAG = Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.HP ) + ": ";
	private static final String MP_TAG = Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MP ) + ": ";

	private static final String HP_REGEN_MIN_TAG =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.HP_REGEN_MIN ) + ": ";
	private static final String HP_REGEN_MAX_TAG =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.HP_REGEN_MAX ) + ": ";
	private static final String MP_REGEN_MIN_TAG =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MP_REGEN_MIN ) + ": ";
	private static final String MP_REGEN_MAX_TAG =
		Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.MP_REGEN_MAX ) + ": ";

	public static int elementalResistance( final int element )
	{
		switch ( element )
		{
		case MonsterDatabase.COLD:
			return Modifiers.COLD_RESISTANCE;
		case MonsterDatabase.HEAT:
			return Modifiers.HOT_RESISTANCE;
		case MonsterDatabase.SLEAZE:
			return Modifiers.SLEAZE_RESISTANCE;
		case MonsterDatabase.SPOOKY:
			return Modifiers.SPOOKY_RESISTANCE;
		case MonsterDatabase.STENCH:
			return Modifiers.STENCH_RESISTANCE;
		}
		return -1;
	}

	public static ArrayList getPotentialChanges( final int index )
	{
		ArrayList<AdventureResult> available = new ArrayList<AdventureResult>();

		Modifiers currentTest;
		Object[] check = Modifiers.modifiersByName.keySet().toArray();

		boolean hasEffect;
		AdventureResult currentEffect;

		for ( int i = 0; i < check.length; ++i )
		{
			if ( !EffectDatabase.contains( (String) check[ i ] ) )
			{
				continue;
			}

			currentTest = Modifiers.getModifiers( (String) check[ i ] );
			float value = ( (Modifiers) currentTest ).get( index );

			if ( value == 0.0f )
			{
				continue;
			}

			currentEffect = new AdventureResult( (String) check[ i ], 1, true );
			hasEffect = KoLConstants.activeEffects.contains( currentEffect );

			if ( value > 0.0f && !hasEffect )
			{
				available.add( currentEffect );
			}
			else if ( value < 0.0f && hasEffect )
			{
				available.add( currentEffect );
			}
		}

		return available;
	}

	private static int findName( final Object[][] table, final String name )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			if ( name.equalsIgnoreCase( (String) table[ i ][ 0 ] ) )
			{
				return i;
			}
		}
		return -1;
	};
	
	public static int findName( String name )
	{
		return Modifiers.findName( Modifiers.floatModifiers, name );
	}

	public static int findBooleanName( String name )
	{
		return Modifiers.findName( Modifiers.booleanModifiers, name );
	}

	private String name;
	private boolean variable;
	private final float[] floats;
	private final int[] bitmaps;
	private final String[] strings;
	private ModifierExpression[] expressions;

	public Modifiers()
	{
		this.variable = false;
		this.floats = new float[ Modifiers.FLOAT_MODIFIERS ];
		this.bitmaps = new int[ Modifiers.BITMAP_MODIFIERS ];
		this.strings = new String[ Modifiers.STRING_MODIFIERS ];
		this.reset();
	};

	public void reset()
	{
		Arrays.fill( this.floats, 0.0f );
		Arrays.fill( this.bitmaps, 0 );
		Arrays.fill( this.strings, "" );
		this.expressions = null;
	};

	public float get( final int index )
	{
		if ( index < 0 || index >= this.floats.length )
		{
			return 0.0f;
		}

		return this.floats[ index ];
	};

	public float get( final String name )
	{
		int index = Modifiers.findName( Modifiers.floatModifiers, name );
		if ( index < 0 || index >= this.floats.length )
		{
			index = Modifiers.findName( Modifiers.derivedModifiers, name );
			if ( index < 0 || index >= Modifiers.DERIVED_MODIFIERS )
			{
				return (float) this.getBitmap( name );
			}
			return this.predict()[ index ];
		}

		return this.floats[ index ];
	};

	public int getRawBitmap( final int index )
	{
		if ( index < 0 || index >= this.bitmaps.length )
		{
			return 0;
		}

		return this.bitmaps[ index ];
	};

	public int getRawBitmap( final String name )
	{
		int index = Modifiers.findName( Modifiers.bitmapModifiers, name );
		if ( index < 0 || index >= this.bitmaps.length )
		{
			return 0;
		}

		return this.bitmaps[ index ];
	};

	public int getBitmap( final int index )
	{
		if ( index < 0 || index >= this.bitmaps.length )
		{
			return 0;
		}

		int n = this.bitmaps[ index ];
		// Count the bits:
		if ( n == 0 ) return 0;
		n = ((n & 0xAAAAAAAA) >>>  1) + (n & 0x55555555);
		n = ((n & 0xCCCCCCCC) >>>  2) + (n & 0x33333333);
		n = ((n & 0xF0F0F0F0) >>>  4) + (n & 0x0F0F0F0F);
		n = ((n & 0xFF00FF00) >>>  8) + (n & 0x00FF00FF);
		n = ((n & 0xFFFF0000) >>> 16) + (n & 0x0000FFFF);
		return n;
	};

	public int getBitmap( final String name )
	{
		return this.getBitmap( Modifiers.findName( Modifiers.bitmapModifiers, name ) );
	};

	public boolean getBoolean( final int index )
	{
		if ( index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS )
		{
			return false;
		}

		return ((this.bitmaps[ 0 ] >>> index) & 1) != 0;
	};

	public boolean getBoolean( final String name )
	{
		int index = Modifiers.findName( Modifiers.booleanModifiers, name );
		if ( index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS )
		{
			return false;
		}

		return ((this.bitmaps[ 0 ] >>> index) & 1) != 0;
	};

	public String getString( final int index )
	{
		if ( index < 0 || index >= this.strings.length )
		{
			return "";
		}

		return this.strings[ index ];
	};

	public String getString( final String name )
	{
		int index = Modifiers.findName( Modifiers.stringModifiers, name );
		if ( index < 0 || index >= this.strings.length )
		{
			return "";
		}

		return this.strings[ index ];
	};
	
	public boolean set( final int index, final double mod )
	{
		if ( index < 0 || index >= this.floats.length )
		{
			return false;
		}

		if ( this.floats[ index ] != mod )
		{
			this.floats[ index ] = (float) mod;
			return true;
		}
		return false;
	};

	public boolean set( final int index, final int mod )
	{
		if ( index < 0 || index >= this.bitmaps.length )
		{
			return false;
		}

		if ( this.bitmaps[ index ] != mod )
		{
			this.bitmaps[ index ] = mod;
			return true;
		}
		return false;
	};

	public boolean set( final int index, final boolean mod )
	{
		if ( index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS )
		{
			return false;
		}

		int mask = 1 << index;
		int val = mod ? mask : 0;
		if ( (this.bitmaps[ 0 ] & mask) != val )
		{
			this.bitmaps[ 0 ] ^= mask;
			return true;
		}
		return false;
	};

	public boolean set( final int index, String mod )
	{
		if ( index < 0 || index >= this.strings.length )
		{
			return false;
		}

		if ( mod == null )
		{
			mod = "";
		}

		if ( !mod.equals( this.strings[ index ] ) )
		{
			this.strings[ index ] = mod;
			return true;
		}
		return false;
	};

	public boolean set( final Modifiers mods )
	{
		if ( mods == null )
		{
			return false;
		}

		boolean changed = false;
		this.name = mods.name;

		float[] copyFloats = mods.floats;
		for ( int index = 0; index < this.floats.length; ++index )
		{
			if ( this.floats[ index ] != copyFloats[ index ] )
			{
				this.floats[ index ] = copyFloats[ index ];
				changed = true;
			}
		}

		int[] copyBitmaps = mods.bitmaps;
		for ( int index = 0; index < this.bitmaps.length; ++index )
		{
			if ( this.bitmaps[ index ] != copyBitmaps[ index ] )
			{
				this.bitmaps[ index ] = copyBitmaps[ index ];
				changed = true;
			}
		}

		String[] copyStrings = mods.strings;
		for ( int index = 0; index < this.strings.length; ++index )
		{
			if ( !this.strings[ index ].equals( copyStrings[ index ] ) )
			{
				this.strings[ index ] = copyStrings[ index ];
				changed = true;
			}
		}
		
		return changed;
	}

	public void add( final int index, final double mod, final String desc )
	{
		switch ( index )
		{
		case COMBAT_RATE:
			// Combat Rate has diminishing returns beyond + or - 25%
			
			// Assume that all the sources of Combat Rate modifiers are of + or - 5%,
			// and start by obtaining the current value without the diminishing returns taken into account
			float rate = this.floats[ index ];
			float extra = Math.abs( rate ) - 25.0f;
			if ( extra > 0.0f )
			{
				rate = ( 25.0f + ( float ) Math.ceil( extra ) * 5.0f ) * ( rate < 0.0f ? -1.0f : 1.0f );
			}

			// Add mod and calculate the new value with the diminishing returns taken into account
			rate += ( float ) mod;
			extra = Math.abs( rate ) - 25.0f;
			if ( extra > 0.0f )
			{
				rate = ( 25.0f + ( float ) Math.ceil( extra / 5.0f ) ) * ( rate < 0.0f ? -1.0f : 1.0f );
			}
			this.floats[ index ] = rate;
			break;
		case CRITICAL:
			// Critical hit modifier is maximum, not additive
			if ( mod > this.floats[ index ] )
			{
				this.floats[ index ] = (float) mod;
			}
			break;
		case MANA_COST:
			// Total Mana Cost reduction cannot exceed 3
			this.floats[ index ] += mod;
			if ( this.floats[ index ] < -3 )
			{
				this.floats[ index ] = -3;
			}
			break;
		case FAMILIAR_WEIGHT_PCT:
			// The three current sources of -wt% do not stack
			if ( this.floats[ index ] > mod )
			{
				this.floats[ index ] = (float) mod;
			}
			break;
		default:
			this.floats[ index ] += mod;
			break;
		}
	};

	public void add( final Modifiers mods )
	{
		if ( mods == null )
		{
			return;
		}

		// Make sure the modifiers apply to current class
		String type = mods.strings[ Modifiers.CLASS ];
		if ( type != "" && !type.equals( KoLCharacter.getClassType() ) )
		{
			return;
		}

		// Unarmed modifiers apply only if the character has no weapon or offhand
		boolean unarmed = mods.getBoolean( Modifiers.UNARMED );
		if ( unarmed && !Modifiers.unarmed )
		{
			return;
		}
		
		String name = mods.name;

		// Add in the float modifiers

		float[] addition = mods.floats;

		for ( int i = 0; i < this.floats.length; ++i )
		{
			if ( addition[ i ] != 0.0f )
			{
				if ( i == Modifiers.ADVENTURES &&
					(mods.bitmaps[ 0 ] & this.bitmaps[ 0 ] &
						(1 << Modifiers.NONSTACKABLE_WATCH)) != 0 )
				{
					continue;
				}
				this.add( i, addition[ i ], name );
			}
		}
		
		// Add in string modifiers as appropriate.
		// Note that there are '==' comparisons with the empty string here:
		// this is safe because the initializing empty string for the
		// strings array is contained in the same source file (in reset()),
		// and is therefore guaranteed by Java to refer to the same string
		// object, not merely an equal one.
		
		String val;
		val = mods.strings[ Modifiers.EQUALIZE ];
		if ( val != "" && this.strings[ Modifiers.EQUALIZE ] == "" )
		{
			this.strings[ Modifiers.EQUALIZE ] = val;
		}
		val = mods.strings[ Modifiers.INTRINSIC_EFFECT ];
		if ( val != "" )
		{
			String prev = this.strings[ INTRINSIC_EFFECT ];
			if ( prev == "" )
			{
				this.strings[ Modifiers.INTRINSIC_EFFECT ] = val;
			}
			else
			{
				this.strings[ Modifiers.INTRINSIC_EFFECT ] = prev + "\t" + val;
			}
		}
		val = mods.strings[ Modifiers.STAT_TUNING ];
		if ( val != "" )
		{
			this.strings[ Modifiers.STAT_TUNING ] = val;
		}
		val = mods.strings[ Modifiers.FAMILIAR_TUNING ];
		if ( val != "" )
		{
			this.strings[ Modifiers.FAMILIAR_TUNING ] = val;
		}

		// OR in the bitmap modifiers (including all the boolean modifiers)
		this.bitmaps[ Modifiers.MUTEX_VIOLATIONS ] |=
			this.bitmaps[ Modifiers.MUTEX ] & mods.bitmaps[ Modifiers.MUTEX ];
		for ( int i = 0; i < this.bitmaps.length; ++i )
		{
			this.bitmaps[ i ] |= mods.bitmaps[ i ];
		}
	}

	public static Modifiers getModifiers( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}

		name = StringUtilities.getCanonicalName( name );
		Object modifier = Modifiers.modifiersByName.get( name );

		if ( modifier == null )
		{
			return null;
		}

		if ( modifier instanceof Modifiers )
		{
			Modifiers mods = (Modifiers) modifier;
			if ( mods.variable )
			{
				mods.override( name );
			}
			return mods;
		}

		if ( !( modifier instanceof String ) )
		{
			return null;
		}

		String string = (String) modifier;

		Modifiers newMods = new Modifiers();
		newMods.name = name;
		float[] newFloats = newMods.floats;
		int[] newBitmaps = newMods.bitmaps;
		String[] newStrings = newMods.strings;

		for ( int i = 0; i < newFloats.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.floatModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}

			if ( matcher.group( 1 ) != null )
			{
				newFloats[ i ] = Float.parseFloat( matcher.group( 1 ) );
			}
			else
			{
				if ( newMods.expressions == null )
				{
					newMods.expressions = new ModifierExpression[ Modifiers.FLOAT_MODIFIERS ];
				}
				newMods.expressions[ i ] = new ModifierExpression( matcher.group( 2 ),
									   name );
			}
		}

		for ( int i = 0; i < newBitmaps.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.bitmapModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}
			int bitcount = 1;
			if ( matcher.groupCount() > 0 )
			{
				bitcount = StringUtilities.parseInt( matcher.group( 1 ) );
			}
			int mask = Modifiers.bitmapMasks[ i ];
			switch ( bitcount )
			{
			case 1:
				Modifiers.bitmapMasks[ i ] <<= 1;
				break;
			case 2:
				mask |= mask << 1;
				Modifiers.bitmapMasks[ i ] <<= 2;
				break;
			default:
				KoLmafia.updateDisplay( "ERROR: invalid count for bitmap modifier in " + name );
				continue;
			}
			if ( Modifiers.bitmapMasks[ i ] == 0 )
			{
				KoLmafia.updateDisplay( "ERROR: too many sources for bitmap modifier " + Modifiers.modifierName( Modifiers.bitmapModifiers, i ) + ", consider using longs." );
			}

			newBitmaps[ i ] |= mask;
		}

		for ( int i = 0; i < Modifiers.BOOLEAN_MODIFIERS; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.booleanModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}

			newBitmaps[ 0 ] |= 1 << i;
		}

		for ( int i = 0; i < newStrings.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.stringModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}

			newStrings[ i ] = matcher.group( 1 );
		}

		newStrings[ Modifiers.MODIFIERS ] = string;

		newMods.variable = newMods.override( name );
		if ( newMods.variable || name.startsWith( "loc:" ) || name.startsWith( "zone:" ) )
		{
			newBitmaps[ 0 ] |= 1 << Modifiers.VARIABLE;
		}
		
		Modifiers.modifiersByName.put( name, newMods );

		return newMods;
	}

	private boolean override( final String name )
	{
		if ( this.expressions != null )
		{
			for ( int i = 0; i < this.expressions.length; ++i )
			{
				ModifierExpression expr = this.expressions[ i ];
				if ( expr != null )
				{
					this.floats[ i ] = expr.eval();
				}
			}
			return true;
		}

		int itemId = ItemDatabase.getItemId( name );

		switch ( itemId )
		{
		case ItemPool.TUESDAYS_RUBY:
		{
			// Reset to defaults

			this.set( Modifiers.MEATDROP, 0.0 );
			this.set( Modifiers.ITEMDROP, 0.0 );
			this.set( Modifiers.MOX_PCT, 0.0 );
			this.set( Modifiers.MUS_PCT, 0.0 );
			this.set( Modifiers.MYS_PCT, 0.0 );
			this.set( Modifiers.HP_REGEN_MIN, 0.0 );
			this.set( Modifiers.HP_REGEN_MAX, 0.0 );
			this.set( Modifiers.MP_REGEN_MIN, 0.0 );
			this.set( Modifiers.MP_REGEN_MAX, 0.0 );

			// Set modifiers depending on what KoL day of the week it is

			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
			switch ( date.get( Calendar.DAY_OF_WEEK ) )
			{
			case Calendar.SUNDAY:
				// +5% Meat from Monsters
				this.set( Modifiers.MEATDROP, 5.0 );
				break;
			case Calendar.MONDAY:
				// Muscle +5%
				this.set( Modifiers.MUS_PCT, 5.0 );
				break;
			case Calendar.TUESDAY:
				// Regenerate 3-7 MP per adventure
				this.set( Modifiers.MP_REGEN_MIN, 3.0 );
				this.set( Modifiers.MP_REGEN_MAX, 7.0 );
				break;
			case Calendar.WEDNESDAY:
				// +5% Mysticality
				this.set( Modifiers.MYS_PCT, 5.0 );
				break;
			case Calendar.THURSDAY:
				// +5% Item Drops from Monsters
				this.set( Modifiers.ITEMDROP, 5.0 );
				break;
			case Calendar.FRIDAY:
				// +5% Moxie
				this.set( Modifiers.MOX_PCT, 5.0 );
				break;
			case Calendar.SATURDAY:
				// Regenerate 3-7 HP per adventure
				this.set( Modifiers.HP_REGEN_MIN, 3.0 );
				this.set( Modifiers.HP_REGEN_MAX, 7.0 );
				break;
			}
			return true;
		}

		case ItemPool.UNCLE_HOBO_BEARD:
		{
			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
			double adventures = date.get( Calendar.MONTH ) == Calendar.DECEMBER ? 9.0 : 6.0;
			this.set( Modifiers.ADVENTURES, adventures );
			return true;
		}

		case ItemPool.PATRIOT_SHIELD:
		{
			// Muscle classes
			this.set( Modifiers.HP_REGEN_MIN, 0.0 );
			this.set( Modifiers.HP_REGEN_MAX, 0.0 );
			// Seal clubber
			this.set( Modifiers.WEAPON_DAMAGE, 0.0 );
			this.set( Modifiers.DAMAGE_REDUCTION, 0.0 );
			// Turtle Tamer
			this.set( Modifiers.FAMILIAR_WEIGHT, 0.0 );
			// Disco Bandit
			this.set( Modifiers.RANGED_DAMAGE, 0.0 );
			// Accordion Thief
			this.set( Modifiers.FOUR_SONGS, false );
			// Mysticality classes
			this.set( Modifiers.MP_REGEN_MIN, 0.0 );
			this.set( Modifiers.MP_REGEN_MAX, 0.0 );
			// Pastamancer
			this.set( Modifiers.COMBAT_MANA_COST, 0.0 );
			// Sauceror
			this.set( Modifiers.SPELL_DAMAGE, 0.0 );

			// Set modifiers depending on Character class
			String classType = KoLCharacter.getClassType();
			if ( classType == KoLCharacter.SEAL_CLUBBER )
			{
				this.set( Modifiers.HP_REGEN_MIN, 10.0 );
				this.set( Modifiers.HP_REGEN_MAX, 12.0 );
				this.set( Modifiers.WEAPON_DAMAGE, 15.0 );
				this.set( Modifiers.DAMAGE_REDUCTION, 1.0 );
			}
			else if ( classType == KoLCharacter.TURTLE_TAMER )
			{
				this.set( Modifiers.HP_REGEN_MIN, 10.0 );
				this.set( Modifiers.HP_REGEN_MAX, 12.0 );
				this.set( Modifiers.FAMILIAR_WEIGHT, 5.0 );
			}
			else if ( classType == KoLCharacter.DISCO_BANDIT )
			{
				this.set( Modifiers.RANGED_DAMAGE, 20.0 );
			}
			else if ( classType == KoLCharacter.ACCORDION_THIEF )
			{
				this.set( Modifiers.FOUR_SONGS, true );
			}
			else if ( classType == KoLCharacter.PASTAMANCER )
			{
				this.set( Modifiers.MP_REGEN_MIN, 5.0 );
				this.set( Modifiers.MP_REGEN_MAX, 6.0 );
				this.set( Modifiers.COMBAT_MANA_COST, -3.0 );
			}
			else if ( classType == KoLCharacter.SAUCEROR )
			{
				this.set( Modifiers.MP_REGEN_MIN, 5.0 );
				this.set( Modifiers.MP_REGEN_MAX, 6.0 );
				this.set( Modifiers.SPELL_DAMAGE, 20.0 );
			}
			return true;
		}
		}

		return false;
	}

	public static float getNumericModifier( final String name, final String mod )
	{
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return 0.0f;
		}
		return mods.get( mod );
	}

	public static float getNumericModifier( final FamiliarData fam, final String mod, final int passedWeight, final AdventureResult item )
	{
		int familiarId = fam != null ? fam.getId() : -1;
		if ( familiarId == -1 )
		{
			return 0.0f;
		}
		Modifiers tempMods = new Modifiers();
		tempMods.setFamiliar( fam );
		if ( familiarId != FamiliarPool.HATRACK && familiarId != FamiliarPool.SCARECROW )
		{
			// Mad Hatrack ... hats do not give their normal modifiers
			// Fancypants Scarecrow ... pants do not give their normal modifiers
			// (should I be checking the item is a hat or pants?)
			tempMods.add( Modifiers.getModifiers( item.getName() ) );
		}
		int weight = passedWeight + (int) tempMods.get( Modifiers.FAMILIAR_WEIGHT ) + (int) tempMods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT ) + ( fam.getFeasted() ? 10 : 0 );
		float percent = tempMods.get( Modifiers.FAMILIAR_WEIGHT_PCT ) / 100.0f;
		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}
		tempMods.lookupFamiliarModifiers( fam, weight, item );
		return tempMods.get( mod );
	}

	public static boolean getBooleanModifier( final String name, final String mod )
	{
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return false;
		}
		return mods.getBoolean( mod );
	}

	public static String getStringModifier( final String name, final String mod )
	{
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return "";
		}
		return mods.getString( mod );
	}

	public void applyPassiveModifiers()
	{
		// You'd think this could be done at class initialization time,
		// but no: the SkillDatabase depends on the Mana Cost
		// modifier being set.

		if ( Modifiers.passiveSkills.isEmpty() )
		{
			Object[] keys = Modifiers.modifiersByName.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
			{
				String skill = (String) keys[ i ];
				if ( !SkillDatabase.contains( skill ) )
				{
					continue;
				}

				if ( SkillDatabase.getSkillType( SkillDatabase.getSkillId( skill ) ) == SkillDatabase.PASSIVE )
				{
					Modifiers.passiveSkills.add( UseSkillRequest.getUnmodifiedInstance( skill ) );
				}
			}
		}

		for ( int i = Modifiers.passiveSkills.size() - 1; i >= 0; --i )
		{
			UseSkillRequest skill = (UseSkillRequest) Modifiers.passiveSkills.get( i );
			if ( KoLCharacter.hasSkill( skill ) )
			{
				this.add( Modifiers.getModifiers( skill.getSkillName() ) );
			}
		}

		if ( KoLCharacter.getFamiliar().getId() == FamiliarPool.DODECAPEDE && KoLCharacter.hasAmphibianSympathy() )
		{
			this.add( Modifiers.FAMILIAR_WEIGHT, -10, "dodecapede sympathy" );
		}
	}
	
	public void applySynergies()
	{
		int synergetic = this.getRawBitmap( Modifiers.SYNERGETIC );
		if ( synergetic == 0 ) return;	// nothing possible
		Iterator i = Modifiers.synergies.iterator();
		while ( i.hasNext() )
		{
			String name = (String) i.next();
			int mask = (Integer) i.next();
			if ( (synergetic & mask) == mask )
			{
				this.add( Modifiers.getModifiers( name ) );
			}
		}
	}

	// Returned iterator yields alternating names / bitmaps
	public static Iterator getSynergies()
	{
		return Modifiers.synergies.iterator();
	}

	public void applyFamiliarModifiers( final FamiliarData familiar, AdventureResult famItem )
	{
		int familiarId = familiar.getId();
		int weight = familiar.getWeight();

		weight += (int) this.get( Modifiers.FAMILIAR_WEIGHT );
		weight += (int) this.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
		weight += ( familiar.getFeasted() ? 10 : 0 );

		float percent = this.get( Modifiers.FAMILIAR_WEIGHT_PCT ) / 100.0f;
		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}

		weight = Math.max( 1, weight );
		this.lookupFamiliarModifiers( familiar, weight, famItem );
	}

	public void lookupFamiliarModifiers( final FamiliarData familiar, int weight, final AdventureResult famItem )
	{
		int familiarId = familiar.getId();
		weight = Math.max( 1, weight );
		Modifiers.currentWeight = weight;
		
		this.add( Modifiers.getModifiers( "fam:" + familiar.getRace() ) );
		if ( famItem != null )
		{
			this.add( Modifiers.getModifiers( "fameq:" + famItem.getName() ) );
		}

		int cap = (int)this.get( Modifiers.FAMILIAR_WEIGHT_CAP );
		int cappedWeight = ( cap == 0 ) ? weight : Math.min( weight, cap );

		double effective = cappedWeight * this.get( Modifiers.VOLLEYBALL_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isVolleyType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.VOLLEYBALL_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			factor = factor * Math.sqrt( effective );
			String tuning = this.getString( Modifiers.FAMILIAR_TUNING );
			if ( tuning.equals( "Muscle" ) )
			{
				this.add( Modifiers.MUS_EXPERIENCE, factor, "Tuned Volleyball" );
			}
			else if ( tuning.equals( "Mysticality" ) )
			{
				this.add( Modifiers.MYS_EXPERIENCE, factor, "Tuned Volleyball" );
			}
			else if ( tuning.equals( "Moxie" ) )
			{
				this.add( Modifiers.MOX_EXPERIENCE, factor, "Tuned Volleyball" );
			}
			else
			{			
				this.add( Modifiers.EXPERIENCE, factor, "Volleyball" );
			}
		}

		effective = cappedWeight * this.get( Modifiers.SOMBRERO_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isSombreroType( familiarId ) )
		{
			effective = weight;
		}
		effective += this.get( Modifiers.SOMBRERO_BONUS );
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.SOMBRERO_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			// currentML is always >= 4, so we don't need to check for negatives
			this.add( Modifiers.EXPERIENCE, factor * Math.sqrt( effective ) *
				(1.0 + Math.sqrt( Modifiers.currentML - 4.0f )) / 10.0, "Sombrero" );
		}

		effective = cappedWeight * this.get( Modifiers.LEPRECHAUN_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isMeatDropType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.LEPRECHAUN_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			this.add( Modifiers.MEATDROP, factor * (Math.sqrt( 220 * effective ) + 2 * effective - 6),
				"Leprechaun" );
		}

		effective = cappedWeight * this.get( Modifiers.FAIRY_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isFairyType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.FAIRY_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			this.add( Modifiers.ITEMDROP, factor * (Math.sqrt( 55 * effective ) + effective - 3),
				"Fairy" );
		}

		switch ( familiarId )
		{
		case FamiliarPool.HATRACK:
			if ( famItem == EquipmentRequest.UNEQUIP )
			{
				this.add( Modifiers.HATDROP, 50.0, "naked hatrack" );
			}
			break;
		case FamiliarPool.SCARECROW:
			if ( famItem == EquipmentRequest.UNEQUIP )
			{
				this.add( Modifiers.PANTSDROP, 50.0, "naked scarecrow" );
			}
			break;
		}
	}
	
	public static String getFamiliarEffect( final String itemName )
	{
		return (String) Modifiers.familiarEffectByName.get( 
			StringUtilities.getCanonicalName( itemName ) );
	}

	public void applyMinstrelModifiers( final int level, AdventureResult instrument )
	{
		String name = instrument.getName();
		Modifiers imods = Modifiers.getModifiers( name );

		double effective = imods.get( Modifiers.VOLLEYBALL_WEIGHT );
		if ( effective != 0.0 )
		{
			double factor = Math.sqrt( effective );
			this.add( Modifiers.EXPERIENCE, factor, name );
		}

		effective = imods.get( Modifiers.FAIRY_WEIGHT );
		if ( effective != 0.0 )
		{
			double factor = Math.sqrt( 55 * effective ) + effective - 3;
			this.add( Modifiers.ITEMDROP, factor, name );
		}

		this.add( Modifiers.HP_REGEN_MIN, imods.get( Modifiers.HP_REGEN_MIN ), name );
		this.add( Modifiers.HP_REGEN_MAX, imods.get( Modifiers.HP_REGEN_MAX ), name );
		this.add( Modifiers.MP_REGEN_MIN, imods.get( Modifiers.MP_REGEN_MIN ), name );
		this.add( Modifiers.MP_REGEN_MAX, imods.get( Modifiers.MP_REGEN_MAX ), name );
	}


	// Parsing item enchantments into KoLmafia modifiers

	private static final Pattern DR_PATTERN = Pattern.compile( "Damage Reduction: (<b>)?([+-]?\\d+)(</b>)?" );

	public static String parseDamageReduction( final String text )
	{
		Matcher matcher = Modifiers.DR_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.DAMAGE_REDUCTION ) + ": " + matcher.group( 2 );
		}

		return null;
	}

	private static final Pattern SINGLE_PATTERN =
		Pattern.compile( "You may not equip more than one of this item at a time" );

	public static String parseSingleEquip( final String text )
	{
		Matcher matcher = Modifiers.SINGLE_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierTag( Modifiers.booleanModifiers, Modifiers.SINGLE );
		}

		return null;
	}

	private static final Pattern SOFTCORE_PATTERN =
		Pattern.compile( "This item cannot be equipped while in Hardcore" );

	public static String parseSoftcoreOnly( final String text )
	{
		Matcher matcher = Modifiers.SOFTCORE_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierTag( Modifiers.booleanModifiers, Modifiers.SOFTCORE );
		}

		return null;
	}

	private static final Pattern FREE_PULL_PATTERN =
		Pattern.compile( "Free pull from Hagnk's" );

	public static String parseFreePull( final String text )
	{
		Matcher matcher = Modifiers.FREE_PULL_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierTag( Modifiers.booleanModifiers, Modifiers.FREE_PULL );
		}

		return null;
	}

	private static final Pattern EFFECT_PATTERN = Pattern.compile( "Effect: <b><a[^>]*>([^<]*)</a></b>" );

	public static String parseEffect( final String text )
	{
		Matcher matcher = Modifiers.EFFECT_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierTag( Modifiers.stringModifiers, Modifiers.EFFECT ) + ": \"" + matcher.group( 1 ) + "\"";
		}

		return null;
	}

	private static final Pattern DURATION_PATTERN = Pattern.compile( "Duration: <b>([\\d]*) Adventures</b>" );

	public static String parseDuration( final String text )
	{
		Matcher matcher = Modifiers.DURATION_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierTag( Modifiers.floatModifiers, Modifiers.EFFECT_DURATION ) + ": " + matcher.group( 1 );
		}

		return null;
	}

	private static final Pattern ALL_ATTR_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)$" );
	private static final Pattern ALL_ATTR_PCT_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)%$" );
	private static final Pattern CLASS_PATTERN = Pattern.compile( "Bonus&nbsp;for&nbsp;(.*)&nbsp;only" );
	private static final Pattern COMBAT_PATTERN = Pattern.compile( "Monsters will be (.*) attracted to you" );
	private static final Pattern HP_MP_PATTERN = Pattern.compile( "^Maximum HP/MP ([+-]\\d+)$" );

	public static String parseModifier( final String enchantment )
	{
		String result;

		// Search the float modifiers first

		result = Modifiers.parseModifier( Modifiers.floatModifiers, enchantment, false );
		if ( result != null )
		{
			return result;
		}

		// Then the boolean modifiers

		result = Modifiers.parseModifier( Modifiers.booleanModifiers, enchantment, false );
		if ( result != null )
		{
			return result;
		}

		// Then the string modifiers

		result = Modifiers.parseModifier( Modifiers.stringModifiers, enchantment, true );
		if ( result != null )
		{
			return result;
		}

		// Special handling needed

		Matcher matcher;

		matcher = Modifiers.ALL_ATTR_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.MUSCLE + mod + ", " + Modifiers.MYSTICALITY + mod + ", " + Modifiers.MOXIE + mod;
		}

		matcher = Modifiers.ALL_ATTR_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.MUSCLE_PCT + mod + ", " + Modifiers.MYSTICALITY_PCT + mod + ", " + Modifiers.MOXIE_PCT + mod;
		}

		matcher = Modifiers.CLASS_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String plural = matcher.group( 1 );
			String cls = "none";
			if ( plural.equals( "Accordion&nbsp;Thieves" ) )
			{
				cls = KoLCharacter.ACCORDION_THIEF;
			}
			else if ( plural.equals( "Disco&nbsp;Bandits" ) )
			{
				cls = KoLCharacter.DISCO_BANDIT;
			}
			else if ( plural.equals( "Pastamancers" ) )
			{
				cls = KoLCharacter.PASTAMANCER;
			}
			else if ( plural.equals( "Saucerors" ) )
			{
				cls = KoLCharacter.SAUCEROR;
			}
			else if ( plural.equals( "Seal&nbsp;Clubbers" ) )
			{
				cls = KoLCharacter.SEAL_CLUBBER;
			}
			else if ( plural.equals( "Turtle&nbsp;Tamers" ) )
			{
				cls = KoLCharacter.TURTLE_TAMER;
			}
			else
			{
				return null;
			}
			return Modifiers.modifierTag( Modifiers.stringModifiers, Modifiers.CLASS ) + ": \"" + cls + "\"";
		}

		matcher = Modifiers.COMBAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String tag = Modifiers.modifierTag( Modifiers.floatModifiers, !enchantment.contains( "Underwater only" ) ? Modifiers.COMBAT_RATE : Modifiers.UNDERWATER_COMBAT_RATE );
			return  tag + ": " + ( matcher.group( 1 ).equals( "more" ) ? "+5" : "-5" );
		}

		matcher = Modifiers.HP_MP_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.HP_TAG + mod + ", " + Modifiers.MP_TAG + mod;
		}

		if ( enchantment.contains( "Regenerate" ) )
		{
			return Modifiers.parseRegeneration( enchantment );
		}

		if ( enchantment.contains( "Resistance" ) )
		{
			return Modifiers.parseResistance( enchantment );
		}

		return null;
	}

	private static String parseModifier( final Object[][] table, final String enchantment, final boolean quoted )
	{
		String quote = quoted ? "\"" : "";
		for ( int i = 0; i < table.length; ++i )
		{
			Object object = Modifiers.modifierDescPattern( table, i );
			if ( object == null )
			{
				continue;
			}

			Object [] patterns;

			if ( object instanceof Pattern )
			{
				patterns = new Pattern[1];
				patterns[0] = (Pattern) object;
			}
			else
			{
				patterns = (Object[]) object;
			}

			for ( int j = 0; j < patterns.length; ++j )
			{
				Pattern pattern = (Pattern) patterns[ j ];
				Matcher matcher = pattern.matcher( enchantment );
				if ( !matcher.find() )
				{
					continue;
				}

				if ( matcher.groupCount() == 0 )
				{
					return Modifiers.modifierTag( table, i );
				}
				return Modifiers.modifierTag( table, i ) + ": " + quote + matcher.group( 1 ) + quote;
			}
		}

		return null;
	}

	private static final Pattern REGEN_PATTERN =
		Pattern.compile( "Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$" );

	private static String parseRegeneration( final String enchantment )
	{
		Matcher matcher = Modifiers.REGEN_PATTERN.matcher( enchantment );
		if ( !matcher.find() )
		{
			return null;
		}

		String min = matcher.group( 1 );
		String max = matcher.group( 2 ) == null ? min : matcher.group( 2 );
		boolean hp = matcher.group( 3 ).equals( "HP" );
		boolean both = matcher.group( 4 ) != null;

		if ( max.equals( "" ) )
		{
			max = min;
		}

		if ( both )
		{
			return Modifiers.HP_REGEN_MIN_TAG + min + ", " + Modifiers.HP_REGEN_MAX_TAG + max + ", " + Modifiers.MP_REGEN_MIN_TAG + min + ", " + Modifiers.MP_REGEN_MAX_TAG + max;
		}

		if ( hp )
		{
			return Modifiers.HP_REGEN_MIN_TAG + min + ", " + Modifiers.HP_REGEN_MAX_TAG + max;
		}

		return Modifiers.MP_REGEN_MIN_TAG + min + ", " + Modifiers.MP_REGEN_MAX_TAG + max;
	}

	private static String parseResistance( final String enchantment )
	{
		String level = "";

		if ( enchantment.contains( "Slight" ) )
		{
			level = "+1";
		}
		else if ( enchantment.contains( "So-So" ) )
		{
			level = "+2";
		}
		else if ( enchantment.contains( "Serious" ) )
		{
			level = "+3";
		}
		else if ( enchantment.contains( "Stupendous" ) )
		{
			level = "+4";
		}
		else if ( enchantment.contains( "Superhuman" ) )
		{
			level = "+5";
		}
		else if ( enchantment.contains( "Sublime" ) )
		{
			level = "+9";
		}

		if ( enchantment.contains( "All Elements" ) )
		{
			return Modifiers.COLD + level + ", " + Modifiers.HOT + level + ", " + Modifiers.SLEAZE + level + ", " + Modifiers.SPOOKY + level + ", " + Modifiers.STENCH + level;
		}

		if ( enchantment.contains( "Cold" ) )
		{
			return Modifiers.COLD + level;
		}

		if ( enchantment.contains( "Hot" ) )
		{
			return Modifiers.HOT + level;
		}

		if ( enchantment.contains( "Sleaze" ) )
		{
			return Modifiers.SLEAZE + level;
		}

		if ( enchantment.contains( "Spooky" ) )
		{
			return Modifiers.SPOOKY + level;
		}

		if ( enchantment.contains( "Stench" ) )
		{
			return Modifiers.STENCH + level;
		}

		return null;
	}

	private static boolean findModifier( final Object[][] table, final String tag )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( table, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( tag );
			if ( matcher.find() )
			{
				return true;
			}
		}
		return false;
	}

	public static void checkModifiers()
	{
		Object[] keys = Modifiers.modifiersByName.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			Object modifier = Modifiers.modifiersByName.get( name );

			if ( modifier == null )
			{
				RequestLogger.printLine( "Key \"" + name + "\" has no modifiers" );
				continue;
			}

			if ( modifier instanceof Modifiers )
			{
				modifier = ((Modifiers) modifier).getString( Modifiers.MODIFIERS );
			}

			if ( !( modifier instanceof String ) )
			{
				RequestLogger.printLine( "Key \"" + name + "\" has bogus modifiers of class " + modifier.getClass().toString() );
				continue;
			}

			// It's a string. Check all modifiers.
			// Familiar Effect has to be special-cased since its parameter contains commas.
			String[] strings = ( (String) modifier ).replaceFirst(
				"(, )?Familiar Effect: \"[^\"]+\"" , "" ).split( ", " );
			for ( int j = 0; j < strings.length; ++j )
			{
				String mod = strings[ j ].trim();
				if ( mod.equals( "" ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.floatModifiers, mod ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.bitmapModifiers, mod ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.booleanModifiers, mod ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.stringModifiers, mod ) )
				{
					continue;
				}
				if ( mod.startsWith( "Clownosity:" ) )
				{
					continue;
				}
				if ( name.startsWith( "fameq:" ) )
				{
					continue;	// these may contain freeform text
				}
				RequestLogger.printLine( "Key \"" + name + "\" has unknown modifier: \"" + mod + "\"" );
			}
		}
	}

	public static void setLocation( KoLAdventure location )
	{
		if ( location == null )
		{
			Modifiers.currentLocation = "";
			Modifiers.currentZone = "";
			Modifiers.currentML = 4.0f;
			return;
		}

		Modifiers.currentLocation = location.getAdventureName().toLowerCase();
		Modifiers.currentZone = location.getZone().toLowerCase();
		AreaCombatData data = location.getAreaSummary();
		Modifiers.currentML = Math.max( 4.0f, data == null ? 0.0f : data.getAverageML() );
	}

	public static void setFamiliar( FamiliarData fam )
	{
		Modifiers.currentFamiliar = fam == null ? "" : fam.getRace().toLowerCase();
	}

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "modifiers.txt", KoLConstants.MODIFIERS_VERSION );
		String[] data;

	loop:
		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length != 2 )
			{
				continue;
			}

			String name = StringUtilities.getCanonicalName( data[ 0 ] );
			if ( Modifiers.modifiersByName.containsKey( name ) )
			{
				KoLmafia.updateDisplay( "Duplicate modifiers for: " + name );
			}

			String modifiers = new String( data[ 1 ] );
			Modifiers.modifiersByName.put( name, modifiers );
			
			Matcher matcher = FAMILIAR_EFFECT_PATTERN.matcher( modifiers );
			if ( matcher.find() )
			{
				Modifiers.familiarEffectByName.put( name, matcher.group( 1 ) );
				String effect = matcher.group( 1 );
				matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN.matcher( effect );
				if ( matcher.find() )
				{
					effect = matcher.replaceAll( FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT );
				}
				matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN2.matcher( effect );
				if ( matcher.find() )
				{
					effect = matcher.replaceAll( FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 );
				}
				Modifiers.modifiersByName.put( "fameq:" + name, effect );
			}
			
			if ( name.startsWith( "synergy" ) )
			{
				String[] pieces = name.split( "\\Q" + name.substring( 7, 8 ) );
				if ( pieces.length < 3 )
				{
					KoLmafia.updateDisplay( name + " contain less than 2 elements." );
					continue loop;
				}
				int mask = 0;
				for ( int i = 1; i < pieces.length; ++i )
				{
					Modifiers mods = Modifiers.getModifiers( pieces[ i ] );
					if ( mods == null )
					{
						KoLmafia.updateDisplay( name + " contains element " + pieces[ i ] + " with no modifiers." );
						continue loop;
					}
					int emask = mods.bitmaps[ Modifiers.SYNERGETIC ];
					if ( emask == 0 )
					{
						KoLmafia.updateDisplay( name + " contains element " + pieces[ i ] + " that isn't Synergetic." );
						continue loop;
					}
					mask |= emask;
				}
				Modifiers.synergies.add( name );
				Modifiers.synergies.add( IntegerPool.get( mask ) );
			}
			else if ( name.startsWith( "mutex" ) )
			{
				String[] pieces = name.split( "\\Q" + name.substring( 5, 6 ) );
				if ( pieces.length < 3 )
				{
					KoLmafia.updateDisplay( name + " contain less than 2 elements." );
					continue loop;
				}
				int bit = 1 << Modifiers.mutexes.size();
				for ( int i = 1; i < pieces.length; ++i )
				{
					Modifiers mods = Modifiers.getModifiers( pieces[ i ] );
					if ( mods == null )
					{
						KoLmafia.updateDisplay( name + " contains element " + pieces[ i ] + " with no modifiers." );
						continue loop;
					}
					mods.bitmaps[ Modifiers.MUTEX ] |= bit;
				}
				Modifiers.mutexes.add( name );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static void writeModifiers( final File output )
	{
		RequestLogger.printLine( "Writing data override: " + output );

		// One map per equipment category
		Map hats = new TreeMap();
		Map weapons = new TreeMap();
		Map offhands = new TreeMap();
		Map shirts = new TreeMap();
		Map pants = new TreeMap();
		Map accessories = new TreeMap();
		Map containers = new TreeMap();
		Map famitems = new TreeMap();
		Map bedazzlements = new TreeMap();
		Map freepulls = new TreeMap();
		Map potions = new TreeMap();
		Map wikiname = new TreeMap();

		// Iterate over all items and assign item id to category
		Iterator it = ItemDatabase.dataNameEntrySet().iterator();
		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer key = (Integer) entry.getKey();
			String name = (String) entry.getValue();
			int type = ItemDatabase.getConsumptionType( key );

			switch ( type )
			{
			case KoLConstants.EQUIP_HAT:
				hats.put( name, null );
				break;
			case KoLConstants.EQUIP_PANTS:
				pants.put( name, null );
				break;
			case KoLConstants.EQUIP_SHIRT:
				shirts.put( name, null );
				break;
			case KoLConstants.EQUIP_WEAPON:
				weapons.put( name, null );
				break;
			case KoLConstants.EQUIP_OFFHAND:
				offhands.put( name, null );
				break;
			case KoLConstants.EQUIP_ACCESSORY:
				accessories.put( name, null );
				break;
			case KoLConstants.EQUIP_CONTAINER:
				containers.put( name, null );
				break;
			case KoLConstants.EQUIP_FAMILIAR:
				famitems.put( name, null );
				break;
			case KoLConstants.CONSUME_STICKER:
				bedazzlements.put( name, null );
				break;
			default:
				Modifiers mods = Modifiers.getModifiers( name );
				if ( mods == null )
				{
					break;
				}
				if ( !mods.getString( Modifiers.EFFECT ).equals( "" ) )
				{
					potions.put( name, null );
				}
				else if ( mods.getBoolean( Modifiers.FREE_PULL ) )
				{
					freepulls.put( name, null );
				}
				else if ( !mods.getString( Modifiers.WIKI_NAME).equals( "" ) )
				{
					wikiname.put( name, null );
				}
				break;
			}
		}

		// Make a map of familiars
		Map familiars = new TreeMap();
		familiars.put( "Fam:(none)", null );

		it = FamiliarDatabase.entrySet().iterator();
		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer key = (Integer) entry.getKey();
			String name = "Fam:" + (String) entry.getValue();
			if ( Modifiers.getModifiers( name ) != null )
			{
				familiars.put( name, null );
			}
		}

		// Make a map of campground items
		Map campground = new TreeMap();

		for ( int i = 0; i < CampgroundRequest.campgroundItems.length; ++i )
		{
			int itemId = CampgroundRequest.campgroundItems[i];
			String name = ItemDatabase.getItemDataName( itemId );
			// Sanity check: if the user has an old override file
			// which we didn't delete for some reason, we may have
			// an unknown item on the list of campground items.
			if ( name == null )
			{
				KoLmafia.updateDisplay( "Campground item #" + itemId + " not found in data file. Do 'update clear' to remove stale override!" );
			}
			// Skip toilet paper, since we want that in the free
			// pull section
			else if ( itemId != ItemPool.TOILET_PAPER )
			{
				campground.put( name, null );
			}
		}

		// Make a map of status effects
		Map effects = new TreeMap();

		it = EffectDatabase.dataNameEntrySet().iterator();
		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer key = (Integer) entry.getKey();
			String name = (String) entry.getValue();
			// Skip effect which is also an item
			if ( !name.equals( "The Spirit of Crimbo" ) )
			{
				effects.put( name, null );
			}
		}

		// Make a map of passive skills
		Map passives = new TreeMap();

		it = SkillDatabase.entrySet().iterator();
		while ( it.hasNext() )
		{
			Entry entry = (Entry) it.next();
			Integer key = (Integer) entry.getKey();
			String name = (String) entry.getValue();
			if ( SkillDatabase.getSkillType( key ) == SkillDatabase.PASSIVE )
			{
				passives.put( name, null );
			}
		}

		// Make a map of outfits
		Map outfits = new TreeMap();
		int outfitCount = EquipmentDatabase.getOutfitCount();

		for ( int i = 1; i <= outfitCount; ++i )
		{
			SpecialOutfit outfit = EquipmentDatabase.getOutfit( i );
			if ( outfit != null )
			{
				outfits.put( outfit.getName(), null );
			}
		}

		// Make a map of zodiac signs
		Map zodiacs = new TreeMap();
		int signCount = KoLCharacter.ZODIACS.length;

		for ( int i = 0; i < signCount; ++i )
		{
			String key = KoLCharacter.ZODIACS[ i ];
			String name = "Sign:" + key;
			zodiacs.put( name, null );
		}

		// Make a map of stat days
		Map statdays = new TreeMap();
		statdays.put( "Muscle Day", null );
		statdays.put( "Mysticality Day", null );
		statdays.put( "Moxie Day", null );

		// Make a map of zones
		Map zones = new TreeMap();

		it = AdventureDatabase.ZONE_DESCRIPTIONS.keySet().iterator();
		while ( it.hasNext() )
		{
			String key = (String) it.next();
			String name = "Zone:" + key;
			if ( Modifiers.getModifiers( name ) != null )
			{
				zones.put( name, null );
			}
		}

		// Make a map of locations
		Map locations = new TreeMap();

		it = AdventureDatabase.getAsLockableListModel().iterator();
		while ( it.hasNext() )
		{
			KoLAdventure key = (KoLAdventure) it.next();
			String name = "Loc:" + key.getAdventureName();
			if ( Modifiers.getModifiers( name ) != null )
			{
				locations.put( name, null );
			}
		}

		// Make a map of synergies
		Map synergies = new TreeMap();

		it = Modifiers.synergies.iterator();
		while ( it.hasNext() )
		{
			String name = (String) it.next();
			int mask = (Integer) it.next();
			synergies.put( name, null );
		}

		// Make a map of mutexes
		Map mutexes = new TreeMap();

		it = Modifiers.mutexes.iterator();
		while ( it.hasNext() )
		{
			String name = (String) it.next();
			mutexes.put( name, null );
		}

		// Make a map of maximization categories
		Map maximization = new TreeMap();
		int maximizationCount = MaximizerFrame.maximizationCategories.length;

		for ( int i = 0; i < maximizationCount; ++i )
		{
			maximization.put( MaximizerFrame.maximizationCategories[i], null );
		}

		// Open the output file
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.EQUIPMENT_VERSION );

		// For each equipment category, write the map entries
		Modifiers.writeModifierCategory( writer, hats, "Hats" );
		writer.println();
		Modifiers.writeModifierCategory( writer, pants, "Pants" );
		writer.println();
		Modifiers.writeModifierCategory( writer, shirts, "Shirts" );
		writer.println();
		Modifiers.writeModifierCategory( writer, weapons, "Weapons" );
		writer.println();
		Modifiers.writeModifierCategory( writer, offhands, "Off-hand" );
		writer.println();
		Modifiers.writeModifierCategory( writer, accessories, "Accessories" );
		writer.println();
		Modifiers.writeModifierCategory( writer, containers, "Containers" );
		writer.println();
		Modifiers.writeModifierCategory( writer, famitems, "Familiar Items" );
		writer.println();
		Modifiers.writeModifierCategory( writer, familiars, "Familiars" );
		writer.println();
		Modifiers.writeModifierCategory( writer, bedazzlements, "Bedazzlements" );
		writer.println();
		Modifiers.writeModifierCategory( writer, campground, "Campground equipment" );
		writer.println();
		Modifiers.writeModifierCategory( writer, effects, "Status Effects" );
		writer.println();
		Modifiers.writeModifierCategory( writer, passives, "Passive Skills" );
		writer.println();
		Modifiers.writeModifierCategory( writer, outfits, "Outfits" );
		writer.println();
		Modifiers.writeModifierCategory( writer, zodiacs, "Zodiac Sign" );
		writer.println();
		Modifiers.writeModifierCategory( writer, statdays, "Stat Day" );
		writer.println();
		Modifiers.writeModifierCategory( writer, zones, "Zone-specific" );
		writer.println();
		Modifiers.writeModifierCategory( writer, locations, "Location-specific" );
		writer.println();
		Modifiers.writeModifierCategory( writer, synergies, "Synergies" );
		writer.println();
		Modifiers.writeModifierCategory( writer, mutexes, "Mutual exclusions" );
		writer.println();
		Modifiers.writeModifierCategory( writer, maximization, "Maximization categories" );
		writer.println();
		Modifiers.writeModifierCategory( writer, potions, "Everything Else" );
		Modifiers.writeModifierCategory( writer, freepulls );
		Modifiers.writeModifierCategory( writer, wikiname );

		writer.close();
	}

	private static void writeModifierCategory( final PrintStream writer, final Map map, final String tag )
	{
		writer.println( "# " + tag + " section of modifiers.txt" );
		Modifiers.writeModifierCategory( writer, map );
	}

	private static void writeModifierCategory( final PrintStream writer, final Map map )
	{
		writer.println();

		Object[] keys = map.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			String cname = StringUtilities.getCanonicalName( name );
			Object modifiers = Modifiers.modifiersByName.get( cname );
			Modifiers.writeModifierItem( writer, name, modifiers );
		}
	}

	public static void writeModifierItem( final PrintStream writer, final String name, Object modifiers )
	{
		if ( modifiers == null )
		{
			Modifiers.writeModifierComment( writer, name );
			return;
		}

		if ( modifiers instanceof Modifiers )
		{
			modifiers = ((Modifiers) modifiers).getString( Modifiers.MODIFIERS );
		}

		Modifiers.writeModifierString( writer, name, (String) modifiers );
	}

	public static void writeModifierString( final PrintStream writer, final String name, final String modifiers )
	{
		writer.println( Modifiers.modifierString( name, modifiers ) );
	}

	public static String modifierString( final String name, final String modifiers )
	{
		return name + "\t" + modifiers;
	}

	public static void writeModifierComment( final PrintStream writer, final String name, final String unknown )
	{
		writer.println( Modifiers.modifierCommentString( name, unknown ) );
	}

	public static String modifierCommentString( final String name, final String unknown )
	{
		return "# " + name + ": " + unknown;
	}

	public static void writeModifierComment( final PrintStream writer, final String name )
	{
		writer.println( Modifiers.modifierCommentString( name ) );
	}

	public static String modifierCommentString( final String name )
	{
		return "# " + name;
	}

	public static void registerItem( final String name, final String text )
	{
		// Examine the item description and decide what it is.
		ArrayList unknown = new ArrayList();
		String known = DebugDatabase.parseItemEnchantments( text, unknown );
		Modifiers.registerObject( name, unknown, known );
	}

	public static void registerEffect( final String name, final String text )
	{
		// Examine the item description and decide what it is.
		ArrayList unknown = new ArrayList();
		String known = DebugDatabase.parseEffectEnchantments( text, unknown );
		Modifiers.registerObject( name, unknown, known );
	}

	private static void registerObject( final String name, final ArrayList unknown, final String known )
	{
		for ( int i = 0; i < unknown.size(); ++i )
		{
			RequestLogger.printLine( Modifiers.modifierCommentString( name, (String) unknown.get( i ) ) );
		}

		if ( known.equals( "" ) )
		{
			if ( unknown.size() == 0 )
			{
				RequestLogger.printLine( Modifiers.modifierCommentString( name ) );
			}
		}
		else
		{
			RequestLogger.printLine( Modifiers.modifierString( name, known ) );
			String canon = StringUtilities.getCanonicalName( name );
			if ( !Modifiers.modifiersByName.containsKey( canon ) )
			{
				Modifiers.modifiersByName.put( canon, known );
			}
		}
	}
}
