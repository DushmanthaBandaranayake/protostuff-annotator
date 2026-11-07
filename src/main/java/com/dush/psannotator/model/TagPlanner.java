package com.dush.psannotator.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Works out tag numbers without changing anything. The hierarchy is walked from the top-most superclass down, in
 * declaration order, so a subclass never reuses a number that belongs to one of its superclasses:
 * <ul>
 *     <li>numbers already used by an existing {@code @Tag} anywhere in the hierarchy are never handed out again;</li>
 *     <li>an untagged inherited field reserves the next free number (it isn't changed);</li>
 *     <li>static and {@code @Exclude} fields are always skipped, transient fields unless asked for.</li>
 * </ul>
 */
public final class TagPlanner
{
	private TagPlanner()
	{
	}

	/** The class and its superclasses, top-most first. {@code java.lang.Object} is left out. */
	@NotNull
	public static List<TagClass> hierarchyTopDown( @NotNull TagClass target )
	{
		Deque<TagClass> chain = new ArrayDeque<>();
		Set<Object> seen = new HashSet<>();
		for ( TagClass cls = target; cls != null && seen.add( cls.getDeclaration() ); cls = cls.getSuperClass() )
		{
			chain.addFirst( cls );
		}
		return new ArrayList<>( chain );
	}

	/** Fields that {@code Generate} may annotate: not static, not excluded, not transient unless included. */
	public static boolean isTaggable( @NotNull TagField field, boolean includeTransient )
	{
		return !field.isStatic() && !field.isExcluded() && ( includeTransient || !field.isTransient() );
	}

	/** Untagged fields of {@code target} that {@code Generate} would offer, in declaration order. */
	@NotNull
	public static List<TagField> untaggedFields( @NotNull TagClass target, boolean includeTransient )
	{
		List<TagField> result = new ArrayList<>();
		for ( TagField field : target.getFields() )
		{
			if ( isTaggable( field, includeTransient ) && !field.hasTag() )
			{
				result.add( field );
			}
		}
		return result;
	}

	/** Every tag value used in the hierarchy. */
	@NotNull
	public static Set<Integer> usedTags( @NotNull List<TagClass> hierarchy )
	{
		Set<Integer> used = new HashSet<>();
		for ( TagClass cls : hierarchy )
		{
			for ( TagField field : cls.getFields() )
			{
				Integer value = field.getTagValue();
				if ( value != null )
				{
					used.add( value );
				}
			}
		}
		return used;
	}

	/**
	 * One more than the largest valid tag in the hierarchy, or 1 if there are none. Never fills gaps. Out-of-range
	 * values are ignored, so fixing {@code @Tag(999999999)} doesn't push the next number to the limit.
	 */
	public static int nextAfterMax( @NotNull TagClass target )
	{
		Set<Integer> used = usedTags( hierarchyTopDown( target ) );
		int max = 0;
		for ( int value : used )
		{
			if ( TagNames.isValidTag( value ) )
			{
				max = Math.max( max, value );
			}
		}
		if ( max < TagNames.MAX_TAG )
		{
			return max + 1;
		}
		// Highest number taken: fall back to the lowest free one.
		for ( int value = 1; value <= TagNames.MAX_TAG; value++ )
		{
			if ( !used.contains( value ) )
			{
				return value;
			}
		}
		throw new IllegalStateException( "Ran out of tag numbers (max " + TagNames.MAX_TAG + ")" );
	}

	/**
	 * Numbers for the untagged fields of {@code target}.
	 *
	 * @param selected which of the target's untagged fields to number; {@code null} means all of them. Unselected
	 *                 fields don't use up a number.
	 * @return planned values in declaration order
	 */
	@NotNull
	public static Map<TagField, Integer> planGenerate( @NotNull TagClass target, int start, boolean includeTransient,
			@Nullable Predicate<TagField> selected )
	{
		checkStart( start );
		List<TagClass> hierarchy = hierarchyTopDown( target );
		Set<Integer> used = usedTags( hierarchy );
		Map<TagField, Integer> plan = new LinkedHashMap<>();
		Counter next = new Counter( start, used );

		for ( TagClass cls : hierarchy )
		{
			boolean isTarget = cls.sameAs( target );
			for ( TagField field : cls.getFields() )
			{
				if ( !isTaggable( field, includeTransient ) || field.hasTag() )
				{
					continue;
				}
				if ( !isTarget )
				{
					next.take(); // inherited: reserve only
				}
				else if ( selected == null || selected.test( field ) )
				{
					plan.put( field, next.take() );
				}
			}
		}
		return plan;
	}

	/**
	 * New numbers for the tagged fields of {@code target}, in declaration order from {@code start}, skipping numbers
	 * that belong to superclasses.
	 */
	@NotNull
	public static Map<TagField, Integer> planRenumber( @NotNull TagClass target, int start )
	{
		checkStart( start );
		List<TagClass> hierarchy = hierarchyTopDown( target );
		Set<Integer> used = usedTags( hierarchy.subList( 0, hierarchy.size() - 1 ) );
		Counter next = new Counter( start, used );

		// Untagged inherited fields reserve numbers, exactly as in planGenerate.
		for ( TagClass cls : hierarchy.subList( 0, hierarchy.size() - 1 ) )
		{
			for ( TagField field : cls.getFields() )
			{
				if ( isTaggable( field, false ) && !field.hasTag() )
				{
					next.take();
				}
			}
		}

		Map<TagField, Integer> plan = new LinkedHashMap<>();
		for ( TagField field : target.getFields() )
		{
			if ( field.hasTag() )
			{
				plan.put( field, next.take() );
			}
		}
		return plan;
	}

	private static void checkStart( int start )
	{
		if ( !TagNames.isValidTag( start ) )
		{
			throw new IllegalArgumentException( "Start tag must be between 1 and " + TagNames.MAX_TAG + ": " + start );
		}
	}

	private static final class Counter
	{
		private final Set<Integer> used;
		private int next;

		Counter( int start, Set<Integer> used )
		{
			this.next = start;
			this.used = new HashSet<>( used );
		}

		int take()
		{
			while ( used.contains( next ) )
			{
				next++;
			}
			if ( next > TagNames.MAX_TAG )
			{
				throw new IllegalStateException( "Ran out of tag numbers (max " + TagNames.MAX_TAG + ")" );
			}
			used.add( next );
			return next++;
		}
	}
}
