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

package net.sourceforge.kolmafia.webui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BasementRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BasementDecorator
{
	public static void decorate( final StringBuffer buffer )
	{
		addBasementButtons( buffer );

		if ( buffer.indexOf( "Got Silk?" ) != -1 )
		{
			BasementDecorator.addBasementChoiceSpoilers( buffer, "Moxie", "Muscle" );
			return;
		}

		if ( buffer.indexOf( "Save the Dolls" ) != -1 )
		{
			BasementDecorator.addBasementChoiceSpoilers( buffer, "Mysticality", "Moxie" );
			return;
		}

		if ( buffer.indexOf( "Take the Red Pill" ) != -1 )
		{
			BasementDecorator.addBasementChoiceSpoilers( buffer, "Muscle", "Mysticality" );
			return;
		}

		addBasementSpoilers( buffer );
	}

	private static void addBasementButtons( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayAddsCustomCombat" ) )
		{
			return;
		}

		int insertionPoint = buffer.indexOf( "<tr" );
		if ( insertionPoint != -1 )
		{
			StringBuffer actionBuffer = new StringBuffer();
			actionBuffer.append( "<tr><td align=left>" );

			BasementDecorator.addBasementButton( "action", buffer, actionBuffer, "auto", true );
			BasementDecorator.addBasementButton( "rebuff", buffer, actionBuffer, "rebuff", false );
			BasementDecorator.addBasementButton( "", buffer, actionBuffer, "refresh", true );

			actionBuffer.append( "</td></tr><tr><td><font size=1>&nbsp;</font></td></tr>" );
			buffer.insert( insertionPoint, actionBuffer.toString() );
		}
	}

	private static void addBasementButton( final String parameter, final StringBuffer response,
		final StringBuffer buffer, final String action, final boolean isEnabled )
	{
		buffer.append( "<input type=\"button\" onClick=\"" );

		if ( parameter.startsWith( "rebuff" ) )
		{
			buffer.append( "runBasementScript(); void(0);" );
		}
		else
		{
			buffer.append( "document.location.href='basement.php" );

			if ( parameter.equals( "action" ) )
			{
				buffer.append( "?action=" );
				buffer.append( BasementRequest.getBasementAction( response.toString() ) );
			}

			buffer.append( "'; void(0);" );
		}

		buffer.append( "\" value=\"" );
		buffer.append( action );
		buffer.append( "\"" + ( isEnabled ? "" : " disabled" ) + ">&nbsp;" );
	}

	public static void addBasementSpoilers( final StringBuffer buffer )
	{
		if ( !BasementRequest.checkBasement( false, buffer.toString() ) )
		{
			return;
		}

		buffer.insert(
			buffer.indexOf( "</head>" ), "<script language=\"Javascript\" src=\"/basement.js\"></script></head>" );

		StringBuffer changes = new StringBuffer();
		changes.append( "<table id=\"basementhelper\" style=\"width:100%;\">" );
		changes.append( "<tr><td style=\"width:90%;\"><select id=\"gear\" style=\"width: 100%;\"><option value=\"none\">- change your equipment -</option>" );

		// Add outfits. Skip the "No Change" entry at index 0.

		List outfits = EquipmentManager.getCustomOutfits();
		int count = outfits.size();
		for ( int i = 1; i < count; ++i )
		{
			SpecialOutfit outfit = (SpecialOutfit) outfits.get( i );

			changes.append( "<option value=\"outfit " );
			changes.append( outfit.getName() );
			changes.append( "\">outfit " );
			changes.append( outfit.getName() );
			changes.append( "</option>" );
		}

		for ( Iterator i = KoLCharacter.getFamiliarList().iterator(); i.hasNext(); )
		{
			FamiliarData fam = (FamiliarData) i.next();
			boolean useful = false;
			switch ( fam.getId() )
			{
			case FamiliarPool.HAND:			
			case FamiliarPool.SANDWORM:
			case FamiliarPool.PARROT:
			case FamiliarPool.PRESSIE:
			case FamiliarPool.RIFTLET:
			case FamiliarPool.GIBBERER:
			case FamiliarPool.HARE:
				useful = true;
				break;
			case FamiliarPool.SOMBRERO:
				useful = !KoLCharacter.getFamiliarList().contains( BasementRequest.SANDWORM );
				break;
			case FamiliarPool.BADGER:
				useful = Preferences.getInteger( "_astralDrops" ) < 5;
				break;
			case FamiliarPool.PIXIE:
				useful = Preferences.getInteger( "_absintheDrops" ) < 5;
				break;
			case FamiliarPool.LLAMA:
				useful = Preferences.getInteger( "_gongDrops" ) < 5;
				break;
			case FamiliarPool.TRON:
				useful = Preferences.getInteger( "_tokenDrops" ) < 5;
				break;
			}
			if ( !useful ) continue;

			changes.append( "<option value=\"familiar " );
			changes.append( fam.getRace() );
			changes.append( "\">familiar " );
			changes.append( fam.getRace() );
			changes.append( "</option>" );
		}

		changes.append( "</select></td><td>&nbsp;</td><td style=\"vertical-align:top; text-align:left;\"><input type=\"button\" value=\"exec\" onClick=\"changeBasementGear();\"></td></tr>" );

		// Add effects

		ArrayList listedEffects = BasementRequest.getStatBoosters();

		if ( !listedEffects.isEmpty() )
		{
			String computeFunction =
				"computeNetBoost(" + BasementRequest.getBasementTestCurrent() + "," + BasementRequest.getBasementTestValue() + ");";

			String modifierName = Modifiers.getModifierName( BasementRequest.getActualStatNeeded() );
			modifierName = StringUtilities.globalStringDelete( modifierName, "Maximum " ).toLowerCase();

			changes.append( "<tr><td style=\"width:90%;\"><select onchange=\"" );
			changes.append( computeFunction );
			changes.append( "\" id=\"potion\" style=\"width: 100%;\" multiple size=5>" );

			if ( KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() )
			{
				if ( KoLCharacter.hasSkill( "Cannelloni Cocoon" ) )
				{
					changes.append( "<option value=0>cast Cannelloni Cocoon (hp restore)</option>" );
				}
				else
				{
					changes.append( "<option value=0>use 1 scroll of drastic healing (hp restore)</option>" );
				}
			}

			if ( KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
			{
				changes.append( "<option value=0" );

				if ( KoLCharacter.getFullness() == KoLCharacter.getFullnessLimit() )
				{
					changes.append( " disabled" );
				}

				changes.append( ">eat 1 Jumbo Dr. Lucifer (mp restore)</option>" );
			}

			for ( int i = 0; i < listedEffects.size(); ++i )
			{
				StatBooster booster = (StatBooster) listedEffects.get( i );
				BasementDecorator.appendBasementEffect( changes, booster );
			}

			changes.append( "</select></td><td>&nbsp;</td><td style=\"vertical-align:top; text-align:left;\">" );
			changes.append( "<input type=\"button\" value=\"exec\" onClick=\"changeBasementEffects();\">" );
 			changes.append( "<br/><br/><font size=-1><nobr id=\"changevalue\">" );
			changes.append( BasementRequest.getBasementTestCurrent() );
			changes.append( "</nobr><br/><nobr id=\"changetarget\">" );
			changes.append( BasementRequest.getBasementTestValue() );
			changes.append( "</nobr></td></tr>" );
		}

		changes.append( "</table>" );
		buffer.insert( buffer.indexOf( "</center><blockquote>" ), changes.toString() );

		String checkString = BasementRequest.getRequirement();
		buffer.insert( buffer.lastIndexOf( "</b>" ) + 4, "<br/>" );
		buffer.insert( buffer.lastIndexOf( "<img" ), "<table><tr><td>" );
		buffer.insert(
			buffer.indexOf( ">", buffer.lastIndexOf( "<img" ) ) + 1,
			"</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><font id=\"spoiler\" size=2>" + checkString + "</font></td></tr></table>" );
	}

	private static void addBasementChoiceSpoilers( final StringBuffer buffer, final String choice1,
		final String choice2 )
	{
		String text = buffer.toString();

		// Update level string and such for the session log.
		BasementRequest.checkBasement( false, text );

		buffer.setLength( 0 );

		int index1 = 0, index2;

		// Add first choice spoiler
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice1 + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add second choice spoiler
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice2 + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Append remainder of buffer
		buffer.append( text.substring( index1 ) );
	}

	private static void appendBasementEffect( final StringBuffer changes, final StatBooster effect )
	{
		changes.append( "<option value=" );
		changes.append( effect.getEffectiveBoost() );

		if ( effect.disabled() )
		{
			changes.append( " disabled" );
		}

		changes.append( ">" );

		if ( !effect.itemAvailable() )
		{
			if ( Preferences.getInteger( "basementMallPrices" ) > 0 )
			{
				changes.append( "acquire (~" );
				changes.append( KoLConstants.COMMA_FORMAT.format(
					effect.getItemPrice() * effect.getItem().getCount() ) );
				changes.append( " meat) &amp; " );
			}
			else
			{
				changes.append( "acquire &amp; " );
			}
		}
		else if ( effect.getItem() != null && 
			Preferences.getInteger( "basementMallPrices" ) > 1 )
		{
			changes.append( "(~" );
			changes.append( KoLConstants.COMMA_FORMAT.format(
				effect.getItemPrice() * effect.getItem().getCount() ) );
			changes.append( " meat) " );
		}

		changes.append( effect.getAction() );
		changes.append( " (" );

		String effectName = effect.getName();

		if ( effect.getComputedBoost() == 0.0f )
		{
			if ( effectName.equals( Effect.ASTRAL_SHELL.effectName() ) )
			{
				changes.append( "damage absorption/element resist" );
			}
			else if ( effect.isStatEqualizer() )
			{
				changes.append( "stat equalizer" );
			}
			else if ( effect.isDamageAbsorption() )
			{
				changes.append( "damage absorption" );
			}
			else if ( effect.isElementalImmunity() )
			{
				changes.append( "element immunity" );
			}
			else
			{
				changes.append( "element resist" );
			}
		}
		else
		{
			changes.append( "+" );
			changes.append( KoLConstants.COMMA_FORMAT.format( effect.getEffectiveBoost() ) );
		}

		changes.append( ")</option>" );
	}

	public static class StatBooster
		implements Comparable
	{
		private final String name, action;
		private final int computedBoost;
		private final int effectiveBoost;
		private AdventureResult item;
		private boolean itemAvailable;
		private int fullness;
		private int spleen;
		private int inebriety;
		private boolean isDamageAbsorption;
		private boolean isElementalImmunity;
		private boolean isStatEqualizer;

		private static boolean moxieControlsMP = false;

		private static boolean absOfTin = false;
		private static boolean gnomishHardigness = false;
		private static boolean gnomishUgnderstanding = false;
		private static boolean marginallyInsane = false;
		private static boolean spiritOfRavioli = false;
		private static boolean wisdomOfTheElderTortoise = false;

		private static final AdventureResult MOXIE_MAGNET = new AdventureResult( 519, 1 );
		private static final AdventureResult TRAVOLTAN_TROUSERS = new AdventureResult( 1792, 1 );

		public StatBooster( final String name )
		{
			this.name = name;

            computedBoost = computeBoost();
            effectiveBoost = computedBoost > 0.0f ? computedBoost : 0 - computedBoost;

            action =
                    computedBoost < 0 ? "uneffect " + name : MoodManager.getDefaultAction( "lose_effect", name );

            item = null;
            itemAvailable = true;
            fullness = 0;
            spleen = 0;
            inebriety = 0;
            isDamageAbsorption =
				this.name.equals( Effect.ASTRAL_SHELL.effectName() ) || this.name.equals( Effect.GHOSTLY_SHELL.effectName() );
            isElementalImmunity = BasementRequest.isElementalImmunity( this.name );
            isStatEqualizer =
				this.name.equals( Effect.EXPERT_OILINESS.effectName() ) || this.name.equals( Effect.SLIPPERY_OILINESS.effectName() ) || this.name.equals( Effect.STABILIZING_OILINESS.effectName() );

			if ( action.startsWith( "use" ) ||
                    action.startsWith( "chew" ) ||
                    action.startsWith( "drink" ) ||
                    action.startsWith( "eat" ) )
			{
				int index = action.indexOf( " " ) + 1;
                item = ItemFinder.getFirstMatchingItem( action.substring( index ).trim(), false );
				if ( item != null )
				{
                    itemAvailable = InventoryManager.hasItem( item );
                    fullness = ItemDatabase.getFullness( item.getName() );
                    spleen = ItemDatabase.getSpleenHit( item.getName() );
                    inebriety = ItemDatabase.getInebriety( item.getName() );
				}
			}
		}

		public static boolean moxieControlsMP()
		{
			// With Moxie Magnet, uses Moxie, not Mysticality
			if ( KoLCharacter.hasEquipped( MOXIE_MAGNET ) )
				return true;

			// Ditto if Travoltan trousers and Mox > Mys
			if ( KoLCharacter.hasEquipped( TRAVOLTAN_TROUSERS ) )
				return KoLCharacter.getAdjustedMoxie() > KoLCharacter.getAdjustedMysticality();

			return false;
		}

		public final boolean isDamageAbsorption()
		{
			return isDamageAbsorption;
		}

		public final boolean isElementalImmunity()
		{
			return isElementalImmunity;
		}

		public final boolean isStatEqualizer()
		{
			return isStatEqualizer;
		}

		public static void checkSkills()
		{
			StatBooster.moxieControlsMP = moxieControlsMP();

			StatBooster.absOfTin = KoLCharacter.hasSkill( "Abs of Tin" );
			StatBooster.gnomishHardigness = KoLCharacter.hasSkill( "Gnomish Hardigness" );
			StatBooster.gnomishUgnderstanding = KoLCharacter.hasSkill( "Cosmic Ugnderstanding" );
			StatBooster.marginallyInsane = KoLCharacter.hasSkill( "Marginally Insane" );
			StatBooster.spiritOfRavioli = KoLCharacter.hasSkill( "Spirit of Ravioli" );
			StatBooster.wisdomOfTheElderTortoise = KoLCharacter.hasSkill( "Wisdom of the Elder Tortoises" );
		}

		@Override
		public boolean equals( final Object o )
		{
			return o instanceof StatBooster && name.equals( ((StatBooster) o).name );
		}

		public int compareTo( final Object o )
		{
			if ( effectiveBoost == 0.0f )
			{
				if ( ( (StatBooster) o ).effectiveBoost != 0.0f )
				{
					return -1;
				}
				if ( isElementalImmunity )
				{
					return -1;
				}
				if ( ( (StatBooster) o ).isElementalImmunity )
				{
					return 1;
				}
				return name.compareToIgnoreCase( ((StatBooster) o).name );
			}

			if ( ( (StatBooster) o ).effectiveBoost == 0.0f )
			{
				return 1;
			}

			if ( effectiveBoost != ( (StatBooster) o ).effectiveBoost )
			{
				return effectiveBoost > ( (StatBooster) o ).effectiveBoost ? -1 : 1;
			}

			return name.compareToIgnoreCase( ((StatBooster) o).name );
		}

		public String getName()
		{
			return name;
		}

		public AdventureResult getItem()
		{
			return item;
		}
		
		public int getItemPrice()
		{
			if ( item == null ) return 0;
			if ( MallPriceDatabase.getAge( item.getItemId() ) > 7.0f )
			{
				StoreManager.getMallPrice( item );
			}
			return MallPriceDatabase.getPrice( item.getItemId() );
		}

		public boolean itemAvailable()
		{
			return itemAvailable;
		}

		public int getFullness()
		{
			return spleen;
		}

		public int getSpleen()
		{
			return spleen;
		}

		public int getInebriety()
		{
			return inebriety;
		}

		public boolean disabled()
		{
			if ( item == null )
			{
				if ( action.startsWith( "concert " ) &&
					Preferences.getBoolean( "concertVisited" ) )
				{
					return true;
				}
	
				if ( action.startsWith( "telescope " ) &&
					Preferences.getBoolean( "telescopeLookedHigh" ) )
				{
					return true;
				}
	
				return false;
			}

			if ( fullness > 0 && ( KoLCharacter.getFullness() + fullness) > KoLCharacter.getFullnessLimit() )
			{
				return true;
			}

			if ( spleen > 0 && ( KoLCharacter.getSpleenUse() + spleen) > KoLCharacter.getSpleenLimit() )
			{
				return true;
			}

			if ( inebriety > 0 && ( KoLCharacter.getInebriety() + inebriety) > KoLCharacter.getInebrietyLimit() )
			{
				return true;
			}
			
			return false;
		}

		public int getComputedBoost()
		{
			return computedBoost;
		}

		public int getEffectiveBoost()
		{
			return effectiveBoost;
		}

		public String getAction()
		{
			return action;
		}

		public int computeBoost()
		{
			Modifiers m = Modifiers.getModifiers( name );
			if ( m == null )
			{
				return 0;
			}

			if ( BasementRequest.getActualStatNeeded() == Modifiers.HP )
			{
				return StatBooster.boostMaxHP( m );
			}

			if ( BasementRequest.getActualStatNeeded() == Modifiers.MP )
			{
				return StatBooster.boostMaxMP( m );
			}

			float base = StatBooster.getEqualizedStat( BasementRequest.getPrimaryBoost() );
			float boost =
				m.get( BasementRequest.getSecondaryBoost() ) + m.get( BasementRequest.getPrimaryBoost() ) * base / 100.0f;

			return (int) Math.ceil( boost );
		}

		public static float getEqualizedStat( final int mod )
		{
			float currentStat = 0.0f;

			switch ( mod )
			{
			case Modifiers.MUS_PCT:
				currentStat = KoLCharacter.getBaseMuscle();
				break;
			case Modifiers.MYS_PCT:
				currentStat = KoLCharacter.getBaseMysticality();
				break;
			case Modifiers.MOX_PCT:
				currentStat = KoLCharacter.getBaseMoxie();
				break;
			default:
				return 0.0f;
			}

			if ( KoLConstants.activeEffects.contains( BasementRequest.MUS_EQUAL ) )
			{
				currentStat = Math.max( KoLCharacter.getBaseMuscle(), currentStat );
			}

			if ( KoLConstants.activeEffects.contains( BasementRequest.MYS_EQUAL ) )
			{
				currentStat = Math.max( KoLCharacter.getBaseMysticality(), currentStat );
			}

			if ( KoLConstants.activeEffects.contains( BasementRequest.MOX_EQUAL ) )
			{
				currentStat = Math.max( KoLCharacter.getBaseMoxie(), currentStat );
			}

			return currentStat;
		}

		public static int boostMaxHP( final Modifiers m )
		{
			float addedMuscleFixed = m.get( Modifiers.MUS );
			float addedMusclePercent = m.get( Modifiers.MUS_PCT );
			int addedHealthFixed = (int) m.get( Modifiers.HP );

			if ( addedMuscleFixed == 0.0f && addedMusclePercent == 0.0f && addedHealthFixed == 0 )
			{
				return 0;
			}

			float muscleBase = StatBooster.getEqualizedStat( Modifiers.MUS_PCT );
			float muscleBonus = addedMuscleFixed + (float) Math.floor( addedMusclePercent * muscleBase / 100.0f );
			float muscleMultiplicator = 1.0f;

			if ( KoLCharacter.isMuscleClass() )
			{
				muscleMultiplicator += 0.5f;
			}

			if ( StatBooster.absOfTin )
			{
				muscleMultiplicator += 0.10f;
			}

			if ( StatBooster.gnomishHardigness )
			{
				muscleMultiplicator += 0.05f;
			}

			if ( StatBooster.spiritOfRavioli )
			{
				muscleMultiplicator += 0.25f;
			}

			return (int) Math.ceil( muscleBonus * muscleMultiplicator ) + addedHealthFixed;
		}

		public static int boostMaxMP( final Modifiers m )
		{
			int statModifier;
			int statPercentModifier;

			if ( StatBooster.moxieControlsMP )
			{
				statModifier = Modifiers.MOX;
				statPercentModifier = Modifiers.MOX_PCT;
			}
			else
			{
				statModifier = Modifiers.MYS;
				statPercentModifier = Modifiers.MYS_PCT;
			}

			float addedStatFixed = m.get( statModifier );
			float addedStatPercent = m.get( statPercentModifier );
			int addedManaFixed = (int) m.get( Modifiers.MP );

			if ( addedStatFixed == 0.0f && addedStatPercent == 0.0f && addedManaFixed == 0.0f )
			{
				return 0;
			}

			float statBase = StatBooster.getEqualizedStat( statPercentModifier );
			float manaBonus = addedStatFixed + addedStatPercent * statBase / 100.0f ;
			float manaMultiplicator = 1.0f;

			if ( KoLCharacter.isMysticalityClass() )
			{
				manaMultiplicator += 0.5f;
			}

			if ( StatBooster.gnomishUgnderstanding )
			{
				manaMultiplicator += 0.05f;
			}

			if ( StatBooster.marginallyInsane )
			{
				manaMultiplicator += 0.1f;
			}

			if ( StatBooster.wisdomOfTheElderTortoise )
			{
				manaMultiplicator += 0.5f;
			}

			return (int) Math.ceil( manaBonus * manaMultiplicator ) + addedManaFixed;
		}
	}
}
