package com.dush.psannotator.kotlin;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtModifierListOwner;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtPropertyAccessor;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.ValueArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class KotlinTagClass implements TagClass
{
	private static final Pattern FIELD_WORD = Pattern.compile( "\\bfield\\b" );

	private final KtClass ktClass;

	KotlinTagClass( @NotNull KtClass ktClass )
	{
		this.ktClass = ktClass;
	}

	@Override public @NotNull String getName()
	{
		String name = ktClass.getName();
		return name != null ? name : "<anonymous>";
	}

	@Override public @NotNull PsiElement getDeclaration()
	{
		return ktClass;
	}

	@Override public @NotNull PsiElement getHighlightElement()
	{
		PsiElement id = ktClass.getNameIdentifier();
		return id != null ? id : ktClass;
	}

	@Override public @NotNull List<KotlinTagField> getFields()
	{
		List<KotlinTagField> fields = new ArrayList<>();
		for ( KtParameter parameter : ktClass.getPrimaryConstructorParameters() )
		{
			if ( parameter.hasValOrVar() )
			{
				fields.add( new KotlinTagField( parameter, this ) );
			}
		}
		KtClassBody body = ktClass.getBody();
		if ( body != null )
		{
			for ( KtDeclaration declaration : body.getDeclarations() )
			{
				if ( declaration instanceof KtProperty property && hasBackingField( property ) )
				{
					fields.add( new KotlinTagField( property, this ) );
				}
			}
		}
		return fields;
	}

	/**
	 * Without type analysis: a property has a backing field unless it is abstract, {@code const} (static),
	 * delegated, an extension, or has custom accessors that never mention {@code field} and no initializer.
	 */
	private static boolean hasBackingField( KtProperty property )
	{
		if ( property.hasModifier( KtTokens.ABSTRACT_KEYWORD ) || property.hasModifier( KtTokens.CONST_KEYWORD )
				|| property.getDelegate() != null || property.getReceiverTypeReference() != null )
		{
			return false;
		}
		if ( property.hasInitializer() || property.hasModifier( KtTokens.LATEINIT_KEYWORD ) )
		{
			return true;
		}
		KtPropertyAccessor getter = property.getGetter();
		if ( getter == null || !getter.hasBody() || mentionsField( getter ) )
		{
			return true; // the default getter reads the field
		}
		if ( !property.isVar() )
		{
			return false; // val with a computed getter
		}
		KtPropertyAccessor setter = property.getSetter();
		return setter == null || !setter.hasBody() || mentionsField( setter );
	}

	private static boolean mentionsField( KtPropertyAccessor accessor )
	{
		return FIELD_WORD.matcher( accessor.getText() ).find();
	}

	@Override public @Nullable TagClass getSuperClass()
	{
		PsiClass light = getPsiClass();
		PsiClass superClass = light != null ? light.getSuperClass() : null;
		if ( superClass == null || Object.class.getName().equals( superClass.getQualifiedName() ) )
		{
			return null;
		}
		return TagModels.forClass( superClass );
	}

	@Override public @Nullable PsiClass getPsiClass()
	{
		return LightClassUtilsKt.toLightClass( ktClass );
	}

	@Override public boolean isWritable()
	{
		return ktClass.isPhysical() && ktClass.isWritable() && ktClass.getContainingFile() instanceof KtFile;
	}

	@Override public void addTag( @NotNull TagField field, int value )
	{
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
		// "field:" so the tag always lands on the backing field: Tag has no @Target, and on a constructor property
		// Kotlin would otherwise put it on the parameter, where Protostuff doesn't look.
		KtModifierListOwner owner = ( (KotlinTagField) field ).getKtDeclaration();
		owner.addAnnotationEntry( createEntry( "@field:" + name + "(" + value + ")" ) );
	}

	@Override public void setTag( @NotNull TagField field, int value )
	{
		KtAnnotationEntry tag = ( (KotlinTagField) field ).getTagAnnotation();
		if ( tag == null )
		{
			return;
		}
		// Replace only the number, so the annotation keeps its name, use-site target and layout.
		KtAnnotationEntry fresh = createEntry( "@Tag(" + value + ")" );
		ValueArgument arg = KotlinTagField.valueArgument( tag );
		KtExpression current = arg != null ? arg.getArgumentExpression() : null;
		KtExpression replacement = fresh.getValueArguments().get( 0 ).getArgumentExpression();
		if ( current != null && replacement != null )
		{
			current.replace( replacement );
		}
		else if ( tag.getValueArgumentList() != null && fresh.getValueArgumentList() != null )
		{
			tag.getValueArgumentList().replace( fresh.getValueArgumentList() );
		}
	}

	@Override public void fixTagTarget( @NotNull TagField field )
	{
		KtAnnotationEntry tag = ( (KotlinTagField) field ).getTagAnnotation();
		if ( tag == null )
		{
			return;
		}
		String text = tag.getText();
		KtAnnotationUseSiteTarget target = tag.getUseSiteTarget();
		String withoutTarget = target != null
				? "@" + text.substring( text.indexOf( ':' ) + 1 )
				: text;
		tag.replace( createEntry( "@field:" + withoutTarget.substring( 1 ) ) );
	}

	@Override public void removeTag( @NotNull TagField field )
	{
		KtAnnotationEntry tag = ( (KotlinTagField) field ).getTagAnnotation();
		if ( tag == null )
		{
			return;
		}
		PsiElement next = tag.getNextSibling();
		tag.delete();
		if ( next instanceof com.intellij.psi.PsiWhiteSpace && next.isValid() )
		{
			next.delete();
		}
	}

	@Override public void removeUnusedImport()
	{
		KtFile file = ktFile();
		if ( file == null || file.getImportList() == null )
		{
			return;
		}
		for ( KtAnnotationEntry entry : PsiTreeUtil.findChildrenOfType( file, KtAnnotationEntry.class ) )
		{
			KtTypeReference type = entry.getTypeReference();
			if ( type != null && TagNames.TAG_SHORT.equals( type.getText() ) )
			{
				return;
			}
		}
		for ( KtImportDirective directive : file.getImportDirectives() )
		{
			if ( isImportOf( directive, TagNames.TAG_FQN ) )
			{
				directive.delete();
			}
		}
	}

	/** Is this annotation the Protostuff one named {@code fqn}? Unresolved simple names count unless shadowed. */
	boolean refersTo( KtAnnotationEntry entry, String fqn, String shortName )
	{
		KtTypeReference type = entry.getTypeReference();
		if ( type == null )
		{
			return false;
		}
		String text = type.getText().replace( " ", "" );
		if ( text.equals( fqn ) )
		{
			return true;
		}
		if ( !text.equals( shortName ) )
		{
			return false;
		}
		KtFile file = ktFile();
		if ( file == null )
		{
			return true;
		}
		String importedAs = importedFqName( file, shortName );
		return importedAs == null || importedAs.equals( fqn );
	}

	@Nullable
	private KtFile ktFile()
	{
		PsiFile file = ktClass.getContainingFile();
		return file instanceof KtFile ? (KtFile) file : null;
	}

	/** The fully qualified name a single (non-star) import binds to {@code simpleName}, or {@code null}. */
	@Nullable
	private static String importedFqName( KtFile file, String simpleName )
	{
		for ( KtImportDirective directive : file.getImportDirectives() )
		{
			FqName fq = directive.getImportedFqName();
			if ( fq == null || directive.isAllUnder() )
			{
				continue;
			}
			String alias = directive.getAliasName();
			String boundName = alias != null ? alias : fq.shortName().asString();
			if ( simpleName.equals( boundName ) )
			{
				return fq.asString();
			}
		}
		return null;
	}

	private static boolean isImportOf( KtImportDirective directive, String fqn )
	{
		FqName fq = directive.getImportedFqName();
		return fq != null && !directive.isAllUnder() && directive.getAliasName() == null && fqn.equals( fq.asString() );
	}

	private boolean canUseShortName()
	{
		KtFile file = ktFile();
		if ( file == null )
		{
			return false;
		}
		String imported = importedFqName( file, TagNames.TAG_SHORT );
		if ( imported != null )
		{
			return imported.equals( TagNames.TAG_FQN );
		}
		// A Tag class in the same package, or nested in / enclosing this class, would shadow the import.
		String pkg = file.getPackageFqName().asString();
		if ( !TagNames.PACKAGE.equals( pkg ) )
		{
			String samePackage = pkg.isEmpty() ? TagNames.TAG_SHORT : pkg + "." + TagNames.TAG_SHORT;
			if ( JavaPsiFacade.getInstance( ktClass.getProject() )
					.findClass( samePackage, GlobalSearchScope.allScope( ktClass.getProject() ) ) != null )
			{
				return false;
			}
		}
		for ( KtClassOrObject cls = ktClass; cls != null;
				cls = PsiTreeUtil.getParentOfType( cls, KtClassOrObject.class, true ) )
		{
			if ( TagNames.TAG_SHORT.equals( cls.getName() ) )
			{
				return false;
			}
			for ( KtDeclaration declaration : cls.getDeclarations() )
			{
				if ( declaration instanceof KtClassOrObject && TagNames.TAG_SHORT.equals( declaration.getName() ) )
				{
					return false;
				}
			}
		}
		return true;
	}

	private void addImportIfMissing()
	{
		KtFile file = ktFile();
		if ( file == null || TagNames.PACKAGE.equals( file.getPackageFqName().asString() ) )
		{
			return;
		}
		for ( KtImportDirective directive : file.getImportDirectives() )
		{
			FqName fq = directive.getImportedFqName();
			if ( isImportOf( directive, TagNames.TAG_FQN )
					|| ( directive.isAllUnder() && fq != null && TagNames.PACKAGE.equals( fq.asString() ) ) )
			{
				return;
			}
		}
		KtImportList importList = file.getImportList();
		if ( importList == null )
		{
			return;
		}
		KtFile dummy = (KtFile) PsiFileFactory.getInstance( ktClass.getProject() )
				.createFileFromText( "_Dummy_.kt", KotlinFileType.INSTANCE, "import " + TagNames.TAG_FQN + "\n" );
		KtImportDirective directive = dummy.getImportDirectives().get( 0 );

		PsiParserFacade parser = PsiParserFacade.getInstance( ktClass.getProject() );
		List<KtImportDirective> existing = importList.getImports();
		if ( existing.isEmpty() )
		{
			importList.add( directive );
			// Separate the new import from the package line before it and the code after it.
			if ( file.getPackageDirective() != null && !file.getPackageDirective().getText().isEmpty() )
			{
				importList.getParent().addBefore( parser.createWhiteSpaceFromText( "\n\n" ), importList );
			}
			importList.getParent().addAfter( parser.createWhiteSpaceFromText( "\n\n" ), importList );
			cleanUpWhitespaceAround( importList );
		}
		else
		{
			KtImportDirective last = existing.get( existing.size() - 1 );
			importList.addAfter( directive, last );
			importList.addAfter( parser.createWhiteSpaceFromText( "\n" ), last );
		}
	}

	/** Merges whitespace left on both sides of the import list after inserting new whitespace. */
	private static void cleanUpWhitespaceAround( KtImportList importList )
	{
		for ( PsiElement ws : new PsiElement[] { importList.getPrevSibling(), importList.getNextSibling() } )
		{
			if ( ws instanceof com.intellij.psi.PsiWhiteSpace )
			{
				PsiElement neighbour = ws == importList.getPrevSibling() ? ws.getPrevSibling() : ws.getNextSibling();
				if ( neighbour instanceof com.intellij.psi.PsiWhiteSpace )
				{
					neighbour.delete();
				}
			}
		}
	}

	private KtAnnotationEntry createEntry( String annotationText )
	{
		KtFile dummy = (KtFile) PsiFileFactory.getInstance( ktClass.getProject() )
				.createFileFromText( "_Dummy_.kt", KotlinFileType.INSTANCE, annotationText + " val x = 1\n" );
		return PsiTreeUtil.findChildOfType( dummy, KtAnnotationEntry.class );
	}

	/** Models are recreated on every {@code getFields()} call, so identity is the underlying declaration. */
	@Override public boolean equals( Object o )
	{
		return o instanceof KotlinTagClass other && ktClass.equals( other.ktClass );
	}

	@Override public int hashCode()
	{
		return ktClass.hashCode();
	}
}
