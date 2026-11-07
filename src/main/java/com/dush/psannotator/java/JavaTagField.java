package com.dush.psannotator.java;

import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class JavaTagField implements TagField
{
	private final PsiField field;

	JavaTagField( @NotNull PsiField field )
	{
		this.field = field;
	}

	@NotNull PsiField getPsiField()
	{
		return field;
	}

	@Override public @NotNull String getName()
	{
		return field.getName();
	}

	@Override public @NotNull String getTypeText()
	{
		return field.getType().getPresentableText();
	}

	@Override public boolean isStatic()
	{
		return field.hasModifierProperty( PsiModifier.STATIC );
	}

	@Override public boolean isTransient()
	{
		return field.hasModifierProperty( PsiModifier.TRANSIENT );
	}

	@Override public boolean isExcluded()
	{
		return findAnnotation( field, TagNames.EXCLUDE_FQN, TagNames.EXCLUDE_SHORT ) != null;
	}

	@Override public @Nullable PsiAnnotation getTagAnnotation()
	{
		return findAnnotation( field, TagNames.TAG_FQN, TagNames.TAG_SHORT );
	}

	@Override public @Nullable Integer getTagValue()
	{
		PsiAnnotation tag = getTagAnnotation();
		if ( tag == null )
		{
			return null;
		}
		PsiAnnotationMemberValue value = tag.findDeclaredAttributeValue( "value" );
		if ( value == null )
		{
			return null;
		}
		Object constant = JavaPsiFacade.getInstance( field.getProject() ).getConstantEvaluationHelper()
				.computeConstantExpression( value );
		return constant instanceof Integer ? (Integer) constant : null;
	}

	@Override public @NotNull PsiElement getDeclaration()
	{
		return field;
	}

	@Override public @NotNull PsiElement getHighlightElement()
	{
		return field.getNameIdentifier();
	}

	/**
	 * Matches the Protostuff annotation by qualified name, and also an unresolved simple name (Protostuff not on the
	 * classpath yet), but not a same-named annotation from another package.
	 */
	@Nullable
	static PsiAnnotation findAnnotation( PsiField field, String fqn, String shortName )
	{
		PsiModifierList modifierList = field.getModifierList();
		if ( modifierList == null )
		{
			return null;
		}
		for ( PsiAnnotation annotation : modifierList.getAnnotations() )
		{
			if ( fqn.equals( annotation.getQualifiedName() ) )
			{
				return annotation;
			}
			PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
			if ( ref != null && shortName.equals( ref.getReferenceName() ) && ref.resolve() == null )
			{
				return annotation;
			}
		}
		return null;
	}

	/** Models are recreated on every {@code getFields()} call, so identity is the underlying declaration. */
	@Override public boolean equals( Object o )
	{
		return o instanceof JavaTagField other && field.equals( other.field );
	}

	@Override public int hashCode()
	{
		return field.hashCode();
	}
}
