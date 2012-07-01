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
import java.awt.Component;

import java.awt.event.ActionListener;

import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BigBrotherRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.Crimbo11Request;
import net.sourceforge.kolmafia.request.CrimboCartelRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.DollHawkerRequest;
import net.sourceforge.kolmafia.request.FDKOLRequest;
import net.sourceforge.kolmafia.request.FreeSnackRequest;
import net.sourceforge.kolmafia.request.FudgeWandRequest;
import net.sourceforge.kolmafia.request.GameShoppeRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.IsotopeSmitheryRequest;
import net.sourceforge.kolmafia.request.LunarLunchRequest;
import net.sourceforge.kolmafia.request.MrStoreRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuartersmasterRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.SwaggerShopRequest;
import net.sourceforge.kolmafia.request.TicketCounterRequest;
import net.sourceforge.kolmafia.request.Tr4pz0rRequest;
import net.sourceforge.kolmafia.request.TravelingTraderRequest;
import net.sourceforge.kolmafia.request.VendingMachineRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersFrame
	extends GenericFrame
	implements ChangeListener
{
	private static final StorageRequest PULL_MR_A_REQUEST =
		new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY,
				    new AdventureResult[] { MrStoreRequest.MR_A } );

	private static final List lighthouseItems = CoinmastersDatabase.getItems( "Lighthouse" );

	private static CoinmastersFrame INSTANCE = null;

	private CardLayoutSelectorPanel selectorPanel = null;

	private CoinmasterPanel dimemasterPanel = null;
	private CoinmasterPanel quartersmasterPanel = null;
	private CoinmasterPanel bhhPanel = null;
	private CoinmasterPanel mrStorePanel = null;
	private CoinmasterPanel hermitPanel = null;
	private CoinmasterPanel bigBrotherPanel = null;
	private CoinmasterPanel arcadePanel = null;
	private CoinmasterPanel gameShoppePanel = null;
	private CoinmasterPanel freeSnackPanel = null;
	private CoinmasterPanel isotopeSmitheryPanel = null;
	private CoinmasterPanel dollhawkerPanel = null;
	private CoinmasterPanel lunarLunchPanel = null;
	private CoinmasterPanel awolPanel = null;
	private CoinmasterPanel fudgeWandPanel = null;
	private CoinmasterPanel travelerPanel = null;
	private CoinmasterPanel tr4pz0rPanel = null;
	private CoinmasterPanel vendingMachinePanel = null;
	private CoinmasterPanel swaggerShopPanel = null;
	private CoinmasterPanel BURTPanel = null;
	private CoinmasterPanel fdkolPanel = null;

	private CoinmasterPanel altarOfBonesPanel = null;
	private CoinmasterPanel crimboCartelPanel = null;
	private CoinmasterPanel CRIMBCOGiftShopPanel = null;
	private CoinmasterPanel crimbo11Panel = null;

	public CoinmastersFrame()
	{
		super( "Coin Masters" );

        selectorPanel = new CardLayoutSelectorPanel( "coinMasterIndex", "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
		JPanel panel;

		// Always available coinmasters
        selectorPanel.addCategory( "Always Available" );

		panel = new JPanel( new BorderLayout() );
		bhhPanel = new BountyHunterHunterPanel();
		panel.add( bhhPanel );
        selectorPanel.addPanel( bhhPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		mrStorePanel = new MrStorePanel();
		panel.add( mrStorePanel );
        selectorPanel.addPanel( mrStorePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		hermitPanel = new HermitPanel();
		panel.add( hermitPanel );
        selectorPanel.addPanel( hermitPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		tr4pz0rPanel = new Tr4pz0rPanel();
		panel.add( tr4pz0rPanel );
        selectorPanel.addPanel( tr4pz0rPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		vendingMachinePanel = new VendingMachinePanel();
		panel.add( vendingMachinePanel );
        selectorPanel.addPanel( vendingMachinePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		swaggerShopPanel = new SwaggerShopPanel();
		panel.add( swaggerShopPanel );
        selectorPanel.addPanel( swaggerShopPanel.getPanelSelector(), panel );

		// Ascension coinmasters
        selectorPanel.addSeparator();
        selectorPanel.addCategory( "Ascension" );

		panel = new JPanel( new BorderLayout() );
		dimemasterPanel = new DimemasterPanel();
		panel.add( dimemasterPanel );
        selectorPanel.addPanel( dimemasterPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		quartersmasterPanel = new QuartersmasterPanel();
		panel.add( quartersmasterPanel );
        selectorPanel.addPanel( quartersmasterPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		BURTPanel = new BURTPanel();
		panel.add( BURTPanel );
        selectorPanel.addPanel( BURTPanel.getPanelSelector(), panel );

		// Aftercore coinmasters
        selectorPanel.addSeparator();
        selectorPanel.addCategory( "Aftercore" );

		panel = new JPanel( new BorderLayout() );
		bigBrotherPanel = new BigBrotherPanel();
		panel.add( bigBrotherPanel );
        selectorPanel.addPanel( bigBrotherPanel.getPanelSelector(), panel );

		// IOTM coinmasters
        selectorPanel.addSeparator();
        selectorPanel.addCategory( "Item of the Month" );

		panel = new JPanel( new BorderLayout() );
		arcadePanel = new TicketCounterPanel();
		panel.add( arcadePanel );
        selectorPanel.addPanel( arcadePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		gameShoppePanel = new GameShoppePanel();
		panel.add( gameShoppePanel );
        selectorPanel.addPanel( gameShoppePanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		freeSnackPanel = new SnackVoucherPanel();
		panel.add( freeSnackPanel );
        selectorPanel.addPanel( freeSnackPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		isotopeSmitheryPanel = new IsotopeSmitheryPanel();
		panel.add( isotopeSmitheryPanel );
        selectorPanel.addPanel( isotopeSmitheryPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		dollhawkerPanel = new DollHawkerPanel();
		panel.add( dollhawkerPanel );
        selectorPanel.addPanel( dollhawkerPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		lunarLunchPanel = new LunarLunchPanel();
		panel.add( lunarLunchPanel );
        selectorPanel.addPanel( lunarLunchPanel.getPanelSelector(), panel );

		// Events coinmasters
        selectorPanel.addSeparator();
        selectorPanel.addCategory( "Special Events" );

		panel = new JPanel( new BorderLayout() );
		awolPanel = new CommendationPanel();
		panel.add( awolPanel );
        selectorPanel.addPanel( awolPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		fudgeWandPanel = new FudgeWandPanel();
		panel.add( fudgeWandPanel );
        selectorPanel.addPanel( fudgeWandPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		travelerPanel = new TravelingTraderPanel();
		panel.add( travelerPanel );
        selectorPanel.addPanel( travelerPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		fdkolPanel = new fdkolPanel();
		panel.add( fdkolPanel );
        selectorPanel.addPanel( fdkolPanel.getPanelSelector(), panel );

		// Removed coinmasters
        selectorPanel.addSeparator();
        selectorPanel.addCategory( "Removed" );

		panel = new JPanel( new BorderLayout() );
		altarOfBonesPanel = new AltarOfBonesPanel();
		panel.add( altarOfBonesPanel );
        selectorPanel.addPanel( altarOfBonesPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		crimboCartelPanel = new CrimboCartelPanel();
		panel.add( crimboCartelPanel );
        selectorPanel.addPanel( crimboCartelPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		CRIMBCOGiftShopPanel = new CRIMBCOGiftShopPanel();
		panel.add( CRIMBCOGiftShopPanel );
        selectorPanel.addPanel( CRIMBCOGiftShopPanel.getPanelSelector(), panel );

		panel = new JPanel( new BorderLayout() );
		crimbo11Panel = new Crimbo11Panel();
		panel.add( crimbo11Panel );
        selectorPanel.addPanel( crimbo11Panel.getPanelSelector(), panel );

        selectorPanel.addChangeListener( this );
        selectorPanel.setSelectedIndex( Preferences.getInteger( "coinMasterIndex" ) );

		JPanel wrapperPanel = new JPanel( new BorderLayout() );
		wrapperPanel.add( selectorPanel, BorderLayout.CENTER );
		wrapperPanel.add( new StatusPanel(), BorderLayout.SOUTH );

        setCenterComponent( wrapperPanel );

		CoinmastersFrame.INSTANCE = this;

		RequestThread.executeMethodAfterInitialization( getClass(), "externalUpdate" );
	}

	/**
	 * Whenever the tab changes, this method is used to change the title to count the coins of the new tab
	 */

	public void stateChanged( final ChangeEvent e )
	{
        setTitle();
	}

	private void setTitle()
	{
		JPanel panel = (JPanel) selectorPanel.currentPanel();
		Component cm = ( panel instanceof JPanel ) ? panel.getComponent( 0 ) : null;
		if ( cm instanceof CoinmasterPanel )
		{
			( (CoinmasterPanel) cm ).setTitle();
		}
	}

	public static void externalUpdate()
	{
		if ( INSTANCE == null )
		{
			return;
		}

		INSTANCE.update();
	}

	private void update()
	{
		dimemasterPanel.update();
		quartersmasterPanel.update();
		bhhPanel.update();
		mrStorePanel.update();
		hermitPanel.update();
		swaggerShopPanel.update();
		BURTPanel.update();
		tr4pz0rPanel.update();
		vendingMachinePanel.update();
		bigBrotherPanel.update();
		arcadePanel.update();
		gameShoppePanel.update();
		freeSnackPanel.update();
		isotopeSmitheryPanel.update();
		dollhawkerPanel.update();
		lunarLunchPanel.update();
		awolPanel.update();
		crimbo11Panel.update();
		fudgeWandPanel.update();
		travelerPanel.update();
		altarOfBonesPanel.update();
		crimboCartelPanel.update();
		CRIMBCOGiftShopPanel.update();
		fdkolPanel.update();
        setTitle();
	}

	private class DimemasterPanel
		extends WarMasterPanel
	{
		public DimemasterPanel()
		{
			super( DimemasterRequest.HIPPY, "hippy");
		}
	}

	private class QuartersmasterPanel
		extends WarMasterPanel
	{
		public QuartersmasterPanel()
		{
			super( QuartersmasterRequest.FRATBOY, "fratboy" );
		}
	}

	private class BountyHunterHunterPanel
		extends CoinmasterPanel
	{
		public BountyHunterHunterPanel()
		{
			super( BountyHunterHunterRequest.BHH );
		}
	}

	public class MrStorePanel
		extends CoinmasterPanel
	{
		private JButton pull = new InvocationButton( "pull Mr. A", this, "pull" );
		private int storageCount = 0;

		public MrStorePanel()
		{
			super( MrStoreRequest.MR_STORE );
            buyPanel.addButton( pull, false );
            storageInTitle = true;
            pullsInTitle = true;
            update();
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
            pull.setEnabled( isEnabled && storageCount > 0 );
		}

		@Override
		public void update()
		{
            storageCount = MrStoreRequest.MR_A.getCount( KoLConstants.storage );
			boolean canPull =
				KoLCharacter.isHardcore() ||
				ConcoctionDatabase.getPullsRemaining() != 0;
            pull.setEnabled( canPull && storageCount > 0 );
		}

		public void pull()
		{
			GenericRequest request = KoLCharacter.isHardcore() ?
				(GenericRequest) new MrStoreRequest( "pullmras" ) :
				(GenericRequest) CoinmastersFrame.PULL_MR_A_REQUEST;
			RequestThread.postRequest( request );
		}
	}

	public class HermitPanel
		extends CoinmasterPanel
	{
		private JButton fish = new InvocationButton( "go fish", this, "fish" );

		public HermitPanel()
		{
			super( HermitRequest.HERMIT );
            buyPanel.addButton( fish, true );
		}

		public void fish()
		{
			int available = HermitRequest.getWorthlessItemCount();
			AdventureResult item = HermitRequest.WORTHLESS_ITEM.getInstance( available + 1 );
			InventoryManager.retrieveItem( item, false );
		}
	}

	public class Tr4pz0rPanel
		extends CoinmasterPanel
	{
		public Tr4pz0rPanel()
		{
			super( Tr4pz0rRequest.L33T_TR4PZ0R );
		}
	}

	public class SwaggerShopPanel
		extends CoinmasterPanel
	{
		public SwaggerShopPanel()
		{
			super( SwaggerShopRequest.SWAGGER_SHOP );
		}
	}

	public class BURTPanel
		extends CoinmasterPanel
	{
		public BURTPanel()
		{
			super( BURTRequest.BURT );
		}
	}

	public class VendingMachinePanel
		extends CoinmasterPanel
	{
		public VendingMachinePanel()
		{
			super( VendingMachineRequest.VENDING_MACHINE );
		}
	}

	private class BigBrotherPanel
		extends CoinmasterPanel
	{
		public BigBrotherPanel()
		{
			super( BigBrotherRequest.BIG_BROTHER );
		}
	}

	private class Crimbo11Panel
		extends CoinmasterPanel
	{
		public Crimbo11Panel()
		{
			super();

            data = Crimbo11Request.CRIMBO11;

            sellPanel = new SellPanel();
            add( sellPanel, BorderLayout.NORTH );

			ActionListener[] listeners = new ActionListener[ 2 ];
			listeners[ 0 ] = new GiftListener();
			listeners[ 1 ] = new DonateListener();

            buyPanel = new BuyPanel( listeners );
            add( buyPanel, BorderLayout.CENTER );
		}

		public Object[] getDesiredItems()
		{
			Object[] items = buyPanel.elementList.getSelectedValues();
			return getDesiredBuyItems( items, false );
		}

		public class GiftListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				CoinmasterData data = Crimbo11Panel.this.data;
				String reason = data.canBuy();
				if ( reason != null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, reason );
					return;
				}

				Object[] items = getDesiredItems();
				if ( items == null )
				{
					return;
				}

				String victim = InputFieldUtilities.input( "Send a gift to whom?" );
				if ( victim == null )
				{
					return;
				}

				Crimbo11Panel.this.execute( data.getBuyAction(), items, "towho=" + victim );
			}

			@Override
			public String toString()
			{
				return "gift";
			}
		}

		public class DonateListener
			extends ThreadedListener
		{
			@Override
			protected void execute()
			{
				CoinmasterData data = Crimbo11Panel.this.data;
				String reason = data.canBuy();
				if ( reason != null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, reason );
					return;
				}

				Object[] items = getDesiredItems();
				if ( items == null )
				{
					return;
				}

				Crimbo11Panel.this.execute( data.getBuyAction(), items, "towho=0" );
			}

			@Override
			public String toString()
			{
				return "donate";
			}
		}
	}

	private class CrimboCartelPanel
		extends CoinmasterPanel
	{
		public CrimboCartelPanel()
		{
			super( CrimboCartelRequest.CRIMBO_CARTEL );
		}
	}

	public class TicketCounterPanel
		extends CoinmasterPanel
	{
		private JButton skeeball = new InvocationButton( "skeeball", this, "skeeball" );
		private int gameGridTokens = 0;

		public TicketCounterPanel()
		{
			super( TicketCounterRequest.TICKET_COUNTER );
            buyPanel.addButton( skeeball, false );
            update();
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			super.setEnabled( isEnabled );
            skeeball.setEnabled( isEnabled && gameGridTokens > 0 );
		}

		@Override
		public void update()
		{
            gameGridTokens = ArcadeRequest.TOKEN.getCount( KoLConstants.inventory );
            skeeball.setEnabled( gameGridTokens > 0 );
		}

		@Override
		public void setTitle( final StringBuffer buffer )
		{
            standardTitle( buffer );
			buffer.append( " (" );
			buffer.append( String.valueOf( gameGridTokens ) );
			buffer.append( " Game Grid tokens)" );
		}

		public void skeeball()
		{
			RequestThread.postRequest( new ArcadeRequest( "skeeball" ) );
		}
	}

	private class GameShoppePanel
		extends CoinmasterPanel
	{
		public GameShoppePanel()
		{
			super( GameShoppeRequest.GAMESHOPPE );
		}
	}

	private class SnackVoucherPanel
		extends CoinmasterPanel
	{
		public SnackVoucherPanel()
		{
			super( FreeSnackRequest.FREESNACKS );
		}
	}

	private class AltarOfBonesPanel
		extends CoinmasterPanel
	{
		public AltarOfBonesPanel()
		{
			super( AltarOfBonesRequest.ALTAR_OF_BONES );
		}
	}

	private class CRIMBCOGiftShopPanel
		extends CoinmasterPanel
	{
		public CRIMBCOGiftShopPanel()
		{
			super( CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP );
		}
	}

	private class CommendationPanel
		extends CoinmasterPanel
	{
		public CommendationPanel()
		{
			super( AWOLQuartermasterRequest.AWOL );
		}
	}

	private class FudgeWandPanel
		extends CoinmasterPanel
	{
		public FudgeWandPanel()
		{
			super( FudgeWandRequest.FUDGEWAND );
		}
	}

	private class TravelingTraderPanel
		extends CoinmasterPanel
	{
		public TravelingTraderPanel()
		{
			super( TravelingTraderRequest.TRAVELER );
		}
	}

	private class fdkolPanel
		extends CoinmasterPanel
	{
		public fdkolPanel()
		{
			super( FDKOLRequest.FDKOL );
		}
	}

	private class IsotopeSmitheryPanel
		extends CoinmasterPanel
	{
		public IsotopeSmitheryPanel()
		{
			super( IsotopeSmitheryRequest.ISOTOPE_SMITHERY );
		}

		@Override
		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	private class DollHawkerPanel
		extends CoinmasterPanel
	{
		public DollHawkerPanel()
		{
			super( DollHawkerRequest.DOLLHAWKER );
		}

		@Override
		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	private class LunarLunchPanel
		extends CoinmasterPanel
	{
		public LunarLunchPanel()
		{
			super( LunarLunchRequest.LUNAR_LUNCH );
		}

		@Override
		public boolean enabled()
		{
			return SpaaaceRequest.immediatelyAccessible();
		}
	}

	private abstract class WarMasterPanel
		extends CoinmasterPanel
	{
		private final String side;

		public WarMasterPanel( CoinmasterData data, String side )
		{
			super( data );
			this.side = side;
		}

		@Override
		public int buyDefault( final int max )
		{
			return max;
		}

		@Override
		public String lighthouseSide()
		{
			return side;
		}
	}

	public abstract class CoinmasterPanel
		extends JPanel
	{
		protected CoinmasterData data;
		protected boolean storageInTitle = false;
		protected boolean pullsInTitle = false;

		protected SellPanel sellPanel = null;
		protected BuyPanel buyPanel = null;

		public CoinmasterPanel()
		{
			super( new BorderLayout() );
		}

		public CoinmasterPanel( final CoinmasterData data )
		{
			super( new BorderLayout() );

			this.data = data;

			if ( data.getSellPrices() != null )
			{
				sellPanel = new SellPanel();
                add( sellPanel, BorderLayout.NORTH );
			}

			if ( data.getBuyPrices() != null )
			{
				buyPanel = new BuyPanel();
                add( buyPanel, BorderLayout.CENTER );
			}

            storageInTitle = this.data.getStorageAction() != null;
		}

		public CoinMasterRequest getRequest()
		{
			return data.getRequest();
		}

		public CoinMasterRequest getRequest( final String action, final AdventureResult it )
		{
			return data.getRequest( action, it );
		}

		public final void setTitle()
		{
			StringBuffer buffer = new StringBuffer();
            setTitle( buffer );
			CoinmastersFrame.this.setTitle( buffer.toString() );
		}

		public void setTitle( final StringBuffer buffer )
		{
            standardTitle( buffer );
		}

		public final void standardTitle( final StringBuffer buffer )
		{
			AdventureResult item = data.getItem();
			int count = data.availableTokens();
			String name = ( count != 1 ) ? data.getPluralToken() : data.getToken();
			buffer.append( "Coin Masters (" );
			buffer.append( String.valueOf( count ) );
			buffer.append( " " );
			buffer.append( name );

			// Makes no sense to show storage except for real items
			if ( storageInTitle && item != null )
			{
				int count1 = item.getCount( KoLConstants.storage );
				buffer.append( ", " );
				buffer.append( String.valueOf( count1 ) );
				buffer.append( " in storage" );

				// Only show pulls if we actually have the item in storage
				if ( pullsInTitle && count1 > 0 && !KoLCharacter.isHardcore() )
				{
					int pulls = ConcoctionDatabase.getPullsRemaining();
					buffer.append( ", " );
					buffer.append( KoLCharacter.inRonin() ? String.valueOf( pulls ) : "unlimited" );
					buffer.append( " pull" );
					buffer.append( pulls != 1 ? "s" : "" );
					buffer.append( " available" );
				}
			}

			buffer.append( ")" );
		}

		public void actionConfirmed()
		{
		}

		public void actionCancelled()
		{
		}

		public boolean addSellMovers()
		{
			return true;
		}

		public String getPanelSelector()
		{
			return "- " + data.getMaster();
		}

		public boolean enabled()
		{
			return data.isAccessible();
		}

		public String lighthouseSide()
		{
			return null;
		}

		public void update()
		{
		}

		public int buyDefault( final int max )
		{
			return 1;
		}

		public void check()
		{
			RequestThread.postRequest( getRequest() );
		}

		protected void execute( final String action, final Object [] items )
		{
            execute( action, items, null );
		}

		protected void execute( final String action, final Object [] items, final String extraAction )
		{
			if ( items.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult it = (AdventureResult)items[i];
				CoinMasterRequest request = getRequest( action, it );
				if ( extraAction != null )
				{
					request.addFormField( extraAction );
				}
				RequestThread.postRequest( request );
			}

			// Update our token count in the title
            setTitle();
		}

		public Object[] getDesiredBuyItems( Object[] items, final boolean fromStorage )
		{
			if ( items.length == 0 )
			{
				return null;
			}

			CoinmasterData data = this.data;
			int originalBalance = fromStorage ?
				data.availableStorageTokens() :
				data.availableTokens();

			int neededSize = items.length;
			int balance = originalBalance;
			Map buyPrices = data.getBuyPrices();

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];
				String itemName = item.getName();
				int price = CoinmastersDatabase.getPrice( itemName, buyPrices );

				if ( price > originalBalance )
				{
					// This was grayed out.
					items[ i ] = null;
					--neededSize;
					continue;
				}

				int max = balance / price;
				int quantity = max;

				if ( max > 1 )
				{
					int def = buyDefault( max );
					String value = InputFieldUtilities.input( "Buying " + itemName + "...", KoLConstants.COMMA_FORMAT.format( def ) );
					if ( value == null )
					{
						// He hit cancel
						return null;
					}

					quantity = StringUtilities.parseInt( value );
				}

				if ( quantity > max )
				{
					quantity = max;
				}

				if ( quantity <= 0 )
				{
					items[ i ] = null;
					--neededSize;
					continue;
				}

				items[ i ] = item.getInstance( quantity );
				balance -= quantity * price;
			}

			// Shrink the array which will be returned so
			// that it removes any nulled values.

			if ( neededSize == 0 )
			{
				return null;
			}

			Object[] desiredItems = new Object[ neededSize ];
			neededSize = 0;

			for ( int i = 0; i < items.length; ++i )
			{
				if ( items[ i ] != null )
				{
					desiredItems[ neededSize++ ] = items[ i ];
				}
			}

			return desiredItems;
		}

		public class SellPanel
			extends ItemManagePanel
		{
			public SellPanel()
			{
				super( KoLConstants.inventory );
                setButtons( true, new ActionListener[] {
						new SellListener(),
					} );

				Map sellPrices = data.getSellPrices();
                elementList.setCellRenderer( getCoinmasterRenderer( data, sellPrices, false, null ) );
                setEnabled( true );
                filterItems();
			}

			@Override
			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
                buttons[ 0 ].setEnabled( enabled() );
			}

			@Override
			public void addFilters()
			{
			}

			@Override
			public void addMovers()
			{
				if ( addSellMovers() )
				{
					super.addMovers();
				}
			}

			@Override
			public AutoFilterTextField getWordFilter()
			{
				return new SellableFilterField();
			}

			@Override
			public void actionConfirmed()
			{
			}

			@Override
			public void actionCancelled()
			{
			}

			public class SellListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					CoinmasterData data = CoinmasterPanel.this.data;
					String reason = data.canSell();
					if ( reason != null )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, reason );
						return;
					}

					if ( !InputFieldUtilities.confirm( "Are you sure you would like to trade in the selected items?" ) )
					{
						return;
					}

					Object[] items = getDesiredItems( "Selling" );
					if ( items == null )
					{
						return;
					}

					CoinmasterPanel.this.execute( data.getSellAction(), items );
				}

				@Override
				public String toString()
				{
					return "sell";
				}
			}

			private class SellableFilterField
				extends FilterItemField
			{
				@Override
				public boolean isVisible( final Object element )
				{
					if ( !( element instanceof AdventureResult ) )
					{
						return false;
					}
					AdventureResult ar = (AdventureResult)element;
					int price = CoinmastersDatabase.getPrice( ar.getName(), data.getSellPrices() );
					return ( price > 0 ) && super.isVisible( element );
				}
			}
		}

		public class BuyPanel
			extends ItemManagePanel
		{
			public BuyPanel( ActionListener[] listeners )
			{
				super( data.getBuyItems() );

				if ( listeners != null )
				{
                    setButtons( true, listeners );
				}

                eastPanel.add( new InvocationButton( "visit", CoinmasterPanel.this, "check" ), BorderLayout.SOUTH );

				Map buyPrices = data.getBuyPrices();
				String side = lighthouseSide();
                elementList.setCellRenderer( getCoinmasterRenderer( data, buyPrices, true, side ) );
                elementList.setVisibleRowCount( 6 );
                setEnabled( true );
			}

			public BuyPanel()
			{
				this( null );

				boolean storage = data.getStorageAction() != null;
				int count = storage ? 2 : 1;
				ActionListener[] listeners = new ActionListener[ count ];
				listeners[ 0 ] = new BuyListener();
				if ( count > 1 )
				{
					listeners[ 1 ] = new BuyUsingStorageListener();
				}

                setButtons( true, listeners );
                setEnabled( true );
			}

			public void addButton( final JButton button, final boolean save )
			{
				JButton[] buttons = new JButton[1 ];
				buttons[ 0 ] = button;
                addButtons( buttons, save );
			}

			@Override
			public void addButtons( final JButton[] buttons, final boolean save )
			{
				super.addButtons( buttons, save );
			}

			@Override
			public void setEnabled( final boolean isEnabled )
			{
				super.setEnabled( isEnabled );
				for ( int i = 0; buttons != null && i < buttons.length; ++i )
				{
                    buttons[ i ].setEnabled( enabled() );
				}
			}

			@Override
			public void addFilters()
			{
			}

			@Override
			public void addMovers()
			{
			}

			public Object[] getDesiredItems( final boolean fromStorage )
			{
				Object[] items = elementList.getSelectedValues();
				return getDesiredBuyItems( items, fromStorage );
			}

			public class BuyListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					CoinmasterData data = CoinmasterPanel.this.data;
					String reason = data.canBuy();
					if ( reason != null )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, reason );
						return;
					}

					Object[] items = getDesiredItems( false );
					if ( items == null )
					{
						return;
					}

					CoinmasterPanel.this.execute( data.getBuyAction(), items );
				}

				@Override
				public String toString()
				{
					return "buy";
				}
			}

			public class BuyUsingStorageListener
				extends ThreadedListener
			{
				@Override
				protected void execute()
				{
					Object[] items = getDesiredItems( true );
					if ( items == null )
					{
						return;
					}

					CoinmasterPanel.this.execute( data.getBuyAction(),
						 items,
                            data.getStorageAction() );
				}

				@Override
				public String toString()
				{
					return "from storage";
				}
			}
		}
	}

	public static DefaultListCellRenderer getCoinmasterRenderer( CoinmasterData data, Map prices, final boolean usesTokens, String side )
	{
		return new CoinmasterRenderer( data, prices, usesTokens, side );
	}

	private static class CoinmasterRenderer
		extends DefaultListCellRenderer
	{
		private CoinmasterData data;
		private Map prices;
		private boolean usesTokens;
		private String side;

		public CoinmasterRenderer( CoinmasterData data, final Map prices, final boolean usesTokens, String side )
		{
            setOpaque( true );
			this.data = data;
			this.prices = prices;
			this.usesTokens = usesTokens;
			this.side = side;
		}

		public boolean allowHighlight()
		{
			return true;
		}

		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null )
			{
				return defaultComponent;
			}

			if ( value instanceof AdventureResult )
			{
				return getRenderer( defaultComponent, (AdventureResult) value );
			}

			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			String name = ar.getName();
			String canonicalName = StringUtilities.getCanonicalName( name );

			if ( side != null &&
			     CoinmastersFrame.lighthouseItems.contains( canonicalName ) &&
			     !Preferences.getString( "sidequestLighthouseCompleted" ).equals( side ) )
			{
				return null;
			}

			Integer iprice = (Integer)prices.get( canonicalName );

			if ( iprice == null )
			{
				return defaultComponent;
			}

			int price = iprice;
			boolean show = CoinmastersDatabase.availableItem( canonicalName );

			if ( show && usesTokens )
			{
				int balance1 = data.availableTokens();
				int balance2 = data.availableStorageTokens();
				if ( price > balance1 && price > balance2 )
				{
					show = false;
				}
			}

			StringBuilder stringForm = new StringBuilder();
			stringForm.append( "<html>" );
			if ( !show )
			{
				stringForm.append( "<font color=gray>" );
			}
			stringForm.append( name );
			stringForm.append( " (" );
			stringForm.append( price );
			stringForm.append( " " );
			stringForm.append( price != 1 ? data.getPluralToken() : data.getToken() );
			stringForm.append( ")" );
			int count = ar.getCount();
			if ( count == -1 )
			{
				stringForm.append( " (unknown)" );
			}
			else if ( count != PurchaseRequest.MAX_QUANTITY )
			{
				stringForm.append( " (" );
				stringForm.append( KoLConstants.COMMA_FORMAT.format( count ) );
				stringForm.append( ")" );
			}
			if ( !show )
			{
				stringForm.append( "</font>" );
			}
			stringForm.append( "</html>" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}
}
