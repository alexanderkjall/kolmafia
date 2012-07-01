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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.BuffBotManager;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BuffBotFrame
	extends GenericFrame
{
	private JList buffListDisplay;

	/**
	 * Constructs a new <code>BuffBotFrame</code> and inserts all of the necessary panels into a tabular layout for
	 * accessibility.
	 */

	public BuffBotFrame()
	{
		super( "BuffBot Manager" );

		// Initialize the display log buffer and the file log

        tabs.addTab( "Run Buffbot", new MainBuffPanel() );

		JPanel optionsContainer = new JPanel( new BorderLayout( 10, 10 ) );
		optionsContainer.add( new BuffOptionsPanel(), BorderLayout.NORTH );
		optionsContainer.add( new BuffListPanel(), BorderLayout.CENTER );

        tabs.addTab( "Edit Offerings", optionsContainer );
        addTab( "Change Settings", new MainSettingsPanel() );

        setCenterComponent( tabs );
	}

	@Override
	public boolean useSidePane()
	{
		return true;
	}

	/**
	 * Internal class used to handle everything related to operating the buffbot.
	 */

	private class MainBuffPanel
		extends ScrollablePanel
	{
		public MainBuffPanel()
		{
			super( "BuffBot Activities", "start", "stop", new JList( BuffBotHome.getMessages() ) );

			BuffBotHome.setFrame( BuffBotFrame.this );
			( (JList) scrollComponent).setCellRenderer( BuffBotHome.getMessageRenderer() );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( confirmedButton == null )
			{
				return;
			}

            confirmedButton.setEnabled( isEnabled );
		}

		@Override
		public void actionConfirmed()
		{
			if ( BuffBotHome.isBuffBotActive() )
			{
				return;
			}

			// Need to make sure everything is up to date.
			// This includes character status, inventory
			// data and current settings.

			BuffBotHome.setBuffBotActive( true );
			BuffBotManager.runBuffBot( Integer.MAX_VALUE );
		}

		@Override
		public void actionCancelled()
		{
			BuffBotHome.setBuffBotActive( false );
		}
	}

	/**
	 * Internal class used to handle everything related to BuffBot options management
	 */

	private class BuffOptionsPanel
		extends GenericPanel
	{
		private final JComboBox skillSelect;
		private final AutoHighlightTextField priceField, countField;

		public BuffOptionsPanel()
		{
			super( "add", "remove", new Dimension( 150, 20 ), new Dimension( 300, 20 ) );
			UseSkillRequest skill;

			LockableListModel skillSet = KoLConstants.usableSkills;
			LockableListModel buffSet = new LockableListModel();
			for ( int i = 0; ( skill = (UseSkillRequest) skillSet.get( i ) ) != null; ++i )
			{
				if ( SkillDatabase.isBuff( SkillDatabase.getSkillId( skill.getSkillName() ) ) )
				{
					buffSet.add( skill );
				}
			}

            skillSelect = new JComboBox( buffSet );

            priceField = new AutoHighlightTextField();
            countField = new AutoHighlightTextField();

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "Buff to cast: ", skillSelect );
			elements[ 1 ] = new VerifiableElement( "Price (in meat): ", priceField );
			elements[ 2 ] = new VerifiableElement( "# of casts: ", countField );
            setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			BuffBotManager.addBuff(
				( (UseSkillRequest) skillSelect.getSelectedItem() ).getSkillName(),
				StringUtilities.parseInt( priceField.getText() ), StringUtilities.parseInt( countField.getText() ) );
		}

		@Override
		public void actionCancelled()
		{
			BuffBotManager.removeBuffs( buffListDisplay.getSelectedValues() );
		}
	}

	private class BuffListPanel
		extends JPanel
	{
		public BuffListPanel()
		{
            setLayout( new BorderLayout() );
            setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
            add( JComponentUtilities.createLabel(
				"Active Buffing List", SwingConstants.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

            buffListDisplay = new JList( BuffBotManager.getBuffCostTable() );
            buffListDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
            buffListDisplay.setVisibleRowCount( 5 );

            add( new GenericScrollPane( buffListDisplay ), BorderLayout.CENTER );
		}
	}

	/**
	 * Internal class used to handle everything related to BuffBot White List management
	 */

	private class MainSettingsPanel
		extends GenericPanel
	{
		private final JTextArea invalidPriceMessage, thanksMessage;
		private final JComboBox philanthropyModeSelect;
		private final JComboBox messageDisposalSelect;

		public MainSettingsPanel()
		{
			super( "save", "reset", new Dimension( 120, 20 ), new Dimension( 200, 20 ), false );

			LockableListModel philanthropyModeChoices = new LockableListModel();
			philanthropyModeChoices.add( "Disabled" );
			philanthropyModeChoices.add( "Once per day" );
			philanthropyModeChoices.add( "Clan only" );
            philanthropyModeSelect = new JComboBox( philanthropyModeChoices );

			LockableListModel messageDisposalChoices = new LockableListModel();
			messageDisposalChoices.add( "Auto-save non-requests" );
			messageDisposalChoices.add( "Auto-delete non-requests" );
			messageDisposalChoices.add( "Do nothing to non-requests" );
            messageDisposalSelect = new JComboBox( messageDisposalChoices );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Philanthropy: ", philanthropyModeSelect );
			elements[ 1 ] = new VerifiableElement( "Message disposal: ", messageDisposalSelect );

            invalidPriceMessage = new JTextArea();
            thanksMessage = new JTextArea();

            invalidPriceMessage.setFont( KoLConstants.DEFAULT_FONT );
            invalidPriceMessage.setLineWrap( true );
            invalidPriceMessage.setWrapStyleWord( true );

            thanksMessage.setFont( KoLConstants.DEFAULT_FONT );
            thanksMessage.setLineWrap( true );
            thanksMessage.setWrapStyleWord( true );

            actionCancelled();
            setContent( elements );
		}

		@Override
		public void setContent( final VerifiableElement[] elements )
		{
			super.setContent( elements );

			JPanel settingsMiddlePanel = new JPanel( new BorderLayout() );
			settingsMiddlePanel.add( JComponentUtilities.createLabel(
				"Invalid Buff Price Message", SwingConstants.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			settingsMiddlePanel.add( invalidPriceMessage, BorderLayout.CENTER );

			JPanel settingsBottomPanel = new JPanel( new BorderLayout() );
			settingsBottomPanel.add( JComponentUtilities.createLabel(
				"Donation Thanks Message", SwingConstants.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
			settingsBottomPanel.add( thanksMessage, BorderLayout.CENTER );

			JPanel settingsPanel = new JPanel( new GridLayout( 2, 1, 10, 10 ) );

			settingsPanel.add( settingsMiddlePanel );
			settingsPanel.add( settingsBottomPanel );

            add( settingsPanel, BorderLayout.CENTER );
		}

		@Override
		public void actionConfirmed()
		{
			Preferences.setInteger(
				"buffBotPhilanthropyType", philanthropyModeSelect.getSelectedIndex() );
			Preferences.setInteger(
				"buffBotMessageDisposal", messageDisposalSelect.getSelectedIndex() );
			Preferences.setString( "invalidBuffMessage", invalidPriceMessage.getText() );
			Preferences.setString( "thanksMessage", thanksMessage.getText() );
		}

		@Override
		public void actionCancelled()
		{
            philanthropyModeSelect.setSelectedIndex( Preferences.getInteger( "buffBotPhilanthropyType" ) );
            messageDisposalSelect.setSelectedIndex( Preferences.getInteger( "buffBotMessageDisposal" ) );
            invalidPriceMessage.setText( Preferences.getString( "invalidBuffMessage" ) );
            thanksMessage.setText( Preferences.getString( "thanksMessage" ) );
		}
	}
}
