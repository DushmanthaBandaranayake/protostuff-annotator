package com.dush.psannotator;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ActionStubBase;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ActionMenuTest extends TagTestCase
{
	private static final String GENERATE = "com.dush.psannotator.GenerateProtostuffTags";
	private static final String RENUMBER = "com.dush.psannotator.RenumberProtostuffTags";
	private static final String REMOVE = "com.dush.psannotator.RemoveProtostuffTags";
	private static final String MENU = "com.dush.psannotator.ProtostuffMenu";

	public void testGenerateDropdownHasAllThreeInOrder()
	{
		List<String> children = childIds( "GenerateGroup" );
		int generate = children.indexOf( GENERATE );
		int renumber = children.indexOf( RENUMBER );
		int remove = children.indexOf( REMOVE );
		assertTrue( "Generate missing from GenerateGroup", generate >= 0 );
		assertEquals( generate + 1, renumber );
		assertEquals( generate + 2, remove );
	}

	public void testProtostuffMenuInEditorProjectViewAndCodeMenus()
	{
		for ( String group : new String[] { "EditorPopupMenu", "ProjectViewPopupMenu", "CodeMenu" } )
		{
			assertTrue( "Protostuff menu missing from " + group, childIds( group ).contains( MENU ) );
		}
	}

	public void testRenumberAndRemoveOnlyShownWhenClassHasTags()
	{
		addProtostuff();
		myFixture.configureByText( "A.java", "class A { int <caret>a; }" );
		assertTrue( visible( GENERATE ) );
		assertFalse( visible( RENUMBER ) );
		assertFalse( visible( REMOVE ) );

		myFixture.configureByText( "B.java", "import io.protostuff.Tag;\nclass B { @Tag(1) int <caret>a; int b; }" );
		assertTrue( visible( GENERATE ) );
		assertTrue( visible( RENUMBER ) );
		assertTrue( visible( REMOVE ) );
	}

	public void testHiddenOutsideAClass()
	{
		myFixture.configureByText( "C.java", "<caret>\nclass C { int a; }" );
		assertFalse( visible( GENERATE ) );
		assertFalse( visible( RENUMBER ) );
		assertFalse( visible( REMOVE ) );
	}

	/** Ids of a group's children. Children may be lazy stubs, so compare by id rather than by instance. */
	private static List<String> childIds( String groupId )
	{
		ActionManager am = ActionManager.getInstance();
		DefaultActionGroup group = (DefaultActionGroup) am.getAction( groupId );
		return Arrays.stream( group.getChildActionsOrStubs() )
				.map( a -> a instanceof ActionStubBase ? ( (ActionStubBase) a ).getId() : am.getId( a ) )
				.collect( Collectors.toList() );
	}

	private boolean visible( String actionId )
	{
		AnAction action = ActionManager.getInstance().getAction( actionId );
		DataContext context = SimpleDataContext.builder()
				.add( CommonDataKeys.PROJECT, getProject() )
				.add( CommonDataKeys.EDITOR, myFixture.getEditor() )
				.add( CommonDataKeys.PSI_FILE, myFixture.getFile() )
				.build();
		AnActionEvent event = AnActionEvent.createFromAnAction( action, null, ActionPlaces.EDITOR_POPUP, context );
		action.update( event );
		Presentation presentation = event.getPresentation();
		return presentation.isEnabledAndVisible();
	}
}
