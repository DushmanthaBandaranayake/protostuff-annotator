package com.dush.psannotator;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiClass;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * Asks for the first tag number and whether transient fields should be tagged.
 *
 * @author dushmantha
 * 2018/1/16
 */
public class GenerateDialog extends DialogWrapper
{
	private final JTextField startField = new JTextField( "1", 8 );
	private final JCheckBox transientCheckBox = new JCheckBox( "Tag transient fields" );

	GenerateDialog( PsiClass psiClass )
	{
		super( psiClass.getProject() );
		setTitle( "Generate Protostuff Tags" );
		init();
	}

	@Nullable @Override protected JComponent createCenterPanel()
	{
		return FormBuilder.createFormBuilder()
				.addLabeledComponent( "Starting tag number:", startField )
				.addComponent( transientCheckBox )
				.getPanel();
	}

	@Override public @Nullable JComponent getPreferredFocusedComponent()
	{
		return startField;
	}

	@Nullable @Override protected ValidationInfo doValidate()
	{
		if ( parseStart() == null )
		{
			return new ValidationInfo( "Enter a whole number from 1 to " + TagGenerator.MAX_TAG, startField );
		}
		return null;
	}

	/** Only valid after the dialog was closed with OK, which requires {@link #doValidate()} to pass. */
	public int getStartingValue()
	{
		Integer start = parseStart();
		return start != null ? start : 1;
	}

	public boolean isTransientFieldsEnabled()
	{
		return transientCheckBox.isSelected();
	}

	@Nullable
	private Integer parseStart()
	{
		try
		{
			int value = Integer.parseInt( startField.getText().trim() );
			return value >= 1 && value <= TagGenerator.MAX_TAG ? value : null;
		}
		catch ( NumberFormatException e )
		{
			return null;
		}
	}
}
