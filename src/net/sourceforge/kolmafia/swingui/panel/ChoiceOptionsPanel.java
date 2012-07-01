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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.CardLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.VioletFogManager;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

import net.sourceforge.kolmafia.textui.command.GongCommand;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * This panel allows the user to select which item they would like to do for each of the different choice
 * adventures.
 */

public class ChoiceOptionsPanel
	extends JTabbedPane
	implements PreferenceListener
{
	private final TreeMap choiceMap;
	private final HashMap selectMap;
	private final CardLayout choiceCards;
	private final JPanel choicePanel;

	private final JComboBox[] optionSelects;

	private final JComboBox castleWheelSelect;
	private final JComboBox palindomePapayaSelect;
	private final JComboBox spookyForestSelect;
	private final JComboBox violetFogSelect;
	private final JComboBox maidenSelect;
	private final JComboBox louvreSelect;
	private final JComboBox manualLouvre;
	private final JComboBox billiardRoomSelect;
	private final JComboBox riseSelect, fallSelect;
	private final OceanDestinationComboBox oceanDestSelect;
	private final JComboBox oceanActionSelect;
	private final JComboBox barrelSelect;
	private final JComboBox darkAtticSelect;
	private final JComboBox unlivingRoomSelect;
	private final JComboBox debasementSelect;
	private final JComboBox propDeportmentSelect;
	private final JComboBox reloadedSelect;
	private final JComboBox sororityGuideSelect;
	private final JComboBox gongSelect;
	private final JComboBox basementMallSelect;
	private final JComboBox breakableSelect;
	private final JComboBox addingSelect;

	/**
	 * Constructs a new <code>ChoiceOptionsPanel</code>.
	 */

	public ChoiceOptionsPanel()
	{
		super( JTabbedPane.LEFT );
        choiceCards = new CardLayout( 10, 10 );

        choiceMap = new TreeMap();
        selectMap = new HashMap();

        choicePanel = new JPanel( choiceCards );
        choicePanel.add( new JPanel(), "" );
        addTab( "Zone", new GenericScrollPane( choicePanel ) );
        setToolTipTextAt( 0, "Choices specific to the current adventure zone" );

		String[] options;

        optionSelects = new JComboBox[ ChoiceManager.CHOICE_ADVS.length ];
		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length; ++i )
		{
            optionSelects[ i ] = new JComboBox();
            optionSelects[ i ].addItem( "show in browser" );
			options = ChoiceManager.CHOICE_ADVS[ i ].getOptions();
			for ( int j = 0; j < options.length; ++j )
			{
                optionSelects[ i ].addItem( options[ j ] );
			}
		}

        castleWheelSelect = new JComboBox();
        castleWheelSelect.addItem( "Turn to map quest position (via moxie)" );
        castleWheelSelect.addItem( "Turn to map quest position (via mysticality)" );
        castleWheelSelect.addItem( "Turn to muscle position" );
        castleWheelSelect.addItem( "Turn to mysticality position" );
        castleWheelSelect.addItem( "Turn to moxie position" );
        castleWheelSelect.addItem( "Turn clockwise" );
        castleWheelSelect.addItem( "Turn counterclockwise" );
        castleWheelSelect.addItem( "Ignore this adventure" );

        palindomePapayaSelect = new JComboBox();
        palindomePapayaSelect.addItem( "3 papayas" );
        palindomePapayaSelect.addItem( "Trade papayas for stats" );
        palindomePapayaSelect.addItem( "Fewer stats" );
        palindomePapayaSelect.addItem( "Stats until out of papayas then papayas" );
        palindomePapayaSelect.addItem( "Stats until out of papayas then fewer stats" );

        spookyForestSelect = new JComboBox();
        spookyForestSelect.addItem( "show in browser" );
        spookyForestSelect.addItem( "mosquito larva or spooky mushrooms" );
        spookyForestSelect.addItem( "Spooky-Gro fertilizer" );
        spookyForestSelect.addItem( "spooky sapling & sell bar skins" );
        spookyForestSelect.addItem( "Spooky Temple map then skip adventure" );
        spookyForestSelect.addItem( "meet vampire hunter" );
        spookyForestSelect.addItem( "meet vampire" );
        spookyForestSelect.addItem( "gain meat" );
        spookyForestSelect.addItem( "loot Seal Clubber corpse" );
        spookyForestSelect.addItem( "loot Turtle Tamer corpse" );
        spookyForestSelect.addItem( "loot Pastamancer corpse" );
        spookyForestSelect.addItem( "loot Sauceror corpse" );
        spookyForestSelect.addItem( "loot Disco Bandit corpse" );
        spookyForestSelect.addItem( "loot Accordion Thief corpse" );

        violetFogSelect = new JComboBox();
		for ( int i = 0; i < VioletFogManager.FogGoals.length; ++i )
		{
            violetFogSelect.addItem( VioletFogManager.FogGoals[i] );
		}

        louvreSelect = new JComboBox();
        louvreSelect.addItem( "Ignore this adventure" );
		for ( int i = 0; i < LouvreManager.LouvreGoals.length - 3; ++i )
		{
            louvreSelect.addItem( LouvreManager.LouvreGoals[i] );
		}
		for ( int i = LouvreManager.LouvreGoals.length - 3; i < LouvreManager.LouvreGoals.length; ++i )
		{
            louvreSelect.addItem( "Boost " + LouvreManager.LouvreGoals[i] );
		}

        louvreSelect.addItem( "Boost Prime Stat" );
        louvreSelect.addItem( "Boost Lowest Stat" );

		LockableListModel overrideList = new LockableListModel();

        manualLouvre = new AutoFilterComboBox( overrideList, true );
		overrideList.add( "Use specified goal" );

		for ( int i = 1; i <= 3; ++i )
		{
			for ( int j = 1; j <= 3; ++j )
			{
				for ( int k = 1; k <= 3; ++k )
				{
					overrideList.add( getLouvreDirection( i ) + ", " + getLouvreDirection( j ) + ", " + getLouvreDirection( k ) );
				}
			}
		}

		String overrideSetting = Preferences.getString( "louvreOverride" );
		if ( !overrideSetting.equals( "" ) && !overrideList.contains( overrideSetting ) )
		{
			overrideList.add( 1, overrideSetting );
		}

        maidenSelect = new JComboBox();
        maidenSelect.addItem( "Ignore this adventure" );
        maidenSelect.addItem( "Fight a random knight" );
        maidenSelect.addItem( "Only fight the wolf knight" );
        maidenSelect.addItem( "Only fight the snake knight" );
        maidenSelect.addItem( "Maidens, then fight a random knight" );
        maidenSelect.addItem( "Maidens, then fight the wolf knight" );
        maidenSelect.addItem( "Maidens, then fight the snake knight" );

        billiardRoomSelect = new JComboBox();
        billiardRoomSelect.addItem( "ignore this adventure" );
        billiardRoomSelect.addItem( "muscle substats" );
        billiardRoomSelect.addItem( "mysticality substats" );
        billiardRoomSelect.addItem( "moxie substats" );
        billiardRoomSelect.addItem( "Spookyraven Library Key" );

        riseSelect = new JComboBox();
        riseSelect.addItem( "ignore this adventure" );
        riseSelect.addItem( "boost mysticality substats" );
        riseSelect.addItem( "boost moxie substats" );
        riseSelect.addItem( "acquire mysticality skill" );
        riseSelect.addItem( "unlock second floor stairs" );

        fallSelect = new JComboBox();
        fallSelect.addItem( "ignore this adventure" );
        fallSelect.addItem( "boost muscle substats" );
        fallSelect.addItem( "reveal key in conservatory" );
        fallSelect.addItem( "unlock second floor stairs" );

        oceanDestSelect = new OceanDestinationComboBox();

        oceanActionSelect = new JComboBox();
        oceanActionSelect.addItem( "continue" );
        oceanActionSelect.addItem( "show" );
        oceanActionSelect.addItem( "stop" );
        oceanActionSelect.addItem( "save and continue" );
        oceanActionSelect.addItem( "save and show" );
        oceanActionSelect.addItem( "save and stop" );

        barrelSelect = new JComboBox();
        barrelSelect.addItem( "top rows (mixed drinks)" );
        barrelSelect.addItem( "middle rows (basic booze)" );
        barrelSelect.addItem( "top & middle rows" );
        barrelSelect.addItem( "bottom rows (schnapps, fine wine)" );
        barrelSelect.addItem( "top & bottom rows" );
        barrelSelect.addItem( "middle & bottom rows" );
        barrelSelect.addItem( "all available drinks" );

        darkAtticSelect = new JComboBox();
        darkAtticSelect.addItem( "show in browser" );
        darkAtticSelect.addItem( "staff guides" );
        darkAtticSelect.addItem( "ghost trap" );
        darkAtticSelect.addItem( "mass kill werewolves with silver shotgun shell" );
        darkAtticSelect.addItem( "raise area ML, then staff guides" );
        darkAtticSelect.addItem( "raise area ML, then ghost trap" );
        darkAtticSelect.addItem( "raise area ML, then mass kill werewolves" );
        darkAtticSelect.addItem( "raise area ML, then mass kill werewolves or ghost trap" );
        darkAtticSelect.addItem( "lower area ML, then staff guides" );
        darkAtticSelect.addItem( "lower area ML, then ghost trap" );
        darkAtticSelect.addItem( "lower area ML, then mass kill werewolves" );
        darkAtticSelect.addItem( "lower area ML, then mass kill werewolves or ghost trap" );

        unlivingRoomSelect = new JComboBox();
        unlivingRoomSelect.addItem( "show in browser" );
        unlivingRoomSelect.addItem( "mass kill zombies with chainsaw chain" );
        unlivingRoomSelect.addItem( "mass kill skeletons with funhouse mirror" );
        unlivingRoomSelect.addItem( "get costume item" );
        unlivingRoomSelect.addItem( "raise area ML, then mass kill zombies" );
        unlivingRoomSelect.addItem( "raise area ML, then mass kill skeletons" );
        unlivingRoomSelect.addItem( "raise area ML, then mass kill zombies/skeletons" );
        unlivingRoomSelect.addItem( "raise area ML, then get costume item" );
        unlivingRoomSelect.addItem( "lower area ML, then mass kill zombies" );
        unlivingRoomSelect.addItem( "lower area ML, then mass kill skeletons" );
        unlivingRoomSelect.addItem( "lower area ML, then get costume item" );
        unlivingRoomSelect.addItem( "lower area ML, then mass kill zombies/skeletons" );

        debasementSelect = new JComboBox();
        debasementSelect.addItem( "show in browser" );
        debasementSelect.addItem( "Prop Deportment" );
        debasementSelect.addItem( "mass kill vampires with plastic vampire fangs" );
        debasementSelect.addItem( "raise area ML, then Prop Deportment" );
        debasementSelect.addItem( "raise area ML, then mass kill vampires" );
        debasementSelect.addItem( "lower area ML, then Prop Deportment" );
        debasementSelect.addItem( "lower area ML, then mass kill vampires" );

        propDeportmentSelect = new JComboBox();
        propDeportmentSelect.addItem( "show in browser" );
        propDeportmentSelect.addItem( "chainsaw chain" );
        propDeportmentSelect.addItem( "silver item" );
        propDeportmentSelect.addItem( "funhouse mirror" );
        propDeportmentSelect.addItem( "chainsaw/mirror" );

        reloadedSelect = new JComboBox();
        reloadedSelect.addItem( "show in browser" );
        reloadedSelect.addItem( "melt Maxwell's Silver Hammer" );
        reloadedSelect.addItem( "melt silver tongue charrrm bracelet" );
        reloadedSelect.addItem( "melt silver cheese-slicer" );
        reloadedSelect.addItem( "melt silver shrimp fork" );
        reloadedSelect.addItem( "melt silver pat� knife" );
        reloadedSelect.addItem( "don't melt anything" );

        sororityGuideSelect = new JComboBox();
        sororityGuideSelect.addItem( "show in browser" );
        sororityGuideSelect.addItem( "attic" );
        sororityGuideSelect.addItem( "main floor" );
        sororityGuideSelect.addItem( "basement" );

        gongSelect = new JComboBox();
		for ( int i = 0; i < GongCommand.GONG_PATHS.length; ++i )
		{
            gongSelect.addItem( GongCommand.GONG_PATHS[i] );
		}

        basementMallSelect = new JComboBox();
        basementMallSelect.addItem( "do not show Mall prices" );
        basementMallSelect.addItem( "show Mall prices for items you don't have" );
        basementMallSelect.addItem( "show Mall prices for all items" );

        breakableSelect = new JComboBox();
        breakableSelect.addItem( "abort on breakage" );
        breakableSelect.addItem( "equip previous" );
        breakableSelect.addItem( "re-equip from inventory, or abort" );
        breakableSelect.addItem( "re-equip from inventory, or previous" );
        breakableSelect.addItem( "acquire & re-equip" );

        addingSelect = new JComboBox();
        addingSelect.addItem( "show in browser" );
        addingSelect.addItem( "create goal scrolls only" );
        addingSelect.addItem( "create goal & 668 scrolls" );
        addingSelect.addItem( "create goal, 31337, 668 scrolls" );

        addChoiceSelect( "Item-Driven", "Llama Gong", gongSelect );
        addChoiceSelect( "Item-Driven", "Breakable Equipment", breakableSelect );
        addChoiceSelect( "Plains", "Castle Wheel", castleWheelSelect );
        addChoiceSelect( "Plains", "Papaya War", palindomePapayaSelect );
        addChoiceSelect( "Plains", "Ferny's Basement", basementMallSelect );
        addChoiceSelect( "Woods", "Spooky Forest", spookyForestSelect );
        addChoiceSelect( "Item-Driven", "Violet Fog", violetFogSelect );
        addChoiceSelect( "Manor1", "Billiard Room", billiardRoomSelect );
        addChoiceSelect( "Manor1", "Rise of Spookyraven", riseSelect );
        addChoiceSelect( "Manor1", "Fall of Spookyraven", fallSelect );
        addChoiceSelect( "Manor1", "Louvre Goal", louvreSelect );
        addChoiceSelect( "Manor1", "Louvre Override", manualLouvre );
        addChoiceSelect( "Manor1", "The Maidens", maidenSelect );
        addChoiceSelect( "Island", "Ocean Destination", oceanDestSelect );
        addChoiceSelect( "Island", "Ocean Action", oceanActionSelect );
        addChoiceSelect( "Mountain", "Barrel full of Barrels", barrelSelect );
        addChoiceSelect( "Mountain", "Orc Chasm", addingSelect );
        addChoiceSelect( "Events", "Sorority House Attic", darkAtticSelect );
        addChoiceSelect( "Events", "Sorority House Unliving Room", unlivingRoomSelect );
        addChoiceSelect( "Events", "Sorority House Debasement", debasementSelect );
        addChoiceSelect( "Events", "Sorority House Prop Deportment", propDeportmentSelect );
        addChoiceSelect( "Events", "Sorority House Relocked and Reloaded", reloadedSelect );
        addChoiceSelect( "Item-Driven", "Sorority Staff Guide", sororityGuideSelect );

		for ( int i = 0; i < optionSelects.length; ++i )
		{
            addChoiceSelect(
				ChoiceManager.CHOICE_ADVS[ i ].getZone(), ChoiceManager.CHOICE_ADVS[ i ].getName(),
                    optionSelects[ i ] );
		}

        addChoiceSelect( "Item-Driven", "Item",
			new CommandButton( "use 1 llama lama gong" ) );
        addChoiceSelect( "Item-Driven", "Item",
			new CommandButton( "use 1 tiny bottle of absinthe" ) );
        addChoiceSelect( "Item-Driven", "Item",
			new CommandButton( "use 1 haunted sorority house staff guide" ) );

		PreferenceListenerRegistry.registerListener( "choiceAdventure*", this );
		PreferenceListenerRegistry.registerListener( "violetFogGoal", this );
		PreferenceListenerRegistry.registerListener( "louvreOverride", this );
		PreferenceListenerRegistry.registerListener( "louvreDesiredGoal", this );
		PreferenceListenerRegistry.registerListener( "barrelGoal", this );
		PreferenceListenerRegistry.registerListener( "gongPath", this );
		PreferenceListenerRegistry.registerListener( "oceanAction", this );
		PreferenceListenerRegistry.registerListener( "oceanDestination", this );
		PreferenceListenerRegistry.registerListener( "basementMallPrices", this );
		PreferenceListenerRegistry.registerListener( "breakableHandling", this );
		PreferenceListenerRegistry.registerListener( "addingScrolls", this );

        loadSettings();

		ArrayList optionsList;
		Object[] keys = choiceMap.keySet().toArray();

		for ( int i = 0; i < keys.length; ++i )
		{
			optionsList = (ArrayList) choiceMap.get( keys[i] );
			if ( keys[ i ].equals( "Item-Driven" ) )
			{
                addTab( "Item",
					new GenericScrollPane( new ChoicePanel( optionsList ) ) );
                setToolTipTextAt( 1, "Choices related to the use of an item" );
			}
			else
			{
                choicePanel.add( new ChoicePanel( optionsList ), keys[i] );
			}
		}
	}

	public UpdateChoicesListener getUpdateListener()
	{
		return new UpdateChoicesListener();
	}

	private String getLouvreDirection( final int i )
	{
		switch ( i )
		{
		case 1:
			return "up";
		case 2:
			return "down";
		default:
			return "side";
		}
	}

	private void addChoiceSelect( final String zone, final String name, final JComponent option )
	{
		if ( zone == null )
		{
			return;
		}

		if ( !choiceMap.containsKey( zone ) )
		{
            choiceMap.put( zone, new ArrayList() );
		}

		ArrayList options = (ArrayList) choiceMap.get( zone );

		if ( !options.contains( name ) )
		{
			options.add( name );
            selectMap.put( name, new ArrayList() );
		}

		options = (ArrayList) selectMap.get( name );
		options.add( option );
	}

	private class ChoicePanel
		extends GenericPanel
	{
		public ChoicePanel( final ArrayList options )
		{
			super( new Dimension( 150, 20 ), new Dimension( 300, 20 ) );

			Object key;
			ArrayList value;

			ArrayList elementList = new ArrayList();

			for ( int i = 0; i < options.size(); ++i )
			{
				key = options.get( i );
				value = (ArrayList) selectMap.get( key );

				if ( value.size() == 1 )
				{
					elementList.add( new VerifiableElement( key + ":  ", (JComponent) value.get( 0 ) ) );
				}
				else
				{
					for ( int j = 0; j < value.size(); ++j )
					{
						elementList.add( new VerifiableElement(
							key + " " + ( j + 1 ) + ":  ", (JComponent) value.get( j ) ) );
					}
				}
			}

			VerifiableElement[] elements = new VerifiableElement[ elementList.size() ];
			elementList.toArray( elements );

            setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
            saveSettings();
		}

		@Override
		public void actionCancelled()
		{
		}

		@Override
		public void addStatusLabel()
		{
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	private class OceanDestinationComboBox
		extends JComboBox
		implements ActionListener
	{
		public OceanDestinationComboBox()
		{
			super();
            createMenu( Preferences.getString( "oceanDestination" ) );
            addActionListener( this );
		}

		private void createMenu( String dest )
		{
            addItem( "ignore adventure" );
            addItem( "manual control" );
            addItem( "muscle" );
            addItem( "mysticality" );
            addItem( "moxie" );
            addItem( "El Vibrato power sphere" );
            addItem( "the plinth" );
            addItem( "random choice" );
			if ( dest.contains( "," ) )
			{
                addItem( "go to " + dest );
			}
            addItem( "choose destination..." );
		}

		public void loadSettings()
		{
            loadSettings( Preferences.getString( "oceanDestination" ) );
		}

		private void loadSettings( String dest )
		{
			// Default is "Manual"
			int index = 1;

			if ( dest.equals( "ignore" ) )
			{
				index = 0;
			}
			else if ( dest.equals( "manual" ) )
			{
				index = 1;
			}
			else if ( dest.equals( "muscle" ) )
			{
				index = 2;
			}
			else if ( dest.equals( "mysticality" ) )
			{
				index = 3;
			}
			else if ( dest.equals( "moxie" ) )
			{
				index = 4;
			}
			else if ( dest.equals( "sphere" ) )
			{
				index = 5;
			}
			else if ( dest.equals( "plinth" ) )
			{
				index = 6;
			}
			else if ( dest.equals( "random" ) )
			{
				index = 7;
			}
			else if ( dest.contains( "," ) )
			{
				index = 8;
			}

            setSelectedIndex( index );
		}

		public void saveSettings()
		{
			String dest = (String) getSelectedItem();
			if ( dest == null )
			{
				return;
			}

			if ( dest.startsWith( "ignore" ) )
			{
				Preferences.setString( "choiceAdventure189", "2" );
				Preferences.setString( "oceanDestination", "ignore" );
				return;
			}

			String value = "";
			if ( dest.startsWith( "muscle" ) )
			{
				value = "muscle";
			}
			else if ( dest.startsWith( "mysticality" ) )
			{
				value = "mysticality";
			}
			else if ( dest.startsWith( "moxie" ) )
			{
				value = "moxie";
			}
			else if ( dest.startsWith( "El Vibrato power sphere" ) )
			{
				value = "sphere";
			}
			else if ( dest.startsWith( "the plinth" ) )
			{
				value = "plinth";
			}
			else if ( dest.startsWith( "random" ) )
			{
				value = "random";
			}
			else if ( dest.startsWith( "go to " ) )
			{
				value = dest.substring( 6 );
			}
			else if ( dest.startsWith( "choose " ) )
			{
				return;
			}
			else	// For anything else, assume Manual Control
			{
				// For manual control, do not take a choice first
				Preferences.setString( "choiceAdventure189", "0" );
				Preferences.setString( "oceanDestination", "manual" );
				return;
			}

			Preferences.setString( "choiceAdventure189", "1" );
			Preferences.setString( "oceanDestination", value );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			String dest = (String) getSelectedItem();
			if ( dest == null )
			{
				return;
			}

			// Are we choosing a custom destination?
			if ( !dest.startsWith( "choose" ) )
			{
				return;
			}

			// Prompt for a new destination
			String coords = getCoordinates();
			if ( coords == null )
			{
				// Restore previous selection
                loadSettings();
				return;
			}

			// Rebuild combo box
            removeAllItems();
            createMenu( coords );

			// Select the "go to" menu item
            setSelectedIndex( 8 );

			// Request that the settings be saved in a different thread.
			RequestThread.runInParallel( new SaveOceanDestinationSettingsRunnable( this ) );
		}

		private String getCoordinates()
		{
			String coords = InputFieldUtilities.input( "Longitude, Latitude" );
			if ( coords == null )
			{
				return null;
			}

			int index = coords.indexOf( "," );
			if ( index == -1 )
			{
				return null;
			}

			int longitude = StringUtilities.parseInt( coords.substring( 0, index ) );
			if ( longitude < 1 || longitude > 242 )
			{
				return null;
			}

			int latitude = StringUtilities.parseInt( coords.substring( index + 1 ) );
			if ( latitude < 1 || latitude > 100 )
			{
				return null;
			}

			return String.valueOf( longitude ) + "," + String.valueOf( latitude );
		}
	}

	private static class SaveOceanDestinationSettingsRunnable
		implements Runnable
	{
		private OceanDestinationComboBox dest;

		public SaveOceanDestinationSettingsRunnable( OceanDestinationComboBox dest )
		{
			this.dest = dest;
		}

		public void run()
		{
            dest.saveSettings();
		}
	}

	private class UpdateChoicesListener
		implements ListSelectionListener
	{
		public void valueChanged( final ListSelectionEvent e )
		{
			JList source = (JList) e.getSource();
			KoLAdventure location = (KoLAdventure) source.getSelectedValue();
			if ( location == null )
			{
				return;
			}
			String zone = location.getParentZone();
			if ( zone.equals( "Item-Driven" ) )
			{
                setSelectedIndex( 1 );
                choiceCards.show(
                        choicePanel, "" );
			}
			else
			{
                setSelectedIndex( 0 );
                choiceCards.show(
                        choicePanel,
                        choiceMap.containsKey( zone ) ? zone : "" );
			}
			KoLCharacter.updateSelectedLocation( location );
		}
	}

	private boolean isAdjusting = false;

	public synchronized void update()
	{
		if ( !isAdjusting )
		{
            loadSettings();
		}
	}

	public synchronized void saveSettings()
	{
		if ( isAdjusting )
		{
			return;
		}
        isAdjusting = true;

		Object override = manualLouvre.getSelectedItem();
		int overrideIndex = manualLouvre.getSelectedIndex();
		Preferences.setString( "louvreOverride",
			overrideIndex == 0 || override == null ? "" : (String) override );

		Preferences.setInteger( "violetFogGoal", violetFogSelect.getSelectedIndex() );
		Preferences.setString( "choiceAdventure127",
			String.valueOf( palindomePapayaSelect.getSelectedIndex() + 1 ) );
		Preferences.setInteger( "barrelGoal", barrelSelect.getSelectedIndex() + 1 );
		Preferences.setString( "choiceAdventure549",
			String.valueOf( darkAtticSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure550",
			String.valueOf( unlivingRoomSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure551",
			String.valueOf( debasementSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure552",
			String.valueOf( propDeportmentSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure553",
			String.valueOf( reloadedSelect.getSelectedIndex() ) );
		Preferences.setString( "choiceAdventure554",
			String.valueOf( sororityGuideSelect.getSelectedIndex() ) );
		Preferences.setInteger( "basementMallPrices", basementMallSelect.getSelectedIndex() );
		Preferences.setInteger( "breakableHandling", breakableSelect.getSelectedIndex() + 1 );
		Preferences.setInteger( "addingScrolls", addingSelect.getSelectedIndex() );
		Preferences.setInteger( "gongPath", gongSelect.getSelectedIndex() );
		GongCommand.setPath( gongSelect.getSelectedIndex() );

		int louvreGoal = louvreSelect.getSelectedIndex();
		Preferences.setString( "choiceAdventure91",
			String.valueOf( overrideIndex > 0 || louvreGoal > 0 ? "1" : "2" ) );
		Preferences.setInteger( "louvreDesiredGoal", louvreGoal );

		for ( int i = 0; i < optionSelects.length; ++i )
		{
			int index = optionSelects[ i ].getSelectedIndex();
			String choice = ChoiceManager.CHOICE_ADVS[ i ].getSetting();
			Preferences.setString( choice, String.valueOf( index ) );
		}

		//              The Wheel:

		//              Muscle
		// Moxie          +         Mysticality
		//            Map Quest

		// Option 1: Turn the wheel clockwise
		// Option 2: Turn the wheel counterclockwise
		// Option 3: Leave the wheel alone

		switch ( castleWheelSelect.getSelectedIndex() )
		{
		case 0: // Map quest position (choice adventure 11)
			// Muscle goes through moxie
			Preferences.setString( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			Preferences.setString( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			Preferences.setString( "choiceAdventure11", "3" ); // Leave the map quest position alone
			Preferences.setString( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 1: // Map quest position (choice adventure 11)
			// Muscle goes through mysticality
			Preferences.setString( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			Preferences.setString( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			Preferences.setString( "choiceAdventure11", "3" ); // Leave the map quest position alone
			Preferences.setString( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 2: // Muscle position (choice adventure 9)
			Preferences.setString( "choiceAdventure9", "3" ); // Leave the muscle position alone
			Preferences.setString( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			Preferences.setString( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			Preferences.setString( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 3: // Mysticality position (choice adventure 10)
			Preferences.setString( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			Preferences.setString( "choiceAdventure10", "3" ); // Leave the mysticality position alone
			Preferences.setString( "choiceAdventure11", "2" ); // Turn the map quest position counterclockwise
			Preferences.setString( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 4: // Moxie position (choice adventure 12)
			Preferences.setString( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			Preferences.setString( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			Preferences.setString( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			Preferences.setString( "choiceAdventure12", "3" ); // Leave the moxie position alone
			break;

		case 5: // Turn the wheel clockwise
			Preferences.setString( "choiceAdventure9", "1" ); // Turn the muscle position clockwise
			Preferences.setString( "choiceAdventure10", "1" ); // Turn the mysticality position clockwise
			Preferences.setString( "choiceAdventure11", "1" ); // Turn the map quest position clockwise
			Preferences.setString( "choiceAdventure12", "1" ); // Turn the moxie position clockwise
			break;

		case 6: // Turn the wheel counterclockwise
			Preferences.setString( "choiceAdventure9", "2" ); // Turn the muscle position counterclockwise
			Preferences.setString( "choiceAdventure10", "2" ); // Turn the mysticality position counterclockwise
			Preferences.setString( "choiceAdventure11", "2" ); // Turn the map quest position counterclockwise
			Preferences.setString( "choiceAdventure12", "2" ); // Turn the moxie position counterclockwise
			break;

		case 7: // Ignore this adventure
			Preferences.setString( "choiceAdventure9", "3" ); // Leave the muscle position alone
			Preferences.setString( "choiceAdventure10", "3" ); // Leave the mysticality position alone
			Preferences.setString( "choiceAdventure11", "3" ); // Leave the map quest position alone
			Preferences.setString( "choiceAdventure12", "3" ); // Leave the moxie position alone
			break;
		}

		switch ( spookyForestSelect.getSelectedIndex() )
		{
		case 0:		// Manual Control
			Preferences.setString( "choiceAdventure502", "0" );
			break;
		case 1:		// Mosquito Larva or Spooky Mushrooms
			Preferences.setString( "choiceAdventure502", "2" );
			Preferences.setString( "choiceAdventure505", "1" );
			break;
		case 2:		// Spooky-Gro Fertilizer
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "2" );
			break;
		case 3:		// Spooky Sapling & Sell Bar Skins
			Preferences.setString( "choiceAdventure502", "1" );
			Preferences.setString( "choiceAdventure503", "3" );
			// If we have no Spooky Sapling
			// Preferences.setString( "choiceAdventure504", "3" );
			// If we have bear skins:
			// Preferences.setString( "choiceAdventure504", "2" );
			// Exit choice
			Preferences.setString( "choiceAdventure504", "4" );
			break;
		case 4:		// Spooky Temple Map then skip adventure
			// Without tree-holed coin
			Preferences.setString( "choiceAdventure502", "2" );
			Preferences.setString( "choiceAdventure505", "2" );
			// With tree-holed coin
			// Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "3" );
			Preferences.setString( "choiceAdventure507", "1" );
			break;
		case 5:		// Meet Vampire Hunter
			Preferences.setString( "choiceAdventure502", "1" );
			Preferences.setString( "choiceAdventure503", "2" );
			break;
		case 6:		// Meet Vampire
			Preferences.setString( "choiceAdventure502", "2" );
			Preferences.setString( "choiceAdventure505", "3" );
			break;
		case 7:		// Gain Meat
			Preferences.setString( "choiceAdventure502", "1" );
			Preferences.setString( "choiceAdventure503", "1" );
			break;
		case 8:	 // Seal clubber corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "1" );
			Preferences.setString( "choiceAdventure27", "1" );
			break;
		case 9:	// Loot Turtle Tamer corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "1" );
			Preferences.setString( "choiceAdventure27", "2" );
			break;
		case 10:	// Loot Pastamancer corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "2" );
			Preferences.setString( "choiceAdventure28", "1" );
			break;
		case 11:	// Loot Sauceror corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "2" );
			Preferences.setString( "choiceAdventure28", "2" );
			break;
		case 12:	// Loot Disco Bandit corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "3" );
			Preferences.setString( "choiceAdventure29", "1" );
			break;
		case 13:	// Loot Accordion Thief corpse
			Preferences.setString( "choiceAdventure502", "3" );
			Preferences.setString( "choiceAdventure506", "1" );
			Preferences.setString( "choiceAdventure26", "3" );
			Preferences.setString( "choiceAdventure29", "2" );
			break;
		}

		switch ( billiardRoomSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure77", "3" );
			Preferences.setString( "choiceAdventure78", "3" );
			Preferences.setString( "choiceAdventure79", "3" );
			break;

		case 1: // Muscle
			Preferences.setString( "choiceAdventure77", "2" );
			Preferences.setString( "choiceAdventure78", "2" );
			Preferences.setString( "choiceAdventure79", "3" );
			break;

		case 2: // Mysticality
			Preferences.setString( "choiceAdventure77", "2" );
			Preferences.setString( "choiceAdventure78", "1" );
			Preferences.setString( "choiceAdventure79", "2" );
			break;

		case 3: // Moxie
			Preferences.setString( "choiceAdventure77", "1" );
			Preferences.setString( "choiceAdventure78", "3" );
			Preferences.setString( "choiceAdventure79", "3" );
			break;

		case 4: // Library Key
			Preferences.setString( "choiceAdventure77", "2" );
			Preferences.setString( "choiceAdventure78", "1" );
			Preferences.setString( "choiceAdventure79", "1" );
			break;
		}

		switch ( riseSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure80", "4" );
			break;

		case 1: // Mysticality
			Preferences.setString( "choiceAdventure80", "3" );
			Preferences.setString( "choiceAdventure88", "1" );
			break;

		case 2: // Moxie
			Preferences.setString( "choiceAdventure80", "3" );
			Preferences.setString( "choiceAdventure88", "2" );
			break;

		case 3: // Mysticality Class Skill
			Preferences.setString( "choiceAdventure80", "3" );
			Preferences.setString( "choiceAdventure88", "3" );
			break;

		case 4: // Second Floor
			Preferences.setString( "choiceAdventure80", "99" );
			break;
		}

		switch ( fallSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure81", "4" );
			break;

		case 1: // Muscle
			Preferences.setString( "choiceAdventure81", "3" );
			break;

		case 2: // Gallery Key
			Preferences.setString( "choiceAdventure81", "1" );
			Preferences.setString( "choiceAdventure87", "2" );
			break;

		case 3: // Second Floor
			Preferences.setString( "choiceAdventure81", "99" );
			break;
		}

		// necessary for backwards-compatibility
		switch ( maidenSelect.getSelectedIndex() )
		{
		case 0: // Ignore this adventure
			Preferences.setString( "choiceAdventure89", "6" );
			break;

		case 1: // Fight a random knight
		case 2: // Only fight the wolf knight
		case 3: // Only fight the snake knight
		case 4: // Maidens, then fight a random knight
		case 5: // Maidens, then fight the wolf knight
		case 6: // Maidens, then fight the snake knight
			Preferences.setString( "choiceAdventure89",
				String.valueOf( maidenSelect.getSelectedIndex() - 1 ) );
			break;
		}

		// OceanDestinationComboBox handles its own settings.
        oceanDestSelect.saveSettings();

		switch ( oceanActionSelect.getSelectedIndex() )
		{
		case 0:
			Preferences.setString( "oceanAction", "continue" );
			break;
		case 1:
			Preferences.setString( "oceanAction", "show" );
			break;
		case 2:
			Preferences.setString( "oceanAction", "stop" );
			break;
		case 3:
			Preferences.setString( "oceanAction", "savecontinue" );
			break;
		case 4:
			Preferences.setString( "oceanAction", "saveshow" );
			break;
		case 5:
			Preferences.setString( "oceanAction", "savestop" );
			break;
		}

        isAdjusting = false;
	}

	public synchronized void loadSettings()
	{
        isAdjusting = true;
		ActionPanel.enableActions( false );	// prevents recursive actions from being triggered

		int index = Preferences.getInteger( "violetFogGoal" );
		if ( index >= 0 )
		{
            violetFogSelect.setSelectedIndex( index );
		}

		String setting = Preferences.getString( "louvreOverride" );
		if ( setting.equals( "" ) )
		{
            manualLouvre.setSelectedIndex( 0 );
		}
		else
		{
            manualLouvre.setSelectedItem( setting );
		}

		index = Preferences.getInteger( "louvreDesiredGoal" );
		if ( index >= 0 )
		{
            louvreSelect.setSelectedIndex( index );
		}

        palindomePapayaSelect.setSelectedIndex( Math.max( 0, Preferences.getInteger( "choiceAdventure127" ) - 1 ) );
        barrelSelect.setSelectedIndex( Math.max( 0, Preferences.getInteger( "barrelGoal" ) - 1 ) );
        darkAtticSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure549" ) );
        unlivingRoomSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure550" ) );
        debasementSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure551" ) );
        propDeportmentSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure552" ) );
        reloadedSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure553" ) );
        sororityGuideSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure554" ) );
        basementMallSelect.setSelectedIndex( Preferences.getInteger( "basementMallPrices" ) );
        breakableSelect.setSelectedIndex( Math.max( 0, Preferences.getInteger( "breakableHandling" ) - 1 ) );

		int adding = Preferences.getInteger( "addingScrolls" );
		if ( adding == -1 )
		{
			adding = Preferences.getBoolean( "createHackerSummons" ) ? 3 : 2;
			Preferences.setInteger( "addingScrolls", adding );
		}
        addingSelect.setSelectedIndex( adding );

        gongSelect.setSelectedIndex( Preferences.getInteger( "gongPath" ) );

		for ( int i = 0; i < optionSelects.length; ++i )
		{
			index = Preferences.getInteger( ChoiceManager.CHOICE_ADVS[ i ].getSetting() );
			if ( index >= 0 )
			{
				if ( index >= optionSelects[ i ].getItemCount() )
				{
					System.out.println( "Invalid setting " + index + " for "
						+ ChoiceManager.CHOICE_ADVS[ i ].getSetting() );
					index = 0;
				}
                optionSelects[ i ].setSelectedIndex( index );
			}
		}

		// Determine the desired wheel position by examining
		// which choice adventure has the "3" value.
		// If none are "3", may be clockwise or counterclockwise
		// If they are all "3", leave wheel alone

		int[] counts = { 0, 0, 0, 0 };
		int option3 = 11;

		for ( int i = 9; i < 13; ++i )
		{
			int choice = Preferences.getInteger( "choiceAdventure" + i );
			counts[ choice ]++ ;

			if ( choice == 3 )
			{
				option3 = i;
			}
		}

		index = 0;

		if ( counts[ 1 ] == 4 )
		{
			// All choices say turn clockwise
			index = 5;
		}
		else if ( counts[ 2 ] == 4 )
		{
			// All choices say turn counterclockwise
			index = 6;
		}
		else if ( counts[ 3 ] == 4 )
		{
			// All choices say leave alone
			index = 7;
		}
		else if ( counts[ 3 ] != 1 )
		{
			// Bogus. Assume map quest
			index = 0;
		}
		else if ( option3 == 9 )
		{
			// Muscle says leave alone
			index = 2;
		}
		else if ( option3 == 10 )
		{
			// Mysticality says leave alone
			index = 3;
		}
		else if ( option3 == 11 )
		{
			// Map Quest says leave alone. If we turn
			// clockwise twice, we are going through
			// mysticality. Otherwise, through moxie.
			index = counts[ 1 ] == 2 ? 1 : 0;
		}
		else if ( option3 == 12 )
		{
			// Moxie says leave alone
			index = 4;
		}

		if ( index >= 0 )
		{
            castleWheelSelect.setSelectedIndex( index );
		}

		// Figure out what to do in the spooky forest
		switch ( Preferences.getInteger( "choiceAdventure502" ) )
		{
		default:
		case 0:
			// Manual Control
			index = 0;
			break;

		case 1:
			switch ( Preferences.getInteger( "choiceAdventure503" ) )
			{
			case 1:	// Get Meat
				index = 7;
				break;
			case 2:	// Meet Vampire Hunter
				index = 5;
				break;
			case 3:	// Spooky Sapling & Sell Bar Skins
				index = 4;
				break;
			}
		case 2:
			switch ( Preferences.getInteger( "choiceAdventure505" ) )
			{
			case 1:	// Mosquito Larva or Spooky Mushrooms
				index = 1;
				break;
			case 2:	// Tree-holed coin -> Spooky Temple Map
				index = 3;
				break;
			case 3:	// Meet Vampire
				index = 6;
				break;
			}
			break;
		case 3:
			switch ( Preferences.getInteger( "choiceAdventure506" ) )
			{
			case 1:	// Forest Corpses
				index = Preferences.getInteger( "choiceAdventure26" );
				index = index * 2 + Preferences.getInteger( "choiceAdventure" + ( 26 + index ) ) - 3;
				index += 8;
				break;
			case 2:	// Spooky-Gro Fertilizer
				index = 2;
				break;
			case 3:	// Spooky Temple Map
				index = 3;
				break;
			}
		}

        spookyForestSelect.setSelectedIndex( index < 0 || index > 13 ? 0 : index );

		// Figure out what to do in the billiard room

		switch ( Preferences.getInteger( "choiceAdventure77" ) )
		{
		case 1:

			// Moxie
			index = 3;
			break;

		case 2:
			index = Preferences.getInteger( "choiceAdventure78" );

			switch ( index )
			{
			case 1:
				index = Preferences.getInteger( "choiceAdventure79" );
				index = index == 1 ? 4 : index == 2 ? 2 : 0;
				break;
			case 2:
				// Muscle
				index = 1;
				break;
			case 3:
				// Ignore this adventure
				index = 0;
				break;
			}

			break;

		case 3:

			// Ignore this adventure
			index = 0;
			break;
		}

		if ( index >= 0 )
		{
            billiardRoomSelect.setSelectedIndex( index );
		}

		// Figure out what to do at the bookcases

		index = Preferences.getInteger( "choiceAdventure80" );
		if ( index == 4 )
		{
            riseSelect.setSelectedIndex( 0 );
		}
		else if ( index == 99 )
		{
            riseSelect.setSelectedIndex( 4 );
		}
		else
		{
            riseSelect.setSelectedIndex( Preferences.getInteger( "choiceAdventure88" ) );
		}

		index = Preferences.getInteger( "choiceAdventure81" );
		if ( index == 4 )
		{
            fallSelect.setSelectedIndex( 0 );
		}
		else if ( index == 3 )
		{
            fallSelect.setSelectedIndex( 1 );
		}
		else if ( index == 99 )
		{
            riseSelect.setSelectedIndex( 3 );
		}
		else
		{
            fallSelect.setSelectedIndex( 2 );
		}

		// Figure out what to do at the maidens
		// necessary for backwards-compatibility

		index = Preferences.getInteger( "choiceAdventure89" );
		if ( index == 6 )
		{
            maidenSelect.setSelectedIndex( 0 );
		}
		else
		{
            maidenSelect.setSelectedIndex( index + 1 );
		}

		// OceanDestinationComboBox handles its own settings.
        oceanDestSelect.loadSettings();

		String action = Preferences.getString( "oceanAction" );
		if ( action.equals( "continue" ) )
		{
            oceanActionSelect.setSelectedIndex( 0 );
		}
		else if ( action.equals( "show" ) )
		{
            oceanActionSelect.setSelectedIndex( 1 );
		}
		else if ( action.equals( "stop" ) )
		{
            oceanActionSelect.setSelectedIndex( 2 );
		}
		else if ( action.equals( "savecontinue" ) )
		{
            oceanActionSelect.setSelectedIndex( 3 );
		}
		else if ( action.equals( "saveshow" ) )
		{
            oceanActionSelect.setSelectedIndex( 4 );
		}
		else if ( action.equals( "savestop" ) )
		{
            oceanActionSelect.setSelectedIndex( 5 );
		}

        isAdjusting = false;
		ActionPanel.enableActions( true );
	}

	public static class CommandButton
		extends JButton
		implements ActionListener
	{
		public CommandButton( String cmd )
		{
			super( cmd );

            setHorizontalAlignment( SwingConstants.LEFT );

            setActionCommand( cmd );
            addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			CommandDisplayFrame.executeCommand( e.getActionCommand() );
		}
	}
}
