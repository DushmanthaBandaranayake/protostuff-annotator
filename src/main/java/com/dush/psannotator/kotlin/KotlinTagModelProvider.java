package com.dush.psannotator.kotlin;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagModelProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.List;

/** Kotlin classes from source. Only loaded when the Kotlin plugin is installed. */
public final class KotlinTagModelProvider implements TagModelProvider
{
	@Override public @Nullable TagClass forClass( @NotNull PsiElement element )
	{
		if ( element instanceof KtLightClass light )
		{
			element = light.getKotlinOrigin();
		}
		if ( !( element instanceof KtClass ktClass ) || element instanceof KtEnumEntry )
		{
			return null;
		}
		if ( ktClass.isInterface() || ktClass.isEnum() || ktClass.isAnnotation() )
		{
			return null;
		}
		return new KotlinTagClass( ktClass );
	}

	@Override public @Nullable TagClass enclosingClass( @NotNull PsiElement element )
	{
		if ( !( element.getContainingFile() instanceof KtFile ) )
		{
			return null;
		}
		KtClassOrObject cls = element instanceof KtClassOrObject ? (KtClassOrObject) element
				: PsiTreeUtil.getParentOfType( element, KtClassOrObject.class );
		return cls != null ? forClass( cls ) : null;
	}

	@Override public @NotNull List<TagClass> classesInFile( @NotNull PsiFile file )
	{
		List<TagClass> result = new ArrayList<>();
		if ( file instanceof KtFile )
		{
			for ( KtClassOrObject cls : PsiTreeUtil.findChildrenOfType( file, KtClassOrObject.class ) )
			{
				TagClass model = forClass( cls );
				if ( model != null )
				{
					result.add( model );
				}
			}
		}
		return result;
	}
}
