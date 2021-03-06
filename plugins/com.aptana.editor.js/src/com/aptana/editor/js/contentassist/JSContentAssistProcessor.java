/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import beaver.Scanner;

import com.aptana.core.IFilter;
import com.aptana.core.util.ArrayUtil;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonContentAssistProcessor;
import com.aptana.editor.common.contentassist.CommonCompletionProposal;
import com.aptana.editor.common.contentassist.ILexemeProvider;
import com.aptana.editor.common.contentassist.UserAgentManager;
import com.aptana.editor.js.IJSConstants;
import com.aptana.editor.js.JSLanguageConstants;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.JSSourceConfiguration;
import com.aptana.editor.js.JSTypeConstants;
import com.aptana.editor.js.contentassist.index.IJSIndexConstants;
import com.aptana.editor.js.contentassist.model.FunctionElement;
import com.aptana.editor.js.contentassist.model.ParameterElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.inferencing.JSPropertyCollection;
import com.aptana.editor.js.inferencing.JSScope;
import com.aptana.editor.js.parsing.JSFlexLexemeProvider;
import com.aptana.editor.js.parsing.JSFlexScanner;
import com.aptana.editor.js.parsing.JSParseState;
import com.aptana.editor.js.parsing.ast.IJSNodeTypes;
import com.aptana.editor.js.parsing.ast.JSArgumentsNode;
import com.aptana.editor.js.parsing.ast.JSFunctionNode;
import com.aptana.editor.js.parsing.ast.JSGetPropertyNode;
import com.aptana.editor.js.parsing.ast.JSNode;
import com.aptana.editor.js.parsing.ast.JSObjectNode;
import com.aptana.editor.js.parsing.lexer.JSTokenType;
import com.aptana.index.core.Index;
import com.aptana.parsing.ParserPoolFactory;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.lexer.IRange;
import com.aptana.parsing.lexer.Lexeme;

public class JSContentAssistProcessor extends CommonContentAssistProcessor
{
	/**
	 * This class is used via {@link CollectionsUtil#filter(Collection, IFilter)} to remove duplicate proposals based on
	 * display names. Duplicate proposals are merged into a single entry
	 */
	public class ProposalMerger implements IFilter<ICompletionProposal>
	{
		private ICompletionProposal lastProposal = null;

		public boolean include(ICompletionProposal item)
		{
			boolean result;

			if (lastProposal == null || !lastProposal.getDisplayString().equals(item.getDisplayString()))
			{
				result = true;
				lastProposal = item;
			}
			else
			{
				result = false;
				// TODO: merge proposal with last proposal
			}

			return result;
		}
	}

	private static final Image JS_FUNCTION = JSPlugin.getImage("/icons/js_function.png"); //$NON-NLS-1$
	private static final Image JS_PROPERTY = JSPlugin.getImage("/icons/js_property.png"); //$NON-NLS-1$
	private static final Image JS_KEYWORD = JSPlugin.getImage("/icons/keyword.png"); //$NON-NLS-1$
	private static final IFilter<PropertyElement> isVisibleFilter = new IFilter<PropertyElement>()
	{
		public boolean include(PropertyElement item)
		{
			return !item.isInternal();
		}
	};

	// @formatter:off
	private static String[] KEYWORDS = ArrayUtil.flatten(
		JSLanguageConstants.KEYWORD_OPERATORS,
		JSLanguageConstants.GRAMMAR_KEYWORDS,
		JSLanguageConstants.KEYWORD_CONTROL
	);
	// @formatter:on

	private static Set<String> AUTO_ACTIVATION_PARTITION_TYPES;
	{
		AUTO_ACTIVATION_PARTITION_TYPES = new HashSet<String>();
		AUTO_ACTIVATION_PARTITION_TYPES.add(JSSourceConfiguration.DEFAULT);
		AUTO_ACTIVATION_PARTITION_TYPES.add(IDocument.DEFAULT_CONTENT_TYPE);
	}

