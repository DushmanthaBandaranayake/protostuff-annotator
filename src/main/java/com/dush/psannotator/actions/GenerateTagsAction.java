package com.dush.psannotator.actions;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagOperations;
import com.dush.psannotator.model.TagPlanner;
import com.dush.psannotator.ui.BulkGenerateDialog;
import com.dush.psannotator.ui.GenerateDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GenerateTagsAction extends TagActionBase
{
	@Override protected String title()
	{
		return "Generate Protostuff Tags";
	}

	@Override protected void runOnClass( @NotNull Project project, @NotNull TagClass cls )
	{
		if ( !GenerateDialog.hasFieldsToTag( cls ) )
		{
			Messages.showInfoMessage( project, "Every field in " + cls.getName() + " that can be tagged already has a @Tag.",
					title() );
			return;
		}
		GenerateDialog dialog = new GenerateDialog( cls );
		if ( !dialog.showAndGet() )
		{
			return;
		}
		Set<PsiElement> selected = dialog.getSelected();
		write( project, List.of( cls ), () -> TagOperations.generate( cls, dialog.getStart(), dialog.isIncludeTransient(),
				f -> selected.contains( f.getDeclaration() ) ) );
	}

	@Override protected void runOnClasses( @NotNull Project project, @NotNull List<TagClass> classes )
	{
		BulkGenerateDialog dialog = new BulkGenerateDialog( project, classes.size() );
		if ( !dialog.showAndGet() )
		{
			return;
		}
		List<TagClass> targets = new ArrayList<>();
		for ( TagClass cls : classes )
		{
			if ( !TagPlanner.untaggedFields( cls, dialog.isIncludeTransient() ).isEmpty()
					&& ( !dialog.isOnlyTaggedClasses() || TagOperations.hasAnyTag( cls ) ) )
			{
				targets.add( cls );
			}
		}
		if ( targets.isEmpty() )
		{
			Messages.showInfoMessage( project, "Nothing to tag in the selected classes.", title() );
			return;
		}
		Integer start = dialog.getStart();
		int[] classCount = { 0 };
		int fields = write( project, targets, () -> {
			int total = 0;
			for ( TagClass cls : TagOperations.sortSuperclassesFirst( targets ) )
			{
				int from = start != null ? start : TagPlanner.nextAfterMax( cls );
				int added = TagOperations.generate( cls, from, dialog.isIncludeTransient(), null );
				total += added;
				classCount[0] += added > 0 ? 1 : 0;
			}
			return total;
		} );
		Messages.showInfoMessage( project, "Tagged " + TagTargets.plural( fields, "field" ) + " in "
				+ TagTargets.plural( classCount[0], "class" ) + ".", title() );
	}
}
