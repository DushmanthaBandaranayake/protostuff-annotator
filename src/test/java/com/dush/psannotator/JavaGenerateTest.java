package com.dush.psannotator;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagOperations;
import com.dush.psannotator.model.TagPlanner;

import java.util.Map;
import java.util.stream.Collectors;

public class JavaGenerateTest extends TagTestCase
{
	public void testAddsImportAndNumbersFields()
	{
		addProtostuff();
		edit( "A.java", "class A { int a; String b; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.Tag;\n\n"
				+ "class A { @Tag(1)\n"
				+ "int a; @Tag(2)\n"
				+ "String b; }" );
	}

	public void testSkipsStaticTransientAndExcluded()
	{
		addProtostuff();
		edit( "A.java", "import io.protostuff.Exclude;\nclass A { static int s; transient int t; @Exclude int x; int a; }",
				c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.Exclude;\n"
				+ "import io.protostuff.Tag;\n\n"
				+ "class A { static int s; transient int t; @Exclude int x; @Tag(1)\n"
				+ "int a; }" );
	}

	public void testTagsTransientWhenEnabled()
	{
		addProtostuff();
		edit( "A.java", "class A { transient int t; int a; }", c -> TagOperations.generate( c, 5, true, null ) );
		check( "import io.protostuff.Tag;\n\n"
				+ "class A { @Tag(5)\n"
				+ "transient int t; @Tag(6)\n"
				+ "int a; }" );
	}

	public void testOnlySelectedFieldsAndUnselectedUseNoNumber()
	{
		addProtostuff();
		edit( "A.java", "class A { int a; int b; int c; }",
				c -> TagOperations.generate( c, 1, false, f -> !f.getName().equals( "b" ) ) );
		check( "import io.protostuff.Tag;\n\n"
				+ "class A { @Tag(1)\n"
				+ "int a; int b; @Tag(2)\n"
				+ "int c; }" );
	}

	public void testRunningTwiceDoesNotDuplicateOrReuseNumbers()
	{
		addProtostuff();
		edit( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(2) int a; int b; int c; }",
				c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.Tag;\n"
				+ "class A { @Tag(2) int a; @Tag(1)\n"
				+ "int b; @Tag(3)\n"
				+ "int c; }" );
	}

	public void testReservesNumbersOfInheritedFields()
	{
		addProtostuff();
		myFixture.addClass( "import io.protostuff.Tag;\n"
				+ "class Base { int untagged; @Tag(7) int tagged; static int ignored; }" );
		edit( "A.java", "class A extends Base { int a; int b; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.Tag;\n\n"
				+ "class A extends Base { @Tag(2)\n"
				+ "int a; @Tag(3)\n"
				+ "int b; }" );
	}

	public void testPlanMatchesWhatIsWritten()
	{
		addProtostuff();
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { int a; @Tag(2) int b; int c; }" );
		TagClass cls = classAtCaretOrFirst();
		Map<String, Integer> plan = TagPlanner.planGenerate( cls, 1, false, null ).entrySet().stream()
				.collect( Collectors.toMap( e -> e.getKey().getName(), Map.Entry::getValue ) );
		assertEquals( Map.of( "a", 1, "c", 3 ), plan );
		assertEquals( 3, TagPlanner.nextAfterMax( cls ) );
	}

	public void testAddsImportWhenProtostuffIsNotOnClasspath()
	{
		edit( "A.java", "package p;\n\nclass A { int a; int b; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "package p;\n\n"
				+ "import io.protostuff.Tag;\n\n"
				+ "class A { @Tag(1)\n"
				+ "int a; @Tag(2)\n"
				+ "int b; }" );
	}

	public void testKeepsExistingWildcardImport()
	{
		addProtostuff();
		edit( "A.java", "import io.protostuff.*;\nclass A { int a; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.*;\n"
				+ "class A { @Tag(1)\n"
				+ "int a; }" );
	}

	public void testUsesQualifiedNameWhenAnotherTagIsImported()
	{
		addProtostuff();
		myFixture.addClass( "package other;\npublic @interface Tag { }" );
		edit( "A.java", "import other.Tag;\nclass A { int a; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "import other.Tag;\n"
				+ "class A { @io.protostuff.Tag(1)\n"
				+ "int a; }" );
	}

	public void testUsesQualifiedNameWhenSamePackageHasTag()
	{
		myFixture.addClass( "package p;\npublic class Tag { }" );
		edit( "A.java", "package p;\nclass A { int a; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "package p;\n"
				+ "class A { @io.protostuff.Tag(1)\n"
				+ "int a; }" );
	}

	public void testNoImportWhenNothingToTag()
	{
		edit( "A.java", "class A { static int s; }", c -> TagOperations.generate( c, 1, false, null ) );
		check( "class A { static int s; }" );
	}

	public void testEnumsAndInterfacesAreNotTaggable()
	{
		myFixture.configureByText( "E.java", "enum E { X; int a; } interface I { int C = 1; }" );
		assertEmpty( com.dush.psannotator.model.TagModels.classesInFile( myFixture.getFile() ) );
	}

}
