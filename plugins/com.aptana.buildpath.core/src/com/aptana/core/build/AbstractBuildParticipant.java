/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license-epl.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.build;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import com.aptana.buildpath.core.BuildPathCorePlugin;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.resources.TaskTag;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.parsing.ast.IParseNode;

/**
 * It is recommended for instances of IBuildParticipant to subclass this class. This takes care of the getter/setter for
 * priority, as well as provides some helper methods for detecting tasks, generating {@link IProblem}s and determining
 * the line number of an offset in the document.
 * 
 * @author cwilliams
 */
public abstract class AbstractBuildParticipant implements IBuildParticipant, IExecutableExtension
{

	public static final String FILTER_DELIMITER = "####"; //$NON-NLS-1$
	private static final Pattern filterSplitter = Pattern.compile(FILTER_DELIMITER);

	/**
	 * Constants for dealing with build participants through the extension point.
	 */
	private static final String CONTENT_TYPE_ID = "contentTypeId"; //$NON-NLS-1$
	private static final String CONTENT_TYPE_BINDING = "contentTypeBinding"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String ATTR_PRIORITY = "priority"; //$NON-NLS-1$
	public static final int DEFAULT_PRIORITY = 50;

	private int fPriority = DEFAULT_PRIORITY;
	private Set<IContentType> contentTypes = Collections.emptySet();
	private String fId;
	private String fName;
	private String contributor;

	public int getPriority()
	{
		return fPriority;
	}

	public Set<IContentType> getContentTypes()
	{
		return Collections.unmodifiableSet(contentTypes);
	}

	public String getName()
	{
		return fName;
	}

	public String getId()
	{
		return fId;
	}

	/**
	 * By default participants are not 'required'. We override this for many of our own builtin ones that perform
	 * indexing/task detection, etc.
	 */
	public boolean isRequired()
	{
		return false;
	}

	public boolean isEnabled(BuildType type)
	{
		if (isRequired())
		{
			return true;
		}
		return Platform.getPreferencesService().getBoolean(getPreferenceNode(), getEnablementPreferenceKey(type),
				false, new IScopeContext[] { EclipseUtil.instanceScope(), EclipseUtil.defaultScope() });
	}

	public void setEnabled(BuildType type, boolean enabled)
	{
		if (isRequired())
		{
			return;
		}

		IEclipsePreferences prefs = EclipseUtil.instanceScope().getNode(getPreferenceNode());
		prefs.putBoolean(getEnablementPreferenceKey(type), enabled);
		try
		{
			prefs.flush();
		}
		catch (BackingStoreException e)
		{
			IdeLog.logError(BuildPathCorePlugin.getDefault(), e);
		}
	}

	public String getEnablementPreferenceKey(BuildType type)
	{
		return MessageFormat.format("{0}_{1}_enabled", getId(), type.name().toLowerCase()); //$NON-NLS-1$
	}

	public void restoreDefaults()
	{
		if (isRequired())
		{
			// no-op if required for now, since we don't do filters/etc here yet.
			return;
		}

		IEclipsePreferences prefs = EclipseUtil.instanceScope().getNode(getPreferenceNode());
		prefs.remove(getEnablementPreferenceKey(BuildType.BUILD));
		prefs.remove(getEnablementPreferenceKey(BuildType.RECONCILE));
		prefs.remove(getFiltersPreferenceKey());
		try
		{
			prefs.flush();
		}
		catch (BackingStoreException e)
		{
			IdeLog.logError(BuildPathCorePlugin.getDefault(), e);
		}
	}

	public List<String> getFilters()
	{
		if (isRequired())
		{
			return Collections.emptyList();
		}
		String rawFilters = Platform.getPreferencesService().getString(getPreferenceNode(), getFiltersPreferenceKey(),
				null, new IScopeContext[] { EclipseUtil.instanceScope(), EclipseUtil.defaultScope() });
		if (StringUtil.isEmpty(rawFilters))
		{
			return Collections.emptyList();
		}
		return Arrays.asList(filterSplitter.split(rawFilters));
	}

	public void setFilters(IScopeContext context, String... filters)
	{
		if (isRequired())
		{
			return; // currently required can't be edited.
		}

		IEclipsePreferences prefs = context.getNode(getPreferenceNode());
		prefs.put(getFiltersPreferenceKey(), StringUtil.join(FILTER_DELIMITER, filters));
		try
		{
			prefs.flush();
		}
		catch (BackingStoreException e)
		{
			IdeLog.logError(BuildPathCorePlugin.getDefault(), e);
		}
	}

	protected String getFiltersPreferenceKey()
	{
		return MessageFormat.format("{0}_filters", getId()); //$NON-NLS-1$
	}

	/**
	 * The string id of the root preference node. Typically the contributing plugin's id. this is the qualifier we use
	 * to search for pref values for this participant.
	 * 
	 * @return
	 */
	protected String getPreferenceNode()
	{
		return contributor;
	}

	public void buildStarting(IProject project, int kind, IProgressMonitor monitor)
	{
		// no-op. Most impls won't do project level setup.
	}

	public void buildEnding(IProgressMonitor monitor)
	{
		// no-op. Most impls won't do project level setup.
	}

