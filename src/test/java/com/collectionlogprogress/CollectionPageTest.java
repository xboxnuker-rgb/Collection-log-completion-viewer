package com.collectionlogprogress;

import java.util.Arrays;
import java.util.HashSet;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CollectionPageTest
{
    @Test
    public void countsOnlyObtainedItemsOnThePage()
    {
        CollectionPage page = new CollectionPage(new int[] {10, 20, 30});

        assertEquals(3, page.getTotal());
        assertEquals(
            2,
            page.countObtained(new HashSet<>(Arrays.asList(10, 30, 999)))
        );
    }

    @Test
    public void countsAnObtainedItemVariationForItsCollectionLogSlot()
    {
        CollectionPage page = new CollectionPage(new int[] {ItemID.WEARABLE_SAW});

        assertEquals(1, page.getTotal());
        assertEquals(
            1,
            page.countObtained(new HashSet<>(Arrays.asList(ItemID.WEARABLE_SAW_OFFHAND)))
        );
    }

    @Test
    public void doesNotTreatAnUnrelatedVariationAsACollectionLogSlot()
    {
        CollectionPage page = new CollectionPage(new int[] {ItemID.CONSTRUCTION_SUPPLY_CRATE});

        assertEquals(1, page.getTotal());
        assertEquals(
            0,
            page.countObtained(new HashSet<>(Arrays.asList(ItemID.WINT_REWARD_BOX)))
        );
    }
}
