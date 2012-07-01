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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class ScrollablePanel
	extends ActionPanel
{
	protected ConfirmedListener CONFIRM_LISTENER = new ConfirmedListener();
	protected CancelledListener CANCEL_LISTENER = new CancelledListener();

	public JPanel actualPanel;
	public JPanel centerPanel;

	public JPanel eastPanel;
	public VerifyButtonPanel buttonPanel;
	public JComponent scrollComponent;
	public JLabel titleComponent;
	public GenericScrollPane scrollPane;

	public ScrollablePanel( final String title, final JComponent scrollComponent )
	{
		this( title, null, null, scrollComponent );
	}

	public ScrollablePanel( final String title, final String confirmedText, final String cancelledText,
		final JComponent scrollComponent )
	{
		this( title, confirmedText, cancelledText, scrollComponent, true );
	}

	public ScrollablePanel( final String title, final String confirmedText, final String cancelledText,
		final JComponent scrollComponent, final boolean isRootPane )
	{
		this.scrollComponent = scrollComponent;

        centerPanel = new JPanel( new BorderLayout() );

		if ( !title.equals( "" ) )
		{
            titleComponent = JComponentUtilities.createLabel(
				title, SwingConstants.CENTER, Color.black, Color.white );
            centerPanel.add( titleComponent, BorderLayout.NORTH );
		}

        scrollPane = new GenericScrollPane( scrollComponent );
        centerPanel.add( scrollPane, BorderLayout.CENTER );
        actualPanel = new JPanel( new BorderLayout( 20, 10 ) );
        actualPanel.add( centerPanel, BorderLayout.CENTER );

        eastPanel = new JPanel( new BorderLayout() );

		if ( confirmedText != null )
		{
            buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText, cancelledText, CONFIRM_LISTENER, CANCEL_LISTENER );
            buttonPanel.setBothDisabledOnClick( true );

            eastPanel.add( buttonPanel, BorderLayout.NORTH );
            actualPanel.add( eastPanel, BorderLayout.EAST );
		}

		JPanel containerPanel = new JPanel( new CardLayout( 10, 10 ) );
		containerPanel.add( actualPanel, "" );

		if ( isRootPane )
		{
            getContentPane().setLayout( new BorderLayout() );
            getContentPane().add( containerPanel, BorderLayout.CENTER );
		}
		else
		{
            setLayout( new BorderLayout() );
            add( containerPanel, BorderLayout.CENTER );
		}

		( (JPanel) getContentPane() ).setOpaque( true );
		StaticEntity.registerPanel( this );

        contentSet = true;
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		if ( scrollComponent == null || buttonPanel == null )
		{
			return;
		}

        scrollComponent.setEnabled( isEnabled );
        buttonPanel.setEnabled( isEnabled );
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
	public void dispose()
	{
		if ( buttonPanel != null )
		{
            buttonPanel.dispose();
		}
	}

	private class ConfirmedListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( contentSet )
			{
                actionConfirmed();
			}
		}
	}

	private class CancelledListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( contentSet )
			{
                actionCancelled();
			}
		}
	}
}
