/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.mortbay.util.ajax.JSON.Convertible;
import org.mortbay.util.ajax.JSON.Output;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.SourcePrinter;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.contentassist.UserAgentManager;
import com.aptana.editor.common.contentassist.UserAgentManager.UserAgent;
import com.aptana.editor.js.JSPlugin;
import com.aptana.index.core.IndexDocument;
import com.aptana.index.core.IndexUtil;
import com.aptana.index.core.ui.views.IPropertyInformation;

public abstract class BaseElement<P extends Enum<P> & IPropertyInformation<? extends BaseElement<P>>> implements
		Convertible, IndexDocument, IPropertySource
{
	private static final String USER_AGENTS_PROPERTY = "userAgents"; //$NON-NLS-1$
	private static final String SINCE_PROPERTY = "since"; //$NON-NLS-1$
	private static final String DESCRIPTION_PROPERTY = "description"; //$NON-NLS-1$
	private static final String NAME_PROPERTY = "name"; //$NON-NLS-1$

	// A special instance used to indicate that this element should associate all user agents with it
	private static final Set<UserAgentElement> ALL_USER_AGENTS = Collections.emptySet();

	private String _name;
	private String _description;
	private Set<UserAgentElement> _userAgents;
	private List<SinceElement> _sinceList;
	private List<String> _documents;

	/**
	 * addDocument
	 * 
	 * @param document
	 */
	public void addDocument(String document)
	{
		if (document != null && document.length() > 0)
		{
			if (this._documents == null)
			{
				this._documents = new ArrayList<String>();
			}

			this._documents.add(document);
		}
	}

	/**
	 * addSince
	 * 
	 * @param since
	 */
	public void addSince(SinceElement since)
	{
		if (since != null)
		{
			if (this._sinceList == null)
			{
				this._sinceList = new ArrayList<SinceElement>();
			}

			this._sinceList.add(since);
		}
	}

	/**
	 * addUserAgent
	 * 
	 * @param userAgent
	 */
	public void addUserAgent(UserAgentElement userAgent)
	{
		if (userAgent != null)
		{
			if (this._userAgents == ALL_USER_AGENTS)
			{
				// grab the expanded set of all user agents
				Set<UserAgentElement> userAgents = new HashSet<UserAgentElement>(this.getUserAgents());

				// if the specified user agent exists in the expanded list, then don't do anything. Otherwise, we need
				// to generate the union of the expanded list and the specified user agent
				if (!userAgents.contains(userAgent))
				{
					this._userAgents = userAgents;
					this._userAgents.add(userAgent);
				}
			}
			else if (this._userAgents == null)
			{
				this._userAgents = new HashSet<UserAgentElement>();
				this._userAgents.add(userAgent);
			}
			else
			{
				this._userAgents.add(userAgent);
			}
		}
	}

	/**
	 * createUserAgentSet
	 * 
	 * @param object
	 * @return
	 */
	protected Set<UserAgentElement> createUserAgentSet(Object object)
	{
		Set<UserAgentElement> result = null;

		if (object != null && object.getClass().isArray())
		{
			Object[] objects = (Object[]) object;

			if (objects.length > 0)
			{
				result = new HashSet<UserAgentElement>();

				for (Object value : objects)
				{
					if (value instanceof Map)
					{
						UserAgentElement userAgent = UserAgentElement.createUserAgentElement((Map<?, ?>) value);

						result.add(userAgent);
					}
				}
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mortbay.util.ajax.JSON.Convertible#fromJSON(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	public void fromJSON(Map object)
	{
		this.setName(StringUtil.getStringValue(object.get(NAME_PROPERTY)));
		this.setDescription(StringUtil.getStringValue(object.get(DESCRIPTION_PROPERTY)));
		this._sinceList = IndexUtil.createList(object.get(SINCE_PROPERTY), SinceElement.class);

		Object userAgentsProperty = object.get(USER_AGENTS_PROPERTY);

		if (userAgentsProperty == null)
		{
			this._userAgents = ALL_USER_AGENTS;
		}
		else
		{
			this._userAgents = createUserAgentSet(userAgentsProperty);
		}
	}

	/**
	 * getDescription
	 * 
	 * @return
	 */
	public String getDescription()
	{
		return StringUtil.getStringValue(this._description);
	}

	/**
	 * getDocuments
	 * 
	 * @return
	 */
	public List<String> getDocuments()
	{
		return CollectionsUtil.getListValue(this._documents);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getEditableValue()
	 */
	public Object getEditableValue()
	{
		return null;
	}

	/**
	 * getName
	 * 
	 * @return
	 */
	public String getName()
	{
		return StringUtil.getStringValue(this._name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		List<IPropertyDescriptor> result = new ArrayList<IPropertyDescriptor>();

		for (P p : getPropertyInfoSet())
		{
			PropertyDescriptor descriptor = new PropertyDescriptor(p, p.getHeader());
			String category = p.getCategory();

			if (!StringUtil.isEmpty(category))
			{
				descriptor.setCategory(category);
			}

			result.add(descriptor);
		}

		return result.toArray(new IPropertyDescriptor[result.size()]);
	}

	/**
	 * getPropertyInfoSet
	 * 
	 * @return Set
	 */
	protected Set<P> getPropertyInfoSet()
	{
		return Collections.emptySet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public Object getPropertyValue(Object id)
	{
		Object result = null;

		if (id instanceof IPropertyInformation)
		{
			result = ((IPropertyInformation<BaseElement<P>>) id).getPropertyValue(this);
		}

		return result;
	}

	/**
	 * getSinceList
	 * 
	 * @return
	 */
	public List<SinceElement> getSinceList()
	{
		return CollectionsUtil.getListValue(this._sinceList);
	}

	/**
	 * getUserAgentNames
	 * 
	 * @return
	 */
	public List<String> getUserAgentNames()
	{
		List<String> result = new ArrayList<String>();

		for (UserAgentElement userAgent : this.getUserAgents())
		{
			result.add(userAgent.getPlatform());
		}

		return result;
	}

	/**
	 * getUserAgents
	 * 
	 * @return
	 */
	public List<UserAgentElement> getUserAgents()
	{
		Set<UserAgentElement> userAgents;

		if (_userAgents == ALL_USER_AGENTS)
		{
			userAgents = new HashSet<UserAgentElement>();

			for (UserAgent userAgent : UserAgentManager.getInstance().getAllUserAgents())
			{
				userAgents.add(UserAgentElement.createUserAgentElement(userAgent.ID));
			}
		}
		else
		{
			userAgents = _userAgents;
		}

		return new ArrayList<UserAgentElement>(CollectionsUtil.getSetValue(userAgents));
	}

	/**
	 * A predicate used to determine if this element has been tagged to use all user agents. Note that this will return
	 * true only if setHasAllUserAgents has been called previously. If user agents have been added to this element and
	 * they so happen to be equivalent to a set of all user agents, this method will still return false.
	 * 
	 * @return
	 */
	public boolean hasAllUserAgents()
	{
		return _userAgents == ALL_USER_AGENTS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#isPropertySet(java.lang.Object)
	 */
	public boolean isPropertySet(Object id)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
	 */
	public void resetPropertyValue(Object id)
	{
	}

	/**
	 * setDescription
	 * 
	 * @param description
	 */
	public void setDescription(String description)
	{
		this._description = description;
	}

	/**
	 * setHasAllUserAgents
	 */
	public void setHasAllUserAgents()
	{
		if (_userAgents != ALL_USER_AGENTS)
		{
			if (!CollectionsUtil.isEmpty(_userAgents))
			{
				IdeLog.logWarning(JSPlugin.getDefault(),
						"User agents may have been deleted when setting element to use all user agents: " + toSource());
			}

			_userAgents = ALL_USER_AGENTS;
		}
	}

	/**
	 * setName
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
		this._name = name;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object, java.lang.Object)
	 */
	public void setPropertyValue(Object id, Object value)
	{
	}

	/*
	 * (non-Javadoc)
	 * @see org.mortbay.util.ajax.JSON.Convertible#toJSON(org.mortbay.util.ajax.JSON.Output)
	 */
	public void toJSON(Output out)
	{
		out.add(NAME_PROPERTY, this.getName());
		out.add(DESCRIPTION_PROPERTY, this.getDescription());
		out.add(SINCE_PROPERTY, this.getSinceList());

		if (hasAllUserAgents())
		{
			// NOTE: use 'null' to indicate that all user agents should be associated with this element
			out.add(USER_AGENTS_PROPERTY, null);
		}
		else
		{
			out.add(USER_AGENTS_PROPERTY, this.getUserAgents());
		}
	}

	/**
	 * toSource
	 * 
	 * @return
	 */
	public String toSource()
	{
		SourcePrinter printer = new SourcePrinter();

		this.toSource(printer);

		return printer.toString();
	}

	/**
	 * toSource
	 * 
	 * @param printer
	 */
	public void toSource(SourcePrinter printer)
	{
		// Subclasses need to override this method
	}
}
