package com.dush.psannotator.ui;

import com.dush.psannotator.TagSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/** Asks for the first number and makes the compatibility risk explicit. */
public final class RenumberDialog extends DialogWrapper
{
	private final String what;
	private final StartNumberField startField = new StartNumberField( TagSettings.getRenumberStart() );

	public RenumberDialog( @NotNull Project project, @NotNull String what )
	{
		super( project );
		this.what = what;
		setTitle( "Renumber Protostuff Tags" );
		setOKButtonText( "Renumber" );
		init();
	}

	@Nullable @Override protected JComponent createCenterPanel()
	{
		JBLabel warning = new JBLabel( "<html>Renumbers the tagged fields of " + what + " in declaration order.<br>"
				+ "<b>Data serialized with the old numbers can no longer be read correctly.</b></html>",
				AllIcons.General.Warning, JBLabel.LEFT );
		return FormBuilder.createFormBuilder()
				.addComponent( warning )
				.addLabeledComponent( "Starting tag number:", startField )
				.getPanel();
	}

	@Nullable @Override protected ValidationInfo doValidate()
	{
		return startField.validateValue();
	}

	@Override protected void doOKAction()
	{
		Integer start = startField.value();
		if ( start != null )
		{
			TagSettings.setRenumberStart( start );
		}
		super.doOKAction();
	}

	public int getStart()
	{
		Integer start = startField.value();
		return start != null ? start : 1;
	}
}
