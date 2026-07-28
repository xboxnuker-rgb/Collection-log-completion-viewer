package com.collectionlogprogress;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(CollectionLogProgressConfig.GROUP)
public interface CollectionLogProgressConfig extends Config
{
    String GROUP = "collection-log-progress";

    @ConfigSection(
        name = "Sidebar",
        description = "Collection Log page-list appearance",
        position = 0
    )
    String sidebarSection = "sidebar";

    @ConfigSection(
        name = "Colours",
        description = "Progress colour anchors",
        position = 1
    )
    String coloursSection = "colours";

    @ConfigItem(
        position = 0,
        keyName = "colourPageNames",
        name = "Colour names",
        description = "Use red for unstarted, orange for partial and green for completed names",
        section = sidebarSection
    )
    default boolean colourPageNames()
    {
        return true;
    }

    @ConfigItem(
        position = 1,
        keyName = "smoothColourScale",
        name = "Smooth colour scale",
        description = "Gradually blend page names red through orange to green; tab titles are always scaled",
        section = sidebarSection
    )
    default boolean smoothColourScale()
    {
        return false;
    }

    @ConfigItem(
        position = 2,
        keyName = "showPercentages",
        name = "Show percentages",
        description = "Append whole-number completion percentages to Collection Log page and tab names",
        section = sidebarSection
    )
    default boolean showPercentages()
    {
        return true;
    }

    @ConfigItem(
        position = 3,
        keyName = "showZeroPercentages",
        name = "Show 0%",
        description = "Show the percentage number on unstarted page and tab names",
        section = sidebarSection
    )
    default boolean showZeroPercentages()
    {
        return true;
    }

    @ConfigItem(
        position = 4,
        keyName = "showCompletePercentages",
        name = "Show 100%",
        description = "Show the percentage number on completed page and tab names",
        section = sidebarSection
    )
    default boolean showCompletePercentages()
    {
        return true;
    }

    @ConfigItem(
        keyName = "hideUnstartedPages",
        name = "Hide unstarted pages",
        description = "In-log filter state for pages with no obtained items",
        hidden = true
    )
    default boolean hideUnstartedPages()
    {
        return false;
    }

    @ConfigItem(
        keyName = "hidePartialPages",
        name = "Hide partial pages",
        description = "In-log filter state for partially completed pages",
        hidden = true
    )
    default boolean hidePartialPages()
    {
        return false;
    }

    @ConfigItem(
        keyName = "hideCompletedPages",
        name = "Hide completed pages",
        description = "In-log filter state for completed pages",
        hidden = true
    )
    default boolean hideCompletedPages()
    {
        return false;
    }

    @ConfigItem(
        position = 0,
        keyName = "unstartedColour",
        name = "Unstarted (0%)",
        description = "Colour used when no items have been obtained",
        section = coloursSection
    )
    default Color unstartedColour()
    {
        return new Color(255, 48, 48);
    }

    @ConfigItem(
        position = 1,
        keyName = "partialColour",
        name = "Partial (1–99%)",
        description = "Colour used for partial progress and the middle of the smooth scale",
        section = coloursSection
    )
    default Color partialColour()
    {
        return new Color(255, 152, 31);
    }

    @ConfigItem(
        position = 2,
        keyName = "completedColour",
        name = "Completed (100%)",
        description = "Colour used when every item has been obtained",
        section = coloursSection
    )
    default Color completedColour()
    {
        return new Color(13, 193, 13);
    }
}
