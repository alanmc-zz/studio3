/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.css.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;

import com.aptana.editor.common.ExtendedFastPartitioner;
import com.aptana.editor.css.CSSSourceConfiguration;
import com.aptana.editor.css.CSSSourceEditor;
import com.aptana.editor.css.CSSSourcePartitionScanner;
import com.aptana.editor.css.CSSSourceViewerConfiguration;

/**
 * @author cwilliams
 */
public class CSSMergeViewer extends TextMergeViewer
{
	public CSSMergeViewer(Composite parent, CompareConfiguration configuration)
	{
		super(parent, configuration);
	}

	@Override
	protected IDocumentPartitioner getDocumentPartitioner()
	{
		IDocumentPartitioner partitioner = new ExtendedFastPartitioner(new CSSSourcePartitionScanner(),
				CSSSourceConfiguration.getDefault().getContentTypes());
		return partitioner;
	}

	@Override
	protected String getDocumentPartitioning()
	{
		return IDocumentExtension3.DEFAULT_PARTITIONING;
	}

	@Override
	protected void configureTextViewer(TextViewer textViewer)
	{
		if (textViewer instanceof SourceViewer)
		{
			SourceViewer sourceViewer = (SourceViewer) textViewer;
			sourceViewer.unconfigure();
			IPreferenceStore preferences = CSSSourceEditor.getChainedPreferenceStore();
			CSSSourceViewerConfiguration config = new CSSSourceViewerConfiguration(preferences, null);
			sourceViewer.configure(config);
		}
	}
}
