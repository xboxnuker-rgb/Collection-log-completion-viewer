package com.collectionlogprogress;

enum CompletionOrder
{
    DEFAULT(false, false),
    DESCENDING(true, false),
    ASCENDING(true, true);

    private final boolean sorted;
    private final boolean reversed;

    CompletionOrder(boolean sorted, boolean reversed)
    {
        this.sorted = sorted;
        this.reversed = reversed;
    }

    static CompletionOrder fromConfig(boolean sorted, boolean reversed)
    {
        if (!sorted)
        {
            return DEFAULT;
        }
        return reversed ? ASCENDING : DESCENDING;
    }

    CompletionOrder next()
    {
        switch (this)
        {
            case DEFAULT:
                return DESCENDING;
            case DESCENDING:
                return ASCENDING;
            case ASCENDING:
            default:
                return DEFAULT;
        }
    }

    boolean isSorted()
    {
        return sorted;
    }

    boolean isReversed()
    {
        return reversed;
    }
}
