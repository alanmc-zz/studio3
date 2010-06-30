/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
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

package com.aptana.syncing.ui.internal;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.aptana.ide.syncing.ui.SyncingUIPlugin;
import com.aptana.ide.syncing.ui.internal.SyncPresentationUtils;
import com.aptana.syncing.core.model.ISyncItem;
import com.aptana.syncing.core.model.ISyncItem.Changes;
import com.aptana.syncing.core.model.ISyncItem.Type;

/**
 * @author Max Stepanov
 *
 */
public class SyncViewerLabelProvider extends DecoratingLabelProvider implements ITableLabelProvider {

	private static final Color SYNC_LEFT_COLOR = new Color(null, 188, 242, 255);
	private static final Color SYNC_RIGHT_COLOR = new Color(null, 204, 255, 204);
	private static final Color SYNC_CONFLICT_COLOR = new Color(null, 255, 216, 216);

	private boolean flatMode;

	/**
	 * 
	 */
	public SyncViewerLabelProvider() {
		super(new WorkbenchLabelProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return getImage(element);
        case 1:
        	return getOperationImage((ISyncItem) element);
        default:
            return null;
        }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.DecoratingLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		ISyncItem syncItem = (ISyncItem) element;
		return flatMode ? syncItem.getPath().toPortableString() : super.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getColumnText(Object element, int columnIndex) {
		ISyncItem syncItem = (ISyncItem) element;
        switch (columnIndex) {
        case 0:
        	return getText(element);
        case 2:
        	if (syncItem.getLeftFileInfo().exists()) {
        		if (syncItem.getType() == Type.FILE) {
        			return SyncPresentationUtils.getFileSize(syncItem.getLeftFileInfo());
        		} else if (syncItem.getType() == Type.FOLDER && syncItem.getChanges() == Changes.LEFT_TO_RIGHT) {
        			return getFolderSize(syncItem);
        		}
        	}
        	break;
        case 3:
        	if (syncItem.getRightFileInfo().exists()) {
        		if (syncItem.getType() == Type.FILE) {
        			return SyncPresentationUtils.getFileSize(syncItem.getRightFileInfo());
        		} else if (syncItem.getType() == Type.FOLDER && syncItem.getChanges() == Changes.RIGHT_TO_LEFT) {
        			return getFolderSize(syncItem);
        		}
        	}
        	break;
        case 4:
        	if (syncItem.getType() == Type.FILE && syncItem.getLeftFileInfo().exists()) {
        		return SyncPresentationUtils.getLastModified(syncItem.getLeftFileInfo());
        	}
        	break;
        case 5:
        	if (syncItem.getType() == Type.FILE && syncItem.getRightFileInfo().exists()) {
        		return SyncPresentationUtils.getLastModified(syncItem.getRightFileInfo());
        	}
        	break;
        default:
        	break;
        }
        return ""; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.DecoratingLabelProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element) {
		ISyncItem syncItem = (ISyncItem) element;
		switch (syncItem.getChanges()) {
		case LEFT_TO_RIGHT:
			return SYNC_RIGHT_COLOR;
		case RIGHT_TO_LEFT:
			return SYNC_LEFT_COLOR;
		case CONFLICT:
			return SYNC_CONFLICT_COLOR;
		case NONE:
		default:
			return null;
		}
	}

	private Image getOperationImage(ISyncItem syncItem) {
		switch (syncItem.getOperation()) {
		case LEFT_TO_RIGHT:
			if (!syncItem.getLeftFileInfo().exists()) {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_right_delete.png");
			} else if (!syncItem.getRightFileInfo().exists()) {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_right_add.png");
			} else {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_right.png");
			}
		case RIGHT_TO_LEFT:
			if (!syncItem.getRightFileInfo().exists()) {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_left_delete.png");
			} else if (!syncItem.getLeftFileInfo().exists()) {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_left_add.png");
			} else {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_left.png");
			}
		case NONE:
			if (syncItem.getType() == Type.UNSUPPORTED) {
				return SyncingUIPlugin.getImage("/icons/full/obj16/sync_unsupported.png");				
			}
			return null;
		default:
			return null;
		}
	}
	
	private String getFolderSize(ISyncItem syncItem) {
		ISyncItem[] childItems = syncItem.getChildItems();
		if (childItems == null) {
			return "(unknown)";
		} else if (childItems.length == 0) {
			return "(empty)";
		} else {
			return MessageFormat.format("({0} items)", childItems.length);
		}
	}

	/**
	 * @param flatMode the flatMode to set
	 */
	public void setFlatMode(boolean flatMode) {
		this.flatMode = flatMode;
	}

}
