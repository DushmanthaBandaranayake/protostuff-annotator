package com.dush.psannotator.actions;

import com.dush.psannotator.model.TagClass;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.IntSupplier;

/** Runs on the class at the caret, or on the Project view selection. */
abstract class TagActionBase extends AnAction
{
	@Override public final void update( @NotNull AnActionEvent e )
	{
		boolean inEditor = e.getData( CommonDataKeys.EDITOR ) != null;
		boolean enabled = inEditor ? TagTargets.classAtCaret( e ) != null : TagTargets.selectionMayContainClasses( e );
		e.getPresentation().setEnabledAndVisible( enabled );
	}

	@Override public final @NotNull ActionUpdateThread getActionUpdateThread()
	{
		return ActionUpdateThread.BGT;
	}

	@Override public final void actionPerformed( @NotNull AnActionEvent e )
	{
		Project project = e.getProject();
		if ( project == null )
		{
			return;
		}
		TagClass atCaret = TagTargets.classAtCaret( e );
		if ( atCaret != null )
		{
			runOnClass( project, atCaret );
			return;
		}
		VirtualFile[] roots = e.getData( CommonDataKeys.VIRTUAL_FILE_ARRAY );
		if ( roots == null || roots.length == 0 )
		{
			return;
		}
		List<TagClass> classes = TagTargets.classesInSelection( project, roots );
		if ( classes.isEmpty() )
		{
			Messages.showInfoMessage( project, "No Java or Kotlin classes found in the selection.", title() );
			return;
		}
		runOnClasses( project, classes );
	}

	protected abstract String title();

	protected abstract void runOnClass( @NotNull Project project, @NotNull TagClass cls );

	protected abstract void runOnClasses( @NotNull Project project, @NotNull List<TagClass> classes );

	/** Runs {@code edit} as one undoable command over the classes' files. */
	protected final int write( Project project, List<TagClass> classes, IntSupplier edit )
	{
		int[] count = { 0 };
		WriteCommandAction.writeCommandAction( project, TagTargets.files( classes ) )
				.withName( title() )
				.run( () -> count[0] = edit.getAsInt() );
		return count[0];
	}
}
