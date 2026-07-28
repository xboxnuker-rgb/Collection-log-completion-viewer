package com.collectionlogprogress;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProgressStateTest
{
    @Test
    public void classifiesUnstartedPartialAndCompletedPages()
    {
        assertEquals(ProgressState.UNSTARTED, ProgressState.from(0, 60));
        assertEquals(ProgressState.PARTIAL, ProgressState.from(1, 60));
        assertEquals(ProgressState.PARTIAL, ProgressState.from(59, 60));
        assertEquals(ProgressState.COMPLETED, ProgressState.from(60, 60));
    }

    @Test
    public void emptyPagesAreUnstarted()
    {
        assertEquals(ProgressState.UNSTARTED, ProgressState.from(0, 0));
    }
}
