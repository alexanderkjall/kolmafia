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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTree;

import javax.swing.tree.DefaultTreeModel;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.button.RelayBrowserButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class CustomCombatPanel
	extends JPanel
{
	private JComboBox actionSelect;
	protected JTree combatTree;
	protected JTextArea combatEditor;
	protected DefaultTreeModel combatModel;

	protected JPanel combatCardPanel;
	protected CardLayout combatCards;
	public JComboBox availableScripts;

	private static ImageIcon stealImg, entangleImg;
	private static ImageIcon potionImg, sphereImg, olfactImg, puttyImg;
	private static ImageIcon antidoteImg, restoreImg;
	static
	{
		CustomCombatPanel.stealImg = CustomCombatPanel.getImage( "knobsack.gif" );
		CustomCombatPanel.entangleImg = CustomCombatPanel.getImage( "entnoodles.gif" );
		CustomCombatPanel.potionImg = CustomCombatPanel.getImage( "exclam.gif" );
		CustomCombatPanel.sphereImg = CustomCombatPanel.getImage( "spherecrack.gif" );
		CustomCombatPanel.olfactImg = CustomCombatPanel.getImage( "footprints.gif" );
		CustomCombatPanel.puttyImg = CustomCombatPanel.getImage( "sputtycopy.gif" );
		CustomCombatPanel.antidoteImg = CustomCombatPanel.getImage( "poisoncup.gif" );
		CustomCombatPanel.restoreImg = CustomCombatPanel.getImage( "mp.gif" );
	}

	public CustomCombatPanel()
	{
        combatTree = new JTree();
        combatModel = (DefaultTreeModel) combatTree.getModel();

        combatCards = new CardLayout();
        combatCardPanel = new JPanel( combatCards );

        availableScripts = new CombatComboBox();

        combatCardPanel.add( "tree", new CustomCombatTreePanel() );
        combatCardPanel.add( "editor", new CustomCombatEditorPanel() );

        setLayout( new BorderLayout( 5, 5 ) );

        add( new SpecialActionsPanel(), BorderLayout.NORTH );
        add( combatCardPanel, BorderLayout.CENTER );

        updateFromPreferences();
	}

	public void updateFromPreferences()
	{
		if ( actionSelect != null )
		{
			String battleAction = Preferences.getString( "battleAction" );
			int battleIndex = KoLCharacter.getBattleSkillNames().indexOf( battleAction );
			KoLCharacter.getBattleSkillNames().setSelectedIndex( battleIndex == -1 ? 0 : battleIndex );
		}

		CombatActionManager.updateFromPreferences();
        refreshCombatEditor();
	}

	public void refreshCombatEditor()
	{
		try
		{
			String script = (String) availableScripts.getSelectedItem();
			BufferedReader reader = FileUtilities.getReader( CombatActionManager.getStrategyLookupFile( script ) );

			if ( reader == null )
			{
				return;
			}

			StringBuilder buffer = new StringBuilder();
			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				buffer.append( line );
				buffer.append( '\n' );
			}

			reader.close();
			reader = null;

            combatEditor.setText( buffer.toString() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

        refreshCombatTree();
	}

	/**
	 * Internal class used to handle everything related to displaying custom combat.
	 */

	public void refreshCombatTree()
	{
        combatModel.setRoot( CombatActionManager.getStrategyLookup() );
        combatTree.setRootVisible( false );

		for ( int i = 0; i < combatTree.getRowCount(); ++i )
		{
            combatTree.expandRow( i );
		}
	}

	private static ImageIcon getImage( final String filename )
	{
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + filename );
		return JComponentUtilities.getImage( "itemimages/" + filename );
	}

	private class SpecialActionsPanel
		extends GenericPanel
		implements PreferenceListener
	{
		private final JPanel special;
		private final JPopupMenu specialPopup;

		private final JLabel stealLabel, entangleLabel;
		private final JLabel potionLabel, sphereLabel, olfactLabel, puttyLabel;
		private final JLabel antidoteLabel, restoreLabel;

		private final JCheckBoxMenuItem stealItem, entangleItem;
		private final JCheckBoxMenuItem potionItem, sphereItem, olfactItem, puttyItem;
		private final JCheckBoxMenuItem restoreItem;
		private final JMenu poisonItem;
		private boolean updating = true;

		public SpecialActionsPanel()
		{
			super( new Dimension( 70, -1 ), new Dimension( 200, -1 ) );

            actionSelect = new AutoFilterComboBox( KoLCharacter.getBattleSkillNames(), false );
            actionSelect.addActionListener( new BattleActionListener() );

			JPanel special = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			this.special = special;
			special.setBackground( Color.WHITE );
			special.setBorder( BorderFactory.createLoweredBevelBorder() );

			MouseListener listener = new SpecialPopListener();
			special.addMouseListener( listener );

            stealLabel =
                    label(
					special, listener, CustomCombatPanel.stealImg,
					"Pickpocketing will be tried (if appropriate) with non-CCS actions." );
            entangleLabel =
                    label(
					special, listener, CustomCombatPanel.entangleImg,
					"Entangling Noodles will be cast before non-CCS actions." );
            olfactLabel = label( special, listener, CustomCombatPanel.olfactImg, null );
            puttyLabel = label( special, listener, CustomCombatPanel.puttyImg, null );
            sphereLabel =
                    label(
					special,
					listener,
					CustomCombatPanel.sphereImg,
					"<html>Hidden City spheres will be identified by using them in combat.<br>Requires 'special' action if a CCS is used.</html>" );
            potionLabel =
                    label(
					special,
					listener,
					CustomCombatPanel.potionImg,
					"<html>Dungeons of Doom potions will be identified by using them in combat.<br>Requires 'special' action if a CCS is used.</html>" );
            antidoteLabel = label( special, listener, CustomCombatPanel.antidoteImg, null );
            restoreLabel =
                    label(
					special, listener, CustomCombatPanel.restoreImg, "MP restores will be used in combat if needed." );

            specialPopup = new JPopupMenu( "Special Actions" );
            stealItem = checkbox( specialPopup, listener, "Pickpocket before simple actions" );
            entangleItem = checkbox( specialPopup, listener, "Cast Noodles before simple actions" );
            specialPopup.addSeparator();

            olfactItem = checkbox( specialPopup, listener, "One-time automatic Olfaction..." );
            puttyItem =
                    checkbox( specialPopup, listener, "One-time automatic Spooky Putty/Rain-Doh box/4-d camera..." );
            sphereItem = checkbox( specialPopup, listener, "Identify stone spheres" );
            potionItem = checkbox( specialPopup, listener, "Identify bang potions" );
            specialPopup.addSeparator();

            poisonItem = new JMenu( "Minimum poison level for antidote use" );
			ButtonGroup group = new ButtonGroup();
            poison( poisonItem, group, listener, "No automatic use" );
            poison( poisonItem, group, listener, "Toad In The Hole (-\u00BDHP/round)" );
            poison( poisonItem, group, listener, "Majorly Poisoned (-90%, -11)" );
            poison( poisonItem, group, listener, "Really Quite Poisoned (-70%, -9)" );
            poison( poisonItem, group, listener, "Somewhat Poisoned (-50%, -7)" );
            poison( poisonItem, group, listener, "A Little Bit Poisoned (-30%, -5)" );
            poison( poisonItem, group, listener, "Hardly Poisoned at All (-10%, -3)" );
            specialPopup.add( poisonItem );
            restoreItem = checkbox( specialPopup, listener, "Restore MP in combat" );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Action:  ", actionSelect );
			elements[ 1 ] = new VerifiableElement( "Special:  ", special );

            setContent( elements );
			( (BorderLayout) container.getLayout() ).setHgap( 0 );
			( (BorderLayout) container.getLayout() ).setVgap( 0 );

			PreferenceListenerRegistry.registerListener( "autoSteal", this );
			PreferenceListenerRegistry.registerListener( "autoEntangle", this );
			PreferenceListenerRegistry.registerListener( "autoOlfact", this );
			PreferenceListenerRegistry.registerListener( "autoPutty", this );
			PreferenceListenerRegistry.registerListener( "autoSphereID", this );
			PreferenceListenerRegistry.registerListener( "autoPotionID", this );
			PreferenceListenerRegistry.registerListener( "autoAntidote", this );
			PreferenceListenerRegistry.registerListener( "autoManaRestore", this );
			PreferenceListenerRegistry.registerListener( "(skill)", this );

            update();
		}

		public void update()
		{
            updating = true;

            actionSelect.setSelectedItem( Preferences.getString( "battleAction" ) );

			if ( KoLCharacter.hasSkill( "Entangling Noodles" ) )
			{
                entangleItem.setEnabled( true );
			}
			else
			{
                entangleItem.setEnabled( false );
				Preferences.setBoolean( "autoEntangle", false );
			}

			String text;
			boolean pref;
			pref = Preferences.getBoolean( "autoSteal" );
            stealLabel.setVisible( pref );
            stealItem.setSelected( pref );
			pref = Preferences.getBoolean( "autoEntangle" );
            entangleLabel.setVisible( pref );
            entangleItem.setSelected( pref );
			text = Preferences.getString( "autoOlfact" );
			pref = text.length() > 0;
            olfactLabel.setVisible( pref );
            olfactItem.setSelected( pref );
            olfactLabel.setToolTipText( "<html>Automatic Olfaction or odor extractor use: " + text + "<br>Requires 'special' action if a CCS is used.</html>" );
			text = Preferences.getString( "autoPutty" );
			pref = text.length() > 0;
            puttyLabel.setVisible( pref );
            puttyItem.setSelected( pref );
            puttyLabel.setToolTipText( "<html>Automatic Spooky Putty sheet, Rain-Doh black box, 4-d camera or portable photocopier use: " + text + "<br>Requires 'special' action if a CCS is used.</html>" );
			pref = Preferences.getBoolean( "autoSphereID" );
            sphereLabel.setVisible( pref );
            sphereItem.setSelected( pref );
			pref = Preferences.getBoolean( "autoPotionID" );
            potionLabel.setVisible( pref );
            potionItem.setSelected( pref );
			int antidote = Preferences.getInteger( "autoAntidote" );
            antidoteLabel.setVisible( antidote > 0 );
			if ( antidote >= 0 && antidote < poisonItem.getMenuComponentCount() )
			{
				JRadioButtonMenuItem option = (JRadioButtonMenuItem) poisonItem.getMenuComponent( antidote );
				option.setSelected( true );
                antidoteLabel.setToolTipText( "Anti-anti-antidote will be used in combat if you get " + option.getText() + " or worse." );
			}
			pref = Preferences.getBoolean( "autoManaRestore" );
            restoreLabel.setVisible( pref );
            restoreItem.setSelected( pref );

            updating = false;
		}

		private JLabel label( final JPanel special, final MouseListener listener, final ImageIcon img,
			final String toolTip )
		{
			JLabel rv = new JLabel( img );
			rv.setToolTipText( toolTip );
			rv.addMouseListener( listener );
			special.add( rv );
			return rv;
		}

		private JCheckBoxMenuItem checkbox( final JPopupMenu menu, final Object listener, final String text )
		{
			JCheckBoxMenuItem rv = new JCheckBoxMenuItem( text );
			menu.add( rv );
			rv.addItemListener( (ItemListener) listener );
			return rv;
		}

		private void poison( final JMenu menu, final ButtonGroup group, final Object listener, final String text )
		{
			JRadioButtonMenuItem rb = new JRadioButtonMenuItem( text );
			menu.add( rb );
			group.add( rb );
			rb.addItemListener( (ItemListener) listener );
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
		public void addStatusLabel()
		{
		}

		private class BattleActionListener
			implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				// Don't set preferences from widgets when we
				// are in the middle of loading widgets from
				// preferences.
				if ( updating )
				{
					return;
				}

				String value = (String) actionSelect.getSelectedItem();

				if ( value != null )
				{
					Preferences.setString( "battleAction", value );
				}
			}
		}

		private class SpecialPopListener
			extends MouseAdapter
			implements ItemListener
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
                specialPopup.show( special, 0, 32 );
			}

			public void itemStateChanged( final ItemEvent e )
			{
				// Don't set preferences from widgets when we
				// are in the middle of loading widgets from
				// preferences.
				if ( updating )
				{
					return;
				}

				boolean state = e.getStateChange() == ItemEvent.SELECTED;
				JMenuItem source = (JMenuItem) e.getItemSelectable();
				if ( source == stealItem )
				{
					Preferences.setBoolean( "autoSteal", state );
				}
				else if ( source == entangleItem )
				{
					Preferences.setBoolean( "autoEntangle", state );
				}
				else if ( source == olfactItem )
				{
					if ( state == !Preferences.getString( "autoOlfact" ).equals( "" ) )
					{ // pref already set externally, don't prompt
						return;
					}
					String option =
						!state ? null : InputFieldUtilities.input(
							"Use Transcendent Olfaction or odor extractor when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
							"goals" );

					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "olfact", option == null ? "none" : option );
				}
				else if ( source == puttyItem )
				{
					if ( state == !Preferences.getString( "autoPutty" ).equals( "" ) )
					{ // pref already set externally, don't prompt
						return;
					}
					String option =
						!state ? null : InputFieldUtilities.input(
							"Use Spooky Putty sheet, Rain-Doh black box, 4-d camera or portable photocopier when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
							"goals abort" );

					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "putty", option == null ? "none" : option );
				}
				else if ( source == sphereItem )
				{
					Preferences.setBoolean( "autoSphereID", state );
				}
				else if ( source == potionItem )
				{
					Preferences.setBoolean( "autoPotionID", state );
				}
				else if ( source == restoreItem )
				{
					Preferences.setBoolean( "autoManaRestore", state );
				}
				else if ( source instanceof JRadioButtonMenuItem )
				{
					Preferences.setInteger( "autoAntidote", Arrays.asList(
                            poisonItem.getMenuComponents() ).indexOf( source ) );
				}
			}
		}
	}

	public class CombatComboBox
		extends JComboBox
		implements ActionListener, PreferenceListener
	{
		public CombatComboBox()
		{
			super( CombatActionManager.getAvailableLookups() );
            addActionListener( this );
			PreferenceListenerRegistry.registerListener( "customCombatScript", this );
		}

		public void update()
		{
            combatCards.show( combatCardPanel, "tree" );
            setSelectedItem( Preferences.getString( "customCombatScript" ) );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			String script = (String) getSelectedItem();
			if ( script != null )
			{
				CombatActionManager.loadStrategyLookup( script );
                refreshCombatTree();
			}
		}
	}

	private class CustomCombatEditorPanel
		extends ScrollablePanel
	{
		public CustomCombatEditorPanel()
		{
			super( "Editor", "save", "cancel", new JTextArea() );
            combatEditor = (JTextArea) scrollComponent;
            combatEditor.setFont( KoLConstants.DEFAULT_FONT );
            refreshCombatTree();

            eastPanel.add( new RelayBrowserButton( "help", "http://kolmafia.sourceforge.net/combat.html" ), BorderLayout.SOUTH );
		}

		@Override
		public void actionConfirmed()
		{
			String script = (String) availableScripts.getSelectedItem();
			String saveText = combatEditor.getText();

			File location = CombatActionManager.getStrategyLookupFile( script );
			PrintStream writer = LogStream.openStream( location, true );

			writer.print( saveText );
			writer.close();
			writer = null;

			KoLCharacter.battleSkillNames.setSelectedItem( "custom combat script" );
			Preferences.setString( "battleAction", "custom combat script" );

			// After storing all the data on disk, go ahead
			// and reload the data inside of the tree.

			CombatActionManager.loadStrategyLookup( script );
			CombatActionManager.saveStrategyLookup( script );

            refreshCombatTree();
            combatCards.show( combatCardPanel, "tree" );
		}

		@Override
		public void actionCancelled()
		{
            refreshCombatEditor();
            combatCards.show( combatCardPanel, "tree" );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	public class CustomCombatTreePanel
		extends ScrollablePanel
	{
		public CustomCombatTreePanel()
		{
			super( "", "edit", "help", combatTree );
            combatTree.setVisibleRowCount( 8 );

            centerPanel.add( availableScripts, BorderLayout.NORTH );

			JPanel extraButtons = new JPanel( new GridLayout( 2, 1, 5, 5 ) );

			extraButtons.add( new ThreadedButton( "new", new NewScriptRunnable() ) );
			extraButtons.add( new ThreadedButton( "copy", new CopyScriptRunnable() ) );

			JPanel buttonHolder = new JPanel( new BorderLayout() );
			buttonHolder.add( extraButtons, BorderLayout.NORTH );

            eastPanel.add( buttonHolder, BorderLayout.SOUTH );
		}

		@Override
		public void actionConfirmed()
		{
            refreshCombatEditor();
            combatCards.show( combatCardPanel, "editor" );
		}

		@Override
		public void actionCancelled()
		{
			RelayLoader.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}

		public class NewScriptRunnable
			implements Runnable
		{
			public void run()
			{
				String name = InputFieldUtilities.input( "Give your combat script a name!" );
				if ( name == null || name.equals( "" ) || name.equals( "default" ) )
				{
					return;
				}

				CombatActionManager.loadStrategyLookup( name );
                refreshCombatTree();
			}
		}

		public class CopyScriptRunnable
			implements Runnable
		{
			public void run()
			{
				String name = InputFieldUtilities.input( "Make a copy of current script called:" );
				if ( name == null || name.equals( "" ) || name.equals( "default" ) )
				{
					return;
				}

				CombatActionManager.copyStrategyLookup( name );
				CombatActionManager.loadStrategyLookup( name );
                refreshCombatTree();
			}
		}
	}
}
