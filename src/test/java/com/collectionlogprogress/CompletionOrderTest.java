package com.collectionlogprogress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompletionOrderTest
{
    @Test
    public void mapsTheExistingSidebarConfigurationToThreeStates()
    {
        assertEquals(CompletionOrder.DEFAULT, CompletionOrder.fromConfig(false, false));
        assertEquals(CompletionOrder.DEFAULT, CompletionOrder.fromConfig(false, true));
        assertEquals(CompletionOrder.DESCENDING, CompletionOrder.fromConfig(true, false));
        assertEquals(CompletionOrder.ASCENDING, CompletionOrder.fromConfig(true, true));
    }

    @Test
    public void cyclesDefaultDescendingAscendingAndBackToDefault()
    {
        CompletionOrder order = CompletionOrder.DEFAULT;
        order = order.next();
        assertEquals(CompletionOrder.DESCENDING, order);
        order = order.next();
        assertEquals(CompletionOrder.ASCENDING, order);
        order = order.next();
        assertEquals(CompletionOrder.DEFAULT, order);
    }

    @Test
    public void exposesTheConfigurationValuesForEachState()
    {
        assertFalse(CompletionOrder.DEFAULT.isSorted());
        assertFalse(CompletionOrder.DEFAULT.isReversed());
        assertTrue(CompletionOrder.DESCENDING.isSorted());
        assertFalse(CompletionOrder.DESCENDING.isReversed());
        assertTrue(CompletionOrder.ASCENDING.isSorted());
        assertTrue(CompletionOrder.ASCENDING.isReversed());
    }
}
