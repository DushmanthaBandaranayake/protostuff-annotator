package com.dush.psannotator;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagHierarchy;
import com.dush.psannotator.model.TagHierarchy.Scope;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagOperations;
import com.dush.psannotator.ui.GenerateDialog;
import com.dush.psannotator.ui.RemoveDialog;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiClass;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base <- Mid <- Leaf, plus a sibling Other <- Base and an Unrelated class. Operations start from Mid.
 */
public class HierarchyScopeTest extends TagTestCase
{
	@Override protected void setUp() throws Exception
	{
		super.setUp();
		addProtostuff();
		myFixture.addFileToProject( "Base.java", "class Base { int b; }" );
		myFixture.addFileToProject( "Mid.java", "class Mid extends Base { int m; }" );
		myFixture.addFileToProject( "Leaf.java", "class Leaf extends Mid { int l; }" );
		myFixture.addFileToProject( "Other.java", "class Other extends Base { int o; }" );
		myFixture.addFileToProject( "Unrelated.java", "class Unrelated { int u; }" );
	}

	public void testFindsSuperclassesAndSubclasses()
	{
		TagHierarchy mid = TagHierarchy.of( cls( "Mid" ) );
		assertEquals( List.of( "Base" ), names( mid.getSuperclasses() ) );
		assertEquals( List.of( "Leaf" ), names( mid.getSubclasses() ) );

		TagHierarchy base = TagHierarchy.of( cls( "Base" ) );
		assertEmpty( base.getSuperclasses() );
		List<String> subs = names( base.getSubclasses() );
		assertSameElements( subs, "Mid", "Other", "Leaf" );
		assertTrue( "subclasses come before their own subclasses", subs.indexOf( "Mid" ) < subs.indexOf( "Leaf" ) );

		assertEquals( List.of( "Mid" ), names( mid.classes( Scope.THIS_CLASS ) ) );
		assertEquals( List.of( "Mid", "Leaf" ), names( mid.classes( Scope.WITH_SUBCLASSES ) ) );
		assertEquals( List.of( "Base", "Mid" ), names( mid.classes( Scope.WITH_SUPERCLASSES ) ) );
		assertEquals( List.of( "Base", "Mid", "Leaf" ), names( mid.classes( Scope.WHOLE_HIERARCHY ) ) );
	}

	public void testGenerateWithSubclasses()
	{
		generate( Scope.WITH_SUBCLASSES );
		// Base stays untagged but its field still reserves 1 for the classes below it.
		assertEquals( "class Base { int b; }", text( "Base" ) );
		assertTrue( text( "Mid" ), text( "Mid" ).contains( "@Tag(2)\nint m;" ) );
		assertTrue( text( "Leaf" ), text( "Leaf" ).contains( "@Tag(3)\nint l;" ) );
		assertEquals( "class Other extends Base { int o; }", text( "Other" ) );
	}

	public void testGenerateWithSuperclasses()
	{
		generate( Scope.WITH_SUPERCLASSES );
		assertTrue( text( "Base" ), text( "Base" ).contains( "@Tag(1)\nint b;" ) );
		assertTrue( text( "Mid" ), text( "Mid" ).contains( "@Tag(2)\nint m;" ) );
		assertEquals( "class Leaf extends Mid { int l; }", text( "Leaf" ) );
	}

	public void testGenerateWholeHierarchyLeavesSiblingsAndUnrelatedAlone()
	{
		generate( Scope.WHOLE_HIERARCHY );
		assertTrue( text( "Base" ).contains( "@Tag(1)\nint b;" ) );
		assertTrue( text( "Mid" ).contains( "@Tag(2)\nint m;" ) );
		assertTrue( text( "Leaf" ).contains( "@Tag(3)\nint l;" ) );
		assertEquals( "class Other extends Base { int o; }", text( "Other" ) );
		assertEquals( "class Unrelated { int u; }", text( "Unrelated" ) );
	}

