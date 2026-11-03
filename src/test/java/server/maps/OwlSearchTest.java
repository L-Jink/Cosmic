package server.maps;

import client.Character;
import client.Client;
import client.inventory.Item;
import net.server.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.Items;
import testutil.Mocks;
import tools.Pair;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwlSearchTest {
    private static final int ITEM = 2000000;
    private static final int OTHER_ITEM = 2000001;

    private Character owner;

    @BeforeEach
    void setUp() {
        owner = Mocks.chr("Owner");
        Client client = mock(Client.class);
        when(client.getChannel()).thenReturn(1);
        when(owner.getClient()).thenReturn(client);
        when(owner.getPosition()).thenReturn(new java.awt.Point(0, 0));
    }

    private static PlayerShopItem listing(int itemId, int bundles, int price) {
        Item item = Items.itemWithQuantity(itemId, 1);
        return new PlayerShopItem(item, (short) bundles, price);
    }

    private HiredMerchant merchant(boolean open, PlayerShopItem... items) {
        HiredMerchant hm = new HiredMerchant(owner, "desc", 5030000);
        for (PlayerShopItem i : items) {
            hm.addItem(i);
        }
        if (open) {
            hm.setOpen(true);
        }
        return hm;
    }

    private PlayerShop playerShop(boolean open, PlayerShopItem... items) {
        PlayerShop ps = new PlayerShop(owner, "desc", 4080000);
        for (PlayerShopItem i : items) {
            ps.addItem(i);
        }
        ps.setOpen(open);
        return ps;
    }

    @Test
    void merchantReturnsOnlyMatchingItemId() {
        HiredMerchant hm = merchant(true, listing(ITEM, 3, 100), listing(OTHER_ITEM, 3, 50));
        List<PlayerShopItem> res = hm.sendAvailableBundles(ITEM);
        assertEquals(1, res.size());
        assertEquals(100, res.get(0).getPrice());
    }

    @Test
    void merchantExcludesSoldOutListings() {
        HiredMerchant hm = merchant(true, listing(ITEM, 0, 100), listing(ITEM, 2, 200));
        List<PlayerShopItem> res = hm.sendAvailableBundles(ITEM);
        assertEquals(1, res.size());
        assertEquals(200, res.get(0).getPrice());
    }

    @Test
    void closedMerchantReturnsNothing() {
        HiredMerchant hm = merchant(false, listing(ITEM, 2, 100));
        assertTrue(hm.sendAvailableBundles(ITEM).isEmpty());
    }

    @Test
    void playerShopOpenReturnsMatchesAndSkipsSoldOut() {
        PlayerShop ps = playerShop(true, listing(ITEM, 0, 100), listing(ITEM, 1, 300), listing(OTHER_ITEM, 1, 5));
        List<PlayerShopItem> res = ps.sendAvailableBundles(ITEM);
        assertEquals(1, res.size());
        assertEquals(300, res.get(0).getPrice());
    }

    @Test
    void closedPlayerShopReturnsNothing() {
        PlayerShop ps = playerShop(false, listing(ITEM, 2, 100));
        assertTrue(ps.sendAvailableBundles(ITEM).isEmpty());
    }

    @Test
    void removedListingIsExcluded() {
        PlayerShopItem gone = listing(ITEM, 2, 100);
        gone.setDoesExist(false);
        HiredMerchant hm = merchant(true, gone);
        assertTrue(hm.sendAvailableBundles(ITEM).isEmpty());
    }

    @Test
    void resultsSortedAscendingByPriceWithoutOverflow() {
        PlayerShop ps = playerShop(true);
        List<Pair<PlayerShopItem, AbstractMapObject>> in = new ArrayList<>();
        in.add(new Pair<>(listing(ITEM, 1, Integer.MAX_VALUE), ps));
        in.add(new Pair<>(listing(ITEM, 1, 1), ps));
        in.add(new Pair<>(listing(ITEM, 1, 1_500_000_000), ps));
        List<Pair<PlayerShopItem, AbstractMapObject>> out = World.sortAndTruncateOwlResults(in);
        assertEquals(1, out.get(0).getLeft().getPrice());
        assertEquals(1_500_000_000, out.get(1).getLeft().getPrice());
        assertEquals(Integer.MAX_VALUE, out.get(2).getLeft().getPrice());
    }

    @Test
    void resultsCappedAt200Cheapest() {
        PlayerShop ps = playerShop(true);
        List<Pair<PlayerShopItem, AbstractMapObject>> in = new ArrayList<>();
        for (int i = 300; i >= 1; i--) {
            in.add(new Pair<>(listing(ITEM, 1, i), ps));
        }
        List<Pair<PlayerShopItem, AbstractMapObject>> out = World.sortAndTruncateOwlResults(in);
        assertEquals(World.OWL_MAX_RESULTS, out.size());
        assertEquals(1, out.get(0).getLeft().getPrice());
        assertEquals(200, out.get(199).getLeft().getPrice());
    }
}
