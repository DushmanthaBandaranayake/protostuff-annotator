package com.dush.psannotator.model;

/** Names and limits from Protostuff and the protobuf wire format. */
public final class TagNames
{
	public static final String TAG_FQN = "io.protostuff.Tag";
	public static final String EXCLUDE_FQN = "io.protostuff.Exclude";
	public static final String PACKAGE = "io.protostuff";
	public static final String TAG_SHORT = "Tag";
	public static final String EXCLUDE_SHORT = "Exclude";

	/** Largest field number allowed by the protobuf wire format, which Protostuff uses. */
	public static final int MAX_TAG = ( 1 << 29 ) - 1;

	private TagNames()
	{
	}

	public static boolean isValidTag( int value )
	{
		return value >= 1 && value <= MAX_TAG;
	}
}
