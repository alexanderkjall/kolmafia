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
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.swingui.panel.StatusEffectPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SkillBuffFrame
	extends GenericFrame
{
	private LockableListModel contacts;

	private SkillTypeComboBox typeSelect;
	private AutoFilterComboBox skillSelect;
	private AutoHighlightTextField amountField;
	private AutoFilterComboBox targetSelect;
	private final ShowDescriptionList effectList;

	public SkillBuffFrame()
	{
		this( "" );
	}

	public SkillBuffFrame( final String recipient )
	{
		super( "Skill Casting" );

		JPanel skillWrapper = new JPanel( new BorderLayout() );
		skillWrapper.add( new SkillBuffPanel(), BorderLayout.NORTH );

        effectList = new ShowDescriptionList( KoLConstants.activeEffects, 12 );
        effectList.addListSelectionListener( new SkillReselector() );

        tabs.addTab( "Active Effects", new StatusEffectPanel( effectList ) );
        tabs.addTab( "Recovery Items", new RestorativeItemPanel() );

		skillWrapper.add( tabs, BorderLayout.CENTER );

        setCenterComponent( skillWrapper );

        setRecipient( recipient );
	}

	public void setRecipient( String recipient )
	{
		if ( !contacts.contains( recipient ) )
		{
			recipient = ContactManager.getPlayerName( recipient );
            contacts.add( 0, recipient );
		}

        targetSelect.getEditor().setItem( recipient );
        targetSelect.setSelectedItem( recipient );
	}

	private class SkillReselector
		implements ListSelectionListener
	{
		public void valueChanged( final ListSelectionEvent e )
		{
			AdventureResult effect = (AdventureResult) effectList.getSelectedValue();
			if ( effect == null )
			{
				return;
			}

            skillSelect.setSelectedItem( UseSkillRequest.getInstance( UneffectRequest.effectToSkill( effect.getName() ) ) );
		}
	}

	private class SkillBuffPanel
		extends GenericPanel
	{
		public SkillBuffPanel()
		{
			super( "cast", "maxcast", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

            typeSelect = new SkillTypeComboBox();
            skillSelect = new AutoFilterComboBox( KoLConstants.usableSkills, false );
            amountField = new AutoHighlightTextField();

            contacts = (LockableListModel) ContactManager.getMailContacts().getMirrorImage();
            targetSelect = new AutoFilterComboBox( contacts, true );

			VerifiableElement[] elements = new VerifiableElement[ 4 ];
			elements[ 0 ] = new VerifiableElement( "Skill Type: ", typeSelect );
			elements[ 1 ] = new VerifiableElement( "Skill Name: ", skillSelect );
			elements[ 2 ] = new VerifiableElement( "# of Casts: ", amountField );
			elements[ 3 ] = new VerifiableElement( "The Victim: ", targetSelect );

            setContent( elements );
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( skillSelect == null || targetSelect == null )
			{
				return;
			}

			super.setEnabled( isEnabled );

            skillSelect.setEnabled( isEnabled );
            targetSelect.setEnabled( isEnabled );
		}

		@Override
		public void actionConfirmed()
		{
            buff( false );
		}

		@Override
		public void actionCancelled()
		{
            buff( true );
		}

		private void buff( boolean maxBuff )
		{
			UseSkillRequest request = (UseSkillRequest) skillSelect.getSelectedItem();
			if ( request == null )
			{
				return;
			}

			String buffName = request.getSkillName();
			if ( buffName == null )
			{
				return;
			}

			String[] targets =
				StaticEntity.getClient().extractTargets( (String) targetSelect.getSelectedItem() );

			int buffCount = !maxBuff ? InputFieldUtilities.getValue( amountField, 1 ) : Integer.MAX_VALUE;
			if ( buffCount == 0 )
			{
				return;
			}

			SpecialOutfit.createImplicitCheckpoint();

			if ( targets.length == 0 )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( buffName, KoLCharacter.getUserName(), buffCount ) );
			}
			else
			{
				for ( int i = 0; i < targets.length && KoLmafia.permitsContinue(); ++i )
				{
					if ( targets[ i ] != null )
					{
						RequestThread.postRequest( UseSkillRequest.getInstance( buffName, targets[ i ], buffCount ) );
					}
				}
			}

			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	private class SkillTypeComboBox
		extends JComboBox
	{
		public SkillTypeComboBox()
		{
			super();
			addItem( "All Castable Skills" );
			addItem( "Summoning Skills" );
			addItem( "Remedies" );
			addItem( "Self-Only" );
			addItem( "Buffs" );
			addActionListener( new SkillTypeListener() );
		}

		private class SkillTypeListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				int index = getSelectedIndex();
				switch ( index )
				{
				case 0:
					// All skills
                    skillSelect.setModel( KoLConstants.usableSkills );
					break;
				case 1:
					// Summoning skills
                    skillSelect.setModel( KoLConstants.summoningSkills );
					break;
				case 2:
					// Remedy skills
                    skillSelect.setModel( KoLConstants.remedySkills );
					break;
				case 3:
					// Self-only skills
                    skillSelect.setModel( KoLConstants.selfOnlySkills );
					break;
				case 4:
					// Buff skills
                    skillSelect.setModel( KoLConstants.buffSkills );
					break;
				}
			}
		}
	}
}
