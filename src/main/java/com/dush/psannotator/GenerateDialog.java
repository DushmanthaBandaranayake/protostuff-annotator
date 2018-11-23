package com.dush.psannotator;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.ui.CollectionListModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author dushmantha
 * 2018/1/16
 */
public class GenerateDialog extends DialogWrapper
{
	private final CollectionListModel<PsiField> fields;
	private final LabeledComponent<JPanel> labeledComponent;
	private final JTextField jTextFieldFrom;
	private final JCheckBox jChkBoxTagStaticFields;
	private final JCheckBox jChkBoxTagTransientFields;

	GenerateDialog( PsiClass psiClass )
	{
		super( psiClass.getProject() );
		setTitle( "Protostuff Tag Annotator" );
		fields = new CollectionListModel<>( psiClass.getAllFields() );
		jChkBoxTagStaticFields = new JCheckBox("Tag Static fields");
		jChkBoxTagTransientFields = new JCheckBox("Tag Transient fields");
		jTextFieldFrom = new JTextField( "1", 5 )
		{
			@Override public void setText( String t )
			{
				if ( t != null && !t.isEmpty() )
				{
					try
					{
						final int number = Integer.parseInt( t );
						if ( !( number < 0 ) )
						{
							super.setText( t );
						}
					}
					catch ( Exception e )
					{
						JOptionPane.showMessageDialog(null, t);
						//pass
						//not a valid number
					}
				}
			}
		};

		//txtFieldFrom = new JTextField( "1" );
		JPanel panel = new JPanel();
		panel.add( jTextFieldFrom );
		panel.add( jChkBoxTagStaticFields );
		panel.add( jChkBoxTagTransientFields );

		labeledComponent = LabeledComponent.create( panel, "Plese Enter the Starting Tag Value" );
		init();
	}

	@Nullable @Override protected JComponent createCenterPanel()
	{
		return labeledComponent;

	}

	public List<PsiField> getFields()
	{
		return fields.getItems();
	}

	public int getStartingValue()
	{
		return Integer.parseInt( jTextFieldFrom.getText().trim() );
	}

	public boolean isStaticFieldsEnabled()
	{
		return jChkBoxTagStaticFields.isSelected();
	}
	public boolean isTransientFieldsEnabled()
	{
		return jChkBoxTagTransientFields.isSelected();
	}
}
