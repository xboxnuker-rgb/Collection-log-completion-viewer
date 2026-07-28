package com.collectionlogprogress;

import java.util.Arrays;
import java.util.HashSet;
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
}
