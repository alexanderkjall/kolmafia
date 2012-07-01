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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoodTrigger
	implements Comparable
{
	private static Map knownSources = new HashMap();

	private int skillId = -1;
	private final AdventureResult effect;
	private boolean isThiefTrigger = false;

	private final StringBuffer stringForm;

	private String action;
	private final String type;
	private final String name;

	private int count;
	private AdventureResult item;
	private UseSkillRequest skill;

	private MoodTrigger( final String type, final AdventureResult effect, final String action )
	{
		this.type = type;
		this.effect = effect;
        name = effect == null ? null : effect.getName();

		if ( ( action.startsWith( "use " ) || action.startsWith( "cast " ) ) && !action.contains( ";" ) )
		{
			// Determine the command, the count amount,
			// and the parameter's unambiguous form.

			int spaceIndex = action.indexOf( " " );
			String parameters = StringUtilities.getDisplayName( action.substring( spaceIndex + 1 ).trim() );

			if ( action.startsWith( "use" ) )
			{
                item = ItemFinder.getFirstMatchingItem( parameters, false );

				if ( item != null )
				{
                    count = item.getCount();
					this.action = "use " + count + " " + item.bangPotionAlias();
				}
			}
			else
			{
                count = 1;

				if ( Character.isDigit( parameters.charAt( 0 ) ) )
				{
					spaceIndex = parameters.indexOf( " " );
                    count = StringUtilities.parseInt( parameters.substring( 0, spaceIndex ) );
					parameters = parameters.substring( spaceIndex ).trim();
				}

				if ( !SkillDatabase.contains( parameters ) )
				{
					parameters = SkillDatabase.getUsableSkillName( parameters );
				}

                skill = UseSkillRequest.getInstance( parameters );

				if ( skill != null )
				{
					this.action = "cast " + count + " " + skill.getSkillName();
				}
			}
		}

		if ( this.action == null )
		{
            count = 1;
			this.action = action;
		}

		if ( type != null && type.equals( "lose_effect" ) && effect != null )
		{
			Set existingActions = (Set) MoodTrigger.knownSources.get( effect.getName() );

			if ( existingActions == null )
			{
				existingActions = new LinkedHashSet();
				MoodTrigger.knownSources.put( effect.getName(), existingActions );
			}

			existingActions.add( this.action );

			String skillName = UneffectRequest.effectToSkill( effect.getName() );
			if ( SkillDatabase.contains( skillName ) )
			{
                skillId = SkillDatabase.getSkillId( skillName );
                isThiefTrigger = skillId > 6000 && skillId < 7000;
			}
		}

        stringForm = new StringBuffer();
        updateStringForm();
	}

	public static String getKnownSources( String name )
	{
		Set existingActions = (Set) MoodTrigger.knownSources.get( name );

		if ( existingActions == null )
		{
			return "";
		}

		StringBuilder buffer = new StringBuilder();

		Iterator actionIterator = existingActions.iterator();

		while ( actionIterator.hasNext() )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( "|" );
			}

			String action = (String) actionIterator.next();
			buffer.append( action );
		}

		return buffer.toString();
	}

	public static void clearKnownSources()
	{
		MoodTrigger.knownSources.clear();
	}
	
	public boolean matches()
	{
		return KoLConstants.activeEffects.contains( effect );
	}
	
	public boolean matches( AdventureResult effect )
	{
		if ( this.effect == null )
		{
			return effect == null;
		}
		
		if ( effect == null )
		{
			return false;
		}
		
		return this.effect.equals( effect );
	}

	public boolean isSkill()
	{
		return skillId != -1;
	}
	
	public AdventureResult getEffect()
	{
		return effect;
	}
	
	public String getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public String getAction()
	{
		return action;
	}

	@Override
	public String toString()
	{
		return stringForm.toString();
	}

	public String toSetting()
	{
		if ( effect == null )
		{
			return type + " => " + action;
		}

		if ( item != null )
		{
			return type + " " + StringUtilities.getCanonicalName( name ) + " => use " + count + " " + StringUtilities.getCanonicalName( item.bangPotionAlias() );
		}

		if ( skill != null )
		{
			return type + " " + StringUtilities.getCanonicalName( name ) + " => cast " + count + " " + StringUtilities.getCanonicalName( skill.getSkillName() );
		}

		return type + " " + StringUtilities.getCanonicalName( name ) + " => " + action;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( o == null || !( o instanceof MoodTrigger ) )
		{
			return false;
		}

		MoodTrigger mt = (MoodTrigger) o;
		if ( !type.equals( mt.getType() ) )
		{
			return false;
		}

		if ( name == null )
		{
			return mt.name == null;
		}

		if ( mt.getType() == null )
		{
			return false;
		}

		return name.equals( mt.name );
	}

	public void execute( final int multiplicity )
	{
		if ( !shouldExecute( multiplicity ) )
		{
			return;
		}

		if ( item != null )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( item.getInstance( Math.max(
                    count, count * multiplicity ) ) ) );

			return;
		}

		if ( skill != null )
		{
			int casts = Math.max( count, count * multiplicity );
            skill.setBuffCount( casts );
            skill.setTarget( KoLCharacter.getUserName() );
			RequestThread.postRequest( skill );

			if ( !UseSkillRequest.lastUpdate.equals( "" ) )
			{
				String name = skill.getSkillName();
				KoLmafia.updateDisplay( MafiaState.ERROR, "Mood failed to cast " + casts + " " + name + ": " + UseSkillRequest.lastUpdate );
				RequestThread.declareWorldPeace();
			}

			return;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( action );
	}

	public boolean shouldExecute( final int multiplicity )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		if ( type.equals( "unconditional" ) || effect == null )
		{
			return true;
		}

		if ( type.equals( "lose_effect" ) && multiplicity > 0 )
		{
			return true;
		}

		if ( type.equals( "gain_effect" ) )
		{
			return KoLConstants.activeEffects.contains( effect );
		}

		if ( MoodManager.unstackableAction( action ) )
		{
			return !KoLConstants.activeEffects.contains( effect );
		}

		int activeCount = effect.getCount( KoLConstants.activeEffects );

		if ( multiplicity == -1 )
		{
			return activeCount <= 1;
		}

		return activeCount <= 5;
	}

	public boolean isThiefTrigger()
	{
		return isThiefTrigger;
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof MoodTrigger ) )
		{
			return -1;
		}

		String othertype = ( (MoodTrigger) o ).getType();
		String othername = ( (MoodTrigger) o ).name;
		String otherTriggerAction = ( (MoodTrigger) o ).action;

		int compareResult = 0;

		if ( type.equals( "unconditional" ) )
		{
			if ( othertype.equals( "unconditional" ) )
			{
				compareResult = action.compareToIgnoreCase( otherTriggerAction );
			}
			else
			{
				compareResult = -1;
			}
		}
		else if ( type.equals( "gain_effect" ) )
		{
			if ( othertype.equals( "unconditional" ) )
			{
				compareResult = 1;
			}
			else if ( othertype.equals( "gain_effect" ) )
			{
				compareResult = name.compareToIgnoreCase( othername );
			}
			else
			{
				compareResult = -1;
			}
		}
		else if ( type.equals( "lose_effect" ) )
		{
			if ( othertype.equals( "lose_effect" ) )
			{
				compareResult = name.compareToIgnoreCase( othername );
			}
			else
			{
				compareResult = 1;
			}
		}

		return compareResult;
	}

	public void updateStringForm()
	{
        stringForm.setLength( 0 );

		if ( type.equals( "gain_effect" ) )
		{
            stringForm.append( "When I get" );
		}
		else if ( type.equals( "lose_effect" ) )
		{
            stringForm.append( "When I run low on" );
		}
		else
		{
            stringForm.append( "Always" );
		}

		if ( name != null )
		{
            stringForm.append( " " );
            stringForm.append( name );
		}

        stringForm.append( ", " );
        stringForm.append( action );
	}

	public static MoodTrigger constructNode( final String line )
	{
		String[] pieces = CharacterEntities.unescape( line ).split( " => " );
		if ( pieces.length != 2 )
		{
			return null;
		}

		String type = null;

		if ( pieces[ 0 ].startsWith( "gain_effect" ) )
		{
			type = "gain_effect";
		}
		else if ( pieces[ 0 ].startsWith( "lose_effect" ) )
		{
			type = "lose_effect";
		}
		else if ( pieces[ 0 ].startsWith( "unconditional" ) )
		{
			type = "unconditional";
		}

		if ( type == null )
		{
			return null;
		}

		String name =
			type.equals( "unconditional" ) ? null : pieces[ 0 ].substring( pieces[ 0 ].indexOf( " " ) ).trim();

		AdventureResult effect = null;

		if ( !type.equals( "unconditional" ) )
		{
			effect = EffectDatabase.getFirstMatchingEffect( name );

			if ( effect == null )
			{
				return null;
			}
		}

		return new MoodTrigger( type, effect, pieces[ 1 ].trim() );
	}
}
