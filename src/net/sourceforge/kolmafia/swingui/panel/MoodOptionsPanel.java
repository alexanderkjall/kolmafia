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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.moods.Mood;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.MoodTrigger;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MoodOptionsPanel
	extends JPanel
{
	protected JList moodList;

	public MoodOptionsPanel()
	{
		super( new BorderLayout() );

        add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		AddTriggerPanel triggers = new AddTriggerPanel();
        moodList.addListSelectionListener( triggers );
        add( triggers, BorderLayout.NORTH );
	}

	private class MoodTriggerListPanel
		extends ScrollablePanel
	{
		public JComboBox availableMoods;

		public MoodTriggerListPanel()
		{
			super( "", new ShowDescriptionList( MoodManager.getTriggers() ) );

            availableMoods = new MoodComboBox();

            centerPanel.add( availableMoods, BorderLayout.NORTH );
            moodList = (JList) scrollComponent;

			JPanel extraButtons = new JPanel( new GridLayout( 4, 1, 5, 5 ) );

			extraButtons.add( new ThreadedButton( "new list", new NewMoodRunnable() ) );
			extraButtons.add( new ThreadedButton( "delete list", new DeleteMoodRunnable() ) );
			extraButtons.add( new ThreadedButton( "copy list", new CopyMoodRunnable() ) );
			extraButtons.add( new ThreadedButton( "execute", new ExecuteRunnable() ) );

			JPanel buttonHolder = new JPanel( new BorderLayout() );
			buttonHolder.add( extraButtons, BorderLayout.NORTH );

            actualPanel.add( buttonHolder, BorderLayout.EAST );
		}

		@Override
		public void actionConfirmed()
		{
		}

		@Override
		public void actionCancelled()
		{
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}

		private class MoodComboBox
			extends JComboBox
		{
			public MoodComboBox()
			{
				super( MoodManager.getAvailableMoods() );

				MoodManager.updateFromPreferences();

                addActionListener( new MoodComboBoxListener() );
			}

			public class MoodComboBoxListener
				implements ActionListener
			{
				public void actionPerformed( final ActionEvent e )
				{
					Mood mood = (Mood) getSelectedItem();
					if ( mood != null )
					{
						MoodManager.setMood( mood.toString() );
					}
				}
			}
		}

		private class NewMoodRunnable
			implements Runnable
		{
			public void run()
			{
				String name = InputFieldUtilities.input( "Give your list a name!" );
				if ( name == null )
				{
					return;
				}

				MoodManager.setMood( name );
				MoodManager.saveSettings();
			}
		}

		private class DeleteMoodRunnable
			implements Runnable
		{
			public void run()
			{
				MoodManager.deleteCurrentMood();
				MoodManager.saveSettings();
			}
		}

		private class CopyMoodRunnable
			implements Runnable
		{
			public void run()
			{
				String moodName = InputFieldUtilities.input( "Make a copy of current mood list called:" );
				if ( moodName == null )
				{
					return;
				}

				if ( moodName.equals( "default" ) )
				{
					return;
				}

				MoodManager.copyTriggers( moodName );
				MoodManager.setMood( moodName );
				MoodManager.saveSettings();
			}
		}

		private class ExecuteRunnable
			implements Runnable
		{
			public void run()
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mood execute" );
			}
		}
	}

	public class AddTriggerPanel
		extends GenericPanel
		implements ListSelectionListener
	{
		public LockableListModel EMPTY_MODEL = new LockableListModel();
		public LockableListModel EFFECT_MODEL = new LockableListModel();

		public TypeComboBox typeSelect;
		public ValueComboBox valueSelect;
		public JTextField commandField;

		public AddTriggerPanel()
		{
			super( "add entry", "auto-fill" );

            typeSelect = new TypeComboBox();

			Object[] names = EffectDatabase.values().toArray();

			for ( int i = 0; i < names.length; ++i )
			{
                EFFECT_MODEL.add( names[i].toString() );
			}

            EFFECT_MODEL.sort();

            valueSelect = new ValueComboBox();
            commandField = new JTextField();

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "Trigger On: ", typeSelect );
			elements[ 1 ] = new VerifiableElement( "Check For: ", valueSelect );
			elements[ 2 ] = new VerifiableElement( "Command: ", commandField );

            setContent( elements );
		}

		public void valueChanged( final ListSelectionEvent e )
		{
			Object selected = moodList.getSelectedValue();
			if ( selected == null )
			{
				return;
			}

			MoodTrigger node = (MoodTrigger) selected;
			String type = node.getType();

			// Update the selected type

			if ( type.equals( "lose_effect" ) )
			{
                typeSelect.setSelectedIndex( 0 );
			}
			else if ( type.equals( "gain_effect" ) )
			{
                typeSelect.setSelectedIndex( 1 );
			}
			else if ( type.equals( "unconditional" ) )
			{
                typeSelect.setSelectedIndex( 2 );
			}

			// Update the selected effect

            valueSelect.setSelectedItem( node.getName() );
            commandField.setText( node.getAction() );
		}

		@Override
		public void actionConfirmed()
		{
			String currentMood = Preferences.getString( "currentMood" );
			if ( currentMood.equals( "apathetic" ) )
			{
				InputFieldUtilities.alert( "You cannot add triggers to an apathetic mood." );
				return;
			}

			MoodManager.addTrigger(
                    typeSelect.getSelectedType(), (String) valueSelect.getSelectedItem(),
                    commandField.getText() );
			MoodManager.saveSettings();
		}

		@Override
		public void actionCancelled()
		{
			String[] autoFillTypes =
				new String[] { "minimal set (current active buffs)", "maximal set (all castable buffs)" };
			String desiredType =
				(String) InputFieldUtilities.input( "Which kind of buff set would you like to use?", autoFillTypes );

			if ( desiredType == null )
			{
				return;
			}

			if ( desiredType.equals( autoFillTypes[ 0 ] ) )
			{
				MoodManager.minimalSet();
			}
			else
			{
				MoodManager.maximalSet();
			}

			MoodManager.saveSettings();
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}

		@Override
		public void addStatusLabel()
		{
		}

		private class ValueComboBox
			extends AutoFilterComboBox
		{
			public ValueComboBox()
			{
				super( EFFECT_MODEL, false );
			}

			@Override
			public void setSelectedItem( final Object anObject )
			{
                commandField.setText( MoodManager.getDefaultAction(
                        typeSelect.getSelectedType(), (String) anObject ) );
				super.setSelectedItem( anObject );
			}
		}

		private class TypeComboBox
			extends JComboBox
		{
			public TypeComboBox()
			{
                addItem( "When an effect is lost" );
                addItem( "When an effect is gained" );
                addItem( "Unconditional trigger" );

                addActionListener( new TypeComboBoxListener() );
			}

			public String getSelectedType()
			{
				switch ( getSelectedIndex() )
				{
				case 0:
					return "lose_effect";
				case 1:
					return "gain_effect";
				case 2:
					return "unconditional";
				default:
					return null;
				}
			}

			private class TypeComboBoxListener
				implements ActionListener
			{
				public void actionPerformed( final ActionEvent e )
				{
                    valueSelect.setModel( getSelectedIndex() == 2 ? EMPTY_MODEL : EFFECT_MODEL );
				}
			}
		}
	}
}
