package com.collectionlogprogress;

import java.util.Set;

final class CollectionPage
{
    private final int[] itemIds;

    CollectionPage(int[] itemIds)
    {
        this.itemIds = itemIds.clone();
    }

    int getTotal()
    {
        return itemIds.length;
    }

    int countObtained(Set<Integer> obtainedItems)
    {
        int count = 0;
        for (int itemId : itemIds)
        {
            if (obtainedItems.contains(itemId))
            {
                count++;
            }
        }
        return count;
    }
}
