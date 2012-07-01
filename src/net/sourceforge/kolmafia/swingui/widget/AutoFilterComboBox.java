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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JComboBox;

import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AutoFilterComboBox
	extends JComboBox
	implements ListElementFilter
{
	private int currentIndex = -1;
	private boolean isRecentFocus = false;

	private String currentName;
	private String matchString;
	public Object currentMatch;
	private LockableListModel model;
	private boolean allowAdditions;

	private boolean active, strict;
	private final JTextComponent editor;

	public AutoFilterComboBox( final LockableListModel model, final boolean allowAdditions )
	{
        setModel( model );

        setEditable( true );

		this.allowAdditions = allowAdditions;
		NameInputListener listener = new NameInputListener();

        addItemListener( listener );
        editor = (JTextComponent) getEditor().getEditorComponent();

        editor.addFocusListener( listener );
        editor.addKeyListener( listener );
	}

	public String getText()
	{
		return (String) (getSelectedItem() != null ? getSelectedItem() : currentMatch);
	}

	public void setText( final String text )
	{
		if ( model.indexOf( text ) != -1 )
		{
            setSelectedItem( text );
		}
		else
		{
            setSelectedItem( null );
            currentMatch = text;
            editor.setText( text );
		}
	}

	public void setModel( final LockableListModel model )
	{
		super.setModel( model );
		this.model = model;
		this.model.setFilter( this );
	}

	public void forceAddition()
	{
		if ( currentName == null || currentName.length() == 0 )
		{
			return;
		}

		if ( currentMatch == null && allowAdditions && !model.contains( currentName ) )
		{
            model.add( currentName );
		}

        setSelectedItem( currentName );
	}

	private void update()
	{
		if ( currentName == null )
		{
			return;
		}

        isRecentFocus = false;
        currentIndex = -1;
        model.setSelectedItem( null );

        active = true;
        matchString = currentName.toLowerCase();

        strict = true;
        model.updateFilter( false );

		if ( model.getSize() > 0 )
		{
			return;
		}

        strict = false;
        model.updateFilter( false );
	}

	public synchronized void findMatch( final int keyCode )
	{
        currentName = getEditor().getItem().toString();
		int caretPosition = editor.getCaretPosition();

		if ( !allowAdditions && model.contains( currentName ) )
		{
            setSelectedItem( currentName );
			return;
		}

        currentMatch = null;
        update();

		if ( allowAdditions )
		{
			if ( model.getSize() != 1 || keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE )
			{
                editor.setText( currentName );
                editor.setCaretPosition( caretPosition );
				return;
			}

            currentMatch = model.getElementAt( 0 );
            matchString = currentMatch.toString().toLowerCase();

            editor.setText( currentMatch.toString() );
            editor.moveCaretPosition( caretPosition );
			return;
		}

        editor.setText( currentName );
		if ( !isPopupVisible() )
		{
            showPopup();
		}
	}

	public boolean isVisible( final Object element )
	{
		if ( !active )
		{
			return true;
		}

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		if ( matchString == null || matchString.length() == 0 )
		{
			return true;
		}

		String elementName = element.toString().toLowerCase();
		return allowAdditions ? elementName.startsWith( matchString ) : strict ? elementName.contains( matchString ) : StringUtilities.fuzzyMatches(
			elementName, matchString );
	}

	private class NameInputListener
		extends KeyAdapter
		implements FocusListener, ItemListener
	{
		@Override
		public void keyReleased( final KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				if ( !isRecentFocus && currentIndex + 1 < model.getSize() )
				{
                    currentMatch =
                            model.getElementAt( ++currentIndex );
				}

                isRecentFocus = false;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				if ( !isRecentFocus && model.getSize() > 0 && currentIndex > 0 )
				{
                    currentMatch =
                            model.getElementAt( --currentIndex );
				}

                isRecentFocus = false;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB )
			{
                focusLost( null );
			}
			else if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
			{
                findMatch( e.getKeyCode() );
			}
		}

		public final void itemStateChanged( final ItemEvent e )
		{
            currentMatch = getSelectedItem();

			if ( currentMatch == null )
			{
				return;
			}

            currentName = currentMatch.toString();

			if ( !isPopupVisible() )
			{
                active = false;
                model.updateFilter( false );
			}
		}

		public final void focusGained( final FocusEvent e )
		{
            getEditor().selectAll();

            isRecentFocus = true;
            currentIndex = model.getSelectedIndex();
		}

		public final void focusLost( final FocusEvent e )
		{
			if ( currentMatch != null )
			{
                setSelectedItem( currentMatch );
			}
			else if ( currentName != null && currentName.trim().length() != 0 )
			{
                forceAddition();
			}
		}
	}
}
