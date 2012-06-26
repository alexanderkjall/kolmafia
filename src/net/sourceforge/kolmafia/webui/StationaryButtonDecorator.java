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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StationaryButtonDecorator
{
	private static final ArrayList combatHotkeys = new ArrayList();

	private static final boolean builtInSkill( final String skillId )
	{
		// Entangling Noodles
		if ( skillId.equals( "3004" ) )
		{
			return true;
		}

		// Transcendent Olfaction
		if ( skillId.equals( "19" ) )
		{
			return true;
		}
   
		return false;
	}

	public static final void addButton( final String skillId )
	{
		if ( skillId == null || skillId.equals( "none" ) )
		{
			return;
		}

		// Don't add a button for using a built-in skill
		if ( StationaryButtonDecorator.builtInSkill( skillId ) )
		{
			return;
		}

		int buttons = Preferences.getInteger( "relaySkillButtonCount" );
		int maximumIndex = buttons + 1;
		int insertIndex = 0;

		// Examine all buttons and find a place for this skill.
		for ( int i = 1; i < maximumIndex; )
		{
			String old = Preferences.getString( "stationaryButton" + i );
			// Remove built-in skills.
			if ( StationaryButtonDecorator.builtInSkill( old ) )
			{
				StationaryButtonDecorator.removeButton( i, buttons );
				continue;
			}

			// If the button is already there, use it.
			if ( old.equals( skillId ) )
			{
				insertIndex = i;
			}

			// If we already have an insertion point, keep it
			else if ( insertIndex != 0 )
			{
			}

			// Choose first unused button.
			else if ( old.equals( "" ) || old.equals( "none" ) )
			{
				insertIndex = i;
			}
			++i;
		}

		// If all buttons are in use, remove oldest and insert at end.
		if ( insertIndex == 0 )
		{
			StationaryButtonDecorator.removeButton( 1, buttons );
			insertIndex = buttons;
		}

		Preferences.setString( "stationaryButton" + insertIndex, skillId );
	}

	private static final void removeButton( final int index, int buttons )
	{
		for ( int i = index + 1; i <= buttons; ++i )
		{
			String next = Preferences.getString( "stationaryButton" + i );
			Preferences.setString( "stationaryButton" + ( i - 1 ), next );
		}
	}

	public static final void decorate( final String urlString, final StringBuffer buffer )
	{
		if ( Preferences.getBoolean( "hideServerDebugText" ) )
		{
			int beginDebug = buffer.indexOf( "<div style='max-height" );
			if ( beginDebug != -1 )
			{
				int endDebug = buffer.indexOf( "</div>", beginDebug ) + 6;
				buffer.delete( beginDebug, endDebug );
			}
		}

		if ( Preferences.getBoolean( "serverAddsCustomCombat" ) )
		{
			int imageIndex = buffer.indexOf( "<td><img src='http://images.kingdomofloathing.com/itemimages/book3.gif' id='skills'>" );
			if ( imageIndex != -1 )
			{
				boolean again = FightRequest.getCurrentRound() == 0;
				String location = again ? getAdventureAgainLocation( buffer ) : "fight.php?action=custom";

				// Add a "script" button to the left

				buffer.insert( imageIndex, "<td><a href='" + location + "'><img src='http://images.kingdomofloathing.com/itemimages/plexpock.gif'></td><td class=spacer></td>" );

				// Give it either the "script" or "again" label
				int labelIndex = buffer.indexOf( "<tr class=label>", imageIndex ) + 16;

				buffer.insert( labelIndex, again ? "<td>again</td><td></td>" : "<td>script</td><td></td>" );

				// Also add spacers to the header
				labelIndex = buffer.indexOf( "<tbody><tr class=label><td></td><td></td><td>1</td><td>2</td>" ) + 23;
				buffer.insert( labelIndex, "<td></td><td></td>" );
			}
			else
			{
				// We are going to craft our own CAB. Pull in
				// the necessary Javascript.

				int insertIndex = buffer.indexOf( "</head>" );
				buffer.insert( insertIndex, "<link rel='stylesheet' type='text/css' href='http://images.kingdomofloathing.com/actionbar.6.css'><!--[if IE]><link rel='stylesheet' type='text/css' href='http://images.kingdomofloathing.com/actionbar.ie.4.css'><![endif]-->" );

				// Build the CAB in a new StringBuffer

				StringBuilder CAB = new StringBuilder();
				boolean choice = buffer.indexOf( "<input" ) != -1;

				CAB.append( "<img src='http://images.kingdomofloathing.com/itemimages/blank.gif' id='dragged'>" );
				CAB.append( "<div id='debug'></div>" );
				CAB.append( "<div class=contextmenu id='skillmenu'></div>" );
				CAB.append( "<div class=contextmenu id='itemsmenu'></div>" );

				// *** Start of 'topbar' div
				CAB.append( "<div id=topbar>" );
				CAB.append( "<center><table class=actionbar cellpadding=0 cellspacing=1><tbody>" );

				// *** Row 1 of table: class=label cols=19
				CAB.append( "<tr class=label>" );
				//     Column 1
				CAB.append( "<td>&nbsp;</td>" );
				//     Column 2-19
				for ( int i = 2; i <= 19; ++i )
				{
					CAB.append( "<td></td>" );
				}
				CAB.append( "</tr>" );

				// *** Row 2 of table: class=blueback cols=19
				CAB.append( "<tr class=blueback>" );
				//     Column 1
				CAB.append( "<td><a href='" );
				CAB.append( choice ? "choice.php?action=auto" : getAdventureAgainLocation( buffer ) );
				CAB.append( "'><img src='http://images.kingdomofloathing.com/itemimages/plexpock.gif'></td>" );
				//     Column 2
				CAB.append( "<td class=spacer></td>" );
				//     Column 3
				CAB.append( "<td><img src='http://images.kingdomofloathing.com/itemimages/blank.gif' id='skills'></td>" );
				//     Column 4
				CAB.append( "<td class=spacer></td>" );
				//     Column 5-16
				for ( int i = 5; i <= 16; ++i )
				{
					CAB.append( "<td><img src='http://images.kingdomofloathing.com/itemimages/blank.gif'></td>" );
				}
				//     Column 17
				CAB.append( "<td class=spacer></td>" );
				//     Column 18
				CAB.append( "<td class=spacer></td>" );
				//     Column 19
				CAB.append( "<td><img src='http://images.kingdomofloathing.com/itemimages/blank.gif'></td>" );
				CAB.append( "</tr>" );

				// *** Row 3 of table: class=label cols=19
				CAB.append( "<tr class=label>" );
				//	Column 1
				CAB.append( "<td>" );
				CAB.append( choice ? "auto" : "again" );
				CAB.append( "</td>" );
				//	Column 2-19
				for ( int i = 2; i < 19; ++i )
				{
					CAB.append( "<td></td>" );
				}
				CAB.append( "</tr>" );
				CAB.append( "</tbody></table></center>" );

				CAB.append( "</div>" );
				// *** End of 'topbar' div

				// *** Start of 'content' div
				CAB.append( "<div class='content' id='content_'>" );
				CAB.append( "<div id='effdiv' style='display: none;'></div>" );

				// *** Start of 'overflow' div
				CAB.append( "<div style='overflow: auto;'>" );

				insertIndex = buffer.indexOf( "<body>" ) + 6;
				buffer.insert( insertIndex, CAB.toString() );

				
				insertIndex = buffer.indexOf( "</body>" );
				buffer.insert( insertIndex, "</div></div>" );
				// *** End of 'overflow' div
				// *** End of 'content' div
			}

			return;
		}

		if ( !Preferences.getBoolean( "relayAddsCustomCombat" ) )
		{
			return;
		}

		int insertionPoint = buffer.indexOf( "<body" );
		if ( insertionPoint == -1 )
		{
			return;
		}
		insertionPoint = buffer.indexOf( ">", insertionPoint ) + 1;

		StringBuffer actionBuffer = new StringBuffer();

		actionBuffer.append( "<div id=\"mafiabuttons\"><center>" );
		actionBuffer.append( "<table width=\"95%\"><tr><td align=left>" );

		StationaryButtonDecorator.addFightButton( urlString, buffer, actionBuffer, "attack", true );

		boolean inBirdForm = KoLConstants.activeEffects.contains( FightRequest.BIRDFORM );
		if ( inBirdForm || KoLCharacter.isMoxieClass() )
		{
			StationaryButtonDecorator.addFightButton(
				urlString, buffer, actionBuffer, "steal", FightRequest.canStillSteal() );
		}

		if ( !inBirdForm && 
		     KoLCharacter.getClassName().equals( "Pastamancer" ) &&
		     !Preferences.getString( "pastamancerGhostType" ).equals( "" ))
		{
			boolean enabled = FightRequest.getCurrentRound() > 0 &&
				FightRequest.canStillSummon();
			StationaryButtonDecorator.addFightButton(
				urlString, buffer, actionBuffer, "summon", enabled );
		}

		if ( EquipmentManager.usingChefstaff() )
		{
			boolean enabled = FightRequest.getCurrentRound() > 0;
			StationaryButtonDecorator.addFightButton(
				urlString, buffer, actionBuffer, "jiggle", enabled );
		}

		if ( !inBirdForm && KoLCharacter.hasSkill( "Entangling Noodles" ) )
		{
			boolean enabled = FightRequest.getCurrentRound() > 0 &&
				FightRequest.canCastNoodles();
			StationaryButtonDecorator.addFightButton(
				urlString, buffer, actionBuffer, "3004", enabled );
		}

		if ( !inBirdForm && KoLCharacter.hasSkill( "Transcendent Olfaction" ) )
		{
			boolean enabled = FightRequest.getCurrentRound() > 0 &&
				FightRequest.canOlfact();
			StationaryButtonDecorator.addFightButton(
				urlString, buffer, actionBuffer, "19", enabled );
		}

		StationaryButtonDecorator.addFightButton(
			urlString, buffer, actionBuffer, "script", FightRequest.getCurrentRound() > 0 );

		int buttons = Preferences.getInteger( "relaySkillButtonCount" );
		for ( int i = 1; i <= buttons; ++i )
		{
			String action = Preferences.getString( "stationaryButton" + i );
			if ( action.equals( "" ) || action.equals( "none" ) )
			{
				continue;
			}

			// We use Skill IDs for button actions, but users can screw them up.
			if ( !StringUtilities.isNumeric( action ) )
			{
				action = String.valueOf( SkillDatabase.getSkillId( action ) );
			}

			String name = SkillDatabase.getSkillName( Integer.parseInt( action ) );
			boolean hasSkill = name != null && KoLCharacter.hasSkill( name );

			boolean remove = false;

			// If it's a completely bogus skill id, flush it
			if ( name == null )
			{
				remove = true;
			}
			// If we are in bird form, we can only use birdform skills.
			else if ( inBirdForm )
			{
				// Birdform skills do not appear on our list of
				// known skills. Display only unknown skills
				// but keep known skills in the preferences
				if ( hasSkill )
				{
					continue;
				}
			}
			// Otherwise, remove unknown skills from preferences
			else if ( !hasSkill )
			{
				remove = true;
			}

			if ( remove )
			{
				for ( int j = i; j < buttons; ++j )
				{
					Preferences.setString(
						"stationaryButton" + j, Preferences.getString( "stationaryButton" + ( j + 1 ) ) );
				}

				Preferences.setString( "stationaryButton" + buttons, "" );
				--i;	// retry with the skill that's now in this position
				continue;
			}

			// Show this skill.
			StationaryButtonDecorator.addFightButton(
				urlString,
				buffer,
				actionBuffer,
				action,
				FightRequest.getCurrentRound() > 0 );
		}

		// Add conditionally available combat skills
		// parsed from the fight page

		for ( int i = 0; i < KoLConstants.availableConditionalSkills.size(); ++i )
		{
			UseSkillRequest current = (UseSkillRequest) KoLConstants.availableConditionalSkills.get( i );
			String action = String.valueOf( current.getSkillId() );

			StationaryButtonDecorator.addFightButton(
				urlString,
				buffer,
				actionBuffer,
				action,
				FightRequest.getCurrentRound() > 0 );
		}

		if ( StationaryButtonDecorator.combatHotkeys.isEmpty() )
		{
			StationaryButtonDecorator.reloadCombatHotkeyMap();
		}

		actionBuffer.append( "</td><td align=right>" );
		actionBuffer.append( "<select id=\"hotkeyViewer\" onchange=\"updateCombatHotkey();\">" );

		actionBuffer.append( "<option>- update hotkeys -</option>" );

		for ( int i = 0; i < StationaryButtonDecorator.combatHotkeys.size(); ++i )
		{
			actionBuffer.append( "<option>" );
			actionBuffer.append( i );
			actionBuffer.append( ": " );

			actionBuffer.append( StationaryButtonDecorator.combatHotkeys.get( i ) );
			actionBuffer.append( "</option>" );
		}

		actionBuffer.append( "</select>" );

		actionBuffer.append( "</td></tr></table>" );
		actionBuffer.append( "</center></div>" );
		buffer.insert( insertionPoint, actionBuffer.toString() );

		StringUtilities.insertBefore( buffer, "</html>", "<script src=\"/hotkeys.js\"></script>" );
		if ( !Preferences.getBoolean( "macroLens" ) )
		{	// this would make it impossible to type numbers in the macro field!
			StringUtilities.insertAfter( buffer, "<body", " onkeyup=\"handleCombatHotkey(event,false);\" onkeydown=\"handleCombatHotkey(event,true);\" " );
		}
	}

	private static final void addFightButton( final String urlString, final StringBuffer response,
		final StringBuffer buffer, final String action, boolean isEnabled )
	{
		boolean forceFocus = action.equals( "attack" );

		String name = StationaryButtonDecorator.getActionName( action );
		buffer.append( "<input type=\"button\" onClick=\"document.location.href='" );

		int body = response.indexOf( "<body>" );
		if ( urlString.startsWith( "choice.php" ) && response.indexOf( "choice.php", body + 1 ) != -1 )
		{
			if ( forceFocus )
			{
				name = "auto";
			}

			buffer.append( "choice.php?action=auto" );
		}
		else if ( FightRequest.getCurrentRound() == 0 )
		{
			String location = getAdventureAgainLocation( response );
			buffer.append( location );
			isEnabled &= !location.equals( "main.php" );
		}
		else
		{
			buffer.append( "fight.php?" );

			if ( action.equals( "script" ) )
			{
				buffer.append( "action=" );

				if ( urlString.endsWith( "action=script" ) && isEnabled )
				{
					name = "abort";
					buffer.append( "abort" );
				}
				else
				{
					buffer.append( "custom" );
				}
			}
			else if ( action.equals( "attack" ) || action.equals( "steal" ) )
			{
				buffer.append( "action=" );
				buffer.append( action );
			}
			else if ( action.equals( "jiggle" ) )
			{
				buffer.append( "action=chefstaff" );
				isEnabled &= !FightRequest.alreadyJiggled();
			}
			else if ( action.equals( "summon" ) )
			{
				buffer.append( "action=summon" );
				isEnabled &= FightRequest.canStillSummon();
			}
			else
			{
				buffer.append( "action=skill&whichskill=" );
				buffer.append( action );
				isEnabled &=
					SkillDatabase.getMPConsumptionById( StringUtilities.parseInt( action ) ) <= KoLCharacter.getCurrentMP();
			}
		}

		buffer.append( "';void(0);\" value=\"" );
		buffer.append( name );

		buffer.append( "\"" );

		if ( forceFocus )
		{
			buffer.append( " id=\"defaultButton\"" );
		}

		if ( !isEnabled )
		{
			buffer.append( " disabled" );
		}

		buffer.append( ">&nbsp;" );
	}

	private static final String getAdventureAgainLocation( StringBuffer response )
	{
		// Get the "adventure again" link from the page
		int startIndex = response.indexOf( "<a href=\"", response.indexOf( "<body" ) + 1 );
		if ( startIndex != -1 )
		{
			return response.substring( startIndex + 9, response.indexOf( "\"", startIndex + 10 ) );
		}

		// If there is none, perhaps we fought a monster as a result of
		// using an item.

		String monster = FightRequest.getLastMonsterName();

		if ( monster.equals( "giant sandworm" ) )
		{
			AdventureResult drumMachine = ItemPool.get( ItemPool.DRUM_MACHINE, 1 );
			if ( KoLConstants.inventory.contains( drumMachine ) )
			{
				return "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=" + ItemPool.DRUM_MACHINE;
			}

			// Look for more drum machines in the Oasis
			return "adventure.php?snarfblat=122";
		}

		if ( monster.equals( "scary pirate" ) )
		{
			return "inv_use.php?pwd=" + GenericRequest.passwordHash +"&which=3&whichitem=" + ItemPool.CURSED_PIECE_OF_THIRTEEN;
		}

		return "main.php";
	}

	private static final String getActionName( final String action )
	{
		if ( action.equals( "attack" ) )
		{
			return FightRequest.getCurrentRound() == 0 ? "again" : "attack";
		}

		if ( action.equals( "steal" ) || action.equals( "jiggle" ) || action.equals( "summon" ) || action.equals( "script" ) )
		{
			return action;
		}

		int skillId = StringUtilities.parseInt( action );
		String name = SkillDatabase.getSkillName( skillId ).toLowerCase();

		switch ( skillId )
		{
		case 15:	// CLEESH
		case 7002:	// Shake Hands
		case 7003:	// Hot Breath
		case 7004:	// Cold Breath
		case 7005:	// Spooky Breath
		case 7006:	// Stinky Breath
		case 7007:	// Sleazy Breath
			name = StringUtilities.globalStringDelete( name, " " );
			break;

		case 7001:	// Give In To Your Vampiric Urges
			name = "bakula";
			break;

		case 7008:	// Moxious Maneuver
			name = "moxman";
			break;

		case 7010:	// red bottle-rocket
		case 7011:	// blue bottle-rocket
		case 7012:	// orange bottle-rocket
		case 7013:	// purple bottle-rocket
		case 7014:	// black bottle-rocket
			name = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( name, "fire " ), "bottle-" );
			break;

		case 2103:	// Head + Knee Combo
		case 2105:	// Head + Shield Combo
		case 2106:	// Knee + Shield Combo
		case 2107:	// Head + Knee + Shield Combo
			name = name.substring( 0, name.length() - 6 );
			break;

		case 1003:	// thrust-smack
			name = "thrust";
			break;

		case 1004:	// lunge-smack
		case 1005:	// lunging thrust-smack
			name = "lunge";
			break;

		case 2:		// Chronic Indigestion
		case 7009:	// Magic Missile
		case 3004:	// Entangling Noodles
		case 3009:	// Lasagna Bandages
		case 3019:	// Fearful Fettucini
		case 19:	// Transcendent Olfaction
		case 7063:	// Falling Leaf Whirlwind
			name = name.substring( name.lastIndexOf( " " ) + 1 );
			break;

		case 50:	// Break It On Down
		case 51:	// Pop and Lock It
		case 52:	// Run Like the WInd
		case 3003:	// Minor Ray of Something
		case 3005:	// eXtreme Ray of Something
		case 3007:	// Cone of Whatever
		case 3008:	// Weapon of the Pastalord
		case 3020:	// Spaghetti Spear
		case 4003:	// Stream of Sauce
		case 4009:	// Wave of Sauce
		case 5019:	// Tango of Terror
		case 7061:	// Spring Raindrop Attack
		case 7062:	// Summer Siesta
		case 7064:	// Winter's Bite Technique
			name = name.substring( 0, name.indexOf( " " ) );
			break;

		case 5003:	// Disco Eye-Poke
			name = "eyepoke";
			break;

		case 5005:	// Disco Dance of Doom
			name = "dance1";
			break;

		case 5008:	// Disco Dance II: Electric Boogaloo
			name = "dance2";
			break;

		case 5012:	// Disco Face Stab
			name = "facestab";
			break;
		}

		return name;
	}

	public static final void reloadCombatHotkeyMap()
	{
		StationaryButtonDecorator.combatHotkeys.clear();

		for ( int i = 0; i <= 9; ++i )
		{
			StationaryButtonDecorator.combatHotkeys.add( Preferences.getString( "combatHotkey" + i ) );
		}
	}
}
