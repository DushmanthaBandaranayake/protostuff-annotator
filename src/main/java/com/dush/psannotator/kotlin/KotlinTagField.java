package com.dush.psannotator.kotlin;

import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagNames;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.psi.ValueArgumentName;

/** A constructor {@code val}/{@code var} parameter or a body property with a backing field. */
final class KotlinTagField implements TagField
{
	private final KtCallableDeclaration declaration;
	private final KotlinTagClass owner;

	KotlinTagField( @NotNull KtCallableDeclaration declaration, @NotNull KotlinTagClass owner )
	{
		this.declaration = declaration;
		this.owner = owner;
	}

	@NotNull KtCallableDeclaration getKtDeclaration()
	{
		return declaration;
	}

	@Override public @NotNull String getName()
	{
		String name = declaration.getName();
		return name != null ? name : "<unnamed>";
	}

	@Override public @NotNull String getTypeText()
	{
		KtTypeReference type = declaration.getTypeReference();
		return type != null ? type.getText() : "";
	}

	@Override public boolean isStatic()
	{
		return false; // companion/object properties live in another class
	}

	@Override public boolean isTransient()
	{
		return findAnnotation( "kotlin.jvm.Transient", "Transient" ) != null;
	}

	@Override public boolean isExcluded()
	{
		return findAnnotation( TagNames.EXCLUDE_FQN, TagNames.EXCLUDE_SHORT ) != null;
	}

	@Override public @Nullable KtAnnotationEntry getTagAnnotation()
	{
		return findAnnotation( TagNames.TAG_FQN, TagNames.TAG_SHORT );
	}

	@Override public @Nullable Integer getTagValue()
	{
		KtAnnotationEntry tag = getTagAnnotation();
		if ( tag == null )
		{
			return null;
		}
		ValueArgument arg = valueArgument( tag );
		KtExpression expression = arg != null ? arg.getArgumentExpression() : null;
		return expression != null ? parseIntLiteral( expression.getText() ) : null;
	}

	/** The {@code value} argument: named {@code value = n}, or the first positional one. */
	@Nullable
	static ValueArgument valueArgument( KtAnnotationEntry tag )
	{
		for ( ValueArgument arg : tag.getValueArguments() )
		{
			ValueArgumentName name = arg.getArgumentName();
			if ( name == null ? !arg.isNamed() : "value".equals( name.getAsName().asString() ) )
			{
				return arg;
			}
		}
		return null;
	}

	/**
	 * {@code io.protostuff.Tag} declares no {@code @Target}, so on a constructor {@code val}/{@code var} without a
	 * use-site target Kotlin applies it to the parameter (as it does with {@code @param:}).
	 */
	@Override public boolean isTagOnWrongTarget()
	{
		KtAnnotationEntry tag = getTagAnnotation();
		if ( tag == null || !( declaration instanceof KtParameter ) )
		{
			return false;
		}
		KtAnnotationUseSiteTarget target = tag.getUseSiteTarget();
		return target == null || "param".equals( target.getText() );
	}

	@Override public @NotNull PsiElement getDeclaration()
	{
		return declaration;
	}

	@Override public @NotNull PsiElement getHighlightElement()
	{
		PsiElement id = declaration.getNameIdentifier();
		return id != null ? id : declaration;
	}

	@Nullable
	private KtAnnotationEntry findAnnotation( String fqn, String shortName )
	{
		for ( KtAnnotationEntry entry : declaration.getAnnotationEntries() )
		{
			if ( owner.refersTo( entry, fqn, shortName ) )
			{
				return entry;
			}
		}
		return null;
	}

	/** Decimal, hex or binary literal with optional underscores and sign. Anything else (constants) is unknown. */
	@Nullable
	static Integer parseIntLiteral( String text )
	{
		String s = text.replace( "_", "" ).trim();
		try
		{
			boolean negative = s.startsWith( "-" );
			if ( negative || s.startsWith( "+" ) )
			{
				s = s.substring( 1 );
			}
			long value;
			if ( s.startsWith( "0x" ) || s.startsWith( "0X" ) )
			{
				value = Long.parseLong( s.substring( 2 ), 16 );
			}
			else if ( s.startsWith( "0b" ) || s.startsWith( "0B" ) )
			{
				value = Long.parseLong( s.substring( 2 ), 2 );
			}
			else
			{
				value = Long.parseLong( s );
			}
			value = negative ? -value : value;
			return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? (int) value : null;
		}
		catch ( NumberFormatException e )
		{
			return null;
		}
	}

	/** Models are recreated on every {@code getFields()} call, so identity is the underlying declaration. */
	@Override public boolean equals( Object o )
	{
		return o instanceof KotlinTagField other && declaration.equals( other.declaration );
	}

	@Override public int hashCode()
	{
		return declaration.hashCode();
	}
}
