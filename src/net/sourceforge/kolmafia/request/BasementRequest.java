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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BasementDecorator.StatBooster;

public class BasementRequest
	extends AdventureRequest
{
	private static final float SAFETY_MARGIN = 1.08f;

	private static int basementLevel = 0;
	private static float basementTestValue = 0;
	private static float basementTestCurrent = 0;

	private static String basementTestString = "";
	public static String basementMonster = "";
	private static String gauntletString = "";

	private static int actualStatNeeded = 0;
	private static int primaryBoost = 0;
	private static int secondaryBoost = 0;

	private static float averageResistanceNeeded = 0.0f;
	private static int element1 = -1, element2 = -1;
	private static int vulnerability = 0;
	private static int goodelement = -1;
	private static AdventureResult goodphial = null;
	private static AdventureResult goodeffect = null;
	private static int badelement1 = -1, badelement2 = -1, badelement3 = -1;
	private static AdventureResult badeffect1 = null, badeffect2 = null, badeffect3 = null;

	private static ArrayList<AdventureResult> desirableEffects = new ArrayList<AdventureResult>();

	private static int level1, level2;
	private static float resistance1, resistance2;
	private static float expected1, expected2;

	private static String lastResponseText = "";
	private static String basementErrorMessage = null;

	public static final AdventureResult MUS_EQUAL = EffectPool.get( Effect.STABILIZING_OILINESS );
	public static final AdventureResult MYS_EQUAL = EffectPool.get( Effect.EXPERT_OILINESS );
	public static final AdventureResult MOX_EQUAL = EffectPool.get( Effect.SLIPPERY_OILINESS );

	private static final AdventureResult BLACK_PAINT = new AdventureResult( "Red Door Syndrome", 1, true );

	private static final AdventureResult HOT_PHIAL = ItemPool.get( ItemPool.PHIAL_OF_HOTNESS, 1 );
	private static final AdventureResult COLD_PHIAL = ItemPool.get( ItemPool.PHIAL_OF_COLDNESS, 1 );
	private static final AdventureResult SPOOKY_PHIAL = ItemPool.get( ItemPool.PHIAL_OF_SPOOKINESS, 1 );
	private static final AdventureResult STENCH_PHIAL = ItemPool.get( ItemPool.PHIAL_OF_STENCH, 1 );
	private static final AdventureResult SLEAZE_PHIAL = ItemPool.get( ItemPool.PHIAL_OF_SLEAZINESS, 1 );

	public static final AdventureResult MAX_HOT = new AdventureResult( "Fireproof Lips", 1, true );
	public static final AdventureResult MAX_COLD = new AdventureResult( "Fever from the Flavor", 1, true );
	public static final AdventureResult MAX_SPOOKY = new AdventureResult( "Hyphemariffic", 1, true );
	public static final AdventureResult MAX_STENCH = new AdventureResult( "Can't Smell Nothin'", 1, true );
	public static final AdventureResult MAX_SLEAZE = new AdventureResult( "Hyperoffended", 1, true );

	private static final AdventureResult HOT_FORM = new AdventureResult( "Hotform", 1, true );
	private static final AdventureResult COLD_FORM = new AdventureResult( "Coldform", 1, true );
	private static final AdventureResult SPOOKY_FORM = new AdventureResult( "Spookyform", 1, true );
	private static final AdventureResult STENCH_FORM = new AdventureResult( "Stenchform", 1, true );
	private static final AdventureResult SLEAZE_FORM = new AdventureResult( "Sleazeform", 1, true );

	private static final Pattern BASEMENT_PATTERN = Pattern.compile( "Level ([\\d,]+)" );

	public static final AdventureResult[] ELEMENT_PHIALS =
		new AdventureResult[]
	{
		BasementRequest.HOT_PHIAL,
		BasementRequest.COLD_PHIAL,
		BasementRequest.SPOOKY_PHIAL,
		BasementRequest.STENCH_PHIAL,
		BasementRequest.SLEAZE_PHIAL
	};

	public static final AdventureResult[] ELEMENT_FORMS =
		new AdventureResult[]
	{
		BasementRequest.HOT_FORM,
		BasementRequest.COLD_FORM,
		BasementRequest.SPOOKY_FORM,
		BasementRequest.STENCH_FORM,
		BasementRequest.SLEAZE_FORM
	};


	public static boolean isElementalImmunity( final String name )
	{
		for ( int j = 0; j < BasementRequest.ELEMENT_FORMS.length; ++j )
		{
			if ( name.equals( BasementRequest.ELEMENT_FORMS[ j ].getName() ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final FamiliarData SANDWORM =
		new FamiliarData( FamiliarPool.SANDWORM );

	/**
	 * Constructs a new <code>/code> which executes an
	 * adventure in Fernswarthy's Basement by posting to the provided form,
	 * notifying the givenof results (or errors).
	 *
	 * @param	adventureName	The name of the adventure location
	 * @param	formSource	The form to which the data will be posted
	 * @param	adventureId	The identifier for the adventure to be executed
	 */

	public BasementRequest( final String adventureName )
	{
		super( adventureName, "basement.php", "0" );
	}

	@Override
	public void run()
	{
		// Clear the data flags and probe the basement to see what we have.

        data.clear();
		super.run();

		// Load up the data variables and switch outfits if it's a fight.
		BasementRequest.checkBasement();

		// If we know we can't pass the test, give an error and bail out now.

		if ( BasementRequest.basementErrorMessage != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, BasementRequest.basementErrorMessage );
			return;
		}

		// Decide which action to set. If it's a stat reward, always
		// boost prime stat.
        addFormField( "action", BasementRequest.getBasementAction( responseText ) );

		// Attempt to pass the test.
		int lastBasementLevel = BasementRequest.basementLevel;

		super.run();

		// Handle redirection

		if ( responseCode != 200 )
		{
			// If it was a fight and we won, good.

			if ( FightRequest.INSTANCE.responseCode == 200 && FightRequest.lastResponseText.contains( "<!--WINWINWIN-->" ) )
			{
				return;
			}

			// Otherwise ... what is this? Refetch the page and see if we passed test.

            data.clear();
			super.run();
		}

		// See what basement level we are on now and fail if we've not advanced.

		if ( BasementRequest.basementLevel == lastBasementLevel )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to pass basement test." );
		}
	}

	@Override
	public void processResults()
	{
		BasementRequest.parseResponse( getURLString(), responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "basement.php" ) )
		{
			return;
		}

		BasementRequest.checkBasement( false, responseText );
	}

	public static String getBasementAction( final String text )
	{
		if ( text.contains( "Got Silk?" ) )
		{
			return KoLCharacter.isMoxieClass() ? "1" : "2";
		}
		if ( text.contains( "Save the Dolls" ) )
		{
			return KoLCharacter.isMysticalityClass() ? "1" : "2";
		}
		if ( text.contains( "Take the Red Pill" ) )
		{
			return KoLCharacter.isMuscleClass() ? "1" : "2";
		}
		return "1";
	}

	public static int getBasementLevel()
	{
		return BasementRequest.basementLevel;
	}

	public static String getBasementLevelSummary()
	{
		if ( BasementRequest.basementTestString.equals( "None" ) || BasementRequest.basementTestString.startsWith( "Monster" ) )
		{
			return "";
		}

		if ( BasementRequest.basementTestString.equals( "Elemental Resist" ) )
		{
			return BasementRequest.basementTestString + " Test: +" + BasementRequest.level1 + " " + MonsterDatabase.elementNames[ BasementRequest.element1 ] + " " + KoLConstants.COMMA_FORMAT.format( BasementRequest.resistance1 ) + "%" + ( BasementRequest.vulnerability == 1 ? " (vulnerable) " : "" )  + " (" + KoLConstants.COMMA_FORMAT.format( BasementRequest.expected1 ) + " hp), +" + BasementRequest.level2 + " " + MonsterDatabase.elementNames[ BasementRequest.element2 ] + " " + KoLConstants.COMMA_FORMAT.format( BasementRequest.resistance2 ) + "%" + ( BasementRequest.vulnerability == 2 ? " (vulnerable) " : "" ) + " (" + KoLConstants.COMMA_FORMAT.format( BasementRequest.expected2 ) + " hp)";
		}

		if ( BasementRequest.basementTestString.startsWith( "Encounter" ) )
		{
			return BasementRequest.basementTestString;
		}

		if ( BasementRequest.basementTestString.equals( "Maximum HP" ) )
		{
			return BasementRequest.basementTestString + " Test: " + KoLConstants.COMMA_FORMAT.format( BasementRequest.basementTestCurrent ) + " current, " + BasementRequest.gauntletString + " needed";
		}

		return BasementRequest.basementTestString + " Test: " + KoLConstants.COMMA_FORMAT.format( BasementRequest.basementTestCurrent ) + " current, " + KoLConstants.COMMA_FORMAT.format( BasementRequest.basementTestValue ) + " needed";
	}

	public static String getRequirement()
	{
		if ( BasementRequest.basementTestString.equals( "Elemental Resist" ) )
		{
			return "<u>" + BasementRequest.basementTestString + "</u><br/>Current: +" + BasementRequest.level1 + " " + MonsterDatabase.elementNames[ BasementRequest.element1 ] + " " + KoLConstants.COMMA_FORMAT.format( BasementRequest.resistance1 ) + "%" + ( BasementRequest.vulnerability == 1 ? " (vulnerable) " : "" ) + " (" + KoLConstants.COMMA_FORMAT.format( BasementRequest.expected1 ) + " hp), +" + BasementRequest.level2 + " " + MonsterDatabase.elementNames[ BasementRequest.element2 ] + " " + KoLConstants.COMMA_FORMAT.format( BasementRequest.resistance2 ) + "%" + ( BasementRequest.vulnerability == 2 ? " (vulnerable) " : "" ) + " (" + KoLConstants.COMMA_FORMAT.format( BasementRequest.expected2 ) + " hp)</br>" + "Needed: " + KoLConstants.COMMA_FORMAT.format( BasementRequest.averageResistanceNeeded ) + "% average resistance or " + BasementRequest.goodeffect.getName();
		}

		if ( BasementRequest.basementTestString.startsWith( "Monster" ) )
		{
			int index = BasementRequest.basementTestString.indexOf( ": " );
			if ( index == -1 )
			{
				return "";
			}

			return "<u>Monster</u><br/>" + BasementRequest.basementTestString.substring( index + 2 );
		}

		if ( BasementRequest.basementTestString.equals( "Maximum HP" ) )
		{
			return "<u>" + BasementRequest.basementTestString + "</u><br/>" + "Current: " + KoLConstants.COMMA_FORMAT.format( BasementRequest.basementTestCurrent ) + "<br/>" + "Needed: " + BasementRequest.gauntletString;
		}

		return "<u>" + BasementRequest.basementTestString + "</u><br/>" + "Current: " + KoLConstants.COMMA_FORMAT.format( BasementRequest.basementTestCurrent ) + "<br/>" + "Needed: " + KoLConstants.COMMA_FORMAT.format( BasementRequest.basementTestValue );
	}

	private static void changeBasementOutfit( final String name )
	{
		Object currentTest;
		String currentTestString;

		// Find desired outfit. Skip "No Change" entry at index 0.
		List available = EquipmentManager.getCustomOutfits();
		int count = available.size();
		for ( int i = 1; i < count; ++i )
		{
			currentTest = available.get( i );
			currentTestString = currentTest.toString().toLowerCase();

			if ( currentTestString.contains( name ) )
			{
				RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) currentTest ) );
				// Restoring to the original outfit after Basement auto-adventuring is
				// slow and pointless - you're going to want something related to the
				// current outfit to continue, not the original one.
				SpecialOutfit.forgetCheckpoints();
				return;
			}
		}
	}

	private static boolean checkForElementalTest( boolean autoSwitch, final String responseText )
	{
		if ( responseText.contains( "<b>Peace, Bra!</b>" ) )
		{
			BasementRequest.element1 = MonsterDatabase.STENCH;
			BasementRequest.element2 = MonsterDatabase.SLEAZE;

			BasementRequest.goodelement = BasementRequest.element2;
			BasementRequest.goodphial = BasementRequest.SLEAZE_PHIAL;
			BasementRequest.goodeffect = BasementRequest.SLEAZE_FORM;

			// Stench is vulnerable to Sleaze
			BasementRequest.badelement1 = MonsterDatabase.STENCH;
			BasementRequest.badeffect1 = BasementRequest.STENCH_FORM;

			// Spooky is vulnerable to Stench
			BasementRequest.badelement2 = MonsterDatabase.SPOOKY;
			BasementRequest.badeffect2 = BasementRequest.SPOOKY_FORM;

			// Hot is vulnerable to Sleaze and Stench
			BasementRequest.badelement3 = MonsterDatabase.HEAT;
			BasementRequest.badeffect3 = BasementRequest.HOT_FORM;
		}
		else if ( responseText.contains( "<b>Singled Out</b>" ) )
		{
			BasementRequest.element1 = MonsterDatabase.COLD;
			BasementRequest.element2 = MonsterDatabase.SLEAZE;

			BasementRequest.goodelement = BasementRequest.element1;
			BasementRequest.goodphial = BasementRequest.COLD_PHIAL;
			BasementRequest.goodeffect = BasementRequest.COLD_FORM;

			// Sleaze is vulnerable to Cold
			BasementRequest.badelement1 = MonsterDatabase.SLEAZE;
			BasementRequest.badeffect1 = BasementRequest.SLEAZE_FORM;

			// Stench is vulnerable to Cold
			BasementRequest.badelement2 = MonsterDatabase.STENCH;
			BasementRequest.badeffect2 = BasementRequest.STENCH_FORM;

			// Hot is vulnerable to Sleaze
			BasementRequest.badelement3 = MonsterDatabase.HEAT;
			BasementRequest.badeffect3 = BasementRequest.HOT_FORM;
		}
		else if ( responseText.contains( "<b>Still Better than Pistachio</b>" ) )
		{
			BasementRequest.element1 = MonsterDatabase.STENCH;
			BasementRequest.element2 = MonsterDatabase.HEAT;

			BasementRequest.goodelement = BasementRequest.element1;
			BasementRequest.goodphial = BasementRequest.STENCH_PHIAL;
			BasementRequest.goodeffect = BasementRequest.STENCH_FORM;

			// Cold is vulnerable to Hot
			BasementRequest.badelement1 = MonsterDatabase.COLD;
			BasementRequest.badeffect1 = BasementRequest.COLD_FORM;

			// Spooky is vulnerable to Hot
			BasementRequest.badelement2 = MonsterDatabase.SPOOKY;
			BasementRequest.badeffect2 = BasementRequest.SPOOKY_FORM;

			// Hot is vulnerable to Stench
			BasementRequest.badelement3 = MonsterDatabase.HEAT;
			BasementRequest.badeffect3 = BasementRequest.HOT_FORM;
		}
		else if ( responseText.contains( "<b>Unholy Writes</b>" ) )
		{
			BasementRequest.element1 = MonsterDatabase.HEAT;
			BasementRequest.element2 = MonsterDatabase.SPOOKY;

			BasementRequest.goodelement = BasementRequest.element1;
			BasementRequest.goodphial = BasementRequest.HOT_PHIAL;
			BasementRequest.goodeffect = BasementRequest.HOT_FORM;

			// Cold is vulnerable to Spooky
			BasementRequest.badelement1 = MonsterDatabase.COLD;
			BasementRequest.badeffect1 = BasementRequest.COLD_FORM;

			// Spooky is vulnerable to Hot
			BasementRequest.badelement2 = MonsterDatabase.SPOOKY;
			BasementRequest.badeffect2 = BasementRequest.SPOOKY_FORM;

			// Sleaze is vulnerable to Spooky
			BasementRequest.badelement3 = MonsterDatabase.SLEAZE;
			BasementRequest.badeffect3 = BasementRequest.SLEAZE_FORM;
		}
		else if ( responseText.contains( "<b>The Unthawed</b>" ) )
		{
			BasementRequest.element1 = MonsterDatabase.COLD;
			BasementRequest.element2 = MonsterDatabase.SPOOKY;

			BasementRequest.goodelement = BasementRequest.element2;
			BasementRequest.goodphial = BasementRequest.SPOOKY_PHIAL;
			BasementRequest.goodeffect = BasementRequest.SPOOKY_FORM;

			// Cold is vulnerable to Spooky
			BasementRequest.badelement1 = MonsterDatabase.COLD;
			BasementRequest.badeffect1 = BasementRequest.COLD_FORM;

			// Stench is vulnerable to Cold
			BasementRequest.badelement2 = MonsterDatabase.STENCH;
			BasementRequest.badeffect2 = BasementRequest.STENCH_FORM;

			// Sleaze is vulnerable to Cold
			BasementRequest.badelement3 = MonsterDatabase.SLEAZE;
			BasementRequest.badeffect3 = BasementRequest.SLEAZE_FORM;
		}
		else
		{
			// Not a known elemental test
			return false;
		}

		BasementRequest.actualStatNeeded = Modifiers.HP;
		BasementRequest.primaryBoost = Modifiers.MUS_PCT;
		BasementRequest.secondaryBoost = Modifiers.MUS;

		// Add the only beneficial elemental form for this test

		boolean hasGoodEffect = KoLConstants.activeEffects.contains( BasementRequest.goodeffect );

		if ( !hasGoodEffect )
		{
			BasementRequest.desirableEffects.add( BasementRequest.goodeffect );
		}

		BasementRequest.addDesiredEqualizer();

		// Add effects that resist the specific elements being tested
		// unless we have elemental immunity to that element.

		if ( BasementRequest.element1 != BasementRequest.goodelement || !hasGoodEffect )
		{
			BasementRequest.addDesirableEffects( Modifiers.getPotentialChanges( Modifiers.elementalResistance( BasementRequest.element1 ) ) );
		}

		if ( BasementRequest.element2 != BasementRequest.goodelement || !hasGoodEffect )
		{
			BasementRequest.addDesirableEffects( Modifiers.getPotentialChanges( Modifiers.elementalResistance( BasementRequest.element2 ) ) );
		}

		// Add some effects that resist all elements
		if ( !KoLConstants.activeEffects.contains( EffectPool.get( Effect.ASTRAL_SHELL ) ) )
		{
			BasementRequest.desirableEffects.add( EffectPool.get( Effect.ASTRAL_SHELL ) );
		}

		if ( !KoLConstants.activeEffects.contains( EffectPool.get( Effect.ELEMENTAL_SPHERE ) ) )
		{
			BasementRequest.desirableEffects.add( EffectPool.get( Effect.ELEMENTAL_SPHERE ) );
		}

		if ( !KoLConstants.activeEffects.contains( BasementRequest.BLACK_PAINT ) )
		{
			BasementRequest.desirableEffects.add( BasementRequest.BLACK_PAINT );
		}

		if ( BasementRequest.canHandleElementTest( autoSwitch, false ) )
		{
			return true;
		}

		if ( !autoSwitch )
		{
			return true;
		}

		BasementRequest.changeBasementOutfit( "element" );
		BasementRequest.canHandleElementTest( autoSwitch, true );
		return true;
	}

	private static boolean canHandleElementTest( boolean autoSwitch, boolean switchedOutfits )
	{
		// According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
		// total elemental damage is roughly 4.48 * x^1.4.  Assume the worst-case.

		float damage1 =
			( (float) Math.pow( BasementRequest.basementLevel, 1.4 ) * 4.48f + 8.0f ) * BasementRequest.SAFETY_MARGIN;
		float damage2 = damage1;

		BasementRequest.level1 = KoLCharacter.getElementalResistanceLevels( BasementRequest.element1 );
		BasementRequest.resistance1 = KoLCharacter.elementalResistanceByLevel( BasementRequest.level1 );
		BasementRequest.level2 = KoLCharacter.getElementalResistanceLevels( BasementRequest.element2 );
		BasementRequest.resistance2 = KoLCharacter.elementalResistanceByLevel( BasementRequest.level2 );

		if ( KoLConstants.activeEffects.contains( BasementRequest.goodeffect ) )
		{
			if ( BasementRequest.element1 == BasementRequest.goodelement )
			{
				BasementRequest.resistance1 = 100.0f;
			}
			else
			{
				BasementRequest.resistance2 = 100.0f;
			}
		}

		BasementRequest.vulnerability = 0;

		// If you have an elemental form which gives you vulnerability
		// to an element, you retain your elemental resistance (as
		// shown on the Character Sheet), but damage taken seems to be
		// quadrupled.
		if ( KoLConstants.activeEffects.contains( BasementRequest.badeffect1 ) || KoLConstants.activeEffects.contains( BasementRequest.badeffect2 ) || KoLConstants.activeEffects.contains( BasementRequest.badeffect3 ) )
		{
			if ( BasementRequest.element1 == BasementRequest.badelement1 || BasementRequest.element1 == BasementRequest.badelement2 || BasementRequest.element1 == BasementRequest.badelement3 )
			{
				BasementRequest.vulnerability = 1;
				damage1 *= 4;
			}
			else
			{
				BasementRequest.vulnerability = 2;
				damage2 *= 4;
			}
		}

		BasementRequest.expected1 = Math.max( 1.0f, damage1 * ( 100.0f - BasementRequest.resistance1 ) / 100.0f );
		BasementRequest.expected2 = Math.max( 1.0f, damage2 * ( 100.0f - BasementRequest.resistance2 ) / 100.0f );

		// If you can survive the current elemental test even without a phial,
		// then don't bother with any extra buffing.

		BasementRequest.basementTestString = "Elemental Resist";
		BasementRequest.averageResistanceNeeded =
			Math.max( 0, (int) Math.ceil( 100.0f * ( 1.0f - KoLCharacter.getMaximumHP() / ( damage1 + damage2 ) ) ) );

		BasementRequest.basementTestCurrent = KoLCharacter.getMaximumHP();
		BasementRequest.basementTestValue = BasementRequest.expected1 + BasementRequest.expected2;

		if ( BasementRequest.expected1 + BasementRequest.expected2 < KoLCharacter.getCurrentHP() )
		{
			return true;
		}

		if ( BasementRequest.expected1 + BasementRequest.expected2 < KoLCharacter.getMaximumHP() )
		{
			if ( autoSwitch )
			{
				RecoveryManager.recoverHP( (int) ( BasementRequest.expected1 + BasementRequest.expected2 ) );
			}

			return KoLmafia.permitsContinue();
		}

		// If you already have the right phial effect, check to see if
		// it's sufficient.

		if ( KoLConstants.activeEffects.contains( BasementRequest.goodeffect ) )
		{
			return false;
		}

		// If you haven't switched outfits yet, it's possible that a simple
		// outfit switch will be sufficient to buff up.

		if ( !switchedOutfits )
		{
			return false;
		}

		// If you can't survive the test, even after an outfit switch, then
		// automatically fail.

		if ( BasementRequest.expected1 >= BasementRequest.expected2 )
		{
			if ( 1.0f + BasementRequest.expected2 >= KoLCharacter.getMaximumHP() )
			{
				BasementRequest.basementErrorMessage =
					"You must have at least " + BasementRequest.basementTestValue + "% elemental resistance.";
				return false;
			}
		}
		else if ( 1.0f + BasementRequest.expected1 >= KoLCharacter.getMaximumHP() )
		{
			BasementRequest.basementErrorMessage =
				"You must have at least " + BasementRequest.basementTestValue + "% elemental resistance.";
			return false;
		}

		if ( !autoSwitch )
		{
			BasementRequest.basementErrorMessage =
				"You must have at least " + BasementRequest.basementTestValue + "% elemental resistance.";
			return false;
		}

		// You can survive, but you need an elemental phial in order to
		// do so.  Go ahead and use one, which will automatically
		// uneffect any competing phials, first

		RequestThread.postRequest( UseItemRequest.getInstance( BasementRequest.goodphial ) );

		float damage =
			BasementRequest.expected1 >= BasementRequest.expected2 ? BasementRequest.expected2 : BasementRequest.expected1;
		RecoveryManager.recoverHP( (int) ( 1.0f + damage ) );

		return KoLmafia.permitsContinue();
	}

	private static AdventureResult getDesiredEqualizer()
	{
		if ( KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality() && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie() )
		{
			return BasementRequest.MUS_EQUAL;
		}

		if ( KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle() && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie() )
		{
			return BasementRequest.MYS_EQUAL;
		}

		return BasementRequest.MOX_EQUAL;
	}

	private static void addDesiredEqualizer()
	{
		AdventureResult equalizer = BasementRequest.getDesiredEqualizer();
		if ( !KoLConstants.activeEffects.contains( equalizer ) )
		{
			BasementRequest.desirableEffects.add( equalizer );
		}
	}

	private static boolean checkForStatTest( final boolean autoSwitch, final String responseText )
	{
		// According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
		// stat requirement is x^1.4 + 2.  Assume the worst-case.

		float statRequirement =
			( (float) Math.pow( BasementRequest.basementLevel, 1.4 ) + 2.0f ) * BasementRequest.SAFETY_MARGIN;

		if ( responseText.contains( "Lift 'em" ) || responseText.contains( "Push it Real Good" ) || responseText.contains( "Ring that Bell" ) )
		{
			BasementRequest.basementTestString = "Buffed Muscle";
			BasementRequest.basementTestCurrent = KoLCharacter.getAdjustedMuscle();
			BasementRequest.basementTestValue = (int) statRequirement;

			BasementRequest.actualStatNeeded = Modifiers.MUS;
			BasementRequest.primaryBoost = Modifiers.MUS_PCT;
			BasementRequest.secondaryBoost = Modifiers.MUS;

			BasementRequest.addDesiredEqualizer();

			if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
			{
				if ( autoSwitch )
				{
					BasementRequest.changeBasementOutfit( "muscle" );
					if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
					{
						KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "maximize",
							"mus " + statRequirement + " min");
					}
				}

				if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
				{
					BasementRequest.basementErrorMessage =
						"You must have at least " + BasementRequest.basementTestValue + " muscle.";
				}
			}

			return true;
		}

		if ( responseText.contains( "Gathering:  The Magic" ) || responseText.contains( "Mop the Floor" ) || responseText.contains( "'doo" ) )
		{
			BasementRequest.basementTestString = "Buffed Mysticality";
			BasementRequest.basementTestCurrent = KoLCharacter.getAdjustedMysticality();
			BasementRequest.basementTestValue = (int) statRequirement;

			BasementRequest.actualStatNeeded = Modifiers.MYS;
			BasementRequest.primaryBoost = Modifiers.MYS_PCT;
			BasementRequest.secondaryBoost = Modifiers.MYS;

			BasementRequest.addDesiredEqualizer();

			if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
			{
				if ( autoSwitch )
				{
					BasementRequest.changeBasementOutfit( "mysticality" );
					if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
					{
						KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "maximize",
							"mys " + statRequirement + " min");
					}
				}

				if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
				{
					BasementRequest.basementErrorMessage =
						"You must have at least " + BasementRequest.basementTestValue + " mysticality.";
				}
			}

			return true;
		}

		if ( responseText.contains( "Don't Wake the Baby" ) || responseText.contains( "Grab a cue" ) || responseText.contains( "Smooth Moves" ) )
		{
			BasementRequest.basementTestString = "Buffed Moxie";
			BasementRequest.basementTestCurrent = KoLCharacter.getAdjustedMoxie();
			BasementRequest.basementTestValue = (int) statRequirement;

			BasementRequest.actualStatNeeded = Modifiers.MOX;
			BasementRequest.primaryBoost = Modifiers.MOX_PCT;
			BasementRequest.secondaryBoost = Modifiers.MOX;

			BasementRequest.addDesiredEqualizer();

			if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
			{
				if ( autoSwitch )
				{
					BasementRequest.changeBasementOutfit( "moxie" );
					if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
					{
						KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "maximize",
							"mox " + statRequirement + " min");
					}
				}

				if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
				{
					BasementRequest.basementErrorMessage =
						"You must have at least " + BasementRequest.basementTestValue + " moxie.";
				}
			}

			return true;
		}

		return false;
	}

	private static boolean checkForDrainTest( final boolean autoSwitch, final String responseText )
	{

		if ( responseText.contains( "Grab the Handles" ) )
		{
			// According to
			// http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
			// drain requirement is 1.67 * x^1.4 Assume worst-case.

			float drainRequirement =
				(float) Math.pow( BasementRequest.basementLevel, 1.4 ) * 1.67f * BasementRequest.SAFETY_MARGIN;

			BasementRequest.basementTestString = "Maximum MP";
			BasementRequest.basementTestCurrent = KoLCharacter.getMaximumMP();
			BasementRequest.basementTestValue = (int) drainRequirement;

			BasementRequest.actualStatNeeded = Modifiers.MP;
			if ( StatBooster.moxieControlsMP() )
			{
				BasementRequest.primaryBoost = Modifiers.MOX_PCT;
				BasementRequest.secondaryBoost = Modifiers.MOX;
			}
			else
			{
				BasementRequest.primaryBoost = Modifiers.MYS_PCT;
				BasementRequest.secondaryBoost = Modifiers.MYS;
			}

			BasementRequest.addDesiredEqualizer();

			if ( KoLCharacter.getMaximumMP() < drainRequirement )
			{
				if ( autoSwitch )
				{
					BasementRequest.changeBasementOutfit( "mpdrain" );
					if ( KoLCharacter.getMaximumMP() < drainRequirement )
					{
						KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "maximize",
							"MP " + drainRequirement + " min");
					}
				}

				if ( KoLCharacter.getMaximumMP() < drainRequirement )
				{
					BasementRequest.basementErrorMessage = "Insufficient mana to continue.";
					return true;
				}
			}

			if ( autoSwitch )
			{
				RecoveryManager.recoverMP( (int) drainRequirement );
			}

			return true;
		}

		if ( responseText.contains( "Run the Gauntlet Gauntlet" ) )
		{
			// According to starwed at
			// http://forums.kingdomofloathing.com/viewtopic.php?t=83342&start=201
			// drain requirement is 10.0 * x^1.4. Assume worst-case.

			float drainRequirement =
				(float) Math.pow( BasementRequest.basementLevel, 1.4 ) * 10.0f * BasementRequest.SAFETY_MARGIN;

			BasementRequest.basementTestString = "Maximum HP";
			BasementRequest.basementTestCurrent = KoLCharacter.getMaximumHP();

			BasementRequest.actualStatNeeded = Modifiers.HP;
			BasementRequest.primaryBoost = Modifiers.MUS_PCT;
			BasementRequest.secondaryBoost = Modifiers.MUS;

			BasementRequest.addDesiredEqualizer();

			// Add some effects that improve Damage Absorption
			if ( !KoLConstants.activeEffects.contains( EffectPool.get( Effect.ASTRAL_SHELL ) ) )
			{
				BasementRequest.desirableEffects.add( EffectPool.get( Effect.ASTRAL_SHELL ) );
			}

			if ( !KoLConstants.activeEffects.contains( EffectPool.get( Effect.GHOSTLY_SHELL ) ) )
			{
				BasementRequest.desirableEffects.add( EffectPool.get( Effect.GHOSTLY_SHELL ) );
			}

			float damageAbsorb =
				1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
			float healthRequirement = drainRequirement * damageAbsorb;

			BasementRequest.basementTestValue = (int) healthRequirement;
			BasementRequest.gauntletString =
				(int) drainRequirement + " * " + KoLConstants.FLOAT_FORMAT.format( damageAbsorb ) + " (" + KoLCharacter.getDamageAbsorption() + " DA) = " + KoLConstants.COMMA_FORMAT.format( healthRequirement );

			if ( KoLCharacter.getMaximumHP() < healthRequirement )
			{
				if ( autoSwitch )
				{
					BasementRequest.changeBasementOutfit( "gauntlet" );

					damageAbsorb =
						1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
					healthRequirement = drainRequirement * damageAbsorb;
					BasementRequest.basementTestValue = (int) healthRequirement;
				}

				if ( KoLCharacter.getMaximumHP() < healthRequirement )
				{
					BasementRequest.basementErrorMessage = "Insufficient health to continue.";
					return true;
				}
			}

			if ( autoSwitch )
			{
				RecoveryManager.recoverHP( (int) healthRequirement );
			}

			return true;
		}

		return false;
	}

	private static boolean checkForReward( final String responseText )
	{
		if ( responseText.contains( "De Los Dioses" ) )
		{
			BasementRequest.basementTestString = "Encounter: De Los Dioses";
			return true;
		}

		if ( responseText.contains( "The Dusk Zone" ) )
		{
			BasementRequest.basementTestString = "Encounter: The Dusk Zone";
			return true;
		}

		if ( responseText.contains( "Giggity Bobbity Boo!" ) )
		{
			BasementRequest.basementTestString = "Encounter: Giggity Bobbity Boo!";
			return true;
		}

		if ( responseText.contains( "No Good Deed" ) )
		{
			BasementRequest.basementTestString = "Encounter: No Good Deed";
			return true;
		}

		if ( responseText.contains( "<b>Fernswarthy's Basement, Level 500</b>" ) )
		{
			BasementRequest.basementTestString = "Encounter: Fernswarthy's Basement, Level 500";
			return true;
		}

		if ( responseText.contains( "Got Silk?" ) )
		{
			BasementRequest.basementTestString = "Encounter: Got Silk?/Leather is Betther";
			return true;
		}

		if ( responseText.contains( "Save the Dolls" ) )
		{
			BasementRequest.basementTestString = "Encounter: Save the Dolls/Save the Cardboard";
			return true;
		}

		if ( responseText.contains( "Take the Red Pill" ) )
		{
			BasementRequest.basementTestString = "Encounter: Take the Red Pill/Take the Blue Pill";
			return true;
		}

		return false;
	}

	private static String monsterLevelString()
	{
		float level =
			2.0f * (float) Math.pow( BasementRequest.basementLevel, 1.4 ) + KoLCharacter.getMonsterLevelAdjustment();
		return "Monster: Attack/Defense = " + (int) level;
	}

	private static boolean checkForMonster( final String responseText )
	{
		if ( responseText.contains( "Don't Fear the Ear" ) )
		{
			// The Beast with n Ears
			BasementRequest.basementMonster = "The Beast with n Ears";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString();
			return true;
		}

		if ( responseText.contains( "Commence to Pokin" ) )
		{
			// The Beast with n Eyes
			BasementRequest.basementMonster = "The Beast with n Eyes";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString();
			return true;
		}

		if ( responseText.contains( "Stone Golem" ) )
		{
			// A n Stone Golem
			BasementRequest.basementMonster = "A n Stone Golem";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString();
			return true;
		}

		if ( responseText.contains( "Hydra" ) )
		{
			// A n-Headed Hydra
			BasementRequest.basementMonster = "A n-Headed Hydra";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString();
			return true;
		}

		if ( responseText.contains( "Toast that Ghost" ) )
		{
			// The Ghost of Fernswarthy's n great-grandfather
			BasementRequest.basementMonster = "The Ghost of Fernswarthy's n great-grandfather";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString() + "<br>Physically resistant";
			return true;
		}

		if ( responseText.contains( "Bottles of Beer on a Golem" ) )
		{
			// N Bottles of Beer on a Golem
			BasementRequest.basementMonster = "n Bottles of Beer on a Golem";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString() + "<br>Blocks most spells";
			return true;
		}

		if ( responseText.contains( "Collapse That Waveform" ) )
		{
			// A n-Dimensional Horror
			BasementRequest.basementMonster = "A n-Dimensional Horror";
			BasementRequest.basementTestString = BasementRequest.monsterLevelString() + "<br>Blocks physical attacks";
			return true;
		}

		return false;
	}

	private static void newBasementLevel( final String responseText )
	{
		BasementRequest.basementErrorMessage = null;
		BasementRequest.basementTestString = "None";
		BasementRequest.basementTestValue = 0;

		BasementRequest.element1 = -1;
		BasementRequest.element2 = -1;
		BasementRequest.vulnerability = 0;

		BasementRequest.goodelement = -1;
		BasementRequest.goodphial = null;
		BasementRequest.goodeffect = null;

		BasementRequest.badeffect1 = null;
		BasementRequest.badeffect2 = null;
		BasementRequest.badeffect3 = null;
		BasementRequest.badelement1 = -1;
		BasementRequest.badelement2 = -1;
		BasementRequest.badelement3 = -1;

		Matcher levelMatcher = BasementRequest.BASEMENT_PATTERN.matcher( responseText );
		if ( !levelMatcher.find() )
		{
			return;
		}

		BasementRequest.basementLevel = StringUtilities.parseInt( levelMatcher.group( 1 ) );
	}

	public static void checkBasement()
	{
		BasementRequest.checkBasement( true, BasementRequest.lastResponseText );
	}

	public static boolean checkBasement( final boolean autoSwitch, final String responseText )
	{
		BasementRequest.lastResponseText = responseText;

		BasementRequest.desirableEffects.clear();
		BasementRequest.newBasementLevel( responseText );

		if ( BasementRequest.checkForReward( responseText ) )
		{
			return false;
		}

		if ( BasementRequest.checkForElementalTest( autoSwitch, responseText ) )
		{
			return true;
		}

		if ( BasementRequest.checkForStatTest( autoSwitch, responseText ) )
		{
			return true;
		}

		if ( BasementRequest.checkForDrainTest( autoSwitch, responseText ) )
		{
			return true;
		}

		if ( !BasementRequest.checkForMonster( responseText ) )
		{
			return false;
		}

		BasementRequest.basementTestCurrent = 0;
		BasementRequest.basementTestValue = 0;

		BasementRequest.actualStatNeeded = Modifiers.HP;
		BasementRequest.primaryBoost = Modifiers.MUS_PCT;
		BasementRequest.secondaryBoost = Modifiers.MUS;

		BasementRequest.addDesiredEqualizer();

		if ( autoSwitch )
		{
			BasementRequest.changeBasementOutfit( "damage" );
		}

		return true;
	}

	private static void getStatBoosters( final ArrayList<AdventureResult> sourceList, final ArrayList<StatBooster> targetList )
	{
		// Cache skills to avoid lots of string lookups
		StatBooster.checkSkills();

		Iterator<AdventureResult> it = sourceList.iterator();

		while ( it.hasNext() )
		{
			AdventureResult effect = it.next();
			if ( !BasementRequest.wantEffect( effect ) )
			{
				continue;
			}

			StatBooster addition = new StatBooster( effect.getName() );

			if ( !targetList.contains( addition ) )
			{
				targetList.add( addition );
			}
		}
	}

	private static void addDesirableEffects( final ArrayList<AdventureResult> sourceList )
	{
		Iterator<AdventureResult> it = sourceList.iterator();

		while ( it.hasNext() )
		{
			AdventureResult effect = it.next();
			if ( BasementRequest.wantEffect( effect ) && !BasementRequest.desirableEffects.contains( effect ) )
			{
				BasementRequest.desirableEffects.add( effect );
			}
		}
	}

	private static boolean wantEffect( final AdventureResult effect )
	{
		String action = MoodManager.getDefaultAction( "lose_effect", effect.getName() );
		if ( action.equals( "" ) )
		{
			return false;
		}

		if ( action.startsWith( "cast" ) )
		{
			if ( !KoLCharacter.hasSkill( UneffectRequest.effectToSkill( effect.getName() ) ) )
			{
				return false;
			}
		}

		return true;
	}

	public static ArrayList<StatBooster> getStatBoosters()
	{
		ArrayList<StatBooster> targetList = new ArrayList<StatBooster>();

		BasementRequest.getStatBoosters( BasementRequest.desirableEffects, targetList );

		BasementRequest.getStatBoosters( Modifiers.getPotentialChanges( BasementRequest.primaryBoost ), targetList );
		BasementRequest.getStatBoosters( Modifiers.getPotentialChanges( BasementRequest.secondaryBoost ), targetList );

		if ( BasementRequest.actualStatNeeded == Modifiers.HP )
		{
			BasementRequest.getStatBoosters( Modifiers.getPotentialChanges( Modifiers.HP_PCT ), targetList );
			BasementRequest.getStatBoosters( Modifiers.getPotentialChanges( Modifiers.HP ), targetList );
		}
		else if ( BasementRequest.actualStatNeeded == Modifiers.MP )
		{
			BasementRequest.getStatBoosters( Modifiers.getPotentialChanges( Modifiers.MP_PCT ), targetList );
			BasementRequest.getStatBoosters( Modifiers.getPotentialChanges( Modifiers.MP ), targetList );
		}

		Collections.sort( targetList );
		return targetList;
	}

	public static int getBasementTestCurrent()
	{
		return (int) BasementRequest.basementTestCurrent;
	}

	public static int getBasementTestValue()
	{
		return (int) BasementRequest.basementTestValue;
	}

	public static int getActualStatNeeded()
	{
		return BasementRequest.actualStatNeeded;
	}

	public static int getPrimaryBoost()
	{
		return BasementRequest.primaryBoost;
	}

	public static int getSecondaryBoost()
	{
		return BasementRequest.secondaryBoost;
	}
}
