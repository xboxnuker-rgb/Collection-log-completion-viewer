package com.collectionlogprogress;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.FontID;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptEvent;
import net.runelite.api.ScriptID;
import net.runelite.api.StructComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Collection Log Progress",
    description = "Colour-coded progress, percentages and filters in Collection Log names",
    tags = {"collection", "log", "clog", "progress", "completion", "filter", "colour", "color"}
)
public class CollectionLogProgressPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(CollectionLogProgressPlugin.class);

    // These cache-backed collection scripts do not currently have public
    // ScriptID constants. COLLECTION_DELAYED_TRANSMIT supplies item id/quantity
    // after the player clicks the native Collection Log Search button.
    private static final int COLLECTION_DELAYED_TRANSMIT = 4100;
    private static final int COLLECTION_LOG_SETUP = 7797;

    private static final int COLLECTION_TAB_ENUM = 2102;
    private static final int TAB_PAGE_ENUM_PARAM = 683;
    private static final int PAGE_ITEM_ENUM_PARAM = 690;
    private static final int EMPTY_SNAPSHOT_WAIT_TICKS = 8;
    private static final int TRANSMIT_SETTLE_TICKS = 2;
    private static final int HIDDEN_ROW_Y = -10_000;
    private static final int FILTER_COUNT = 3;
    private static final int FILTER_UNSTARTED = 0;
    private static final int FILTER_PARTIAL = 1;
    private static final int FILTER_COMPLETED = 2;
    private static final int FILTER_BUTTON_HEIGHT = 22;
    private static final int FILTER_PROMPT_HEIGHT = 12;
    private static final int FILTER_PROMPT_GAP = 1;
    private static final int FILTER_BUTTON_IDEAL_WIDTH = 56;
    private static final int FILTER_BUTTON_MIN_WIDTH = 36;
    private static final int FILTER_BUTTON_GAP = 4;
    private static final int FILTER_SEARCH_GAP = 5;
    private static final int FILTER_CONTROLS_PREFERRED_X = 178;
    private static final int FILTER_TITLE_GAP = 8;
    private static final int ROW_OPACITY_EVEN = 235;
    private static final int ROW_OPACITY_ODD = 255;
    private static final int SELECTED_ROW_OPACITY = 200;
    private static final int BUTTON_FRAME_COLOR = 0x211A12;

    private static final int[] TITLE_WIDGETS = {
        InterfaceID.Collection.BOSS_TEXT,
        InterfaceID.Collection.RAID_TEXT,
        InterfaceID.Collection.CLUE_TEXT,
        InterfaceID.Collection.MINIGAME_TEXT,
        InterfaceID.Collection.OTHER_TEXT
    };

    private static final int[] TAB_WIDGETS = {
        InterfaceID.Collection.BOSS_TAB,
        InterfaceID.Collection.RAID_TAB,
        InterfaceID.Collection.CLUE_TAB,
        InterfaceID.Collection.MINIGAME_TAB,
        InterfaceID.Collection.OTHER_TAB
    };

    private static final int[] BACKGROUND_WIDGETS = {
        InterfaceID.Collection.BOSS_BACKGROUND,
        InterfaceID.Collection.RAID_BACKGROUND,
        InterfaceID.Collection.CLUE_BACKGROUND,
        InterfaceID.Collection.MINIGAME_BACKGROUND,
        InterfaceID.Collection.OTHER_BACKGROUND
    };

    private static final int[] CONTAINER_WIDGETS = {
        InterfaceID.Collection.BOSS_CONTAINER,
        InterfaceID.Collection.RAID_CONTAINER,
        InterfaceID.Collection.CLUE_CONTAINER,
        InterfaceID.Collection.MINIGAME_CONTAINER,
        InterfaceID.Collection.OTHER_CONTAINER
    };

    private static final int[] SCROLLBAR_WIDGETS = {
        InterfaceID.Collection.BOSS_SCROLLBAR,
        InterfaceID.Collection.RAID_SCROLLBAR,
        InterfaceID.Collection.CLUE_SCROLLBAR,
        InterfaceID.Collection.MINIGAME_SCROLLBAR,
        InterfaceID.Collection.OTHER_SCROLLBAR
    };

    private static final String[] TAB_NAMES = {
        "Bosses",
        "Raids",
        "Clues",
        "Minigames",
        "Other"
    };

    private static final String[] FILTER_NAMES = {
        "Unstarted",
        "Partial",
        "Completed"
    };

    private static final String[] FILTER_CONFIG_KEYS = {
        "hideUnstartedPages",
        "hidePartialPages",
        "hideCompletedPages"
    };

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private CollectionLogProgressConfig config;

    @Inject
    private ConfigManager configManager;

    private final Set<Integer> obtainedItems = new HashSet<>();
    private final Set<Integer> pendingObtainedItems = new HashSet<>();
    private final List<List<CollectionPage>> pagesByTab = new ArrayList<>();
    private final List<int[]> obtainedCountsByTab = new ArrayList<>();
    private final Map<Widget, WidgetState> originalWidgetStates = new IdentityHashMap<>();
    private final Map<Widget, ContainerState> originalContainerStates = new IdentityHashMap<>();
    private final FilterButton[] filterButtons = new FilterButton[FILTER_COUNT];
    private int[] tabObtainedCounts = new int[0];
    private int[] tabTotalCounts = new int[0];
    private Widget filterControlsLayer;
    private Widget searchPrompt;

    private boolean snapshotLoading;
    private boolean snapshotReady;
    private boolean sidebarApplyQueued;
    private int snapshotStartTick = -1;
    private int lastTransmitTick = -1;

    @Provides
    CollectionLogProgressConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CollectionLogProgressConfig.class);
    }

    @Override
    protected void startUp()
    {
        clientThread.invokeLater(() ->
        {
            loadPageDefinitions();
            if (client.getWidget(InterfaceID.Collection.FRAME) != null)
            {
                queueSidebarApply();
            }
        });
    }

    @Override
    protected void shutDown()
    {
        removeFilterControls();
        restoreSidebar();
        resetSnapshot();
        pagesByTab.clear();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() != InterfaceID.COLLECTION)
        {
            return;
        }

        forgetFilterControls();
        restoreSidebar();
        loadPageDefinitions();
        clientThread.invokeLater(() ->
        {
            if (isAnotherPlayersLog())
            {
                cancelSnapshotCapture();
            }
            else
            {
                queueSidebarApply();
            }
        });
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event.getParam1() != InterfaceID.Collection.SEARCH_TOGGLE
            || (event.getMenuAction() != MenuAction.CC_OP
                && event.getMenuAction() != MenuAction.RUNELITE))
        {
            return;
        }

        if (isAnotherPlayersLog())
        {
            cancelSnapshotCapture();
            return;
        }

        beginSnapshotCapture();
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event)
    {
        if (!snapshotLoading || event.getScriptId() != COLLECTION_DELAYED_TRANSMIT)
        {
            return;
        }
        if (isAnotherPlayersLog())
        {
            cancelSnapshotCapture();
            return;
        }

        ScriptEvent scriptEvent = event.getScriptEvent();
        if (scriptEvent == null)
        {
            return;
        }

        Object[] arguments = scriptEvent.getArguments();
        if (arguments == null || arguments.length < 3
            || !(arguments[1] instanceof Integer) || !(arguments[2] instanceof Integer))
        {
            return;
        }

        int itemId = (Integer) arguments[1];
        int quantity = (Integer) arguments[2];
        if (itemId >= 0 && quantity > 0)
        {
            pendingObtainedItems.add(itemId);
        }
        lastTransmitTick = client.getTickCount();
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() == COLLECTION_LOG_SETUP)
        {
            // WikiSync and similar integrations rebuild the shared UNIVERSE
            // children during setup. Recreate our controls after every plugin
            // has finished handling the setup event.
            queueSidebarApply();
            return;
        }

        if (event.getScriptId() == ScriptID.COLLECTION_DRAW_LIST)
        {
            queueSidebarApply();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!snapshotLoading)
        {
            return;
        }

        int tick = client.getTickCount();
        boolean transmissionsSettled = lastTransmitTick >= 0
            && lastTransmitTick + TRANSMIT_SETTLE_TICKS < tick;
        boolean emptySnapshotSettled = lastTransmitTick < 0
            && snapshotStartTick + EMPTY_SNAPSHOT_WAIT_TICKS < tick;

        if (transmissionsSettled || emptySnapshotSettled)
        {
            finishSnapshotCapture();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            removeFilterControls();
            restoreSidebar();
            resetSnapshot();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!CollectionLogProgressConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            restoreSidebar();
            applySidebarProgress();
        });
    }

    private void beginSnapshotCapture()
    {
        pendingObtainedItems.clear();
        snapshotLoading = true;
        snapshotStartTick = client.getTickCount();
        lastTransmitTick = -1;
        log.debug("Listening for the native Collection Log Search snapshot");
    }

    private void finishSnapshotCapture()
    {
        if (isAnotherPlayersLog())
        {
            cancelSnapshotCapture();
            return;
        }

        snapshotLoading = false;
        snapshotStartTick = -1;
        lastTransmitTick = -1;
        obtainedItems.clear();
        obtainedItems.addAll(pendingObtainedItems);
        pendingObtainedItems.clear();
        snapshotReady = true;
        rebuildProgressCache();
        log.debug("Collection Log snapshot ready with {} obtained item ids", obtainedItems.size());
        applySidebarProgress();
    }

    private void cancelSnapshotCapture()
    {
        snapshotLoading = false;
        snapshotStartTick = -1;
        lastTransmitTick = -1;
        pendingObtainedItems.clear();
    }

    private boolean isAnotherPlayersLog()
    {
        return client.getVarbitValue(VarbitID.COLLECTION_POH_HOST_BOOK_OPEN) == 1;
    }

    private void loadPageDefinitions()
    {
        if (!pagesByTab.isEmpty())
        {
            return;
        }

        EnumComposition tabEnum = client.getEnum(COLLECTION_TAB_ENUM);
        int[] tabStructIds = tabEnum == null ? null : tabEnum.getIntVals();
        if (tabStructIds == null || tabStructIds.length == 0)
        {
            log.debug("Collection Log tab definitions were unavailable");
            return;
        }

        for (int tabStructId : tabStructIds)
        {
            StructComposition tabStruct = client.getStructComposition(tabStructId);
            int pageEnumId = tabStruct.getIntValue(TAB_PAGE_ENUM_PARAM);
            EnumComposition pageEnum = client.getEnum(pageEnumId);
            int[] pageStructIds = pageEnum == null ? null : pageEnum.getIntVals();
            if (pageStructIds == null)
            {
                pagesByTab.add(Collections.emptyList());
                continue;
            }

            List<CollectionPage> pages = new ArrayList<>(pageStructIds.length);
            for (int pageStructId : pageStructIds)
            {
                StructComposition pageStruct = client.getStructComposition(pageStructId);
                int itemEnumId = pageStruct.getIntValue(PAGE_ITEM_ENUM_PARAM);
                EnumComposition itemEnum = client.getEnum(itemEnumId);
                int[] itemIds = itemEnum == null ? null : itemEnum.getIntVals();
                pages.add(new CollectionPage(itemIds == null ? new int[0] : itemIds));
            }
            pagesByTab.add(Collections.unmodifiableList(pages));
        }

        log.debug("Loaded Collection Log definitions for {} tabs", pagesByTab.size());
    }

    private void applySidebarProgress()
    {
        if (client.getWidget(InterfaceID.Collection.FRAME) == null)
        {
            return;
        }
        if (isAnotherPlayersLog())
        {
            removeFilterControls();
            restoreSidebar();
            return;
        }

        ensureFilterControls();
        if (!snapshotReady)
        {
            return;
        }
        if (pagesByTab.isEmpty())
        {
            loadPageDefinitions();
        }
        if (obtainedCountsByTab.size() != pagesByTab.size())
        {
            rebuildProgressCache();
        }

        int tabCount = Math.min(
            TITLE_WIDGETS.length,
            Math.min(pagesByTab.size(), obtainedCountsByTab.size())
        );
        for (int tabIndex = 0; tabIndex < tabCount; tabIndex++)
        {
            Widget titleLayer = client.getWidget(TITLE_WIDGETS[tabIndex]);
            if (titleLayer == null || titleLayer.isHidden())
            {
                continue;
            }

            Widget[] titleWidgets = titleLayer.getDynamicChildren();
            List<CollectionPage> pages = pagesByTab.get(tabIndex);
            int[] obtainedCounts = obtainedCountsByTab.get(tabIndex);
            int pageCount = Math.min(
                titleWidgets.length,
                Math.min(pages.size(), obtainedCounts.length)
            );
            if (titleWidgets.length != pages.size())
            {
                log.debug(
                    "Collection Log tab {} has {} title widgets but {} page definitions",
                    tabIndex,
                    titleWidgets.length,
                    pages.size()
                );
            }

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++)
            {
                Widget titleWidget = titleWidgets[pageIndex];
                CollectionPage page = pages.get(pageIndex);
                applyProgress(
                    titleWidget,
                    obtainedCounts[pageIndex],
                    page.getTotal(),
                    config.smoothColourScale()
                );
            }

            filterPageRows(tabIndex, titleWidgets, pages, obtainedCounts, pageCount);
        }

        applyTabProgress();
    }

    private void applyTabProgress()
    {
        int tabCount = Math.min(TAB_WIDGETS.length, pagesByTab.size());
        for (int tabIndex = 0; tabIndex < tabCount; tabIndex++)
        {
            Widget titleWidget = getTabTitleWidget(tabIndex);
            if (titleWidget == null)
            {
                log.debug("Could not find title text for Collection Log tab {}", TAB_NAMES[tabIndex]);
                continue;
            }

            if (tabIndex >= tabObtainedCounts.length || tabIndex >= tabTotalCounts.length)
            {
                continue;
            }
            applyProgress(
                titleWidget,
                tabObtainedCounts[tabIndex],
                tabTotalCounts[tabIndex],
                true
            );
        }
    }

    private void applyProgress(Widget titleWidget, int obtained, int total, boolean smooth)
    {
        WidgetState original = rememberWidget(titleWidget);
        int percentage = ProgressScale.percentage(obtained, total);

        String text = original.text;
        if (shouldShowPercentage(percentage))
        {
            text += " " + percentage + "%";
        }
        if (!text.equals(titleWidget.getText()))
        {
            titleWidget.setText(text);
        }

        int color = original.textColor;
        if (config.colourPageNames())
        {
            color = ProgressScale.color(
                obtained,
                total,
                smooth,
                config.unstartedColour().getRGB(),
                config.partialColour().getRGB(),
                config.completedColour().getRGB()
            );
        }
        if (color != titleWidget.getTextColor())
        {
            titleWidget.setTextColor(color);
        }
    }

    private void filterPageRows(
        int tabIndex,
        Widget[] textChildren,
        List<CollectionPage> pages,
        int[] obtainedCounts,
        int pageCount
    )
    {
        if (!hasActivePageFilter() || tabIndex >= BACKGROUND_WIDGETS.length)
        {
            return;
        }

        Widget backgroundLayer = client.getWidget(BACKGROUND_WIDGETS[tabIndex]);
        Widget container = client.getWidget(CONTAINER_WIDGETS[tabIndex]);
        if (backgroundLayer == null || container == null || backgroundLayer.isHidden())
        {
            return;
        }

        Widget[] backgroundChildren = backgroundLayer.getDynamicChildren();
        if (textChildren == null || backgroundChildren == null)
        {
            return;
        }
        int count = Math.min(pageCount, backgroundChildren.length);
        if (count == 0)
        {
            return;
        }

        originalContainerStates.computeIfAbsent(
            container,
            widget -> new ContainerState(tabIndex, widget.getScrollHeight(), widget.getScrollY())
        );

        int startY = Integer.MAX_VALUE;
        int strideY = 0;
        int previousY = Integer.MIN_VALUE;
        for (int pageIndex = 0; pageIndex < count; pageIndex++)
        {
            WidgetState backgroundState = rememberWidget(backgroundChildren[pageIndex]);
            int originalY = backgroundState.originalY;
            startY = Math.min(startY, originalY);
            if (previousY != Integer.MIN_VALUE && originalY > previousY && strideY == 0)
            {
                strideY = originalY - previousY;
            }
            previousY = originalY;
        }
        if (startY == Integer.MAX_VALUE)
        {
            startY = 0;
        }
        if (strideY <= 0)
        {
            strideY = Math.max(1, backgroundChildren[0].getOriginalHeight());
        }

        String selectedPageName = getCurrentPageName();
        int visibleSlot = 0;
        for (int pageIndex = 0; pageIndex < count; pageIndex++)
        {
            Widget textChild = textChildren[pageIndex];
            Widget backgroundChild = backgroundChildren[pageIndex];
            WidgetState textState = rememberWidget(textChild);
            rememberWidget(backgroundChild);

            int obtained = obtainedCounts[pageIndex];
            int total = pages.get(pageIndex).getTotal();
            if (shouldHidePage(obtained, total))
            {
                hideRowWidget(textChild);
                hideRowWidget(backgroundChild);
                continue;
            }

            int newY = startY + visibleSlot * strideY;
            showRowWidget(textChild, newY);
            showRowWidget(backgroundChild, newY);

            boolean selected = selectedPageName != null && selectedPageName.equals(textState.text);
            backgroundChild.setOpacity(
                selected
                    ? SELECTED_ROW_OPACITY
                    : (visibleSlot % 2 == 0 ? ROW_OPACITY_EVEN : ROW_OPACITY_ODD)
            );
            backgroundChild.revalidate();
            visibleSlot++;
        }

        int newScrollHeight = visibleSlot == 0
            ? 0
            : startY + (visibleSlot - 1) * strideY + backgroundChildren[0].getOriginalHeight();
        if (visibleSlot > 0 && newScrollHeight < container.getHeight() + strideY)
        {
            newScrollHeight = container.getHeight();
        }

        container.setScrollHeight(newScrollHeight);
        int maxScrollY = Math.max(0, newScrollHeight - container.getHeight());
        int scrollY = Math.min(container.getScrollY(), maxScrollY);
        container.setScrollY(scrollY);
        updateScrollbar(tabIndex, scrollY);
    }

    private boolean shouldHidePage(int obtained, int total)
    {
        switch (ProgressState.from(obtained, total))
        {
            case UNSTARTED:
                return config.hideUnstartedPages();
            case PARTIAL:
                return config.hidePartialPages();
            case COMPLETED:
                return config.hideCompletedPages();
            default:
                throw new IllegalStateException("Unknown progress state");
        }
    }

    private boolean hasActivePageFilter()
    {
        return config.hideUnstartedPages()
            || config.hidePartialPages()
            || config.hideCompletedPages();
    }

    private void hideRowWidget(Widget widget)
    {
        widget.setHidden(true);
        widget.setOriginalY(HIDDEN_ROW_Y);
        widget.revalidate();
    }

    private void showRowWidget(Widget widget, int y)
    {
        widget.setHidden(false);
        widget.setOriginalY(y);
        widget.revalidate();
    }

    private String getCurrentPageName()
    {
        Widget headerText = client.getWidget(InterfaceID.Collection.HEADER_TEXT);
        Widget pageName = findFirstNonEmptyText(
            headerText,
            Collections.newSetFromMap(new IdentityHashMap<>())
        );
        return pageName == null ? null : pageName.getText();
    }

    private Widget findFirstNonEmptyText(Widget widget, Set<Widget> visited)
    {
        if (widget == null || !visited.add(widget))
        {
            return null;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty())
        {
            return widget;
        }

        Widget result = findFirstNonEmptyTextInChildren(widget.getDynamicChildren(), visited);
        if (result != null)
        {
            return result;
        }
        result = findFirstNonEmptyTextInChildren(widget.getStaticChildren(), visited);
        if (result != null)
        {
            return result;
        }
        return findFirstNonEmptyTextInChildren(widget.getNestedChildren(), visited);
    }

    private Widget findFirstNonEmptyTextInChildren(Widget[] children, Set<Widget> visited)
    {
        if (children == null)
        {
            return null;
        }
        for (Widget child : children)
        {
            Widget result = findFirstNonEmptyText(child, visited);
            if (result != null)
            {
                return result;
            }
        }
        return null;
    }

    private void updateScrollbar(int tabIndex, int scrollY)
    {
        int scrollbarId = SCROLLBAR_WIDGETS[tabIndex];
        int containerId = CONTAINER_WIDGETS[tabIndex];
        clientThread.invokeAtTickEnd(() ->
            client.runScript(ScriptID.UPDATE_SCROLLBAR, scrollbarId, containerId, scrollY));
    }

    private void ensureFilterControls()
    {
        // UNIVERSE is the empty full-interface host used by Collection Log
        // integrations such as WikiSync. HEADER is the selected-page header,
        // so children attached there cannot occupy the top title strip.
        Widget universe = client.getWidget(InterfaceID.Collection.UNIVERSE);
        if (universe == null)
        {
            return;
        }

        if (filterControlsLayer == null
            || filterControlsLayer.getParent() != universe
            || !containsDynamicChild(universe, filterControlsLayer))
        {
            createFilterControls(universe);
        }

        layoutFilterControls(universe);
        for (int filterIndex = 0; filterIndex < FILTER_COUNT; filterIndex++)
        {
            updateFilterButton(filterIndex);
        }
        updateSearchPrompt();
    }

    private boolean containsDynamicChild(Widget parent, Widget expectedChild)
    {
        Widget[] children = parent.getDynamicChildren();
        if (children == null)
        {
            return false;
        }
        for (Widget child : children)
        {
            if (child == expectedChild)
            {
                return true;
            }
        }
        return false;
    }

    private void createFilterControls(Widget universe)
    {
        forgetFilterControls();
        filterControlsLayer = universe.createChild(WidgetType.LAYER);
        searchPrompt = filterControlsLayer.createChild(WidgetType.TEXT)
            .setText("Click Search to load")
            .setFontId(FontID.PLAIN_11)
            .setTextShadowed(true)
            .setXTextAlignment(WidgetTextAlignment.CENTER)
            .setYTextAlignment(WidgetTextAlignment.CENTER);

        for (int filterIndex = 0; filterIndex < FILTER_COUNT; filterIndex++)
        {
            Widget root = filterControlsLayer.createChild(WidgetType.LAYER);
            root.setNoClickThrough(true);
            Widget frame = root.createChild(WidgetType.RECTANGLE).setFilled(true);
            Widget fill = root.createChild(WidgetType.RECTANGLE).setFilled(true);
            Widget highlight = root.createChild(WidgetType.RECTANGLE).setFilled(true);
            Widget shadow = root.createChild(WidgetType.RECTANGLE).setFilled(true);
            Widget checkbox = root.createChild(WidgetType.RECTANGLE).setFilled(true);
            Widget checkboxMark = root.createChild(WidgetType.TEXT)
                .setText("X")
                .setFontId(FontID.PLAIN_11)
                .setTextColor(0xFFFFFF)
                .setTextShadowed(true)
                .setXTextAlignment(WidgetTextAlignment.CENTER)
                .setYTextAlignment(WidgetTextAlignment.CENTER);
            Widget label = root.createChild(WidgetType.TEXT)
                .setText("Hide")
                .setFontId(FontID.PLAIN_11)
                .setXTextAlignment(WidgetTextAlignment.CENTER)
                .setYTextAlignment(WidgetTextAlignment.CENTER);

            final int clickedFilter = filterIndex;
            root.setHasListener(true);
            root.setOnOpListener((JavaScriptCallback) event -> togglePageFilter(clickedFilter));
            root.setOnMouseOverListener((JavaScriptCallback) event ->
                frame.setTextColor(0xFFFFFF));
            root.setOnMouseLeaveListener((JavaScriptCallback) event ->
                frame.setTextColor(BUTTON_FRAME_COLOR));

            filterButtons[filterIndex] = new FilterButton(
                root,
                frame,
                fill,
                highlight,
                shadow,
                checkbox,
                checkboxMark,
                label
            );
        }
    }

    private void layoutFilterControls(Widget universe)
    {
        Widget searchButton = client.getWidget(InterfaceID.Collection.SEARCH_TOGGLE);

        int controlsX = FILTER_CONTROLS_PREFERRED_X;
        int titleLeft = universe.getWidth() / 2 - 100;
        int y = 7;
        if (searchButton != null)
        {
            controlsX = searchButton.getOriginalX()
                + searchButton.getOriginalWidth() + FILTER_SEARCH_GAP;
            y = Math.max(
                1,
                searchButton.getOriginalY()
                    + (searchButton.getOriginalHeight() - FILTER_BUTTON_HEIGHT) / 2
            );
        }

        int availableWidth = titleLeft - FILTER_TITLE_GAP - controlsX;
        int buttonWidth = Math.min(
            FILTER_BUTTON_IDEAL_WIDTH,
            (availableWidth - (FILTER_COUNT - 1) * FILTER_BUTTON_GAP) / FILTER_COUNT
        );
        if (buttonWidth < FILTER_BUTTON_MIN_WIDTH)
        {
            filterControlsLayer.setHidden(true);
            return;
        }

        int totalWidth = FILTER_COUNT * buttonWidth + (FILTER_COUNT - 1) * FILTER_BUTTON_GAP;
        int totalHeight = FILTER_BUTTON_HEIGHT;
        if (!snapshotReady)
        {
            totalHeight += FILTER_PROMPT_GAP + FILTER_PROMPT_HEIGHT;
        }
        filterControlsLayer
            .setOriginalX(controlsX)
            .setOriginalY(y)
            .setOriginalWidth(totalWidth)
            .setOriginalHeight(totalHeight)
            .setHidden(false)
            .revalidate();

        for (int filterIndex = 0; filterIndex < FILTER_COUNT; filterIndex++)
        {
            FilterButton button = filterButtons[filterIndex];
            int x = filterIndex * (buttonWidth + FILTER_BUTTON_GAP);
            button.root
                .setOriginalX(x)
                .setOriginalY(0)
                .setOriginalWidth(buttonWidth)
                .setOriginalHeight(FILTER_BUTTON_HEIGHT)
                .revalidate();

            setBounds(button.frame, 0, 0, buttonWidth, FILTER_BUTTON_HEIGHT);
            setBounds(button.fill, 1, 1, buttonWidth - 2, FILTER_BUTTON_HEIGHT - 2);
            setBounds(button.highlight, 2, 2, buttonWidth - 4, 1);
            setBounds(
                button.shadow,
                2,
                FILTER_BUTTON_HEIGHT - 3,
                buttonWidth - 4,
                1
            );
            setBounds(button.checkbox, 4, 7, 9, 9);
            setBounds(button.checkboxMark, 4, 4, 9, 14);
            setBounds(button.label, 14, 1, buttonWidth - 15, FILTER_BUTTON_HEIGHT - 2);
        }

        setBounds(
            searchPrompt,
            0,
            FILTER_BUTTON_HEIGHT + FILTER_PROMPT_GAP,
            totalWidth,
            FILTER_PROMPT_HEIGHT
        );
    }

    private void updateSearchPrompt()
    {
        if (searchPrompt == null)
        {
            return;
        }

        searchPrompt
            .setTextColor(config.partialColour().getRGB() & 0xFFFFFF)
            .setHidden(snapshotReady);
    }

    private void setBounds(Widget widget, int x, int y, int width, int height)
    {
        widget
            .setOriginalX(x)
            .setOriginalY(y)
            .setOriginalWidth(width)
            .setOriginalHeight(height)
            .revalidate();
    }

    private void updateFilterButton(int filterIndex)
    {
        FilterButton button = filterButtons[filterIndex];
        if (button == null)
        {
            return;
        }

        int color = getFilterColor(filterIndex);
        int textColor = contrastTextColor(color);
        boolean enabled = isPageFilterEnabled(filterIndex);

        button.frame.setTextColor(BUTTON_FRAME_COLOR);
        button.fill.setTextColor(color);
        button.highlight.setTextColor(blend(color, 0xFFFFFF, 0.30));
        button.shadow.setTextColor(blend(color, 0x000000, 0.40));
        button.checkbox.setTextColor(BUTTON_FRAME_COLOR);
        button.checkboxMark.setTextColor(0xFFFFFF).setHidden(!enabled);
        button.label
            .setTextColor(textColor)
            .setTextShadowed(textColor == 0xFFFFFF);

        String action = enabled ? "Show" : "Hide";
        button.root.setAction(0, action + " " + FILTER_NAMES[filterIndex].toLowerCase() + " pages");
        button.root.setName(
            "<col=" + String.format("%06x", color) + ">" + FILTER_NAMES[filterIndex] + "</col>"
        );
    }

    private int getFilterColor(int filterIndex)
    {
        switch (filterIndex)
        {
            case FILTER_UNSTARTED:
                return config.unstartedColour().getRGB() & 0xFFFFFF;
            case FILTER_PARTIAL:
                return config.partialColour().getRGB() & 0xFFFFFF;
            case FILTER_COMPLETED:
                return config.completedColour().getRGB() & 0xFFFFFF;
            default:
                throw new IllegalArgumentException("Unknown page filter " + filterIndex);
        }
    }

    private boolean isPageFilterEnabled(int filterIndex)
    {
        switch (filterIndex)
        {
            case FILTER_UNSTARTED:
                return config.hideUnstartedPages();
            case FILTER_PARTIAL:
                return config.hidePartialPages();
            case FILTER_COMPLETED:
                return config.hideCompletedPages();
            default:
                throw new IllegalArgumentException("Unknown page filter " + filterIndex);
        }
    }

    private void togglePageFilter(int filterIndex)
    {
        configManager.setConfiguration(
            CollectionLogProgressConfig.GROUP,
            FILTER_CONFIG_KEYS[filterIndex],
            !isPageFilterEnabled(filterIndex)
        );
    }

    private int contrastTextColor(int color)
    {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int luminance = red * 299 + green * 587 + blue * 114;
        return luminance >= 145_000 ? 0x000000 : 0xFFFFFF;
    }

    private int blend(int from, int to, double amount)
    {
        int red = blendChannel(from >> 16, to >> 16, amount);
        int green = blendChannel(from >> 8, to >> 8, amount);
        int blue = blendChannel(from, to, amount);
        return (red << 16) | (green << 8) | blue;
    }

    private int blendChannel(int from, int to, double amount)
    {
        return (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
    }

    private WidgetState rememberWidget(Widget widget)
    {
        return originalWidgetStates.computeIfAbsent(
            widget,
            current -> new WidgetState(
                current.getText(),
                current.getTextColor(),
                current.getOriginalY(),
                current.isHidden(),
                current.getOpacity()
            )
        );
    }

    private void removeFilterControls()
    {
        if (filterControlsLayer != null)
        {
            for (FilterButton button : filterButtons)
            {
                if (button == null)
                {
                    continue;
                }
                button.root.setHidden(true);
                button.frame.setHidden(true);
                button.fill.setHidden(true);
                button.highlight.setHidden(true);
                button.shadow.setHidden(true);
                button.checkbox.setHidden(true);
                button.checkboxMark.setHidden(true);
                button.label.setHidden(true);
            }
            if (searchPrompt != null)
            {
                searchPrompt.setHidden(true);
            }
            filterControlsLayer.deleteAllChildren();
            filterControlsLayer
                .setOriginalWidth(0)
                .setOriginalHeight(0)
                .setHidden(true)
                .revalidate();
        }
        forgetFilterControls();
    }

    private void forgetFilterControls()
    {
        filterControlsLayer = null;
        searchPrompt = null;
        for (int filterIndex = 0; filterIndex < FILTER_COUNT; filterIndex++)
        {
            filterButtons[filterIndex] = null;
        }
    }

    private boolean shouldShowPercentage(int percentage)
    {
        return config.showPercentages()
            && (percentage != 0 || config.showZeroPercentages())
            && (percentage != 100 || config.showCompletePercentages());
    }

    private Widget getTabTitleWidget(int tabIndex)
    {
        Widget tabRoot = client.getWidget(TAB_WIDGETS[tabIndex]);
        return findTextWidget(
            tabRoot,
            TAB_NAMES[tabIndex],
            Collections.newSetFromMap(new IdentityHashMap<>())
        );
    }

    private Widget findTextWidget(Widget widget, String expectedText, Set<Widget> visited)
    {
        if (widget == null || !visited.add(widget))
        {
            return null;
        }

        String text = widget.getText();
        if (expectedText.equals(text) || text.startsWith(expectedText + " "))
        {
            return widget;
        }

        Widget title = findTextWidgetInChildren(widget.getDynamicChildren(), expectedText, visited);
        if (title != null)
        {
            return title;
        }
        title = findTextWidgetInChildren(widget.getStaticChildren(), expectedText, visited);
        if (title != null)
        {
            return title;
        }
        return findTextWidgetInChildren(widget.getNestedChildren(), expectedText, visited);
    }

    private Widget findTextWidgetInChildren(
        Widget[] children,
        String expectedText,
        Set<Widget> visited
    )
    {
        if (children == null)
        {
            return null;
        }

        for (Widget child : children)
        {
            Widget title = findTextWidget(child, expectedText, visited);
            if (title != null)
            {
                return title;
            }
        }
        return null;
    }

    private void restoreSidebar()
    {
        for (Map.Entry<Widget, WidgetState> entry : originalWidgetStates.entrySet())
        {
            Widget widget = entry.getKey();
            WidgetState state = entry.getValue();
            widget.setText(state.text);
            widget.setTextColor(state.textColor);
            widget.setOriginalY(state.originalY);
            widget.setHidden(state.hidden);
            widget.setOpacity(state.opacity);
            widget.revalidate();
        }
        originalWidgetStates.clear();

        for (Map.Entry<Widget, ContainerState> entry : originalContainerStates.entrySet())
        {
            Widget container = entry.getKey();
            ContainerState state = entry.getValue();
            container.setScrollHeight(state.scrollHeight);
            container.setScrollY(state.scrollY);
            container.revalidate();
            updateScrollbar(state.tabIndex, state.scrollY);
        }
        originalContainerStates.clear();
    }

    private void queueSidebarApply()
    {
        if (sidebarApplyQueued)
        {
            return;
        }

        sidebarApplyQueued = true;
        // COLLECTION_DRAW_LIST can be nested inside a wider interface redraw.
        // Apply after that script stack returns so later tab setup cannot
        // overwrite the five aggregate titles.
        clientThread.invokeLater(() ->
        {
            sidebarApplyQueued = false;
            applySidebarProgress();
        });
    }

    private void rebuildProgressCache()
    {
        obtainedCountsByTab.clear();
        tabObtainedCounts = new int[pagesByTab.size()];
        tabTotalCounts = new int[pagesByTab.size()];

        for (int tabIndex = 0; tabIndex < pagesByTab.size(); tabIndex++)
        {
            List<CollectionPage> pages = pagesByTab.get(tabIndex);
            int[] obtainedCounts = new int[pages.size()];
            for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++)
            {
                CollectionPage page = pages.get(pageIndex);
                int obtained = page.countObtained(obtainedItems);
                obtainedCounts[pageIndex] = obtained;
                tabObtainedCounts[tabIndex] += obtained;
                tabTotalCounts[tabIndex] += page.getTotal();
            }
            obtainedCountsByTab.add(obtainedCounts);
        }
    }

    private void resetSnapshot()
    {
        cancelSnapshotCapture();
        snapshotReady = false;
        obtainedItems.clear();
        obtainedCountsByTab.clear();
        tabObtainedCounts = new int[0];
        tabTotalCounts = new int[0];
    }

    private static final class WidgetState
    {
        private final String text;
        private final int textColor;
        private final int originalY;
        private final boolean hidden;
        private final int opacity;

        private WidgetState(
            String text,
            int textColor,
            int originalY,
            boolean hidden,
            int opacity
        )
        {
            this.text = text;
            this.textColor = textColor;
            this.originalY = originalY;
            this.hidden = hidden;
            this.opacity = opacity;
        }
    }

    private static final class ContainerState
    {
        private final int tabIndex;
        private final int scrollHeight;
        private final int scrollY;

        private ContainerState(int tabIndex, int scrollHeight, int scrollY)
        {
            this.tabIndex = tabIndex;
            this.scrollHeight = scrollHeight;
            this.scrollY = scrollY;
        }
    }

    private static final class FilterButton
    {
        private final Widget root;
        private final Widget frame;
        private final Widget fill;
        private final Widget highlight;
        private final Widget shadow;
        private final Widget checkbox;
        private final Widget checkboxMark;
        private final Widget label;

        private FilterButton(
            Widget root,
            Widget frame,
            Widget fill,
            Widget highlight,
            Widget shadow,
            Widget checkbox,
            Widget checkboxMark,
            Widget label
        )
        {
            this.root = root;
            this.frame = frame;
            this.fill = fill;
            this.highlight = highlight;
            this.shadow = shadow;
            this.checkbox = checkbox;
            this.checkboxMark = checkboxMark;
            this.label = label;
        }
    }
}
