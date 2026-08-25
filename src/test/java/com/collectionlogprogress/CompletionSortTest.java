package com.collectionlogprogress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompletionSortTest
{
    private static final int[] PERCENTAGES = {0, 100, 50, 50};
    private static final String[] PAGE_NAMES = {
        "Zulrah",
        "Brutus",
        "Callisto and Artio",
        "Barrows Chests"
    };

    @Test
    public void defaultsToHighestCompletionFirst()
    {
        List<Integer> indexes = allIndexes();

        CompletionSort.sort(indexes, PERCENTAGES, PAGE_NAMES, false);

        assertEquals(Arrays.asList(1, 3, 2, 0), indexes);
    }

    @Test
    public void reversesToLowestCompletionFirst()
    {
        List<Integer> indexes = allIndexes();

        CompletionSort.sort(indexes, PERCENTAGES, PAGE_NAMES, true);

        assertEquals(Arrays.asList(0, 3, 2, 1), indexes);
    }

    @Test
    public void keepsMatchingPercentagesAlphabeticalInEitherDirection()
    {
        List<Integer> descending = allIndexes();
        List<Integer> ascending = allIndexes();

        CompletionSort.sort(descending, PERCENTAGES, PAGE_NAMES, false);
        CompletionSort.sort(ascending, PERCENTAGES, PAGE_NAMES, true);

        assertEquals(Arrays.asList(3, 2), descending.subList(1, 3));
        assertEquals(Arrays.asList(3, 2), ascending.subList(1, 3));
    }

    @Test
    public void sortsOnlyIndexesLeftVisibleByFilters()
    {
        List<Integer> visibleIndexes = new ArrayList<>(Arrays.asList(0, 2, 3));

        CompletionSort.sort(visibleIndexes, PERCENTAGES, PAGE_NAMES, false);

        assertEquals(Arrays.asList(3, 2, 0), visibleIndexes);
    }

    private static List<Integer> allIndexes()
    {
        return new ArrayList<>(Arrays.asList(0, 1, 2, 3));
    }
}
