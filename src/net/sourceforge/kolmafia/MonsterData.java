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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;

public class MonsterData
	extends AdventureResult
{
	private Object health;
	private Object attack;
	private Object defense;
	private Object initiative;
	private Object experience;
	private final int attackElement;
	private final int defenseElement;
	private final int minMeat;
	private final int maxMeat;
	private final int phylum;
	private final int poison;
	private final boolean boss;
	private final int beeCount;

	private final ArrayList<AdventureResult> items;
	private final ArrayList<Float> pocketRates;

	public MonsterData( final String name, final Object health,
		final Object attack, final Object defense, final Object initiative,
		final Object experience, final int attackElement,
		final int defenseElement, final int minMeat, final int maxMeat,
		final int phylum, final int poison, final boolean boss )
	{
		super( AdventureResult.MONSTER_PRIORITY, name );

		this.health = health;
		this.attack = attack;
		this.defense = defense;
		this.initiative = initiative;
		this.experience = experience;
		this.attackElement = attackElement;
		this.defenseElement = defenseElement;
		this.minMeat = minMeat;
		this.maxMeat = maxMeat;
		this.phylum = phylum;
		this.poison = poison;
		this.boss = boss;

		int beeCount = 0;
		for ( int i = 0; i < name.length(); ++i )
		{
			char c = name.charAt( i );
			if ( c == 'b' || c == 'B' )
			{
				beeCount++;
			}
		}
		this.beeCount = beeCount;

        items = new ArrayList<AdventureResult>();
        pocketRates = new ArrayList<Float>();
	}

	private static int ML()
	{
		/* For brevity, and to handle the possible future need for
		   asking for speculative monster stats */
		return KoLCharacter.getMonsterLevelAdjustment();
	}

	private MonsterExpression compile( Object expr )
	{
		return new MonsterExpression( (String) expr, getName() );
	}
 
	private float getBeeosity()
	{
		return 1.0f + ( KoLCharacter.inBeecore() ? (beeCount * 0.20f ) : 0.0f );
	}

	public int getHP()
	{
		if ( health == null )
		{
			return 0;
		}
		if ( health instanceof Integer )
		{
			int hp = (Integer) health;
			return hp == 0 ? 0 : (int) Math.floor( Math.max( 1, hp + ML() ) * getBeeosity() );
		}
		if ( health instanceof String )
		{
            health = compile( health );
		}
		return Math.max( 1, (int) (((MonsterExpression) health).eval() * getBeeosity() ) );
	}

	public int getAttack()
	{
		if ( attack == null )
		{
			return 0;
		}
		if ( attack instanceof Integer )
		{
			int attack = (Integer) this.attack;
			return attack == 0 ? 0 : (int) Math.floor( Math.max( 1, attack + ML() ) * getBeeosity() );
		}
		if ( attack instanceof String )
		{
            attack = compile( attack );
		}
		return Math.max( 1, (int) (((MonsterExpression) attack).eval() * getBeeosity() ) );
	}

	public int getDefense()
	{
		if ( defense == null )
		{
			return 0;
		}
		if ( defense instanceof Integer )
		{
			int defense = (Integer) this.defense;
			return defense == 0 ? 0 :
				(int) Math.floor( Math.max( 1, (int) Math.ceil( 0.9 * ( defense + ML() ) ) ) * getBeeosity() );
		}
		if ( defense instanceof String )
		{
            defense = compile( defense );
		}
		return Math.max( 1, (int) (((MonsterExpression) defense).eval() * getBeeosity() ) );
	}

	public int getInitiative()
	{
		if ( initiative == null )
		{
			return 0;
		}
		if ( initiative instanceof Integer )
		{
			return (Integer) initiative;
		}
		if ( initiative instanceof String )
		{
            initiative = compile( initiative );
		}
		return (int) ((MonsterExpression) initiative).eval();
	}

	public int getAttackElement()
	{
		return attackElement;
	}

	public int getDefenseElement()
	{
		return defenseElement;
	}

	public int getMinMeat()
	{
		return minMeat;
	}

	public int getMaxMeat()
	{
		return maxMeat;
	}

	public int getPhylum()
	{
		return phylum;
	}

	public int getPoison()
	{
		return poison;
	}

	public boolean isBoss()
	{
		return boss;
	}

	public List getItems()
	{
		return items;
	}

	public List getPocketRates()
	{
		return pocketRates;
	}

	public boolean shouldSteal()
	{
		// If the player has an acceptable dodge rate or
		// then steal anything.

		if ( willUsuallyDodge( 0 ) )
		{
			return shouldSteal( items );
		}

		// Otherwise, only steal from monsters that drop
		// something on your conditions list.

		return shouldSteal( GoalManager.getGoals() );
	}

	private boolean shouldSteal( final List checklist )
	{
		float dropModifier = AreaCombatData.getDropRateModifier();

		for ( int i = 0; i < checklist.size(); ++i )
		{
			if ( shouldStealItem( (AdventureResult) checklist.get( i ), dropModifier ) )
			{
				return true;
			}
		}

		return false;
	}

	private boolean shouldStealItem( AdventureResult item, final float dropModifier )
	{
		if ( !item.isItem() )
		{
			return false;
		}

		int itemIndex = items.indexOf( item );

		// If the monster drops this item, then return true
		// when the drop rate is less than 100%.

		if ( itemIndex != -1 )
		{
			item = (AdventureResult) items.get( itemIndex );
			switch ( (char) item.getCount() & 0xFFFF )
			{
			case 'p':
				return true;
			case 'n':
			case 'c':
			case 'f':
			case 'b':
				return false;
			default:
				return (item.getCount() >> 16) * dropModifier < 100.0f;
			}
		}

		// If the item does not drop, check to see if maybe
		// the monster drops one of its ingredients.

		AdventureResult[] subitems = ConcoctionDatabase.getStandardIngredients( item.getItemId() );
		if ( subitems.length < 2 )
		{
			return false;
		}

		for ( int i = 0; i < subitems.length; ++i )
		{
			if ( shouldStealItem( subitems[ i ], dropModifier ) )
			{
				return true;
			}
		}

		// The monster doesn't drop the item or any of its
		// ingredients.

		return false;
	}

	public void clearItems()
	{
        items.clear();
	}

	public void addItem( final AdventureResult item )
	{
        items.add( item );
	}

	public void doneWithItems()
	{
        items.trimToSize();

		// Calculate the probability that an item will be yoinked
		// based on the integral provided by Buttons on the HCO forums.
		// http://forums.hardcoreoxygenation.com/viewtopic.php?t=3396

		float probability = 0.0f;
		float[] coefficients = new float[items.size() ];

		for ( int i = 0; i < items.size(); ++i )
		{
			coefficients[ 0 ] = 1.0f;
			for ( int j = 1; j < coefficients.length; ++j )
			{
				coefficients[ j ] = 0.0f;
			}

			for ( int j = 0; j < items.size(); ++j )
			{
				AdventureResult item = (AdventureResult) items.get( j );
				probability = (item.getCount() >> 16) / 100.0f;
				switch ( (char) item.getCount() & 0xFFFF )
				{
				case 'p':
					if ( probability == 0.0f )
					{	// assume some probability of a pickpocket-only item
						probability = 0.05f;
					}
					break;
				case 'n':
				case 'c':
				case 'f':
				case 'b':
					probability = 0.0f;
					break;
				}

				if ( i == j )
				{
					for ( int k = 0; k < coefficients.length; ++k )
					{
						coefficients[ k ] = coefficients[ k ] * probability;
					}
				}
				else
				{
					for ( int k = coefficients.length - 1; k >= 1; --k )
					{
						coefficients[ k ] = coefficients[ k ] - probability * coefficients[ k - 1 ];
					}
				}
			}

			probability = 0.0f;

			for ( int j = 0; j < coefficients.length; ++j )
			{
				probability += coefficients[ j ] / ( j + 1 );
			}

            pocketRates.add( probability );
		}
	}

	public float getExperience()
	{
		if ( experience == null )
		{
			return (getAttack() / getBeeosity() ) / 8.0f;
		}
		if ( experience instanceof Integer )
		{
			return (Integer) experience / 2.0f;
		}
		if ( experience instanceof String )
		{
            experience = compile( experience );
		}
		return ((MonsterExpression) experience).eval() / 2.0f;
	}

	public boolean willUsuallyMiss()
	{
		return willUsuallyMiss( 0 );
	}

	public boolean willUsuallyDodge( final int offenseModifier )
	{
		int dodgeRate = KoLCharacter.getAdjustedMoxie() - (getAttack() + offenseModifier ) - 6;
		return dodgeRate > 0;
	}

	public boolean willUsuallyMiss( final int defenseModifier )
	{
		int hitStat = EquipmentManager.getAdjustedHitStat();

		return AreaCombatData.hitPercent( hitStat - defenseModifier, getDefense() ) <= 50.0f;
	}
}