	private JSIndexQueryHelper indexHelper;
	private IParseNode targetNode;
	private IParseNode statementNode;
	private IRange replaceRange;
	private IRange activeRange;

	/**
	 * JSIndexContentAssistProcessor
	 * 
	 * @param editor
	 */
	public JSContentAssistProcessor(AbstractThemeableEditor editor)
	{
		super(editor);

		indexHelper = new JSIndexQueryHelper();
	}

	/**
	 * JSContentAssistProcessor
	 * 
	 * @param editor
	 * @param activeRange
	 */
	public JSContentAssistProcessor(AbstractThemeableEditor editor, IRange activeRange)
	{
		this(editor);

		this.activeRange = activeRange;
	}

	/**
	 * addCoreGlobals
	 * 
	 * @param proposals
	 * @param offset
	 */
	private void addCoreGlobals(Set<ICompletionProposal> proposals, int offset)
	{
		List<PropertyElement> globals = indexHelper.getCoreGlobals();

		if (globals != null)
		{
			URI projectURI = getProjectURI();
			String location = IJSIndexConstants.CORE;

			for (PropertyElement property : CollectionsUtil.filter(globals, isVisibleFilter))
			{
				String name = property.getName();
				String description = JSModelFormatter.getDescription(property, projectURI);
				Image image = JSModelFormatter.getImage(property);
				String[] userAgents = property.getUserAgentNames().toArray(new String[0]);

				addProposal(proposals, name, image, description, userAgents, location, offset);
			}
		}
	}

	/**
	 * @param prefix
	 * @param completionProposals
	 */
	private void addKeywords(Set<ICompletionProposal> proposals, int offset)
	{
		for (String name : KEYWORDS)
		{
			String description = StringUtil.format(Messages.JSContentAssistProcessor_KeywordDescription, name);
			addProposal(proposals, name, JS_KEYWORD, description, getActiveUserAgentIds(),
					Messages.JSContentAssistProcessor_KeywordLocation, offset);
		}
	}

	/**
	 * addObjectLiteralProperties
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addObjectLiteralProperties(Set<ICompletionProposal> proposals, ITextViewer viewer, int offset)
	{
		FunctionElement function = getFunctionElement(viewer, offset);

		if (function != null)
		{
			List<ParameterElement> params = function.getParameters();
			int index = getArgumentIndex(offset);

			if (0 <= index && index < params.size())
			{
				ParameterElement param = params.get(index);

				for (String type : param.getTypes())
				{
					List<PropertyElement> properties = indexHelper.getTypeProperties(getIndex(), type);

					for (PropertyElement property : CollectionsUtil.filter(properties, isVisibleFilter))
					{
						String name = property.getName();
						String description = JSModelFormatter.getDescription(property, getProjectURI());
						Image image = JSModelFormatter.getImage(property);
						List<String> userAgentNameList = property.getUserAgentNames();
						String[] userAgentNames = userAgentNameList.toArray(new String[userAgentNameList.size()]);
						String owningType = JSModelFormatter.getTypeDisplayName(property.getOwningType());

						addProposal(proposals, name, image, description, userAgentNames, owningType, offset);
					}
				}
			}
		}
	}

	/**
	 * addProjectGlobalFunctions
	 * 
	 * @param proposals
	 * @param offset
	 */
	private void addProjectGlobals(Set<ICompletionProposal> proposals, int offset)
	{
		List<PropertyElement> projectGlobals = indexHelper.getProjectGlobals(getIndex());

		if (projectGlobals != null && !projectGlobals.isEmpty())
		{
			String[] userAgentNames = getActiveUserAgentIds();
			URI projectURI = getProjectURI();

			for (PropertyElement property : CollectionsUtil.filter(projectGlobals, isVisibleFilter))
			{
				String name = property.getName();
				String description = JSModelFormatter.getDescription(property, projectURI);
				Image image = JSModelFormatter.getImage(property);
				List<String> documents = property.getDocuments();
				String location = (documents != null && documents.size() > 0) ? JSModelFormatter
						.getDocumentDisplayName(documents.get(0)) : null;

				addProposal(proposals, name, image, description, userAgentNames, location, offset);
			}
		}
	}

