package com.dush.psannotator.inspection;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagModels;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

/** Reports {@code @Tag} mistakes in Java and Kotlin classes; see {@link TagProblems}. */
public class ProtostuffTagInspection extends LocalInspectionTool
{
	@Override public @NotNull PsiElementVisitor buildVisitor( @NotNull ProblemsHolder holder, boolean isOnTheFly )
	{
		return new PsiElementVisitor()
		{
			@Override public void visitElement( @NotNull PsiElement element )
			{
				TagClass cls = TagModels.forClass( element );
				if ( cls == null || !cls.isWritable() )
				{
					return;
				}
				for ( TagProblems.Problem problem : TagProblems.check( cls ) )
				{
					holder.registerProblem( problem.element(), problem.message(), highlight( problem.kind() ),
							fixes( problem.kind() ) );
				}
			}
		};
	}

	private static ProblemHighlightType highlight( TagProblems.Kind kind )
	{
		return kind == TagProblems.Kind.IGNORED ? ProblemHighlightType.WEAK_WARNING
				: ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
	}

	private static LocalQuickFix[] fixes( TagProblems.Kind kind )
	{
		return switch ( kind )
		{
			case MISSING_TAG -> new LocalQuickFix[] { new TagFixes.TagRemainingFieldsFix() };
			case MISSING_TAG_INHERITED -> new LocalQuickFix[] { new TagFixes.TagInheritedFieldsFix() };
			case DUPLICATE, OUT_OF_RANGE -> new LocalQuickFix[] { new TagFixes.UseNextFreeNumberFix() };
			case IGNORED -> new LocalQuickFix[] { new TagFixes.RemoveTagFix() };
		};
	}
}
