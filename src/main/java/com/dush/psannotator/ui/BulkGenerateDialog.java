package com.dush.psannotator.ui;

import com.dush.psannotator.TagSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;

/** Generate for several classes at once. */
public final class BulkGenerateDialog extends DialogWrapper
{
	private final int classCount;
	private final JBRadioButton afterMax = new JBRadioButton( "Continue after the highest existing tag in each class", true );
	private final JBRadioButton fromStart = new JBRadioButton( "Start from:" );
	private final StartNumberField startField = new StartNumberField( 1 );
	private final JBCheckBox transientBox = new JBCheckBox( "Tag transient fields", TagSettings.isIncludeTransient() );
	private final JBCheckBox onlyTaggedBox =
			new JBCheckBox( "Only classes that already have some @Tag fields", TagSettings.isOnlyTaggedClasses() );

	public BulkGenerateDialog( @NotNull Project project, int classCount )
	{
		super( project );
		this.classCount = classCount;
		ButtonGroup group = new ButtonGroup();
		group.add( afterMax );
		group.add( fromStart );
		startField.setEnabled( false );
		afterMax.addActionListener( e -> startField.setEnabled( false ) );
		fromStart.addActionListener( e -> startField.setEnabled( true ) );
		setTitle( "Generate Protostuff Tags" );
		setOKButtonText( "Generate" );
		init();
	}

	@Nullable @Override protected JComponent createCenterPanel()
	{
		return FormBuilder.createFormBuilder()
				.addComponent( new JBLabel( classCount + ( classCount == 1 ? " class" : " classes" ) + " selected." ) )
				.addComponent( afterMax )
				.addLabeledComponent( fromStart, startField )
				.addComponent( transientBox )
				.addComponent( onlyTaggedBox )
				.getPanel();
	}

	@Nullable @Override protected ValidationInfo doValidate()
	{
		return fromStart.isSelected() ? startField.validateValue() : null;
	}

	@Override protected void doOKAction()
	{
		TagSettings.setIncludeTransient( transientBox.isSelected() );
		TagSettings.setOnlyTaggedClasses( onlyTaggedBox.isSelected() );
		super.doOKAction();
	}

	/** {@code null} means continue after the highest tag in each class. */
	public @Nullable Integer getStart()
	{
		return fromStart.isSelected() ? startField.value() : null;
	}

	public boolean isIncludeTransient()
	{
		return transientBox.isSelected();
	}

	public boolean isOnlyTaggedClasses()
	{
		return onlyTaggedBox.isSelected();
	}
}
