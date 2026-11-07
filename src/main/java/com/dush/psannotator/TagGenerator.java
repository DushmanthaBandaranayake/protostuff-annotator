package com.dush.psannotator;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ide.highlighter.JavaFileType;
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
 * Annotations are written as {@code @Tag(n)} and {@code import io.protostuff.Tag;} is added when it is missing, even
 * if Protostuff can't be resolved yet. The fully qualified name is only used when {@code Tag} already means a
 * different class in that file.
 * Must be called inside a write command.
 *
 * @author dushmantha
 */
public final class TagGenerator
{
	public static final String TAG_FQN = "io.protostuff.Tag";
	private static final String TAG_PACKAGE = "io.protostuff";
	private static final String TAG_SHORT_NAME = "Tag";
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
		boolean useShortName = canUseShortName( psiClass );
		boolean importChecked = false;

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
					if ( useShortName && !importChecked )
					{
						addImportIfMissing( psiClass );
						importChecked = true;
					}
					addTag( field, tag, useShortName ? TAG_SHORT_NAME : TAG_FQN, factory );
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

	private static void addTag( PsiField field, int tag, String annotationName, PsiElementFactory factory )
	{
		PsiModifierList modifierList = field.getModifierList();
		if ( modifierList == null )
		{
			return;
		}
		PsiAnnotation annotation = factory.createAnnotationFromText( "@" + annotationName + "(" + tag + ")", field );
		modifierList.addAfter( annotation, null );
	}

	/**
	 * {@code @Tag} can be written unqualified unless the simple name {@code Tag} already refers to some other class
	 * in this file: a single-type import of another {@code Tag}, a class named {@code Tag} in the same package, or a
	 * nested/enclosing class named {@code Tag}.
	 */
	private static boolean canUseShortName( PsiClass psiClass )
	{
		if ( !( psiClass.getContainingFile() instanceof PsiJavaFile ) )
		{
			return false;
		}
		PsiJavaFile file = (PsiJavaFile) psiClass.getContainingFile();
		PsiImportList importList = file.getImportList();
		if ( importList != null )
		{
			for ( PsiImportStatementBase statement : importList.getAllImportStatements() )
			{
				PsiJavaCodeReferenceElement ref = statement.getImportReference();
				if ( ref == null || statement.isOnDemand() )
				{
					continue;
				}
				if ( TAG_SHORT_NAME.equals( ref.getReferenceName() ) )
				{
					// An existing single-type import decides what "Tag" means.
					return TAG_FQN.equals( ref.getQualifiedName() );
				}
			}
		}
		PsiClass resolved = JavaPsiFacade.getInstance( psiClass.getProject() ).getResolveHelper()
				.resolveReferencedClass( TAG_SHORT_NAME, psiClass );
		return resolved == null || TAG_FQN.equals( resolved.getQualifiedName() );
	}

	/** Adds {@code import io.protostuff.Tag;} unless it, or {@code import io.protostuff.*;}, is already there. */
	private static void addImportIfMissing( PsiClass psiClass )
	{
		PsiJavaFile file = (PsiJavaFile) psiClass.getContainingFile();
		if ( TAG_PACKAGE.equals( file.getPackageName() ) )
		{
			return;
		}
		PsiImportList importList = file.getImportList();
		if ( importList == null || importList.findSingleClassImportStatement( TAG_FQN ) != null
				|| importList.findOnDemandImportStatement( TAG_PACKAGE ) != null )
		{
			return;
		}

		PsiClass tagClass = JavaPsiFacade.getInstance( psiClass.getProject() )
				.findClass( TAG_FQN, GlobalSearchScope.allScope( psiClass.getProject() ) );
		if ( tagClass != null )
		{
			// Places the import according to the project's import layout.
			JavaCodeStyleManager.getInstance( psiClass.getProject() ).addImport( file, tagClass );
			return;
		}

		// Protostuff isn't resolvable yet (not on the classpath, or still indexing): add the import as text.
		PsiFile dummy = PsiFileFactory.getInstance( psiClass.getProject() )
				.createFileFromText( "_Dummy_.java", JavaFileType.INSTANCE, "import " + TAG_FQN + ";" );
		PsiImportList dummyImports = ( (PsiJavaFile) dummy ).getImportList();
		PsiImportStatement statement = dummyImports != null ? dummyImports.findSingleClassImportStatement( TAG_FQN ) : null;
		if ( statement != null )
		{
			importList.add( statement );
		}
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
