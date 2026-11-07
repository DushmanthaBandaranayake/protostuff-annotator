package com.dush.psannotator;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Code | Generate action that adds Protostuff {@code @Tag} annotations to the fields of the class at the caret.
 *
 * @author dushmantha
 * Date 2018/1/16
 */
public class GenerateAction extends AnAction
{
	@Override public void actionPerformed( @NotNull AnActionEvent e )
	{
		PsiClass psiClass = getPsiClassFromContext( e );
		if ( psiClass == null )
		{
			return;
		}

		GenerateDialog dialog = new GenerateDialog( psiClass );
		if ( !dialog.showAndGet() )
		{
			return;
		}

		boolean includeTransient = dialog.isTransientFieldsEnabled();
		if ( !TagGenerator.hasTaggableFields( psiClass, includeTransient ) )
		{
			Messages.showInfoMessage( psiClass.getProject(), "Every field in " + psiClass.getName()
					+ " that can be tagged already has a @Tag.", "Generate Protostuff Tags" );
			return;
		}

		WriteCommandAction.writeCommandAction( psiClass.getProject(), psiClass.getContainingFile() )
				.withName( "Generate Protostuff Tags" )
				.run( () -> TagGenerator.generate( psiClass, dialog.getStartingValue(), includeTransient ) );
	}

	@Override public void update( @NotNull AnActionEvent e )
	{
		e.getPresentation().setEnabledAndVisible( getPsiClassFromContext( e ) != null );
	}

	@Override public @NotNull ActionUpdateThread getActionUpdateThread()
	{
		return ActionUpdateThread.BGT;
	}

	@Nullable
	private static PsiClass getPsiClassFromContext( AnActionEvent e )
	{
		PsiFile psiFile = e.getData( CommonDataKeys.PSI_FILE );
		Editor editor = e.getData( CommonDataKeys.EDITOR );
		if ( !( psiFile instanceof PsiJavaFile ) || editor == null )
		{
			return null;
		}
		PsiElement elementAt = psiFile.findElementAt( editor.getCaretModel().getOffset() );
		PsiClass psiClass = PsiTreeUtil.getParentOfType( elementAt, PsiClass.class );
		if ( psiClass == null || psiClass.isInterface() || psiClass.isAnnotationType() )
		{
			return null;
		}
		return psiClass;
	}
}
