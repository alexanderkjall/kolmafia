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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.CreateItemRequest;

import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.utilities.LowerCaseEntry;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class AutoFilterTextField
	extends AutoHighlightTextField
	implements ActionListener, ListElementFilter
{
	protected JList list;
	protected String text;
	protected LockableListModel model;
	protected boolean strict;
	protected int quantity;
	protected int price;
	protected boolean qtyChecked;
	protected boolean qtyEQ, qtyLT, qtyGT;
	protected boolean asChecked;
	protected boolean asEQ, asLT, asGT;
	protected boolean notChecked;

	private FilterDelayThread thread;

	private static final Pattern QTYSEARCH_PATTERN = Pattern.compile(
		"\\s*#\\s*([<=>]+)\\s*([\\d,]+)\\s*" );

	private static final Pattern ASSEARCH_PATTERN = Pattern.compile(
		"\\s*\\p{Sc}\\s*([<=>]+)\\s*([\\d,]+)\\s*" );

	private static final Pattern NOTSEARCH_PATTERN = Pattern.compile(
		"\\s*!\\s*=\\s*(.+)\\s*" );

	public AutoFilterTextField( final JList list )
	{
        setList( list );

        addKeyListener( new FilterListener() );
		
		// Make this look like a normal search field on OS X.
		// Note that the field MUST NOT be forced to a height other than its
		// preferred height; that produces some ugly visual glitches.

        putClientProperty( "JTextField.variant", "search" );
	}

	public AutoFilterTextField( final JList list, Object initial )
	{
        setList( list );

        addKeyListener( new FilterListener() );

		// Make this look like a normal search field on OS X.
		// Note that the field MUST NOT be forced to a height other than its
		// preferred height; that produces some ugly visual glitches.

        putClientProperty( "JTextField.variant", "search" );

		if ( initial != null )
		{
            setText( initial.toString() );
		}
	}

	public AutoFilterTextField( LockableListModel displayModel )
	{
        addKeyListener( new FilterListener() );

        model = displayModel;
        model.setFilter( this );

		// Make this look like a normal search field on OS X.
		// Note that the field MUST NOT be forced to a height other than its
		// preferred height; that produces some ugly visual glitches.

        putClientProperty( "JTextField.variant", "search" );
	}

	public void setList( final JList list )
	{
		this.list = list;
        model = (LockableListModel) list.getModel();
        model.setFilter( this );
		this.list.clearSelection();
	}

	public void actionPerformed( final ActionEvent e )
	{
        prepareUpdate();
	}

	@Override
	public void setText( final String text )
	{
		super.setText( text );
        prepareUpdate();
	}

	public boolean isVisible( final Object element )
	{
		if ( qtyChecked )
		{
			int qty = AutoFilterTextField.getResultQuantity( element );
			if ( ( qty == quantity && !qtyEQ) ||
			     ( qty < quantity && !qtyLT) ||
			     ( qty > quantity && !qtyGT) )
			{
				return false;
			}
		}

		if ( asChecked )
		{
			int as = AutoFilterTextField.getResultPrice( element );
			if ( ( as == price && !asEQ) ||
			     ( as < price && !asLT) ||
			     ( as > price && !asGT) )
			{
				return false;
			}
		}

		if ( text == null || text.length() == 0 )
		{
			return true;
		}

		// If it's not a result, then check to see if you need to
		// filter based on its string form.

		String elementName = AutoFilterTextField.getResultName( element );

		if ( notChecked )
		{
			return !elementName.contains( text );
		}

		return strict ? elementName.contains( text ) :
			StringUtilities.fuzzyMatches( elementName, text );
	}

	public static String getResultName( final Object element )
	{
		if ( element == null )
		{
			return "";
		}

		if ( element instanceof AdventureResult )
		{
			return ( (AdventureResult) element ).getName().toLowerCase();
		}
		if ( element instanceof CreateItemRequest )
		{
			return ( (CreateItemRequest) element ).getName().toLowerCase();
		}
		if ( element instanceof Concoction )
		{
			return ( (Concoction) element ).getName().toLowerCase();
		}
		if ( element instanceof SoldItem )
		{
			return ( (SoldItem) element ).getItemName().toLowerCase();
		}
		if ( element instanceof LowerCaseEntry )
		{
			return ( (LowerCaseEntry) element ).getLowerCase();
		}
		if ( element instanceof KoLAdventure )
		{
			return ( (KoLAdventure) element ).toLowerCaseString();
		}
		if ( element instanceof Monster )
		{
			return ( (Monster) element ).toLowerCaseString();
		}

		return element.toString();
	}

	public static int getResultPrice( final Object element )
	{
		if ( element == null )
		{
			return -1;
		}

		if ( element instanceof AdventureResult )
		{
			return ItemDatabase.getPriceById( ( (AdventureResult) element ).getItemId() );
		}

		return -1;
	}


	public static int getResultQuantity( final Object element )
	{
		if ( element == null )
		{
			return -1;
		}

		if ( element instanceof AdventureResult )
		{
			return ( (AdventureResult) element ).getCount();
		}
		if ( element instanceof CreateItemRequest )
		{
			return ( (CreateItemRequest) element ).getQuantityPossible();
		}
		if ( element instanceof Concoction )
		{
			return ( (Concoction) element ).getAvailable();
		}
		if ( element instanceof SoldItem )
		{
			return ( (SoldItem) element ).getQuantity();
		}
		if ( element instanceof LowerCaseEntry )
		{	// no meaningful integer fields
			return -1;
		}
		if ( element instanceof KoLAdventure )
		{
			return StringUtilities.parseInt( ( (KoLAdventure) element ).getAdventureId() );
		}

		return -1;
	}

	public synchronized void update()
	{
		try
		{
            model.setFiltering( true );

            qtyChecked = false;
            asChecked = false;
            notChecked = false;
            text = getText().toLowerCase();

			Matcher mqty = AutoFilterTextField.QTYSEARCH_PATTERN.matcher( text );
			if ( mqty.find() )
			{
                qtyChecked = true;
                quantity = StringUtilities.parseInt( mqty.group( 2 ) );

				String op = mqty.group( 1 );

                qtyEQ = op.contains( "=" );
                qtyLT = op.contains( "<" );
                qtyGT = op.contains( ">" );
                text = mqty.replaceFirst( "" );
			}

			Matcher mas = AutoFilterTextField.ASSEARCH_PATTERN.matcher( text );
			if ( mas.find() )
			{
                asChecked = true;
                price = StringUtilities.parseInt( mas.group( 2 ) );

				String op = mas.group( 1 );

                asEQ = op.contains( "=" );
                asLT = op.contains( "<" );
                asGT = op.contains( ">" );
                text = mas.replaceFirst( "" );
			}

			Matcher mnot = AutoFilterTextField.NOTSEARCH_PATTERN.matcher( text );
			if ( mnot.find() )
			{
                notChecked = true;
                text = mnot.group( 1 );
			}

            strict = true;
            model.updateFilter( false );

			if ( model.getSize() == 0 )
			{
                strict = false;
                model.updateFilter( false );
			}

			if ( list != null )
			{
				if ( model.getSize() == 1 )
				{
                    list.setSelectedIndex( 0 );
				}
				else if ( list.getSelectedIndices().length != 1 )
				{
                    list.clearSelection();
				}
			}
		}
		finally
		{
            model.setFiltering( false );

			if ( model.size() > 0 )
			{
                model.fireContentsChanged(
                        model, 0, model.size() - 1 );
			}
		}
	}

	public synchronized void prepareUpdate()
	{
		if ( thread != null )
		{
            thread.prepareUpdate();
			return;
		}

        thread = new FilterDelayThread();

        thread.start();
	}

	private class FilterListener
		extends KeyAdapter
	{
		@Override
		public void keyReleased( final KeyEvent e )
		{
            prepareUpdate();
		}
	}

	private class FilterDelayThread
		extends Thread
	{
		private boolean updating = true;

		@Override
		public void run()
		{
			PauseObject pauser = new PauseObject();

			while ( updating )
			{
                updating = false;
				pauser.pause( 100 );
			}

			SwingUtilities.invokeLater( new FilterRunnable() );
		}

		public void prepareUpdate()
		{
            updating = true;
		}
	}

	private class FilterRunnable
		implements Runnable
	{
		public void run()
		{
            thread = null;

            update();
		}
	}
}
