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
		GenerateDialog dialog = new GenerateDialog( com.dush.psannotator.model.TagHierarchy.of( cls ) );
		try
		{
			assertEquals( 5, dialog.getStart() );
			assertFalse( dialog.isIncludeTransient() );
			assertEquals( Set.of( "b" ), names( dialog.getSelected() ) );
			// The Tag column shows the planned number for each row (b is row 0, t is row 1).
			assertEquals( java.util.Map.of( "b", 5 ), dialog.plannedTagsByName() );
			assertEquals( "5", dialog.tagColumnText( 0 ) );
			assertEquals( "", dialog.tagColumnText( 1 ) );
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
			GenerateDialog dialog = new GenerateDialog( com.dush.psannotator.model.TagHierarchy.of( classAtCaretOrFirst() ) );
			try
			{
				assertEquals( 1, dialog.getStart() );
				assertEquals( Set.of( "b", "t" ), names( dialog.getSelected() ) );
				assertEquals( java.util.Map.of( "b", 1, "t", 2 ), dialog.plannedTagsByName() );
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
