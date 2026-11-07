package com.dush.psannotator.model;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A field (Java) or backing-field property (Kotlin) that Protostuff may serialize. */
public interface TagField
{
	@NotNull String getName();

	/** Declared type as written, for display only. */
	@NotNull String getTypeText();

	boolean isStatic();

	boolean isTransient();

	/** Marked with {@code @io.protostuff.Exclude}. */
	boolean isExcluded();

	/** The field's {@code @Tag} annotation, or {@code null}. */
	@Nullable PsiElement getTagAnnotation();

	/** The {@code @Tag} value if it is a constant integer, otherwise {@code null}. */
	@Nullable Integer getTagValue();

	/** The declaration itself. */
	@NotNull PsiElement getDeclaration();

	/** Where problems about this field are highlighted (normally its name). */
	@NotNull PsiElement getHighlightElement();

	/**
	 * {@code true} if the {@code @Tag} is there but lands somewhere Protostuff can't see it, e.g. a Kotlin
	 * constructor property annotated without {@code @field:}, where the annotation goes on the parameter.
	 */
	default boolean isTagOnWrongTarget()
	{
		return false;
	}

	default boolean hasTag()
	{
		return getTagAnnotation() != null;
	}

	/** Protostuff serializes this field: not static, not transient, not excluded. */
	default boolean isSerialized()
	{
		return !isStatic() && !isTransient() && !isExcluded();
	}
}
