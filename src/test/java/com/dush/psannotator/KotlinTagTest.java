package com.dush.psannotator;

import com.dush.psannotator.kotlin.ProtostuffTagKotlinInspection;
import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagModels;
import com.dush.psannotator.model.TagOperations;

import java.util.stream.Collectors;

public class KotlinTagTest extends TagTestCase
{
	public void testFieldsFromConstructorAndBody()
	{
		myFixture.configureByText( "A.kt", "class A(val a: Int, b: Int, var c: String) {\n"
				+ "    val d = 1\n"
				+ "    val computed: Int get() = 2\n"
				+ "    var withField: Int = 0\n        get() = field\n"
				+ "    val lazyOne by lazy { 3 }\n"
				+ "    lateinit var e: String\n"
				+ "    @Transient var t = 0\n"
				+ "    companion object { val s = 1 }\n"
				+ "}" );
		TagClass cls = TagModels.classesInFile( myFixture.getFile() ).get( 0 );
		String names = cls.getFields().stream().map( TagField::getName ).collect( Collectors.joining( "," ) );
		assertEquals( "a,c,d,withField,e,t", names );
		assertTrue( cls.getFields().get( 5 ).isTransient() );
	}

	public void testGenerateAddsImportAndTags()
	{
		edit( "A.kt", "package p\n\nclass A(val a: Int) {\n    var b = 0\n}\n", c -> TagOperations.generate( c, 1, false, null ) );
		check( "package p\n\nimport io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int) {\n    @field:Tag(2)\n    var b = 0\n}\n" );
	}

	public void testGenerateAppendsToExistingImports()
	{
		edit( "A.kt", "import java.io.Serializable\n\nclass A(val a: Int) : Serializable\n",
				c -> TagOperations.generate( c, 1, false, null ) );
		check( "import java.io.Serializable\nimport io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int) : Serializable\n" );
	}

	public void testExistingTagsAreKeptAndSkipped()
	{
		edit( "A.kt", "import io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int, val b: Int)\n",
				c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int, @field:Tag(2) val b: Int)\n" );
	}

	public void testQualifiedNameWhenAnotherTagImported()
	{
		edit( "A.kt", "import other.Tag\n\nclass A(val a: Int)\n", c -> TagOperations.generate( c, 1, false, null ) );
		check( "import other.Tag\n\nclass A(@field:io.protostuff.Tag(1) val a: Int)\n" );
	}

	public void testRenumberAndRemove()
	{
		edit( "A.kt", "import io.protostuff.Tag\n\nclass A(@field:Tag(7) val a: Int, @field:Tag(value = 9) val b: Int) {\n    @Tag(3) var c = 0\n}\n",
				c -> TagOperations.renumber( c, 1 ) );
		check( "import io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int, @field:Tag(value = 2) val b: Int) {\n    @Tag(3) var c = 0\n}\n" );
		edit( "A.kt", "import io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int)\n", TagOperations::removeAll );
		check( "class A(val a: Int)\n" );
	}

	public void testKotlinSubclassOfJavaBase()
	{
		addProtostuff();
		myFixture.addClass( "import io.protostuff.Tag;\npublic class Base { @Tag(1) int x; }" );
		edit( "A.kt", "class A(val a: Int) : Base()\n", c -> TagOperations.generate( c, 1, false, null ) );
		check( "import io.protostuff.Tag\n\nclass A(@field:Tag(2) val a: Int) : Base()\n" );
	}

	public void testInspectionAndIntention()
	{
		myFixture.enableInspections( new ProtostuffTagKotlinInspection() );
		// The light project has no Kotlin stdlib, so only our own warnings are compared.
		myFixture.configureByText( "A.kt", "import io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int, @field:Tag(1) val b: Int, val c: Int)\n" );
		java.util.Set<String> warnings = myFixture.doHighlighting( com.intellij.lang.annotation.HighlightSeverity.WARNING )
				.stream().map( h -> h.getDescription() ).filter( d -> d != null && d.contains( "Tag" ) && !d.contains( "nresolved" ) )
				.collect( Collectors.toSet() );
		assertEquals( java.util.Set.of( "Tag 1 is also used by A.b", "Tag 1 is also used by A.a",
				"Field 'c' has no @Tag; Protostuff requires all serialized fields to be tagged once any field is" ), warnings );

		myFixture.configureByText( "B.kt", "import io.protostuff.Tag\n\nclass B(@field:Tag(4) val a: Int, val <caret>b: Int)\n" );
		myFixture.launchAction( myFixture.findSingleIntention( "Add @Tag(5)" ) );
		check( "import io.protostuff.Tag\n\nclass B(@field:Tag(4) val a: Int, @field:Tag(5) val b: Int)\n" );
	}

	public void testTagOnConstructorParameterIsReportedAndFixed()
	{
		myFixture.enableInspections( new ProtostuffTagKotlinInspection() );
		myFixture.configureByText( "A.kt", "import io.protostuff.Tag\n\nclass A(@Tag(<caret>1) val a: Int, @param:Tag(2) val b: Int, @field:Tag(3) val c: Int) {\n    @Tag(4) var d = 0\n}\n" );
		long wrong = myFixture.doHighlighting( com.intellij.lang.annotation.HighlightSeverity.WARNING ).stream()
				.filter( h -> h.getDescription() != null && h.getDescription().contains( "use @field:Tag" ) ).count();
		assertEquals( 2, wrong ); // a and b; c uses field:, d is a body property
		myFixture.launchAction( myFixture.findSingleIntention( "Change to @field:Tag" ) );
		check( "import io.protostuff.Tag\n\nclass A(@field:Tag(1) val a: Int, @param:Tag(2) val b: Int, @field:Tag(3) val c: Int) {\n    @Tag(4) var d = 0\n}\n" );
	}

	public void testObjectsInterfacesEnumsAreNotTaggable()
	{
		myFixture.configureByText( "A.kt", "object O { val a = 1 }\ninterface I\nenum class E { X }\nannotation class Ann\n" );
		assertEmpty( TagModels.classesInFile( myFixture.getFile() ) );
	}
}
