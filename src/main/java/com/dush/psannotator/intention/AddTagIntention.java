package com.dush.psannotator.intention;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagPlanner;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Alt+Enter on a field: adds {@code @Tag} with one more than the highest number in the class hierarchy. It never
 * fills a gap, because a gap is often a removed field whose number shouldn't be reused.
 */
public class AddTagIntention extends PsiElementBaseIntentionAction
{
	@Override public @NotNull String getFamilyName()
	{
		return "Add Protostuff @Tag";
	}

	@Override public boolean isAvailable( @NotNull Project project, Editor editor, @NotNull PsiElement element )
	{
		TagField field = TagModels.fieldAt( element );
		TagClass cls = TagModels.enclosingClass( element );
		if ( field == null || cls == null || !canModify( element ) || field.hasTag()
				|| !TagPlanner.isTaggable( field, true ) )
		{
			return false;
		}
		setText( "Add @Tag(" + TagPlanner.nextAfterMax( cls ) + ")" );
		return true;
	}

	@Override public void invoke( @NotNull Project project, Editor editor, @NotNull PsiElement element )
	{
		TagField field = TagModels.fieldAt( element );
		TagClass cls = TagModels.enclosingClass( element );
		if ( field != null && cls != null && !field.hasTag() )
		{
			cls.addTag( field, TagPlanner.nextAfterMax( cls ) );
		}
	}
}
