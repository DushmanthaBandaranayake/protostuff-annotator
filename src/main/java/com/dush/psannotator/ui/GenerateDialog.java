package com.dush.psannotator.ui;

import com.dush.psannotator.TagSettings;
import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagField;
import com.dush.psannotator.model.TagHierarchy;
import com.dush.psannotator.model.TagPlanner;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiElement;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Generate for one class: pick fields and see the numbers they will get before pressing OK. */
public final class GenerateDialog extends DialogWrapper
{
	private final TagClass target;
	private final TagHierarchy hierarchy;
	private final ScopeChooser scope;
	private final JBLabel othersLabel = new JBLabel();
	private final List<TagField> fields;
	private final boolean[] checked;
	private final StartNumberField startField;
	private final JBCheckBox transientBox = new JBCheckBox( "Tag transient fields", TagSettings.isIncludeTransient() );
	private final FieldTableModel tableModel = new FieldTableModel();
	private Map<TagField, Integer> plan = Map.of();

	public GenerateDialog( @NotNull TagHierarchy hierarchy )
	{
		super( hierarchy.getTarget().getDeclaration().getProject() );
		this.hierarchy = hierarchy;
		this.target = hierarchy.getTarget();
		this.scope = new ScopeChooser( hierarchy, List.of( TagHierarchy.Scope.values() ), TagSettings.getGenerateScope(),
				GenerateDialog::hasFieldsToTag );
		this.fields = TagPlanner.untaggedFields( target, true );
		this.checked = new boolean[fields.size()];
		boolean hasTags = !TagPlanner.usedTags( TagPlanner.hierarchyTopDown( target ) ).isEmpty();
		this.startField = new StartNumberField( hasTags ? TagPlanner.nextAfterMax( target ) : 1 );
		applyTransientChoice();

		setTitle( "Generate Protostuff Tags for " + target.getName() );
		setOKButtonText( "Generate" );
		startField.getDocument().addDocumentListener( new DocumentAdapter()
		{
			@Override protected void textChanged( @NotNull DocumentEvent e )
			{
				replan();
			}
		} );
		transientBox.addActionListener( e -> {
			applyTransientChoice();
			replan();
		} );
		scope.onChange( this::replan );
		replan();
		init();
	}

	/** Untagged fields, transient ones included (they're unticked by default). */
	public static boolean hasFieldsToTag( @NotNull TagClass target )
	{
		return !TagPlanner.untaggedFields( target, true ).isEmpty();
	}

	@Override protected @Nullable String getDimensionServiceKey()
	{
		return "com.dush.psannotator.GenerateDialog";
	}

	@Nullable @Override protected JComponent createCenterPanel()
	{
		JBTable table = new JBTable( tableModel );
		table.getColumnModel().getColumn( 0 ).setMaxWidth( JBUI.scale( 40 ) );
		table.getColumnModel().getColumn( 3 ).setMaxWidth( JBUI.scale( 80 ) );
		table.setShowGrid( false );
		JComponent scroll = ScrollPaneFactory.createScrollPane( table );
		scroll.setPreferredSize( JBUI.size( 460, 260 ) );

		return FormBuilder.createFormBuilder()
				.addLabeledComponent( "Starting tag number:", startField )
				.addComponent( transientBox )
				.addLabeledComponent( "Classes:", scope.getPanel() )
				.addComponent( new JBLabel( "Numbers already used in a class or its superclasses are skipped." ) )
				.addComponentFillVertically( scroll, JBUI.scale( 4 ) )
				.addComponent( othersLabel )
				.getPanel();
	}

	@Override public @Nullable JComponent getPreferredFocusedComponent()
	{
		return startField;
	}

	@Nullable @Override protected ValidationInfo doValidate()
	{
		ValidationInfo invalid = startField.validateValue();
		if ( invalid != null )
		{
			return invalid;
		}
		if ( plan.isEmpty() && otherClassesToTag().isEmpty() )
		{
			return new ValidationInfo( "Select at least one field" );
		}
		return null;
	}

