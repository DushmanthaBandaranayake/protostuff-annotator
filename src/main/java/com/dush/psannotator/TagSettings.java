package com.dush.psannotator;

import com.intellij.ide.util.PropertiesComponent;

/** Dialog choices remembered between runs (IDE-wide). */
public final class TagSettings
{
	private static final String PREFIX = "com.dush.psannotator.";
	private static final String INCLUDE_TRANSIENT = PREFIX + "includeTransient";
	private static final String ONLY_TAGGED_CLASSES = PREFIX + "onlyTaggedClasses";
	private static final String RENUMBER_START = PREFIX + "renumberStart";

	private TagSettings()
	{
	}

	public static boolean isIncludeTransient()
	{
		return PropertiesComponent.getInstance().getBoolean( INCLUDE_TRANSIENT, false );
	}

	public static void setIncludeTransient( boolean value )
	{
		PropertiesComponent.getInstance().setValue( INCLUDE_TRANSIENT, value, false );
	}

	/** Bulk generate: only touch classes that already have at least one {@code @Tag}. */
	public static boolean isOnlyTaggedClasses()
	{
		return PropertiesComponent.getInstance().getBoolean( ONLY_TAGGED_CLASSES, true );
	}

	public static void setOnlyTaggedClasses( boolean value )
	{
		PropertiesComponent.getInstance().setValue( ONLY_TAGGED_CLASSES, value, true );
	}

	public static int getRenumberStart()
	{
		return PropertiesComponent.getInstance().getInt( RENUMBER_START, 1 );
	}

	public static void setRenumberStart( int value )
	{
		PropertiesComponent.getInstance().setValue( RENUMBER_START, value, 1 );
	}
}