	/**
	 * addProperties
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addProperties(Set<ICompletionProposal> proposals, int offset)
	{
		JSGetPropertyNode node = ParseUtil.getGetPropertyNode(targetNode, statementNode);
		List<String> types = getParentObjectTypes(node, offset);

		// add all properties of each type to our proposal list
		for (String type : types)
		{
			addTypeProperties(proposals, type, offset);
		}
	}

	/**
	 * addProposal - The display name is used as the insertion text
	 * 
	 * @param proposals
	 * @param name
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param fileLocation
	 * @param offset
	 */
	private void addProposal(Set<ICompletionProposal> proposals, String name, Image image, String description,
			String[] userAgentIds, String fileLocation, int offset)
	{
		addProposal(proposals, name, name, image, description, userAgentIds, fileLocation, offset);
	}

	/**
	 * addProposal - The display name and insertion text are defined separately
	 * 
	 * @param proposals
	 * @param displayName
	 * @param insertionText
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param fileLocation
	 * @param offset
	 */
	private void addProposal(Set<ICompletionProposal> proposals, String displayName, String insertionText, Image image,
			String description, String[] userAgentIds, String fileLocation, int offset)
	{
		if (isActiveByUserAgent(userAgentIds))
		{
			int length = insertionText.length();

			// calculate what text will be replaced
			int replaceLength = 0;

			if (replaceRange != null)
			{
				offset = replaceRange.getStartingOffset(); // $codepro.audit.disable questionableAssignment
				replaceLength = replaceRange.getLength();
			}

			// build proposal
			IContextInformation contextInfo = null;
			Image[] userAgents = UserAgentManager.getInstance().getUserAgentImages(getNatureIds(), userAgentIds);

			CommonCompletionProposal proposal = new CommonCompletionProposal(insertionText, offset, replaceLength,
					length, image, displayName, contextInfo, description);
			proposal.setFileLocation(fileLocation);
			proposal.setUserAgentImages(userAgents);
			proposal.setTriggerCharacters(getProposalTriggerCharacters());

			// add the proposal to the list
			proposals.add(proposal);
		}
	}

	/**
	 * addSymbolsInScope
	 * 
	 * @param proposals
	 */
	protected void addSymbolsInScope(Set<ICompletionProposal> proposals, int offset)
	{
		if (targetNode != null)
		{
			JSScope globalScope = ParseUtil.getGlobalScope(targetNode);

			if (globalScope != null)
			{
				JSScope localScope = globalScope.getScopeAtOffset(offset);
				String fileLocation = getFilename();
				String[] userAgentNames = getActiveUserAgentIds();

				while (localScope != null && localScope != globalScope)
				{
					List<String> symbols = localScope.getLocalSymbolNames();

					for (String symbol : symbols)
					{
						boolean isFunction = false;
						JSPropertyCollection object = localScope.getLocalSymbol(symbol);
						List<JSNode> nodes = object.getValues();

						if (nodes != null)
						{
							for (JSNode node : nodes)
							{
								if (node instanceof JSFunctionNode)
								{
									isFunction = true;
									break;
								}
							}
						}

						String name = symbol;
						String description = null;
						Image image = (isFunction) ? JS_FUNCTION : JS_PROPERTY;

						addProposal(proposals, name, image, description, userAgentNames, fileLocation, offset);
					}

					localScope = localScope.getParentScope();
				}
			}
		}
	}