	public void clean(IProject project, IProgressMonitor monitor)
	{
		// no-op. Most impls won't do batch clean stuff
	}

	protected int getLineNumber(int start, String source)
	{
		if (start < 0 || start >= source.length())
		{
			return -1;
		}
		if (start == 0)
		{
			return 1;
		}

		Matcher m = StringUtil.LINE_SPLITTER.matcher(source.substring(0, start));
		int line = 1;
		while (m.find())
		{
			int offset = m.start();
			if (offset > start)
			{
				break;
			}
			line++;
		}
		return line;
	}

	/**
	 * Common code for detecting tasks in comment nodes from ASTs
	 **/
	protected Collection<IProblem> processCommentNode(String filePath, String source, int initialOffset,
			IParseNode commentNode, String commentEnding)
	{
		if (commentNode == null)
		{
			return Collections.emptyList();
		}

		String text = commentNode.getText();
		if (StringUtil.isEmpty(text))
		{
			text = getText(source, commentNode);
		}

		if (!TaskTag.isCaseSensitive())
		{
			text = text.toLowerCase();
		}

		String[] lines = StringUtil.LINE_SPLITTER.split(text);
		Collection<IProblem> tasks = new ArrayList<IProblem>();
		for (String line : lines)
		{
			for (TaskTag entry : TaskTag.getTaskTags())
			{
				String tag = entry.getName();
				if (!TaskTag.isCaseSensitive())
				{
					tag = tag.toLowerCase();
				}
				int index = line.indexOf(tag);
				if (index == -1)
				{
					continue;
				}

				String message = new String(line.substring(index).trim());
				// Remove "*/" or whatever language specific comment ending from the end of the line!
				if (message.endsWith(commentEnding))
				{
					message = message.substring(0, message.length() - commentEnding.length()).trim();
				}
				// Start of comment + index of line + index of tag on line + initial offset
				int lineIndex = text.indexOf(line);
				int start = commentNode.getStartingOffset() + lineIndex + index + initialOffset;
				tasks.add(createTask(filePath, message, entry.getPriority(), getLineNumber(start, source), start, start
						+ message.length()));
			}
		}
		return tasks;
	}

	private String getText(String source, IParseNode commentNode)
	{
		if (source == null)
		{
			return StringUtil.EMPTY;
		}
		int start = Math.max(0, commentNode.getStartingOffset());
		int end = Math.min(commentNode.getEndingOffset() + 1, source.length());
		return new String(source.substring(start, end));
	}

	/**
	 * @deprecated
	 * @param sourcePath
	 * @param message
	 * @param priority
	 * @param lineNumber
	 * @param offset
	 * @param endOffset
	 * @return
	 */
	protected IProblem createTask(String sourcePath, String message, Integer priority, int lineNumber, int offset,
			int endOffset)
	{
		return new Problem(IMarker.SEVERITY_INFO, message, offset, endOffset - offset, lineNumber, sourcePath, priority);
	}

	protected IProblem createInfo(String message, int lineNumber, int offset, int length, String sourcePath)
	{
		return new Problem(IMarker.SEVERITY_INFO, message, offset, length, lineNumber, sourcePath);
	}

	protected IProblem createWarning(String message, int lineNumber, int offset, int length, String sourcePath)
	{
		return new Problem(IMarker.SEVERITY_WARNING, message, offset, length, lineNumber, sourcePath);
	}

	protected IProblem createError(String message, int lineNumber, int offset, int length, String sourcePath)
	{
		return new Problem(IMarker.SEVERITY_ERROR, message, offset, length, lineNumber, sourcePath);
	}

	protected boolean hasErrorOrWarningOnLine(List<IProblem> items, int line)
	{
		if (items == null)
		{
			return false;
		}

		for (IProblem item : items)
		{
			if (item.getLineNumber() == line)
			{
				return true;
			}
		}

		return false;
	}

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException
	{
		String rawPriority = config.getAttribute(ATTR_PRIORITY);
		if (!StringUtil.isEmpty(rawPriority))
		{
			try
			{
				this.fPriority = Integer.parseInt(rawPriority);
			}
			catch (NumberFormatException e)
			{
				IdeLog.logWarning(BuildPathCorePlugin.getDefault(), MessageFormat.format(
						"Unable to parse priority value ({0}) as an integer, defaulting to 50.", rawPriority), e); //$NON-NLS-1$
			}
		}
		this.fId = config.getAttribute(ID);
		this.fName = config.getAttribute(NAME);
		this.contributor = config.getContributor().getName();

		// Read in the content types
		this.contentTypes = new HashSet<IContentType>();
		IContentTypeManager manager = Platform.getContentTypeManager();
		IConfigurationElement[] rawContentTypes = config.getChildren(CONTENT_TYPE_BINDING);
		for (IConfigurationElement contentTypeBinding : rawContentTypes)
		{
			String contentTypeId = contentTypeBinding.getAttribute(CONTENT_TYPE_ID);
			IContentType type = manager.getContentType(contentTypeId);
			if (type != null)
			{
				contentTypes.add(type);
			}
		}
	}
}
