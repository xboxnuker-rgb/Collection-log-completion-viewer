package com.collectionlogprogress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ProgressScaleTest
{
    @Test
    public void zeroProgressUsesFixedRed()
    {
        assertEquals(ProgressScale.START_TEXT_COLOR, ProgressScale.color(0, 60, false));
    }

    @Test
    public void allPartialProgressUsesExactOrange()
    {
        assertEquals(ProgressScale.MIDPOINT_TEXT_COLOR, ProgressScale.color(1, 60, false));
        assertEquals(ProgressScale.MIDPOINT_TEXT_COLOR, ProgressScale.color(30, 60, false));
        assertEquals(ProgressScale.MIDPOINT_TEXT_COLOR, ProgressScale.color(59, 60, false));
    }

    @Test
    public void completeProgressUsesExactCompletionGreen()
    {
        assertEquals(
            ProgressScale.COMPLETED_TEXT_COLOR,
            ProgressScale.color(3, 3, false)
        );
    }

    @Test
    public void firstLargePageSlotUsesPartialColor()
    {
        assertEquals(
            ProgressScale.MIDPOINT_TEXT_COLOR,
            ProgressScale.color(1, 60, false)
        );
    }

    @Test
    public void smallPagesUseThreeDiscreteStates()
    {
        int zero = ProgressScale.color(0, 3, false);
        int one = ProgressScale.color(1, 3, false);
        int two = ProgressScale.color(2, 3, false);
        int three = ProgressScale.color(3, 3, false);

        assertNotEquals(zero, one);
        assertEquals(one, two);
        assertNotEquals(two, three);
    }

    @Test
    public void optionalSmoothScaleHasFixedAnchorsAndDistinctPartialStages()
    {
        int zero = ProgressScale.color(0, 60, true);
        int early = ProgressScale.color(1, 60, true);
        int half = ProgressScale.color(30, 60, true);
        int late = ProgressScale.color(59, 60, true);
        int complete = ProgressScale.color(60, 60, true);

        assertEquals(ProgressScale.START_TEXT_COLOR, zero);
        assertNotEquals(zero, early);
        assertEquals(ProgressScale.MIDPOINT_TEXT_COLOR, half);
        assertNotEquals(half, late);
        assertEquals(ProgressScale.COMPLETED_TEXT_COLOR, complete);
    }

    @Test
    public void configuredColoursReplaceAllThreeAnchors()
    {
        int start = 0x112233;
        int partial = 0x445566;
        int complete = 0x778899;

        assertEquals(start, ProgressScale.color(0, 10, false, start, partial, complete));
        assertEquals(partial, ProgressScale.color(5, 10, false, start, partial, complete));
        assertEquals(complete, ProgressScale.color(10, 10, false, start, partial, complete));
        assertEquals(partial, ProgressScale.color(5, 10, true, start, partial, complete));
    }

    @Test
    public void incompleteSmoothProgressDoesNotRoundToCompletionColour()
    {
        assertNotEquals(
            ProgressScale.COMPLETED_TEXT_COLOR,
            ProgressScale.color(999_999, 1_000_000, true)
        );
    }

    @Test
    public void incompleteRoundedPercentageNeverShowsOneHundred()
    {
        assertEquals(99, ProgressScale.percentage(199, 200));
        assertEquals(100, ProgressScale.percentage(200, 200));
    }
}