	/**
	 * addTypeProperties
	 * 
	 * @param proposals
	 * @param typeName
	 * @param offset
	 */
	protected void addTypeProperties(Set<ICompletionProposal> proposals, String typeName, int offset)
	{
		Index index = getIndex();

		// grab all ancestors of the specified type
		List<String> allTypes = indexHelper.getTypeAncestorNames(index, typeName);

		// include the type in the list as well
		allTypes.add(0, typeName);

		// add properties and methods
		List<PropertyElement> properties = indexHelper.getTypeMembers(index, allTypes);

		for (PropertyElement property : CollectionsUtil.filter(properties, isVisibleFilter))
		{
			String name = property.getName();
			String description = JSModelFormatter.getDescription(property, getProjectURI());
			Image image = JSModelFormatter.getImage(property);
			List<String> userAgentNameList = property.getUserAgentNames();
			String[] userAgentNames = userAgentNameList.toArray(new String[userAgentNameList.size()]);
			String owningType = JSModelFormatter.getTypeDisplayName(property.getOwningType());

			addProposal(proposals, name, image, description, userAgentNames, owningType, offset);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.common.CommonContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer
	 * , int)
	 */
	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset)
	{
		List<IContextInformation> result = new ArrayList<IContextInformation>();
		FunctionElement function = getFunctionElement(viewer, offset);

		if (function != null)
		{
			JSArgumentsNode node = getArgumentsNode(offset);

			if (node != null)
			{
				boolean inObjectLiteral = false;

				// find argument we're in
				for (IParseNode arg : node)
				{
					if (arg.contains(offset))
					{
						// Not foolproof, but this should cover 99% of the cases we're likely to encounter
						inObjectLiteral = (arg instanceof JSObjectNode);
						break;
					}
				}

				// prevent context info popup from appearing and immediately disappearing
				if (!inObjectLiteral)
				{
					String info = JSModelFormatter.getContextInfo(function);
					List<String> lines = JSModelFormatter.getContextLines(function);
					IContextInformation ci = new JSContextInformation(info, lines, node.getStartingOffset());

					result.add(ci);
				}
			}
		}

		return result.toArray(new IContextInformation[result.size()]);
	}

