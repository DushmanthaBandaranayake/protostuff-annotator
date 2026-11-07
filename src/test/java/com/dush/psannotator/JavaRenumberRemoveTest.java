package com.dush.psannotator;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagOperations;
import com.intellij.openapi.command.WriteCommandAction;

import java.util.List;

public class JavaRenumberRemoveTest extends TagTestCase
{
	public void testRenumberInDeclarationOrder()
	{
		addProtostuff();
		edit( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(9) int a; int u; @Tag(4) int b; }",
				c -> assertEquals( 2, TagOperations.renumber( c, 1 ) ) );
		check( "import io.protostuff.Tag;\nclass A { @Tag(1) int a; int u; @Tag(2) int b; }" );
	}

	public void testRenumberKeepsQualifiedName()
	{
		addProtostuff();
		edit( "A.java", "class A { @io.protostuff.Tag(5) int a; }", c -> TagOperations.renumber( c, 1 ) );
		check( "class A { @io.protostuff.Tag(1) int a; }" );
	}

	public void testRenumberSkipsSuperclassNumbers()
	{
		addProtostuff();
		myFixture.addClass( "import io.protostuff.Tag;\nclass Base { @Tag(1) int x; int untagged; }" );
		edit( "A.java", "import io.protostuff.Tag;\nclass A extends Base { @Tag(10) int a; @Tag(11) int b; }",
				c -> TagOperations.renumber( c, 1 ) );
		// 1 is Base.x, 2 is reserved by Base.untagged.
		check( "import io.protostuff.Tag;\nclass A extends Base { @Tag(3) int a; @Tag(4) int b; }" );
	}

	public void testRenumberSuperclassFirstAcrossClasses()
	{
		addProtostuff();
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\n"
				+ "class Sub extends Base { @Tag(1) int s; }\n"
				+ "class Base { @Tag(5) int b; }" );
		List<TagClass> classes = TagModels.classesInFile( myFixture.getFile() );
		WriteCommandAction.runWriteCommandAction( getProject(), () -> {
			for ( TagClass cls : TagOperations.sortSuperclassesFirst( classes ) )
			{
				TagOperations.renumber( cls, 1 );
			}
		} );
		check( "import io.protostuff.Tag;\n"
				+ "class Sub extends Base { @Tag(2) int s; }\n"
				+ "class Base { @Tag(1) int b; }" );
	}

	public void testRemoveAllAndUnusedImport()
	{
		addProtostuff();
		edit( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(2) int b; }",
				c -> assertEquals( 2, TagOperations.removeAll( c ) ) );
		check( "class A { int a; int b; }" );
	}

	public void testRemoveKeepsImportUsedElsewhereInFile()
	{
		addProtostuff();
		edit( "A.java", "import io.protostuff.Tag;\nclass <caret>A { @Tag(1) int a; }\nclass B { @Tag(1) int b; }",
				TagOperations::removeAll );
		check( "import io.protostuff.Tag;\nclass A { int a; }\nclass B { @Tag(1) int b; }" );
	}
}
