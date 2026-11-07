package com.dush.psannotator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class TagGeneratorTest extends LightJavaCodeInsightFixtureTestCase
{
	private static final String TAG_SOURCE = "package io.protostuff;\n"
			+ "public @interface Tag { int value(); }";

	@Override protected @NotNull LightProjectDescriptor getProjectDescriptor()
	{
		// Mock JDK only; the default descriptor downloads JetBrains annotations from Maven, which the tests don't need.
		return new DefaultLightProjectDescriptor();
	}

	public void testAddsImportAndNumbersFields()
	{
		myFixture.addClass( TAG_SOURCE );
		run( "class A { int a; String b; }", 1, false );
		myFixture.checkResult( "import io.protostuff.Tag;\n\n"
				+ "class A { @Tag(1)\n"
				+ "int a; @Tag(2)\n"
				+ "String b; }" );
	}

	public void testSkipsStaticAndTransientByDefault()
	{
		myFixture.addClass( TAG_SOURCE );
		run( "class A { static int s; transient int t; int a; }", 1, false );
		myFixture.checkResult( "import io.protostuff.Tag;\n\n"
				+ "class A { static int s; transient int t; @Tag(1)\n"
				+ "int a; }" );
	}

	public void testTagsTransientWhenEnabled()
	{
		myFixture.addClass( TAG_SOURCE );
		run( "class A { transient int t; int a; }", 5, true );
		myFixture.checkResult( "import io.protostuff.Tag;\n\n"
				+ "class A { @Tag(5)\n"
				+ "transient int t; @Tag(6)\n"
				+ "int a; }" );
	}

	public void testRunningTwiceDoesNotDuplicateOrReuseNumbers()
	{
		myFixture.addClass( TAG_SOURCE );
		run( "import io.protostuff.Tag;\nclass A { @Tag(2) int a; int b; int c; }", 1, false );
		myFixture.checkResult( "import io.protostuff.Tag;\n"
				+ "class A { @Tag(2) int a; @Tag(1)\n"
				+ "int b; @Tag(3)\n"
				+ "int c; }" );
	}

	public void testReservesNumbersOfInheritedFields()
	{
		myFixture.addClass( TAG_SOURCE );
		myFixture.addClass( "import io.protostuff.Tag;\n"
				+ "class Base { int untagged; @Tag(7) int tagged; static int ignored; }" );
		run( "class A extends Base { int a; int b; }", 1, false );
		// Base.untagged reserves 1 and Base.tagged owns 7.
		myFixture.checkResult( "import io.protostuff.Tag;\n\n"
				+ "class A extends Base { @Tag(2)\n"
				+ "int a; @Tag(3)\n"
				+ "int b; }" );
	}

	public void testUnresolvedTagWithoutProtostuffOnClasspath()
	{
		run( "class A { @Tag(1) int a; int b; }", 1, false );
		myFixture.checkResult( "class A { @Tag(1) int a; @io.protostuff.Tag(2)\n"
				+ "int b; }" );
	}

	public void testHasTaggableFields()
	{
		myFixture.addClass( TAG_SOURCE );
		myFixture.configureByText( "A.java",
				"import io.protostuff.Tag;\nclass A { @Tag(1) int a; static int s; transient int t; }" );
		PsiClass psiClass = ( (PsiJavaFile) myFixture.getFile() ).getClasses()[0];
		assertFalse( TagGenerator.hasTaggableFields( psiClass, false ) );
		assertTrue( TagGenerator.hasTaggableFields( psiClass, true ) );
	}

	private void run( String source, int start, boolean includeTransient )
	{
		myFixture.configureByText( "A.java", source );
		PsiClass psiClass = ( (PsiJavaFile) myFixture.getFile() ).getClasses()[0];
		WriteCommandAction.runWriteCommandAction( getProject(),
				() -> { TagGenerator.generate( psiClass, start, includeTransient ); } );
	}
}
