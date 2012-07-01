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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.MushroomManager;

import net.sourceforge.kolmafia.swingui.MushroomFrame;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MushroomScriptPanel
	extends JPanel
{
	private static final Color TODAY_COLOR = new Color( 192, 255, 192 );
	private static final Color OTHER_COLOR = new Color( 240, 240, 240 );

	private String currentLayout = "";

	private final JPanel centerPanel;
	private int currentForecast = 2;
	private final JButton addToLayoutButton, deleteFromLayoutButton;

	private final String[][] planningData;
	private final String[][] originalData;

	private final JLabel[] headers;
	private final JPanel[] planningPanels;
	private final MushroomButton[][][] planningButtons;

	public MushroomScriptPanel()
	{
        headers = new JLabel[ MushroomFrame.MAX_FORECAST + 1 ];

        planningData = new String[ MushroomFrame.MAX_FORECAST + 1 ][ 16 ];
        originalData = new String[ MushroomFrame.MAX_FORECAST + 1 ][ 16 ];

		for ( int i = 0; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			for ( int j = 0; j < 16; ++j )
			{
                planningData[ i ][ j ] = "__";
                originalData[ i ][ j ] = "__";
			}
		}

        centerPanel = new JPanel( new GridLayout( 0, 4, 20, 20 ) );

		// Now add the first panel to the layout so that the person
		// can add more panels as they are needed.

        planningPanels = new JPanel[ MushroomFrame.MAX_FORECAST + 1 ];
        planningButtons = new MushroomButton[ MushroomFrame.MAX_FORECAST + 1 ][ 4 ][ 4 ];

		for ( int i = 0; i < MushroomFrame.MAX_FORECAST; ++i )
		{
            planningPanels[ i ] = new JPanel( new GridLayout( 4, 4, 0, 2 ) );
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
                    planningButtons[ i ][ j ][ k ] = new MushroomButton( i, j * 4 + k );
                    planningPanels[ i ].add( planningButtons[ i ][ j ][ k ] );
				}
			}
		}

        centerPanel.add( constructPanel( 0, planningPanels[0] ) );
        centerPanel.add( constructPanel( 1, planningPanels[1] ) );

		// Dummy buttons for the mushroom plot (just for layout
		// viewing purposes.  To be replaced with real functionality
		// at a later date.

		JPanel buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );

		// Now add the various action buttons.

        addToLayoutButton = new InvocationButton( "Add a Day", this, "addToLayout" );
        deleteFromLayoutButton = new InvocationButton( "Delete a Day", this, "removeFromLayout" );
        deleteFromLayoutButton.setEnabled( false );

		buttonPanel.add( addToLayoutButton );
		buttonPanel.add( deleteFromLayoutButton );
		buttonPanel.add( new InvocationButton( "Run Layout", this, "runLayout" ) );
		buttonPanel.add( new InvocationButton( "Load Layout", this, "loadLayout" ) );
		buttonPanel.add( new InvocationButton( "Save Layout", this, "saveLayout" ) );
        centerPanel.add( buttonPanel );

        setLayout( new CardLayout( 40, 40 ) );
        add( centerPanel, "" );

        enableLayout();
        currentLayout = Preferences.getString( "plantingScript" );
        initializeLayout();
	}

	private void enableLayout()
	{
		for ( int i = 0; i < currentForecast; ++i )
		{
            headers[ i ].setText( "Day " + ( i + 1 ) );
		}

        headers[currentForecast - 1 ].setText( "Final Day" );

		for ( int i = 0; i < 16; ++i )
		{
            planningData[currentForecast][ i ] = "__";
            originalData[currentForecast][ i ] = "__";
		}

        updateForecasts( currentForecast - 1 );

        centerPanel.validate();
        centerPanel.repaint();

        addToLayoutButton.setEnabled( currentForecast != MushroomFrame.MAX_FORECAST );
        deleteFromLayoutButton.setEnabled( currentForecast != 2 );
	}

	public void addToLayout()
	{
        centerPanel.invalidate();
        centerPanel.add(
                constructPanel( currentForecast, planningPanels[currentForecast] ),
                currentForecast < 3 ? currentForecast : currentForecast + 1 );

		++currentForecast;
        enableLayout();
	}

	public void removeFromLayout()
	{
        centerPanel.invalidate();
        centerPanel.remove( currentForecast < 4 ? currentForecast - 1 : currentForecast );

		--currentForecast;
        enableLayout();
	}

	public void initializeLayout()
	{
		int plantingLength = 2;
		int indexToHighlight = 0;

		if ( currentLayout.equals( "" ) )
		{
			Preferences.setInteger( "plantingDay", -1 );
			Preferences.setString( "plantingDate", "" );
			Preferences.setInteger( "plantingLength", 0 );
		}
		else
		{
			plantingLength = MushroomManager.loadLayout( currentLayout, originalData, planningData );
			indexToHighlight = Preferences.getInteger( "plantingDay" );
		}

		if ( plantingLength > currentForecast )
		{
            centerPanel.invalidate();
			for ( int i = currentForecast; i < plantingLength; ++i )
			{
                centerPanel.add( constructPanel( i, planningPanels[i] ), i < 3 ? i : i + 1 );
			}

            currentForecast = plantingLength;
            enableLayout();
		}
		else if ( plantingLength > 1 )
		{
            centerPanel.invalidate();
			for ( int i = currentForecast; i > plantingLength; --i )
			{
                centerPanel.remove( i < 4 ? i - 1 : i );
			}

            currentForecast = plantingLength;
            enableLayout();
		}

		String today = KoLConstants.DAILY_FORMAT.format( new Date() );

		if ( !Preferences.getString( "plantingDate" ).equals( today ) )
		{
			++indexToHighlight;
		}

		for ( int i = 0; i < currentForecast; ++i )
		{
            headers[ i ].setBackground( i == indexToHighlight ? MushroomScriptPanel.TODAY_COLOR : MushroomScriptPanel.OTHER_COLOR );
		}

        updateImages();
	}

	public void runLayout()
	{
		if ( currentLayout.equals( "" ) )
		{
            saveLayout();
		}

		if ( !currentLayout.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + KoLConstants.PLOTS_DIRECTORY + currentLayout + ".ash" );
		}
	}

	public void loadLayout()
	{
		File[] layouts = DataUtilities.listFiles( KoLConstants.PLOTS_LOCATION );
		ArrayList names = new ArrayList();

		for ( int i = 0; i < layouts.length; ++i )
		{
			String name = layouts[ i ].getName();
			if ( name.endsWith( ".txt" ) )
			{
				name = name.substring( 0, name.length() - 4 );
				if ( !names.contains( name ) )
				{
					names.add( name );
				}
			}
		}

		if ( names.isEmpty() )
		{
			return;
		}

		String layout = (String) InputFieldUtilities.input( "Which mushroom plot?", names.toArray() );
		if ( layout != null )
		{
            loadLayout( layout );
		}
	}

	public void loadLayout( final String layout )
	{
		if ( layout == null || layout.equals( "" ) || currentLayout.equals( layout ) )
		{
			return;
		}

        currentLayout = layout;
        initializeLayout();
	}

	public void saveLayout()
	{
		String location = InputFieldUtilities.input( "Name your mushroom plot!" );
		if ( location == null )
		{
			return;
		}

        currentLayout = location;

		String[] planned = new String[ 16 ];

		for ( int i = 0; i < 16; ++i )
		{
			planned[ i ] = planningData[currentForecast - 1 ][ i ];
            planningData[currentForecast - 1 ][ i ] = "__";
		}

		MushroomManager.saveLayout( location, originalData, planningData );
		for ( int i = 0; i < 16; ++i )
		{
            planningData[currentForecast - 1 ][ i ] = planned[ i ];
		}
	}

	public void updateForecasts( final int startDay )
	{
		for ( int i = startDay; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			String[][] holdingData = new String[ 4 ][ 4 ];
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
					holdingData[ j ][ k ] = planningData[ i - 1 ][ j * 4 + k ];
				}
			}

			String[] forecastData = MushroomManager.getForecastedPlot( true, holdingData ).split( ";" );
			for ( int j = 0; j < 16; ++j )
			{
                planningData[ i ][ j ] = forecastData[ j ];
                originalData[ i ][ j ] = forecastData[ j ];
			}
		}

        updateImages();
	}

	private void updateImages()
	{
		for ( int i = 0; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
                    planningButtons[ i ][ j ][ k ].updateImage();
				}
			}
		}
	}

	public JPanel constructPanel( final int dayIndex, final Component c )
	{
		JPanel panel = new JPanel( new BorderLayout() );
		panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

        headers[ dayIndex ] = new JLabel( "Day " + ( dayIndex + 1 ), SwingConstants.CENTER );

		panel.add( headers[ dayIndex ], BorderLayout.NORTH );
		panel.add( c, BorderLayout.CENTER );

		return panel;
	}

	private class MushroomButton
		extends JButton
		implements ActionListener
	{
		private final int dayIndex;
		private int loopIndex;
		private final int squareIndex;

		public MushroomButton( final int dayIndex, final int squareIndex )
		{
			super( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );

			this.dayIndex = dayIndex;
            loopIndex = 4;
			this.squareIndex = squareIndex;

			JComponentUtilities.setComponentSize( this, 30, 30 );
            addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( dayIndex == currentForecast - 1 )
			{
				return;
			}

            planningData[dayIndex][squareIndex] = toggleMushroom();
            updateForecasts( dayIndex + 1 );
		}

		public void updateImage()
		{
			String currentMushroom = planningData[dayIndex][squareIndex];

			if ( currentMushroom.equals( "__" ) )
			{
                setIcon( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );
			}
			else if ( currentMushroom.equals( currentMushroom.toLowerCase() ) )
			{
                setIcon( JComponentUtilities.getImage( "itemimages/mushsprout.gif" ) );
			}
			else
			{
                setIcon( JComponentUtilities.getImage( MushroomManager.getMushroomImage( currentMushroom ) ) );
			}

			for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
			{
				if ( currentMushroom.equals( MushroomManager.MUSHROOMS[ i ][ 2 ] ) || currentMushroom.equals( MushroomManager.MUSHROOMS[ i ][ 3 ] ) )
				{
                    setToolTipText( (String) MushroomManager.MUSHROOMS[ i ][ 5 ] );
				}
			}
		}

		private String toggleMushroom()
		{
            currentLayout = "";

			// Everything rotates based on what was there
			// when you clicked on the image.

            loopIndex = (loopIndex + 1 ) % 5;

			switch ( loopIndex )
			{
			// If you loop around, then test to see if the
			// old data was a blank.  If it was, then you
			// have already displayed it, so move on to the
			// next element in the cycle.  If not, return a
			// blank, as that's the next element in the cycle.

			case 0:

				if ( originalData[dayIndex][squareIndex].equals( "__" ) )
				{
                    loopIndex = 1;
				}
				else
				{
					return "__";
				}

				// In all other cases, return the next element
				// in the mushroom toggle cycle.

			case 1:
				return "kb";
			case 2:
				return "kn";
			case 3:
				return "sp";
			case 4:
				return originalData[dayIndex][squareIndex];
			}

			return "__";
		}
	}
}
