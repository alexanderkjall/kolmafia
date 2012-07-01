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
import java.awt.CardLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;

public class CharSheetFrame
	extends GenericFrame
{
	private final JLabel avatar;
	private JProgressBar[] tnpDisplay;
	private final KoLCharacterAdapter statusRefresher;

	/**
	 * Constructs a new character sheet, using the data located in the provided session.
	 */

	public CharSheetFrame()
	{
		super( "Daily Deeds" );

		JPanel statusPanel = new JPanel( new BorderLayout( 10, 10 ) );

        avatar = new JLabel( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );

		statusPanel.add( createStatusPanel(), BorderLayout.CENTER );
		statusPanel.add( avatar, BorderLayout.WEST );

		JPanel statusContainer = new JPanel( new CardLayout( 10, 10 ) );
		statusContainer.add( statusPanel, "" );

		JPanel summaryContainer = new JPanel( new CardLayout( 10, 10 ) );
		summaryContainer.add( AdventureSelectPanel.getAdventureSummary( "statusDropdown" ), "" );

		JPanel charSheetPanel = new JPanel( new BorderLayout() );
		charSheetPanel.add( statusContainer, BorderLayout.NORTH );
		charSheetPanel.add( summaryContainer, BorderLayout.CENTER );

        statusRefresher = new KoLCharacterAdapter( new StatusRefreshRunnable() );
		KoLCharacter.addCharacterListener( statusRefresher );

        statusRefresher.updateStatus();
		JComponentUtilities.setComponentSize( charSheetPanel, -1, 480 );

        setCenterComponent( charSheetPanel );
	}

	@Override
	public void dispose()
	{
		KoLCharacter.removeCharacterListener( statusRefresher );
		super.dispose();
	}

	@Override
	public boolean useSidePane()
	{
		return false;
	}

	/**
	 * Utility method for modifying a panel that displays the given label, using formatting if the values are different.
	 */

	private void refreshValuePanel( final int displayIndex, final int baseValue,
		final int tillNextPoint, final String label )
	{
		JProgressBar tnp = tnpDisplay[ displayIndex ];

		if ( baseValue == KoLCharacter.MAX_BASEPOINTS )
		{
			tnp.setMaximum( 0 );
			tnp.setValue( 0 );
			tnp.setString( "No more progress possible" );
		}
		else
		{
			tnp.setMaximum( 2 * baseValue + 1 );
			tnp.setValue( 2 * baseValue + 1 - tillNextPoint );
			tnp.setString( label +
				KoLConstants.COMMA_FORMAT.format( tnp.getValue() ) + " / " +
				KoLConstants.COMMA_FORMAT.format( tnp.getMaximum() ) );
		}

		int points = KoLCharacter.getTriggerPoints( displayIndex );

		int triggerItemId = KoLCharacter.getTriggerItem( displayIndex );

		if ( points == Integer.MAX_VALUE || triggerItemId <= 0 )
		{
			tnp.setToolTipText( "You can equip everything you have!" );
		}
		else
		{
			String triggerItem = ItemDatabase.getItemName( triggerItemId );

			tnp.setToolTipText( "At " + points +
				" points, you'll be able to equip a " +
				triggerItem + " (and maybe more)" );
		}
	}

	/**
	 * Utility method for creating a panel displaying the character's vital statistics, including a basic stat overview
	 * and available turns/meat.
	 *
	 * @return a <code>JPanel</code> displaying the character's statistics
	 */

	private Box createStatusPanel()
	{
		Box statusPanel = Box.createVerticalBox();
		statusPanel.add( Box.createVerticalGlue() );

        tnpDisplay = new JProgressBar[ 3 ];
		for ( int i = 0; i < 3; ++i )
		{
            tnpDisplay[ i ] = new JProgressBar();
            tnpDisplay[ i ].setValue( 0 );
            tnpDisplay[ i ].setStringPainted( true );
			statusPanel.add( tnpDisplay[ i ] );
			statusPanel.add( Box.createVerticalGlue() );
		}

		statusPanel.setBorder( BorderFactory.createTitledBorder(
			"Substats till next point" ) );
		return statusPanel;
	}

	private class StatusRefreshRunnable
		implements Runnable
	{
		public void run()
		{
            refreshValuePanel(
				0, KoLCharacter.getBaseMuscle(),
				KoLCharacter.getMuscleTNP(), "Mus: " );
            refreshValuePanel(
				1, KoLCharacter.getBaseMysticality(), KoLCharacter.getMysticalityTNP(), "Mys: " );
            refreshValuePanel(
				2, KoLCharacter.getBaseMoxie(),
				KoLCharacter.getMoxieTNP(), "Mox: " );

			// Set the current avatar
            avatar.setIcon( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );
		}
	}
}
