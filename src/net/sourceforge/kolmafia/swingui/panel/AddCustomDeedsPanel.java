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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

public class AddCustomDeedsPanel
	extends JPanel
{
	public static CardLayoutSelectorPanel selectorPanel;

	private JTextField commandField1;
	private JTextField commandField2;
	private JTextField commandField3;
	private JTextField commandField4;
	private JTextField itemField1;
	private JTextField itemField2;
	private JTextField itemField3;
	private JTextField itemField4;
	private JTextField skillField1;
	private JTextField skillField2;
	private JTextField skillField3;
	private JTextField skillField4;
	private JTextField textField;
	private JTextField simpleField1;
	private JTextField simpleField2;
	private JTextField simpleField3;

	private static final int COMMAND_FIELD_1 = 0;
	private static final int COMMAND_FIELD_2 = 1;
	private static final int COMMAND_FIELD_3 = 2;
	private static final int COMMAND_FIELD_4 = 3;
	private static final int ITEM_FIELD_1 = 4;
	private static final int ITEM_FIELD_2 = 5;
	private static final int ITEM_FIELD_3 = 6;
	private static final int ITEM_FIELD_4 = 7;
	private static final int SKILL_FIELD_1 = 8;
	private static final int SKILL_FIELD_2 = 9;
	private static final int SKILL_FIELD_3 = 10;
	private static final int SKILL_FIELD_4 = 11;
	private static final int TEXT_FIELD = 12;
	private static final int SIMPLE_FIELD_1 = 13;
	private static final int SIMPLE_FIELD_2 = 14;
	private static final int SIMPLE_FIELD_3 = 15;

	private JLabel commandLabel1;
	private JLabel commandLabel2;
	private JLabel commandLabel3;
	private JLabel commandLabel4;
	private JLabel itemLabel1;
	private JLabel itemLabel2;
	private JLabel itemLabel3;
	private JLabel itemLabel4;
	private JLabel skillLabel1;
	private JLabel skillLabel2;
	private JLabel skillLabel3;
	private JLabel skillLabel4;
	private JLabel simpleLabel1;
	private JLabel simpleLabel2;
	private JLabel simpleLabel3;

	private static final int COMMAND_LABEL_1 = 0;
	private static final int COMMAND_LABEL_2 = 1;
	private static final int COMMAND_LABEL_3 = 2;
	private static final int COMMAND_LABEL_4 = 3;
	private static final int ITEM_LABEL_1 = 4;
	private static final int ITEM_LABEL_2 = 5;
	private static final int ITEM_LABEL_3 = 6;
	private static final int ITEM_LABEL_4 = 7;
	private static final int SKILL_LABEL_1 = 8;
	private static final int SKILL_LABEL_2 = 9;
	private static final int SKILL_LABEL_3 = 10;
	private static final int SKILL_LABEL_4 = 11;
	private static final int SIMPLE_LABEL_1 = 12;
	private static final int SIMPLE_LABEL_2 = 13;
	private static final int SIMPLE_LABEL_3 = 14;

	private ThreadedButton commandButton;
	private ThreadedButton itemButton;
	private ThreadedButton skillButton;
	private ThreadedButton textDeedButton;
	private ThreadedButton addTextButton;
	private ThreadedButton simpleButton;

	private JTextArea textArea;
	private ArrayList textDeed = new ArrayList();

	public AddCustomDeedsPanel()
	{
		buildCustomDeed();
	}

	private void buildCustomDeed()
	{
		AddCustomDeedsPanel.selectorPanel = new CardLayoutSelectorPanel( "", "ABCDEFGHIJKLM" );
		AddCustomDeedsPanel.selectorPanel.addCategory( "Custom Deeds" );

		addSimpleDeed();
		addCommandDeed();
		addItemDeed();
		addSkillDeed();
		addTextDeed();

		AddCustomDeedsPanel.selectorPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
	}
	
	private void addSimpleDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

        simpleField1 = new JTextField( 25 );
        simpleField2 = new JTextField( 25 );
        simpleField3 = new JTextField( 25 );
        simpleLabel1 = new JLabel( "required" );
        simpleLabel1.setToolTipText( "The text to display on the button." );
        simpleLabel2 = new JLabel( "(optional)" );
        simpleLabel2.setToolTipText( "The command that the button will execute." );
        simpleLabel3 = new JLabel( "(optional)" );
        simpleLabel3
			.setToolTipText( "Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );

        simpleField1.getDocument().addDocumentListener( new SimpleField1Listener() );
        simpleField2.getDocument().addDocumentListener( new SimpleField2Listener() );
        simpleField3.getDocument().addDocumentListener( new SimpleField3Listener() );

        simpleButton = new ThreadedButton( "add deed", new SimpleActionRunnable() );

		title.add( new JLabel( "Adding simple deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "command:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "maxUses:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( simpleField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( simpleField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( simpleField3, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( simpleLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( simpleLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( simpleLabel3, c );

		c.gridx = 0;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( simpleButton, c );
        simpleButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Simple", panel );
	}

	private void addCommandDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

        commandField1 = new JTextField( 25 );
        commandField2 = new JTextField( 25 );
        commandField3 = new JTextField( 25 );
        commandField4 = new JTextField( 25 );
        commandLabel1 = new JLabel( "required" );
        commandLabel1.setToolTipText( "The text to display on the button." );
        commandLabel2 = new JLabel( "required" );
        commandLabel2.setToolTipText( "The preference that the button will track." );
        commandLabel3 = new JLabel( "(optional)" );
        commandLabel3.setToolTipText( "The command that the button will execute." );
        commandLabel4 = new JLabel( "(optional)" );
        commandLabel4
			.setToolTipText( "Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );

        commandField1.getDocument().addDocumentListener( new CommandField1Listener() );
        commandField2.getDocument().addDocumentListener( new CommandField2Listener() );
        commandField3.getDocument().addDocumentListener( new CommandField3Listener() );
        commandField4.getDocument().addDocumentListener( new CommandField4Listener() );

        commandButton = new ThreadedButton( "add deed", new CommandActionRunnable() );

		title.add( new JLabel( "Adding command deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "command:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "maxUses:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( commandField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( commandField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( commandField3, c );
		c.gridx = 1;
		c.gridy = 4;
		textPanel.add( commandField4, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( commandLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( commandLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( commandLabel3, c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( commandLabel4, c );

		c.gridx = 0;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( commandButton, c );
        commandButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Command", panel );
	}

	private void addItemDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

        itemField1 = new JTextField( 25 );
        itemField2 = new JTextField( 25 );
        itemField3 = new JTextField( 25 );
        itemField4 = new JTextField( 25 );
        itemLabel1 = new JLabel( "required" );
        itemLabel1.setToolTipText( "The text to display on the button." );
        itemLabel2 = new JLabel( "required" );
        itemLabel2.setToolTipText( "The preference that the button will track." );
        itemLabel3 = new JLabel( "(optional)" );
        itemLabel3
			.setToolTipText( "If an item is not specified, defaults to displayText.  Uses fuzzy matching." );
        itemLabel4 = new JLabel( "(optional)" );
        itemLabel4
			.setToolTipText( "Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );

        itemField1.getDocument().addDocumentListener( new ItemField1Listener() );
        itemField2.getDocument().addDocumentListener( new ItemField2Listener() );
        itemField3.getDocument().addDocumentListener( new ItemField1Listener() );
		// listener 1 sets the state of both label1 and label3
        itemField4.getDocument().addDocumentListener( new ItemField4Listener() );

        itemButton = new ThreadedButton( "add deed", new ItemPrefRunnable() );

		title.add( new JLabel( "Adding Item Deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "item:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "maxUses:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( itemField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( itemField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( itemField3, c );
		c.gridx = 1;
		c.gridy = 4;
		textPanel.add( itemField4, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( itemLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( itemLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( itemLabel3, c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( itemLabel4, c );

		c.gridx = 0;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( itemButton, c );
        itemButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Item", panel );
	}

	private void addSkillDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

        skillField1 = new JTextField( 25 );
        skillField2 = new JTextField( 25 );
        skillField3 = new JTextField( 25 );
        skillField4 = new JTextField( 25 );
        skillLabel1 = new JLabel( "required" );
        skillLabel1.setToolTipText( "The text to display on the button." );
        skillLabel2 = new JLabel( "required" );
        skillLabel2.setToolTipText( "The preference that the button will track." );
        skillLabel3 = new JLabel( "(optional)" );
        skillLabel3.setToolTipText( "The skill that the button will cast." );
        skillLabel4 = new JLabel( "(optional)" );
        skillLabel4.setToolTipText( "Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );

        skillField1.getDocument().addDocumentListener( new SkillField1Listener() );
        skillField2.getDocument().addDocumentListener( new SkillField2Listener() );
		// listener 1 sets the state of both label1 and label3
        skillField3.getDocument().addDocumentListener( new SkillField1Listener() );
        skillField4.getDocument().addDocumentListener( new SkillField4Listener() );

        skillButton = new ThreadedButton( "add deed", new SkillActionRunnable() );

		title.add( new JLabel( "Adding Skill Deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		textPanel.add( new JLabel( "displayText:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "preference:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "skill:" ), c );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.EAST;
		textPanel.add( new JLabel( "maxCasts:" ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.CENTER;
		textPanel.add( skillField1, c );
		c.gridx = 1;
		c.gridy = 2;
		textPanel.add( skillField2, c );
		c.gridx = 1;
		c.gridy = 3;
		textPanel.add( skillField3, c );
		c.gridx = 1;
		c.gridy = 4;
		textPanel.add( skillField4, c );

		c.gridx = 2;
		c.gridy = 1;
		textPanel.add( skillLabel1, c );
		c.gridx = 2;
		c.gridy = 2;
		textPanel.add( skillLabel2, c );
		c.gridx = 2;
		c.gridy = 3;
		textPanel.add( skillLabel3, c );
		c.gridx = 2;
		c.gridy = 4;
		textPanel.add( skillLabel4, c );

		c.gridx = 0;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( skillButton, c );
        skillButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Skill", panel );
	}

	private void addTextDeed()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		JPanel title = new JPanel();
		title.setLayout( new BoxLayout( title, BoxLayout.Y_AXIS ) );

		JPanel textPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

        textField = new JTextField( 25 );
        textArea = new JTextArea();
        textArea.setColumns( 10 );
        textArea.setRows( 4 );
        textArea.setMaximumSize( textArea.getPreferredSize() );
        textArea.setBorder( BorderFactory.createLoweredBevelBorder() );
        textArea.setLineWrap( true );
        textArea.setWrapStyleWord( true );
        textArea.setEditable( false );
        textArea.setOpaque( false );
        textArea.setFont( KoLConstants.DEFAULT_FONT );

		ThreadedButton undoButton = new ThreadedButton( "undo", new RemoveLastTextRunnable() );
		ThreadedButton clearButton = new ThreadedButton( "clear", new ClearTextRunnable() );

        textField.getDocument().addDocumentListener( new TextFieldListener() );
        addTextButton = new ThreadedButton( "add text", new AddTextRunnable() );
        textDeedButton = new ThreadedButton( "add deed", new TextActionRunnable() );

		title.add( new JLabel( "Adding Text Deed." ) );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.5;
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 3;
		textPanel.add( new JSeparator(), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.NORTH;
		textPanel.add( textField, c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 2;
		c.gridy = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		textPanel.add( addTextButton, c );
        addTextButton.setEnabled( false );
		c.fill = GridBagConstraints.NONE;
		c.gridx = 2;
		c.gridy = 2;
		c.anchor = GridBagConstraints.WEST;
		textPanel.add( undoButton, c );
		c.gridx = 2;
		c.gridy = 3;
		c.anchor = GridBagConstraints.NORTHWEST;
		textPanel.add( clearButton, c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 2;
		c.anchor = GridBagConstraints.CENTER;
		c.gridheight = 2;
		textPanel.add( textArea, c );

		c.gridx = 0;
		c.gridy = 5;
		c.gridheight = 1;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );
		c.gridx = 2;
		c.gridy = 5;
		textPanel.add( Box.createRigidArea( new Dimension( 75, 5 ) ), c );

		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		textPanel.add( textDeedButton, c );
        textDeedButton.setEnabled( false );

		panel.add( title );
		panel.add( textPanel );
		AddCustomDeedsPanel.selectorPanel.addPanel( "- Text", panel );
	}

	public class CommandField4Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( COMMAND_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
			{
				if ( getField( COMMAND_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( COMMAND_LABEL_3, "(optional)",
						"The command that the button will execute." );
				}
				setLabel( COMMAND_LABEL_4, "(optional)",
					"Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );
			}
			else
			{
				if ( getField( COMMAND_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( COMMAND_LABEL_3, "required",
						"You specified maxUses, so you must specify a command  here." );
				}
				try
				{
					Integer.parseInt( getField( COMMAND_FIELD_4 ).getText() );
					setLabel( COMMAND_LABEL_4, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( COMMAND_LABEL_4, "BAD", "Integer only, please." );
				}
			}
			String label1 = getLabel( COMMAND_LABEL_1 ).getText();
			String label2 = getLabel( COMMAND_LABEL_2 ).getText();
			String label3 = getLabel( COMMAND_LABEL_3 ).getText();
			String label4 = getLabel( COMMAND_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getCommandButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class CommandField3Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent arg0 )
		{
			if ( getField( COMMAND_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
			{
				if ( getField( COMMAND_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( COMMAND_LABEL_3, "(optional)",
						"The command that the button will execute." );
				}
				else
				{
					setLabel( COMMAND_LABEL_3, "required",
						"You specified maxUses, so you must specify a command here." );
				}
			}
			else
			{
				setLabel( COMMAND_LABEL_3, "OK" );
			}
			String label1 = getLabel( COMMAND_LABEL_1 ).getText();
			String label2 = getLabel( COMMAND_LABEL_2 ).getText();
			String label3 = getLabel( COMMAND_LABEL_3 ).getText();
			String label4 = getLabel( COMMAND_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getCommandButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class CommandField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			// check if deed is properly formed, update label, enable/disable add deeds button
			if ( getField( COMMAND_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( COMMAND_LABEL_2, "required", "The preference that the button will track." );
			}
			else
			{
				setLabel( COMMAND_LABEL_2, "OK" );
			}

			String label1 = getLabel( COMMAND_LABEL_1 ).getText();
			String label2 = getLabel( COMMAND_LABEL_2 ).getText();
			String label3 = getLabel( COMMAND_LABEL_3 ).getText();
			String label4 = getLabel( COMMAND_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getCommandButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class CommandField1Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( COMMAND_FIELD_1 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( COMMAND_LABEL_1, "required", "The text to display on the button." );
			}
			else
			{
				setLabel( COMMAND_LABEL_1, "OK" );
			}
			String label1 = getLabel( COMMAND_LABEL_1 ).getText();
			String label2 = getLabel( COMMAND_LABEL_2 ).getText();
			String label3 = getLabel( COMMAND_LABEL_3 ).getText();
			String label4 = getLabel( COMMAND_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getCommandButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class ItemField4Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( ITEM_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
			{
				if ( getField( ITEM_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( ITEM_LABEL_3, "(optional)", "The item that the button will use." );
				}
				setLabel( ITEM_LABEL_4, "(optional)",
					"Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );
			}
			else
			{
				if ( getField( ITEM_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( ITEM_LABEL_3, "required",
						"You specified maxUses, so you must specify an item here." );
				}
				try
				{
					Integer.parseInt( getField( ITEM_FIELD_4 ).getText() );
					setLabel( ITEM_LABEL_4, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( ITEM_LABEL_4, "BAD", "Integer only, please." );
				}
			}
			String label1 = getLabel( ITEM_LABEL_1 ).getText();
			String label2 = getLabel( ITEM_LABEL_2 ).getText();
			String label3 = getLabel( ITEM_LABEL_3 ).getText();
			String label4 = getLabel( ITEM_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getItemButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class ItemField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{

			// check if deed is properly formed, update label, enable/disable add deeds button
			if ( getField( ITEM_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( ITEM_LABEL_2, "required", "The preference that the button will track." );
			}
			else
			{
				setLabel( ITEM_LABEL_2, "OK" );
			}

			String label1 = getLabel( ITEM_LABEL_1 ).getText();
			String label2 = getLabel( ITEM_LABEL_2 ).getText();
			String label3 = getLabel( ITEM_LABEL_3 ).getText();
			String label4 = getLabel( ITEM_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getItemButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class ItemField1Listener
		implements DocumentListener
	{
		boolean isBool;
		boolean isInteger;

		public void changedUpdate( DocumentEvent arg0 )
		{
			boolean field1empty = getField( ITEM_FIELD_1 ).getText().equalsIgnoreCase( "" );
			boolean field3empty = getField( ITEM_FIELD_3 ).getText().equalsIgnoreCase( "" );
			boolean field1matching = ItemDatabase.getItemId( getField( ITEM_FIELD_1 ).getText() ) != -1;
			boolean field3matching = ItemDatabase
				.getItemId( getField( ITEM_FIELD_3 ).getText().split( ";" )[ 0 ] ) != -1;

			/*
			 * Since the states of field 1 and field 3 depend on each other, set the states of both
			 * whenever one of the fields is changed.
			 *
			 * State 1: displayText empty, item empty = [ required, (optional) ]
			 * State 2: displayText non-matching, item empty = [ (need item), required ]
			 * State 3: displayText matching, item empty = [ OK, (optional) ]
			 * State 4: displayText empty, item non-matching = [ required, BAD ]
			 * State 5: displayText empty, item matching = [ required, OK ]
			 * State 6: displayText non-empty, item non-matching = [ OK, BAD ]
			 * State 7: displayText non-empty, item matching = [ OK, OK ]
			 *
			 * To enable the button, we check that label 1 is OK and label 3 not BAD
			 */

			/* State 1 */
			if ( field1empty && field3empty )
			{
				setLabel( ITEM_LABEL_1, "required", "The text to display on the button." );
				if ( getField( ITEM_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( ITEM_LABEL_3, "(optional)",
						"If an item is not specified, defaults to displayText.  Uses fuzzy matching." );
				}
				else
				{
					setLabel( ITEM_LABEL_3, "required",
						"You specified maxUses, so you must specify an item here." );
				}

			}
			/* State 2 */
			else if ( !field1matching && field3empty )
			{
				setLabel( ITEM_LABEL_1, "(need item)",
					"The display text does not match an item, so you need to specify one under item:" );
				setLabel( ITEM_LABEL_3, "required",
					"The display text does not match an item, so you need to specify one." );
			}
			/* State 3 */
			else if ( field1matching && field3empty )
			{
				setLabel(
					ITEM_LABEL_1,
					"OK",
					"Display text matches item: "
						+ ItemDatabase.getItemName( ItemDatabase.getItemId( getField(
							ITEM_FIELD_1 ).getText() ) ) );
				if ( getField( ITEM_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( ITEM_LABEL_3, "(optional)",
						"If an item is not specified, defaults to displayText.  Uses fuzzy matching." );
				}
				else
				{
					setLabel( ITEM_LABEL_3, "required",
						"You specified maxUses, so you must specify an item here." );
				}
			}
			/* State 4 */
			else if ( field1empty && !field3matching )
			{
				setLabel( ITEM_LABEL_1, "required", "The text to display on the button." );
				setLabel( ITEM_LABEL_3, "BAD",
					"Could not find a matching item for: " + getField( ITEM_FIELD_3 ).getText() );
			}
			/* State 5 */
			else if ( field1empty && field3matching )
			{
				setLabel( ITEM_LABEL_1, "required", "You still need to specify the text to display." );
				setLabel(
					ITEM_LABEL_3,
					"OK",
					"Matching item found: "
						+ ItemDatabase.getItemName( ItemDatabase.getItemId( getField(
							ITEM_FIELD_3 ).getText() ) ) );
			}
			/* State 6 */
			else if ( !field1empty && !field3matching )
			{
				setLabel( ITEM_LABEL_1, "OK" );
				setLabel( ITEM_LABEL_3, "BAD",
					"Could not find a matching item for: " + getField( ITEM_FIELD_3 ).getText() );
			}
			/* State 7 */
			else if ( !field1empty && field3matching )
			{
				setLabel( ITEM_LABEL_1, "OK" );
				setLabel(
					ITEM_LABEL_3,
					"OK",
					"Matching item found: "
						+ ItemDatabase.getItemName( ItemDatabase.getItemId( getField(
							ITEM_FIELD_3 ).getText() ) ) );
			}

			String label1 = getLabel( ITEM_LABEL_1 ).getText();
			String label2 = getLabel( ITEM_LABEL_2 ).getText();
			String label3 = getLabel( ITEM_LABEL_3 ).getText();
			String label4 = getLabel( ITEM_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getItemButton().setEnabled( enabled );

		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillField4Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SKILL_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
			{
				if ( getField( SKILL_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_3, "(optional)", "The skill that the button will cast." );
				}
				setLabel( SKILL_LABEL_4, "(optional)",
					"Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );
			}
			else
			{
				if ( getField( SKILL_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_3, "required",
						"You specified maxCasts, so you must specify a skill here." );
				}
				try
				{
					Integer.parseInt( getField( SKILL_FIELD_4 ).getText() );
					setLabel( SKILL_LABEL_4, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( SKILL_LABEL_4, "BAD", "Integer only, please." );
				}
			}
			String label1 = getLabel( SKILL_LABEL_1 ).getText();
			String label2 = getLabel( SKILL_LABEL_2 ).getText();
			String label3 = getLabel( SKILL_LABEL_3 ).getText();
			String label4 = getLabel( SKILL_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getSkillButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SKILL_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( SKILL_LABEL_2, "required", "The preference that the button will track." );
			}
			else
			{
				setLabel( SKILL_LABEL_2, "OK" );
			}

			String label1 = getLabel( SKILL_LABEL_1 ).getText();
			String label2 = getLabel( SKILL_LABEL_2 ).getText();
			String label3 = getLabel( SKILL_LABEL_3 ).getText();
			String label4 = getLabel( SKILL_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getSkillButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillField1Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			boolean field1empty = getField( SKILL_FIELD_1 ).getText().equalsIgnoreCase( "" );
			boolean field3empty = getField( SKILL_FIELD_3 ).getText().equalsIgnoreCase( "" );
			boolean field1matching = SkillDatabase.getMatchingNames( getField( SKILL_FIELD_1 ).getText() )
				.size() == 1;
			boolean field3matching = SkillDatabase.getMatchingNames( getField( SKILL_FIELD_3 ).getText() )
				.size() == 1;

			/*
			 * Since the states of field 1 and field 3 depend on each other, set the states of both
			 * whenever one of the fields is changed.
			 *
			 * State 1: displayText empty, skill empty = [ required, (optional) ]
			 * State 2: displayText non-matching, skill empty = [ (need skill), required ]
			 * State 3: displayText matching, skill empty = [ OK, (optional) ]
			 * State 4: displayText empty, skill non-matching = [ required, BAD ]
			 * State 5: displayText empty, skill matching = [ required, OK ]
			 * State 6: displayText non-empty, skill non-matching = [ OK, BAD ]
			 * State 7: displayText non-empty, skill matching = [ OK, OK ]
			 *
			 * To enable the button, we check that label 1 is OK and label 3 not BAD
			 */

			/* State 1 */
			if ( field1empty && field3empty )
			{
				setLabel( SKILL_LABEL_1, "required", "The text to display on the button." );
				if ( getField( SKILL_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_3, "(optional)",
						"If an skill is not specified, defaults to displayText.  Uses fuzzy matching." );
				}
				else
				{
					setLabel( SKILL_LABEL_3, "required",
						"You specified maxCasts, so you must specify a skill here." );
				}
			}

			/* State 2 */
			else if ( !field1matching && field3empty )
			{
				setLabel( SKILL_LABEL_1, "(need skill)",
					"The display text does not match a skill, so you need to specify one under skill:" );
				setLabel( SKILL_LABEL_3, "required",
					"The display text does not match a skill, so you need to specify one." );
			}

			/* State 3 */
			else if ( field1matching && field3empty )
			{
				setLabel( SKILL_LABEL_1, "OK", "Display text matches skill: "
					+ SkillDatabase.getMatchingNames( getField( SKILL_FIELD_1 ).getText() ).get( 0 ) );
				if ( getField( SKILL_FIELD_4 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SKILL_LABEL_3, "(optional)",
						"The display text matches a skill, so you don't need to specify one here." );
				}
				else
				{
					setLabel( SKILL_LABEL_3, "required",
						"You specified maxCasts, so you must specify a skill here." );
				}
			}

			/* State 4 */
			else if ( field1empty && !field3matching )
			{
				setLabel( SKILL_LABEL_1, "required", "The text to display on the button." );
				setLabel( SKILL_LABEL_3, "BAD",
					"Could not find a matching skill for: " + getField( SKILL_FIELD_3 ).getText() );
			}

			/* State 5 */
			else if ( field1empty && field3matching )
			{
				setLabel( SKILL_LABEL_1, "required", "You still need to specify the text to display." );
				setLabel( SKILL_LABEL_3, "OK", "Matching skill found: "
					+ SkillDatabase.getMatchingNames( getField( SKILL_FIELD_3 ).getText() ).get( 0 ) );
			}

			/* State 6 */
			else if ( !field1empty && !field3matching )
			{
				setLabel( SKILL_LABEL_1, "OK" );
				setLabel( SKILL_LABEL_3, "BAD",
					"Could not find a matching skill for: " + getField( SKILL_FIELD_3 ).getText() );
			}

			/* State 7 */
			else if ( !field1empty && field3matching )
			{
				setLabel( SKILL_LABEL_1, "OK" );
				setLabel( SKILL_LABEL_3, "OK", "Matching skill found: "
					+ SkillDatabase.getMatchingNames( getField( SKILL_FIELD_3 ).getText() ).get( 0 ) );
			}

			String label1 = getLabel( SKILL_LABEL_1 ).getText();
			String label2 = getLabel( SKILL_LABEL_2 ).getText();
			String label3 = getLabel( SKILL_LABEL_3 ).getText();
			String label4 = getLabel( SKILL_LABEL_4 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) && label2.equalsIgnoreCase( "OK" )
				&& ( label3.equalsIgnoreCase( "OK" ) || label3.equalsIgnoreCase( "(optional)" ) )
				&& !label4.equalsIgnoreCase( "BAD" );

			getSkillButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class TextFieldListener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( TEXT_FIELD ).getText().equalsIgnoreCase( "" ) )
			{
				getAddTextButton().setEnabled( false );
			}
			else
			{
				getAddTextButton().setEnabled( true );
			}
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class SkillActionRunnable
		implements Runnable
	{
		public void run()
		{
			String display = getField( SKILL_FIELD_1 ).getText();
			String pref = getField( SKILL_FIELD_2 ).getText();
			String skill = getField( SKILL_FIELD_3 ).getText();
			String maxCasts = getField( SKILL_FIELD_4 ).getText();

			String deed = "$CUSTOM|Skill|" + display + "|" + pref;

			if ( !skill.equals( "" ) )
			{
				deed += "|" + skill;
			}
			if ( !maxCasts.equals( "" ) )
			{
				deed += "|" + maxCasts;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );

			getSkillButton().setEnabled( false );
		}
	}

	public class ItemPrefRunnable
		implements Runnable
	{
		public void run()
		{
			String display = getField( ITEM_FIELD_1 ).getText();
			String pref = getField( ITEM_FIELD_2 ).getText();
			String item = getField( ITEM_FIELD_3 ).getText();
			String maxUses = getField( ITEM_FIELD_4 ).getText();

			String deed = "$CUSTOM|Item|" + display + "|" + pref;

			if ( !item.equals( "" ) )
			{
				deed += "|" + item;
			}
			if ( !maxUses.equals( "" ) )
			{
				deed += "|" + maxUses;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );
			getItemButton().setEnabled( false );
		}
	}

	private class SimpleField3Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SIMPLE_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
			{
				if ( getField( SIMPLE_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SIMPLE_LABEL_2, "(optional)", "The item that the button will use." );
				}
				setLabel( SIMPLE_LABEL_3, "(optional)",
					"Provide an integer to disable the button at.  The button will be enabled until the preference reaches this number." );
			}
			else
			{
				if ( getField( SIMPLE_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SIMPLE_LABEL_2, "required",
						"You specified maxUses, so you must specify an item here." );
				}
				try
				{
					Integer.parseInt( getField( SIMPLE_FIELD_3 ).getText() );
					setLabel( SIMPLE_LABEL_3, "OK" );
				}
				catch ( NumberFormatException exception )
				{
					setLabel( SIMPLE_LABEL_3, "BAD", "Integer only, please." );
				}
			}
			String label1 = getLabel( SIMPLE_LABEL_1 ).getText();
			String label2 = getLabel( SIMPLE_LABEL_2 ).getText();
			String label3 = getLabel( SIMPLE_LABEL_3 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) 
				&& ( label2.equalsIgnoreCase( "OK" ) || label2.equalsIgnoreCase( "(optional)" ) )
				&& !label3.equalsIgnoreCase( "BAD" );

			getSimpleButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	private class SimpleField2Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent arg0 )
		{
			if ( getField( SIMPLE_FIELD_2 ).getText().equalsIgnoreCase( "" ) )
			{
				if ( getField( SIMPLE_FIELD_3 ).getText().equalsIgnoreCase( "" ) )
				{
					setLabel( SIMPLE_LABEL_2, "(optional)",
						"The command that the button will execute." );
				}
				else
				{
					setLabel( SIMPLE_LABEL_2, "required",
						"You specified maxUses, so you must specify a command here." );
				}
			}
			else
			{
				setLabel( SIMPLE_LABEL_2, "OK" );
			}
			String label1 = getLabel( SIMPLE_LABEL_1 ).getText();
			String label2 = getLabel( SIMPLE_LABEL_2 ).getText();
			String label3 = getLabel( SIMPLE_LABEL_3 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) 
				&& ( label2.equalsIgnoreCase( "OK" ) || label2.equalsIgnoreCase( "(optional)" ) )
				&& !label3.equalsIgnoreCase( "BAD" );

			getSimpleButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	private class SimpleField1Listener
		implements DocumentListener
	{
		public void changedUpdate( DocumentEvent e )
		{
			if ( getField( SIMPLE_FIELD_1 ).getText().equalsIgnoreCase( "" ) )
			{
				setLabel( SIMPLE_LABEL_1, "required", "The text to display on the button." );
			}
			else
			{
				setLabel( SIMPLE_LABEL_1, "OK" );
			}
			String label1 = getLabel( SIMPLE_LABEL_1 ).getText();
			String label2 = getLabel( SIMPLE_LABEL_2 ).getText();
			String label3 = getLabel( SIMPLE_LABEL_3 ).getText();

			boolean enabled = label1.equalsIgnoreCase( "OK" ) 
				&& ( label2.equalsIgnoreCase( "OK" ) || label2.equalsIgnoreCase( "(optional)" ) )
				&& !label3.equalsIgnoreCase( "BAD" );

			getSimpleButton().setEnabled( enabled );
		}

		public void insertUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}

		public void removeUpdate( DocumentEvent e )
		{
			changedUpdate( e );
		}
	}

	public class CommandActionRunnable
		implements Runnable
	{
		public void run()
		{
			String display = getField( COMMAND_FIELD_1 ).getText();
			String pref = getField( COMMAND_FIELD_2 ).getText();
			String command = getField( COMMAND_FIELD_3 ).getText();
			String maxUses = getField( COMMAND_FIELD_4 ).getText();

			String deed = "$CUSTOM|Command|" + display + "|" + pref;

			if ( !command.equals( "" ) )
			{
				deed += "|" + command;
			}
			if ( !maxUses.equals( "" ) )
			{
				deed += "|" + maxUses;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );
			getCommandButton().setEnabled( false );
		}
	}

	public class RemoveLastTextRunnable
		implements Runnable
	{
		public void run()
		{
			String piece;
			ArrayList buffer = getTextDeed();
			String display = "";

			buffer.remove( buffer.size() - 1 );

			for ( int i = 0; i < buffer.size(); ++i )
			{
				piece = (String) buffer.get( i );
				piece = piece.replaceAll( "\\|", "" );

				if ( Preferences.getString( piece ).equals( "" ) )
				{
					display += piece;
				}
				else
				{
					display += Preferences.getString( piece );
				}
			}

			display = display.replaceAll( "\\|", "" );
			getTextArea().setText( display );
			if ( buffer.size() == 0 )
			{
				getTextDeedButton().setEnabled( false );
			}
		}
	}

	public class AddTextRunnable
		implements Runnable
	{
		public void run()
		{
			String piece;
			ArrayList buffer = getTextDeed();
			String display = "";

			if ( buffer.isEmpty() )
			{
				buffer.add( getField( TEXT_FIELD ).getText().replaceAll( ",", ",|" ) );
			}
			else
			{
				buffer.add( "|" + getField( TEXT_FIELD ).getText().replaceAll( ",", ",|" ) );
			}

			for ( int i = 0; i < buffer.size(); ++i )
			{
				piece = (String) buffer.get( i );
				String[] pieces = piece.split( "\\|" );

				for ( int j = 0; j < pieces.length; ++j )
				{
					if ( pieces[ j ].equals( null ) || pieces[ j ].equals( "" ) )
					{
						continue;
					}
					if ( Preferences.getString( pieces[ j ] ).equals( "" ) )
					{
						display += pieces[ j ];
					}
					else
					{
						display += Preferences.getString( pieces[ j ] );
					}
				}
			}

			getField( TEXT_FIELD ).setText( "" );
			getTextArea().setText( display );
			getAddTextButton().setEnabled( false );
			getTextDeedButton().setEnabled( true );
		}
	}

	public class ClearTextRunnable
		implements Runnable
	{
		public void run()
		{
			getTextDeed().clear();
			getTextArea().setText( "" );
			getTextDeedButton().setEnabled( false );
		}
	}

	public class TextActionRunnable
		implements Runnable
	{
		public void run()
		{
			ArrayList buffer = getTextDeed();
			String deed = "$CUSTOM|Text|";

			for ( int i = 0; i < buffer.size(); ++i )
			{
				deed += (String) buffer.get( i );
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );

			getTextDeed().clear();
			getTextArea().setText( "" );
			getTextDeedButton().setEnabled( false );
		}
	}
	
	private class SimpleActionRunnable
		implements Runnable
	{
		public void run()
		{
			String display = getField( SIMPLE_FIELD_1 ).getText();
			String command = getField( SIMPLE_FIELD_2 ).getText();
			String maxUses = getField( SIMPLE_FIELD_3 ).getText();

			String deed = "$CUSTOM|Simple|" + display;

			if ( !command.equals( "" ) )
			{
				deed += "|" + command;
			}
			if ( !maxUses.equals( "" ) )
			{
				deed += "|" + maxUses;
			}

			String oldString = Preferences.getString( "dailyDeedsOptions" );
			Preferences.setString( "dailyDeedsOptions", oldString + "," + deed );

			RequestLogger.printLine( "Custom deed added: " + deed );
			getSimpleButton().setEnabled( false );
		}
	}

	public JTextField getField( int choice )
	{
		JTextField[] fields =
		{
                commandField1, commandField2, commandField3, commandField4,
                itemField1, itemField2, itemField3, itemField4, skillField1,
                skillField2, skillField3, skillField4, textField, simpleField1,
                simpleField2, simpleField3
		};
		return fields[ choice ];
	}

	public JLabel getLabel( int choice )
	{
		JLabel[] labels =
		{
                commandLabel1, commandLabel2, commandLabel3, commandLabel4,
                itemLabel1, itemLabel2, itemLabel3, itemLabel4, skillLabel1,
                skillLabel2, skillLabel3, skillLabel4, simpleLabel1, simpleLabel2,
                simpleLabel3
		};
		return labels[ choice ];
	}

	public void setLabel( int choice, String label )
	{
		setLabel( choice, label, null );
	}

	public void setLabel( int choice, String label, String tip )
	{
		getLabel( choice ).setText( label );
		getLabel( choice ).setToolTipText( tip );
	}

	public ThreadedButton getCommandButton()
	{
		return commandButton;
	}

	public ThreadedButton getItemButton()
	{
		return itemButton;
	}

	public ThreadedButton getSkillButton()
	{
		return skillButton;
	}

	public ThreadedButton getTextDeedButton()
	{
		return textDeedButton;
	}

	public ThreadedButton getAddTextButton()
	{
		return addTextButton;
	}

	public JTextArea getTextArea()
	{
		return textArea;
	}

	public ArrayList getTextDeed()
	{
		return textDeed;
	}
	
	public ThreadedButton getSimpleButton()
	{
		return simpleButton;
	}

}
