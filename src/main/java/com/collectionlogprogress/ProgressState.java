package com.collectionlogprogress;

enum ProgressState
{
    UNSTARTED,
    PARTIAL,
    COMPLETED;

    static ProgressState from(int obtained, int total)
    {
        if (total > 0 && obtained >= total)
        {
            return COMPLETED;
        }
        if (obtained <= 0)
        {
            return UNSTARTED;
        }
        return PARTIAL;
    }
}
