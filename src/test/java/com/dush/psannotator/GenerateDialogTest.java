package com.dush.psannotator;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.ui.GenerateDialog;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;

import java.util.Set;
import java.util.stream.Collectors;

public class GenerateDialogTest extends TagTestCase
{
	public void testDefaultsContinueAfterHighestAndSkipTransient()
	{
		addProtostuff();
		TagSettings.setIncludeTransient( false );
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(4) int a; int b; transient int t; static int s; }" );
		TagClass cls = classAtCaretOrFirst();
		GenerateDialog dialog = new GenerateDialog( cls );
		try
		{
			assertEquals( 5, dialog.getStart() );
			assertFalse( dialog.isIncludeTransient() );
			assertEquals( Set.of( "b" ), names( dialog.getSelected() ) );
		}
		finally
		{
			Disposer.dispose( dialog.getDisposable() );
		}
	}

	public void testRemembersTransientChoice()
	{
		TagSettings.setIncludeTransient( true );
		try
		{
			myFixture.configureByText( "A.java", "class A { int b; transient int t; }" );
			GenerateDialog dialog = new GenerateDialog( classAtCaretOrFirst() );
			try
			{
				assertEquals( 1, dialog.getStart() );
				assertEquals( Set.of( "b", "t" ), names( dialog.getSelected() ) );
			}
			finally
			{
				Disposer.dispose( dialog.getDisposable() );
			}
		}
		finally
		{
			TagSettings.setIncludeTransient( false );
		}
	}

	private static Set<String> names( Set<PsiElement> elements )
	{
		return elements.stream().map( e -> ( (com.intellij.psi.PsiField) e ).getName() ).collect( Collectors.toSet() );
	}
}
