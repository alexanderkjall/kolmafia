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

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.button.RequestButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionTable;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class ItemTableManagePanel
	extends ScrollablePanel
{
	public static final int USE_MULTIPLE = 0;

	public static final int TAKE_ALL = 1;
	public static final int TAKE_ALL_BUT_USABLE = 2;
	public static final int TAKE_MULTIPLE = 3;
	public static final int TAKE_ONE = 4;

	public JPanel northPanel;
	public LockableListModel elementModel;
	public ShowDescriptionTable elementList;

	public JButton[] buttons;
	public JCheckBox[] filters;
	public JRadioButton[] movers;

	protected final AutoFilterTextField filterfield;
	private JPanel buttonPanel;

	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel )
	{
		this(
			confirmedText,
			cancelledText,
			elementModel,
			true,
			elementModel == KoLConstants.tally ||
			elementModel == KoLConstants.inventory ||
			elementModel == KoLConstants.closet ||
			elementModel == ConcoctionDatabase.getCreatables() ||
			elementModel == ConcoctionDatabase.getUsables(),
			new boolean[] { false, false });
	}
	
	public ItemTableManagePanel( final String confirmedText, final String cancelledText, final LockableListModel elementModel, final boolean[] flags )
	{
		this(
			confirmedText,
			cancelledText,
			elementModel,
			true,
			elementModel == KoLConstants.tally ||
			elementModel == KoLConstants.inventory ||
			elementModel == KoLConstants.closet ||
			elementModel == ConcoctionDatabase.getCreatables() ||
			elementModel == ConcoctionDatabase.getUsables(),
			flags);
	}

	public ItemTableManagePanel( final String confirmedText, final String cancelledText,
		final LockableListModel elementModel, final boolean addFilterField, final boolean addRefreshButton, final boolean[] flags )
	{
		super( "", confirmedText, cancelledText, new ShowDescriptionTable( elementModel, flags ), false );

        elementList = (ShowDescriptionTable) scrollComponent;
		this.elementModel = elementList.getDisplayModel();

        elementList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        elementList.setVisibleRowCount( 8 );

        filterfield = getWordFilter();

		if ( addFilterField )
		{
            centerPanel.add( filterfield, BorderLayout.NORTH );
		}

		if ( addRefreshButton )
		{
            eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );
		}

        northPanel = new JPanel( new BorderLayout() );
        actualPanel.add( northPanel, BorderLayout.NORTH );
	}

	protected AutoFilterTextField getWordFilter()
	{
		return new FilterItemField();
	}

	protected void listenToCheckBox( final JCheckBox box )
	{
		box.addActionListener( filterfield );
	}

	protected void listenToRadioButton( final JRadioButton button )
	{
		button.addActionListener( filterfield );
	}

	public ItemTableManagePanel( final LockableListModel elementModel )
	{
		this(
			elementModel,
			true,
			elementModel == KoLConstants.tally ||
			elementModel == KoLConstants.inventory ||
			elementModel == KoLConstants.closet ||
			elementModel == ConcoctionDatabase.getCreatables() ||
			elementModel == ConcoctionDatabase.getUsables(),
			new boolean[] {false, false});
	}
	
	public ItemTableManagePanel( final LockableListModel elementModel, final boolean[] flags )
	{
		this(
			elementModel,
			true,
			elementModel == KoLConstants.tally ||
			elementModel == KoLConstants.inventory ||
			elementModel == KoLConstants.closet ||
			elementModel == ConcoctionDatabase.getCreatables() ||
			elementModel == ConcoctionDatabase.getUsables(),
			flags);
	}

	public ItemTableManagePanel( final LockableListModel elementModel, final boolean addFilterField,
		final boolean addRefreshButton, final boolean[] flags )
	{
		super( "", null, null, new ShowDescriptionTable( elementModel, flags ), false );

        elementList = (ShowDescriptionTable) scrollComponent;
		this.elementModel = elementList.getDisplayModel();

        northPanel = new JPanel( new BorderLayout() );
        actualPanel.add( northPanel, BorderLayout.NORTH );

        filterfield = getWordFilter();

		if ( addFilterField )
		{
            centerPanel.add( filterfield, BorderLayout.NORTH );
		}

		if ( addRefreshButton )
		{
            eastPanel.add( new RefreshButton(), BorderLayout.SOUTH );
		}
	}

	@Override
	public void actionConfirmed()
	{
	}

	@Override
	public void actionCancelled()
	{
	}

	public void setFixedFilter( final boolean food, final boolean booze, final boolean equip, final boolean other,
		final boolean notrade )
	{
		if ( filterfield instanceof FilterItemField )
		{
			FilterItemField itemfilter = (FilterItemField) filterfield;

			itemfilter.food = food;
			itemfilter.booze = booze;
			itemfilter.equip = equip;
			itemfilter.other = other;
			itemfilter.notrade = notrade;
		}

        filterItems();
	}

	public void addFilters()
	{
		JPanel filterPanel = new JPanel();
        filters = new JCheckBox[ 5 ];

        filters[ 0 ] = new JCheckBox( "food", KoLCharacter.canEat() );
        filters[ 1 ] = new JCheckBox( "booze", KoLCharacter.canDrink() );
        filters[ 2 ] = new JCheckBox( "equip", true );
        filters[ 3 ] = new JCheckBox( "others", true );
        filters[ 4 ] = new JCheckBox( "no-trade", true );

		for ( int i = 0; i < 5; ++i )
		{
			filterPanel.add( filters[ i ] );
            listenToCheckBox( filters[ i ] );
		}

        northPanel.add( filterPanel, BorderLayout.NORTH );
        filterItems();
	}

	public void filterItems()
	{
        filterfield.update();
	}

	public void setButtons( final ActionListener[] buttonListeners )
	{
        setButtons( true, buttonListeners );
	}

	public void setButtons( boolean addFilters, final ActionListener[] buttonListeners )
	{
		// Handle buttons along the right hand side, if there are
		// supposed to be buttons.

		if ( buttonListeners != null )
		{
            buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
            buttons = new JButton[ buttonListeners.length ];

			for ( int i = 0; i < buttonListeners.length; ++i )
			{
				if ( buttonListeners[ i ] instanceof JButton )
				{
                    buttons[ i ] = (JButton) buttonListeners[ i ];
				}
				else
				{
                    buttons[ i ] = new JButton( buttonListeners[ i ].toString() );
                    buttons[ i ].addActionListener( buttonListeners[ i ] );
				}

                buttonPanel.add( buttons[i] );
			}

            eastPanel.add( buttonPanel, BorderLayout.NORTH );
		}

		// Handle filters and movers along the top

		if ( addFilters )
		{
            addFilters();
            addMovers();
		}
		else
		{
            filters = null;
		}

		if ( buttonListeners != null )
		{
            actualPanel.add( eastPanel, BorderLayout.EAST );
		}
	}

	public void addButtons( final JButton[] buttons )
	{
        addButtons( buttons, true );
	}

	public void addButtons( final JButton[] buttons, final boolean save )
	{
		for ( int i = 0; i < buttons.length; ++i )
		{
            buttonPanel.add( buttons[i] );
		}

		if ( !save )
		{
			return;
		}

		JButton[] oldButtons = this.buttons;
		int oldSize = oldButtons.length;
		int newSize = oldSize + buttons.length;
		JButton[] newButtons = new JButton[ newSize ];

		// Copy in the old buttons
		for ( int i = 0; i < oldSize; ++i )
		{
			newButtons[ i ] = oldButtons[ i ];
		}

		// Copy in the new buttons
		for ( int i = oldSize; i < newSize; ++i )
		{
			JButton newButton = buttons[ i - oldSize ];
			newButtons[ i ] = newButton;
		}

		// Save the button list
		this.buttons = newButtons;
	}

	public void addMovers()
	{
		JPanel moverPanel = new JPanel();

        movers = new JRadioButton[ 4 ];
        movers[ 0 ] = new JRadioButton( "max possible" );
        movers[ 1 ] = new JRadioButton( "all but usable" );
        movers[ 2 ] = new JRadioButton( "multiple", true );
        movers[ 3 ] = new JRadioButton( "exactly one" );

		ButtonGroup moverGroup = new ButtonGroup();
		for ( int i = 0; i < 4; ++i )
		{
			moverGroup.add( movers[ i ] );
			moverPanel.add( movers[ i ] );
		}

        northPanel.add( moverPanel, BorderLayout.SOUTH );
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		if ( elementList == null || buttons == null )
		{
			super.setEnabled( isEnabled );
			return;
		}

		if ( buttons.length > 0 && buttons[buttons.length - 1 ] == null )
		{
			super.setEnabled( isEnabled );
			return;
		}

        elementList.setEnabled( isEnabled );
		for ( int i = 0; i < buttons.length; ++i )
		{
            buttons[ i ].setEnabled( isEnabled );
		}
	}

	public Object[] getDesiredItems( final String message )
	{
		if ( movers == null || movers[ 2 ].isSelected() )
		{
			return getDesiredItems(
				message,
				message.equals( "Queue" ) || message.equals( "Consume" ) || message.equals( "Feed" ) ? ItemManagePanel.USE_MULTIPLE : ItemManagePanel.TAKE_MULTIPLE );
		}

		return getDesiredItems(
			message,
                movers[ 0 ].isSelected() ? ItemManagePanel.TAKE_ALL : movers[ 1 ].isSelected() ? ItemManagePanel.TAKE_ALL_BUT_USABLE : ItemManagePanel.TAKE_ONE );
	}

	public Object[] getDesiredItems( final String message, final int quantityType )
	{
		Object[] items = elementList.getSelectedValues();
		if ( items.length == 0 )
		{
			return null;
		}

		int neededSize = items.length;
		boolean isTally = elementList.getOriginalModel() == KoLConstants.tally;

		String itemName;
		int itemCount, quantity;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i] == null )
			{
				--neededSize;
				continue;
			}

			if ( items[ i ] instanceof AdventureResult )
			{
				itemName = ( (AdventureResult) items[ i ] ).getName();
				itemCount =
					isTally ? ( (AdventureResult) items[ i ] ).getCount( KoLConstants.inventory ) : ( (AdventureResult) items[ i ] ).getCount();
			}
			else
			{
				itemName = ( (Concoction) items[ i ] ).getName();
				itemCount = ( (Concoction) items[ i ] ).getAvailable();
			}

			quantity =
				Math.min(
                        getDesiredItemAmount( items[ i ], itemName, itemCount, message, quantityType ), itemCount );
			if ( quantity == Integer.MIN_VALUE )
			{
				return null;
			}

			// Otherwise, if it was not a manual entry, then reset
			// the entry to null so that it can be re-processed.

			if ( quantity <= 0 )
			{
				items[ i ] = null;
				--neededSize;
			}
			else if ( items[ i ] instanceof AdventureResult )
			{
				items[ i ] = ( (AdventureResult) items[ i ] ).getInstance( quantity );
			}
			else
			{
				ConcoctionDatabase.push( (Concoction) items[ i ], quantity );
				items[ i ] = null;
			}
		}

		// Otherwise, shrink the array which will be
		// returned so that it removes any nulled values.

		if ( neededSize == 0 )
		{
			return null;
		}

		Object[] desiredItems = new Object[ neededSize ];
		neededSize = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[ i ] != null )
			{
				desiredItems[ neededSize++ ] = items[ i ];
			}
		}

		return desiredItems;
	}

	protected int getDesiredItemAmount( final Object item, final String itemName, final int itemCount,
		final String message, final int quantityType )
	{
		int quantity = 0;
		switch ( quantityType )
		{
		case TAKE_ALL:
			quantity = itemCount;
			break;

		case TAKE_ALL_BUT_USABLE:
			quantity = itemCount - getUsableItemAmount( item, itemName );
			break;

		case TAKE_MULTIPLE:
		{
			Integer value = InputFieldUtilities.getQuantity( message + " " + itemName + "...", itemCount );
			if ( value == null )
			{
				return Integer.MIN_VALUE;
			}
				
			quantity = value;

			break;
		}

		case USE_MULTIPLE:

			int standard = itemCount;

			if ( !message.equals( "Feed" ) )
			{
				if ( item instanceof Concoction )
				{
					int previous = 0, capacity = itemCount, unit = 0;

					if ( ( (Concoction) item ).getFullness() > 0 )
					{
						previous = KoLCharacter.getFullness() + ConcoctionDatabase.getQueuedFullness();
						capacity = KoLCharacter.getFullnessLimit();
						unit = ( (Concoction) item ).getFullness();
						standard = previous >= capacity ? itemCount : Math.min( ( capacity - previous ) / unit, itemCount );
					}
					else if ( ( (Concoction) item ).getInebriety() > 0 )
					{
						previous = KoLCharacter.getInebriety() + ConcoctionDatabase.getQueuedInebriety();
						capacity = KoLCharacter.getInebrietyLimit();
						unit = ( (Concoction) item ).getInebriety();
						standard = previous > capacity ? itemCount : Math.max( 1, Math.min( ( capacity - previous ) / unit, itemCount ) );
					}
					else if ( ( (Concoction) item ).getSpleenHit() > 0 )
					{
						previous = KoLCharacter.getSpleenUse() + ConcoctionDatabase.getQueuedSpleenHit();
						capacity = KoLCharacter.getSpleenLimit();
						unit = ( (Concoction) item ).getSpleenHit();
						standard = previous >= capacity ? itemCount : Math.min( ( capacity - previous ) / unit, itemCount );
					}
				}

				int maximum = UseItemRequest.maximumUses( itemName );

				standard = Math.min( standard, maximum );
			}

			quantity = standard;
			if ( standard >= 2 )
			{
				Integer value = InputFieldUtilities.getQuantity( message + " " + itemName + "...", itemCount, Math.min( standard, itemCount ) );
				if ( value == null )
				{
					return Integer.MIN_VALUE;
				}
				quantity = value;
			}

			break;

		default:
			quantity = 1;
			break;
		}

		return quantity;
	}

	protected int getUsableItemAmount( final Object item, final String itemName )
	{
		int id;
		if ( item instanceof Concoction )
		{
			id = ((Concoction) item).getItemId();
		}
		else
		{
			id = ((AdventureResult) item).getItemId();
		}
		switch ( ItemDatabase.getConsumptionType( id ) )
		{
		case KoLConstants.EQUIP_HAT:
			return Preferences.getInteger( "usableHats" );
		case KoLConstants.EQUIP_WEAPON:
			switch ( EquipmentDatabase.getHands( itemName ) )
			{
			case 3:
				return Preferences.getInteger( "usable3HWeapons" );
			case 2:
				return Preferences.getInteger( "usable2HWeapons" );
			default:
				return Preferences.getInteger( "usable1HWeapons" );
			}
		case KoLConstants.EQUIP_OFFHAND:
			return Preferences.getInteger( "usableOffhands" );
		case KoLConstants.EQUIP_SHIRT:
			return Preferences.getInteger( "usableShirts" );
		case KoLConstants.EQUIP_PANTS:
			return Preferences.getInteger( "usablePants" );
		case KoLConstants.EQUIP_ACCESSORY:
			Modifiers mods = Modifiers.getModifiers( itemName );
			if ( mods != null && mods.getBoolean( Modifiers.SINGLE ) )
			{
				return Preferences.getInteger( "usable1xAccs" );
			}
			else
			{
				return Preferences.getInteger( "usableAccessories" );
			}
		default:
			return Preferences.getInteger( "usableOther" );
		}
	}

	public abstract class TransferListener
		extends ThreadedListener
	{
		public String description;
		public boolean retrieveFromClosetFirst;

		public TransferListener( final String description, final boolean retrieveFromClosetFirst )
		{
			this.description = description;
			this.retrieveFromClosetFirst = retrieveFromClosetFirst;
		}

		public Object[] initialSetup()
		{
			return retrieveItems( getDesiredItems( description ) );
		}

		public Object[] initialSetup( final int transferType )
		{
			return retrieveItems( getDesiredItems( description, transferType ) );
		}

		private Object[] retrieveItems( final Object[] items )
		{
			if ( items == null )
			{
				return null;
			}

			if ( retrieveFromClosetFirst )
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, items ) );
			}

			return items;
		}
		
		@Override
		protected boolean retainFocus()
		{
			return true;
		}
	}

	public class ConsumeListener
		extends TransferListener
	{
		public ConsumeListener( final boolean retrieveFromClosetFirst )
		{
			super( "Consume", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				RequestThread.postRequest( UseItemRequest.getInstance( (AdventureResult) items[ i ] ) );
			}
		}

		@Override
		public String toString()
		{
			return "use item";
		}
	}

	public class EquipListener
		extends TransferListener
	{
		public EquipListener( final boolean retrieveFromClosetFirst )
		{
			super( "Equip", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				int usageType = ItemDatabase.getConsumptionType( item.getItemId() );

				switch ( usageType )
				{
				case KoLConstants.EQUIP_FAMILIAR:
				case KoLConstants.EQUIP_ACCESSORY:
				case KoLConstants.EQUIP_HAT:
				case KoLConstants.EQUIP_PANTS:
				case KoLConstants.EQUIP_CONTAINER:
				case KoLConstants.EQUIP_SHIRT:
				case KoLConstants.EQUIP_WEAPON:
				case KoLConstants.EQUIP_OFFHAND:
					RequestThread.postRequest( new EquipmentRequest(
									   item, EquipmentManager.consumeFilterToEquipmentType( usageType ) ) );
				}
			}
		}

		@Override
		public String toString()
		{
			return "equip item";
		}
	}

	public class PutInClosetListener
		extends TransferListener
	{
		public PutInClosetListener( final boolean retrieveFromClosetFirst )
		{
			super( retrieveFromClosetFirst ? "Bagging" : "Closeting", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = initialSetup();
			if ( items == null )
			{
				return;
			}

			if ( !retrieveFromClosetFirst )
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, items ) );
			}
		}

		@Override
		public String toString()
		{
			return retrieveFromClosetFirst ? "put in bag" : "put in closet";
		}
	}

	public class AutoSellListener
		extends TransferListener
	{
		private final boolean autosell;

		public AutoSellListener( final boolean retrieveFromClosetFirst, final boolean autosell )
		{
			super( autosell ? "Autoselling" : "Mallselling", retrieveFromClosetFirst );
			this.autosell = autosell;
		}

		@Override
		protected void execute()
		{
			if ( !autosell && !KoLCharacter.hasStore() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a store in the mall." );
				return;
			}

			if ( autosell && !InputFieldUtilities.confirm( "Are you sure you would like to sell the selected items?" ) )
			{
				return;
			}

			if ( !autosell && !InputFieldUtilities.confirm( "Are you sure you would like to place the selected items in your store?" ) )
			{
				return;
			}

			Object[] items = initialSetup();
			if ( items == null )
			{
				return;
			}

			if ( autosell )
			{
				RequestThread.postRequest( new AutoSellRequest( items ) );
			}
			else
			{
				RequestThread.postRequest( new AutoMallRequest( items ) );
			}
		}

		@Override
		public String toString()
		{
			return autosell ? "auto sell" : "place in mall";
		}
	}

	public class GiveToClanListener
		extends TransferListener
	{
		public GiveToClanListener( final boolean retrieveFromClosetFirst )
		{
			super( "Stashing", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = initialSetup();
			if ( items == null )
			{
				return;
			}

			RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
		}

		@Override
		public String toString()
		{
			return "clan stash";
		}
	}

	public class PutOnDisplayListener
		extends TransferListener
	{
		public PutOnDisplayListener( final boolean retrieveFromClosetFirst )
		{
			super( "Showcasing", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = initialSetup();
			if ( items == null )
			{
				return;
			}

			if ( !KoLCharacter.hasDisplayCase() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own a display case in the Cannon Museum." );
				return;
			}

			RequestThread.postRequest( new DisplayCaseRequest( items, true ) );
		}

		@Override
		public String toString()
		{
			return "display case";
		}
	}

	public class PulverizeListener
		extends TransferListener
	{
		public PulverizeListener( final boolean retrieveFromClosetFirst )
		{
			super( "Smashing", retrieveFromClosetFirst );
		}

		@Override
		protected void execute()
		{
			Object[] items = initialSetup();
			if ( items == null || items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new PulverizeRequest( (AdventureResult) items[ i ] ) );
			}
		}

		@Override
		public String toString()
		{
			return "pulverize";
		}
	}

	/**
	 * Special instance of a JComboBox which overrides the default key events of a JComboBox to allow you to catch key
	 * events.
	 */

	public class FilterItemField
		extends AutoFilterTextField
	{
		public boolean food, booze, equip, restores, other, notrade;

		public FilterItemField()
		{
			super( elementList.getDisplayModel() );

            food = true;
            booze = true;
            equip = true;
            restores = true;
            other = true;
            notrade = true;
		}

		@Override
		public void update()
		{
			if ( filters != null )
			{
                food = filters[ 0 ].isSelected();
                booze = filters[ 1 ].isSelected();
                equip = filters[ 2 ].isSelected();

                other = filters[ 3 ].isSelected();
                restores = other;
                notrade = filters[ 4 ].isSelected();
			}

			super.update();
		}

		@Override
		public boolean isVisible( final Object element )
		{
			if ( element instanceof AdventureResult && ( (AdventureResult) element ).getCount() < 0 )
			{
				// return false;
			}

			String name = AutoFilterTextField.getResultName( element );
			boolean isVisibleWithFilter = true;

			switch ( ItemDatabase.getConsumptionType( name ) )
			{
			case KoLConstants.CONSUME_EAT:
				isVisibleWithFilter = food;
				break;

			case KoLConstants.CONSUME_DRINK:
				isVisibleWithFilter = booze;
				break;

			case KoLConstants.EQUIP_HAT:
			case KoLConstants.EQUIP_SHIRT:
			case KoLConstants.EQUIP_WEAPON:
			case KoLConstants.EQUIP_OFFHAND:
			case KoLConstants.EQUIP_PANTS:
			case KoLConstants.EQUIP_CONTAINER:
			case KoLConstants.EQUIP_ACCESSORY:
			case KoLConstants.EQUIP_FAMILIAR:
				isVisibleWithFilter = equip;
				break;

			default:

				if ( element instanceof CreateItemRequest )
				{
					switch ( ConcoctionDatabase.getMixingMethod( name ) & KoLConstants.CT_MASK )
					{
					case KoLConstants.COOK:
					case KoLConstants.COOK_FANCY:
						isVisibleWithFilter = food || other;
						break;

					case KoLConstants.WOK:
					case KoLConstants.SUSHI:
						isVisibleWithFilter = food;
						break;

					case KoLConstants.MIX:
					case KoLConstants.MIX_FANCY:
					case KoLConstants.STILL_BOOZE:
						isVisibleWithFilter = booze;
						break;

					default:
						isVisibleWithFilter = other;
						break;
					}
				}
				else
				{
					// Milk of magnesium is marked as food,
					// as are munchies pills; all others
					// are marked as expected.

					isVisibleWithFilter = other;
					if ( name.equalsIgnoreCase( "milk of magnesium" ) ||
					     name.equalsIgnoreCase( "munchies pills" ) ||
					     name.equalsIgnoreCase( "distention pill" ))
					{
						isVisibleWithFilter |= food;
					}
				}
			}

			if ( !isVisibleWithFilter )
			{
				return false;
			}

			int itemId = element instanceof AdventureResult ?
				( (AdventureResult) element ).getItemId() :
				ItemDatabase.getItemId( name, 1, false );

			if ( itemId < 1 )
			{
				return filters == null && super.isVisible( element );
			}

			if ( !notrade && !ItemDatabase.isTradeable( itemId ) )
			{
				return false;
			}

			return super.isVisible( element );
		}
	}

	protected class RefreshButton
		extends RequestButton
	{
		public RefreshButton()
		{
			super( "refresh", new EquipmentRequest( EquipmentRequest.REFRESH ) );
		}
	}
}
