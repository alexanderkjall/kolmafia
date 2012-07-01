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
import java.awt.Component;

import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class CardLayoutSelectorPanel
	extends JPanel
{
	private final String indexPreference;

	public final LockableListModel panelNames = new LockableListModel();
	private final JList panelList = new JList( panelNames );
	public final ArrayList panels = new ArrayList();
	private final CardLayout panelCards = new CardLayout();
	private final JPanel mainPanel = new JPanel( panelCards );
	protected ChangeListener changeListener = null;

	public CardLayoutSelectorPanel()
	{
		this( null );
	}

	public CardLayoutSelectorPanel( final String indexPreference )
	{
		this( indexPreference, "ABCDEFGHIJKLM" );
	}

	public CardLayoutSelectorPanel( final String indexPreference, final String prototype )
	{
		super( new BorderLayout() );

		this.indexPreference = indexPreference;

        panelList.addListSelectionListener( new CardSwitchListener() );
        panelList.setPrototypeCellValue( prototype );
        panelList.setCellRenderer( new OptionRenderer() );

		JPanel listHolder = new JPanel( new CardLayout( 10, 10 ) );
		listHolder.add( new GenericScrollPane( panelList ), "" );

        add( listHolder, BorderLayout.WEST );
        add( mainPanel, BorderLayout.CENTER );
	}

	public void addChangeListener( final ChangeListener changeListener )
	{
		this.changeListener = changeListener;
	}

	public void setSelectedIndex( final int selectedIndex )
	{
        panelList.setSelectedIndex( selectedIndex );
	}

	public void addPanel( final String name, final JComponent panel )
	{
        addPanel( name, panel, false );
	}

	public void addPanel( final String name, JComponent panel, boolean addScrollPane )
	{
        panelNames.add( name );

		if ( addScrollPane )
		{
			panel = new GenericScrollPane( panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
			JComponentUtilities.setComponentSize( panel, 500, 400 );
		}

        panels.add( panel );
        mainPanel.add( panel, String.valueOf( panelNames.size() ) );
	}

	public void addSeparator()
	{
		JPanel separator = new JPanel();
		separator.setOpaque( false );
		separator.setLayout( new BoxLayout( separator, BoxLayout.Y_AXIS ) );

		separator.add( Box.createVerticalGlue() );
		separator.add( new JSeparator() );
        panelNames.add( separator );
        panels.add( separator );
	}

	public void addCategory( final String name )
	{
		JPanel category = new JPanel();
		category.setOpaque( false );
		category.setLayout( new BoxLayout( category, BoxLayout.Y_AXIS ) );
		category.add( new JLabel( name ) );
        panelNames.add( category );
        panels.add( category );
	}

	public JComponent currentPanel()
	{
		int cardIndex = panelList.getSelectedIndex();
		return (JComponent) panels.get( cardIndex );
	}

	private class CardSwitchListener
		implements ListSelectionListener
	{
		public void valueChanged( final ListSelectionEvent e )
		{
			int cardIndex = panelList.getSelectedIndex();

			if ( panelNames.get( cardIndex ) instanceof JComponent )
			{
				return;
			}

			if ( indexPreference != null )
			{
				Preferences.setInteger( indexPreference, cardIndex );
			}

            panelCards.show(
                    mainPanel, String.valueOf( cardIndex + 1 ) );

			if ( changeListener != null )
			{
                changeListener.stateChanged( new ChangeEvent( CardLayoutSelectorPanel.this ) );
			}
		}
	}

	private class OptionRenderer
		extends DefaultListCellRenderer
	{
		public OptionRenderer()
		{
            setOpaque( true );
		}

		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			return value instanceof JComponent ? (Component) value : super.getListCellRendererComponent(
				list, value, index, isSelected, cellHasFocus );
		}
	}
}
