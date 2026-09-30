package com.collectionlogprogress;

import java.util.Collection;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemVariationMapping;

final class CollectionPage
{
    private final int[][] itemIdsBySlot;

    CollectionPage(int[] itemIds)
    {
        itemIdsBySlot = new int[itemIds.length][];
        for (int itemIndex = 0; itemIndex < itemIds.length; itemIndex++)
        {
            int itemId = itemIds[itemIndex];
            // Normalize bag slots only: generic mappings can group unrelated reward crates.
            if (itemId == ItemID.COAL_BAG || itemId == ItemID.GEM_BAG)
            {
                itemId = ItemVariationMapping.map(itemId);
            }
            Collection<Integer> variations = ItemVariationMapping.getVariations(itemId);
            int[] slotItemIds = new int[variations.size()];
            int variationIndex = 0;
            for (int variation : variations)
            {
                slotItemIds[variationIndex++] = variation;
            }
            itemIdsBySlot[itemIndex] = slotItemIds;
        }
    }

    int getTotal()
    {
        return itemIdsBySlot.length;
    }

    int countObtained(Set<Integer> obtainedItems)
    {
        int count = 0;
        for (int[] slotItemIds : itemIdsBySlot)
        {
            for (int itemId : slotItemIds)
            {
                if (obtainedItems.contains(itemId))
                {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}
