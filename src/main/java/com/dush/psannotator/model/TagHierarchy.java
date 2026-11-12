package com.dush.psannotator.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class with the editable superclasses and subclasses an operation can be widened to. Subclasses are found with
 * the inheritor search (needs indexes, so build it outside dumb mode and inside a read action).
 */
public final class TagHierarchy
{
	/** Which classes, besides the one at the caret, an operation covers. */
	public enum Scope
	{
		THIS_CLASS( "This class only" ),
		WITH_SUBCLASSES( "This class and its subclasses" ),
		WITH_SUPERCLASSES( "This class and its superclasses" ),
		WHOLE_HIERARCHY( "This class, its superclasses and subclasses" );

		private final String label;

		Scope( String label )
		{
			this.label = label;
		}

		public @NotNull String getLabel()
		{
			return label;
		}

		boolean includesSuperclasses()
		{
			return this == WITH_SUPERCLASSES || this == WHOLE_HIERARCHY;
		}

		boolean includesSubclasses()
		{
			return this == WITH_SUBCLASSES || this == WHOLE_HIERARCHY;
		}
	}

	private final TagClass target;
	private final List<TagClass> superclasses;
	private final List<TagClass> subclasses;

	private TagHierarchy( TagClass target, List<TagClass> superclasses, List<TagClass> subclasses )
	{
		this.target = target;
		this.superclasses = superclasses;
		this.subclasses = subclasses;
	}

	/** Superclasses in the project (top-most first) and all subclasses in the project, at any depth. */
	@NotNull
	public static TagHierarchy of( @NotNull TagClass target )
	{
		List<TagClass> supers = new ArrayList<>();
		for ( TagClass cls : TagPlanner.hierarchyTopDown( target ) )
		{
			if ( !cls.sameAs( target ) && cls.isWritable() )
			{
				supers.add( cls );
			}
		}

		List<TagClass> subs = new ArrayList<>();
		PsiClass psiClass = target.getPsiClass();
		if ( psiClass != null )
		{
			Collection<PsiClass> inheritors = ClassInheritorsSearch
					.search( psiClass, GlobalSearchScope.projectScope( psiClass.getProject() ), true )
					.findAll();
			for ( PsiClass inheritor : inheritors )
			{
				TagClass sub = TagModels.forClass( inheritor );
				if ( sub != null && sub.isWritable() && !sub.sameAs( target )
						&& subs.stream().noneMatch( s -> s.sameAs( sub ) ) )
				{
					subs.add( sub );
				}
			}
		}
		return new TagHierarchy( target, supers, TagOperations.sortSuperclassesFirst( subs ) );
	}

	public @NotNull TagClass getTarget()
	{
		return target;
	}

	public @NotNull List<TagClass> getSuperclasses()
	{
		return superclasses;
	}

	public @NotNull List<TagClass> getSubclasses()
	{
		return subclasses;
	}

	/** The classes besides the target that {@code scope} covers, superclasses first. */
	@NotNull
	public List<TagClass> others( @NotNull Scope scope )
	{
		List<TagClass> result = new ArrayList<>();
		if ( scope.includesSuperclasses() )
		{
			result.addAll( superclasses );
		}
		if ( scope.includesSubclasses() )
		{
			result.addAll( subclasses );
		}
		return result;
	}

	/** The target plus {@link #others}, superclasses before subclasses so numbering sees parents' tags. */
	@NotNull
	public List<TagClass> classes( @NotNull Scope scope )
	{
		List<TagClass> result = new ArrayList<>();
		if ( scope.includesSuperclasses() )
		{
			result.addAll( superclasses );
		}
		result.add( target );
		if ( scope.includesSubclasses() )
		{
			result.addAll( subclasses );
		}
		return result;
	}

	/** Whether {@code scope} adds any class beyond the target. */
	public boolean adds( @NotNull Scope scope )
	{
		return !others( scope ).isEmpty();
	}
}
