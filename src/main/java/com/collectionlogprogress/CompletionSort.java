package com.collectionlogprogress;

import java.util.List;

final class CompletionSort
{
    private CompletionSort()
    {
    }

    static void sort(
        List<Integer> pageIndexes,
        int[] percentages,
        String[] pageNames,
        boolean reverse
    )
    {
        pageIndexes.sort((leftIndex, rightIndex) -> compare(
            percentages[leftIndex],
            pageNames[leftIndex],
            percentages[rightIndex],
            pageNames[rightIndex],
            reverse
        ));
    }

    private static int compare(
        int leftPercentage,
        String leftName,
        int rightPercentage,
        String rightName,
        boolean reverse
    )
    {
        int percentageComparison = reverse
            ? Integer.compare(leftPercentage, rightPercentage)
            : Integer.compare(rightPercentage, leftPercentage);
        if (percentageComparison != 0)
        {
            return percentageComparison;
        }

        String safeLeftName = leftName == null ? "" : leftName;
        String safeRightName = rightName == null ? "" : rightName;
        int nameComparison = String.CASE_INSENSITIVE_ORDER.compare(safeLeftName, safeRightName);
        return nameComparison != 0
            ? nameComparison
            : safeLeftName.compareTo(safeRightName);
    }
}
