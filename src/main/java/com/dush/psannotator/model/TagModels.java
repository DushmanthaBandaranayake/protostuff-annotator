package com.dush.psannotator.model;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Entry point to the language-specific {@link TagModelProvider}s. */
public final class TagModels
{
	private TagModels()
	{
	}

	@Nullable
	public static TagClass forClass( @Nullable PsiElement element )
	{
		if ( element == null )
		{
			return null;
		}
		for ( TagModelProvider provider : TagModelProvider.EP_NAME.getExtensionList() )
		{
			TagClass model = provider.forClass( element );
			if ( model != null )
			{
				return model;
			}
		}
		return null;
	}

	@Nullable
	public static TagClass enclosingClass( @Nullable PsiElement element )
	{
		if ( element == null )
		{
			return null;
		}
		for ( TagModelProvider provider : TagModelProvider.EP_NAME.getExtensionList() )
		{
			TagClass model = provider.enclosingClass( element );
			if ( model != null )
			{
				return model;
			}
		}
		return null;
	}

	@NotNull
	public static List<TagClass> classesInFile( @NotNull PsiFile file )
	{
		List<TagClass> result = new ArrayList<>();
		for ( TagModelProvider provider : TagModelProvider.EP_NAME.getExtensionList() )
		{
			result.addAll( provider.classesInFile( file ) );
		}
		return result;
	}

	/** The field of the innermost taggable class whose declaration contains {@code element}, or {@code null}. */
	@Nullable
	public static TagField fieldAt( @Nullable PsiElement element )
	{
		TagClass cls = enclosingClass( element );
		if ( cls == null )
		{
			return null;
		}
		for ( TagField field : cls.getFields() )
		{
			if ( PsiTreeUtil.isAncestor( field.getDeclaration(), element, false ) )
			{
				return field;
			}
		}
		return null;
	}
}
