package com.dush.psannotator;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagModels;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class TagTestCase extends LightJavaCodeInsightFixtureTestCase
{
	protected static final String TAG_SOURCE = "package io.protostuff;\n"
			+ "public @interface Tag { int value(); }";
	protected static final String EXCLUDE_SOURCE = "package io.protostuff;\n"
			+ "public @interface Exclude { }";

	@Override protected @NotNull LightProjectDescriptor getProjectDescriptor()
	{
		// Mock JDK only; the default descriptor downloads JetBrains annotations from Maven, which the tests don't need.
		return new DefaultLightProjectDescriptor();
	}

	protected void addProtostuff()
	{
		myFixture.addClass( TAG_SOURCE );
		myFixture.addClass( EXCLUDE_SOURCE );
	}

	/** Opens {@code source} and runs {@code edit} on the class marked by the caret (or the first class). */
	protected void edit( String fileName, String source, Consumer<TagClass> edit )
	{
		myFixture.configureByText( fileName, source );
		TagClass cls = classAtCaretOrFirst();
		assertNotNull( "no taggable class", cls );
		WriteCommandAction.runWriteCommandAction( getProject(), () -> edit.accept( cls ) );
	}

	/** Like checkResult, but the failure shows both texts. */
	protected void check( String expected )
	{
		com.intellij.psi.PsiDocumentManager.getInstance( getProject() ).commitAllDocuments();
		assertEquals( expected, myFixture.getEditor().getDocument().getText() );
	}

	protected TagClass classAtCaretOrFirst()
	{
		int offset = myFixture.getCaretOffset();
		if ( offset > 0 )
		{
			PsiElement element = myFixture.getFile().findElementAt( offset );
			return TagModels.enclosingClass( element );
		}
		return TagModels.classesInFile( myFixture.getFile() ).get( 0 );
	}
}
