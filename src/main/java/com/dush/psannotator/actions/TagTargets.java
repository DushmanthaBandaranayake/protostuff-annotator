package com.dush.psannotator.actions;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagModels;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What an action works on: the class at the caret in an editor, or every taggable class in the files, folders and
 * packages selected in the Project view.
 */
final class TagTargets
{
	private static final Set<String> EXTENSIONS = Set.of( "java", "kt" );

	private TagTargets()
	{
	}

	/** The class at the caret, or {@code null} when not in an editor. */
	@Nullable
	static TagClass classAtCaret( @NotNull AnActionEvent e )
	{
		Editor editor = e.getData( CommonDataKeys.EDITOR );
		PsiFile file = e.getData( CommonDataKeys.PSI_FILE );
		if ( editor == null || file == null )
		{
			return null;
		}
		PsiElement element = file.findElementAt( editor.getCaretModel().getOffset() );
		TagClass cls = TagModels.enclosingClass( element );
		return cls != null && cls.isWritable() ? cls : null;
	}

	/** Cheap check for update(): is anything in the selection possibly a source file? */
	static boolean selectionMayContainClasses( @NotNull AnActionEvent e )
	{
		VirtualFile[] files = e.getData( CommonDataKeys.VIRTUAL_FILE_ARRAY );
		if ( files == null || e.getProject() == null )
		{
			return false;
		}
		ProjectFileIndex index = ProjectFileIndex.getInstance( e.getProject() );
		for ( VirtualFile file : files )
		{
			if ( index.isInSourceContent( file ) && ( file.isDirectory() || EXTENSIONS.contains( file.getExtension() ) ) )
			{
				return true;
			}
		}
		return false;
	}

	/** All writable taggable classes in the selection, searched with a cancellable progress dialog. */
	@NotNull
	static List<TagClass> classesInSelection( @NotNull Project project, @NotNull VirtualFile[] roots )
	{
		List<TagClass> result = new ArrayList<>();
		boolean finished = ProgressManager.getInstance().runProcessWithProgressSynchronously( () -> {
			Set<VirtualFile> files = new LinkedHashSet<>();
			ProjectFileIndex index = ProjectFileIndex.getInstance( project );
			for ( VirtualFile root : roots )
			{
				VfsUtilCore.visitChildrenRecursively( root, new VirtualFileVisitor<Void>()
				{
					@Override public boolean visitFile( @NotNull VirtualFile file )
					{
						ProgressManager.checkCanceled();
						if ( !ReadAction.compute( () -> index.isInSourceContent( file ) ) )
						{
							return file.isDirectory() && ReadAction.compute( () -> index.isInContent( file ) );
						}
						if ( !file.isDirectory() && EXTENSIONS.contains( file.getExtension() ) )
						{
							files.add( file );
						}
						return true;
					}
				} );
			}
			for ( VirtualFile file : files )
			{
				ProgressManager.checkCanceled();
				ReadAction.run( () -> {
					PsiFile psiFile = PsiManager.getInstance( project ).findFile( file );
					if ( psiFile != null )
					{
						for ( TagClass cls : TagModels.classesInFile( psiFile ) )
						{
							if ( cls.isWritable() )
							{
								result.add( cls );
							}
						}
					}
				} );
			}
		}, "Finding Classes", true, project );
		return finished ? result : List.of();
	}

	@NotNull
	static PsiFile[] files( @NotNull List<TagClass> classes )
	{
		Set<PsiFile> files = new LinkedHashSet<>();
		for ( TagClass cls : classes )
		{
			files.add( cls.getDeclaration().getContainingFile() );
		}
		return files.toArray( PsiFile.EMPTY_ARRAY );
	}

	static String plural( int count, String noun )
	{
		if ( count == 1 )
		{
			return count + " " + noun;
		}
		return count + " " + noun + ( noun.endsWith( "s" ) ? "es" : "s" );
	}
}
