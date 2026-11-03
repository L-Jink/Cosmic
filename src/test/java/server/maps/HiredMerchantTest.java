package server.maps;

import client.Character;
import client.Client;
import client.inventory.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.PacketCreator;

import java.awt.Point;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HiredMerchantTest {
    private static final int ITEM_ID = 2000000;   // red potion
    private static final int PRICE = 1000;

    private HiredMerchant merchant;
    private PlayerShopItem listing;
    private Client buyerClient;
    private Character buyer;

    @BeforeEach
    void setUp() {
        Character owner = mock(Character.class);
        Client ownerClient = mock(Client.class);
        when(owner.getClient()).thenReturn(ownerClient);
        when(owner.getPosition()).thenReturn(new Point(0, 0));
        when(owner.getMap()).thenReturn(mock(MapleMap.class));
        when(owner.getName()).thenReturn("Owner");
        when(owner.getId()).thenReturn(1);

        merchant = new HiredMerchant(owner, "test", 5030000);
        // 5 bundles of 100 potions
        listing = new PlayerShopItem(new Item(ITEM_ID, (short) 0, (short) 100), (short) 5, PRICE);
        assertTrue(merchant.addItem(listing));

        buyerClient = mock(Client.class);
        buyer = mock(Character.class);
        when(buyerClient.getPlayer()).thenReturn(buyer);
        when(buyer.getMeso()).thenReturn(Integer.MAX_VALUE);
    }

    private void assertRejectedUntouched() {
        assertEquals(5, listing.getBundles());
        assertTrue(listing.isExist());
        verify(buyer, never()).gainMeso(any(Integer.class), any(Boolean.class));
        assertTrue(merchant.getSold().isEmpty());
    }

    @Test
    void closedShopCannotSell() {
        // never opened / closed by owner: stock is being returned to the owner, so it must not be sellable
        merchant.buy(buyerClient, 0, (short) 1);

        verify(buyerClient).sendPacket(PacketCreator.enableActions());
        assertRejectedUntouched();
    }

    @Test
    void shopClosedAfterOpeningCannotSell() {
        merchant.setOpen(true);
        merchant.setOpen(false);

        merchant.buy(buyerClient, 0, (short) 1);

        assertRejectedUntouched();
    }

    @Test
    void outOfRangeListingIndexIsRejectedWithoutException() {
        merchant.setOpen(true);

        merchant.buy(buyerClient, -1, (short) 1);
        merchant.buy(buyerClient, 1, (short) 1);
        merchant.buy(buyerClient, 127, (short) 1);

        assertRejectedUntouched();
    }

    @Test
    void cannotBuyMoreBundlesThanListed() {
        merchant.setOpen(true);

        merchant.buy(buyerClient, 0, (short) 6);          // partial stack: only 5 left
        merchant.buy(buyerClient, 0, Short.MAX_VALUE);
        merchant.buy(buyerClient, 0, (short) 0);
        merchant.buy(buyerClient, 0, (short) -1);

        assertRejectedUntouched();
    }

    @Test
    void soldOutListingCannotBeBoughtAgain() {
        merchant.setOpen(true);
        listing.setBundles((short) 0);
        listing.setDoesExist(false);

        merchant.buy(buyerClient, 0, (short) 1);

        verify(buyer, never()).gainMeso(any(Integer.class), any(Boolean.class));
        assertTrue(merchant.getSold().isEmpty());
    }

    @Test
    void takeItemBackWithBadSlotDoesNothing() {
        Character owner = mock(Character.class);
        merchant.takeItemBack(-1, owner);
        merchant.takeItemBack(7, owner);

        assertEquals(5, listing.getBundles());
        assertEquals(1, merchant.getItems().size());
    }

    @Test
    void getItemsIsASnapshot() {
        var snapshot = merchant.getItems();
        merchant.addItem(new PlayerShopItem(new Item(ITEM_ID, (short) 1, (short) 1), (short) 1, PRICE));

        assertEquals(1, snapshot.size());
        assertEquals(2, merchant.getItems().size());
    }

    @Test
    void concurrentBuyersCannotSellFromAClosedShop() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 50; j++) {
                        merchant.buy(buyerClient, 0, (short) 1);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertRejectedUntouched();
    }

    @Test
    void totalPriceUsesExactIntegerMath() {
        // 16,777,217 is not representable as a float: the old float math charged 16,777,216
        assertEquals(16_777_217L, HiredMerchant.totalPrice(16_777_217, 1));
        assertEquals(2_000_000_001L, HiredMerchant.totalPrice(2_000_000_001, 1));
        assertEquals(Integer.MAX_VALUE, HiredMerchant.totalPrice(Integer.MAX_VALUE, 1));
    }

    @Test
    void totalPriceOverflowIsRejected() {
        assertEquals(-1, HiredMerchant.totalPrice(Integer.MAX_VALUE, 2));
        assertEquals(-1, HiredMerchant.totalPrice(1_100_000_000, 2));
        assertEquals(2_000_000_000L, HiredMerchant.totalPrice(1_000_000_000, 2));
    }

    @Test
    void merchantMesoOverflowCheck() {
        assertTrue(HiredMerchant.canHoldMerchantMesos(0, Integer.MAX_VALUE));
        assertTrue(HiredMerchant.canHoldMerchantMesos(Integer.MAX_VALUE - 10, 10));
        assertFalse(HiredMerchant.canHoldMerchantMesos(Integer.MAX_VALUE - 10, 11));
        assertFalse(HiredMerchant.canHoldMerchantMesos(Integer.MAX_VALUE, 1));
    }
}
