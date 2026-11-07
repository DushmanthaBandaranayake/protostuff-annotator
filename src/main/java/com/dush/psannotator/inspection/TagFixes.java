package com.dush.psannotator.inspection;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagOperations;
import com.dush.psannotator.model.TagPlanner;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/** Quick fixes for {@link ProtostuffTagInspection}. They find the model again from the problem's element. */
final class TagFixes
{
	private TagFixes()
	{
	}

	static final class TagRemainingFieldsFix implements LocalQuickFix
	{
		@Override public @NotNull String getFamilyName()
		{
			return "Tag the remaining fields";
		}

		@Override public void applyFix( @NotNull Project project, @NotNull ProblemDescriptor descriptor )
		{
			TagClass cls = TagModels.enclosingClass( descriptor.getPsiElement() );
			if ( cls != null )
			{
				TagOperations.tagRemaining( cls );
			}
		}
	}

	/** Also edits superclasses, which may be in other files, so it has no preview. */
	static final class TagInheritedFieldsFix implements LocalQuickFix
	{
		@Override public @NotNull String getFamilyName()
		{
			return "Tag the remaining fields here and in superclasses";
		}

		@Override public @NotNull IntentionPreviewInfo generatePreview( @NotNull Project project,
				@NotNull ProblemDescriptor previewDescriptor )
		{
			return IntentionPreviewInfo.EMPTY;
		}

		@Override public void applyFix( @NotNull Project project, @NotNull ProblemDescriptor descriptor )
		{
			TagClass cls = TagModels.enclosingClass( descriptor.getPsiElement() );
			if ( cls != null )
			{
				TagOperations.tagRemainingInHierarchy( cls );
			}
		}
	}

	static final class UseNextFreeNumberFix implements LocalQuickFix
	{
		@Override public @NotNull String getFamilyName()
		{
			return "Use the next free tag number";
		}

		@Override public void applyFix( @NotNull Project project, @NotNull ProblemDescriptor descriptor )
		{
			PsiElement element = descriptor.getPsiElement();
			TagClass cls = TagModels.enclosingClass( element );
			TagField field = TagModels.fieldAt( element );
			if ( cls != null && field != null && field.hasTag() )
			{
				cls.setTag( field, TagPlanner.nextAfterMax( cls ) );
			}
		}
	}

	static final class RemoveTagFix implements LocalQuickFix
	{
		@Override public @NotNull String getFamilyName()
		{
			return "Remove @Tag";
		}

		@Override public void applyFix( @NotNull Project project, @NotNull ProblemDescriptor descriptor )
		{
			PsiElement element = descriptor.getPsiElement();
			TagClass cls = TagModels.enclosingClass( element );
			TagField field = TagModels.fieldAt( element );
			if ( cls != null && field != null && field.hasTag() )
			{
				cls.removeTag( field );
				cls.removeUnusedImport();
			}
		}
	}
}
