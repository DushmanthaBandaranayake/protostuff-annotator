package com.dush.psannotator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * @author dushmantha
 * Date 2018/1/16
 */
public class GenerateAction extends AnAction
{

	@Override public void actionPerformed( AnActionEvent e )
	{
		PsiClass psiClass = getPsiClassFromContext( e );

		if ( psiClass != null )
		{
			GenerateDialog dialog = new GenerateDialog( psiClass );
			dialog.show();
			if ( dialog.isOK() )
			{
				generateTags( psiClass, dialog.getFields(), dialog.getStartingValue(), dialog.isStaticFieldsEnabled(), dialog.isTransientFieldsEnabled() );
			}
		}

	}

	private void generateTags( PsiClass psiClass, List<PsiField> fields, int startTagNum, boolean staticFieldsEnabled, boolean transientFieldsEnabled )
	{
		if ( startTagNum < 0 || psiClass == null )
		{
			return;
		}
		new WriteCommandAction.Simple( psiClass.getContainingFile().getProject(), psiClass.getContainingFile() )
		{
			@Override protected void run()
			{

				PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory( getProject() );
				int index = startTagNum;
				for ( PsiField field : fields )
				{
					PsiModifierList modifierList = field.getModifierList();
					if ( field.hasModifierProperty( PsiModifier.STATIC ) && !staticFieldsEnabled )
					{

						continue;
					}
					if ( field.hasModifierProperty( PsiModifier.TRANSIENT ) && !transientFieldsEnabled )
					{
						continue;
					}
					if ( field.getContainingClass() != null && !( field.getContainingClass().equals( psiClass ) ) )
					{
						index++;
						continue;
					}
					PsiAnnotation annotationFromText = elementFactory.createAnnotationFromText( "@Tag(" + index++ + ")", psiClass );
					field.addBefore( annotationFromText, modifierList );
				}

			}
		}.execute();
	}

	@Override public void update( AnActionEvent e )
	{
		PsiClass psiClass = getPsiClassFromContext( e );
		e.getPresentation().setEnabled( psiClass != null );
	}

	private PsiClass getPsiClassFromContext( AnActionEvent e )
	{
		PsiFile psiFile = e.getData( LangDataKeys.PSI_FILE );
		Editor editor = e.getData( PlatformDataKeys.EDITOR );

		if ( psiFile == null || editor == null )
		{
			return null;
		}
		int offset = editor.getCaretModel().getOffset();
		PsiElement elementAt = psiFile.findElementAt( offset );
		return PsiTreeUtil.getParentOfType( elementAt, PsiClass.class );
	}
}
