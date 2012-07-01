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

import com.sun.java.forums.TableSorter;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.ProfileSnapshot;

import net.sourceforge.kolmafia.request.ClanBuffRequest;
import net.sourceforge.kolmafia.request.ClanMembersRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClanWarRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;

import net.sourceforge.kolmafia.session.ClanManager;

import net.sourceforge.kolmafia.swingui.button.RequestButton;

import net.sourceforge.kolmafia.swingui.listener.TableButtonListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;

import net.sourceforge.kolmafia.swingui.table.ButtonRenderer;
import net.sourceforge.kolmafia.swingui.table.ListWrapperTableModel;
import net.sourceforge.kolmafia.swingui.table.TransparentTable;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan management functionality of Kingdom of Loathing.
 */

public class ClanManageFrame
	extends GenericFrame
{
	private static final int MOVE_ALL = 2;
	private static final int MOVE_ALL_BUT = 3;
	private final JTable members;
	private final SnapshotPanel snapshot;
	private final ClanBuffPanel clanBuff;
	private final StoragePanel storing;
	private final WithdrawPanel withdrawal;
	private final DonationPanel donation;
	private final AttackPanel attacks;
	private final WarfarePanel warfare;
	private final MemberSearchPanel search;

	public ClanManageFrame()
	{
		super( "Clan Management" );

        snapshot = new SnapshotPanel();
        attacks = new AttackPanel();
        storing = new StoragePanel();
        clanBuff = new ClanBuffPanel();
        donation = new DonationPanel();
        withdrawal = new WithdrawPanel();
        search = new MemberSearchPanel();
        warfare = new WarfarePanel();

		JPanel adminPanel = new JPanel();
		adminPanel.setLayout( new BoxLayout( adminPanel, BoxLayout.Y_AXIS ) );
		adminPanel.add( attacks );
		adminPanel.add( snapshot );

        addTab( "Admin", adminPanel );

		JPanel spendPanel = new JPanel();
		spendPanel.setLayout( new BoxLayout( spendPanel, BoxLayout.Y_AXIS ) );
		spendPanel.add( donation );
		spendPanel.add( clanBuff );
		spendPanel.add( warfare );

        addTab( "Coffers", spendPanel );
        tabs.addTab( "Deposit", storing );
        tabs.addTab( "Withdraw", withdrawal );

        members = new TransparentTable( new MemberTableModel() );
        members.setModel( new TableSorter( members.getModel(), members.getTableHeader() ) );
        members.getTableHeader().setReorderingAllowed( false );

        members.setRowSelectionAllowed( false );
        members.setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN );

        members.addMouseListener( new TableButtonListener( members ) );
        members.setDefaultRenderer( JButton.class, new ButtonRenderer() );

        members.setShowGrid( false );
        members.setIntercellSpacing( new Dimension( 5, 5 ) );
        members.setRowHeight( 25 );

        members.getColumnModel().getColumn( 0 ).setMinWidth( 30 );
        members.getColumnModel().getColumn( 0 ).setMaxWidth( 30 );

        members.getColumnModel().getColumn( 1 ).setMinWidth( 120 );
        members.getColumnModel().getColumn( 1 ).setMaxWidth( 120 );

        members.getColumnModel().getColumn( 3 ).setMinWidth( 120 );
        members.getColumnModel().getColumn( 3 ).setMaxWidth( 120 );

        members.getColumnModel().getColumn( 4 ).setMinWidth( 45 );
        members.getColumnModel().getColumn( 4 ).setMaxWidth( 45 );

		GenericScrollPane results = new GenericScrollPane( members );
		JComponentUtilities.setComponentSize( results, 400, 300 );

		JPanel searchPanel = new JPanel( new BorderLayout() );
		searchPanel.add( search, BorderLayout.NORTH );

		JPanel resultsPanel = new JPanel( new BorderLayout() );
		resultsPanel.add( members.getTableHeader(), BorderLayout.NORTH );
		resultsPanel.add( results, BorderLayout.CENTER );
		searchPanel.add( resultsPanel, BorderLayout.CENTER );

        tabs.addTab( "Member Search", searchPanel );

        setCenterComponent( tabs );
	}

	/**
	 * An internal class which represents the panel used for clan buffs in the <code>ClanManageFrame</code>.
	 */

	private class ClanBuffPanel
		extends LabeledPanel
	{
		private final boolean isBuffing;
		private final JComboBox buffField;
		private final AutoHighlightTextField countField;

		public ClanBuffPanel()
		{
			super( "Buy Clan Buffs", "purchase", "take break", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );
            isBuffing = false;

            buffField = new JComboBox( ClanBuffRequest.getRequestList() );
            countField = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[ 1 ] = new VerifiableElement( "# of times: ", countField );

            setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			StaticEntity.getClient().makeRequest(
				(Runnable) buffField.getSelectedItem(), InputFieldUtilities.getValue( countField ) );
		}

		@Override
		public void actionCancelled()
		{
			if ( isBuffing )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Purchase attempts cancelled." );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for clan buffs in the <code>ClanManageFrame</code>.
	 */

	private class AttackPanel
		extends LabeledPanel
	{
		private final JLabel nextAttack;
		private final AutoFilterComboBox enemyList;

		public AttackPanel()
		{
			super( "Loot Another Clan", "attack", "refresh", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

            nextAttack = new JLabel( ClanWarRequest.getNextAttack() );
            enemyList = new AutoFilterComboBox( ClanWarRequest.getEnemyClans(), false );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Victim: ", enemyList );
			elements[ 1 ] = new VerifiableElement( " ", nextAttack );
            setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			RequestThread.postRequest( (ClanWarRequest) enemyList.getSelectedItem() );
		}

		@Override
		public void actionCancelled()
		{
			RequestThread.postRequest( new ClanWarRequest() );
            nextAttack.setText( ClanWarRequest.getNextAttack() );
		}
	}

	private class WarfarePanel
		extends LabeledPanel
	{
		private final AutoHighlightTextField goodies;
		private final AutoHighlightTextField oatmeal, recliners;
		private final AutoHighlightTextField grunts, flyers, archers;

		public WarfarePanel()
		{
			super( "Prepare for WAR!!!", "purchase", "calculate", new Dimension( 120, 20 ), new Dimension( 200, 20 ) );

            goodies = new AutoHighlightTextField();
            oatmeal = new AutoHighlightTextField();
            recliners = new AutoHighlightTextField();
            grunts = new AutoHighlightTextField();
            flyers = new AutoHighlightTextField();
            archers = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 6 ];
			elements[ 0 ] = new VerifiableElement( "Goodies: ", goodies );
			elements[ 1 ] = new VerifiableElement( "Oatmeal: ", oatmeal );
			elements[ 2 ] = new VerifiableElement( "Recliners: ", recliners );
			elements[ 3 ] = new VerifiableElement( "Ground Troops: ", grunts );
			elements[ 4 ] = new VerifiableElement( "Airborne Troops: ", flyers );
			elements[ 5 ] = new VerifiableElement( "La-Z-Archers: ", archers );

            setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			RequestThread.postRequest( new ClanWarRequest(
				InputFieldUtilities.getValue( goodies ), InputFieldUtilities.getValue( oatmeal ),
				InputFieldUtilities.getValue( recliners ), InputFieldUtilities.getValue( grunts ),
				InputFieldUtilities.getValue( flyers ), InputFieldUtilities.getValue( archers ) ) );
		}

		@Override
		public void actionCancelled()
		{
			int totalValue =
				InputFieldUtilities.getValue( goodies ) * 1000 + InputFieldUtilities.getValue( oatmeal ) * 3 + InputFieldUtilities.getValue( recliners ) * 1500 + InputFieldUtilities.getValue( grunts ) * 300 + InputFieldUtilities.getValue( flyers ) * 500 + InputFieldUtilities.getValue( archers ) * 500;

			InputFieldUtilities.alert( "This purchase will cost " + totalValue + " meat" );
		}
	}

	/**
	 * An internal class which represents the panel used for donations to the clan coffer.
	 */

	private class DonationPanel
		extends LabeledPanel
	{
		private final AutoHighlightTextField amountField;

		public DonationPanel()
		{
			super( "Fund Your Clan", "donate meat", "loot clan", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

            amountField = new AutoHighlightTextField();
			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Amount: ", amountField );
            setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			RequestThread.postRequest( new ClanStashRequest( InputFieldUtilities.getValue( amountField ) ) );
		}

		@Override
		public void actionCancelled()
		{
			InputFieldUtilities.alert( "The Hermit beat you to it.  ARGH!" );
		}
	}

	private class StoragePanel
		extends ItemManagePanel
	{
		public StoragePanel()
		{
			super( KoLConstants.inventory );
            setButtons( new ActionListener[] { new StorageListener(), new RequestButton(
				"refresh", new EquipmentRequest( EquipmentRequest.REFRESH ) ) } );
		}

		private class StorageListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				Object[] items = getDesiredItems( "Deposit" );
				if ( items == null )
				{
					return;
				}

				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.ITEMS_TO_STASH ) );
			}

			@Override
			public String toString()
			{
				return "add items";
			}
		}
	}

	/**
	 * Internal class used to handle everything related to placing items into the stash.
	 */

	private class WithdrawPanel
		extends ItemManagePanel
	{
		public WithdrawPanel()
		{
			super( ClanManager.getStash() );

            setButtons( new ActionListener[] { new WithdrawListener( ClanManageFrame.MOVE_ALL ), new WithdrawListener(
				ClanManageFrame.MOVE_ALL_BUT ), new RequestButton( "refresh", new ClanStashRequest() ) } );
            elementList.setCellRenderer( ListCellRendererFactory.getDefaultRenderer() );
		}

		private class WithdrawListener
			extends ThreadedListener
		{
			private final int moveType;

			public WithdrawListener( final int moveType )
			{
				this.moveType = moveType;
			}

			@Override
			protected void execute()
			{
				if ( !KoLCharacter.canInteract() )
				{
					return;
				}

				Object[] items;

				if ( moveType == ClanManageFrame.MOVE_ALL_BUT )
				{
					items = elementList.getSelectedValues();
					if ( items.length == 0 )
					{
						items = elementModel.toArray();
					}

					if ( items.length == 0 )
					{
						return;
					}

					Integer value = InputFieldUtilities.getQuantity( "Maximum number of each item allowed in the stash?", Integer.MAX_VALUE, 100 );
					int quantity = ( value == null ) ? 0 : value;

					if ( quantity == 0 )
					{
						return;
					}


					for ( int i = 0; i < items.length; ++i )
					{
						AdventureResult currentItem = (AdventureResult) items[ i ];
						items[ i ] = currentItem.getInstance( Math.max( 0, currentItem.getCount() - quantity ) );
					}
				}
				else
				{
					items = getDesiredItems( "Take" );
				}

				if ( items == null )
				{
					return;
				}

				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.STASH_TO_ITEMS ) );
			}

			@Override
			public String toString()
			{
				return moveType == ClanManageFrame.MOVE_ALL_BUT ? "cap stash" : "take items";
			}
		}
	}

	private class MemberSearchPanel
		extends GenericPanel
	{
		private final JComboBox parameterSelect;
		private final JComboBox matchSelect;
		private final AutoHighlightTextField valueField;

		public MemberSearchPanel()
		{
			super( "search clan", "apply changes", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

            parameterSelect = new JComboBox();
			for ( int i = 0; i < ProfileSnapshot.FILTER_NAMES.length; ++i )
			{
                parameterSelect.addItem( ProfileSnapshot.FILTER_NAMES[i] );
			}

            matchSelect = new JComboBox();
            matchSelect.addItem( "Less than..." );
            matchSelect.addItem( "Equal to..." );
            matchSelect.addItem( "Greater than..." );

            valueField = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "Parameter: ", parameterSelect );
			elements[ 1 ] = new VerifiableElement( "Constraint: ", matchSelect );
			elements[ 2 ] = new VerifiableElement( "Value:", valueField );

            setContent( elements, true );
		}

		@Override
		public void actionConfirmed()
		{
			ClanManager.applyFilter(
                    matchSelect.getSelectedIndex() - 1, parameterSelect.getSelectedIndex(),
                    valueField.getText() );
			KoLmafia.updateDisplay( "Search results retrieved." );
		}

		@Override
		public void actionCancelled()
		{
			if ( !InputFieldUtilities.finalizeTable( members ) )
			{
				return;
			}

			KoLmafia.updateDisplay( "Determining changes..." );

			ArrayList titleChange = new ArrayList();
			ArrayList newTitles = new ArrayList();
			ArrayList boots = new ArrayList();

			for ( int i = 0; i < members.getRowCount(); ++i )
			{
				if ( (Boolean) members.getValueAt( i, 4 ) )
				{
					boots.add( members.getValueAt( i, 1 ) );
				}

				titleChange.add( members.getValueAt( i, 1 ) );
				newTitles.add( members.getValueAt( i, 2 ) );
			}

			KoLmafia.updateDisplay( "Applying changes..." );
			RequestThread.postRequest( new ClanMembersRequest(
				titleChange.toArray(), newTitles.toArray(), boots.toArray() ) );
			KoLmafia.updateDisplay( "Changes have been applied." );
		}
	}

	private class MemberTableModel
		extends ListWrapperTableModel
	{
		public MemberTableModel()
		{
			super(
				new String[] { " ", "Name", "Clan Title", "Total Karma", "Boot" },
				new Class[] { JButton.class, String.class, String.class, Integer.class, Boolean.class },
				new boolean[] { false, false, true, false, true }, ProfileSnapshot.getFilteredList() );
		}

		@Override
		public Vector constructVector( final Object o )
		{
			ProfileRequest p = (ProfileRequest) o;

			Vector value = new Vector();

			JButton profileButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			profileButton.addMouseListener( new ShowProfileListener( p ) );
			JComponentUtilities.setComponentSize( profileButton, 20, 20 );

			value.add( profileButton );
			value.add( p.getPlayerName() );
			value.add( p.getTitle() );
			value.add( p.getKarma() );
			value.add( Boolean.FALSE );

			return value;
		}
	}

	private class ShowProfileListener
		extends ThreadedListener
	{
		private final ProfileRequest profile;

		public ShowProfileListener( final ProfileRequest profile )
		{

			this.profile = profile;
		}

		@Override
		protected void execute()
		{
            ProfileFrame.showRequest( profile );
		}
	}

	private class SnapshotPanel
		extends LabeledPanel
	{
		private final AutoHighlightTextField mostAscensionsBoardSizeField;
		private final AutoHighlightTextField mainBoardSizeField;
		private final AutoHighlightTextField classBoardSizeField;
		private final AutoHighlightTextField maxAgeField;

		private final JCheckBox playerMoreThanOnceOption;
		private final JCheckBox localProfileOption;

		public SnapshotPanel()
		{
			super( "Clan Snapshot", "snapshot", "activity log", new Dimension( 250, 20 ), new Dimension( 50, 20 ) );

			VerifiableElement[] elements = new VerifiableElement[ 7 ];

            mostAscensionsBoardSizeField = new AutoHighlightTextField( "20" );
            mainBoardSizeField = new AutoHighlightTextField( "10" );
            classBoardSizeField = new AutoHighlightTextField( "5" );
            maxAgeField = new AutoHighlightTextField( "0" );

            playerMoreThanOnceOption = new JCheckBox();
            localProfileOption = new JCheckBox();

			elements[ 0 ] = new VerifiableElement( "Most Ascensions Board Size:  ", mostAscensionsBoardSizeField );
			elements[ 1 ] = new VerifiableElement( "Fastest Ascensions Board Size:  ", mainBoardSizeField );
			elements[ 2 ] = new VerifiableElement( "Class Breakdown Board Size:  ", classBoardSizeField );
			elements[ 3 ] = new VerifiableElement( "Maximum Ascension Age (in days):  ", maxAgeField );
			elements[ 4 ] = new VerifiableElement();
			elements[ 5 ] = new VerifiableElement( "Add Internal Profile Links:  ", localProfileOption );
			elements[ 6 ] = new VerifiableElement( "Allow Multiple Appearances:  ", playerMoreThanOnceOption );

            setContent( elements, true );
		}

		@Override
		public void actionConfirmed()
		{
			// Now that you've got everything, go ahead and
			// generate the snapshot.

			int mostAscensionsBoardSize = InputFieldUtilities.getValue( mostAscensionsBoardSizeField, Integer.MAX_VALUE );
			int mainBoardSize = InputFieldUtilities.getValue( mainBoardSizeField, Integer.MAX_VALUE );
			int classBoardSize = InputFieldUtilities.getValue( classBoardSizeField, Integer.MAX_VALUE );
			int maxAge = InputFieldUtilities.getValue( maxAgeField, Integer.MAX_VALUE );

			boolean playerMoreThanOnce = playerMoreThanOnceOption.isSelected();
			boolean localProfileLink = localProfileOption.isSelected();

			// Now that you've got everything, go ahead and
			// generate the snapshot.

			ClanManager.takeSnapshot(
				mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink );
		}

		@Override
		public void actionCancelled()
		{
			ClanManager.saveStashLog();
		}
	}
}
