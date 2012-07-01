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

import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class UseItemDequeuePanel
	extends ItemManagePanel
{
	private final JTabbedPane queueTabs;
	private final boolean food, booze, spleen;

	public UseItemDequeuePanel( final boolean food, final boolean booze, final boolean spleen )
	{
		super( ConcoctionDatabase.getUsables(), false, false );
		// Remove the default borders inherited from ScrollablePanel.
		BorderLayout a = (BorderLayout) actualPanel.getLayout();
		a.setVgap( 0 );
		CardLayout b = (CardLayout) actualPanel.getParent().getLayout();
		b.setVgap( 0 );

		// Add a 10px top border.
        northPanel.add( Box.createVerticalStrut( 10 ), BorderLayout.NORTH );

		this.food = food;
		this.booze = booze;
		this.spleen = spleen;

        queueTabs = KoLmafiaGUI.getTabbedPane();

		if ( this.food )
		{
            queueTabs.addTab( "0 Full Queued", centerPanel );
		}
		else if ( this.booze )
		{
            queueTabs.addTab( "0 Drunk Queued", centerPanel );
		}
		else if ( this.spleen )
		{
            queueTabs.addTab( "0 Spleen Queued", centerPanel );
		}

        queueTabs.addTab( "Resources Used", new GenericScrollPane( ConcoctionDatabase.getQueuedIngredients( this.food, this.booze, this.spleen ), 7 ) );

		JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

        elementList.setCellRenderer( ListCellRendererFactory.getCreationQueueRenderer() );
        elementList.setFixedCellHeight( (int) (test.getPreferredSize().getHeight() * 2.5f) );

        elementList.setVisibleRowCount( 3 );
        elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

        actualPanel.add( queueTabs, BorderLayout.CENTER );

        setButtons( false, new ActionListener[] { new ConsumeListener(), new CreateListener() } );

        eastPanel.add( new ThreadedButton( "undo", new UndoQueueRunnable() ), BorderLayout.SOUTH );

        setEnabled( true );
        filterItems();
	}

	public JTabbedPane getQueueTabs()
	{
		return queueTabs;
	}

	@Override
	public AutoFilterTextField getWordFilter()
	{
		return new ConsumableFilterField();
	}

	private class ConsumeListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			ConcoctionDatabase.handleQueue( food, booze, spleen, KoLConstants.CONSUME_USE );

			if ( food )
			{
                queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( booze )
			{
                queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( spleen )
			{
                queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		@Override
		public String toString()
		{
			return "consume";
		}
	}

	private class CreateListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			ConcoctionDatabase.handleQueue( food, booze, spleen, KoLConstants.NO_CONSUME );

			if ( food )
			{
                queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( booze )
			{
                queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( spleen )
			{
                queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		@Override
		public String toString()
		{
			return "create";
		}
	}

	private class UndoQueueRunnable
		implements Runnable
	{
		public void run()
		{
			ConcoctionDatabase.pop( food, booze, spleen );
			ConcoctionDatabase.refreshConcoctions( true );

			if ( food )
			{
                queueTabs.setTitleAt(
                        0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( booze )
			{
                queueTabs.setTitleAt(
                        0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( spleen )
			{
                queueTabs.setTitleAt(
                        0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private class ConsumableFilterField
		extends FilterItemField
	{
		@Override
		public boolean isVisible( final Object element )
		{
			Concoction creation = (Concoction) element;

			if ( creation.getQueued() == 0 )
			{
				return false;
			}

			if ( ItemDatabase.getFullness( creation.getName() ) > 0 )
			{
				return UseItemDequeuePanel.this.food && super.isVisible( element );
			}

			if ( ItemDatabase.getInebriety( creation.getName() ) > 0 )
			{
				return UseItemDequeuePanel.this.booze && super.isVisible( element );
			}

			if ( ItemDatabase.getSpleenHit( creation.getName() ) > 0 )
			{
				return spleen && super.isVisible( element );
			}

			switch ( ItemDatabase.getConsumptionType( creation.getName() ) )
			{
			case KoLConstants.CONSUME_FOOD_HELPER:
				if ( UseItemDequeuePanel.this.food )
				{
					return true;
				}
				break;

			case KoLConstants.CONSUME_DRINK_HELPER:
				if ( UseItemDequeuePanel.this.booze )
				{
					return true;
				}
				break;

			case KoLConstants.CONSUME_MULTIPLE:
				if ( UseItemDequeuePanel.this.food &&
				     creation.getItemId() == ItemPool.MUNCHIES_PILL )
				{
					return true;
				}
				break;

			case KoLConstants.CONSUME_USE:
				if ( UseItemDequeuePanel.this.food &&
				     creation.getItemId() == ItemPool.DISTENTION_PILL )
				{
					return true;
				}
				break;
			}

			return false;
		}
	}
}
