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

package net.sourceforge.kolmafia.moods;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MoodManager
{
	private static final AdventureResult[] AUTO_CLEAR =
	{
		new AdventureResult( "Beaten Up", 1, true ),
		new AdventureResult( "Tetanus", 1, true ),
		new AdventureResult( "Amnesia", 1, true ),
		new AdventureResult( "Cunctatitis", 1, true ),
		new AdventureResult( "Hardly Poisoned at All", 1, true ),
		new AdventureResult( "Majorly Poisoned", 1, true ),
		new AdventureResult( "A Little Bit Poisoned", 1, true ),
		new AdventureResult( "Somewhat Poisoned", 1, true ),
		new AdventureResult( "Really Quite Poisoned", 1, true ),
	};

	public static final AdventureResult TURTLING_ROD = ItemPool.get( ItemPool.TURTLING_ROD, 1 );
	public static final AdventureResult EAU_DE_TORTUE = EffectPool.get( Effect.EAU_DE_TORTUE );

	private static Mood currentMood = null;
	private static final SortedListModel availableMoods = new SortedListModel();
	private static final SortedListModel displayList = new SortedListModel();
	
	static boolean isExecuting = false;

	public static File getFile()
	{
		return new File( UtilityConstants.SETTINGS_LOCATION, KoLCharacter.baseUserName() + "_moods.txt" );
	}

	public static boolean isExecuting()
	{
		return MoodManager.isExecuting;
	}

	public static void updateFromPreferences()
	{
		MoodTrigger.clearKnownSources();
		MoodManager.availableMoods.clear();

		MoodManager.currentMood = null;
		MoodManager.displayList.clear();

		String currentMood = Preferences.getString( "currentMood" );
		MoodManager.loadSettings();

		MoodManager.setMood( currentMood );
		MoodManager.saveSettings();
	}

	public static SortedListModel getAvailableMoods()
	{
		return MoodManager.availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given mood. Also ensures that all defaults are loaded for the given
	 * mood if no data exists.
	 */

	public static void setMood( String newMoodName )
	{
		if ( newMoodName == null || newMoodName.trim().equals( "" ) )
		{
			newMoodName = "default";
		}

		if ( newMoodName.equals( "clear" ) || newMoodName.equals( "autofill" ) || newMoodName.startsWith( "exec" ) || newMoodName.startsWith( "repeat" ) )
		{
			return;
		}

		Preferences.setString( "currentMood", newMoodName );
		
		Mood newMood = new Mood( newMoodName );
		Iterator moodIterator = MoodManager.availableMoods.iterator();
		
		MoodManager.currentMood = null;
		
		while ( moodIterator.hasNext() )
		{
			Mood mood = (Mood) moodIterator.next();
			
			if ( mood.equals( newMood ) )
			{
				MoodManager.currentMood = mood;
				
				if ( newMoodName.contains( " extends " ) || newMoodName.contains( "," ) )
				{
					MoodManager.currentMood.setParentNames( newMood.getParentNames() );
				}
				
				break;
			}
		}

		if ( MoodManager.currentMood == null )
		{
			MoodManager.currentMood = newMood;			
			MoodManager.availableMoods.remove( MoodManager.currentMood );
			MoodManager.availableMoods.add( MoodManager.currentMood );
		}

		MoodManager.displayList.clear();
		MoodManager.displayList.addAll( MoodManager.currentMood.getTriggers() );
		
		MoodManager.availableMoods.setSelectedItem( MoodManager.currentMood );
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static SortedListModel getTriggers()
	{
		return MoodManager.displayList;
	}
	
	public static List getTriggers( String moodName )
	{
		if ( moodName == null || moodName.length() == 0 )
		{
			return Collections.EMPTY_LIST;
		}
		
		Mood moodToFind = new Mood( moodName );
		
		Iterator moodIterator = MoodManager.availableMoods.iterator();

		while ( moodIterator.hasNext() )
		{
			Mood mood = (Mood) moodIterator.next();
			
			if ( mood.equals( moodToFind ) )
			{
				return mood.getTriggers();
			}
		}
		
		return Collections.EMPTY_LIST;
	}

	public static void addTriggers( final Object[] nodes, final int duration )
	{
		MoodManager.removeTriggers( nodes );
		StringBuilder newAction = new StringBuilder();

		for ( int i = 0; i < nodes.length; ++i )
		{
			MoodTrigger mt = (MoodTrigger) nodes[ i ];
			String[] action = mt.getAction().split( " " );

			newAction.setLength( 0 );
			newAction.append( action[ 0 ] );

			if ( action.length > 1 )
			{
				newAction.append( ' ' );
				int startIndex = 2;

				if ( action[ 1 ].charAt( 0 ) == '*' )
				{
					newAction.append( '*' );
				}
				else
				{
					if ( !Character.isDigit( action[ 1 ].charAt( 0 ) ) )
					{
						startIndex = 1;
					}

					newAction.append( duration );
				}

				for ( int j = startIndex; j < action.length; ++j )
				{
					newAction.append( ' ' );
					newAction.append( action[ j ] );
				}
			}

			MoodManager.addTrigger( mt.getType(), mt.getName(), newAction.toString() );
		}
	}

	/**
	 * Adds a trigger to the temporary mood settings.
	 */

	public static MoodTrigger addTrigger( final String type, final String name, final String action )
	{
		MoodTrigger trigger = MoodTrigger.constructNode( type + " " + name + " => " + action );

		if ( MoodManager.currentMood.addTrigger( trigger ) )
		{
			MoodManager.displayList.remove( trigger );
			MoodManager.displayList.add( trigger );
		}

		return trigger;
	}

	/**
	 * Removes all the current displayList.
	 */

	public static void removeTriggers( final Object[] triggers )
	{
		for ( int i = 0; i < triggers.length; ++i )
		{
			MoodTrigger trigger = (MoodTrigger) triggers[ i ];

			if ( MoodManager.currentMood.removeTrigger( trigger ) )
			{
				MoodManager.displayList.remove( trigger );
			}
		}
	}

	public static void removeTriggers( final Collection triggers )
	{
		Iterator it = triggers.iterator();
		while ( it.hasNext() )
		{
			MoodTrigger trigger = (MoodTrigger) it.next();
			if ( MoodManager.currentMood.removeTrigger( trigger ) )
			{
				MoodManager.displayList.remove( trigger );
			}
		}
	}

	public static void minimalSet()
	{
		String currentMood = Preferences.getString( "currentMood" );
		if ( currentMood.equals( "apathetic" ) )
		{
			return;
		}

		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult[] effects = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = MoodManager.getDefaultAction( "lose_effect", effects[ i ].getName() );
			if ( action != null && !action.equals( "" ) )
			{
				MoodManager.addTrigger( "lose_effect", effects[ i ].getName(), action );
			}
		}
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	private static final String [] hardcoreThiefBuffs = new String[]
	{
		"Fat Leon's Phat Loot Lyric",
		"The Moxious Madrigal",
		"Aloysius' Antiphon of Aptitude",
		"The Sonata of Sneakiness",
		"The Psalm of Pointiness",
		"Ur-Kel's Aria of Annoyance"
	};

	private static final String [] softcoreThiefBuffs = new String[]
	{
		"Fat Leon's Phat Loot Lyric",
		"Aloysius' Antiphon of Aptitude",
		"Ur-Kel's Aria of Annoyance",
		"The Sonata of Sneakiness",
		"Jackasses' Symphony of Destruction",
		"Cletus's Canticle of Celerity"
	};

	private static final String [] rankedBorisSongs = new String[]
	{
		"Song of Fortune",
		"Song of Accompaniment",
		// Can't actually pick the following, since it is in the same
		// skill tree as the preceding Songs
		"Song of Solitude",
		"Song of Cockiness",
	};

	public static void maximalSet()
	{
		String currentMood = Preferences.getString( "currentMood" );
		if ( currentMood.equals( "apathetic" ) )
		{
			return;
		}

		UseSkillRequest[] skills = new UseSkillRequest[ KoLConstants.availableSkills.size() ];
		KoLConstants.availableSkills.toArray( skills );

		ArrayList thiefSkills = new ArrayList();
		ArrayList borisSongs = new ArrayList();

		for ( int i = 0; i < skills.length; ++i )
		{
			int skillId = skills[ i ].getSkillId();

			if ( skillId < 1000 )
			{
				continue;
			}

			// Combat rate increasers are not handled by mood
			// autofill, since KoLmafia has a preference for
			// non-combats in the area below.
			// Musk of the Moose, Carlweather's Cantata of Confrontation,
			// Song of Battle

			if ( skillId == 1019 || skillId == 6016 || skillId == 11019 )
			{
				continue;
			}

			// Skip skills that aren't mood appropriate because they add effects
			// outside of battle.
			// Canticle of Carboloading, The Ode to Booze,
			// Inigo's Incantation of Inspiration, Song of the Glorious Lunch

			if ( skillId == 3024 || skillId == 6014 || skillId == 6028 || skillId == 11023 )
			{
				continue;
			}

			String skillName = skills[ i ].getSkillName();

			if ( skillId > 6000 && skillId < 7000 )
			{
				thiefSkills.add( skillName );
				continue;
			}

			if ( skillId >= 11000 && skillId < 12000 )
			{
				if ( SkillDatabase.isSong( skillId ) )
				{
					borisSongs.add( skillName );
					continue;
				}
			}

			String effectName = UneffectRequest.skillToEffect( skillName );
			if ( EffectDatabase.contains( effectName ) )
			{
				String action = MoodManager.getDefaultAction( "lose_effect", effectName );
				MoodManager.addTrigger( "lose_effect", effectName, action );
			}
		}

		// If we know Boris Songs, pick one
		if ( !borisSongs.isEmpty() )
		{
			MoodManager.pickSkills( borisSongs, 1, MoodManager.rankedBorisSongs );
		}

		// If we know Accordion Thief Songs, pick some
		if ( !thiefSkills.isEmpty() )
		{
			String[] rankedBuffs =
				KoLCharacter.isHardcore() ?
				MoodManager.hardcoreThiefBuffs :
				MoodManager.softcoreThiefBuffs;
			MoodManager.pickSkills( thiefSkills, UseSkillRequest.songLimit(), rankedBuffs );
		}

		// Now add in all the buffs from the minimal buff set, as those
		// are included here.

		MoodManager.minimalSet();
	}

	private static void pickSkills( final List skills, final int limit, final String [] rankedBuffs )
	{
		if ( skills.isEmpty() )
		{
			return;
		}

		int skillCount = skills.size();

		// If we know fewer skills than our capacity, add them all

		if ( skillCount <= limit )
		{
			String[] skillNames = new String[ skillCount ];
			skills.toArray( skillNames );

			for ( int i = 0; i < skillNames.length; ++i )
			{
				String effectName = UneffectRequest.skillToEffect( skillNames[ i ] );
				MoodManager.addTrigger( "lose_effect", effectName, "cast " + skillNames[ i ] );
			}

			return;
		}

		// Otherwise, pick from the ranked list of "useful" skills

		int foundSkillCount = 0;
		for ( int i = 0; i < rankedBuffs.length && foundSkillCount < limit; ++i )
		{
			if ( KoLCharacter.hasSkill( rankedBuffs[ i ] ) )
			{
				String effectName =  UneffectRequest.skillToEffect( rankedBuffs[ i ] );
				MoodManager.addTrigger( "lose_effect",effectName, "cast " + rankedBuffs[ i ] );
				++foundSkillCount;
			}
		}
	}

	/**
	 * Deletes the current mood and sets the current mood to apathetic.
	 */

	public static void deleteCurrentMood()
	{
		MoodManager.displayList.clear();

		Mood current = MoodManager.currentMood;
		if ( current.getName().equals( "default" ) )
		{
			MoodManager.removeTriggers( current.getTriggers() );
			return;
		}

		MoodManager.availableMoods.remove( current );
		MoodManager.setMood( "apathetic" );
	}

	/**
	 * Duplicates the current trigger list into a new list
	 */

	public static void copyTriggers( final String newMoodName )
	{
		// Copy displayList from current list, then
		// create and switch to new list

		Mood newMood = new Mood( newMoodName );
		newMood.copyFrom( MoodManager.currentMood );
		
		MoodManager.availableMoods.add( newMood );
		MoodManager.setMood( newMoodName );
	}

	/**
	 * Executes all the mood displayList for the current mood.
	 */

	public static void execute()
	{
		MoodManager.execute( -1 );
	}

	public static boolean effectInMood( final AdventureResult effect )
	{
		return MoodManager.currentMood.isTrigger( effect );
	}

	public static void execute( final int multiplicity )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return;
		}

		if ( !MoodManager.willExecute( multiplicity ) )
		{
			return;
		}

		MoodManager.isExecuting = true;

		MoodTrigger current = null;

		AdventureResult[] effects = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effects );

		// If you have too many accordion thief buffs to execute
		// your displayList, then shrug off your extra buffs, but
		// only if the user allows for this.

		// First we determine which buffs are already affecting the
		// character in question.

		ArrayList thiefBuffs = new ArrayList();
		for ( int i = 0; i < effects.length; ++i )
		{
			String skillName = UneffectRequest.effectToSkill( effects[ i ].getName() );
			if ( SkillDatabase.contains( skillName ) )
			{
				int skillId = SkillDatabase.getSkillId( skillName );
				if ( skillId > 6000 && skillId < 7000 )
				{
					thiefBuffs.add( effects[ i ] );
				}
			}
		}

		// Then, we determine the triggers which are thief skills, and
		// thereby would be cast at this time.

		ArrayList thiefKeep = new ArrayList();
		ArrayList thiefNeed = new ArrayList();
		
		List triggers = MoodManager.currentMood.getTriggers();
		
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			current = (MoodTrigger) triggerIterator.next();

			if ( current.isThiefTrigger() )
			{
				AdventureResult effect = current.getEffect();
				
				if ( thiefBuffs.remove( effect ) )
				{	// Already have this one
					thiefKeep.add( effect );
				}
				else
				{	// New or completely expired buff - we may
					// need to shrug a buff to make room for it.
					thiefNeed.add( effect );
				}
			}
		}

		int buffsToRemove = thiefNeed.isEmpty() ? 0 :
			thiefBuffs.size() + thiefKeep.size() + thiefNeed.size() - UseSkillRequest.songLimit();

		for ( int i = 0; i < buffsToRemove && i < thiefBuffs.size(); ++i )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "uneffect " + ( (AdventureResult) thiefBuffs.get( i ) ).getName() );
		}

		// Now that everything is prepared, go ahead and execute
		// the displayList which have been set.  First, start out
		// with any skill casting.
		
		triggerIterator = triggers.iterator();

		while ( !KoLmafia.refusesContinue() && triggerIterator.hasNext() )
		{
			current = (MoodTrigger) triggerIterator.next();

			if ( current.isSkill() )
			{
				current.execute( multiplicity );
			}
		}

		triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			current = (MoodTrigger) triggerIterator.next();

			if ( !current.isSkill() )
			{
				current.execute( multiplicity );
			}
		}

		MoodManager.isExecuting = false;
	}

	public static boolean willExecute( final int multiplicity )
	{
		if ( !MoodManager.currentMood.isExecutable() )
		{
			return false;
		}

		boolean willExecute = false;
		
		List triggers = MoodManager.currentMood.getTriggers();
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();
			willExecute |= current.shouldExecute( multiplicity );
		}

		return willExecute;
	}

	public static List getMissingEffects()
	{
		List triggers = MoodManager.currentMood.getTriggers();

		if ( triggers.isEmpty() )
		{
			return Collections.EMPTY_LIST;
		}

		ArrayList missing = new ArrayList();
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();
			if ( current.getType().equals( "lose_effect" ) && !current.matches() )
			{
				missing.add( current.getEffect() );
			}
		}

		// Special case: if the character has a turtling rod equipped,
		// assume the Eau de Tortue is a possibility

		if ( KoLCharacter.hasEquipped( MoodManager.TURTLING_ROD, EquipmentManager.OFFHAND ) && !KoLConstants.activeEffects.contains( MoodManager.EAU_DE_TORTUE ) )
		{
			missing.add( MoodManager.EAU_DE_TORTUE );
		}

		return missing;
	}

	public static void removeMalignantEffects()
	{
		for ( int i = 0; i < MoodManager.AUTO_CLEAR.length && KoLmafia.permitsContinue(); ++i )
		{
			AdventureResult effect = MoodManager.AUTO_CLEAR[ i ];

			if ( KoLConstants.activeEffects.contains( effect ) )
			{
				RequestThread.postRequest( new UneffectRequest( effect ) );
			}
		}
	}

	public static int getMaintenanceCost()
	{
		List triggers = MoodManager.currentMood.getTriggers();

		if ( triggers.isEmpty() )
		{
			return 0;
		}

		int runningTally = 0;
		Iterator triggerIterator = triggers.iterator();

		// Iterate over the entire list of applicable triggers,
		// locate the ones which involve spellcasting, and add
		// the MP cost for maintenance to the running tally.

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();
			if ( !current.getType().equals( "lose_effect" ) || !current.shouldExecute( -1 ) )
			{
				continue;
			}

			String action = current.getAction();
			if ( !action.startsWith( "cast" ) && !action.startsWith( "buff" ) )
			{
				continue;
			}

			int spaceIndex = action.indexOf( " " );
			if ( spaceIndex == -1 )
			{
				continue;
			}

			action = action.substring( spaceIndex + 1 );

			int multiplier = 1;

			if ( Character.isDigit( action.charAt( 0 ) ) )
			{
				spaceIndex = action.indexOf( " " );
				multiplier = StringUtilities.parseInt( action.substring( 0, spaceIndex ) );
				action = action.substring( spaceIndex + 1 );
			}

			String skillName = SkillDatabase.getSkillName( action );
			if ( skillName != null )
			{
				runningTally +=
					SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( skillName ) ) * multiplier;
			}
		}

		// Running tally calculated, return the amount of
		// MP required to sustain this mood.

		return runningTally;
	}

	/**
	 * Stores the settings maintained in this <code>MoodManager</code> object to disk for later retrieval.
	 */

	public static void saveSettings()
	{
		PrintStream writer = LogStream.openStream( getFile(), true );
		Iterator moodIterator = MoodManager.availableMoods.iterator();

		while ( moodIterator.hasNext() )
		{
			Mood mood = (Mood) moodIterator.next();
			writer.println( mood.toSettingString() );;
		}

		writer.close();
	}

	/**
	 * Loads the settings located in the given file into this object. Note that all settings are overridden; if the
	 * given file does not exist, the current global settings will also be rewritten into the appropriate file.
	 */

	public static void loadSettings()
	{
		MoodManager.availableMoods.clear();

		Mood mood = new Mood( "apathetic" );
		MoodManager.availableMoods.add( mood );
		
		mood = new Mood( "default" );
		MoodManager.availableMoods.add( mood );
		
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			BufferedReader reader = FileUtilities.getReader( getFile() );
			
			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				line = line.trim();
				
				if ( line.length() == 0 )
				{
					continue;
				}
				
				if ( !line.startsWith( "[" ) )
				{
					mood.addTrigger( MoodTrigger.constructNode( line ) );
					continue;
				}

				int closeBracketIndex = line.indexOf( "]" );
				
				if ( closeBracketIndex == -1 )
				{
					continue;
				}

				String moodName = line.substring( 1, closeBracketIndex );
				mood = new Mood( moodName );

				MoodManager.availableMoods.remove( mood );
				MoodManager.availableMoods.add( mood );
			}

			reader.close();
			reader = null;

			MoodManager.setMood( Preferences.getString( "currentMood" ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static String getDefaultAction( final String type, final String name )
	{
		if ( type == null || name == null )
		{
			return "";
		}

		// We can look at the displayList list to see if it matches
		// your current mood.  That way, the "default action" is
		// considered whatever your current mood says it is.

		String action = "";

		List triggers = ( MoodManager.currentMood == null ) ? Collections.EMPTY_LIST : MoodManager.currentMood.getTriggers();
		Iterator triggerIterator = triggers.iterator();

		while ( triggerIterator.hasNext() )
		{
			MoodTrigger current = (MoodTrigger) triggerIterator.next();

			if ( current.getType().equals( type ) && current.getName().equals( name ) )
			{
				action = current.getAction();
			}
		}

		if ( type.equals( "unconditional" ) )
		{
			return action;
		}
		else if ( type.equals( "lose_effect" ) )
		{
			if ( action.equals( "" ) )
			{
				action = EffectDatabase.getDefaultAction( name );

				if ( action == null )
				{
					action = MoodTrigger.getKnownSources( name );
				}
			}

			return action;
		}
		else
		{
			if ( action.equals( "" ) )
			{
				action = "uneffect " + name;
			}

			return action;
		}
	}

	public static boolean currentlyExecutable( final AdventureResult effect, final String action )
	{
		// It's always OK to boost a stackable effect.
		// Otherwise, it's only OK if it's not active.

		return !MoodManager.unstackableAction( action ) || !KoLConstants.activeEffects.contains( effect );
	}

	public static boolean unstackableAction( final String action )
	{
		return
                action.contains( "absinthe" ) ||
                        action.contains( "astral mushroom" ) ||
                        action.contains( "oasis" ) ||
                        action.contains( "turtle pheromones" ) ||
                        action.contains( "gong" );
	}
}
