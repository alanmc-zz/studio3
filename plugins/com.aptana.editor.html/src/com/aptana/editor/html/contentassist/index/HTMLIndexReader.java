/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html.contentassist.index;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aptana.core.util.RegexUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.css.contentassist.index.ICSSIndexConstants;
import com.aptana.editor.html.contentassist.model.AttributeElement;
import com.aptana.editor.html.contentassist.model.ElementElement;
import com.aptana.editor.html.contentassist.model.EntityElement;
import com.aptana.editor.html.contentassist.model.EventElement;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexReader;
import com.aptana.index.core.QueryResult;
import com.aptana.index.core.SearchPattern;

public class HTMLIndexReader extends IndexReader
{
	/**
	 * createAttribute
	 * 
	 * @param attribute
	 * @param key
	 * @return
	 */
	private AttributeElement createAttribute(QueryResult attribute)
	{
		return this.populateElement(new AttributeElement(), attribute, 1);
	}

	/**
	 * createElement
	 * 
	 * @param element
	 * @return
	 */
	private ElementElement createElement(QueryResult element)
	{
		return this.populateElement(new ElementElement(), element, 1);
	}

	/**
	 * createEntity
	 * 
	 * @param entity
	 * @return
	 */
	private EntityElement createEntity(QueryResult entity)
	{
		return this.populateElement(new EntityElement(), entity, 1);
	}

	/**
	 * createEvent
	 * 
	 * @param event
	 * @return
	 */
	private EventElement createEvent(QueryResult event)
	{
		return this.populateElement(new EventElement(), event, 1);
	}

	/**
	 * getAttribute
	 * 
	 * @param index
	 * @param name
	 * @return
	 */
	public List<AttributeElement> getAttribute(Index index, String name)
	{
		List<AttributeElement> result = new ArrayList<AttributeElement>();

		if (index != null && name != null && name.length() > 0)
		{
			List<QueryResult> attributes = index.query( //
					new String[] { IHTMLIndexConstants.ATTRIBUTE }, //
					name + ICSSIndexConstants.DELIMITER, //
					SearchPattern.PREFIX_MATCH //
					);

			if (attributes != null)
			{
				for (QueryResult attribute : attributes)
				{
					result.add(this.createAttribute(attribute));
				}
			}
		}

		return result;
	}

	/**
	 * getAttributePattern
	 * 
	 * @param names
	 * @return
	 */
	private String getAttributePattern(List<String> names)
	{
		String namePattern = RegexUtil.createQuotedListPattern(names);

		return MessageFormat.format("^{1}{0}", this.getDelimiter(), namePattern); //$NON-NLS-1$
	}

	/**
	 * getAttribute
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public List<AttributeElement> getAttributes(Index index, List<String> names)
	{
		List<AttributeElement> result = new ArrayList<AttributeElement>();

		if (index != null && names != null)
		{
			List<QueryResult> attributes = index.query( //
					new String[] { IHTMLIndexConstants.ATTRIBUTE }, //
					this.getAttributePattern(names), //
					SearchPattern.REGEX_MATCH //
					);

			if (attributes != null)
			{
				for (QueryResult attribute : attributes)
				{
					result.add(this.createAttribute(attribute));
				}
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.index.core.IndexReader#getDelimiter()
	 */
	@Override
	protected String getDelimiter()
	{
		return IHTMLIndexConstants.DELIMITER;
	}