	public void testRemoveOnlyThisClassOrWholeHierarchy()
	{
		generate( Scope.WHOLE_HIERARCHY );
		TagHierarchy mid = TagHierarchy.of( cls( "Mid" ) );
		WriteCommandAction.runWriteCommandAction( getProject(), () -> {
			TagOperations.removeInScope( mid, Scope.THIS_CLASS );
		} );
		assertFalse( text( "Mid" ).contains( "@Tag" ) );
		assertTrue( text( "Base" ).contains( "@Tag(1)" ) );
		assertTrue( text( "Leaf" ).contains( "@Tag(3)" ) );

		WriteCommandAction.runWriteCommandAction( getProject(), () -> {
			int[] counts = TagOperations.removeInScope( TagHierarchy.of( cls( "Mid" ) ), Scope.WHOLE_HIERARCHY );
			assertEquals( 2, counts[0] ); // Base.b and Leaf.l; Mid had none left
			assertEquals( 2, counts[1] );
		} );
		for ( String name : new String[] { "Base", "Mid", "Leaf" } )
		{
			assertFalse( name, text( name ).contains( "Tag" ) ); // tags and the now-unused import are gone
		}
	}

	public void testGenerateDialogScopes()
	{
		GenerateDialog dialog = new GenerateDialog( TagHierarchy.of( cls( "Mid" ) ) );
		try
		{
			dialog.selectScope( Scope.THIS_CLASS );
			assertEmpty( dialog.otherClassesToTag() );
			dialog.selectScope( Scope.WHOLE_HIERARCHY );
			assertEquals( Scope.WHOLE_HIERARCHY, dialog.getScope() );
			assertEquals( List.of( "Base", "Leaf" ), names( dialog.otherClassesToTag() ) );
		}
		finally
		{
			Disposer.dispose( dialog.getDisposable() );
		}
	}

	public void testScopesWithoutRelatedClassesCantBeChosen()
	{
		GenerateDialog dialog = new GenerateDialog( TagHierarchy.of( cls( "Unrelated" ) ) );
		try
		{
			dialog.selectScope( Scope.WHOLE_HIERARCHY ); // disabled, so the selection stays put
			assertEquals( Scope.THIS_CLASS, dialog.getScope() );
		}
		finally
		{
			Disposer.dispose( dialog.getDisposable() );
		}
	}

	public void testRemoveDialogDefaultsToThisClass()
	{
		generate( Scope.WHOLE_HIERARCHY );
		RemoveDialog dialog = new RemoveDialog( TagHierarchy.of( cls( "Mid" ) ) );
		try
		{
			assertEquals( Scope.THIS_CLASS, dialog.getScope() );
		}
		finally
		{
			Disposer.dispose( dialog.getDisposable() );
		}
	}

	public void testFindsKotlinSubclasses()
	{
		myFixture.addFileToProject( "K.kt", "class K(val k: Int) : Base()\nopen class KBase(val a: Int)\nclass KSub(val s: Int) : KBase(1)\n" );
		assertTrue( names( TagHierarchy.of( cls( "Base" ) ).getSubclasses() ).contains( "K" ) );
		assertEquals( List.of( "KSub" ), names( TagHierarchy.of( cls( "KBase" ) ).getSubclasses() ) );
	}

	private void generate( Scope scope )
	{
		TagHierarchy mid = TagHierarchy.of( cls( "Mid" ) );
		WriteCommandAction.runWriteCommandAction( getProject(), () -> {
			TagOperations.generateInScope( mid, scope, 1, false, null );
		} );
	}

	private TagClass cls( String name )
	{
		PsiClass psiClass = myFixture.findClass( name );
		TagClass model = TagModels.forClass( psiClass );
		assertNotNull( name, model );
		return model;
	}

	private String text( String name )
	{
		com.intellij.psi.PsiDocumentManager.getInstance( getProject() ).commitAllDocuments();
		return myFixture.findClass( name ).getContainingFile().getText();
	}

	private static List<String> names( List<TagClass> classes )
	{
		return classes.stream().map( TagClass::getName ).collect( Collectors.toList() );
	}
}