	@Override protected void doOKAction()
	{
		TagSettings.setIncludeTransient( transientBox.isSelected() );
		TagSettings.setGenerateScope( scope.getSelected() );
		super.doOKAction();
	}

	/** Which classes to tag besides this one. */
	public @NotNull TagHierarchy.Scope getScope()
	{
		return scope.getSelected();
	}

	/** For tests. */
	public void selectScope( @NotNull TagHierarchy.Scope value )
	{
		scope.select( value );
		replan();
	}

	/** Classes in the chosen scope, other than this one, that have fields to tag. */
	public @NotNull List<TagClass> otherClassesToTag()
	{
		List<TagClass> result = new ArrayList<>();
		for ( TagClass cls : hierarchy.others( scope.getSelected() ) )
		{
			if ( !TagPlanner.untaggedFields( cls, isIncludeTransient() ).isEmpty() )
			{
				result.add( cls );
			}
		}
		return result;
	}

	public int getStart()
	{
		Integer start = startField.value();
		return start != null ? start : 1;
	}

	/** Whether transient fields count as taggable when numbering (true if any transient field was picked). */
	public boolean isIncludeTransient()
	{
		for ( int i = 0; i < fields.size(); i++ )
		{
			if ( checked[i] && fields.get( i ).isTransient() )
			{
				return true;
			}
		}
		return transientBox.isSelected();
	}

	/** Declarations of the fields to tag. */
	public @NotNull Set<PsiElement> getSelected()
	{
		Set<PsiElement> selected = new HashSet<>();
		for ( int i = 0; i < fields.size(); i++ )
		{
			if ( checked[i] )
			{
				selected.add( fields.get( i ).getDeclaration() );
			}
		}
		return selected;
	}

	/** Planned number per field name, as shown in the Tag column. */
	public @NotNull Map<String, Integer> plannedTagsByName()
	{
		Map<String, Integer> result = new java.util.LinkedHashMap<>();
		for ( TagField field : fields )
		{
			Integer value = plan.get( field );
			if ( value != null )
			{
				result.put( field.getName(), value );
			}
		}
		return result;
	}

	public @NotNull String tagColumnText( int row )
	{
		return (String) tableModel.getValueAt( row, 3 );
	}

	private void applyTransientChoice()
	{
		for ( int i = 0; i < fields.size(); i++ )
		{
			checked[i] = !fields.get( i ).isTransient() || transientBox.isSelected();
		}
	}

	private void replan()
	{
		Integer start = startField.value();
		if ( start == null )
		{
			plan = Map.of();
		}
		else
		{
			Set<PsiElement> selected = getSelected();
			plan = TagPlanner.planGenerate( target, start, isIncludeTransient(),
					f -> selected.contains( f.getDeclaration() ) );
		}
		tableModel.fireTableDataChanged();
		List<TagClass> others = otherClassesToTag();
		othersLabel.setText( others.isEmpty() ? " "
				: "Also tags every untagged field in: " + others.stream().map( TagClass::getName ).collect( Collectors.joining( ", " ) ) );
	}

	private final class FieldTableModel extends AbstractTableModel
	{
		private final String[] columns = { "", "Field", "Type", "Tag" };

		@Override public int getRowCount()
		{
			return fields.size();
		}

		@Override public int getColumnCount()
		{
			return columns.length;
		}

		@Override public String getColumnName( int column )
		{
			return columns[column];
		}

		@Override public Class<?> getColumnClass( int column )
		{
			return column == 0 ? Boolean.class : String.class;
		}

		@Override public boolean isCellEditable( int row, int column )
		{
			return column == 0;
		}

		@Override public Object getValueAt( int row, int column )
		{
			TagField field = fields.get( row );
			return switch ( column )
			{
				case 0 -> checked[row];
				case 1 -> field.getName() + ( field.isTransient() ? " (transient)" : "" );
				case 2 -> field.getTypeText();
				default -> {
					Integer value = plan.get( field );
					yield value != null ? value.toString() : "";
				}
			};
		}

		@Override public void setValueAt( Object value, int row, int column )
		{
			if ( column == 0 )
			{
				checked[row] = Boolean.TRUE.equals( value );
				replan();
			}
		}
	}

}
