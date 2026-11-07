package com.dush.psannotator.model;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Builds {@link TagClass} models for one language. */
public interface TagModelProvider
{
	ExtensionPointName<TagModelProvider> EP_NAME = ExtensionPointName.create( "com.dush.psannotator.tagModelProvider" );

	/**
	 * @param element a class declaration, or a {@code PsiClass} (possibly a light class or a compiled class) reached
	 *                through a superclass reference
	 * @return the model, or {@code null} if this provider doesn't handle the element or it isn't a taggable class
	 * (interfaces, annotations, enums, objects)
	 */
	@Nullable TagClass forClass( @NotNull PsiElement element );

	/** The innermost taggable class that contains {@code element}, or {@code null}. */
	@Nullable TagClass enclosingClass( @NotNull PsiElement element );

	/** All taggable classes in the file, nested ones included. Empty if this provider doesn't handle the file. */
	@NotNull List<TagClass> classesInFile( @NotNull PsiFile file );
}
