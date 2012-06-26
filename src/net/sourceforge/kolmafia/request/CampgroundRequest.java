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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundRequest
	extends GenericRequest
{
	private static final Pattern LIBRAM_PATTERN =
		Pattern.compile( "Summon (Candy Heart|Party Favor|Love Song|BRICKOs|Dice|Resolutions) *.[(]([\\d,]+) MP[)]" );
	private static final Pattern HOUSING_PATTERN =
		Pattern.compile( "/rest(\\d+|a)(tp)?(_free)?.gif" );
	private static final Pattern FURNISHING_PATTERN =
		Pattern.compile( "<b>(?:an? )?(.*?)</b>" );

	private static int currentDwellingLevel = 0;
	private static AdventureResult currentDwelling = null;
	private static AdventureResult currentBed = null;

	public static final AdventureResult BIG_ROCK = ItemPool.get( ItemPool.BIG_ROCK, 1 );

	public static final AdventureResult BLACK_BLUE_LIGHT = ItemPool.get( ItemPool.BLACK_BLUE_LIGHT, 1 );
	public static final AdventureResult LOUDMOUTH_LARRY = ItemPool.get( ItemPool.LOUDMOUTH_LARRY, 1 );
	public static final AdventureResult PLASMA_BALL = ItemPool.get( ItemPool.PLASMA_BALL, 1 );

	public static final int [] campgroundItems =
	{
		// Housing
		ItemPool.BIG_ROCK,
		ItemPool.NEWBIESPORT_TENT,
		ItemPool.BARSKIN_TENT,
		ItemPool.COTTAGE,
		ItemPool.BRICKO_PYRAMID,
		ItemPool.HOUSE,
		ItemPool.SANDCASTLE,
		ItemPool.TWIG_HOUSE,
		ItemPool.GINGERBREAD_HOUSE,
		ItemPool.HOBO_FORTRESS,

		// Bedding
		ItemPool.BEANBAG_CHAIR,
		ItemPool.COLD_BEDDING,
		ItemPool.GAUZE_HAMMOCK,
		ItemPool.HOT_BEDDING,
		ItemPool.SLEAZE_BEDDING,
		ItemPool.SPOOKY_BEDDING,
		ItemPool.STENCH_BEDDING,
		ItemPool.SLEEPING_STOCKING,

		// Inside dwelling
		ItemPool.BLACK_BLUE_LIGHT,
		ItemPool.FENG_SHUI,
		ItemPool.LOUDMOUTH_LARRY,
		ItemPool.PLASMA_BALL,

		// Kitchen
		ItemPool.SHAKER,
		ItemPool.COCKTAIL_KIT,
		ItemPool.BARTENDER,
		ItemPool.CLOCKWORK_BARTENDER,
		ItemPool.OVEN,
		ItemPool.RANGE,
		ItemPool.CHEF,
		ItemPool.CLOCKWORK_CHEF,

		// Garden
		ItemPool.PUMPKIN,
		ItemPool.HUGE_PUMPKIN,
		ItemPool.GINORMOUS_PUMPKIN,
		ItemPool.PEPPERMINT_SPROUT,
		ItemPool.GIANT_CANDY_CANE,

		// Outside dwelling
		ItemPool.CLOCKWORK_MAID,
		ItemPool.MAID,
		ItemPool.MEAT_GOLEM,
		ItemPool.PAGODA_PLANS,
		ItemPool.SCARECROW,
		ItemPool.TOILET_PAPER,

		// Special item that aids resting
		ItemPool.COMFY_BLANKET,
	};

	public static final AdventureResult PUMPKIN = ItemPool.get( ItemPool.PUMPKIN, 1 );
	public static final AdventureResult HUGE_PUMPKIN = ItemPool.get( ItemPool.HUGE_PUMPKIN, 1 );
	public static final AdventureResult GINORMOUS_PUMPKIN = ItemPool.get( ItemPool.GINORMOUS_PUMPKIN, 1 );
	public static final AdventureResult PEPPERMINT_SPROUT = ItemPool.get( ItemPool.PEPPERMINT_SPROUT, 1 );
	public static final AdventureResult GIANT_CANDY_CANE = ItemPool.get( ItemPool.GIANT_CANDY_CANE, 1 );

	public static final AdventureResult [] CROPS =
	{
		CampgroundRequest.PUMPKIN,
		CampgroundRequest.HUGE_PUMPKIN,
		CampgroundRequest.GINORMOUS_PUMPKIN,
		CampgroundRequest.PEPPERMINT_SPROUT,
		CampgroundRequest.GIANT_CANDY_CANE,
	};

	public static void reset()
	{
		KoLConstants.campground.clear();
		CampgroundRequest.currentDwellingLevel = 0;
		CampgroundRequest.currentDwelling = null;
		CampgroundRequest.currentBed = null;
	}

	private final String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code> with the specified
	 * action in mind.
	 */

	// campground.php?action=garden&pwd

	public CampgroundRequest( final String action )
	{
		super( "campground.php" );
		this.addFormField( "action", action );
		this.action = action;
	}

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 */

	public CampgroundRequest()
	{
		this( "inspectdwelling" );
	}

	@Override
	public int getAdventuresUsed()
	{
		return this.action.equals( "rest" ) ? 1 : 0;
	}

	private static void setCampgroundItem( final int itemId, int count )
	{
		CampgroundRequest.setCampgroundItem ( ItemPool.get( itemId, count ) );
	}

	private static void setCampgroundItem( final AdventureResult item )
	{
		int i = KoLConstants.campground.indexOf( item );
		if ( i != -1 )
		{
			AdventureResult old = (AdventureResult)KoLConstants.campground.get( i );
			if ( old.getCount() == item.getCount() )
			{
				return;
			}
			KoLConstants.campground.remove( i );
		}
		KoLConstants.campground.add( item );
	}

	public static void removeCampgroundItem( AdventureResult item )
	{
		int i = KoLConstants.campground.indexOf( item );
		if ( i != -1 )
		{
			KoLConstants.campground.remove( i );
		}
	}

	private static int getCropIndex()
	{
		for ( int i = 0; i < CROPS.length; ++i )
		{
			int index = KoLConstants.campground.indexOf( CROPS[ i ] );
			if ( index != -1 )
			{
				return index;
			}
		}
		return -1;
	}

	public static AdventureResult getCrop()
	{
		int i = CampgroundRequest.getCropIndex();
		return i != -1 ? (AdventureResult)KoLConstants.campground.get( i ) : null;
	}

	public static boolean hasCrop( final String crop )
	{
		int i = CampgroundRequest.getCropIndex();
		return i != -1 && crop.equals( ((AdventureResult)KoLConstants.campground.get( i )).getName() );
	}

	public static boolean hasCropOrBetter( final String crop )
	{
		// Get current crop, if any
		AdventureResult current = CampgroundRequest.getCrop();
		if ( current == null || current.getCount() == 0 )
		{
			// Nothing in your garden or no garden.
			return false;
		}

		// We want whatever is there.  Since we made it this far,
		// we have something to pick.
		if ( crop.equals( "any" ) )
		{
			return true;
		}

		// If it equals the desired crop, peachy. Or is it pumpkiny?
		String currentName = current.getName();
		if ( crop.equals( currentName )  )
		{
			return true;
		}

		// Iterate through CROPS.
		for ( int i = 0; i < CROPS.length; ++i )
		{
			String cropName = CROPS[ i ].getName();
			// We found the current crop before we found the
			// desired crop. Not good enough.
			if ( cropName.equals( currentName ) )
			{
				return false;
			}
			// We found the desired crop before we found the
			// current crop - which must be better. Cool.
			if ( cropName.equals( crop ) )
			{
				return true;
			}
		}

		// Shouldn't get here - didn't find either the current or the desired crop
		return false;
	}

	public static void clearCrop()
	{
		int i = CampgroundRequest.getCropIndex();
		if ( i != -1 )
		{
			KoLConstants.campground.remove( i );
		}
	}

	public static void harvestCrop()
	{
		AdventureResult crop = CampgroundRequest.getCrop();
		if ( crop != null && crop.getCount() > 0 )
		{
			RequestThread.postRequest( new CampgroundRequest( "garden" ) );
		}
	}

	@Override
	public void run()
	{
		if ( this.action.equals( "rest" ) &&
		     KoLCharacter.getCurrentHP() == KoLCharacter.getMaximumHP() &&
		     KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP() )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "You don't need to rest." );
			return;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		CampgroundRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return;
		}

		Matcher matcher= GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			CampgroundRequest.parseCampground( responseText );
			return;
		}

		String action = matcher.group(1);

		// A request can have both action=bookshelf and preaction=yyy.
		// Check for that.
		if ( action.equals( "bookshelf" ) && matcher.find() )
		{
			action = matcher.group(1);
		}

		if ( action.equals( "bookshelf" ) )
		{
			// No preaction. Look at books.
			CampgroundRequest.parseBookTitles( responseText );
			return;
		}

		if ( action.endsWith( "powerelvibratoportal" ) )
		{
			PortalRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( action.startsWith( "telescope" ) )
		{
			TelescopeRequest.parseResponse( urlString, responseText );
			return;
		}

		// Using a book skill from the Mystic Bookshelf does this:
		//   campground.php?quantity=1&preaction=summonlovesongs&pwd
		//
		// Using a book skill from the skill menu redirects to the
		// above URL with an additional field:
		//   skilluse=1

		// Combining clip arts does this:
		//   campground.php?action=bookshelf&preaction=combinecliparts&clip1=05&clip2=05&clip3=03&pwd

		if ( action.startsWith( "summon" ) ||
		     action.equals( "combinecliparts" ) )
		{
			UseSkillRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( action.equals( "rest" ) )
		{
			Preferences.increment( "timesRested", 1 );

			// Your black-and-blue light cycles wildly between
			// black and blue, then emits a shower of sparks as it
			// goes permanently black.
			if ( responseText.indexOf( "goes permanently black" ) != -1 )
			{
				CampgroundRequest.removeCampgroundItem( BLACK_BLUE_LIGHT );
			}

			// Your blue plasma ball crackles weakly, emits a whine
			// that sounds like "pika...pika...pika..." and goes
			// dark.
			if ( responseText.indexOf( "crackles weakly" ) != -1 )
			{
				CampgroundRequest.removeCampgroundItem( PLASMA_BALL );
			}

			// Your Loudmouth Larry Lamprey twitches and flops
			// wildly, singing "Daisy, Daisy, tell me your answer
			// true," in ever-slower, distorted loops. Looks like
			// it's ready to go to its eternal fishy reward.
			if ( responseText.indexOf( "eternal fishy reward" ) != -1 )
			{
				CampgroundRequest.removeCampgroundItem( LOUDMOUTH_LARRY );
			}

			// You dream that your teeth fall out, and you put them
			// in your pocket for safe keeping. Fortunately, when
			// you wake up, you appear to have grown a new set.
			if ( responseText.indexOf( "your teeth fall out" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.LOOSE_TEETH, 1 );
			}

			// "Hey," he says, "youse got some teeth. T'anks. Here
			// youse goes."
			if ( responseText.indexOf( "youse got some teeth" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.LOOSE_TEETH, -1 );
			}

			return;
		}

		if ( action.equals( "garden" ) )
		{
			CampgroundRequest.clearCrop();
			CampgroundRequest.parseCampground( responseText );
			return;
		}

		if ( action.equals( "inspectdwelling" ) )
		{
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseDwelling( responseText );
			return;
		}

		if ( action.equals( "inspectkitchen" ) )
		{
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseKitchen( responseText );
			return;
		}
	}

	private static final void parseCampground( final String responseText )
	{
		KoLCharacter.setTelescope( responseText.indexOf( "action=telescope" ) != -1 );
		KoLCharacter.setBookshelf( responseText.indexOf( "action=bookshelf" ) != -1 );

		if ( responseText.contains( "smashstone.gif" ) && !KoLCharacter.getHippyStoneBroken() )
		{
			KoLCharacter.setHippyStoneBroken( true );
			RequestThread.postRequest( new PeeVPeeRequest( "fight" ) );
		}

		findImage( responseText, "pagoda.gif", ItemPool.PAGODA_PLANS );
		findImage( responseText, "maid.gif", ItemPool.MAID );
		findImage( responseText, "maid2.gif", ItemPool.CLOCKWORK_MAID );
		findImage( responseText, "scarecrow.gif", ItemPool.SCARECROW );
		findImage( responseText, "golem.gif", ItemPool.MEAT_GOLEM );

		findImage( responseText, "pumpkinpatch_0.gif", ItemPool.PUMPKIN, 0 );
		findImage( responseText, "pumpkinpatch_1.gif", ItemPool.PUMPKIN, 1 );
		findImage( responseText, "pumpkinpatch_2.gif", ItemPool.PUMPKIN, 2 );
		findImage( responseText, "pumpkinpatch_3.gif", ItemPool.PUMPKIN, 3 );
		findImage( responseText, "pumpkinpatch_4.gif", ItemPool.PUMPKIN, 4 );
		findImage( responseText, "pumpkinpatch_giant.gif", ItemPool.HUGE_PUMPKIN, 1 );
		findImage( responseText, "pumpkinpatch_ginormous.gif", ItemPool.GINORMOUS_PUMPKIN, 1 );
		findImage( responseText, "pepperpatch_0.gif", ItemPool.PEPPERMINT_SPROUT, 0 );
		findImage( responseText, "pepperpatch_1.gif", ItemPool.PEPPERMINT_SPROUT, 3 );
		findImage( responseText, "pepperpatch_2.gif", ItemPool.PEPPERMINT_SPROUT, 6 );
		findImage( responseText, "pepperpatch_3.gif", ItemPool.PEPPERMINT_SPROUT, 9 );
		findImage( responseText, "pepperpatch_4.gif", ItemPool.PEPPERMINT_SPROUT, 12 );
		findImage( responseText, "pepperpatch_huge.gif", ItemPool.GIANT_CANDY_CANE, 1 );
	}

	private static final void parseDwelling( final String responseText )
	{
		Matcher m = HOUSING_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to parse housing!" );
			return;
		}

		String dwelling = m.group( 1 );
		if ( dwelling.equals( "a" ) )
		{
			dwelling = "10";
		}

		int itemId = -1;
		switch ( StringUtilities.parseInt( dwelling ) )
		{
		case 0:
			// placeholder for "the ground"
			CampgroundRequest.currentDwelling = BIG_ROCK;
			CampgroundRequest.currentDwellingLevel = 0;
			break;
		case 1:
			itemId = ItemPool.NEWBIESPORT_TENT;
			break;
		case 2:
			itemId = ItemPool.BARSKIN_TENT;
			break;
		case 3:
			itemId = ItemPool.COTTAGE;
			break;
		case 4:
			itemId = ItemPool.HOUSE;
			break;
		case 5:
			itemId = ItemPool.SANDCASTLE;
			break;
		case 6:
			itemId = ItemPool.TWIG_HOUSE;
			break;
		case 7:
			itemId = ItemPool.HOBO_FORTRESS;
			break;
		case 8:
			itemId = ItemPool.GINGERBREAD_HOUSE;
			break;
		case 9:
			itemId = ItemPool.BRICKO_PYRAMID;
			break;
		case 10:
			itemId = ItemPool.GINORMOUS_PUMPKIN;
			break;
		default:
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unrecognized housing type (" + CampgroundRequest.currentDwellingLevel + ")!" );
			break;
		}

		if ( itemId != -1 )
		{
			CampgroundRequest.currentDwelling = ItemPool.get( itemId, 1 );
			CampgroundRequest.currentDwellingLevel = CampgroundRequest.dwellingLevel( itemId );
		}

		if ( m.group( 2 ) != null )
		{
			CampgroundRequest.setCampgroundItem( ItemPool.TOILET_PAPER, 1 );
		}

		// TODO: check free rest status (m.group(3)!=null)
		// against timesRested, adjust it if there appear to
		// have been rests used outside of KoLmafia.

		int startIndex = responseText.indexOf( "Your dwelling has the following stuff" );
		int endIndex = responseText.indexOf( "<b>Your Campsite</b>", startIndex + 1 );
		if ( startIndex > 0 && endIndex > 0 )
		{
			m = FURNISHING_PATTERN.matcher( responseText.substring( startIndex, endIndex ) );
			while ( m.find() )
			{
				String name = m.group(1);

				if ( name.equals( "Really Good Feng Shui" ) )
				{
					name = "Feng Shui for Big Dumb Idiots";
				}

				AdventureResult ar = ItemPool.get( name, 1 );
				if ( CampgroundRequest.isBedding( ar.getItemId() ) )
				{
					CampgroundRequest.currentBed = ar;
				}

				CampgroundRequest.setCampgroundItem( ar );
			}
		}
	}

	private static final void parseKitchen( final String responseText )
	{
		boolean hasOven = findImage( responseText, "ezcook.gif", ItemPool.OVEN );
		KoLCharacter.setOven( hasOven );

		boolean hasRange = findImage( responseText, "oven.gif", ItemPool.RANGE );
		KoLCharacter.setRange( hasRange );

		boolean hasChef =
			findImage( responseText, "chefinbox.gif", ItemPool.CHEF ) ||
			findImage( responseText, "cchefbox.gif", ItemPool.CLOCKWORK_CHEF );
		KoLCharacter.setChef( hasChef );

		boolean hasShaker = findImage( responseText, "shaker.gif", ItemPool.SHAKER );
		KoLCharacter.setShaker( hasShaker );

		boolean hasCocktailKit = findImage( responseText, "cocktailkit.gif", ItemPool.COCKTAIL_KIT );
		KoLCharacter.setCocktailKit( hasCocktailKit );

		boolean hasBartender =
			findImage( responseText, "bartinbox.gif", ItemPool.BARTENDER ) ||
			findImage( responseText, "cbartbox.gif", ItemPool.CLOCKWORK_BARTENDER );
		KoLCharacter.setBartender( hasBartender );

		boolean hasSushiMat = findImage( responseText, "sushimat.gif", ItemPool.SUSHI_ROLLING_MAT );
		KoLCharacter.setSushiMat( hasSushiMat );
	}

	private static boolean findImage( final String responseText, final String filename, final int itemId )
	{
		return CampgroundRequest.findImage( responseText, filename, itemId, false );
	}

	private static boolean findImage( final String responseText, final String filename, final int itemId, boolean allowMultiple )
	{
		int count = 0;
		int i = responseText.indexOf( filename );
		while ( i != -1 )
		{
			++count;
			i = responseText.indexOf( filename, i + 1 );
		}

		if ( count > 0 )
		{
			CampgroundRequest.setCampgroundItem( itemId, allowMultiple ? count : 1 );
		}

		return ( count > 0 );
	}

	private static boolean findImage( final String responseText, final String filename, final int itemId, int count )
	{
		int i = responseText.indexOf( filename );
		if ( i == -1 )
		{
			return false;
		}

		CampgroundRequest.setCampgroundItem( itemId, count );

		return true;
	}

	public static AdventureResult getCurrentDwelling()
	{
		return currentDwelling == null ? BIG_ROCK : currentDwelling;
	}

	public static int getCurrentDwellingLevel()
	{
		return currentDwellingLevel;
	}

	public static AdventureResult getCurrentBed()
	{
		return currentBed;
	}

	public static boolean isDwelling( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.BRICKO_PYRAMID:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.GINGERBREAD_HOUSE:
		case ItemPool.HOBO_FORTRESS:
		case ItemPool.GINORMOUS_PUMPKIN:
			return true;
		}
		return false;
	}

	public static int dwellingLevel( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.NEWBIESPORT_TENT:
			return 1;
		case ItemPool.BARSKIN_TENT:
			return 2;
		case ItemPool.COTTAGE:
			return 3;
		case ItemPool.BRICKO_PYRAMID:
			return 4;
		case ItemPool.HOUSE:
			return 5;
		case ItemPool.SANDCASTLE:
			return 6;
		case ItemPool.GINORMOUS_PUMPKIN:
			return 7;
		case ItemPool.TWIG_HOUSE:
			return 8;
		case ItemPool.GINGERBREAD_HOUSE:
			return 9;
		case ItemPool.HOBO_FORTRESS:
			return 10;
		}
		return 0;
	}

	public static boolean isBedding( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
			return true;
		}
		return false;
	}

	private static final String[][] BOOKS =
	{
		{
			"Tome of Snowcone Summoning",
			"Summon Snowcones"
		},
		{
			"Tome of Sticker Summoning",
			"Summon Stickers"
		},
		{
			"Tome of Sugar Shummoning",
			"Summon Sugar Sheets"
		},
		{
			"Tome of Clip Art",
			"Summon Clip Art"
		},
		{
			// The bookshelf currently says:
			// "McPhee's Grimoire of Hilarious Item Summoning"
			// gives access to "Summon Hilarious Items".
			//
			// The item is currently named:
			// "McPhee's Grimoire of Hilarious Object Summoning"
			// and gives access to "Summon Hilarious Objects".
			"McPhee's Grimoire",
			"Summon Hilarious Objects",
		},
		{
			"Sp'n-Zor's Grimoire",
			"Summon Tasteful Items",
		},
		{
			"Sorcerers of the Shore Grimoire",
			"Summon Alice's Army Cards",
		},
		{
			"Libram of Candy Heart Summoning",
			"Summon Candy Hearts",
		},
		{
			"Libram of Divine Favors",
			"Summon Party Favor"
		},
		{
			"Libram of Love Songs",
			"Summon Love Song"
		},
		{
			"Libram of BRICKOs",
			"Summon BRICKOs"
		},
		{
			"Gygaxian Libram",
			"Summon Dice"
		},
		{
			"Libram of Resolutions",
			"Summon Resolutions"
		},
	};

	private static void parseBookTitles( final String responseText )
	{
		if ( ( KoLCharacter.inBadMoon() || KoLCharacter.inAxecore() ) &&
		     !KoLCharacter.kingLiberated() )
		{
			// You can't use Mr. Skills in Bad Moon
			// You can't use Mr. Skills as an Avatar of Boris
			return;
		}

		String libram = null;
		for ( int i = 0; i < BOOKS.length; ++i )
		{
			String book = BOOKS[i][0];
			if ( responseText.indexOf( book ) != -1 )
			{
				String skill = BOOKS[i][1];
				KoLCharacter.addAvailableSkill( skill, true );
				if ( book.indexOf( "Libram" ) != -1 )
				{
					libram = skill;
				}
			}
		}

		if ( libram != null )
		{
			Matcher matcher = CampgroundRequest.LIBRAM_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				int cost = StringUtilities.parseInt( matcher.group(2) );
				SkillDatabase.setLibramSkillCasts( cost );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return false;
		}

		Matcher matcher= GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			// Simple visit. Nothing to log.
			return true;
		}

		String action = matcher.group(1);
		if ( action.equals( "bookshelf" ) )
		{
			// A request can have both action=bookshelf and preaction=yyy.
			// Check for that.
			if ( !matcher.find() )
			{
				// Nothing to log.
				return true;
			}
			action = matcher.group(1);
		}

		// Dispatch campground requests to other classes

		if ( action.endsWith( "elvibratoportal" ) )
		{
			return PortalRequest.registerRequest( urlString );
		}

		if ( action.startsWith( "telescope" ) )
		{
			return TelescopeRequest.registerRequest( urlString );
		}

		// campground.php?pwd&action=bookshelf&preaction=combinecliparts&clip1=05&clip2=05&clip3=03
		// 01 = DONUT
		// 02 = BOMB
		// 03 = KITTEN
		// 04 = WINE
		// 05 = CHEESE
		// 06 = LIGHT BULB
		// 07 = SNOWFLAKE
		// 08 = SKULL
		// 09 = CLOCK
		// 10 = HAMMER

		if ( action.startsWith( "summon" ) ||
		     action.equals( "combinecliparts" ) )
		{
			// Detect a redirection to campground.php from
			// skills.php The first one was already logged.
			if ( urlString.indexOf( "skilluse=1" ) != -1 )
			{
				return true;
			}
			return UseSkillRequest.registerRequest( urlString );
		}

		// Dispatch campground requests from this class

		if ( action.equals( "inspectdwelling" ) ||
		     action.equals( "inspectkitchen" ))
		{
			// Nothing to log.
			return true;
		}

		String message = null;

		if ( action.equals( "garden" ) )
		{
			message = "Harvesting your garden";
		}
		else if ( action.equals( "rest" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your dwelling";
		}
		else
		{
			// Unknown action.
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
		return true;
	}
}
