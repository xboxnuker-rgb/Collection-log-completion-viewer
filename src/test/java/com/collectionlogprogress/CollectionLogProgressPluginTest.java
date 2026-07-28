package com.collectionlogprogress;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CollectionLogProgressPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(CollectionLogProgressPlugin.class);
        RuneLite.main(args);
    }
}
