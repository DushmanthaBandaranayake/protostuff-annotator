package com.dush.psannotator.model;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A class whose fields can carry {@code @Tag}. Implementations exist for Java and, when the Kotlin plugin is
 * installed, for Kotlin. Write methods must be called inside a write command.
 */
public interface TagClass
{
	@NotNull String getName();

	/** The class declaration. */
	@NotNull PsiElement getDeclaration();

	/** Where class-level problems are highlighted. */
	@NotNull PsiElement getHighlightElement();

	/** Fields in declaration order. */
	@NotNull List<? extends TagField> getFields();

	/** The superclass, or {@code null} for none or {@code java.lang.Object}. */
	@Nullable TagClass getSuperClass();

	/** {@code true} if the class is in project source and can be edited. */
	boolean isWritable();

	/** Adds {@code @Tag(value)}, plus the import when needed. */
	void addTag( @NotNull TagField field, int value );

	/** Changes the value of an existing {@code @Tag}. */
	void setTag( @NotNull TagField field, int value );

	void removeTag( @NotNull TagField field );

	/** Moves a {@link TagField#isTagOnWrongTarget() misplaced} {@code @Tag} onto the field. */
	default void fixTagTarget( @NotNull TagField field )
	{
	}

	/** Removes {@code import io.protostuff.Tag} if nothing in the file uses it any more. */
	void removeUnusedImport();

	default boolean sameAs( @NotNull TagClass other )
	{
		return getDeclaration().equals( other.getDeclaration() );
	}
}
