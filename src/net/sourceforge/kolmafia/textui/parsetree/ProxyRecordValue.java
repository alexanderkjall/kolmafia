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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ProxyRecordValue
	extends RecordValue
{
	public ProxyRecordValue( final RecordType type, final Value obj )
	{
		super( type );

        contentLong = obj.contentLong;
        contentString = obj.contentString;
        content = obj.content;
	}

	@Override
	public Value aref( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) type).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		return aref( index, interpreter );
	}

	@Override
	public Value aref( final int index, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}

		Object rv;
		try
		{
			rv = getClass().getMethod(
				"get_" + type.getFieldNames()[ index ], null ).invoke( this, null );
		}
		catch ( InvocationTargetException e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e.getCause() );
		}
		catch ( Exception e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e );
		}

		if ( rv == null )
		{
			return type.getFieldTypes()[ index ].initialValue();
		}

		if ( rv instanceof Value )
		{
			return (Value) rv;
		}

		if ( rv instanceof Integer )
		{
			return DataTypes.makeIntValue( (Integer) rv );
		}

		if ( rv instanceof Float )
		{
			return DataTypes.makeFloatValue( (Float) rv );
		}

		if ( rv instanceof String )
		{
			return new Value( rv.toString() );
		}

		if ( rv instanceof Boolean )
		{
			return DataTypes.makeBooleanValue( (Boolean) rv );
		}

		if ( rv instanceof CoinmasterData )
		{
			return DataTypes.makeCoinmasterValue( (CoinmasterData) rv );
		}

		throw interpreter.runtimeException( "Unable to convert attribute value of type: " + rv.getClass() );
	}

	@Override
	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public void aset( final int index, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public Value remove( final Value key, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public void clear()
	{
	}

	/* Helper for building parallel arrays of field names & types */
	private static class RecordBuilder
	{
		private ArrayList<String> names;
		private ArrayList<Type> types;

		public RecordBuilder()
		{
			names = new ArrayList<String>();
			types = new ArrayList<Type>();
		}

		public RecordBuilder add( String name, Type type )
		{
            names.add( name.toLowerCase() );
            types.add( type );
			return this;
		}

		public RecordType finish( String name )
		{
			int len = names.size();
			return new RecordType( name,
                    names.toArray( new String[len] ),
                    types.toArray( new Type[len] ) );
		}
	}

	public static class ClassProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "primestat", DataTypes.STAT_TYPE )
			.finish( "class proxy" );

		public ClassProxy( Value obj )
		{
			super( _type, obj );
		}

		public Value get_primestat()
		{
			int primeIndex = KoLCharacter.getPrimeIndex( contentString );

			String name = AdventureResult.STAT_NAMES[ primeIndex ];

			return DataTypes.parseStatValue( name, true );
		}
	}

	public static class ItemProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "plural", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "levelreq", DataTypes.INT_TYPE )
			.add( "quality", DataTypes.STRING_TYPE )
			.add( "adventures", DataTypes.STRING_TYPE )
			.add( "muscle", DataTypes.STRING_TYPE )
			.add( "mysticality", DataTypes.STRING_TYPE )
			.add( "moxie", DataTypes.STRING_TYPE )
			.add( "fullness", DataTypes.INT_TYPE )
			.add( "inebriety", DataTypes.INT_TYPE )
			.add( "spleen", DataTypes.INT_TYPE )
			.add( "notes", DataTypes.STRING_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "reusable", DataTypes.BOOLEAN_TYPE )
			.add( "usable", DataTypes.BOOLEAN_TYPE )
			.add( "multi", DataTypes.BOOLEAN_TYPE )
			.add( "fancy", DataTypes.BOOLEAN_TYPE )
			.add( "candy", DataTypes.BOOLEAN_TYPE )
			.add( "bounty", DataTypes.LOCATION_TYPE )
			.add( "bounty_count", DataTypes.INT_TYPE )
			.add( "seller", DataTypes.COINMASTER_TYPE )
			.add( "buyer", DataTypes.COINMASTER_TYPE )
			.add( "name_length", DataTypes.INT_TYPE )
			.finish( "item proxy" );

		public ItemProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_plural()
		{
			return ItemDatabase.getPluralName( contentString );
		}

		public String get_descid()
		{
			return ItemDatabase.getDescriptionId( contentString );
		}

		public String get_image()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.getImage( id );
		}

		public Integer get_levelreq()
		{
			return ItemDatabase.getLevelReqByName( contentString );
		}

		public String get_quality()
		{
			return ItemDatabase.getQuality( contentString );
		}

		public String get_adventures()
		{
			return ItemDatabase.getAdvRangeByName( contentString );
		}

		public String get_muscle()
		{
			return ItemDatabase.getMuscleByName( contentString );
		}

		public String get_mysticality()
		{
			return ItemDatabase.getMysticalityByName( contentString );
		}

		public String get_moxie()
		{
			return ItemDatabase.getMoxieByName( contentString );
		}

		public int get_fullness()
		{
			return ItemDatabase.getFullness( contentString );
		}

		public int get_inebriety()
		{
			return ItemDatabase.getInebriety( contentString );
		}

		public int get_spleen()
		{
			return ItemDatabase.getSpleenHit( contentString );
		}

		public String get_notes()
		{
			return ItemDatabase.getNotes( contentString );
		}

		public boolean get_combat()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.getAttribute( id, ItemDatabase.ATTR_COMBAT | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_reusable()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.getConsumptionType( id ) == KoLConstants.INFINITE_USES ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_REUSABLE | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_usable()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.getConsumptionType( id ) == KoLConstants.CONSUME_USE ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_USABLE | ItemDatabase.ATTR_MULTIPLE | ItemDatabase.ATTR_REUSABLE );
		}

		public boolean get_multi()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.getConsumptionType( id ) == KoLConstants.CONSUME_MULTIPLE ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_MULTIPLE );
		}

		public boolean get_fancy()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.isFancyItem( id );
		}

		public boolean get_candy()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.isCandyItem( id );
		}

		public Value get_bounty()
		{
			int id = ItemDatabase.getItemId( contentString );
			KoLAdventure adventure = AdventureDatabase.getBountyLocation( id );
			return adventure == null ?
			       DataTypes.LOCATION_INIT :
			       DataTypes.parseLocationValue( adventure.getAdventureName(), true );
		}

		public int get_bounty_count()
		{
			int id = ItemDatabase.getItemId( contentString );
			AdventureResult bounty = AdventureDatabase.getBounty( id );
			return bounty == null ? 0 : bounty.getCount();
		}

		public CoinmasterData get_seller()
		{
			int id = ItemDatabase.getItemId( contentString );
			String itemName = ItemDatabase.getItemName( id );
			return CoinmasterRegistry.findSeller( itemName );
		}

		public CoinmasterData get_buyer()
		{
			int id = ItemDatabase.getItemId( contentString );
			String itemName = ItemDatabase.getItemName( id );
			return CoinmasterRegistry.findBuyer( itemName );
		}

		public int get_name_length()
		{
			int id = ItemDatabase.getItemId( contentString );
			return ItemDatabase.getNameLength( id );
		}
	}

	public static class FamiliarProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "hatchling", DataTypes.ITEM_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "charges", DataTypes.INT_TYPE )
			.finish( "familiar proxy" );

		public FamiliarProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_combat()
		{
			return FamiliarDatabase.isCombatType( (int) contentLong );
		}

		public Value get_hatchling()
		{
			return DataTypes.makeItemValue(
				FamiliarDatabase.getFamiliarLarva( (int) contentLong ) );
		}

		public String get_image()
		{
			return FamiliarDatabase.getFamiliarImageLocation( (int) contentLong );
		}

		public String get_name()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( contentString );
			return fam == null ? "" : fam.getName();
		}
		public int get_charges()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( contentString );
			return fam == null ? 0 : fam.getCharges();
		}
	}

	public static class SkillProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "level", DataTypes.INT_TYPE )
			.add( "traincost", DataTypes.INT_TYPE )
			.add( "class", DataTypes.CLASS_TYPE )
			.add( "libram", DataTypes.BOOLEAN_TYPE )
			.add( "passive", DataTypes.BOOLEAN_TYPE )
			.add( "buff", DataTypes.BOOLEAN_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "song", DataTypes.BOOLEAN_TYPE )
			.add( "permable", DataTypes.BOOLEAN_TYPE )
			.add( "dailylimit", DataTypes.INT_TYPE )
			.add( "timescast", DataTypes.INT_TYPE )
			.finish( "skill proxy" );

		public SkillProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_level()
		{
			return SkillDatabase.getSkillLevel( (int) contentLong );
		}

		public int get_traincost()
		{
			return SkillDatabase.getSkillPurchaseCost( (int) contentLong );
		}

		public Value get_class()
		{
			return DataTypes.parseClassValue(
				SkillDatabase.getSkillCategory( (int) contentLong ), true );
		}

		public boolean get_libram()
		{
			return SkillDatabase.isLibramSkill( (int) contentLong );
		}

		public boolean get_passive()
		{
			return SkillDatabase.isPassive( (int) contentLong );
		}

		public boolean get_buff()
		{
			return SkillDatabase.isBuff( (int) contentLong );
		}

		public boolean get_combat()
		{
			return SkillDatabase.isCombat( (int) contentLong );
		}

		public boolean get_song()
		{
			return SkillDatabase.isSong( (int) contentLong );
		}

		public boolean get_permable()
		{
			return SkillDatabase.isPermable( (int) contentLong );
		}
		public int get_dailylimit()
		{
			return SkillDatabase.getMaxCasts( (int) contentLong );
		}
		public int get_timescast()
		{
			return SkillDatabase.getCasts( (int) contentLong );
		}
	}

	public static class EffectProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "default", DataTypes.STRING_TYPE )
			.add( "note", DataTypes.STRING_TYPE )
			.add( "all",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.finish( "effect proxy" );

		public EffectProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_default()
		{
			return EffectDatabase.getDefaultAction( contentString );
		}

		public String get_note()
		{
			return EffectDatabase.getActionNote( contentString );
		}

		public Value get_all()
		{
			ArrayList<Value> rv = new ArrayList<Value>();
			Iterator i = EffectDatabase.getAllActions( contentString );
			while ( i.hasNext() )
			{
				rv.add( new Value( (String) i.next() ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public String get_image()
		{
			return EffectDatabase.getImage( (int) contentLong );
		}

		public String get_descid()
		{
			return EffectDatabase.getDescriptionId( (int) contentLong );
		}
	}

	public static class LocationProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "nocombats", DataTypes.BOOLEAN_TYPE )
			.add( "zone", DataTypes.STRING_TYPE )
			.add( "parent", DataTypes.STRING_TYPE )
			.add( "parentdesc", DataTypes.STRING_TYPE )
			.add( "bounty", DataTypes.ITEM_TYPE )
			.finish( "location proxy" );

		public LocationProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_nocombats()
		{
			return ((KoLAdventure) content).isNonCombatsOnly();
		}

		public String get_zone()
		{
			return ((KoLAdventure) content).getZone();
		}

		public String get_parent()
		{
			return ((KoLAdventure) content).getParentZone();
		}

		public String get_parentdesc()
		{
			return ((KoLAdventure) content).getParentZoneDescription();
		}

		public Value get_bounty()
		{
			AdventureResult bounty = AdventureDatabase.getBounty( (KoLAdventure) content );
			return bounty == null ?
			       DataTypes.ITEM_INIT :
			       DataTypes.parseItemValue( bounty.getName(), true );
		}
	}

	public static class MonsterProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "base_hp", DataTypes.INT_TYPE )
			.add( "base_attack", DataTypes.INT_TYPE )
			.add( "base_defense", DataTypes.INT_TYPE )
			.add( "base_initiative", DataTypes.INT_TYPE )
			.add( "attack_element", DataTypes.ELEMENT_TYPE )
			.add( "defense_element", DataTypes.ELEMENT_TYPE )
			.add( "min_meat", DataTypes.INT_TYPE )
			.add( "max_meat", DataTypes.INT_TYPE )
			.add( "base_mainstat_exp", DataTypes.FLOAT_TYPE )
			.add( "phylum", DataTypes.PHYLUM_TYPE )
			.add( "poison", DataTypes.EFFECT_TYPE )
			.add( "boss", DataTypes.BOOLEAN_TYPE )
			.finish( "monster proxy" );

		public MonsterProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_base_hp()
		{
			return ((MonsterData) content).getHP();
		}

		public int get_base_attack()
		{
			return ((MonsterData) content).getAttack();
		}

		public int get_base_defense()
		{
			return ((MonsterData) content).getDefense();
		}

		public int get_base_initiative()
		{
			return ((MonsterData) content).getInitiative();
		}

		public Value get_attack_element()
		{
			return DataTypes.parseElementValue(
				MonsterDatabase.elementNames[ ((MonsterData) content).getAttackElement() ],
				true );
		}

		public Value get_defense_element()
		{
			return DataTypes.parseElementValue(
				MonsterDatabase.elementNames[ ((MonsterData) content).getDefenseElement() ],
				true );
		}

		public int get_min_meat()
		{
			return ((MonsterData) content).getMinMeat();
		}

		public int get_max_meat()
		{
			return ((MonsterData) content).getMaxMeat();
		}

		public float get_base_mainstat_exp()
		{
			return ((MonsterData) content).getExperience();
		}

		public Value get_phylum()
		{
			return DataTypes.parsePhylumValue(
				MonsterDatabase.phylumNames[ ((MonsterData) content).getPhylum() ],
				true );
		}

		public Value get_poison()
		{
			int poisonLevel = ((MonsterData) content).getPoison();
			String poisonName = poisonLevel == Integer.MAX_VALUE ? 
				"none" :
				EffectDatabase.getEffectName( EffectDatabase.POISON_ID[ poisonLevel ] );
			return DataTypes.parseEffectValue( poisonName, true );
		}

		public boolean get_boss()
		{
			return ((MonsterData) content).isBoss();
		}
	}

	public static class CoinmasterProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "token", DataTypes.STRING_TYPE )
			.add( "item", DataTypes.ITEM_TYPE )
			.add( "property", DataTypes.STRING_TYPE )
			.add( "available_tokens", DataTypes.INT_TYPE )
			.add( "buys", DataTypes.BOOLEAN_TYPE )
			.add( "sells", DataTypes.BOOLEAN_TYPE )
			.finish( "coinmaster proxy" );

		public CoinmasterProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_token()
		{
			return ((CoinmasterData) content).getToken();
		}

		public Value get_item()
		{
			CoinmasterData data = ((CoinmasterData) content);
			AdventureResult item = data.getItem();
			return item == null ?
			       DataTypes.ITEM_INIT :
			       DataTypes.parseItemValue( item.getName(), true );
		}

		public String get_property()
		{
			return ((CoinmasterData) content).getProperty();
		}

		public int get_available_tokens()
		{
			return ((CoinmasterData) content).availableTokens();
		}

		public boolean get_buys()
		{
			return ((CoinmasterData) content).getSellAction() != null;
		}

		public boolean get_sells()
		{
			return ((CoinmasterData) content).getBuyAction() != null;
		}
	}
}