	/**
	 * createLexemeProvider
	 * 
	 * @param document
	 * @param offset
	 * @return
	 */
	ILexemeProvider<JSTokenType> createLexemeProvider(IDocument document, int offset)
	{
		Scanner scanner = new JSFlexScanner();
		ILexemeProvider<JSTokenType> result;

		// NOTE: use active range temporarily until we get proper partitions for JS inside of HTML
		if (activeRange != null)
		{
			result = new JSFlexLexemeProvider(document, activeRange, scanner);
		}
		else if (statementNode != null)
		{
			result = new JSFlexLexemeProvider(document, statementNode, scanner);
		}
		else
		{
			result = new JSFlexLexemeProvider(document, offset, scanner);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.common.CommonContentAssistProcessor#doComputeCompletionProposals(org.eclipse.jface.text.ITextViewer
	 * , int, char, boolean)
	 */
	@Override
	protected ICompletionProposal[] doComputeCompletionProposals(ITextViewer viewer, int offset, char activationChar,
			boolean autoActivated)
	{
		// NOTE: Using a linked hash set to preserve add-order. We need this in case we end up filtering proposals. This
		// will give precedence to the first of a collection of proposals with like names
		Set<ICompletionProposal> result = new LinkedHashSet<ICompletionProposal>();

		// grab document
		IDocument document = viewer.getDocument();

		// determine the content assist location type
		LocationType location = getLocation(document, offset);

		// process the resulting location
		switch (location)
		{
			case IN_PROPERTY_NAME:
				addProperties(result, offset);
				break;

			case IN_VARIABLE_NAME:
			case IN_GLOBAL:
			case IN_CONSTRUCTOR:
				addKeywords(result, offset);
				addCoreGlobals(result, offset);
				addProjectGlobals(result, offset);
				// addSymbolsInScope(result, offset);
				break;

			case IN_OBJECT_LITERAL_PROPERTY:
				addObjectLiteralProperties(result, viewer, offset);
				break;

			default:
				break;
		}

		// merge and remove duplicates from the proposal list
		List<ICompletionProposal> filteredProposalList = getMergedProposals(new ArrayList<ICompletionProposal>(result));
		ICompletionProposal[] resultList = filteredProposalList.toArray(new ICompletionProposal[filteredProposalList
				.size()]);

		// select the current proposal based on the prefix
		if (replaceRange != null)
		{
			try
			{
				String prefix = document.get(replaceRange.getStartingOffset(), replaceRange.getLength());

				setSelectedProposal(prefix, resultList);
			}
			catch (BadLocationException e) // $codepro.audit.disable emptyCatchClause
			{
				// ignore
			}
		}

		return resultList;
	}

	/**
	 * getActiveASTNode
	 * 
	 * @param offset
	 * @return
	 */
	IParseNode getActiveASTNode(int offset)
	{
		IParseNode result = null;

		try
		{
			// grab document
			IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

			// grab source which is either the whole document for JS files or a subset for nested JS
			// @formatter:off
			String source =
				(activeRange != null)
					? doc.get(activeRange.getStartingOffset(), activeRange.getLength())
					: doc.get();
			// @formatter:on
			int startingOffset = (activeRange != null) ? activeRange.getStartingOffset() : 0;

			// create parse state and turn off all processing of comments
			JSParseState parseState = new JSParseState();
			parseState.setEditState(source, startingOffset);
			parseState.setAttachComments(false);
			parseState.setCollectComments(false);

			// parse and grab resulting AST
			IParseNode ast = ParserPoolFactory.parse(IJSConstants.CONTENT_TYPE_JS, parseState);

			if (ast != null)
			{
				result = ast.getNodeAtOffset(offset);

				// We won't get a current node if the cursor is outside of the positions
				// recorded by the AST
				if (result == null)
				{
					if (offset < ast.getStartingOffset())
					{
						result = ast.getNodeAtOffset(ast.getStartingOffset());
					}
					else if (ast.getEndingOffset() < offset)
					{
						result = ast.getNodeAtOffset(ast.getEndingOffset());
					}
				}
			}
		}
		catch (Exception e)
		{
			// ignore parse error exception since the user will get markers and/or entries in the Problems View
		}

		return result;
	}

	/**
	 * getArgumentIndex
	 * 
	 * @param offset
	 * @return
	 */
	private int getArgumentIndex(int offset)
	{
		JSArgumentsNode arguments = getArgumentsNode(offset);
		int result = -1;

		if (arguments != null)
		{
			for (IParseNode child : arguments)
			{
				if (child.contains(offset))
				{
					result = child.getIndex();
					break;
				}
			}
		}

		return result;
	}

	/**
	 * getArgumentsNode
	 * 
	 * @param offset
	 * @return
	 */
	private JSArgumentsNode getArgumentsNode(int offset)
	{
		IParseNode node = getActiveASTNode(offset);
		JSArgumentsNode result = null;

		// work a way up the AST to determine if we're in an arguments node
		while (node instanceof JSNode && node.getNodeType() != IJSNodeTypes.ARGUMENTS)
		{
			node = node.getParent();
		}

		// process arguments node as long as we're not to the left of the opening parenthesis
		if (node instanceof JSNode && node.getNodeType() == IJSNodeTypes.ARGUMENTS
				&& node.getStartingOffset() != offset)
		{
			result = (JSArgumentsNode) node;
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#getContextInformationValidator()
	 */
	@Override
	public IContextInformationValidator getContextInformationValidator()
	{
		return new JSContextInformationValidator();
	}

	/**
	 * getFunctionElement
	 * 
	 * @param viewer
	 * @param offset
	 * @return
	 */
	private FunctionElement getFunctionElement(ITextViewer viewer, int offset)
	{
		JSArgumentsNode node = getArgumentsNode(offset);
		FunctionElement result = null;

		// process arguments node as long as we're not to the left of the opening parenthesis
		if (node != null)
		{
			// save current replace range. A bit hacky but better than adding a flag into getLocation's signature
			IRange range = replaceRange;

			// grab the content assist location type for the symbol before the arguments list
			int functionOffset = node.getStartingOffset();
			LocationType location = getLocation(viewer.getDocument(), functionOffset);

			// restore replace range
			replaceRange = range;

			// init type and method names
			String typeName = null;
			String methodName = null;

			switch (location)
			{
				case IN_VARIABLE_NAME:
				{
					typeName = JSTypeConstants.WINDOW_TYPE;
					methodName = node.getParent().getFirstChild().getText();
					break;
				}

				case IN_PROPERTY_NAME:
				{
					JSGetPropertyNode propertyNode = ParseUtil.getGetPropertyNode(node,
							((JSNode) node).getContainingStatementNode());
					List<String> types = getParentObjectTypes(propertyNode, offset);

					if (types.size() > 0)
					{
						typeName = types.get(0);
						methodName = propertyNode.getLastChild().getText();
					}
					break;
				}

				default:
					break;
			}

			if (typeName != null && methodName != null)
			{
				List<PropertyElement> properties = indexHelper.getTypeMembers(getIndex(), typeName, methodName);

				if (properties != null)
				{
					// TODO: Should we do anything special if there is more than one function?
					for (PropertyElement property : properties)
					{
						if (property instanceof FunctionElement)
						{
							result = (FunctionElement) property;
							break;
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * getLocation
	 * 
	 * @param lexemeProvider
	 * @param offset
	 * @return
	 */
	LocationType getLocation(IDocument document, int offset)
	{
		JSLocationIdentifier identifier = new JSLocationIdentifier(offset, getActiveASTNode(offset - 1));
		LocationType result = identifier.getType();

		targetNode = identifier.getTargetNode();
		statementNode = identifier.getStatementNode();
		replaceRange = identifier.getReplaceRange();

		// if we couldn't determine the location type with the AST, then
		// fallback to using lexemes
		if (result == LocationType.UNKNOWN)
		{
			// NOTE: this method call sets replaceRange as a side-effect
			result = getLocationByLexeme(document, offset);
		}

		return result;
	}

	/**
	 * getLocationByLexeme
	 * 
	 * @param lexemeProvider
	 * @param offset
	 * @return
	 */
	LocationType getLocationByLexeme(IDocument document, int offset)
	{
		// grab relevant lexemes around the current offset
		ILexemeProvider<JSTokenType> lexemeProvider = createLexemeProvider(document, offset);

		// assume we can't determine the location type
		LocationType result = LocationType.UNKNOWN;

		// find lexeme nearest to our offset
		int index = lexemeProvider.getLexemeIndex(offset);

		if (index < 0)
		{
			int candidateIndex = lexemeProvider.getLexemeFloorIndex(offset);
			Lexeme<JSTokenType> lexeme = lexemeProvider.getLexeme(candidateIndex);

			if (lexeme != null)
			{
				if (lexeme.getEndingOffset() == offset)
				{
					index = candidateIndex;
				}
				else if (lexeme.getType() == JSTokenType.NEW)
				{
					index = candidateIndex;
				}
			}
		}

		if (index >= 0)
		{
			Lexeme<JSTokenType> lexeme = lexemeProvider.getLexeme(index);

			switch (lexeme.getType())
			{
				case DOT:
					result = LocationType.IN_PROPERTY_NAME;
					break;

				case SEMICOLON:
					if (index > 0)
					{
						Lexeme<JSTokenType> previousLexeme = lexemeProvider.getLexeme(index - 1);

						switch (previousLexeme.getType())
						{
							case IDENTIFIER:
								result = LocationType.IN_GLOBAL;
								break;

							default:
								break;
						}
					}
					break;

				case LPAREN:
					if (offset == lexeme.getEndingOffset())
					{
						Lexeme<JSTokenType> previousLexeme = lexemeProvider.getLexeme(index - 1);

						if (previousLexeme.getType() != JSTokenType.IDENTIFIER)
						{
							result = LocationType.IN_GLOBAL;
						}
					}
					break;

				case RPAREN:
					if (offset == lexeme.getStartingOffset())
					{
						result = LocationType.IN_GLOBAL;
					}
					break;

				case IDENTIFIER:
					if (index > 0)
					{
						Lexeme<JSTokenType> previousLexeme = lexemeProvider.getLexeme(index - 1);

						switch (previousLexeme.getType())
						{
							case DOT:
								result = LocationType.IN_PROPERTY_NAME;
								break;

							case NEW:
								result = LocationType.IN_CONSTRUCTOR;
								break;

							default:
								result = LocationType.IN_VARIABLE_NAME;
								break;
						}
					}
					else
					{
						result = LocationType.IN_VARIABLE_NAME;
					}
					break;

				default:
					break;
			}
		}
		else if (lexemeProvider.size() == 0)
		{
			result = LocationType.IN_GLOBAL;
		}

		return result;
	}

	/**
	 * @param result
	 * @return
	 */
	protected List<ICompletionProposal> getMergedProposals(List<ICompletionProposal> proposals)
	{
		// order proposals by display name
		Collections.sort(proposals, new Comparator<ICompletionProposal>()
		{
			public int compare(ICompletionProposal o1, ICompletionProposal o2)
			{
				int result = getImageIndex(o1) - getImageIndex(o2);

				if (result == 0)
				{
					result = o1.getDisplayString().compareTo(o2.getDisplayString());
				}

				return result;
			}

			protected int getImageIndex(ICompletionProposal proposal)
			{
				Image image = proposal.getImage();
				int result = 0;

				if (image == JS_KEYWORD)
				{
					result = 1;
				}
				else if (image == JS_PROPERTY)
				{
					result = 2;
				}
				else if (image == JS_PROPERTY)
				{
					result = 3;
				}

				return result;
			}
		});

		// remove duplicates, merging duplicates into a single proposal
		return CollectionsUtil.filter(proposals, new ProposalMerger());
	}

	/**
	 * getParentObjectTypes
	 * 
	 * @param node
	 * @param offset
	 * @return
	 */
	protected List<String> getParentObjectTypes(JSGetPropertyNode node, int offset)
	{
		return ParseUtil.getParentObjectTypes(getIndex(), getURI(), targetNode, node, offset);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#getPreferenceNodeQualifier()
	 */
	protected String getPreferenceNodeQualifier()
	{
		return JSPlugin.PLUGIN_ID;
	}

	/**
	 * Expose replace range field for unit tests
	 * 
	 * @return
	 */
	IRange getReplaceRange()
	{
		return replaceRange;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#isValidActivationCharacter(char, int)
	 */
	public boolean isValidActivationCharacter(char c, int keyCode)
	{
		return Character.isWhitespace(c);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#triggerAdditionalAutoActivation(char, int,
	 * org.eclipse.jface.text.IDocument, int)
	 */
	public boolean isValidAutoActivationLocation(char c, int keyCode, IDocument document, int offset)
	{
		// NOTE: If auto-activation logic changes it may be necessary to change this logic
		// to continue walking backwards through partitions until a) a valid activation character
		// or b) a non-whitespace non-valid activation character is encountered. That implementation
		// would need to skip partitions that are effectively whitespace, for example, comment
		// partitions
		boolean result = false;

		try
		{
			ITypedRegion partition = document.getPartition(offset);

			if (partition != null && AUTO_ACTIVATION_PARTITION_TYPES.contains(partition.getType()))
			{
				int start = partition.getOffset();
				int index = offset - 1;

				while (index >= start)
				{
					char candidate = document.getChar(index);

					if (candidate == ',' || candidate == '(')
					{
						result = true;
						break;
					}
					else if (!Character.isWhitespace(candidate))
					{
						break;
					}

					index--;
				}
			}
		}
		catch (BadLocationException e)
		{
			// ignore
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#isValidIdentifier(char, int)
	 */
	public boolean isValidIdentifier(char c, int keyCode)
	{
		return Character.isJavaIdentifierStart(c) || Character.isJavaIdentifierPart(c) || c == '$';
	}

	/**
	 * The currently active range
	 * 
	 * @param activeRange
	 */
	public void setActiveRange(IRange activeRange)
	{
		this.activeRange = activeRange;
	}
}
