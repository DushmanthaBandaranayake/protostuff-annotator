package com.dush.psannotator.inspection;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagNames;
import com.dush.psannotator.model.TagPlanner;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Finds {@code @Tag} mistakes that make Protostuff fail at runtime, or that it silently ignores. Kept free of
 * inspection APIs so it is easy to test.
 */
public final class TagProblems
{
	public enum Kind
	{
		/** Some fields in the hierarchy are tagged, this one isn't. */
		MISSING_TAG,
		/** Same, for fields inherited from a superclass (reported on the class). */
		MISSING_TAG_INHERITED,
		/** Two serialized fields share a number. */
		DUPLICATE,
		/** Below 1 or above 2^29-1. */
		OUT_OF_RANGE,
		/** {@code @Tag} on a static, transient or excluded field, which Protostuff ignores. */
		IGNORED,
		/** Kotlin constructor property tagged without {@code @field:}: the tag goes on the parameter. */
		WRONG_TARGET
	}

	public record Problem(Kind kind, PsiElement element, String message)
	{
	}

	private TagProblems()
	{
	}

	@NotNull
	public static List<Problem> check( @NotNull TagClass target )
	{
		List<Problem> problems = new ArrayList<>();
		List<TagClass> hierarchy = TagPlanner.hierarchyTopDown( target );

		boolean anyTag = false;
		Map<Integer, List<Owned>> byValue = new LinkedHashMap<>();
		for ( TagClass cls : hierarchy )
		{
			for ( TagField field : cls.getFields() )
			{
				if ( !field.isSerialized() || !field.hasTag() )
				{
					continue;
				}
				anyTag = true;
				Integer value = field.getTagValue();
				if ( value != null )
				{
					byValue.computeIfAbsent( value, v -> new ArrayList<>() ).add( new Owned( cls, field ) );
				}
			}
		}

		List<String> untaggedInherited = new ArrayList<>();
		for ( TagClass cls : hierarchy )
		{
			boolean isTarget = cls.sameAs( target );
			for ( TagField field : cls.getFields() )
			{
				if ( isTarget )
				{
					checkOwnField( field, anyTag, byValue, problems );
				}
				else if ( anyTag && field.isSerialized() && !field.hasTag() )
				{
					untaggedInherited.add( cls.getName() + "." + field.getName() );
				}
			}
		}

		if ( !untaggedInherited.isEmpty() )
		{
			problems.add( new Problem( Kind.MISSING_TAG_INHERITED, target.getHighlightElement(),
					"Protostuff needs @Tag on every serialized field, but inherited "
							+ ( untaggedInherited.size() == 1 ? "field " : "fields " )
							+ String.join( ", ", untaggedInherited ) + " " + ( untaggedInherited.size() == 1 ? "has" : "have" )
							+ " none" ) );
		}
		return problems;
	}

	private static void checkOwnField( TagField field, boolean anyTag, Map<Integer, List<Owned>> byValue,
			List<Problem> problems )
	{
		PsiElement tag = field.getTagAnnotation();
		if ( tag == null )
		{
			if ( anyTag && field.isSerialized() )
			{
				problems.add( new Problem( Kind.MISSING_TAG, field.getHighlightElement(),
						"Field '" + field.getName() + "' has no @Tag; Protostuff requires all serialized fields to be "
								+ "tagged once any field is" ) );
			}
			return;
		}

		if ( !field.isSerialized() )
		{
			String why = field.isStatic() ? "static" : field.isExcluded() ? "excluded with @Exclude" : "transient";
			problems.add( new Problem( Kind.IGNORED, tag,
					"@Tag has no effect: '" + field.getName() + "' is " + why + ", so Protostuff doesn't serialize it" ) );
			return;
		}

		if ( field.isTagOnWrongTarget() )
		{
			problems.add( new Problem( Kind.WRONG_TARGET, tag,
					"This @Tag goes on the constructor parameter, not the field, so Protostuff doesn't see it; "
							+ "use @field:Tag" ) );
		}

		Integer value = field.getTagValue();
		if ( value == null )
		{
			return;
		}
		if ( !TagNames.isValidTag( value ) )
		{
			problems.add( new Problem( Kind.OUT_OF_RANGE, tag,
					"Tag " + value + " is out of range; it must be between 1 and " + TagNames.MAX_TAG ) );
			return;
		}
		List<Owned> sharing = byValue.getOrDefault( value, List.of() );
		if ( sharing.size() > 1 )
		{
			String others = sharing.stream()
					.filter( o -> o.field.getDeclaration() != field.getDeclaration() )
					.map( o -> o.cls.getName() + "." + o.field.getName() )
					.collect( Collectors.joining( ", " ) );
			problems.add( new Problem( Kind.DUPLICATE, tag, "Tag " + value + " is also used by " + others ) );
		}
	}

	private record Owned(TagClass cls, TagField field)
	{
	}
}
