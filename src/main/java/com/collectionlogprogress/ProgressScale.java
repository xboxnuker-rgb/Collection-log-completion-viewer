package com.collectionlogprogress;

final class ProgressScale
{
    static final int START_TEXT_COLOR = 0xFF3030;
    static final int MIDPOINT_TEXT_COLOR = 0xFF981F;
    static final int COMPLETED_TEXT_COLOR = 0x0DC10D;
    private static final double EARLY_PROGRESS_EXPONENT = 0.65;

    private ProgressScale()
    {
    }

    static int percentage(int obtained, int total)
    {
        if (total <= 0 || obtained <= 0)
        {
            return 0;
        }
        if (obtained >= total)
        {
            return 100;
        }

        int rounded = (int) Math.round(obtained * 100.0 / total);
        return Math.max(1, Math.min(99, rounded));
    }

    static int color(int obtained, int total, boolean smooth)
    {
        return color(
            obtained,
            total,
            smooth,
            START_TEXT_COLOR,
            MIDPOINT_TEXT_COLOR,
            COMPLETED_TEXT_COLOR
        );
    }

    static int color(
        int obtained,
        int total,
        boolean smooth,
        int startColor,
        int partialColor,
        int completedColor
    )
    {
        if (total <= 0 || obtained <= 0)
        {
            return startColor & 0xFFFFFF;
        }
        if (obtained >= total)
        {
            return completedColor & 0xFFFFFF;
        }
        if (!smooth)
        {
            return partialColor & 0xFFFFFF;
        }

        double progress = obtained / (double) total;
        if (progress <= 0.5)
        {
            double local = Math.pow(progress / 0.5, EARLY_PROGRESS_EXPONENT);
            return interpolate(startColor, partialColor, local);
        }

        double local = Math.pow((progress - 0.5) / 0.5, EARLY_PROGRESS_EXPONENT);
        return keepDistinctFromCompletion(
            interpolate(partialColor, completedColor, local),
            partialColor,
            completedColor
        );
    }

    private static int interpolate(int from, int to, double amount)
    {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        int red = interpolateChannel(from >> 16, to >> 16, clamped);
        int green = interpolateChannel(from >> 8, to >> 8, clamped);
        int blue = interpolateChannel(from, to, clamped);
        return (red << 16) | (green << 8) | blue;
    }

    private static int interpolateChannel(int from, int to, double amount)
    {
        return (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
    }

    private static int keepDistinctFromCompletion(int color, int partialColor, int completedColor)
    {
        int partial = partialColor & 0xFFFFFF;
        int completed = completedColor & 0xFFFFFF;
        if (color != completed || partial == completed)
        {
            return color;
        }

        int[] shifts = {16, 8, 0};
        for (int shift : shifts)
        {
            int partialChannel = (partial >> shift) & 0xFF;
            int completedChannel = (completed >> shift) & 0xFF;
            if (partialChannel != completedChannel)
            {
                int adjusted = completedChannel + Integer.signum(partialChannel - completedChannel);
                return (completed & ~(0xFF << shift)) | (adjusted << shift);
            }
        }
        return color;
    }
}