	/**
	 * getElements
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<ElementElement> getElements(Index index)
	{
		List<ElementElement> result = new ArrayList<ElementElement>();

		if (index != null)
		{
			List<QueryResult> elements = index.query( //
					new String[] { IHTMLIndexConstants.ELEMENT }, //
					"*", //$NON-NLS-1$
					SearchPattern.PATTERN_MATCH //
					);

			if (elements != null)
			{
				for (QueryResult element : elements)
				{
					result.add(this.createElement(element));
				}
			}
		}

		return result;
	}

	/**
	 * getElement
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public List<ElementElement> getElements(Index index, String... names)
	{
		List<ElementElement> result = new ArrayList<ElementElement>();

		if (index != null && names != null)
		{
			for (String name : names)
			{
				List<QueryResult> elements = index.query( //
						new String[] { IHTMLIndexConstants.ELEMENT }, //
						name + ICSSIndexConstants.DELIMITER, //
						SearchPattern.PREFIX_MATCH //
						);

				if (elements != null)
				{
					for (QueryResult element : elements)
					{
						result.add(this.createElement(element));
					}
				}
			}
		}

		return result;
	}

	/**
	 * getEntities
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<EntityElement> getEntities(Index index)
	{
		List<EntityElement> result = new ArrayList<EntityElement>();

		if (index != null)
		{
			List<QueryResult> entities = index.query( //
					new String[] { IHTMLIndexConstants.ENTITY }, //
					"*", //$NON-NLS-1$
					SearchPattern.PATTERN_MATCH //
					);

			if (entities != null)
			{
				for (QueryResult entity : entities)
				{
					result.add(this.createEntity(entity));
				}
			}
		}

		return result;
	}

	/**
	 * getEntity
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public EntityElement getEntity(Index index, String name)
	{
		EntityElement result = null;

		if (index != null)
		{
			List<QueryResult> entities = index.query( //
					new String[] { IHTMLIndexConstants.ENTITY }, //
					name + ICSSIndexConstants.DELIMITER, //
					SearchPattern.PREFIX_MATCH //
					);

			if (entities != null)
			{
				for (QueryResult entity : entities)
				{
					result = this.createEntity(entity);

					// there should only be one match
					break;
				}
			}
		}

		return result;
	}

	/**
	 * getEvent
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public EventElement getEvent(Index index, String name)
	{
		EventElement result = null;

		if (index != null)
		{
			List<QueryResult> events = index.query( //
					new String[] { IHTMLIndexConstants.EVENT }, //
					name + ICSSIndexConstants.DELIMITER, //
					SearchPattern.PREFIX_MATCH //
					);

			if (events != null)
			{
				for (QueryResult event : events)
				{
					result = this.createEvent(event);

					// there should only be one match
					break;
				}
			}
		}

		return result;
	}

	/**
	 * getEvents
	 * 
	 * @param index
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public List<EventElement> getEvents(Index index, List<String> names)
	{
		List<EventElement> result = new ArrayList<EventElement>();

		if (index != null && names != null)
		{
			List<QueryResult> events = index.query( //
					new String[] { IHTMLIndexConstants.EVENT }, //
					this.getAttributePattern(names), //
					SearchPattern.REGEX_MATCH //
					);

			if (events != null)
			{
				for (QueryResult event : events)
				{
					result.add(this.createEvent(event));
				}
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.index.core.IndexReader#getSubDelimiter()
	 */
	@Override
	protected String getSubDelimiter()
	{
		return IHTMLIndexConstants.SUB_DELIMITER;
	}

	/**
	 * getValues
	 * 
	 * @return
	 */
	public Map<String, String> getValues(Index index, String category)
	{
		Map<String, String> result = null;

		if (index != null && !StringUtil.isEmpty(category))
		{
			String pattern = "*"; //$NON-NLS-1$
			List<QueryResult> items = index.query(new String[] { category }, pattern, SearchPattern.PATTERN_MATCH);
			if (items != null && items.size() > 0)
			{
				result = new HashMap<String, String>();

				for (QueryResult item : items)
				{
					Set<String> paths = item.getDocuments();
					String path = (paths != null && !paths.isEmpty()) ? paths.iterator().next() : ""; //$NON-NLS-1$

					try
					{
						URI uri = index.getRelativeDocumentPath(new URI(path));

						result.put(item.getWord(), uri.toString());
					}
					catch (URISyntaxException e)
					{
						result.put(item.getWord(), path);
					}
				}
			}
		}

		return result;
	}
}
