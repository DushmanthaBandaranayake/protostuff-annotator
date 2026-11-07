package com.dush.psannotator.java;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagModelProvider;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Java classes, from source or compiled libraries (libraries are read-only superclasses). */
public final class JavaTagModelProvider implements TagModelProvider
{
	@Override public @Nullable TagClass forClass( @NotNull PsiElement element )
	{
		if ( !( element instanceof PsiClass ) || !element.getLanguage().isKindOf( JavaLanguage.INSTANCE ) )
		{
			return null;
		}
		PsiClass psiClass = (PsiClass) element;
		if ( psiClass.isInterface() || psiClass.isAnnotationType() || psiClass.isEnum()
				|| psiClass instanceof PsiAnonymousClass || psiClass instanceof PsiTypeParameter )
		{
			return null;
		}
		return new JavaTagClass( psiClass );
	}

	@Override public @Nullable TagClass enclosingClass( @NotNull PsiElement element )
	{
		if ( !( element.getContainingFile() instanceof PsiJavaFile ) )
		{
			return null;
		}
		PsiClass psiClass = element instanceof PsiClass ? (PsiClass) element
				: PsiTreeUtil.getParentOfType( element, PsiClass.class );
		while ( psiClass instanceof PsiAnonymousClass )
		{
			psiClass = PsiTreeUtil.getParentOfType( psiClass, PsiClass.class );
		}
		return psiClass != null ? forClass( psiClass ) : null;
	}

	@Override public @NotNull List<TagClass> classesInFile( @NotNull PsiFile file )
	{
		List<TagClass> result = new ArrayList<>();
		if ( file instanceof PsiJavaFile )
		{
			for ( PsiClass psiClass : PsiTreeUtil.findChildrenOfType( file, PsiClass.class ) )
			{
				TagClass model = forClass( psiClass );
				if ( model != null )
				{
					result.add( model );
				}
			}
		}
		return result;
	}
}
