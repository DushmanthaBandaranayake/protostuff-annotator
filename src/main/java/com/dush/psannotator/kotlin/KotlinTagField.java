package com.dush.psannotator.kotlin;

import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagNames;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.ValueArgument;

import java.util.List;

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
		List<? extends ValueArgument> args = tag.getValueArguments();
		if ( args.isEmpty() )
		{
			return null;
		}
		KtExpression expression = args.get( 0 ).getArgumentExpression();
		return expression != null ? parseIntLiteral( expression.getText() ) : null;
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
}
