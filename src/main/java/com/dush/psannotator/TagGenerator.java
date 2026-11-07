package com.dush.psannotator;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds Protostuff {@code @Tag} annotations to the fields of a class.
 * <p>
 * Numbering walks the class hierarchy from the top-most superclass down, in declaration order, so a subclass never
 * reuses a number that belongs to one of its superclasses:
 * <ul>
 *     <li>numbers already used by an existing {@code @Tag} anywhere in the hierarchy are never handed out again;</li>
 *     <li>fields that already have a {@code @Tag} are left alone;</li>
 *     <li>an untagged inherited field reserves the next free number, but is not modified;</li>
 *     <li>static fields are always skipped (Protostuff never serializes them), transient fields are skipped unless
 *     {@code includeTransient} is set.</li>
 * </ul>
 * Must be called inside a write command.
 *
 * @author dushmantha
 */
public final class TagGenerator
{
	public static final String TAG_FQN = "io.protostuff.Tag";
	/** Largest field number allowed by the protobuf wire format, which Protostuff uses. */
	public static final int MAX_TAG = (1 << 29) - 1;

	private TagGenerator()
	{
	}

	/**
	 * @return the number of fields that were annotated
	 */
	public static int generate( @NotNull PsiClass psiClass, int startTag, boolean includeTransient )
	{
		if ( startTag < 1 || startTag > MAX_TAG )
		{
			throw new IllegalArgumentException( "Start tag must be between 1 and " + MAX_TAG + ": " + startTag );
		}

		List<PsiClass> hierarchy = hierarchyTopDown( psiClass );

		Set<Integer> used = new HashSet<>();
		for ( PsiClass cls : hierarchy )
		{
			for ( PsiField field : cls.getFields() )
			{
				Integer existing = existingTagValue( field );
				if ( existing != null )
				{
					used.add( existing );
				}
			}
		}

		PsiElementFactory factory = JavaPsiFacade.getElementFactory( psiClass.getProject() );
		JavaCodeStyleManager codeStyle = JavaCodeStyleManager.getInstance( psiClass.getProject() );

		int next = startTag;
		int annotated = 0;
		for ( PsiClass cls : hierarchy )
		{
			boolean isTarget = cls.equals( psiClass );
			for ( PsiField field : cls.getFields() )
			{
				if ( !isEligible( field, includeTransient ) || findTag( field ) != null )
				{
					continue;
				}
				while ( used.contains( next ) )
				{
					next++;
				}
				if ( next > MAX_TAG )
				{
					throw new IllegalStateException( "Ran out of tag numbers (max " + MAX_TAG + ")" );
				}
				int tag = next++;
				used.add( tag );

				if ( isTarget )
				{
					addTag( field, tag, factory, codeStyle );
					annotated++;
				}
			}
		}
		return annotated;
	}

	/**
	 * @return {@code true} if {@code psiClass} has at least one field that {@link #generate} would annotate
	 */
	public static boolean hasTaggableFields( @NotNull PsiClass psiClass, boolean includeTransient )
	{
		for ( PsiField field : psiClass.getFields() )
		{
			if ( isEligible( field, includeTransient ) && findTag( field ) == null )
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isEligible( PsiField field, boolean includeTransient )
	{
		if ( field.hasModifierProperty( PsiModifier.STATIC ) )
		{
			return false;
		}
		return includeTransient || !field.hasModifierProperty( PsiModifier.TRANSIENT );
	}

	private static void addTag( PsiField field, int tag, PsiElementFactory factory, JavaCodeStyleManager codeStyle )
	{
		PsiModifierList modifierList = field.getModifierList();
		if ( modifierList == null )
		{
			return;
		}
		PsiAnnotation annotation = factory.createAnnotationFromText( "@" + TAG_FQN + "(" + tag + ")", field );
		PsiAnnotation added = (PsiAnnotation) modifierList.addAfter( annotation, null );
		// Adds "import io.protostuff.Tag;" when Protostuff is on the classpath and nothing conflicts;
		// otherwise the fully qualified name is kept so the code still refers to the right annotation.
		codeStyle.shortenClassReferences( added );
	}

	/**
	 * Finds the {@code @Tag} annotation on a field. Matches {@code io.protostuff.Tag}, and also an unresolved
	 * {@code @Tag} (Protostuff not on the classpath yet), but not a {@code Tag} annotation from another package.
	 */
	@Nullable
	static PsiAnnotation findTag( PsiField field )
	{
		PsiModifierList modifierList = field.getModifierList();
		if ( modifierList == null )
		{
			return null;
		}
		for ( PsiAnnotation annotation : modifierList.getAnnotations() )
		{
			String qualifiedName = annotation.getQualifiedName();
			if ( TAG_FQN.equals( qualifiedName ) )
			{
				return annotation;
			}
			PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
			if ( ref != null && "Tag".equals( ref.getReferenceName() ) && ref.resolve() == null )
			{
				return annotation;
			}
		}
		return null;
	}

	@Nullable
	private static Integer existingTagValue( PsiField field )
	{
		PsiAnnotation tag = findTag( field );
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

	private static List<PsiClass> hierarchyTopDown( PsiClass psiClass )
	{
		Deque<PsiClass> chain = new ArrayDeque<>();
		Set<PsiClass> seen = new HashSet<>();
		for ( PsiClass cls = psiClass; cls != null && seen.add( cls ); cls = cls.getSuperClass() )
		{
			if ( !Object.class.getName().equals( cls.getQualifiedName() ) )
			{
				chain.addFirst( cls );
			}
		}
		return new ArrayList<>( chain );
	}
}
