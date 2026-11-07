package com.dush.psannotator;

import com.dush.psannotator.inspection.ProtostuffTagInspection;

public class JavaInspectionTest extends TagTestCase
{
	@Override protected void setUp() throws Exception
	{
		super.setUp();
		myFixture.enableInspections( new ProtostuffTagInspection() );
		addProtostuff();
	}

	public void testNoWarningsWithoutTags()
	{
		myFixture.configureByText( "A.java", "class A { int a; int b; }" );
		myFixture.checkHighlighting();
	}

	public void testNoWarningsWhenAllTagged()
	{
		myFixture.configureByText( "A.java",
				"import io.protostuff.*;\nclass A { @Tag(1) int a; @Tag(2) int b; static int s; transient int t; @Exclude int x; }" );
		myFixture.checkHighlighting();
	}

	public void testMissingTag()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int a; int <warning descr=\"Field 'b' has no @Tag; Protostuff requires all serialized fields to be tagged once any field is\">b</warning>; }" );
		myFixture.checkHighlighting();
	}

	public void testDuplicateIncludingInherited()
	{
		myFixture.addClass( "import io.protostuff.Tag;\nclass Base { @Tag(1) int x; }" );
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A extends Base { <warning descr=\"Tag 1 is also used by Base.x\">@Tag(1)</warning> int a; }" );
		myFixture.checkHighlighting();
	}

	public void testOutOfRange()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { <warning descr=\"Tag 0 is out of range; it must be between 1 and 536870911\">@Tag(0)</warning> int a; }" );
		myFixture.checkHighlighting();
	}

	public void testIgnoredTagOnTransient()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int a; <weak_warning descr=\"@Tag has no effect: 't' is transient, so Protostuff doesn't serialize it\">@Tag(2)</weak_warning> transient int t; }" );
		myFixture.checkHighlighting( true, false, true );
	}

	public void testUntaggedInheritedField()
	{
		myFixture.addClass( "class Base { int x; }" );
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass <warning descr=\"Protostuff needs @Tag on every serialized field, but inherited field Base.x has none\">A</warning> extends Base { @Tag(1) int a; }" );
		myFixture.checkHighlighting();
	}

	public void testTagRemainingFix()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(3) int a; int <caret>b; int c; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Tag the remaining fields" ) );
		check( "import io.protostuff.Tag;\nclass A { @Tag(3) int a; @Tag(4)\nint b; @Tag(5)\nint c; }" );
	}

	public void testTagInheritedFieldsFix()
	{
		myFixture.addFileToProject( "Base.java", "class Base { int x; }" );
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass <caret>A extends Base { @Tag(1) int a; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Tag the remaining fields here and in superclasses" ) );
		check( "import io.protostuff.Tag;\nclass A extends Base { @Tag(1) int a; }" );
		assertEquals( "import io.protostuff.Tag;\n\nclass Base { @Tag(2)\nint x; }",
				myFixture.findClass( "Base" ).getContainingFile().getText() );
	}

	public void testNextFreeIgnoresOutOfRangeValue()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(<caret>999999999) int b; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Use the next free tag number" ) );
		check( "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(2) int b; }" );
	}

	public void testNextFreeNumberFix()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(<caret>1) int b; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Use the next free tag number" ) );
		check( "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(2) int b; }" );
	}

	public void testRemoveIgnoredTagFix()
	{
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(<caret>1) static int s; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Remove @Tag" ) );
		check( "class A { static int s; }" );
	}
}
