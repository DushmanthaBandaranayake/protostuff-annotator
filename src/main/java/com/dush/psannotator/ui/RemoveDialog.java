package com.dush.psannotator.ui;

import com.dush.psannotator.model.TagHierarchy;
import com.dush.psannotator.model.TagOperations;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

/** Remove for one class: only this class, or its superclasses and subclasses too. */
public final class RemoveDialog extends DialogWrapper
{
	private final TagHierarchy hierarchy;
	private final ScopeChooser scope;

	public RemoveDialog( @NotNull TagHierarchy hierarchy )
	{
		super( hierarchy.getTarget().getDeclaration().getProject() );
		this.hierarchy = hierarchy;
		this.scope = new ScopeChooser( hierarchy,
				List.of( TagHierarchy.Scope.THIS_CLASS, TagHierarchy.Scope.WHOLE_HIERARCHY ),
				TagHierarchy.Scope.THIS_CLASS, TagOperations::hasAnyTag );
		setTitle( "Remove Protostuff Tags from " + hierarchy.getTarget().getName() );
		setOKButtonText( "Remove" );
		init();
	}

	@Nullable @Override protected JComponent createCenterPanel()
	{
		return FormBuilder.createFormBuilder()
				.addComponent( new JBLabel( "Remove every @Tag from:" ) )
				.addComponent( scope.getPanel() )
				.addComponent( new JBLabel( "You can undo this." ) )
				.getPanel();
	}

	public @NotNull TagHierarchy.Scope getScope()
	{
		return scope.getSelected();
	}

	public @NotNull TagHierarchy getHierarchy()
	{
		return hierarchy;
	}
}
