package com.collectionlogprogress;

import java.util.Arrays;
import java.util.HashSet;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;
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
    public void countsCanonicalBagIdsOnACompleteMotherlodeMinePage()
    {
        CollectionPage page = motherlodeMinePage();

        assertEquals(6, page.getTotal());
        assertEquals(
            6,
            page.countObtained(new HashSet<>(Arrays.asList(
                ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
                ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS,
                ItemVariationMapping.map(ItemID.COAL_BAG), ItemVariationMapping.map(ItemID.GEM_BAG))))
        );
    }

    @Test
    public void countsOpenBagVariantsOnACompleteMotherlodeMinePage()
    {
        CollectionPage page = motherlodeMinePage();

        assertEquals(
            6,
            page.countObtained(new HashSet<>(Arrays.asList(
                ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
                ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS,
                ItemID.COAL_BAG_OPEN, ItemID.GEM_BAG_OPEN)))
        );
    }

    @Test
    public void countsCollectionLogDummyBagsOnACompleteMotherlodeMinePage()
    {
        CollectionPage page = motherlodeMinePage();

        assertEquals(
            6,
            page.countObtained(new HashSet<>(Arrays.asList(
                ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
                ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS,
                ItemID.COAL_BAG_DUMMY, ItemID.GEM_BAG_DUMMY)))
        );
    }

    @Test
    public void stillCountsTheOriginalMotherlodeMineItemIds()
    {
        CollectionPage page = motherlodeMinePage();

        assertEquals(
            6,
            page.countObtained(new HashSet<>(Arrays.asList(
                ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
                ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS,
                ItemID.COAL_BAG, ItemID.GEM_BAG)))
        );
    }

    @Test
    public void doesNotCountMissingMotherlodeMineBags()
    {
        CollectionPage page = motherlodeMinePage();

        assertEquals(
            4,
            page.countObtained(new HashSet<>(Arrays.asList(
                ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
                ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS)))
        );
    }

    @Test
    public void countsEachBagSlotOnceWhenMultipleVariantsAreObtained()
    {
        CollectionPage page = new CollectionPage(new int[] {ItemID.COAL_BAG, ItemID.GEM_BAG});

        assertEquals(
            2,
            page.countObtained(new HashSet<>(Arrays.asList(
                ItemVariationMapping.map(ItemID.COAL_BAG), ItemID.COAL_BAG, ItemID.COAL_BAG_OPEN,
                ItemVariationMapping.map(ItemID.GEM_BAG), ItemID.GEM_BAG, ItemID.GEM_BAG_OPEN)))
        );
    }

    private static CollectionPage motherlodeMinePage()
    {
        return new CollectionPage(new int[] {
            ItemID.MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_TOP,
            ItemID.MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_BOOTS,
            ItemID.COAL_BAG, ItemID.GEM_BAG
        });
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
