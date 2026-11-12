package com.dush.psannotator.actions;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagHierarchy;
import com.dush.psannotator.model.TagOperations;
import com.dush.psannotator.ui.RemoveDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RemoveTagsAction extends TagActionBase
{
	@Override protected String title()
	{
		return "Remove Protostuff Tags";
	}

	/** Only offered in the editor when the class already has a @Tag. */
	@Override protected boolean isApplicableTo( @NotNull TagClass cls )
	{
		return TagOperations.hasAnyTag( cls );
	}

	@Override protected void runOnClass( @NotNull Project project, @NotNull TagClass cls )
	{
		TagHierarchy hierarchy = TagTargets.hierarchy( project, cls );
		if ( hierarchy == null )
		{
			return; // cancelled
		}
		RemoveDialog dialog = new RemoveDialog( hierarchy );
		if ( !dialog.showAndGet() )
		{
			return;
		}
		TagHierarchy.Scope scope = dialog.getScope();
		int[] result = new int[2];
		write( project, hierarchy.classes( scope ), () -> {
			int[] counts = TagOperations.removeInScope( hierarchy, scope );
			System.arraycopy( counts, 0, result, 0, 2 );
			return counts[0];
		} );
		if ( scope != TagHierarchy.Scope.THIS_CLASS )
		{
			Messages.showInfoMessage( project, "Removed " + TagTargets.plural( result[0], "tag" ) + " from "
					+ TagTargets.plural( result[1], "class" ) + ".", title() );
		}
	}

	@Override protected void runOnClasses( @NotNull Project project, @NotNull List<TagClass> classes )
	{
		List<TagClass> tagged = classes.stream().filter( TagOperations::hasAnyTag ).toList();
		run( project, tagged, TagTargets.plural( tagged.size(), "class" ) );
	}

	private void run( Project project, List<TagClass> classes, String what )
	{
		if ( classes.isEmpty() || classes.stream().noneMatch( TagOperations::hasAnyTag ) )
		{
			Messages.showInfoMessage( project, "There are no @Tag annotations to remove.", title() );
			return;
		}
		int answer = Messages.showOkCancelDialog( project,
				"Remove every @Tag from " + what + "? You can undo this.", title(), "Remove", Messages.getCancelButton(),
				Messages.getQuestionIcon() );
		if ( answer != Messages.OK )
		{
			return;
		}
		removeAll( project, classes );
	}

	private int removeAll( Project project, List<TagClass> classes )
	{
		return write( project, classes, () -> {
			int total = 0;
			for ( TagClass cls : classes )
			{
				total += TagOperations.removeAll( cls );
			}
			return total;
		} );
	}
}
