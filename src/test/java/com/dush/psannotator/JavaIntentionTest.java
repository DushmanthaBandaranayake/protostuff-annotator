package com.dush.psannotator;

public class JavaIntentionTest extends TagTestCase
{
	public void testAddsNextAfterMaxWithoutFillingGaps()
	{
		addProtostuff();
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(5) int b; int <caret>c; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Add @Tag(6)" ) );
		check( "import io.protostuff.Tag;\nclass A { @Tag(1) int a; @Tag(5) int b; @Tag(6)\nint c; }" );
	}

	public void testFirstTagAddsImport()
	{
		myFixture.configureByText( "A.java", "class A { int <caret>a; }" );
		myFixture.launchAction( myFixture.findSingleIntention( "Add @Tag(1)" ) );
		check( "import io.protostuff.Tag;\n\nclass A { @Tag(1)\nint a; }" );
	}

	public void testNotOfferedOnTaggedOrStaticField()
	{
		addProtostuff();
		myFixture.configureByText( "A.java", "import io.protostuff.Tag;\nclass A { @Tag(1) int <caret>a; }" );
		assertEmpty( myFixture.filterAvailableIntentions( "Add @Tag" ) );
		myFixture.configureByText( "B.java", "class B { static int <caret>s; }" );
		assertEmpty( myFixture.filterAvailableIntentions( "Add @Tag" ) );
	}
}
