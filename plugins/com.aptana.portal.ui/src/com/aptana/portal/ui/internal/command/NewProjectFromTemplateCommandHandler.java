/**
 * Aptana Studio
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.portal.ui.internal.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.ui.wizards.IWizardRegistry;

import com.aptana.projects.internal.wizards.ProjectTemplateSelectionPage;
import com.aptana.ui.util.UIUtils;

/**
 * Command handler for "com.aptana.portal.ui.command.newProjectFromTemplate". Opens an Aptana/Titanium wizard and
 * initializes the project template
 * 
 * @author nle
 */
public class NewProjectFromTemplateCommandHandler extends AbstractHandler
{

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		String wizardId = event.getParameter(ProjectTemplateSelectionPage.COMMAND_PROJECT_FROM_TEMPLATE_NEW_WIZARD_ID);
		String templateName = event
				.getParameter(ProjectTemplateSelectionPage.COMMAND_PROJECT_FROM_TEMPLATE_PROJECT_TEMPLATE_NAME);

		IWizardRegistry wizardRegistry = PlatformUI.getWorkbench().getNewWizardRegistry();
		IWizardDescriptor wizardDescriptor = wizardRegistry.findWizard(wizardId);
		if (wizardDescriptor == null)
		{
			throw new ExecutionException("unknown wizard: " + wizardId); //$NON-NLS-1$
		}

		try
		{
			IWorkbenchWizard wizard = wizardDescriptor.createWizard();
			wizard.init(PlatformUI.getWorkbench(), null);

			if (wizard instanceof IExecutableExtension)
			{
				((IExecutableExtension) wizard).setInitializationData(null,
						ProjectTemplateSelectionPage.COMMAND_PROJECT_FROM_TEMPLATE_PROJECT_TEMPLATE_NAME, templateName);
			}

			if (wizardDescriptor.canFinishEarly() && !wizardDescriptor.hasPages())
			{
				wizard.performFinish();
				return null;
			}

			Shell parent = UIUtils.getActiveShell();
			WizardDialog dialog = new WizardDialog(parent, wizard);
			dialog.create();
			dialog.open();

		}
		catch (CoreException ex)
		{
			throw new ExecutionException("error creating wizard", ex); //$NON-NLS-1$
		}
		// TODO Auto-generated method stub
		return null;
	}
}
