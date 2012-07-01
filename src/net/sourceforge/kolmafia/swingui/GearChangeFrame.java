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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GearChangeFrame
	extends GenericFrame
	implements PreferenceListener
{
	private static GearChangeFrame INSTANCE = null;

	private boolean isEnabled;
	private JButton outfitButton;

	private static boolean showContainer;

	private JRadioButton[] weaponTypes;
	private JCheckBox weapon1H;
	private JRadioButton[] offhandTypes;
	private final EquipmentComboBox[] equipment;
	private final SortedListModel hats = new SortedListModel();
	private final SortedListModel pants = new SortedListModel();
	private final SortedListModel weapons = new SortedListModel();
	private final SortedListModel offhands = new SortedListModel();
	private final SortedListModel familiars = new SortedListModel();
	private final OutfitComboBox outfitSelect, customSelect;
	private final FamiliarComboBox familiarSelect;
	private JLabel sticker1Label, sticker2Label, sticker3Label;
	private int modifiersWidth;
	private JLabel modifiersLabel;
	private FamLockCheckbox famLockCheckbox;

	public GearChangeFrame()
	{
		super( "Gear Changer" );

        equipment = new EquipmentComboBox[ EquipmentManager.ALL_SLOTS ];

		LockableListModel[] lists = EquipmentManager.getEquipmentLists();
		// We maintain our own lists of valid hats, pants, weapons and offhand items
		for ( int i = 0; i < equipment.length; ++i )
		{
			LockableListModel list;
			switch ( i )
			{
			case EquipmentManager.HAT:
				list = hats;
				break;
			case EquipmentManager.PANTS:
				list = pants;
				break;
			case EquipmentManager.WEAPON:
				list = weapons;
				break;
			case EquipmentManager.OFFHAND:
				list = offhands;
				break;
			default:
				list = lists[ i ];
				break;
			}

            equipment[ i ] = new EquipmentComboBox( list, i == EquipmentManager.FAMILIAR );
		}

        familiarSelect = new FamiliarComboBox( familiars );
        outfitSelect = new OutfitComboBox( EquipmentManager.getOutfits() );
        customSelect = new OutfitComboBox( EquipmentManager.getCustomOutfits() );

		GearChangeFrame.showContainer = Preferences.getBoolean( "showContainerDropdown" );
        setCenterComponent( new JScrollPane( new EquipPanel() ) );

		GearChangeFrame.INSTANCE = this;

		PreferenceListenerRegistry.registerListener( "showContainerDropdown", this );

		RequestThread.executeMethodAfterInitialization( this, "validateSelections" );
	}

	public void update()
	{
		boolean setting = Preferences.getBoolean( "showContainerDropdown" );
		if ( GearChangeFrame.showContainer != setting )
		{
			GearChangeFrame.showContainer = setting;
            removeCenterComponent();
            setCenterComponent( new JScrollPane( new EquipPanel() ) );
            invalidate();
            validate();
            doLayout();
		}
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public static void showModifiers( Object value, boolean isFamiliarItem )
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		String name = null;
		if ( value instanceof AdventureResult )
		{
			name = ((AdventureResult) value).getName();
			if ( isFamiliarItem &&
			     ( KoLCharacter.getFamiliar().getId() == FamiliarPool.HATRACK ||
			       KoLCharacter.getFamiliar().getId() == FamiliarPool.SCARECROW ) )
			{
				name = "FamItem:" + name;
			}
		}
		else if ( value instanceof SpecialOutfit )
		{
			name = ((SpecialOutfit) value).getName();
		}

		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			GearChangeFrame.INSTANCE.modifiersLabel.setText( "" );
			return;
		}
		name = mods.getString( Modifiers.INTRINSIC_EFFECT );
		if ( name.length() > 0 )
		{
			Modifiers newMods = new Modifiers();
			newMods.add( mods );
			newMods.add( Modifiers.getModifiers( name ) );
			mods = newMods;
		}

		StringBuilder buff = new StringBuilder();
		buff.append( "<html><table><tr><td width=" );
		buff.append( GearChangeFrame.INSTANCE.modifiersWidth );
		buff.append( ">" );

		for ( int i = 0; i < Modifiers.FLOAT_MODIFIERS; ++i )
		{
			float val = mods.get( i );
			if ( val == 0.0f ) continue;
			name = Modifiers.getModifierName( i );
			name = StringUtilities.singleStringReplace( name, "Familiar", "Fam" );
			name = StringUtilities.singleStringReplace( name, "Experience", "Exp" );
			name = StringUtilities.singleStringReplace( name, "Damage", "Dmg" );
			name = StringUtilities.singleStringReplace( name, "Resistance", "Res" );
			name = StringUtilities.singleStringReplace( name, "Percent", "%" );
			buff.append( name );
			buff.append( ":<div align=right>" );
			buff.append( KoLConstants.ROUNDED_MODIFIER_FORMAT.format( val ) );
			buff.append( "</div>" );
		}

		boolean anyBool = false;
		for ( int i = 1; i < Modifiers.BITMAP_MODIFIERS; ++i )
		{
			if ( mods.getRawBitmap( i ) == 0 ) continue;
			if ( anyBool )
			{
				buff.append( ", " );
			}
			anyBool = true;
			buff.append( Modifiers.getBitmapModifierName( i ) );
		}

		for ( int i = 1; i < Modifiers.BOOLEAN_MODIFIERS; ++i )
		{
			if ( !mods.getBoolean( i ) ) continue;
			if ( anyBool )
			{
				buff.append( ", " );
			}
			anyBool = true;
			buff.append( Modifiers.getBooleanModifierName( i ) );
		}

		buff.append( "</td></tr></table></html>" );
		GearChangeFrame.INSTANCE.modifiersLabel.setText( buff.toString() );
	}

	private static int equipmentRows()
	{
		return GearChangeFrame.showContainer ? 22 : 21;
	}

	private class EquipPanel
		extends GenericPanel
	{
		public EquipPanel()
		{
			super( "change gear", "save as outfit",
			       new Dimension( 100, GearChangeFrame.equipmentRows() ),
			       new Dimension( 320, GearChangeFrame.equipmentRows() ) );

			VerifiableElement[] elements = new VerifiableElement[ GearChangeFrame.equipmentRows() ];

			int row = 0;

			elements[ row++ ] = new VerifiableElement( "Hat: ", equipment[ EquipmentManager.HAT ] );
			elements[ row++ ] = new VerifiableElement( "Weapon: ", equipment[ EquipmentManager.WEAPON ] );

			JPanel radioPanel1 = new JPanel( new GridLayout( 1, 4 ) );
			ButtonGroup radioGroup1 = new ButtonGroup();
            weaponTypes = new JRadioButton[ 3 ];

            weaponTypes[ 0 ] = new JRadioButton( "all", true );
            weaponTypes[ 1 ] = new JRadioButton( "melee" );
            weaponTypes[ 2 ] = new JRadioButton( "ranged" );

			for ( int i = 0; i < weaponTypes.length; ++i )
			{
				radioGroup1.add( weaponTypes[ i ] );
				radioPanel1.add( weaponTypes[ i ] );
                weaponTypes[ i ].addActionListener( new RefilterListener() );
			}

            weapon1H = new JCheckBox( "1-hand" );
			radioPanel1.add( weapon1H );
            weapon1H.addActionListener( new RefilterListener() );

			elements[ row++ ] = new VerifiableElement( "", radioPanel1 );

			elements[ row++ ] = new VerifiableElement( "Off-Hand: ", equipment[ EquipmentManager.OFFHAND ] );

			JPanel radioPanel2 = new JPanel( new GridLayout( 1, 5 ) );
			ButtonGroup radioGroup2 = new ButtonGroup();
            offhandTypes = new JRadioButton[ 4 ];

            offhandTypes[ 0 ] = new JRadioButton( "all", true );
            offhandTypes[ 1 ] = new JRadioButton( "weapon" );
            offhandTypes[ 2 ] = new JRadioButton( "shields" );
            offhandTypes[ 3 ] = new JRadioButton( "other" );

			for ( int i = 0; i < offhandTypes.length; ++i )
			{
				radioGroup2.add( offhandTypes[ i ] );
				radioPanel2.add( offhandTypes[ i ] );
                offhandTypes[ i ].addActionListener( new RefilterListener() );
			}
			elements[ row++ ] = new VerifiableElement( "", radioPanel2 );

			if ( GearChangeFrame.showContainer )
			{
				elements[ row++ ] = new VerifiableElement( "Container: ", equipment[ EquipmentManager.CONTAINER ] );
			}

			elements[ row++ ] = new VerifiableElement( "Shirt: ", equipment[ EquipmentManager.SHIRT ] );
			elements[ row++ ] = new VerifiableElement( "Pants: ", equipment[ EquipmentManager.PANTS ] );


			elements[ row++ ] = new VerifiableElement();

			elements[ row++ ] = new VerifiableElement( "Accessory: ", equipment[ EquipmentManager.ACCESSORY1 ] );
			elements[ row++ ] = new VerifiableElement( "Accessory: ", equipment[ EquipmentManager.ACCESSORY2 ] );
			elements[ row++ ] = new VerifiableElement( "Accessory: ", equipment[ EquipmentManager.ACCESSORY3 ] );

			elements[ row++ ] = new VerifiableElement();

			elements[ row++ ] = new VerifiableElement( "Familiar: ", familiarSelect );
			elements[ row++ ] = new VerifiableElement( "Fam Item: ", equipment[ EquipmentManager.FAMILIAR ] );

            famLockCheckbox = new FamLockCheckbox();
			JPanel boxholder = new JPanel( new BorderLayout() );
			boxholder.add( famLockCheckbox );
			elements[ row++ ] = new VerifiableElement( "", boxholder );
			GearChangeFrame.updateFamiliarLock();

			elements[ row++ ] = new VerifiableElement( "Outfit: ", outfitSelect );
			elements[ row++ ] = new VerifiableElement( "Custom: ", customSelect );

			elements[ row++ ] = new VerifiableElement();

			elements[ row ] = new VerifiableElement( "Sticker: ", equipment[ EquipmentManager.STICKER1 ]  );
            sticker1Label = elements[ row++ ].getLabel();
			elements[ row ] = new VerifiableElement( "Sticker: ", equipment[ EquipmentManager.STICKER2 ]  );
            sticker2Label = elements[ row++ ].getLabel();
			elements[ row ] = new VerifiableElement( "Sticker: ", equipment[ EquipmentManager.STICKER3 ]  );
            sticker3Label = elements[ row++ ].getLabel();

            setContent( elements );

            outfitButton = cancelledButton;
            modifiersWidth =
                    eastContainer.getPreferredSize().width;
			JLabel mods = new JLabel();
            modifiersLabel = mods;
            cancelledButton.getParent().getParent().add(
				mods, BorderLayout.CENTER );
            setEnabled( true );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			GearChangeFrame.this.isEnabled = isEnabled;

            outfitButton.setEnabled( isEnabled );
			GearChangeFrame.updateFamiliarLock();

			if ( isEnabled )
			{
                ensureValidSelections();
			}
		}

		@Override
		public void actionConfirmed()
		{
			synchronized ( GearChangeFrame.class )
			{
                changeItems();
			}
		}

		@Override
		public void actionCancelled()
		{
			synchronized ( GearChangeFrame.class )
			{
                changeItems();
			}

			String currentValue = InputFieldUtilities.input( "Name your outfit!", "Backup" );
			if ( currentValue != null )
			{
				RequestThread.postRequest( new EquipmentRequest( currentValue ) );
			}

		}
	}

	private void changeItems()
	{
		// Find out what changed

		AdventureResult[] pieces = new AdventureResult[ EquipmentManager.ALL_SLOTS ];

		for ( int i = 0; i < pieces.length; ++i )
		{
			pieces[ i ] = (AdventureResult) equipment[ i ].getSelectedItem();
			if ( EquipmentManager.getEquipment( i ).equals( pieces[ i ] ) )
			{
				pieces[ i ] = null;
			}
		}

		AdventureResult famitem = (AdventureResult) equipment[ EquipmentManager.FAMILIAR ].getSelectedItem();

		// Start with accessories

		for ( int i = EquipmentManager.ACCESSORY1; i <= EquipmentManager.ACCESSORY3; ++i )
		{
			if ( pieces[ i ] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[ i ], i, true ) );
				pieces[ i ] = null;
			}
		}

		// Move on to other equipment

		for ( int i = 0; i < EquipmentManager.ACCESSORY1; ++i )
		{
			if ( pieces[ i ] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[ i ], i, true ) );
				pieces[ i ] = null;
			}
		}

		for ( int i = EquipmentManager.STICKER1; i <= EquipmentManager.STICKER3; ++i )
		{
			if ( pieces[ i ] != null )
			{
				RequestThread.postRequest( new EquipmentRequest( pieces[ i ], i, true ) );
				pieces[ i ] = null;
			}
		}

		if ( KoLCharacter.getFamiliar().canEquip( famitem ) )
		{
			RequestThread.postRequest( new EquipmentRequest( famitem, EquipmentManager.FAMILIAR ) );
		}
	}

	public static void validateSelections()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static void updateHats()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.hats.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.HAT ) );

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static void updatePants()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.pants.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.PANTS ) );

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static void updateWeapons()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.weapons.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.WEAPON ) );
		GearChangeFrame.INSTANCE.offhands.setSelectedItem( EquipmentManager.getEquipment( EquipmentManager.OFFHAND ) );

		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static void updateStickers( int st1, int st2, int st3 )
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.sticker1Label.setText( "Sticker (" + st1 + "): " );
		GearChangeFrame.INSTANCE.sticker2Label.setText( "Sticker (" + st2 + "): " );
		GearChangeFrame.INSTANCE.sticker3Label.setText( "Sticker (" + st3 + "): " );
	}

	public static void clearWeaponLists()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.weapons.clear();
		GearChangeFrame.INSTANCE.offhands.clear();
	}

	private class EquipmentComboBox
		extends JComboBox
	{
		public EquipmentComboBox( final LockableListModel slot, boolean familiarItems )
		{
			super( slot );

			DefaultListCellRenderer renderer = familiarItems ?
				ListCellRendererFactory.getFamiliarEquipmentRenderer() :
				ListCellRendererFactory.getUsableEquipmentRenderer();

            setRenderer( renderer );
            addPopupMenuListener( new ChangeItemListener() );
		}

		private class ChangeItemListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				LockableListModel model = (LockableListModel) getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// Simply re-validate what it is you need to
				// equip.

                ensureValidSelections();
			}
		}
	}

	private class OutfitComboBox
		extends JComboBox
	{
		public OutfitComboBox( final LockableListModel slot )
		{
			super( slot );

            setRenderer( ListCellRendererFactory.getDefaultRenderer() );
            addActionListener( new ChangeOutfitListener() );
		}

		private class ChangeOutfitListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				LockableListModel model = (LockableListModel) getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// If you're changing an outfit, then the
				// change must occur right away.

				Object outfit = getSelectedItem();
				if ( outfit == null || !( outfit instanceof SpecialOutfit ) )
				{
					return;
				}

				synchronized ( GearChangeFrame.class )
				{
					RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) outfit ) );
				}

                setSelectedItem( null );
			}
		}
	}

	public static void updateFamiliars()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.familiars.setSelectedItem( KoLCharacter.getFamiliar() );
		GearChangeFrame.INSTANCE.ensureValidSelections();
	}

	public static void clearFamiliarList()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}

		GearChangeFrame.INSTANCE.familiars.clear();
	}

	private class FamiliarComboBox
		extends JComboBox
	{
		public FamiliarComboBox( final LockableListModel slot )
		{
			super( slot );
            addActionListener( new ChangeFamiliarListener() );
		}

		private class ChangeFamiliarListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				LockableListModel model = (LockableListModel) getModel();
				if ( model.isEmpty() )
				{
					return;
				}

				// If you're changing your familiar, then make
				// sure all the equipment pieces get changed
				// and the familiar gets changed right after.

				FamiliarData familiar = (FamiliarData) getSelectedItem();
				if ( familiar == null || familiar.equals( KoLCharacter.getFamiliar() ) )
				{
					return;
				}

				synchronized ( GearChangeFrame.class )
				{
                    changeItems();
					RequestThread.postRequest( new FamiliarRequest( familiar ) );
				}
			}
		}
	}

	private class RefilterListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
            ensureValidSelections();
		}
	}

	private void ensureValidSelections()
	{
		// If we are still logging in, defer this
		if ( KoLmafia.isRefreshing() )
		{
			return;
		}

		AdventureResult hatItem = (AdventureResult) equipment[ EquipmentManager.HAT ].getSelectedItem();
		AdventureResult currentHat = EquipmentManager.getEquipment( EquipmentManager.HAT );
		if ( hatItem == null )
		{
			hatItem = currentHat;
		}

		List hatItems = validHatItems( currentHat );
        updateEquipmentList( hats, hatItems, hatItem );

		AdventureResult pantsItem = (AdventureResult) equipment[ EquipmentManager.PANTS ].getSelectedItem();
		AdventureResult currentPants = EquipmentManager.getEquipment( EquipmentManager.PANTS );
		if ( pantsItem == null )
		{
			pantsItem = currentPants;
		}

		List pantsItems = validPantsItems( currentPants );
        updateEquipmentList( pants, pantsItems, pantsItem );

        equipment[ EquipmentManager.SHIRT ].setEnabled( isEnabled && KoLCharacter.hasSkill( "Torso Awaregness" ) );

		AdventureResult weaponItem = (AdventureResult) equipment[ EquipmentManager.WEAPON ].getSelectedItem();
		AdventureResult currentWeapon = EquipmentManager.getEquipment( EquipmentManager.WEAPON );
		if ( weaponItem == null )
		{
			weaponItem = currentWeapon;
		}

		List weaponItems = validWeaponItems( currentWeapon );
        updateEquipmentList( weapons, weaponItems, weaponItem );

		int weaponHands = EquipmentDatabase.getHands( weaponItem.getName() );
		if ( weaponHands > 1 )
		{
			// Equipping 2 or more handed weapon: nothing in off-hand
            equipment[ EquipmentManager.OFFHAND ].setSelectedItem( EquipmentRequest.UNEQUIP );
            equipment[ EquipmentManager.OFFHAND ].setEnabled( false );
		}
		else
		{
			AdventureResult offhandItem = (AdventureResult) equipment[ EquipmentManager.OFFHAND ].getSelectedItem();
			AdventureResult currentOffhand = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
			if ( offhandItem == null )
			{
				offhandItem = currentOffhand;
			}

			if ( EquipmentDatabase.getHands( offhandItem.getName() ) > 0 )
			{
				// Weapon in offhand. Must have compatible
				// weapon in weapon hand
				if ( weaponHands == 0 || EquipmentDatabase.getWeaponType( weaponItem.getName() ) != EquipmentDatabase.getWeaponType( offhandItem.getName() ) )
				{
					offhandItem = EquipmentRequest.UNEQUIP;
				}
			}

			List offhandItems = validOffhandItems( weaponItem, offhandItem );
            updateEquipmentList( offhands, offhandItems, offhandItem );
            equipment[ EquipmentManager.OFFHAND ].setEnabled( isEnabled );
		}

		FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
		FamiliarData selectedFamiliar = (FamiliarData) familiars.getSelectedItem();
		if ( selectedFamiliar == null )
		{
			selectedFamiliar = currentFamiliar;
		}
		List familiars = validFamiliars( currentFamiliar );
        updateEquipmentList( this.familiars, familiars, selectedFamiliar );
	}

	private List validHatItems( final AdventureResult currentHat )
	{
		List items = new ArrayList();

		// Search inventory for hats
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			addHat( items, currentItem );
		}

		// Add the current hat
		addHat( items, currentHat );

		// Add anything your Hatrack is wearing unless it is your current familiar
		FamiliarData hatrack = KoLCharacter.findFamiliar( FamiliarPool.HATRACK );
		if ( hatrack != null && hatrack != KoLCharacter.getFamiliar() )
		{
			addHat( items, hatrack.getItem() );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addHat( final List items, final AdventureResult item )
	{
		if ( !addItem( items, item, KoLConstants.EQUIP_HAT ) )
		{
			return;
		}

		items.add( item );
	}

	private List validPantsItems( final AdventureResult currentPants )
	{
		List items = new ArrayList();

		// Search inventory for pantss
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			addPants( items, currentItem );
		}

		// Add the current pants
		addPants( items, currentPants );

		// Add anything your Scarecrow is wearing unless it is your current familiar
		FamiliarData scarecrow = KoLCharacter.findFamiliar( FamiliarPool.SCARECROW );
		if ( scarecrow != null && scarecrow != KoLCharacter.getFamiliar() )
		{
			addPants( items, scarecrow.getItem() );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addPants( final List items, final AdventureResult item )
	{
		if ( !addItem( items, item, KoLConstants.EQUIP_PANTS ) )
		{
			return;
		}

		items.add( item );
	}

	private List validWeaponItems( final AdventureResult currentWeapon )
	{
		List items = new ArrayList();

		if ( KoLCharacter.inFistcore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			return items;
		}

		if ( KoLCharacter.inAxecore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			items.add( EquipmentRequest.TRUSTY );
			return items;
		}

		// Search inventory for weapons
		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			addWeapon( items, currentItem );
		}

		// Add the current weapon
		addWeapon( items, currentWeapon );

		// Add anything your Disembodied Hand is holding unless it is your current familiar
		FamiliarData hand = KoLCharacter.findFamiliar( FamiliarPool.HAND );
		if ( hand != null && hand != KoLCharacter.getFamiliar() )
		{
			addWeapon( items, hand.getItem() );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private void addWeapon( final List items, final AdventureResult item )
	{
		if ( !addItem( items, item, KoLConstants.EQUIP_WEAPON ) )
		{
			return;
		}

		if ( !filterWeapon( item ) )
		{
			return;
		}

		items.add( item );
	}

	private boolean addItem( final List items, final AdventureResult item, final int type )
	{
		// Only add it once
		if ( items.contains( item ) )
		{
			return false;
		}

		// Only add items of specified type
		if ( type != ItemDatabase.getConsumptionType( item.getItemId() ) )
		{
			return false;
		}

		// Make sure we meet requirements
		if ( !EquipmentManager.canEquip( item.getName() ) )
		{
			return false;
		}

		return true;
	}

	private boolean filterWeapon( final AdventureResult weapon )
	{
		if ( weapon1H.isSelected() && EquipmentDatabase.getHands( weapon.getName() ) > 1 )
		{
			return false;
		}

		if ( weaponTypes[ 0 ].isSelected() )
		{
			return true;
		}

		switch ( EquipmentDatabase.getWeaponType( weapon.getName() ) )
		{
		case KoLConstants.MELEE:
			return weaponTypes[ 1 ].isSelected();
		case KoLConstants.RANGED:
			return weaponTypes[ 2 ].isSelected();
		}
		return false;
	}

	private List validOffhandItems( final AdventureResult weapon, final AdventureResult offhandItem )
	{
		List items = new ArrayList();

		// In Fistcore, you must have both hands free.
		// In Axecore, you can equip only Trusty, a two-handed axe
		if ( KoLCharacter.inFistcore() || KoLCharacter.inAxecore() )
		{
			items.add( EquipmentRequest.UNEQUIP );
			return items;
		}

		// Find all offhand items that are compatible with the selected
		// weapon.

		// We can have weapons if we can dual wield and there is
		// one-handed weapon in the main hand
		boolean weapons =
			EquipmentDatabase.getHands( weapon.getName() ) == 1 && KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" );

		// The type of weapon in the off hand must
		// agree with the weapon in the main hand
		int type = EquipmentDatabase.getWeaponType( weapon.getName() );

		// Search inventory for suitable items

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			if ( !items.contains( currentItem ) && validOffhandItem( currentItem, weapons, type ) )
			{
				items.add( currentItem );
			}
		}

		// Add the selected off-hand item
		if ( !items.contains( offhandItem ) &&
		     validOffhandItem( offhandItem, weapons, type ) )
		{
			items.add( offhandItem );
		}

		// Possibly add the current off-hand item
		AdventureResult currentOffhand = EquipmentManager.getEquipment( EquipmentManager.OFFHAND );
		if ( !items.contains( currentOffhand ) &&
		     validOffhandItem( currentOffhand, weapons, type ) )
		{
			items.add( currentOffhand );
		}

		// Add "(none)"
		if ( !items.contains( EquipmentRequest.UNEQUIP ) )
		{
			items.add( EquipmentRequest.UNEQUIP );
		}

		return items;
	}

	private boolean validOffhandItem( final AdventureResult currentItem, boolean weapons, final int type )
	{
		switch ( ItemDatabase.getConsumptionType( currentItem.getItemId() ) )
		{
		case KoLConstants.EQUIP_WEAPON:
			if ( !weapons )
			{
				return false;
			}
			if ( EquipmentDatabase.getHands( currentItem.getName() ) != 1 )
			{
				return false;
			}
			if ( type != EquipmentDatabase.getWeaponType( currentItem.getName() ) )
			{
				return false;
			}
			// Fall through
		case KoLConstants.EQUIP_OFFHAND:
			// See if user wants this type of item
			if ( !filterOffhand( currentItem ) )
			{
				return false;
			}
			// Make sure we meet requirements
			if ( EquipmentManager.canEquip( currentItem.getName() ) )
			{
				return true;
			}
			break;
		}
		return false;
	}

	private boolean filterOffhand( final AdventureResult offhand )
	{
		if ( offhandTypes[ 0 ].isSelected() )
		{
			return true;
		}

		int itemId = offhand.getItemId();

		if ( ItemDatabase.getConsumptionType( itemId ) == KoLConstants.EQUIP_WEAPON )
		{
			return offhandTypes[ 1 ].isSelected();
		}

		String type = EquipmentDatabase.getItemType( itemId );
		if ( offhandTypes[ 2 ].isSelected() )
		{
			// Shields
			return type.equals( "shield" );
		}

		if ( offhandTypes[ 3 ].isSelected() )
		{
			// Everything Else
			return type.equals( "offhand" );
		}

		return false;
	}

	private List validFamiliars( final FamiliarData currentFamiliar )
	{
		List familiars = new ArrayList();

		// Look at terrarium

		Iterator it = KoLCharacter.getFamiliarList().iterator();
		while ( it.hasNext() )
		{
			FamiliarData fam = (FamiliarData) it.next();

			// Only add it once
			if ( familiars.contains( fam ) )
			{
				continue;
			}

			if ( filterFamiliar( fam ) )
			{
				familiars.add( fam );
			}
		}

		// Add the current familiar

		if ( !familiars.contains( currentFamiliar ) &&
		     filterFamiliar( currentFamiliar ) )
		{
			familiars.add( currentFamiliar );
		}

		// Add "(none)"
		if ( !familiars.contains( FamiliarData.NO_FAMILIAR ) )
		{
			familiars.add( FamiliarData.NO_FAMILIAR );
		}

		return familiars;
	}

	private boolean filterFamiliar( final FamiliarData familiar )
	{
		return familiar.canEquip();
	}

	private void updateEquipmentList( final LockableListModel currentItems, final List newItems,
		final Object equippedItem )
	{
		currentItems.retainAll( newItems );
		newItems.removeAll( currentItems );
		currentItems.addAll( newItems );

		currentItems.setSelectedItem( equippedItem );
	}

	private class FamLockCheckbox
	extends JCheckBox
	implements ActionListener
	{
		public FamLockCheckbox()
		{
			super( "familiar item locked" );
            addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			RequestThread.postRequest( new FamiliarRequest( true ) );
		}
	}

	public static void updateFamiliarLock()
	{
		if ( GearChangeFrame.INSTANCE == null )
		{
			return;
		}
		FamLockCheckbox box = GearChangeFrame.INSTANCE.famLockCheckbox;
		if ( box == null )
		{
			return;
		}
		box.setSelected( EquipmentManager.familiarItemLocked() );
		box.setEnabled( EquipmentManager.familiarItemLockable() );
	}
}
