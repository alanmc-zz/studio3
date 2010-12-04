/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.ui.io.navigator;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IBaseRemoteConnectionPoint;
import com.aptana.ide.core.io.IConnectionPoint;

/**
 * @author Michael Xia (mxia@aptana.com)
 */
public class ConnectionPointLabelDecorator implements ILabelDecorator
{

	public void addListener(ILabelProviderListener listener)
	{
	}

	public void dispose()
	{
	}

	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	public void removeListener(ILabelProviderListener listener)
	{
	}

	public Image decorateImage(Image image, Object element)
	{
		return null;
	}

	public String decorateText(String text, Object element)
	{
		if (element instanceof IBaseRemoteConnectionPoint)
		{
			IBaseRemoteConnectionPoint currentConnection = (IBaseRemoteConnectionPoint) element;
			String currentName = currentConnection.getName();
			if (currentName == null)
			{
				return text;
			}
			IPath currentPath = currentConnection.getPath();
			if (Path.ROOT.equals(currentPath))
			{
				return text;
			}

			IConnectionPoint[] connections = CoreIOPlugin.getConnectionPointManager().getConnectionPoints();
			for (IConnectionPoint connection : connections)
			{
				if (connection != currentConnection && connection instanceof IBaseRemoteConnectionPoint
						&& currentName.equals(connection.getName()))
				{
					// there are remote connections with the same name, so adds the compressed path to distinguish
					String decoratedText = null;
					IPath path = ((IBaseRemoteConnectionPoint) connection).getPath();
					int count = currentPath.segmentCount();
					for (int i = 0; i < count; ++i)
					{
						// finds the first segment in the path that does not match
						if (!currentPath.segment(i).equals(path.segment(i)))
						{
							decoratedText = currentPath.removeFirstSegments(i).toPortableString();
							if (i > 0)
							{
								decoratedText = ".../" + decoratedText; //$NON-NLS-1$
							}
							break;
						}
					}
					return MessageFormat.format("{0} ({1})", text, //$NON-NLS-1$
							decoratedText == null ? currentPath.toPortableString() : decoratedText);
				}
			}
		}
		return text;
	}
}