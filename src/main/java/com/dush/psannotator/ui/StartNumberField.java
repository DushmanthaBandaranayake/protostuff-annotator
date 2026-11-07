package com.dush.psannotator.ui;

import com.dush.psannotator.model.TagNames;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

/** Text field for a tag number with validation. */
final class StartNumberField extends JBTextField
{
	StartNumberField( int initial )
	{
		super( Integer.toString( initial ), 8 );
	}

	@Nullable Integer value()
	{
		try
		{
			int value = Integer.parseInt( getText().trim() );
			return TagNames.isValidTag( value ) ? value : null;
		}
		catch ( NumberFormatException e )
		{
			return null;
		}
	}

	@Nullable ValidationInfo validateValue()
	{
		return value() == null
				? new ValidationInfo( "Enter a whole number from 1 to " + TagNames.MAX_TAG, this )
				: null;
	}
}
