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

import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.FocusEvent;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

import net.sourceforge.kolmafia.chat.ChatFormatter;
import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;

public class ChatFrame
	extends GenericFrame
{
	protected static final GenericRequest PROFILER = new GenericRequest( "" );
	private ChatPanel mainPanel;
	private JComboBox nameClickSelect;

	/**
	 * Constructs a new <code>ChatFrame</code> which is intended to be used for instant messaging to the specified
	 * contact.
	 */

	public ChatFrame( final String associatedContact )
	{
		super(
			associatedContact == null || associatedContact.equals( "" ) ?
			"Loathing Chat" :
			associatedContact.startsWith( "/" ) ?
			"Chat: " + associatedContact :
			"Chat PM: " + associatedContact );

		this.initialize( associatedContact );

		// Add the standard chat options which the user
		// simply clicks on for functionality.

		JToolBar toolbarPanel = this.getToolbar();

		if ( toolbarPanel != null )
		{
			// Add the name click options as a giant combo
			// box, rather than a hidden menu.

			this.nameClickSelect = new JComboBox();
			this.nameClickSelect.addItem( "Name click shows player profile" );
			this.nameClickSelect.addItem( "Name click opens blue message" );
			this.nameClickSelect.addItem( "Name click sends kmail message" );
			this.nameClickSelect.addItem( "Name click opens trade request" );
			this.nameClickSelect.addItem( "Name click shows display case" );
			this.nameClickSelect.addItem( "Name click shows ascension history" );
			this.nameClickSelect.addItem( "Name click shows mall store" );
			this.nameClickSelect.addItem( "Name click performs /whois" );
			this.nameClickSelect.addItem( "Name click friends the player" );
			this.nameClickSelect.addItem( "Name click baleets the player" );
			toolbarPanel.add( this.nameClickSelect );

			this.nameClickSelect.setSelectedIndex( 0 );
		}

		// Set the default size so that it doesn't appear super-small
		// when it's first constructed

		this.setSize( new Dimension( 500, 300 ) );

		if ( this.mainPanel != null && associatedContact != null )
		{
			if ( associatedContact.startsWith( "/" ) )
			{
				this.setTitle( associatedContact );
			}
			else
			{
				this.setTitle( "private to " + associatedContact );
			}
		}

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.mainPanel ) );
	}

	@Override
	public void focusGained( FocusEvent e )
	{
		if ( this.mainPanel != null )
		{
			this.mainPanel.requestFocusInWindow();
		}
	}

	@Override
	public void focusLost( FocusEvent e )
	{
	}

	@Override
	public JToolBar getToolbar()
	{
		if ( !Preferences.getBoolean( "useChatToolbar" ) )
		{
			return null;
		}

		JToolBar toolbarPanel = super.getToolbar( true );

		toolbarPanel.add( new InvocationButton( "/friends", "who2.gif", ChatManager.class, "checkFriends" ) );

		toolbarPanel.add( Box.createHorizontalStrut( 10 ) );

		toolbarPanel.add( new InvocationButton(
			"Add Highlighting", "highlight1.gif", ChatFormatter.class, "addHighlighting" ) );

		toolbarPanel.add( new InvocationButton(
			"Remove Highlighting", "highlight2.gif", ChatFormatter.class, "removeHighlighting" ) );

		return toolbarPanel;
	}

	@Override
	public Component getCenterComponent()
	{
		return this.getFramePanel();
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	@Override
	public boolean shouldAddStatusBar()
	{
		return false;
	}

	@Override
	public boolean showInWindowMenu()
	{
		return false;
	}

	@Override
	public void dispose()
	{
		String contact = this.getAssociatedContact();

		if ( contact != null && contact.equals( ChatManager.getCurrentChannel() ) )
		{
			contact = null;
		}

		ChatManager.closeWindow( contact );

		super.dispose();
	}

	/**
	 * Utility method called to initialize the frame. This method should be overridden, should a different means of
	 * initializing the content of the frame be needed.
	 */

	public void initialize( final String associatedContact )
	{
		this.mainPanel = new ChatPanel( associatedContact, PROFILER, this );
		this.setCenterComponent( this.mainPanel );
	}

	/**
	 * Returns the name of the contact associated with this frame.
	 * 
	 * @return The name of the contact associated with this frame
	 */

	public String getAssociatedContact()
	{
		return this.mainPanel == null ? null : this.mainPanel.getAssociatedContact();
	}

	/**
	 * Returns whether or not the chat frame has focus.
	 */

	@Override
	public boolean hasFocus()
	{
		return super.hasFocus() || this.mainPanel != null && this.mainPanel.hasFocus();
	}

    public JComboBox getNameClickSelect() {
        return nameClickSelect;
    }
}
