package com.dush.psannotator.ui;

import com.dush.psannotator.model.TagClass;
import com.dush.psannotator.model.TagHierarchy;
import com.intellij.ui.components.JBRadioButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Radio buttons for the classes an operation covers. Options that add no class are disabled. */
final class ScopeChooser
{
	private final Map<TagHierarchy.Scope, JBRadioButton> buttons = new EnumMap<>( TagHierarchy.Scope.class );
	private final JPanel panel = new JPanel();

	/**
	 * @param counts which classes count towards an option's "(N more)" label, e.g. only those with tags for Remove
	 */
	ScopeChooser( @NotNull TagHierarchy hierarchy, @NotNull List<TagHierarchy.Scope> scopes,
			@NotNull TagHierarchy.Scope preferred, @NotNull Predicate<TagClass> counts )
	{
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		ButtonGroup group = new ButtonGroup();
		for ( TagHierarchy.Scope scope : scopes )
		{
			long more = hierarchy.others( scope ).stream().filter( counts ).count();
			String text = scope.getLabel();
			if ( scope != TagHierarchy.Scope.THIS_CLASS )
			{
				text += more == 0 ? " (none in the project)" : " (" + more + " more " + ( more == 1 ? "class" : "classes" ) + ")";
			}
			JBRadioButton button = new JBRadioButton( text );
			button.setEnabled( scope == TagHierarchy.Scope.THIS_CLASS || more > 0 );
			group.add( button );
			buttons.put( scope, button );
			panel.add( button );
		}
		JBRadioButton initial = buttons.get( preferred );
		( initial != null && initial.isEnabled() ? initial : buttons.get( TagHierarchy.Scope.THIS_CLASS ) ).setSelected( true );
	}

	@NotNull JPanel getPanel()
	{
		return panel;
	}

	@NotNull TagHierarchy.Scope getSelected()
	{
		for ( Map.Entry<TagHierarchy.Scope, JBRadioButton> entry : buttons.entrySet() )
		{
			if ( entry.getValue().isSelected() )
			{
				return entry.getKey();
			}
		}
		return TagHierarchy.Scope.THIS_CLASS;
	}

	void select( @NotNull TagHierarchy.Scope scope )
	{
		JBRadioButton button = buttons.get( scope );
		if ( button != null && button.isEnabled() )
		{
			button.setSelected( true );
		}
	}

	void onChange( @NotNull Runnable listener )
	{
		buttons.values().forEach( b -> b.addActionListener( e -> listener.run() ) );
	}
}
