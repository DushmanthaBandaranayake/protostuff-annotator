package com.dush.psannotator.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** The edits behind the actions, intention and quick fixes. Must be called inside a write command. */
public final class TagOperations
{
	private TagOperations()
	{
	}

	/** @return the number of fields annotated */
	public static int generate( @NotNull TagClass target, int start, boolean includeTransient,
			@Nullable Predicate<TagField> selected )
	{
		Map<TagField, Integer> plan = TagPlanner.planGenerate( target, start, includeTransient, selected );
		plan.forEach( target::addTag );
		return plan.size();
	}

	/** Tags every untagged serialized field after the current highest number. Used by the inspection's fix. */
	public static int tagRemaining( @NotNull TagClass target )
	{
		return generate( target, TagPlanner.nextAfterMax( target ), false, null );
	}

	/** @return the number of tags changed */
	public static int renumber( @NotNull TagClass target, int start )
	{
		Map<TagField, Integer> plan = TagPlanner.planRenumber( target, start );
		int changed = 0;
		for ( Map.Entry<TagField, Integer> entry : plan.entrySet() )
		{
			if ( !entry.getValue().equals( entry.getKey().getTagValue() ) )
			{
				target.setTag( entry.getKey(), entry.getValue() );
				changed++;
			}
		}
		return changed;
	}

	/** @return the number of tags removed */
	public static int removeAll( @NotNull TagClass target )
	{
		int removed = 0;
		for ( TagField field : target.getFields() )
		{
			if ( field.hasTag() )
			{
				target.removeTag( field );
				removed++;
			}
		}
		if ( removed > 0 )
		{
			target.removeUnusedImport();
		}
		return removed;
	}

	public static boolean hasAnyTag( @NotNull TagClass target )
	{
		for ( TagField field : target.getFields() )
		{
			if ( field.hasTag() )
			{
				return true;
			}
		}
		return false;
	}

	/** Superclasses before subclasses, so renumbering a subclass sees its parents' new numbers. */
	@NotNull
	public static List<TagClass> sortSuperclassesFirst( @NotNull List<TagClass> classes )
	{
		List<TagClass> sorted = new ArrayList<>( classes );
		sorted.sort( Comparator.comparingInt( c -> TagPlanner.hierarchyTopDown( c ).size() ) );
		return sorted;
	}
}
