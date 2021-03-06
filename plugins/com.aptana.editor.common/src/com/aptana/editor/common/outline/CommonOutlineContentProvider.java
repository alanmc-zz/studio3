/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.aptana.core.util.ArrayUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.resolver.IPathResolver;
import com.aptana.parsing.ast.IParseNode;

public class CommonOutlineContentProvider implements ITreeContentProvider
{

	protected static final Object[] EMPTY = ArrayUtil.NO_OBJECTS;
	protected IPathResolver resolver;

	public CommonOutlineItem getOutlineItem(IParseNode node)
	{
		if (node == null)
		{
			return null;
		}
		return new CommonOutlineItem(node.getNameNode().getNameRange(), node);
	}

	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof AbstractThemeableEditor)
		{
			IParseNode rootNode = ((AbstractThemeableEditor) parentElement).getAST();
			if (rootNode != null)
			{
				return filter(rootNode.getChildren());
			}
		}
		else if (parentElement instanceof IParseNode)
		{
			return filter(((IParseNode) parentElement).getChildren());
		}
		else if (parentElement instanceof CommonOutlineItem)
		{
			// delegates to the parse node it references to
			return getChildren(((CommonOutlineItem) parentElement).getReferenceNode());
		}
		return EMPTY;
	}

	public Object getParent(Object element)
	{
		if (element instanceof IParseNode)
		{
			return ((IParseNode) element).getParent();
		}
		if (element instanceof CommonOutlineItem)
		{
			IParseNode node = ((CommonOutlineItem) element).getReferenceNode();
			return getOutlineItem(node.getParent());
		}
		return null;
	}

	public boolean hasChildren(Object element)
	{
		return getChildren(element).length > 0;
	}

	public Object[] getElements(Object inputElement)
	{
		return getChildren(inputElement);
	}

	public void dispose()
	{
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		boolean isCU = (newInput instanceof AbstractThemeableEditor);
		if (isCU)
		{
			final AbstractThemeableEditor editor = (AbstractThemeableEditor) newInput;
			this.resolver = PathResolverProvider.getResolver(editor.getEditorInput());
		}
		else
		{
			AbstractThemeableEditor editor = (AbstractThemeableEditor) oldInput;
			this.resolver = PathResolverProvider.getResolver(editor.getEditorInput());
		}
	}

	/**
	 * Subclass could override to return a specific list from the result.
	 * 
	 * @param nodes
	 *            the array containing the parse result
	 * @return the specific top level objects to display
	 */
	protected Object[] filter(IParseNode[] nodes)
	{
		return nodes;
	}
}
