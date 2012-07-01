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

import com.informit.guides.JDnDList;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.panel.AddCustomDeedsPanel;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.OptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.CollapsibleTextArea;
import net.sourceforge.kolmafia.swingui.widget.ColorChooser;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayServer;

import tab.CloseTabPaneEnhancedUI;

public class OptionsFrame
	extends GenericFrame
{
	public OptionsFrame()
	{
		super( "Preferences" );

		CardLayoutSelectorPanel selectorPanel = new CardLayoutSelectorPanel( null, "mmmmmmmmmmmm" );

		selectorPanel.addPanel( "General", new GeneralOptionsPanel(), true );
		selectorPanel.addPanel( " - Item Acquisition", new ItemOptionsPanel(), true );
		selectorPanel.addPanel( " - Session Logs", new SessionLogOptionsPanel(), true );
		selectorPanel.addPanel( " - Extra Debugging", new DebugOptionsPanel(), true );

		JPanel programsPanel = new JPanel();
		programsPanel.setLayout( new BoxLayout( programsPanel, BoxLayout.Y_AXIS ) );
		programsPanel.add( new EditorPanel() );
		programsPanel.add( new BrowserPanel() );
		programsPanel.add( Box.createVerticalGlue() );
		selectorPanel.addPanel( " - External Programs", programsPanel, true );

		selectorPanel.addPanel( "Look & Feel", new UserInterfacePanel(), true );
		selectorPanel.addPanel( " - Main Interface", new StartupFramesPanel(), true );
		selectorPanel.addPanel( " - Chat Options", new ChatOptionsPanel(), true );
		selectorPanel.addPanel( " - Relay Browser", new RelayOptionsPanel(), true );
		selectorPanel.addPanel( " - Text Colors", new ColorOptionsPanel(), true );

		selectorPanel.addPanel( "Automation", new ScriptPanel(), true );
		selectorPanel.addPanel( " - In Ronin", new BreakfastPanel( "Hardcore" ), true );
		selectorPanel.addPanel( " - After Ronin", new BreakfastPanel( "Softcore" ), true );

		JPanel customDeedPanel = new JPanel();
		customDeedPanel.setLayout( new BoxLayout( customDeedPanel, BoxLayout.Y_AXIS ) );
		customDeedPanel.add( new CustomizeDailyDeedsPanel( "Message" ) );
		customDeedPanel.add( new CustomizeDailyDeedsPanel() );
		selectorPanel.addPanel( " - Daily Deeds", customDeedPanel, true );

		selectorPanel.addPanel( "Script Buttons", new ScriptButtonPanel(), true );
		selectorPanel.addPanel( "Bookmarks", new BookmarkManagePanel(), true );

        setCenterComponent( selectorPanel );

		if ( !Preferences.getBoolean( "customizedTabs" ) )
		{
			selectorPanel.setSelectedIndex( 5 );
		}
		else if ( RelayServer.isRunning() )
		{
			selectorPanel.setSelectedIndex( 8 );
		}
		else
		{
			selectorPanel.setSelectedIndex( 0 );
		}
	}

	private class SessionLogOptionsPanel
		extends OptionsPanel
	{
		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public SessionLogOptionsPanel()
		{
			super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );

			String[][] options =
			{
				{ "logStatusOnLogin", "Session log records your player's state on login" },
				{ "logReverseOrder", "Log adventures left instead of adventures used" },
				{},
				{ "logBattleAction", "Session log records attacks for each round" },
				{ "logFamiliarActions", "Session log records actions made by familiars" },
				{ "logMonsterHealth", "Session log records monster health changes" },
				{},
				{ "logGainMessages", "Session log records HP/MP/meat changes" },
				{ "logStatGains", "Session log records stat gains" },
				{ "logAcquiredItems", "Session log records items acquired" },
				{ "logStatusEffects", "Session log records status effects gained" }
			};

            setOptions( options );
		}
	}

	private class RelayOptionsPanel
		extends OptionsPanel
	{
		private JLabel colorChanger;

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( new Dimension( 16, 16 ), new Dimension( 370, 16 ) );

			String[][] options =
			{
				{ "relayAllowRemoteAccess", "Allow network devices to access relay browser (requires restart)" },
				{ "relayUsesCachedImages", "Cache KoL images to conserve bandwidth (dialup)" },
				{ "relayOverridesImages", "Override certain KoL images" },
				{},
				{ "relayAddsWikiLinks", "Check wiki for item descriptions (fails for unknowns)" },
				{ "relayViewsCustomItems", "View items registered with OneTonTomato's Kilt script" },
				{ "relayAddsQuickScripts", "Add quick script links to menu bar (see Links tab)" },
				{},
				{ "relayAddsRestoreLinks", "Add HP/MP restore links to left side pane" },
				{ "relayAddsUpArrowLinks", "Add buff maintenance links to left side pane" },
				{ "relayTextualizesEffects", "Textualize effect links in left side pane" },
				{ "relayAddsDiscoHelper", "Add Disco Bandit helper to fights" },
				{ "macroLens", "Show Combat Macro helper during fights" },
				{},
				{ "relayRunsAfterAdventureScript", "Run afterAdventureScript after manual adventures" },
				{ "relayRunsBeforeBattleScript", "Run betweenBattleScript before manual adventures" },
				{ "relayMaintainsEffects", "Run moods before manual adventures" },
				{ "relayMaintainsHealth", "Maintain health before manual adventures" },
				{ "relayMaintainsMana", "Maintain mana before manual adventures" },
				{ "relayWarnOnRecoverFailure", "Show a warning if any of the above maintenances fails" },
				{ "relayRunsBeforePVPScript", "Run beforePVPScript before manual PVP attacks" },
				{},
				{ "relayUsesIntegratedChat", "Integrate chat and relay browser gCLI interfaces" },
				{ "relayFormatsChatText", "Reformat incoming chat HTML to conform to web standards" },
				{ "relayAddsGraphicalCLI", "Add command-line interface to right side pane" },
				{},
				{ "relayAddsUseLinks", "Add [use] links when receiving items" },
				{ "relayUsesInlineLinks", "Force results to reload inline for [use] links" },
				{ "relayHidesJunkMallItems", "Hide junk and overpriced items in PC stores" },
				{ "relayTrimsZapList", "Trim zap list to show only known zappable items" },
				{},
				{ "relayAddsCustomCombat", "Add custom buttons to the top of fight pages" },
				{ "arcadeGameHints", "Provide hints for Arcade games" },
				{ "relayShowSpoilers", "Show blatant spoilers for choices and puzzles" },
			};

            setOptions( options );
		}

		@Override
		public void setContent( VerifiableElement[] elements )
		{
			VerifiableElement[] newElements = new VerifiableElement[ elements.length + 1 ];

			System.arraycopy( elements, 0, newElements, 0, elements.length );

            colorChanger = new ColorChooser( "defaultBorderColor" );

			newElements[ elements.length ] = new VerifiableElement(
				"Change the color for tables in the browser interface", SwingConstants.LEFT, colorChanger );

			super.setContent( newElements );
		}

		@Override
		public void actionConfirmed()
		{
			boolean overrideImages = Preferences.getBoolean( "relayOverridesImages" );

			super.actionConfirmed();

			if ( overrideImages != Preferences.getBoolean( "relayOverridesImages" ) )
			{
				RelayRequest.flushOverrideImages();
			}
		}

		@Override
		public void actionCancelled()
		{
			String color = Preferences.getString( "defaultBorderColor" );

			if ( color.equals( "blue" ) )
			{
                colorChanger.setBackground( Color.blue );
			}
			else
			{
                colorChanger.setBackground( DataUtilities.toColor( color ) );
			}

			super.actionCancelled();
		}
	}

	private class GeneralOptionsPanel
		extends OptionsPanel
	{
		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public GeneralOptionsPanel()
		{
			super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );

			String[][] options =
			{
				{ "showAllRequests", "Show all requests in a mini-browser window" },
				{ "showExceptionalRequests", "Automatically load 'click here to load in relay browser' in mini-browser" },

				{},

				{ "useZoneComboBox", "Use zone selection instead of adventure name filter" },
				{ "cacheMallSearches", "Cache mall search terms in mall search interface" },
				{ "saveSettingsOnSet", "Save options to disk whenever they change" },

				{},

				{ "removeMalignantEffects", "Auto-remove malignant status effects" },
				{ "switchEquipmentForBuffs", "Allow equipment changing when casting buffs" },
				{ "allowNonMoodBurning", "Cast buffs not defined in moods during buff balancing" },
				{ "allowSummonBurning", "Cast summoning skills during buff balancing" },

				{},

				{ "requireSewerTestItems", "Require appropriate test items to adventure in clan sewers " },
				{ "mmgDisabled", "Disable access to the MMG in the Relay Browser" },
				{ "mmgAutoConfirmBets", "Auto-confirm bets in the MMG" },

				{},

				{ "sharePriceData", "Share recent Mall price data with other users" },
			};

            setOptions( options );
		}
	}

	private class ItemOptionsPanel
		extends OptionsPanel
	{
		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public ItemOptionsPanel()
		{
			super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );

			String[][] options =
			{
				{ "allowNegativeTally", "Allow item counts in session results to go negative" },

				{},

				{ "cloverProtectActive", "Protect against accidental ten-leaf clover usage" },
				{ "mementoListActive", "Prevent accidental destruction of 'memento' items" },

				{},

				{ "autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed", "yes" },
				{ "autoSatisfyWithStorage", "If you are out of Ronin, pull items from storage whenever needed", "yes" },
				{ "autoSatisfyWithCoinmasters", "Buy items with tokens at coin masters whenever needed", "yes" },
				{ "autoSatisfyWithMall", "Buy items from the mall whenever needed" },
				{ "autoSatisfyWithCloset", "Take items from the closet whenever needed", "yes" },
				{ "autoSatisfyWithStash", "Take items from the clan stash whenever needed" },
			};

            setOptions( options );
		}
	}

	private class DebugOptionsPanel
		extends OptionsPanel
	{
		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a place for the users to select their desired
		 * server and for them to modify any applicable proxy settings.
		 */

		public DebugOptionsPanel()
		{
			super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );

			String[][] options =
			{
				{ "useLastUserAgent", "Use last browser's userAgent" },
				{ "logBrowserInteractions", "Verbosely log communication between KoLmafia and browser" },
				{ "logCleanedHTML", "Log cleaned HTML tree of fight pages" },
				{ "logDecoratedResponses", "Log decorated responses in debug log" },
				{ "logReadableHTML", "Include line breaks in logged HTML" },
			};

            setOptions( options );
		}
	}

	private abstract class ShiftableOrderPanel
		extends ScrollablePanel
		implements ListDataListener
	{
		public LockableListModel list;
		public JList elementList;

		public ShiftableOrderPanel( final String title, final LockableListModel list )
		{
			super( title, "move up", "move down", new JList( list ) );

            elementList = (JList) scrollComponent;
            elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

			this.list = list;
			list.addListDataListener( this );
		}

		@Override
		public void dispose()
		{
            list.removeListDataListener( this );
			super.dispose();
		}

		@Override
		public final void actionConfirmed()
		{
			int index = elementList.getSelectedIndex();
			if ( index == -1 )
			{
				return;
			}

			Comparable<?> value = list.remove( index );
            list.add( index - 1, value );
            elementList.setSelectedIndex( index - 1 );
		}

		@Override
		public final void actionCancelled()
		{
			int index = elementList.getSelectedIndex();
			if ( index == -1 )
			{
				return;
			}

			Comparable<?> value = list.remove( index );
            list.add( index + 1, value );
            elementList.setSelectedIndex( index + 1 );
		}

		public void intervalAdded( final ListDataEvent e )
		{
            saveSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
            saveSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
            saveSettings();
		}

		public abstract void saveSettings();
	}

	private class ScriptButtonPanel
		extends ShiftableOrderPanel
	{
		public ScriptButtonPanel()
		{
			super( "gCLI Toolbar Buttons", new LockableListModel() );
			String[] scriptList = Preferences.getString( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
			{
                list.add( scriptList[i] );
			}

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new ThreadedButton( "script file", new AddScriptRunnable() ), BorderLayout.NORTH );
			extraButtons.add( new ThreadedButton( "cli command", new AddCommandRunnable() ), BorderLayout.CENTER );
			extraButtons.add( new ThreadedButton( "delete", new DeleteListingRunnable() ), BorderLayout.SOUTH );
            buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		private class AddScriptRunnable
			implements Runnable
		{
			public void run()
			{
				try
				{
					String rootPath = KoLConstants.SCRIPT_LOCATION.getCanonicalPath();

					JFileChooser chooser = new JFileChooser( rootPath );
					int returnVal = chooser.showOpenDialog( null );

					if ( chooser.getSelectedFile() == null )
					{
						return;
					}

					if ( returnVal == JFileChooser.APPROVE_OPTION )
					{
						String scriptPath = chooser.getSelectedFile().getCanonicalPath();
						if ( scriptPath.startsWith( rootPath ) )
						{
							scriptPath = scriptPath.substring( rootPath.length() + 1 );
						}

                        list.add( "call " + scriptPath );
					}
				}
				catch ( IOException e )
				{
				}

                saveSettings();
			}
		}

		private class AddCommandRunnable
			implements Runnable
		{
			public void run()
			{
				String currentValue = InputFieldUtilities.input( "Enter the desired CLI Command" );
				if ( currentValue != null && currentValue.length() != 0 )
				{
                    list.add( currentValue );
				}

                saveSettings();
			}
		}

		private class DeleteListingRunnable
			implements Runnable
		{
			public void run()
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
				{
					return;
				}

                list.remove( index );
                saveSettings();
			}
		}

		@Override
		public void saveSettings()
		{
			StringBuilder settingString = new StringBuilder();
			if ( list.size() != 0 )
			{
				settingString.append( (String) list.getElementAt( 0 ) );
			}

			for ( int i = 1; i < list.getSize(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) list.getElementAt( i ) );
			}

			Preferences.setString( "scriptList", settingString.toString() );
		}
	}

	/**
	 * Panel used for handling chat-related options and preferences, including font size, window management and maybe,
	 * eventually, coloring options for contacts.
	 */

	private class ChatOptionsPanel
		extends OptionsPanel
	{
		private ButtonGroup fontSizeGroup;
		private JRadioButton[] fontSizes;
		private JLabel innerGradient, outerGradient;

		public ChatOptionsPanel()
		{
			super( new Dimension( 30, 16 ), new Dimension( 470, 16 ) );

			String[][] options =
			{
				{ "useTabbedChatFrame", "Use tabbed, rather than multi-window, chat" },
				{ "useShinyTabbedChat", "Use shiny closeable tabs when using tabbed chat" },
				{ "addChatCommandLine", "Add a graphical CLI to tabbed chat" },
				{},
				{ "useContactsFrame", "Use a popup window for /friends and /who" },
				{ "chatLinksUseRelay", "Use the relay browser when clicking on chat links" },
				{ "useChatToolbar", "Add a toolbar to chat windows for special commands" },
				{},
				{ "mergeHobopolisChat", "Merge clan dungeon channel displays into /clan" },
				{ "greenScreenProtection", "Ignore event messages in KoLmafia chat" },
				{ "logChatMessages", "Log chats when using KoLmafia (requires restart)" },
			};

            setOptions( options );
		}

		@Override
		public void setContent( VerifiableElement[] elements )
		{
            fontSizeGroup = new ButtonGroup();
            fontSizes = new JRadioButton[ 3 ];
			for ( int i = 0; i < 3; ++i )
			{
                fontSizes[ i ] = new JRadioButton();
                fontSizeGroup.add( fontSizes[i] );
			}

			VerifiableElement[] newElements = new VerifiableElement[ elements.length + 7 ];

			newElements[ 0 ] = new VerifiableElement(
				"Use small fonts in hypertext displays", SwingConstants.LEFT, fontSizes[ 0 ] );
			newElements[ 1 ] = new VerifiableElement(
				"Use medium fonts in hypertext displays", SwingConstants.LEFT, fontSizes[ 1 ] );
			newElements[ 2 ] = new VerifiableElement(
				"Use large fonts in hypertext displays", SwingConstants.LEFT, fontSizes[ 2 ] );

			newElements[ 3 ] = new VerifiableElement();

			System.arraycopy( elements, 0, newElements, 4, elements.length );

			int tabCount = elements.length + 4;

			newElements[ tabCount++ ] = new VerifiableElement();

            outerGradient = new TabColorChanger( "outerChatColor" );
			newElements[ tabCount++ ] = new VerifiableElement(
				"Change the outer portion of highlighted tab gradient", SwingConstants.LEFT, outerGradient );

            innerGradient = new TabColorChanger( "innerChatColor" );
			newElements[ tabCount++ ] = new VerifiableElement(
				"Change the inner portion of highlighted tab gradient", SwingConstants.LEFT, innerGradient );

			super.setContent( newElements );
		}

		@Override
		public void actionConfirmed()
		{
			super.actionConfirmed();

			if ( fontSizes[ 0 ].isSelected() )
			{
				Preferences.setString( "chatFontSize", "small" );
			}
			else if ( fontSizes[ 1 ].isSelected() )
			{
				Preferences.setString( "chatFontSize", "medium" );
			}
			else if ( fontSizes[ 2 ].isSelected() )
			{
				Preferences.setString( "chatFontSize", "large" );
			}

			KoLConstants.commandBuffer.append( null );
		}

		@Override
		public void actionCancelled()
		{
			super.actionCancelled();

            innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedA );
            outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.notifiedB );

			String fontSize = Preferences.getString( "chatFontSize" );
            fontSizes[ fontSize.equals( "large" ) ? 2 : fontSize.equals( "medium" ) ? 1 : 0 ].setSelected( true );
		}

		private class TabColorChanger
			extends ColorChooser
		{
			public TabColorChanger( final String property )
			{
				super( property );
			}

			@Override
			public void applyChanges()
			{
				if ( property.equals( "innerChatColor" ) )
				{
					CloseTabPaneEnhancedUI.notifiedA = innerGradient.getBackground();
				}
				else
				{
					CloseTabPaneEnhancedUI.notifiedB = outerGradient.getBackground();
				}
			}
		}
	}

	/**
	 * A special panel which generates a list of bookmarks which can subsequently be managed.
	 */

	private class BookmarkManagePanel
		extends ShiftableOrderPanel
	{
		public BookmarkManagePanel()
		{
			super( "Configure Bookmarks", KoLConstants.bookmarks );

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( new ThreadedButton( "add", new AddBookmarkRunnable() ), BorderLayout.NORTH );
			extraButtons.add( new ThreadedButton( "rename", new RenameBookmarkRunnable() ), BorderLayout.CENTER );
			extraButtons.add( new ThreadedButton( "delete", new DeleteBookmarkRunnable() ), BorderLayout.SOUTH );
            buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		@Override
		public void saveSettings()
		{
			GenericFrame.saveBookmarks();
		}

		private class AddBookmarkRunnable
			implements Runnable
		{
			public void run()
			{
				String newName = InputFieldUtilities.input( "Add a bookmark!", "http://www.google.com/" );

				if ( newName == null )
				{
					return;
				}

				KoLConstants.bookmarks.add( "New bookmark " + ( KoLConstants.bookmarks.size() + 1 ) + "|" + newName + "|" + String.valueOf( newName.contains( "pwd" ) ) );
			}
		}

		private class RenameBookmarkRunnable
			implements Runnable
		{
			public void run()
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
				{
					return;
				}

				String currentItem = (String) elementList.getSelectedValue();
				if ( currentItem == null )
				{
					return;
				}

				String[] bookmarkData = currentItem.split( "\\|" );

				String name = bookmarkData[ 0 ];
				String location = bookmarkData[ 1 ];
				String pwdhash = bookmarkData[ 2 ];

				String newName = InputFieldUtilities.input( "Rename your bookmark?", name );

				if ( newName == null )
				{
					return;
				}

				KoLConstants.bookmarks.remove( index );
				KoLConstants.bookmarks.add( newName + "|" + location + "|" + pwdhash );
			}
		}

		private class DeleteBookmarkRunnable
			implements Runnable
		{
			public void run()
			{
				int index = elementList.getSelectedIndex();
				if ( index == -1 )
				{
					return;
				}

				KoLConstants.bookmarks.remove( index );
			}
		}
	}

	protected class StartupFramesPanel
		extends GenericPanel
		implements ListDataListener
	{
		private boolean isRefreshing = false;

		private final LockableListModel completeList = new LockableListModel();
		private final LockableListModel startupList = new LockableListModel();
		private final LockableListModel desktopList = new LockableListModel();

		public StartupFramesPanel()
		{
			super();
            setContent( null );

			for ( int i = 0; i < KoLConstants.FRAME_NAMES.length; ++i )
			{
                completeList.add( KoLConstants.FRAME_NAMES[i][0] );
			}

			JPanel optionPanel = new JPanel( new GridLayout( 1, 3 ) );
			optionPanel.add( new ScrollablePanel( "Complete List", new JDnDList( completeList ) ) );
			optionPanel.add( new ScrollablePanel( "Startup as Window", new JDnDList( startupList ) ) );
			optionPanel.add( new ScrollablePanel( "Startup in Tabs", new JDnDList( desktopList ) ) );

			JTextArea message =
				new JTextArea(
					"These are the global settings for what shows up when KoLmafia successfully logs into the Kingdom of Loathing.  You can drag and drop options in the lists below to customize what will show up.\n\n" +

						"When you place the Local Relay Server into the 'startup in tabs' section, KoLmafia will start up the server but not open your browser.  When you place the Contact List into the 'startup in tabs' section, KoLmafia will force a refresh of your contact list on login.\n" );

			// message.setColumns( 32 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

            container.add( message, BorderLayout.NORTH );
            container.add( optionPanel, BorderLayout.SOUTH );
            actionCancelled();

            completeList.addListDataListener( this );
            startupList.addListDataListener( this );
            desktopList.addListDataListener( this );
		}

		@Override
		public void dispose()
		{
            completeList.removeListDataListener( this );
            startupList.removeListDataListener( this );
            desktopList.removeListDataListener( this );

			super.dispose();
		}

		@Override
		public void actionConfirmed()
		{
            actionCancelled();
		}

		@Override
		public void actionCancelled()
		{
            isRefreshing = true;

			String username = (String) KoLConstants.saveStateNames.getSelectedItem();
			if ( username == null )
			{
				username = "";
			}

            startupList.clear();
            desktopList.clear();

			KoLmafiaGUI.checkFrameSettings();

			String frameString = Preferences.getString( "initialFrames" );
			String desktopString = Preferences.getString( "initialDesktop" );

			String[] pieces;

			pieces = frameString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( !startupList.contains( KoLConstants.FRAME_NAMES[j][0] ) && KoLConstants.FRAME_NAMES[ j ][ 1 ].equals( pieces[ i ] ) )
					{
                        startupList.add( KoLConstants.FRAME_NAMES[j][0] );
					}
				}
			}

			pieces = desktopString.split( "," );
			for ( int i = 0; i < pieces.length; ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( !desktopList.contains( KoLConstants.FRAME_NAMES[j][0] ) && KoLConstants.FRAME_NAMES[ j ][ 1 ].equals( pieces[ i ] ) )
					{
                        desktopList.add( KoLConstants.FRAME_NAMES[j][0] );
					}
				}
			}

            isRefreshing = false;
            saveLayoutSettings();
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}

		public void intervalAdded( final ListDataEvent e )
		{
			Object src = e.getSource();
			if ( src == startupList )
			{
                desktopList.removeAll( startupList );
			}
			else if ( src == desktopList )
			{
                startupList.removeAll( desktopList );
			}
			else if ( src == completeList )
			{
				Object item = completeList.get( e.getIndex0() );
                desktopList.remove( item );
                startupList.remove( item );
			}

            saveLayoutSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
            saveLayoutSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( isRefreshing )
			{
				return;
			}

			StringBuilder frameString = new StringBuilder();
			StringBuilder desktopString = new StringBuilder();

			for ( int i = 0; i < startupList.getSize(); ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( startupList.getElementAt( i ).equals( KoLConstants.FRAME_NAMES[ j ][ 0 ] ) )
					{
						if ( frameString.length() != 0 )
						{
							frameString.append( "," );
						}
						frameString.append( KoLConstants.FRAME_NAMES[ j ][ 1 ] );
					}
				}
			}

			for ( int i = 0; i < desktopList.getSize(); ++i )
			{
				for ( int j = 0; j < KoLConstants.FRAME_NAMES.length; ++j )
				{
					if ( desktopList.getElementAt( i ).equals( KoLConstants.FRAME_NAMES[ j ][ 0 ] ) )
					{
						if ( desktopString.length() != 0 )
						{
							desktopString.append( "," );
						}
						desktopString.append( KoLConstants.FRAME_NAMES[ j ][ 1 ] );
					}
				}
			}

			Preferences.setString( "initialFrames", frameString.toString() );
			Preferences.setString( "initialDesktop", desktopString.toString() );
		}
	}

	private class DeedsButtonPanel
		extends ScrollablePanel
		implements ListDataListener
	{
		public DeedsButtonPanel( final String title, final LockableListModel builtIns )
		{
			super( title, "add custom", "reset deeds", new JDnDList( builtIns ) );

            buttonPanel.add( new ThreadedButton( "help", new HelpRunnable() ), BorderLayout.CENTER );
		}

		@Override
		public final void actionConfirmed()
		{
			if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
			{
				InputFieldUtilities.alert( "You must be logged in to use the custom deed builder." );
			}
			else
			{
				JFrame builderFrame = new JFrame( "Building Custom Deed" );
				new AddCustomDeedsPanel();

				builderFrame.getContentPane().add( AddCustomDeedsPanel.selectorPanel );
				builderFrame.pack();
				builderFrame.setResizable( false );
				builderFrame.setLocationRelativeTo( null );
				builderFrame.setVisible( true );
			}
		}

		@Override
		public final void actionCancelled()
		{
			boolean reset = InputFieldUtilities
				.confirm( "This will reset your deeds to the default settings.\nAre you sure?" );
			if ( reset )
			{
				Preferences.resetToDefault( "dailyDeedsOptions" );
			}
		}

		public void intervalAdded( final ListDataEvent e )
		{
            saveSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
            saveSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
            saveSettings();
		}

		private class HelpRunnable
			implements Runnable
		{
			JOptionPane pane;

			public HelpRunnable()
			{
				String message =
					"<html><table width=750><tr><td>All deeds are specified by one comma-delimited preference \"dailyDeedsOptions\".  Order matters.  Built-in deeds are simply called by referring to their built-in name; these are viewable by pulling up the Daily Deeds tab and looking in the \"Built-in Deeds\" list."
						+ "<h3><b>Custom Deeds</b></h3>"
						+ "Custom deeds provide the user with a way of adding buttons or text to their daily deeds panel that is not natively provided for.  All deeds start with the keyword <b>$CUSTOM</b> followed by a pipe (|) symbol.  As you are constructing a custom deed, you separate the different arguments with pipes.<br>"
						+ "<br>"
						+ "All deed types except for Text require a preference to track.  If you want to add a button that is always enabled, you will have to create a dummy preference that is always false.<br>"
						+ "<br>"
						+ "There are currently 5 different types of custom deeds.  Remember that all of these \"acceptable forms\" are prefaced by $CUSTOM|.<br>"
						+ "<br>"
						+ "<b>Command</b> - execute a command with a button press<br>"
						+ "acceptable forms:"
						+ "<br>Command|displayText|preference<br>"
						+ "Command|displayText|preference|command<br>"
						+ "Command|displayText|preference|command|maxUses<br>"
						+ "<br>"
						+ "displayText - the text that will be displayed on the button<br>"
						+ "preference - the preference to track.  The button will be enabled when the preference is less than maxUses (default 1).<br>"
						+ "command - the command to execute.  If not specified, will default to displayText.<br>"
						+ "maxUses - an arbitrary integer.  Specifies a threshold to disable the button at.  A counter in the form of &lt;preference&gt;/&lt;maxUses&gt; will be displayed to the right of the button.<br>"
						+ "<br>"
						+ "<b>Item</b> - this button will use fuzzy matching to find the name of the item specified.  Will execute \"use &lt;itemName&gt;\" when clicked.  Will only be visible when you possess one or more of the item.<br>"
						+ "acceptable forms:<br>"
						+ "Item|displayText|preference<br>"
						+ "Item|displayText|preference|itemName<br>"
						+ "Item|displayText|preference|itemName|maxUses<br>"
						+ "<br>"
						+ "itemName - the name of the item that will be used.  If not specified, will default to displayText.<br>"
						+ "<br>"
						+ "<b>Skill</b> - cast a skill that is tracked by a boolean or int preference.  Will execute \"cast &lt;skillName&gt;\" when clicked.  Will not be visible if you don't know the skill.<br>"
						+ "acceptable forms:<br>"
						+ "Skill|displayText|preference<br>"
						+ "Skill|displayText|preference|skillName<br>"
						+ "Skill|displayText|preference|skillName|maxCasts<br>"
						+ "<br>"
						+ "skillName- the name of the skill that will be cast.  If not specified, will default to displayText.  Must be specified if maxCasts are specified.<br>"
						+ "maxCasts - an arbitrary integer.  Specifies a threshold to disable the button at.  A counter in the form of &lt;preference&gt;/&lt;maxCasts&gt; will be displayed to the right of the button.<br>"
						+ "<br>"
						+ "<b>Text</b><br>"
						+ "acceptable forms:<br>"
						+ "Text|pretty much anything.<br>"
						+ "<br>"
						+ "You can supply as many arguments as you want to a Text deed.  Any argument that uniquely matches a preference will be replaced by that preference's value.  If you want to use a comma in your text, immediately follow the comma with a pipe character so it will not be parsed as the end of the Text deed.</td></tr></table></html>";

				JTextPane textPane = new JTextPane();
				textPane.setContentType( "text/html" );
				textPane.setText( message );
				textPane.setOpaque( false );
				textPane.setEditable( false );
				textPane.setSelectionStart( 0 );
				textPane.setSelectionEnd( 0 ); // don't have scrollPane scrolled to the bottom when you open it

				JScrollPane scrollPane = new JScrollPane( textPane );
				scrollPane.setPreferredSize( new Dimension( 800, 550 ) );
				scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

                pane = new JOptionPane( scrollPane, JOptionPane.PLAIN_MESSAGE );
			}

			public void run()
			{
				JDialog dialog = pane.createDialog( null, "Daily Deeds Help" );
				dialog.setModal( false );
				dialog.setVisible( true );
			}
		}

		public void saveSettings()
		{

		}
	}

	protected class CustomizeDailyDeedsPanel
		extends GenericPanel
		implements ListDataListener, PreferenceListener
	{
		private boolean isRefreshing = false;

		private final LockableListModel builtInsList = new LockableListModel();
		private final LockableListModel deedsList = new LockableListModel();

		public CustomizeDailyDeedsPanel()
		{
			super( new Dimension( 2, 2 ), new Dimension( 2, 2 ) );
            setContent( null );

			JPanel botPanel = new JPanel( new GridLayout( 1, 0, 10, 0 ) );
			JPanel centerPanel = new JPanel( new GridLayout( 1, 2, 0, 0 ) );

			for ( int i = 0; i < DailyDeedsPanel.BUILTIN_DEEDS.length; ++i )
			{
                builtInsList.add( DailyDeedsPanel.BUILTIN_DEEDS[i][1] );
			}

			centerPanel.add( new DeedsButtonPanel( "Built-In Deeds", builtInsList ) );
			botPanel.add( new ScrollablePanel( "Current Deeds", new JDnDList( deedsList ) ) );

            container.add( centerPanel, BorderLayout.PAGE_START );
            container.add( botPanel, BorderLayout.PAGE_END );
            actionCancelled();

            builtInsList.addListDataListener( this );
            deedsList.addListDataListener( this );
			PreferenceListenerRegistry.registerListener( "dailyDeedsOptions", this );
		}

		public CustomizeDailyDeedsPanel( final String string )
		{
			super( new Dimension( 2, 2 ), new Dimension( 2, 2 ) );
            setContent( null );

			JTextArea message = new JTextArea(
				"Edit the appearance of your daily deeds panel.\n\n"
					+ "Drag built-in deeds into the 'Current Deeds' box down below to include, "
					+ "and delete them from there to exclude.  Drag and drop to rearrange. "
					+ "Note that some deeds added to the 'Current Deeds' box may still remain hidden "
					+ "once you add them depending on whether you posess certain "
					+ "items, skills, and/or access to zones." );

			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

            container.add( message, BorderLayout.NORTH );
		}

		@Override
		public void dispose()
		{
            builtInsList.removeListDataListener( this );
            deedsList.removeListDataListener( this );

			super.dispose();
		}

		@Override
		public void actionConfirmed()
		{
            actionCancelled();
		}

		@Override
		public void actionCancelled()
		{
            isRefreshing = true;
			String deedsString = Preferences.getString( "dailyDeedsOptions" );
			String[] pieces;

			pieces = deedsString.split( ",(?!\\|)" );

            deedsList.clear();

			KoLmafiaGUI.checkFrameSettings();

			for ( int i = 0; i < pieces.length; ++i )
			{
				for ( int j = 0; j < DailyDeedsPanel.BUILTIN_DEEDS.length; ++j )
				{
					if ( !deedsList.contains( DailyDeedsPanel.BUILTIN_DEEDS[j][1] )
						&& DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ].equals( pieces[ i ] ) )
					{
                        deedsList.add( DailyDeedsPanel.BUILTIN_DEEDS[j][1] );
					}
				}
				if ( pieces[ i ].split( "\\|" )[ 0 ].equals( "$CUSTOM" ) )
				{
                    deedsList.add( pieces[i] );
				}
			}

            isRefreshing = false;
            saveLayoutSettings();
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}

		public void intervalAdded( final ListDataEvent e )
		{
			Object src = e.getSource();

			if ( src == builtInsList )
			{
				Object item = builtInsList.get( e.getIndex0() );

                deedsList.remove( item );
			}

            saveLayoutSettings();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
            saveLayoutSettings();
		}

		public void contentsChanged( final ListDataEvent e )
		{
		}

		public void saveLayoutSettings()
		{
			if ( isRefreshing )
			{
				return;
			}

			StringBuilder frameString = new StringBuilder();

			for ( int i = 0; i < deedsList.getSize(); ++i )
			{
				for ( int j = 0; j < DailyDeedsPanel.BUILTIN_DEEDS.length; ++j )
				{
					if ( deedsList.getElementAt( i ).equals(
						DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ] ) )
					{
						if ( frameString.length() != 0 )
						{
							frameString.append( "," );
						}
						frameString.append( DailyDeedsPanel.BUILTIN_DEEDS[ j ][ 1 ] );
					}
				}
				if ( deedsList.getElementAt( i ).toString().split( "\\|" )[ 0 ].equals( "$CUSTOM" ) )
				{
					if ( frameString.length() != 0 )
					{
						frameString.append( "," );
					}
					frameString.append( deedsList.getElementAt( i ) );
				}
			}

			Preferences.setString( "dailyDeedsOptions", frameString.toString() );
		}

		public void update()
		{
            actionCancelled();
		}
	}

	/**
	 * Allows the user to select to select the framing mode to use.
	 */

	protected class UserInterfacePanel
		extends OptionsPanel
	{
		private JCheckBox[] optionBoxes;

		private final String[][] options =

			System.getProperty( "os.name" ).startsWith( "Windows" ) ?

				new String[][]
				{
					{ "guiUsesOneWindow", "Restrict interface to a single window" },
					{ "useSystemTrayIcon", "Minimize main interface to system tray" },
					{ "addCreationQueue", "Add creation queueing interface to item manager" },
					{ "addStatusBarToFrames", "Add a status line to independent windows" },
					{ "autoHighlightOnFocus", "Highlight text fields when selected" },
					{},
					{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
					{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
				}

				: System.getProperty( "os.name" ).startsWith( "Mac" ) ?

					new String[][]
					{
						{ "guiUsesOneWindow", "Restrict interface to a single window" },
						{ "useDockIconBadge", "Show turns remaining on Dock icon (OSX 10.5+)" },
						{ "addCreationQueue", "Add creation queueing interface to item manager" },
						{ "addStatusBarToFrames", "Add a status line to independent windows" },
						{ "autoHighlightOnFocus", "Highlight text fields when selected" },
						{},
						{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
						{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
					}

					:

					new String[][]
					{
						{ "guiUsesOneWindow", "Restrict interface to a single window" },
						{ "addCreationQueue", "Add creation queueing interface to item manager" },
						{ "addStatusBarToFrames", "Add a status line to independent windows" },
						{ "autoHighlightOnFocus", "Highlight text fields when selected" },
						{},
						{ "useDecoratedTabs", "Use shiny decorated tabs instead of OS default" },
						{ "allowCloseableDesktopTabs", "Allow tabs on main window to be closed" },
					};

		private final JComboBox looks, toolbars, scripts;

		public UserInterfacePanel()
		{
			super( new Dimension( 80, 20 ), new Dimension( 280, 20 ) );

			UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
			Object[] installedLooks = new Object[ installed.length + 1 ];

			installedLooks[ 0 ] = "Always use OS default look and feel";

			for ( int i = 0; i < installed.length; ++i )
			{
				installedLooks[ i + 1 ] = installed[ i ].getClassName();
			}

            looks = new JComboBox( installedLooks );

            toolbars = new JComboBox();
            toolbars.addItem( "Show global menus only" );
            toolbars.addItem( "Put toolbar along top of panel" );
            toolbars.addItem( "Put toolbar along bottom of panel" );
            toolbars.addItem( "Put toolbar along left of panel" );

            scripts = new JComboBox();
            scripts.addItem( "Do not show script bar on main interface" );
            scripts.addItem( "Put script bar after normal toolbar" );
            scripts.addItem( "Put script bar along right of panel" );

			VerifiableElement[] elements = new VerifiableElement[ 3 ];

			elements[ 0 ] = new VerifiableElement( "Java L&F: ", looks );
			elements[ 1 ] = new VerifiableElement( "Toolbar: ", toolbars );
			elements[ 2 ] = new VerifiableElement( "Scripts: ", scripts );

            actionCancelled();
            setContent( elements );
		}

		public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
		{
			return false;
		}

		@Override
		public void setContent( final VerifiableElement[] elements )
		{
			super.setContent( elements );
            add( new InterfaceCheckboxPanel(), BorderLayout.CENTER );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}

		@Override
		public void actionConfirmed()
		{
			String lookAndFeel = "";

			if ( looks.getSelectedIndex() > 0 )
			{
				lookAndFeel = (String) looks.getSelectedItem();
			}

			Preferences.setString( "swingLookAndFeel", lookAndFeel );

			Preferences.setBoolean( "useToolbars", toolbars.getSelectedIndex() != 0 );
			Preferences.setInteger( "scriptButtonPosition", scripts.getSelectedIndex() );
			Preferences.setInteger( "toolbarPosition", toolbars.getSelectedIndex() );
		}

		@Override
		public void actionCancelled()
		{
			String lookAndFeel = Preferences.getString( "swingLookAndFeel" );

			if ( lookAndFeel.equals( "" ) )
			{
                looks.setSelectedIndex( 0 );
			}
			else
			{
                looks.setSelectedItem( lookAndFeel );
			}

            toolbars.setSelectedIndex( Preferences.getInteger( "toolbarPosition" ) );
            scripts.setSelectedIndex( Preferences.getInteger( "scriptButtonPosition" ) );
		}

		private class InterfaceCheckboxPanel
			extends OptionsPanel
		{
			private final JLabel innerGradient, outerGradient;

			public InterfaceCheckboxPanel()
			{
				super( new Dimension( 20, 16 ), new Dimension( 370, 16 ) );
				VerifiableElement[] elements = new VerifiableElement[ UserInterfacePanel.this.options.length + 3 ];

				UserInterfacePanel.this.optionBoxes = new JCheckBox[ UserInterfacePanel.this.options.length ];

				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					String[] option = UserInterfacePanel.this.options[ i ];
					JCheckBox optionBox = new JCheckBox();
					UserInterfacePanel.this.optionBoxes[ i ] = optionBox;
					elements[ i ] =
						option.length == 0 ?
							new VerifiableElement() :
							new VerifiableElement( option[ 1 ], SwingConstants.LEFT, optionBox );
				}

				elements[ UserInterfacePanel.this.options.length ] = new VerifiableElement();

                outerGradient = new TabColorChanger( "outerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 1 ] =
					new VerifiableElement(
						"Change the outer portion of the tab gradient (shiny tabs)", SwingConstants.LEFT,
                            outerGradient );

                innerGradient = new TabColorChanger( "innerTabColor" );
				elements[ UserInterfacePanel.this.options.length + 2 ] =
					new VerifiableElement(
						"Change the inner portion of the tab gradient (shiny tabs)", SwingConstants.LEFT,
                            innerGradient );

                actionCancelled();
                setContent( elements );
			}

			@Override
			public void actionConfirmed()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					String[] option = UserInterfacePanel.this.options[ i ];
					if ( option.length == 0 )
					{
						continue;
					}
					JCheckBox optionBox = UserInterfacePanel.this.optionBoxes[ i ];
					Preferences.setBoolean( option[ 0 ], optionBox.isSelected() );
				}
			}

			@Override
			public void actionCancelled()
			{
				for ( int i = 0; i < UserInterfacePanel.this.options.length; ++i )
				{
					String[] option = UserInterfacePanel.this.options[ i ];
					if ( option.length == 0 )
					{
						continue;
					}
					JCheckBox optionBox = UserInterfacePanel.this.optionBoxes[ i ];
					optionBox.setSelected( Preferences.getBoolean( option[ 0 ] ) );
				}

                innerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedA );
                outerGradient.setBackground( tab.CloseTabPaneEnhancedUI.selectedB );
			}

			@Override
			public void setEnabled( final boolean isEnabled )
			{
			}

			private class TabColorChanger
				extends ColorChooser
			{
				public TabColorChanger( final String property )
				{
					super( property );
				}

				@Override
				public void applyChanges()
				{
					if ( property.equals( "innerTabColor" ) )
					{
						CloseTabPaneEnhancedUI.selectedA = innerGradient.getBackground();
					}
					else
					{
						CloseTabPaneEnhancedUI.selectedB = outerGradient.getBackground();
					}
				}
			}
		}
	}

	protected class BrowserPanel
		extends OptionsPanel
	{
		private final FileSelectPanel preferredWebBrowser;

		public BrowserPanel()
		{
			AutoHighlightTextField textField = new AutoHighlightTextField();
			boolean button = true;
			String helpText = "";
			String path = null;

			if ( UtilityConstants.USE_OSX_STYLE_DIRECTORIES )
			{
				button = false;
				path = "/Applications";
				helpText =
					"If KoLmafia opens a browser other than your default, enter the name of your preferred browser here. The browser must be in your Applications directory";
			}
			else if ( UtilityConstants.USE_LINUX_STYLE_DIRECTORIES )
			{
				button = true;
				path = "/";
				helpText =
					"If KoLmafia opens a browser other than your default, enter the name of your preferred browser here. If that doesn't work, click the button and browse to the location of your browser.";
			}
			else
			// Windows
			{
				button = true;
				path = "";
				helpText =
					"If KoLmafia opens a browser other than your default, enter the name of your preferred browser here. If that doesn't work, click the button and browse to the location of your browser.";
			}

            preferredWebBrowser = new FileSelectPanel( textField, button );
			if ( button )
			{
                preferredWebBrowser.setPath( new File( path ) );
			}

			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Browser: ", preferredWebBrowser );

            setContent( elements );

			JTextArea message = new JTextArea( helpText );
			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

            container.add( message, BorderLayout.SOUTH );

            actionCancelled();
		}

		@Override
		public void actionConfirmed()
		{
			Preferences.setString( "preferredWebBrowser", preferredWebBrowser.getText() );
		}

		@Override
		public void actionCancelled()
		{
            preferredWebBrowser.setText( Preferences.getString( "preferredWebBrowser" ) );
		}
	}

	protected class EditorPanel
		extends OptionsPanel
	{
		private final FileSelectPanel preferredEditor;

		public EditorPanel()
		{
			AutoHighlightTextField textField = new AutoHighlightTextField();
			boolean button = true;
			String helpText = "";
			String path = null;

			{
				button = false;
				path = "";
				helpText = "The command will be invoked with the full path to the script as its only parameter.";
			}

            preferredEditor = new FileSelectPanel( textField, button );
			if ( button )
			{
                preferredEditor.setPath( new File( path ) );
			}

			VerifiableElement[] elements = new VerifiableElement[ 1 ];
			elements[ 0 ] = new VerifiableElement( "Editor command: ", preferredEditor );

            setContent( elements );

			JTextArea message = new JTextArea( helpText );
			message.setColumns( 40 );
			message.setLineWrap( true );
			message.setWrapStyleWord( true );
			message.setEditable( false );
			message.setOpaque( false );
			message.setFont( KoLConstants.DEFAULT_FONT );

            container.add( message, BorderLayout.SOUTH );

            actionCancelled();
		}

		@Override
		public void actionConfirmed()
		{
			Preferences.setString( "externalEditor", preferredEditor.getText() );
		}

		@Override
		public void actionCancelled()
		{
            preferredEditor.setText( Preferences.getString( "externalEditor" ) );
		}
	}

	protected class ScriptPanel
		extends OptionsPanel
	{
		private final ScriptSelectPanel loginScript;
		private final ScriptSelectPanel logoutScript;
		
		private final ScriptSelectPanel recoveryScript;
		private final ScriptSelectPanel betweenBattleScript;
		private final ScriptSelectPanel afterAdventureScript;
		private final ScriptSelectPanel counterScript;
		
		private final ScriptSelectPanel kingLiberatedScript;
		private final ScriptSelectPanel preAscensionScript;
		private final ScriptSelectPanel postAscensionScript;
		
		private final ScriptSelectPanel beforePVPScript;
		private final ScriptSelectPanel buyScript;
		private final ScriptSelectPanel plantingScript;
		private final ScriptSelectPanel chatBotScript;

		public ScriptPanel()
		{
            loginScript = new ScriptSelectPanel( new CollapsibleTextArea( "On Login: " ) );
            logoutScript = new ScriptSelectPanel( new CollapsibleTextArea( "On Logout: " ) );
            recoveryScript = new ScriptSelectPanel( new CollapsibleTextArea( "Recovery: " ) );
            betweenBattleScript = new ScriptSelectPanel( new CollapsibleTextArea( "Before Adventure: " ) );
            afterAdventureScript = new ScriptSelectPanel( new CollapsibleTextArea( "After Adventure: " ) );
            counterScript = new ScriptSelectPanel( new CollapsibleTextArea( "Counter Script: " ) );
            kingLiberatedScript = new ScriptSelectPanel( new CollapsibleTextArea( "King Freed: " ) );
            preAscensionScript = new ScriptSelectPanel( new CollapsibleTextArea( "Pre-Ascension: " ) );
            postAscensionScript = new ScriptSelectPanel( new CollapsibleTextArea( "Post-Ascension: " ) );
            beforePVPScript = new ScriptSelectPanel( new CollapsibleTextArea( "Before PvP: " ) );
            buyScript = new ScriptSelectPanel( new CollapsibleTextArea( "Buy Script: " ) );
            plantingScript = new ScriptSelectPanel( new CollapsibleTextArea( "Planting: " ) );
            chatBotScript = new ScriptSelectPanel( new CollapsibleTextArea( "Chatbot Script: " ) );

			ArrayList<ScriptSelectPanel> list = new ArrayList<ScriptSelectPanel>();

			list.add( loginScript );
			list.add( logoutScript );
			list.add( recoveryScript );
			list.add( betweenBattleScript );
			list.add( afterAdventureScript );
			list.add( counterScript );
			list.add( kingLiberatedScript );
			list.add( preAscensionScript );
			list.add( postAscensionScript );
			list.add( beforePVPScript );
			list.add( buyScript );
			list.add( plantingScript );
			list.add( chatBotScript );

			JPanel layoutPanel = new JPanel();
			layoutPanel.setLayout( new BoxLayout( layoutPanel, BoxLayout.Y_AXIS ) );

			for ( int i = 0; i < list.size(); ++i )
			{
				JPanel p = new JPanel( new BorderLayout() );
				p.add( list.get( i ).getLabel(), BorderLayout.CENTER );
				p.add( list.get( i ), BorderLayout.EAST );

				p.add( Box.createVerticalStrut( 5 ), BorderLayout.SOUTH );

				layoutPanel.add( p );
			}

			JScrollPane scrollPane = new JScrollPane( layoutPanel );
			scrollPane.setViewportView( layoutPanel );

            contentPane.add( scrollPane );
            actionCancelled();
		}

		@Override
		public void actionConfirmed()
		{
			Preferences.setString( "loginScript", loginScript.getText() );
			Preferences.setString( "logoutScript", logoutScript.getText() );
			Preferences.setString( "recoveryScript", recoveryScript.getText() );
			Preferences.setString( "betweenBattleScript", betweenBattleScript.getText() );
			Preferences.setString( "afterAdventureScript", afterAdventureScript.getText() );
			Preferences.setString( "kingLiberatedScript", kingLiberatedScript.getText() );
			Preferences.setString( "preAscensionScript", preAscensionScript.getText() );
			Preferences.setString( "postAscensionScript", postAscensionScript.getText() );
			Preferences.setString( "beforePVPScript", beforePVPScript.getText() );
			Preferences.setString( "buyScript", buyScript.getText() );
			Preferences.setString( "plantingScript", plantingScript.getText() );
			Preferences.setString( "chatBotScript", chatBotScript.getText() );
		}

		@Override
		public void actionCancelled()
		{
			String loginScript = Preferences.getString( "loginScript" );
			this.loginScript.setText( loginScript );

			String logoutScript = Preferences.getString( "logoutScript" );
			this.logoutScript.setText( logoutScript );

			String recoveryScript = Preferences.getString( "recoveryScript" );
			this.recoveryScript.setText( recoveryScript );

			String betweenBattleScript = Preferences.getString( "betweenBattleScript" );
			this.betweenBattleScript.setText( betweenBattleScript );

			String afterAdventureScript = Preferences.getString( "afterAdventureScript" );
			this.afterAdventureScript.setText( afterAdventureScript );

			String counterScript = Preferences.getString( "counterScript" );
			this.counterScript.setText( counterScript );

			String kingLiberatedScript = Preferences.getString( "kingLiberatedScript" );
			this.kingLiberatedScript.setText( kingLiberatedScript );

			String preAscensionScript = Preferences.getString( "preAscensionScript" );
			this.preAscensionScript.setText( preAscensionScript );

			String postAscensionScript = Preferences.getString( "postAscensionScript" );
			this.postAscensionScript.setText( postAscensionScript );

			String beforePVPScript = Preferences.getString( "beforePVPScript" );
			this.beforePVPScript.setText( beforePVPScript );

			String buyScript = Preferences.getString( "buyScript" );
			this.buyScript.setText( buyScript );

			String plantingScript = Preferences.getString( "plantingScript" );
			this.plantingScript.setText( plantingScript );

			String chatBotScript = Preferences.getString( "chatBotScript" );
			this.chatBotScript.setText( chatBotScript );
		}

	}

	protected class BreakfastPanel
		extends JPanel
		implements ActionListener
	{
		private final String breakfastType;
		private final JCheckBox[] skillOptions;

		private final JCheckBox loginRecovery;
		private final JCheckBox pathedSummons;
		private final JCheckBox rumpusRoom;
		private final JCheckBox clanLounge;

		private final JCheckBox mushroomPlot;
		private final JCheckBox grabClovers;
		private final JCheckBox readManual;
		private final JCheckBox useCrimboToys;

		private final SkillMenu tomeSkills;
		private final SkillMenu libramSkills;
		private final SkillMenu grimoireSkills;

		private final CropMenu cropsMenu;

		public BreakfastPanel( final String breakfastType )
		{
			super( new CardLayout( 10, 10 ) );

			JPanel centerContainer = new JPanel();
			centerContainer.setLayout( new BoxLayout( centerContainer, BoxLayout.Y_AXIS ) );

			int rows = ( UseSkillRequest.BREAKFAST_SKILLS.length + 8 ) / 2 + 1;

			JPanel centerPanel = new JPanel( new GridLayout( rows, 2 ) );

            loginRecovery = new JCheckBox( "enable auto-recovery" );
            loginRecovery.addActionListener( this );
			centerPanel.add( loginRecovery );

            pathedSummons = new JCheckBox( "honor path restrictions" );
            pathedSummons.addActionListener( this );
			centerPanel.add( pathedSummons );

            rumpusRoom = new JCheckBox( "visit clan rumpus room" );
            rumpusRoom.addActionListener( this );
			centerPanel.add( rumpusRoom );

            clanLounge = new JCheckBox( "visit clan VIP lounge" );
            clanLounge.addActionListener( this );
			centerPanel.add( clanLounge );

			this.breakfastType = breakfastType;
            skillOptions = new JCheckBox[ UseSkillRequest.BREAKFAST_SKILLS.length ];
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
                skillOptions[ i ] = new JCheckBox( UseSkillRequest.BREAKFAST_SKILLS[ i ].toLowerCase() );
                skillOptions[ i ].addActionListener( this );
				centerPanel.add( skillOptions[ i ] );
			}

            mushroomPlot = new JCheckBox( "plant mushrooms" );
            mushroomPlot.addActionListener( this );
			centerPanel.add( mushroomPlot );

            grabClovers = new JCheckBox( "get hermit clovers" );
            grabClovers.addActionListener( this );
			centerPanel.add( grabClovers );

            readManual = new JCheckBox( "read guild manual" );
            readManual.addActionListener( this );
			centerPanel.add( readManual );

            useCrimboToys = new JCheckBox( "use once-a-day items" );
            useCrimboToys.addActionListener( this );
			centerPanel.add( useCrimboToys );

			centerContainer.add( centerPanel );
			centerContainer.add( Box.createVerticalStrut( 10 ) );

			centerPanel = new JPanel( new GridLayout( 4, 1 ) );

            tomeSkills =
				new SkillMenu( "Tome Skills", UseSkillRequest.TOME_SKILLS, "tomeSkills" + this.breakfastType );
            tomeSkills.addActionListener( this );
			centerPanel.add( tomeSkills );

            libramSkills =
				new SkillMenu( "Libram Skills", UseSkillRequest.LIBRAM_SKILLS, "libramSkills" + this.breakfastType );
            libramSkills.addActionListener( this );
			centerPanel.add( libramSkills );

            grimoireSkills =
				new SkillMenu(
					"Grimoire Skills", UseSkillRequest.GRIMOIRE_SKILLS, "grimoireSkills" + this.breakfastType );
            grimoireSkills.addActionListener( this );
			centerPanel.add( grimoireSkills );

            cropsMenu = new CropMenu( CampgroundRequest.CROPS, "harvestGarden" + this.breakfastType );
            cropsMenu.addActionListener( this );
			centerPanel.add( cropsMenu );

			centerContainer.add( centerPanel );
			centerContainer.add( Box.createVerticalGlue() );

            add( centerContainer, "" );

            actionCancelled();
		}

		public void actionPerformed( final ActionEvent e )
		{
            actionConfirmed();
		}

		public void actionConfirmed()
		{
			StringBuilder skillString = new StringBuilder();

			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				if ( skillOptions[ i ].isSelected() )
				{
					if ( skillString.length() != 0 )
					{
						skillString.append( "," );
					}

					skillString.append( UseSkillRequest.BREAKFAST_SKILLS[ i ] );
				}
			}

			Preferences.setString( "breakfast" + breakfastType, skillString.toString() );
			Preferences.setBoolean(
				"loginRecovery" + breakfastType, loginRecovery.isSelected() );
			Preferences.setBoolean(
				"pathedSummons" + breakfastType, pathedSummons.isSelected() );
			Preferences.setBoolean(
				"visitRumpus" + breakfastType, rumpusRoom.isSelected() );
			Preferences.setBoolean(
				"visitLounge" + breakfastType, clanLounge.isSelected() );
			Preferences.setBoolean(
				"autoPlant" + breakfastType, mushroomPlot.isSelected() );
			Preferences.setBoolean(
				"grabClovers" + breakfastType, grabClovers.isSelected() );
			Preferences.setBoolean(
				"readManual" + breakfastType, readManual.isSelected() );
			Preferences.setBoolean(
				"useCrimboToys" + breakfastType, useCrimboToys.isSelected() );

            tomeSkills.setPreference();
            libramSkills.setPreference();
            grimoireSkills.setPreference();
            cropsMenu.setPreference();
		}

		public void actionCancelled()
		{
			String skillString = Preferences.getString( "breakfast" + breakfastType );
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
                skillOptions[ i ].setSelected( skillString.contains( UseSkillRequest.BREAKFAST_SKILLS[i] ) );
			}

            loginRecovery.setSelected( Preferences.getBoolean( "loginRecovery" + breakfastType ) );
            pathedSummons.setSelected( Preferences.getBoolean( "pathedSummons" + breakfastType ) );
            rumpusRoom.setSelected( Preferences.getBoolean( "visitRumpus" + breakfastType ) );
            clanLounge.setSelected( Preferences.getBoolean( "visitLounge" + breakfastType ) );
            mushroomPlot.setSelected( Preferences.getBoolean( "autoPlant" + breakfastType ) );
            grabClovers.setSelected( Preferences.getBoolean( "grabClovers" + breakfastType ) );
            readManual.setSelected( Preferences.getBoolean( "readManual" + breakfastType ) );
            useCrimboToys.setSelected( Preferences.getBoolean( "useCrimboToys" + breakfastType ) );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
		}
	}

	private class SkillMenu
		extends JComboBox
	{
		final String preference;

		public SkillMenu( final String name, final String[] skills, final String preference )
		{
			super();
            addItem( "No " + name );
            addItem( "All " + name );
			for ( int i = 0; i < skills.length; ++i )
			{
                addItem( skills[ i ] );
			}

			this.preference = preference;
            getPreference();
		}

		public void getPreference()
		{
			String skill = Preferences.getString( preference );
			if ( skill.equals( "none" ) )
			{
                setSelectedIndex( 0 );
			}
			else if ( skill.equals( "all" ) )
			{
                setSelectedIndex( 1 );
			}
			else
			{
                setSelectedItem( skill );
			}

			if ( getSelectedIndex() < 0 )
			{
                setSelectedIndex( 0 );
			}
		}

		public void setPreference()
		{
			String skill = null;
			int index = getSelectedIndex();
			switch ( index )
			{
			case -1:
			case 0:
				skill = "none";
				break;
			case 1:
				skill = "all";
				break;
			default:
				skill = (String) getItemAt( index );
				break;
			}
			Preferences.setString( preference, skill );
		}
	}

	private class CropMenu
		extends JComboBox
	{
		final String preference;

		public CropMenu( final AdventureResult[] crops, final String preference )
		{
			super();
            addItem( "Harvest Nothing" );
            addItem( "Harvest Anything" );
			for ( int i = 0; i < crops.length; ++i )
			{
                addItem( crops[ i ].getName() );
			}

			this.preference = preference;
            getPreference();
		}

		public void getPreference()
		{
			String crop = Preferences.getString( preference );
			if ( crop.equals( "none" ) )
			{
                setSelectedIndex( 0 );
			}
			else if ( crop.equals( "any" ) )
			{
                setSelectedIndex( 1 );
			}
			else
			{
                setSelectedItem( crop );
			}

			if ( getSelectedIndex() < 0 )
			{
                setSelectedIndex( 0 );
			}
		}

		public void setPreference()
		{
			String crop = null;
			int index = getSelectedIndex();
			switch ( index )
			{
			case -1:
			case 0:
				crop = "none";
				break;
			case 1:
				crop = "any";
				break;
			default:
				crop = (String) getItemAt( index );
				break;
			}
			Preferences.setString( preference, crop );
		}
	}

	private class ColorOptionsPanel
		extends OptionsPanel
	{
		JLabel crappy, decent, good, awesome, epic;
		JLabel memento, junk, notavailable;

		public ColorOptionsPanel()
		{
			super( new Dimension( 16, 16 ), new Dimension( 370, 16 ) );

            setContent();
		}

		private void setContent()
		{
			VerifiableElement[] newElements = new VerifiableElement[ 13 ];

			newElements[ 0 ] = new VerifiableElement( "   ", SwingConstants.RIGHT, new JLabel(
				"This panel alters the appearance of some text colors in the Mafia UI." ) );

			newElements[ 1 ] = new VerifiableElement( "", SwingConstants.RIGHT, new JSeparator() );

			newElements[ 2 ] = new VerifiableElement( "Item Quality Colors:", SwingConstants.LEFT,
				new JLabel() );
            crappy = new FontColorChooser( "crappy" );
			newElements[ 3 ] = new VerifiableElement( "Crappy", SwingConstants.LEFT, crappy );
            decent = new FontColorChooser( "decent" );
			newElements[ 4 ] = new VerifiableElement( "Decent", SwingConstants.LEFT, decent );
            good = new FontColorChooser( "good" );
			newElements[ 5 ] = new VerifiableElement( "Good", SwingConstants.LEFT, good );
            awesome = new FontColorChooser( "awesome" );
			newElements[ 6 ] = new VerifiableElement( "Awesome", SwingConstants.LEFT, awesome );
            epic = new FontColorChooser( "epic" );
			newElements[ 7 ] = new VerifiableElement( "EPIC", SwingConstants.LEFT, epic );

			newElements[ 8 ] = new VerifiableElement();

			newElements[ 9 ] = new VerifiableElement( "Other Font Colors:", SwingConstants.LEFT,
				new JLabel() );
            junk = new FontColorChooser( "junk" );
			newElements[ 10 ] = new VerifiableElement( "Junk Items", SwingConstants.LEFT, junk );
            memento = new FontColorChooser( "memento" );
			newElements[ 11 ] = new VerifiableElement( "Mementos", SwingConstants.LEFT, memento );
            notavailable = new FontColorChooser( "notavailable" );
			newElements[ 12 ] = new VerifiableElement( "Not Equippable/Creatable/Available",
				SwingConstants.LEFT, notavailable );

			super.setContent( newElements );
            readFromPref();
		}

		private void readFromPref()
		{
			String rawPref = Preferences.getString( "textColors" );
			String[] splitPref = rawPref.split( "\\|" );

			for ( int i = 0; i < splitPref.length; ++i )
			{
				String[] it = splitPref[ i ].split( ":" );
				if ( it.length == 2 )
				{
					if ( it[ 0 ].equals( "crappy" ) )
					{
						decodeColor( it[ 1 ], crappy );
					}
					else if ( it[ 0 ].equals( "decent" ) )
					{
						decodeColor( it[ 1 ], decent );
					}
					else if ( it[ 0 ].equals( "good" ) )
					{
						decodeColor( it[ 1 ], good );
					}
					else if ( it[ 0 ].equals( "awesome" ) )
					{
						decodeColor( it[ 1 ], awesome );
					}
					else if ( it[ 0 ].equals( "epic" ) )
					{
						decodeColor( it[ 1 ], epic );
					}
					else if ( it[ 0 ].equals( "memento" ) )
					{
						decodeColor( it[ 1 ], memento );
					}
					else if ( it[ 0 ].equals( "junk" ) )
					{
						decodeColor( it[ 1 ], junk );
					}
					else if ( it[ 0 ].equals( "notavailable" ) )
					{
						decodeColor( it[ 1 ], notavailable );
					}
				}
			}

			fillDefaults();
		}

		private void fillDefaults()
		{
			if ( crappy.getClientProperty( "set" ) == null )
			{
                crappy.setBackground( Color.gray );
			}
			if ( decent.getClientProperty( "set" ) == null )
			{
                decent.setBackground( Color.black );
			}
			if ( good.getClientProperty( "set" ) == null )
			{
                good.setBackground( Color.green );
			}
			if ( awesome.getClientProperty( "set" ) == null )
			{
                awesome.setBackground( Color.blue );
			}
			if ( epic.getClientProperty( "set" ) == null )
			{
                epic.setBackground( DataUtilities.toColor( "#8a2be2" ) ); // purple..ish
			}
			if ( memento.getClientProperty( "set" ) == null )
			{
                memento.setBackground( DataUtilities.toColor( "#808000" ) ); // olive
			}
			if ( junk.getClientProperty( "set" ) == null )
			{
                junk.setBackground( Color.gray );
			}
			if ( notavailable.getClientProperty( "set" ) == null )
			{
                notavailable.setBackground( Color.gray );
			}
		}

		private void decodeColor( String it, JLabel label )
		{
			label.putClientProperty( "set", Boolean.TRUE );
			try
			{
				Field field = Color.class.getField( it );
				Color color = (Color) field.get( null );

				label.setBackground( color );
			}
			catch ( Exception e )
			{
				try
				{
					// maybe the pref was a hex code
					label.setBackground( DataUtilities.toColor( it ) );
				}
				catch ( Exception f )
				{
					// olive color is not an acceptable label, but is recognized by HTML parser
					// just hardcode it, whatever
					if ( it.equals( "olive" ) )
					{
						label.setBackground( DataUtilities.toColor( "#808000" ) );
					}
					// else fall through, invalid color format
				}
			}
		}

		private final class FontColorChooser
			extends JLabel
			implements MouseListener
		{
			protected String property;

			public FontColorChooser( final String property )
			{
				this.property = property;
                setOpaque( true );
                addMouseListener( this );
			}

			public void mousePressed( final MouseEvent e )
			{
				Color c = JColorChooser.showDialog( null, "Choose a color:", getBackground() );
				if ( c == null )
				{
					return;
				}

				updatePref( property, DataUtilities.toHexString( c ) );
                setBackground( c );
			}

			public void updatePref( String property, String hexString )
			{
				String rawPref = Preferences.getString( "textColors" );
				String[] splitPref = rawPref.split( "\\|" );
				String newProperty = property + ":" + hexString;

				for ( int i = 0; i < splitPref.length; ++i )
				{
					String[] it = splitPref[ i ].split( ":" );
					if ( it.length == 2 )
					{
						if ( it[ 0 ].equals( property ) )
						{
							String newPref = StringUtilities.globalStringReplace( rawPref,
								splitPref[ i ], newProperty );
							Preferences.setString( "textColors", newPref );
							return;
						}
					}
				}

				// property does not exist in pref; add it
				String delimiter = "";
				if ( rawPref.length() > 0 )
				{
					delimiter = "|";
				}
				String newPref = rawPref + delimiter + newProperty;
				Preferences.setString( "textColors", newPref );
			}

			public void mouseReleased( final MouseEvent e )
			{
			}

			public void mouseClicked( final MouseEvent e )
			{
			}

			public void mouseEntered( final MouseEvent e )
			{
			}

			public void mouseExited( final MouseEvent e )
			{
			}
		}
	}
}
