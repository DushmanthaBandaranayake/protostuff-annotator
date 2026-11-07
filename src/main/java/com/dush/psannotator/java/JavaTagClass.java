package com.dush.psannotator.java;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagNames;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class JavaTagClass implements TagClass
{
	private final PsiClass psiClass;

	JavaTagClass( @NotNull PsiClass psiClass )
	{
		this.psiClass = psiClass;
	}

	@Override public @NotNull String getName()
	{
		String name = psiClass.getName();
		return name != null ? name : "<anonymous>";
	}

	@Override public @NotNull PsiElement getDeclaration()
	{
		return psiClass;
	}

	@Override public @NotNull PsiElement getHighlightElement()
	{
		PsiElement id = psiClass.getNameIdentifier();
		return id != null ? id : psiClass;
	}

	@Override public @NotNull List<JavaTagField> getFields()
	{
		List<JavaTagField> fields = new ArrayList<>();
		for ( PsiField field : psiClass.getFields() )
		{
			fields.add( new JavaTagField( field ) );
		}
		return fields;
	}

	@Override public @Nullable TagClass getSuperClass()
	{
		PsiClass superClass = psiClass.getSuperClass();
		if ( superClass == null || Object.class.getName().equals( superClass.getQualifiedName() ) )
		{
			return null;
		}
		return TagModels.forClass( superClass );
	}

	@Override public boolean isWritable()
	{
		return psiClass.isPhysical() && psiClass.isWritable() && psiClass.getContainingFile() instanceof PsiJavaFile;
	}

	@Override public void addTag( @NotNull TagField field, int value )
	{
		PsiModifierList modifierList = psi( field ).getModifierList();
		if ( modifierList == null )
		{
			return;
		}
		String name;
		if ( canUseShortName() )
		{
			addImportIfMissing();
			name = TagNames.TAG_SHORT;
		}
		else
		{
			name = TagNames.TAG_FQN;
		}
		modifierList.addAfter( createAnnotation( name, value ), null );
	}

	@Override public void setTag( @NotNull TagField field, int value )
	{
		PsiAnnotation tag = ( (JavaTagField) field ).getTagAnnotation();
		if ( tag == null )
		{
			return;
		}
		// Replace only the number, so the annotation keeps its name and layout.
		PsiAnnotationMemberValue current = tag.findDeclaredAttributeValue( "value" );
		PsiExpression literal = JavaPsiFacade.getElementFactory( psiClass.getProject() )
				.createExpressionFromText( Integer.toString( value ), tag );
		if ( current != null )
		{
			current.replace( literal );
		}
		else
		{
			tag.setDeclaredAttributeValue( null, literal );
		}
	}

	@Override public void removeTag( @NotNull TagField field )
	{
		PsiAnnotation tag = ( (JavaTagField) field ).getTagAnnotation();
		if ( tag != null )
		{
			tag.delete();
		}
	}

	@Override public void removeUnusedImport()
	{
		PsiJavaFile file = javaFile();
		PsiImportList importList = file != null ? file.getImportList() : null;
		if ( importList == null )
		{
			return;
		}
		PsiImportStatement statement = importList.findSingleClassImportStatement( TagNames.TAG_FQN );
		if ( statement != null && !fileUsesShortTag( file ) )
		{
			statement.delete();
		}
	}

	private static PsiField psi( TagField field )
	{
		return ( (JavaTagField) field ).getPsiField();
	}

	private PsiAnnotation createAnnotation( String name, int value )
	{
		PsiElementFactory factory = JavaPsiFacade.getElementFactory( psiClass.getProject() );
		return factory.createAnnotationFromText( "@" + name + "(" + value + ")", psiClass );
	}

	@Nullable
	private PsiJavaFile javaFile()
	{
		PsiFile file = psiClass.getContainingFile();
		return file instanceof PsiJavaFile ? (PsiJavaFile) file : null;
	}

	/**
	 * {@code @Tag} can be written unqualified unless the simple name {@code Tag} already refers to some other class
	 * here: a single-type import of another {@code Tag}, a {@code Tag} class in the same package, or a nested or
	 * enclosing class named {@code Tag}.
	 */
	private boolean canUseShortName()
	{
		PsiJavaFile file = javaFile();
		if ( file == null )
		{
			return false;
		}
		PsiImportList importList = file.getImportList();
		if ( importList != null )
		{
			for ( PsiImportStatementBase statement : importList.getAllImportStatements() )
			{
				PsiJavaCodeReferenceElement ref = statement.getImportReference();
				if ( ref != null && !statement.isOnDemand() && TagNames.TAG_SHORT.equals( ref.getReferenceName() ) )
				{
					return TagNames.TAG_FQN.equals( ref.getQualifiedName() );
				}
			}
		}
		PsiClass resolved = JavaPsiFacade.getInstance( psiClass.getProject() ).getResolveHelper()
				.resolveReferencedClass( TagNames.TAG_SHORT, psiClass );
		return resolved == null || TagNames.TAG_FQN.equals( resolved.getQualifiedName() );
	}

	/** Adds {@code import io.protostuff.Tag;} unless it, or {@code import io.protostuff.*;}, is already there. */
	private void addImportIfMissing()
	{
		PsiJavaFile file = javaFile();
		if ( file == null || TagNames.PACKAGE.equals( file.getPackageName() ) )
		{
			return;
		}
		PsiImportList importList = file.getImportList();
		if ( importList == null || importList.findSingleClassImportStatement( TagNames.TAG_FQN ) != null
				|| importList.findOnDemandImportStatement( TagNames.PACKAGE ) != null )
		{
			return;
		}

		PsiClass tagClass = JavaPsiFacade.getInstance( psiClass.getProject() )
				.findClass( TagNames.TAG_FQN, GlobalSearchScope.allScope( psiClass.getProject() ) );
		if ( tagClass != null )
		{
			// Places the import according to the project's import layout.
			JavaCodeStyleManager.getInstance( psiClass.getProject() ).addImport( file, tagClass );
			return;
		}

		// Protostuff isn't resolvable yet (not on the classpath, or still indexing): add the import as text.
		PsiFile dummy = PsiFileFactory.getInstance( psiClass.getProject() )
				.createFileFromText( "_Dummy_.java", JavaFileType.INSTANCE, "import " + TagNames.TAG_FQN + ";" );
		PsiImportList dummyImports = ( (PsiJavaFile) dummy ).getImportList();
		PsiImportStatement statement =
				dummyImports != null ? dummyImports.findSingleClassImportStatement( TagNames.TAG_FQN ) : null;
		if ( statement != null )
		{
			importList.add( statement );
		}
	}

	private static boolean fileUsesShortTag( PsiJavaFile file )
	{
		boolean[] found = { false };
		file.accept( new JavaRecursiveElementWalkingVisitor()
		{
			@Override public void visitAnnotation( @NotNull PsiAnnotation annotation )
			{
				PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
				if ( ref != null && !ref.isQualified() && TagNames.TAG_SHORT.equals( ref.getReferenceName() ) )
				{
					found[0] = true;
					stopWalking();
				}
			}
		} );
		return found[0];
	}

	/** Models are recreated on every {@code getFields()} call, so identity is the underlying declaration. */
	@Override public boolean equals( Object o )
	{
		return o instanceof JavaTagClass other && psiClass.equals( other.psiClass );
	}

	@Override public int hashCode()
	{
		return psiClass.hashCode();
	}
}
