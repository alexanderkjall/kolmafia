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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import java.io.File;

import java.lang.ref.WeakReference;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import apple.dts.samplecode.osxadapter.OSXAdapter;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.LogoutManager;

import net.sourceforge.kolmafia.swingui.button.LoadScriptButton;

import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.RefreshSessionListener;
import net.sourceforge.kolmafia.swingui.listener.WorldPeaceListener;

import net.sourceforge.kolmafia.swingui.menu.GlobalMenuBar;
import net.sourceforge.kolmafia.swingui.menu.ScriptMenu;

import net.sourceforge.kolmafia.swingui.panel.CompactSidePane;

import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class GenericFrame
	extends JFrame
	implements Runnable, FocusListener
{
	private static int existingFrameCount = 0;
	private boolean packedOnce = false;
	private boolean exists = true;

	private HashMap listenerMap;
	private GlobalMenuBar menuBar;

	private FramePanel framePanel;

	public JTabbedPane tabs;
	protected String lastTitle;
	protected String frameName;

	public CompactSidePane sidepane = null;
	public KoLCharacterAdapter refreshListener = null;

	static
	{
		GenericFrame.compileScripts();
		GenericFrame.compileBookmarks();
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be associated with the given
	 * StaticEntity.getClient().
	 */

	public GenericFrame()
	{
		this( "" );
	}

	/**
	 * Constructs a new <code>KoLFrame</code> with the given title, to be associated with the given
	 * StaticEntity.getClient().
	 */

	public GenericFrame( final String title )
	{
        setTitle( title );
        setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

        tabs = getTabbedPane();
        framePanel = new FramePanel();

        frameName = getClass().getName();
        frameName = frameName.substring( frameName.lastIndexOf( "." ) + 1 );

		if ( shouldAddStatusBar() )
		{
			JScrollPane statusBar = KoLConstants.commandBuffer.addDisplay( new RequestPane() );
			JComponentUtilities.setComponentSize( statusBar, new Dimension( 200, 50 ) );

			JSplitPane doublePane =
				new JSplitPane( JSplitPane.VERTICAL_SPLIT, new GenericScrollPane( framePanel ), statusBar );

			doublePane.setOneTouchExpandable( true );
			doublePane.setDividerLocation( 0.9 );

			JPanel wrappedDoublePane = new JPanel( new BorderLayout( 0, 0 ) );
			wrappedDoublePane.add( doublePane, BorderLayout.CENTER );

            setContentPane( wrappedDoublePane );
		}
		else
		{
            setContentPane( framePanel );
		}

        menuBar = new GlobalMenuBar();
        setJMenuBar( menuBar );
        addHotKeys();

		++GenericFrame.existingFrameCount;

		if ( showInWindowMenu() )
		{
			KoLConstants.existingFrames.add( getFrameName() );
		}

        setFocusCycleRoot( true );
        setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( framePanel ) );

        addFocusListener( this );

		OSXAdapter.setWindowCanFullScreen( this, true );
	}

	public void focusGained( FocusEvent e )
	{
        framePanel.requestFocus();
	}

	public void focusLost( FocusEvent e )
	{
	}

	public void setCenterComponent( Component c )
	{
        framePanel.add( c, BorderLayout.CENTER );
	}

	public Component getCenterComponent()
	{
		return framePanel.centerComponent;
	}

	public void removeCenterComponent()
	{
        framePanel.remove( framePanel.centerComponent );
	}

	public JPanel getFramePanel()
	{
		return framePanel;
	}

	public boolean shouldAddStatusBar()
	{
		return Preferences.getBoolean( "addStatusBarToFrames" ) && !appearsInTab();
	}

	public boolean showInWindowMenu()
	{
		return true;
	}

	protected void addActionListener( final JCheckBox component, final ActionListener listener )
	{
		if ( listenerMap == null )
		{
            listenerMap = new HashMap();
		}

		component.addActionListener( listener );
        listenerMap.put( component, new WeakReference( listener ) );
	}

	protected void addActionListener( final JComboBox component, final ActionListener listener )
	{
		if ( listenerMap == null )
		{
            listenerMap = new HashMap();
		}

		component.addActionListener( listener );
        listenerMap.put( component, new WeakReference( listener ) );
	}

	protected void removeActionListener( final JComponent component, final ActionListener listener )
	{
		if ( component instanceof JCheckBox )
		{
			( (JCheckBox) component ).removeActionListener( listener );
		}
		if ( component instanceof JComboBox )
		{
			( (JComboBox) component ).removeActionListener( listener );
		}
	}

	public boolean appearsInTab()
	{
		return GenericFrame.appearsInTab( frameName );
	}

	public static boolean appearsInTab( String frameName )
	{
		String tabSetting = Preferences.getString( "initialDesktop" );
		return tabSetting.contains( frameName );
	}

	public JTabbedPane getTabbedPane()
	{
		return KoLmafiaGUI.getTabbedPane();
	}

	public void addHotKeys()
	{
		JComponentUtilities.addGlobalHotKey( getRootPane(), KeyEvent.VK_ESCAPE, new WorldPeaceListener() );
		JComponentUtilities.addGlobalHotKey( getRootPane(), KeyEvent.VK_F5, new RefreshSessionListener() );

		JComponentUtilities.addGlobalHotKey(
                getRootPane(), KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK, new TabForwardListener() );
		JComponentUtilities.addGlobalHotKey(
                getRootPane(), KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK, new TabBackwardListener() );
	}

	public void removeHotKeys()
	{
        getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ) );
        getRootPane().unregisterKeyboardAction( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0 ) );

        getRootPane().unregisterKeyboardAction(
			KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_UP, InputEvent.CTRL_MASK ) );
        getRootPane().unregisterKeyboardAction(
			KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_MASK ) );
	}

	private class TabForwardListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			if ( tabs == null )
			{
				return;
			}

            tabs.setSelectedIndex( (tabs.getSelectedIndex() + 1) % tabs.getTabCount() );
		}
	}

	private class TabBackwardListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			if ( tabs == null )
			{
				return;
			}

            tabs.setSelectedIndex( tabs.getSelectedIndex() == 0 ? tabs.getTabCount() - 1 : tabs.getSelectedIndex() - 1 );
		}
	}

	public final void addTab( final String name, final JComponent panel )
	{
		if ( tabs == null )
		{
			return;
		}

        tabs.setOpaque( true );

		GenericScrollPane scroller = new GenericScrollPane( panel );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
        tabs.add( name, scroller );
	}

	@Override
	public final void setTitle( final String newTitle )
	{
        lastTitle = newTitle;
		KoLDesktop.setTitle( this, newTitle );

		if ( this instanceof LoginFrame )
		{
			super.setTitle( lastTitle );
			return;
		}

		String username = KoLCharacter.getUserName();
		if ( username.equals( "" ) )
		{
			username = "Not Logged In";
		}

		super.setTitle( lastTitle + " (" + username + ")" );
	}

	public boolean useSidePane()
	{
		return false;
	}

	public JToolBar getToolbar()
	{
		return getToolbar( false );
	}

	public JToolBar getToolbar( final boolean force )
	{
		JToolBar toolbarPanel = null;

		if ( force )
		{
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
            framePanel.add( toolbarPanel, BorderLayout.NORTH );
			toolbarPanel.setFloatable( false );
			return toolbarPanel;
		}

		switch ( Preferences.getInteger( "toolbarPosition" ) )
		{
		case 1:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
            framePanel.add( toolbarPanel, BorderLayout.NORTH );
			break;

		case 2:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
            framePanel.add( toolbarPanel, BorderLayout.SOUTH );
			break;

		case 3:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
            framePanel.add( toolbarPanel, BorderLayout.WEST );
			break;

		case 4:
			toolbarPanel = new JToolBar( "KoLmafia Toolbar", JToolBar.VERTICAL );
            framePanel.add( toolbarPanel, BorderLayout.EAST );
			break;

		default:

			toolbarPanel = new JToolBar( "KoLmafia Toolbar" );
			if ( this instanceof LoginFrame || this instanceof ChatFrame )
			{
                framePanel.add( toolbarPanel, BorderLayout.NORTH );
				break;
			}
		}

		if ( toolbarPanel != null )
		{
			toolbarPanel.setFloatable( false );
		}

		return toolbarPanel;
	}

	/**
	 * Overrides the default behavior of dispose so that the frames are removed from the internal list of existing
	 * frames. Also allows for automatic exit.
	 */

	@Override
	public void dispose()
	{
		StaticEntity.unregisterPanels( this );

		if ( isVisible() )
		{
            rememberPosition();
            setVisible( false );
		}

		// Determine which frame needs to be removed from
		// the maintained list of frames.

		if ( exists )
		{
            exists = false;
			--GenericFrame.existingFrameCount;
			KoLConstants.existingFrames.remove( getFrameName() );
		}

		// Remove listeners from interface elements

        removeHotKeys();

		if ( listenerMap != null )
		{
			Object[] entries = listenerMap.entrySet().toArray();

			for ( int i = 0; i < entries.length; ++i )
			{
				Entry entry = (Entry) entries[ i ];

				JComponent component = (JComponent) entry.getKey();

				WeakReference reference = (WeakReference) entry.getValue();

				ActionListener listener = (ActionListener) reference.get();

				if ( listener != null )
				{
                    removeActionListener( component, listener );
				}
			}
		}

		if ( refreshListener != null )
		{
			KoLCharacter.removeCharacterListener( refreshListener );
		}

        menuBar.dispose();

		super.dispose();
        checkForLogout();
	}

	public boolean exists()
	{
		return exists;
	}

	public static boolean instanceExists()
	{
		return GenericFrame.existingFrameCount != 0;
	}

	protected void checkForLogout()
	{
		if ( StaticEntity.getClient() instanceof KoLmafiaGUI && !GenericFrame.instanceExists() )
		{
			RequestThread.runInParallel( new LogoutRunnable() );
		}
	}

	@Override
	public String toString()
	{
		return lastTitle;
	}

	public String getLastTitle()
	{
		return lastTitle;
	}

	public String getFrameName()
	{
		return frameName;
	}

	/**
	 * Method which adds a compact pane to the west side of the component. Note that this method can only be used if the
	 * KoLFrame on which it is called has not yet added any components. If there are any added components, this method
	 * will do nothing.
	 */

	public void addCompactPane()
	{
		if ( sidepane != null )
		{
			return;
		}

        sidepane = new CompactSidePane();
        sidepane.run();

        refreshListener = new KoLCharacterAdapter( sidepane );
		KoLCharacter.addCharacterListener( refreshListener );

        sidepane.setBackground( KoLConstants.ENABLED_COLOR );
        framePanel.add( sidepane, BorderLayout.WEST );
	}

	public void addScriptPane()
	{
		int scriptButtons = Preferences.getInteger( "scriptButtonPosition" );
		String[] scriptList = Preferences.getString( "scriptList" ).split( " \\| " );

		if ( scriptButtons == 0 || scriptList.length == 0 )
		{
			return;
		}

		JToolBar scriptBar;

		if ( scriptButtons == 1 )
		{
			scriptBar = getToolbar();
		}
		else
		{
			scriptBar = new JToolBar( SwingConstants.VERTICAL );
			scriptBar.setFloatable( false );
		}

		for ( int i = 0; i < scriptList.length; ++i )
		{
			scriptBar.add( new LoadScriptButton( i + 1, scriptList[ i ] ) );
		}

		if ( scriptButtons == 2 )
		{
            framePanel.add( scriptBar, BorderLayout.EAST );
		}
	}

	public void setStatusMessage( final String message )
	{
	}

	public void updateDisplayState( final MafiaState displayState )
	{
		// Change the background of the frame based on
		// the current display state -- but only if the
		// compact pane has already been constructed.

		Color color;
		boolean enabled;

		switch ( displayState )
		{
		case ABORT:
		case ERROR:
			color = KoLConstants.ERROR_COLOR;
			enabled = true;
			break;

		case ENABLE:
			color = KoLConstants.ENABLED_COLOR;
			enabled = true;
			break;

		default:
			color = KoLConstants.DISABLED_COLOR;
			enabled = false;
			break;
		}

		if ( sidepane != null )
		{
            sidepane.setBackground( color );
		}

        setEnabled( enabled );
	}

	/**
	 * Overrides the default isEnabled() method, because the setEnabled() method does not call the superclass's version.
	 *
	 * @return <code>true</code>
	 */

	@Override
	public final boolean isEnabled()
	{
		return true;
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
	}

	@Override
	public void processWindowEvent( final WindowEvent e )
	{
		if ( isVisible() && e.getID() == WindowEvent.WINDOW_CLOSING )
		{
            rememberPosition();
		}

		super.processWindowEvent( e );

		if ( e.getID() == WindowEvent.WINDOW_ACTIVATED )
		{
			InputFieldUtilities.setActiveWindow( this );
		}
	}

	@Override
	public void setVisible( final boolean isVisible )
	{
		if ( isVisible )
		{
            restorePosition();
		}
		else
		{
            rememberPosition();
		}

		if ( !isVisible )
		{
			super.setVisible( false );
			return;
		}

		try
		{
			if ( SwingUtilities.isEventDispatchThread() )
			{
                run();
			}
			else
			{
				SwingUtilities.invokeAndWait( this );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public void run()
	{
		super.setVisible( true );
		super.setExtendedState( Frame.NORMAL );
		super.repaint();
	}

	@Override
	public void pack()
	{
		if ( !( this instanceof ChatFrame ) && !packedOnce )
		{
			super.pack();
			packedOnce = true;
		}
	}

	private void rememberPosition()
	{
		Point p = getLocation();

		if ( tabs == null )
		{
			Preferences.setString( frameName, (int) p.getX() + "," + (int) p.getY() );
		}
		else
		{
			Preferences.setString(
                    frameName, (int) p.getX() + "," + (int) p.getY() + "," + tabs.getSelectedIndex() );
		}
	}

	private void restorePosition()
	{
		int xLocation = 0;
		int yLocation = 0;

		Dimension screenSize = KoLConstants.TOOLKIT.getScreenSize();
		String position = Preferences.getString( frameName );

		if ( position == null || !position.contains( "," ) )
		{
            setLocationRelativeTo( null );

			if ( !( this instanceof OptionsFrame ) && !( this instanceof KoLDesktop ) && tabs != null && tabs.getTabCount() > 0 )
			{
                tabs.setSelectedIndex( 0 );
			}

			return;
		}

		String[] location = position.split( "," );
		xLocation = StringUtilities.parseInt( location[ 0 ] );
		yLocation = StringUtilities.parseInt( location[ 1 ] );

		if ( xLocation > 0 && yLocation > 0 && xLocation < screenSize.getWidth() && yLocation < screenSize.getHeight() )
		{
            setLocation( xLocation, yLocation );
		}
		else
		{
            setLocationRelativeTo( null );
		}

		if ( location.length > 2 && tabs != null )
		{
			int tabIndex = StringUtilities.parseInt( location[ 2 ] );

			if ( tabIndex >= 0 && tabIndex < tabs.getTabCount() )
			{
                tabs.setSelectedIndex( tabIndex );
			}
			else if ( tabs.getTabCount() > 0 )
			{
                tabs.setSelectedIndex( 0 );
			}
		}
	}

	public static void createDisplay( final Class frameClass )
	{
		KoLmafiaGUI.constructFrame( frameClass );
	}

	public static void createDisplay( final Class frameClass, final Object[] parameters )
	{
		CreateFrameRunnable creator = new CreateFrameRunnable( frameClass, parameters );
		creator.run();
	}

	public static void compileScripts()
	{
		GenericFrame.compileScripts( Preferences.getInteger( "scriptMRULength" ) > 0 );
	}

	public static void compileScripts( final boolean useMRUlist )
	{
		KoLConstants.scripts.clear();

		// Get the list of files in the current directory or build from MRU

		File [] scriptList = useMRUlist ?
			KoLConstants.scriptMList.listAsFiles() :
			DataUtilities.listFiles( KoLConstants.SCRIPT_LOCATION );

		// Iterate through the files. Do this in two
		// passes to make sure that directories start
		// up top, followed by non-directories.

		int directoryIndex = 0;

		for ( int i = 0; i < scriptList.length; ++i )
		{
			if ( !ScriptMenu.shouldAddScript( scriptList[ i ] ) )
			{
			}
			else if ( scriptList[ i ].isDirectory() )
			{
				KoLConstants.scripts.add( directoryIndex++ , scriptList[ i ] );
			}
			else
			{
				KoLConstants.scripts.add( scriptList[ i ] );
			}
		}
	}

	/**
	 * Utility method to save the entire list of bookmarks to the settings file. This should be called after every
	 * update.
	 */

	public static void saveBookmarks()
	{
		StringBuilder bookmarkData = new StringBuilder();

		for ( int i = 0; i < KoLConstants.bookmarks.getSize(); ++i )
		{
			if ( i > 0 )
			{
				bookmarkData.append( '|' );
			}
			bookmarkData.append( (String) KoLConstants.bookmarks.getElementAt( i ) );
		}

		Preferences.setString( "browserBookmarks", bookmarkData.toString() );
	}

	/**
	 * Utility method to compile the list of bookmarks based on the current settings.
	 */

	public static void compileBookmarks()
	{
		KoLConstants.bookmarks.clear();
		String[] bookmarkData = Preferences.getString( "browserBookmarks" ).split( "\\|" );

		if ( bookmarkData.length > 1 )
		{
			for ( int i = 0; i < bookmarkData.length; ++i )
			{
				KoLConstants.bookmarks.add( bookmarkData[ i ] + "|" + bookmarkData[ ++i ] + "|" + bookmarkData[ ++i ] );
			}
		}
	}

	private static class FramePanel
		extends JPanel
		implements FocusListener
	{
		private Component centerComponent;

		public FramePanel()
		{
			super( new BorderLayout() );
            addFocusListener( this );
		}

		@Override
		public void add( Component c, Object constraint )
		{
			super.add( c, constraint );

			if ( constraint == BorderLayout.CENTER )
			{
                centerComponent = c;

                setFocusCycleRoot( true );
                setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( c ) );
			}
		}

		public void focusGained( FocusEvent e )
		{
			if ( centerComponent != null )
			{
                centerComponent.requestFocus();
			}
		}

		public void focusLost( FocusEvent e )
		{
		}
	}

	private static class LogoutRunnable
		implements Runnable
	{
		public void run()
		{
			LogoutManager.logout();
		}
	}
}
