package com.dush.psannotator.actions;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagOperations;
import com.dush.psannotator.ui.RenumberDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RenumberTagsAction extends TagActionBase
{
	@Override protected String title()
	{
		return "Renumber Protostuff Tags";
	}

	@Override protected void runOnClass( @NotNull Project project, @NotNull TagClass cls )
	{
		run( project, List.of( cls ), cls.getName() );
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
			Messages.showInfoMessage( project, "There are no @Tag annotations to renumber.", title() );
			return;
		}
		RenumberDialog dialog = new RenumberDialog( project, what );
		if ( !dialog.showAndGet() )
		{
			return;
		}
		int changed = write( project, classes, () -> {
			int total = 0;
			for ( TagClass cls : TagOperations.sortSuperclassesFirst( classes ) )
			{
				total += TagOperations.renumber( cls, dialog.getStart() );
			}
			return total;
		} );
		if ( classes.size() > 1 || changed == 0 )
		{
			Messages.showInfoMessage( project, changed == 0 ? "The tags already have those numbers."
					: "Changed " + TagTargets.plural( changed, "tag" ) + ".", title() );
		}
	}
}
