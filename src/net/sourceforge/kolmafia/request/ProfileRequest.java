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

package net.sourceforge.kolmafia.request;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ProfileRequest
	extends GenericRequest
	implements Comparable
{
	private static final Pattern DATA_PATTERN = Pattern.compile( "<td.*?>(.*?)</td>" );
	private static final Pattern NUMERIC_PATTERN = Pattern.compile( "\\d+" );
	private static final Pattern CLAN_ID_PATTERN = Pattern.compile( "whichclan=(\\d+)" );
	private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat( "MMMM d, yyyy", Locale.US );
	public static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat( "MM/dd/yy", Locale.US );

	private String playerName;
	private String playerId;
	private Integer playerLevel;
	private boolean isHardcore;
	private String restriction;
	private Integer currentMeat;
	private Integer turnsPlayed, currentRun;
	private String classType;

	private Date created, lastLogin;
	private String food, drink;
	private Integer ascensionCount, pvpRank, karma;

	private Integer muscle, mysticism, moxie;
	private String title, rank;

	private String clanName;
	private int clanId;
	private int equipmentPower;

	public ProfileRequest( final String playerName )
	{
		super( "showplayer.php" );

		if ( playerName.startsWith( "#" ) )
		{
            playerId = playerName.substring( 1 );
			this.playerName = ContactManager.getPlayerName( playerId );
		}
		else
		{
			this.playerName = playerName;
            playerId = ContactManager.getPlayerId( playerName );
		}

        addFormField( "who", playerId );

        muscle = IntegerPool.get( 0 );
        mysticism = IntegerPool.get( 0 );
        moxie = IntegerPool.get( 0 );
        karma = IntegerPool.get( 0 );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	/**
	 * Internal method used to refresh the fields of the profile request based on the response text. This should be
	 * called after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		// Nothing to refresh if no text
		if ( responseText == null || responseText.length() == 0 )
		{
			return;
		}

        isHardcore = responseText.contains( "<b>(Hardcore)</b></td>" );

		// This is a massive replace which makes the profile easier to
		// parse and re-represent inside of editor panes.

		String cleanHTML = responseText.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" );
		StringTokenizer st = new StringTokenizer( cleanHTML, "\n" );

		String token = st.nextToken();

        playerLevel = IntegerPool.get( 0 );
        classType = "Recent Ascension";
        currentMeat = IntegerPool.get( 0 );
        ascensionCount = IntegerPool.get( 0 );
        turnsPlayed = IntegerPool.get( 0 );
        created = new Date();
        lastLogin = new Date();
        food = "none";
        drink = "none";
        pvpRank = IntegerPool.get( 0 );

		if ( cleanHTML.contains( "\nClass:" ) )
		{	// has custom title
			while ( !st.nextToken().startsWith( " (#" ) )
			{
			}
			String title = st.nextToken();	// custom title, may include level
			// Next token will be one of:
			//	(Level n), if the custom title doesn't include the level
			//	(In Ronin) or possibly similar messages
			//	Class:,	if neither of the above applies
			token = st.nextToken();
			if ( token.startsWith( "(Level" ) )
			{
                playerLevel = IntegerPool.get(
					StringUtilities.parseInt( token.substring( 6 ).trim() ) );
			}
			else
			{	// Must attempt to parse the level out of the custom title.
				// This is inherently inaccurate, since the title can contain other digits,
				// before, after, or adjacent to the level.
				Matcher m = ProfileRequest.NUMERIC_PATTERN.matcher( title );
				if ( m.find() && m.group().length() < 5 )
				{
                    playerLevel = IntegerPool.get(
						StringUtilities.parseInt( m.group() ) );
				}
			}
		
			while ( !token.startsWith( "Class" ) )
			{
				token = st.nextToken();
			}
            classType = KoLCharacter.getClassType( st.nextToken().trim() );
		}
		else
		{	// no custom title
			if ( !cleanHTML.contains( "Level" ) )
			{
				return;
			}
	
			while ( !token.contains( "Level" ) )
			{
				token = st.nextToken();
			}

            playerLevel = IntegerPool.get(
				StringUtilities.parseInt( token.substring( 5 ).trim() ) );
            classType = KoLCharacter.getClassType( st.nextToken().trim() );
		}
		
		if ( cleanHTML.contains( "\nAscensions" ) && cleanHTML.contains( "\nPath" ) )
		{
			while ( !st.nextToken().startsWith( "Path" ) )
			{
				;
			}
            restriction = st.nextToken().trim();
		}
		else
		{
            restriction = "No-Path";
		}

		if ( cleanHTML.contains( "\nMeat:" ) )
		{
			while ( !st.nextToken().startsWith( "Meat" ) )
			{
				;
			}
            currentMeat = IntegerPool.get( StringUtilities.parseInt( st.nextToken().trim() ) );
		}

		if ( cleanHTML.contains( "\nAscensions" ) )
		{
			while ( !st.nextToken().startsWith( "Ascensions" ) )
			{
				;
			}
			st.nextToken();
            ascensionCount = IntegerPool.get( StringUtilities.parseInt( st.nextToken().trim() ) );
		}
		else
		{
            ascensionCount = IntegerPool.get( 0 );
		}

		while ( !st.nextToken().startsWith( "Turns" ) )
		{
			;
		}
        turnsPlayed = IntegerPool.get( StringUtilities.parseInt( st.nextToken().trim() ) );

		if ( cleanHTML.contains( "\nAscensions" ) )
		{
			while ( !st.nextToken().startsWith( "Turns" ) )
			{
				;
			}
            currentRun = IntegerPool.get( StringUtilities.parseInt( st.nextToken().trim() ) );
		}
		else
		{
            currentRun = turnsPlayed;
		}

		String dateString = null;
		while ( !st.nextToken().startsWith( "Account" ) )
		{
			;
		}
		try
		{
			dateString = st.nextToken().trim();
            created = ProfileRequest.INPUT_FORMAT.parse( dateString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Could not parse date \"" + dateString + "\"" );
            created = new Date();
		}

		while ( !st.nextToken().startsWith( "Last" ) )
		{
			;
		}

		try
		{
			dateString = st.nextToken().trim();
            lastLogin = ProfileRequest.INPUT_FORMAT.parse( dateString );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Could not parse date \"" + dateString + "\"" );
            lastLogin = created;
		}

		if ( cleanHTML.contains( "\nFavorite Food" ) )
		{
			while ( !st.nextToken().startsWith( "Favorite" ) )
			{
				;
			}
            food = st.nextToken().trim();
		}
		else
		{
            food = "none";
		}

		if ( cleanHTML.contains( "\nFavorite Booze" ) )
		{
			while ( !st.nextToken().startsWith( "Favorite" ) )
			{
				;
			}
            drink = st.nextToken().trim();
		}
		else
		{
            drink = "none";
		}

		if ( cleanHTML.contains( "\nFame" ) )
		{
			while ( !st.nextToken().startsWith( "Fame" ) )
			{
				;
			}
            pvpRank = IntegerPool.get( StringUtilities.parseInt( st.nextToken().trim() ) );
		}
		else
		{
            pvpRank = IntegerPool.get( 0 );
		}

        equipmentPower = 0;
		if ( cleanHTML.contains( "\nEquipment" ) )
		{
			while ( !st.nextToken().startsWith( "Equipment" ) )
			{
				;
			}

			while ( EquipmentDatabase.contains( token = st.nextToken() ) )
			{
				switch ( ItemDatabase.getConsumptionType( token ) )
				{
				case KoLConstants.EQUIP_HAT:
				case KoLConstants.EQUIP_PANTS:
				case KoLConstants.EQUIP_SHIRT:

                    equipmentPower += EquipmentDatabase.getPower( token );
					break;
				}
			}
		}

		if ( cleanHTML.contains( "\nClan" ) )
		{
			while ( !token.startsWith( "Clan" ) )
			{
				token = st.nextToken();
			}

            clanName = st.nextToken();

			Matcher m = CLAN_ID_PATTERN.matcher( responseText );
			if ( m.find() )
			{
                clanId = StringUtilities.parseInt( m.group( 1 ) );
			}
		}

		if ( cleanHTML.contains( "\nTitle" ) )
		{
			while ( !token.startsWith( "Title" ) )
			{
				token = st.nextToken();
			}

            title = st.nextToken();
		}
	}

	/**
	 * static final method used by the clan manager in order to get an
	 * instance of a profile request based on the data already known.
	 */

	public static ProfileRequest getInstance( final String playerName, final String playerId,
		final String playerLevel, final String responseText, final String rosterRow )
	{
		ProfileRequest instance = new ProfileRequest( playerName );

		instance.playerId = playerId;

		// First, initialize the level field for the
		// current player.

		if ( playerLevel == null )
		{
			instance.playerLevel = IntegerPool.get( 0 ); 
		}
		else
		{
			instance.playerLevel = Integer.valueOf( playerLevel );
		}

		// Next, refresh the fields for this player.
		// The response text should be copied over
		// before this happens.

		instance.responseText = responseText;
		instance.refreshFields();

		// Next, parse out all the data in the
		// row of the detail roster table.

		if ( rosterRow == null )
		{
			instance.muscle = IntegerPool.get( 0 );
			instance.mysticism = IntegerPool.get( 0 );
			instance.moxie = IntegerPool.get( 0 );
			
			instance.rank = "";
			instance.karma = IntegerPool.get( 0 );
		}
		else
		{
			Matcher dataMatcher = ProfileRequest.DATA_PATTERN.matcher( rosterRow );
	
			// The name of the player occurs in the first
			// field of the table.  Because you already
			// know the name of the player, this can be
			// arbitrarily skipped.
	
			dataMatcher.find();
			
			// At some point the player class was added to the table.  Skip over it.
	
			dataMatcher.find();
	
			// The player's three primary stats appear in
			// the next three fields of the table.
	
			dataMatcher.find();
			instance.muscle = IntegerPool.get( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
	
			dataMatcher.find();
			instance.mysticism = IntegerPool.get( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
	
			dataMatcher.find();
			instance.moxie = IntegerPool.get( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
	
			// The next field contains the total power,
			// and since this is calculated, it can be
			// skipped in data retrieval.
	
			dataMatcher.find();
	
			// The next three fields contain the ascension
			// count, number of hardcore runs, and their
			// pvp ranking.
	
			dataMatcher.find();
			dataMatcher.find();
			dataMatcher.find();
	
			// Next is the player's rank inside of this clan.
			// Title was removed, so ... not visible here.
	
			dataMatcher.find();
			instance.rank = dataMatcher.group( 1 );
	
			// The last field contains the total karma
			// accumulated by this player.
	
			dataMatcher.find();
			instance.karma = IntegerPool.get( StringUtilities.parseInt( dataMatcher.group( 1 ) ) );
		}

		return instance;
	}

	/**
	 * static final method used by the flower hunter in order to get an
	 * instance of a profile request based on the data already known.
	 */

	public static ProfileRequest getInstance( final String playerName, final String playerId,
		final String clanName, final Integer playerLevel, final String classType, final Integer pvpRank )
	{
		ProfileRequest instance = new ProfileRequest( playerName );
		instance.playerId = playerId;
		instance.playerLevel = playerLevel;
		instance.clanName = clanName == null ? "" : clanName;
		instance.classType = classType;
		instance.pvpRank = pvpRank;

		return instance;
	}

	public void initialize()
	{
		if ( responseText == null )
		{
			RequestThread.postRequest( this );
		}
	}

	public String getPlayerName()
	{
		return playerName;
	}

	public String getPlayerId()
	{
		return playerId;
	}

	public String getClanName()
	{
        initialize();
		return clanName;
	}

	public int getClanId()
	{
        initialize();
		return clanId;
	}

	public boolean isHardcore()
	{
        initialize();
		return isHardcore;
	}

	public String getRestriction()
	{
        initialize();
		return restriction;
	}

	public String getClassType()
	{
		if ( classType == null )
		{
            initialize();
		}

		return classType;
	}

	public Integer getPlayerLevel()
	{
		if ( playerLevel == null || playerLevel == 0 )
		{
            initialize();
		}

		return playerLevel;
	}

	public Integer getCurrentMeat()
	{
        initialize();
		return currentMeat;
	}

	public Integer getTurnsPlayed()
	{
        initialize();
		return turnsPlayed;
	}

	public Integer getCurrentRun()
	{
        initialize();
		return currentRun;
	}

	public Date getLastLogin()
	{
        initialize();
		return lastLogin;
	}

	public Date getCreation()
	{
        initialize();
		return created;
	}

	public String getCreationAsString()
	{
        initialize();
		return ProfileRequest.OUTPUT_FORMAT.format( created );
	}

	public String getLastLoginAsString()
	{
        initialize();
		return ProfileRequest.OUTPUT_FORMAT.format( lastLogin );
	}

	public String getFood()
	{
        initialize();
		return food;
	}

	public String getDrink()
	{
        initialize();
		return drink;
	}

	public Integer getPvpRank()
	{
		if ( pvpRank == null || pvpRank == 0 )
		{
            initialize();
		}

		return pvpRank;
	}

	public Integer getMuscle()
	{
        initialize();
		return muscle;
	}

	public Integer getMysticism()
	{
        initialize();
		return mysticism;
	}

	public Integer getMoxie()
	{
        initialize();
		return moxie;
	}

	public Integer getPower()
	{
        initialize();
		return IntegerPool.get( muscle + mysticism + moxie );
	}

	public Integer getEquipmentPower()
	{
        initialize();
		return IntegerPool.get( equipmentPower );
	}

	public String getTitle()
	{
        initialize();
		return title != null ? title : ClanManager.getTitle( playerName );
	}

	public String getRank()
	{
        initialize();
		return rank;
	}

	public Integer getKarma()
	{
        initialize();
		return karma;
	}

	public Integer getAscensionCount()
	{
        initialize();
		return ascensionCount;
	}

	private static final Pattern GOBACK_PATTERN =
		Pattern.compile( "http://www[2345678]?\\.kingdomofloathing\\.com/ascensionhistory\\.php?back=self&who=([\\d]+)" );

	@Override
	public void processResults()
	{
		Matcher dataMatcher = ProfileRequest.GOBACK_PATTERN.matcher( responseText );
		if ( dataMatcher.find() )
		{
            responseText =
				dataMatcher.replaceFirst( "../ascensions/" + ClanManager.getURLName( ContactManager.getPlayerName( dataMatcher.group( 1 ) ) ) );
		}

        refreshFields();
	}

	public int compareTo( final Object o )
	{
		if ( o == null || !( o instanceof ProfileRequest ) )
		{
			return -1;
		}

		ProfileRequest pr = (ProfileRequest) o;

		if ( getPvpRank().intValue() != pr.getPvpRank().intValue() )
		{
			return getPvpRank() - pr.getPvpRank();
		}

		return getPlayerLevel() - pr.getPlayerLevel();
	}
}
