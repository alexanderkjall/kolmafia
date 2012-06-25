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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CouncilRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;

import net.sourceforge.kolmafia.webui.IslandDecorator;

public class CouncilFrame
	extends RequestFrame
{
	public static final CouncilRequest COUNCIL_VISIT = new CouncilRequest();

	private static final Pattern ORE_PATTERN = Pattern.compile( "3 chunks of (\\w+) ore" );

	public CouncilFrame()
	{
		super( "Council of Loathing" );
	}

	@Override
	public void setVisible( boolean isVisible )
	{
		super.setVisible( isVisible );

		if ( isVisible )
		{
			CouncilFrame.COUNCIL_VISIT.responseText = null;
			this.displayRequest( CouncilFrame.COUNCIL_VISIT );
		}
	}

	@Override
	public boolean hasSideBar()
	{
		return false;
	}

	@Override
	public String getDisplayHTML( final String responseText )
	{
		return super.getDisplayHTML( responseText )
			.replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" )
			.replaceFirst( "table width=95%", "table width=100%" );
	}

	public static final void handleQuestChange( final String location, final String responseText )
	{
		if ( location.startsWith( "adventure" ) )
		{
			if ( location.contains( "216" ) )
			{
				CouncilFrame.handleTrickOrTreatingChange( responseText );
			}
			else if ( KoLCharacter.getInebriety() > 25 )
			{
				CouncilFrame.handleSneakyPeteChange( responseText );
			}
		}
		else if ( location.startsWith( "bigisland" ) )
		{
			IslandDecorator.parseBigIsland( location, responseText );
		}
		else if ( location.startsWith( "cobbsknob.php" ) )
		{
			if ( location.contains( "action=cell37" ) )
			{
				CouncilFrame.handleCell37( responseText );
			}
		}
		else if ( location.startsWith( "council" ) )
		{
			CouncilFrame.handleCouncilChange( responseText );
		}
		else if ( location.startsWith( "friars" ) )
		{
			CouncilFrame.handleFriarsChange( responseText );
		}
		else if ( location.startsWith( "inv_use" ) )
		{
			if ( location.contains( "whichitem=5116" ) )
			{
				AWOLQuartermasterRequest.parseResponse( location, responseText );
			}
			else if ( location.contains( "whichitem=5683" ) )
			{
				BURTRequest.parseResponse( location, responseText );
			}
		}
		else if ( location.startsWith( "lair" ) )
		{
			SorceressLairManager.handleQuestChange( location, responseText );
		}
		else if ( location.startsWith( "mountains" ) )
		{
			CouncilFrame.handleMountainsChange( responseText );
		}
		else if ( location.startsWith( "manor3" ) )
		{
			WineCellarRequest.handleCellarChange( responseText );
		}
		else if ( location.startsWith( "pandamonium" ) )
		{
			// Quest starts the very instant you click on pandamonium.php
			QuestDatabase.setQuestIfBetter( Quest.AZAZEL, QuestDatabase.STARTED );
		}
		else if ( location.startsWith( "plains" ) )
		{
			CouncilFrame.handlePlainsChange( responseText );
		}
		else if ( location.startsWith( "postwarisland" ) )
		{
			IslandDecorator.parsePostwarIsland( location, responseText );
		}
		else if ( location.startsWith( "questlog" ) )
		{
			QuestLogRequest.registerQuests( false, location, responseText );
		}
		else if ( location.startsWith( "beach.php?action=woodencity" ) )
		{
			CouncilFrame.parsePyramidChange( responseText );
		}
		else if ( location.startsWith( "tavern" ) )
		{
			TavernManager.handleTavernChange( responseText );
		}
		else if ( location.startsWith( "trapper" ) )
		{
			CouncilFrame.handleTrapperChange( location, responseText );
		}
		else if ( location.startsWith( "trickortreat" ) )
		{
			CouncilFrame.handleTrickOrTreatingChange( responseText );
		}
		else if ( location.startsWith( "woods" ) )
		{
			// If we see the Hidden Temple, mark it as unlocked
			if ( responseText.contains( "otherimages/woods/temple.gif" ) )
			{
				Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
			}

			// If we see the link to the empty Black Market, Wu Tang has been defeated
			if ( responseText.contains( "action=emptybm" ) )
			{
				Preferences.setInteger( "lastWuTangDefeated", KoLCharacter.getAscensions() );
			}
		}
		// Obsolete. Sigh.
		else if ( location.startsWith( "generate15" ) )
		{
			// You slide the last tile into place ...

			if ( AdventureRequest.registerDemonName( "Strange Cube", responseText ) ||
                    responseText.contains( "slide the last tile" ) )
			{
				ResultProcessor.processItem( ItemPool.STRANGE_CUBE, -1 );
			}
		}
	}

	private static void parsePyramidChange( String responseText )
	{
		// Suddenly, the model bursts into flames and is quickly consumed, leaving behind a pile of ash and a
		// large hidden trapdoor. You open the trapdoor to find a flight of stone stairs, which appear to
		// descend into an ancient buried pyramid.

		// Well, /that/ wasn't quite what you expected.
		
		if ( responseText.contains( "the model bursts into flames and is quickly consumed" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step12" );
		}
	}

	private static final void handleSneakyPeteChange( final String responseText )
	{
		if ( responseText.contains( "You hand him your button and take his glowstick" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.NOVELTY_BUTTON );
			return;
		}

		if ( responseText.contains( "Ah, man, you dropped your crown back there!" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.TATTERED_PAPER_CROWN );
			return;
		}
	}

	private static final void handleTrickOrTreatingChange( final String responseText )
	{
		if ( responseText.contains( "pull the pumpkin off of your head" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.PUMPKINHEAD_MASK );
			return;
		}
		if ( responseText.contains( "gick all over your mummy costume" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.MUMMY_COSTUME );
			return;
		}
		if ( responseText.contains( "unzipping the mask and throwing it behind you" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.WOLFMAN_MASK );
			return;
		}
	}

	private static final void handleCell37( final String responseText )
	{
		// You pass the folder through the little barred window, and hear Subject 37 flipping through the pages
		if ( responseText.contains( "pass the folder through" ) )
		{
			ResultProcessor.processItem( ItemPool.SUBJECT_37_FILE, -1 );
		}
		// You pass the GOTO through the window, and Subject 37 thanks you.
		if ( responseText.contains( "pass the GOTO through" ) )
		{
			ResultProcessor.processItem( ItemPool.GOTO, -1 );
		}
		// You pass the little vial of of weremoose spit through the window.
		if ( responseText.contains( "pass the little vial" ) )
		{
			ResultProcessor.processItem( ItemPool.WEREMOOSE_SPIT, -1 );
		}
		// You hand Subject 37 the glob of abominable blubber.
		if ( responseText.contains( "hand Subject 37 the glob" ) )
		{
			ResultProcessor.processItem( ItemPool.ABOMINABLE_BLUBBER, -1 );
		}
	}

	private static final void handleFriarsChange( final String responseText )
	{
		// "Thank you, Adventurer."

		if ( responseText.contains( "Thank you" ) )
		{
			ResultProcessor.processItem( ItemPool.DODECAGRAM, -1 );
			ResultProcessor.processItem( ItemPool.CANDLES, -1 );
			ResultProcessor.processItem( ItemPool.BUTTERKNIFE, -1 );
			int knownAscensions = Preferences.getInteger( "knownAscensions" );
			Preferences.setInteger( "lastFriarCeremonyAscension", knownAscensions );
			QuestDatabase.setQuestProgress( Quest.FRIAR, QuestDatabase.FINISHED );
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "Taint cleansed." );
			}
		}
	}

	private static final void handleMountainsChange( final String responseText )
	{
		// You approach the Orc Chasm, and whip out your trusty bridge.
		// You place the bridge across the chasm, and the path to the
		// Valley is clear.

		if ( responseText.contains( "trusty bridge" ) )
		{
			ResultProcessor.processItem( ItemPool.BRIDGE, -1 );
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "You have bridged the Orc Chasm." );
			}
			QuestDatabase.setQuestProgress( Quest.LOL, "step1" );
		}
	}

	private static final void handlePlainsChange( final String responseText )
	{
		// You stare at the pile of coffee grounds for a minute and it
		// occurs to you that maybe your grandma wasn't so crazy after
		// all. You pull out an enchanted bean and plop it into the
		// pile of grounds. It immediately grows into an enormous
		// beanstalk.

		if ( responseText.contains( "immediately grows into an enormous beanstalk" ) )
		{
			ResultProcessor.processItem( ItemPool.ENCHANTED_BEAN, -1 );
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "You have planted a beanstalk." );
			}
		}
	}

	private static final void handleTrapperChange( final String location, final String responseText )
	{
		Matcher oreMatcher = CouncilFrame.ORE_PATTERN.matcher( responseText );
		if ( oreMatcher.find() )
		{
			Preferences.setString( "trapperOre", oreMatcher.group( 1 ) + " ore" );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step1" );
		}

		if ( responseText.contains( "Thanks for yer help, Adventurer" ) ||
                responseText.contains( "You ain't got no furs" ) ||
                responseText.contains( "Yeti furs, eh?" ) )
		{
			Preferences.setInteger( "lastTr4pz0rQuest", KoLCharacter.getAscensions() );
			QuestDatabase.setQuestProgress( Quest.TRAPPER, QuestDatabase.FINISHED );
		}

		// If you receive items from the trapper, then you
		// lose some items already in your inventory.

		if ( !responseText.contains( "You acquire" ) )
		{
			return;
		}

		if ( responseText.contains( "asbestos" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "asbestos ore", -3, false ) );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step2" );
		}
		else if ( responseText.contains( "linoleum" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "linoleum ore", -3, false ) );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step2" );
		}
		else if ( responseText.contains( "chrome" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "chrome ore", -3, false ) );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step2" );
		}
		else if ( responseText.contains( "goat cheese pizza" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "goat cheese", -6, false ) );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step3" );
		}
	}

	public static final void unlockGoatlet()
	{
		AdventureRequest goatlet = new AdventureRequest( "Goatlet", "adventure.php", "60" );

		if ( KoLCharacter.inFistcore() )
		{
			// You can actually get here without knowing Worldpunch
			// in Softcore by pulling ores.
			if ( !KoLCharacter.hasSkill( "Worldpunch" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Try again after you learn Worldpunch." );
				return;
			}

			// If you don't have Earthen Fist active, get it.
			if ( !KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST ) )
			{
				UseSkillRequest request = UseSkillRequest.getInstance( "Worldpunch" );
				request.setBuffCount( 1 );
				RequestThread.postRequest( request );
			}

			// Perhaps you ran out of MP.
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Cast Worldpunch and try again." );
			}

			RequestThread.postRequest( goatlet );
			return;
		}

		if ( KoLCharacter.inAxecore() )
		{
			// No outfit needed; just take option #3
			RequestThread.postRequest( goatlet );
			return;
		}

		if ( !EquipmentManager.hasOutfit( 8 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "You need a mining outfit to continue." );
			return;
		}

		if ( EquipmentManager.isWearingOutfit( 8 ) )
		{
			RequestThread.postRequest( goatlet );
			return;
		}

		SpecialOutfit.createImplicitCheckpoint();
		( new EquipmentRequest( EquipmentDatabase.getOutfit( 8 ) ) ).run();
		RequestThread.postRequest( goatlet );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	private static final void handleCouncilChange( final String responseText )
	{
		Preferences.setInteger( "lastCouncilVisit", KoLCharacter.getLevel() );

		if ( responseText.contains( "500" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "mosquito larva", -1, false ) );
		}
		if ( responseText.contains( "batskin belt" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		}
		if ( responseText.contains( "dragonbone belt buckle" ) )
		{
			ResultProcessor.processResult( new AdventureResult( "skull of the bonerdagon", -1, false ) );
		}
		QuestDatabase.handleCouncilText( responseText );
	}
}
