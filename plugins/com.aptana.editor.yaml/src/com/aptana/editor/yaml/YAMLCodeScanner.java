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
package com.aptana.editor.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

import com.aptana.editor.common.text.rules.ExtendedWordRule;
import com.aptana.editor.common.text.rules.SingleCharacterRule;

/**
 * @author Chris Williams
 */
public class YAMLCodeScanner extends BufferedRuleBasedScanner
{

	public YAMLCodeScanner()
	{
		List<IRule> rules = createRules();
		setRules(rules.toArray(new IRule[rules.size()]));
		setDefaultReturnToken(new Token("string.unquoted.yaml")); //$NON-NLS-1$
	}

	protected List<IRule> createRules()
	{
		List<IRule> rules = new ArrayList<IRule>();

		// Key/Property names
		WordRule rule = new YAMLKeyRule(new YAMLKeyDetector(), new Token("entity.name.tag.yaml"), false); //$NON-NLS-1$
		rule.setColumnConstraint(0);
		rules.add(rule);

		// Variables
		rule = new WordRule(new YAMLVariableDetector(), new Token("variable.other.yaml")); //$NON-NLS-1$
		rules.add(rule);

		// Numbers
		rule = new YAMLNumberRule(new YAMLNumberDetector(), new Token("constant.numeric.yaml"), false); //$NON-NLS-1$
		rules.add(rule);

		// Dates
		rule = new YAMLDateRule(new YAMLDateDetector(), new Token("constant.other.date.yaml"), false); //$NON-NLS-1$
		rules.add(rule);

		// Block/Unquoted strings
		rule = new WordRule(new YAMLUnquotedWordDetector(), new Token("string.unquoted.yaml")); //$NON-NLS-1$
		rules.add(rule);

		// Directive Separators
		rule = new YAMLDirectiveSeparatorRule(new Token("meta.separator.yaml")); //$NON-NLS-1$
		rules.add(rule);

		// Document Separators
		rule = new YAMLDocumentSeparatorRule(new Token("meta.separator.yaml")); //$NON-NLS-1$
		rules.add(rule);

		rules.add(new SingleCharacterRule('-', new Token("keyword.operator.symbol"))); //$NON-NLS-1$

		return rules;
	}

	/**
	 * Detects "...".
	 * 
	 * @author cwilliams
	 */
	private final class YAMLDocumentSeparatorRule extends ExtendedWordRule
	{
		private YAMLDocumentSeparatorRule(IToken defaultToken)
		{
			super(new SingleCharacterDetector('.'), defaultToken, false);
			setColumnConstraint(0);
		}

		@Override
		protected boolean wordOK(String word, ICharacterScanner scanner)
		{
			return word.length() == 3;
		}
	}

	private final class SingleCharacterDetector implements IWordDetector
	{
		private char fChar;

		SingleCharacterDetector(char c)
		{
			this.fChar = c;
		}

		public boolean isWordStart(char c)
		{
			return c == fChar;
		}

		public boolean isWordPart(char c)
		{
			return c == fChar;
		}
	}

	/**
	 * Detects "---".
	 * 
	 * @author cwilliams
	 */
	private final class YAMLDirectiveSeparatorRule extends ExtendedWordRule
	{
		private YAMLDirectiveSeparatorRule(IToken defaultToken)
		{
			super(new SingleCharacterDetector('-'), defaultToken, false);
			setColumnConstraint(0);
		}

		@Override
		protected boolean wordOK(String word, ICharacterScanner scanner)
		{
			return word.length() == 3;
		}
	}

	private final class YAMLKeyRule extends ExtendedWordRule
	{
		private Pattern pattern;

		private YAMLKeyRule(IWordDetector detector, IToken defaultToken, boolean ignoreCase)
		{
			super(detector, defaultToken, ignoreCase);
		}

		protected boolean wordOK(String word, ICharacterScanner scanner)
		{
			if (word.length() < 2 || word.indexOf(':') == -1)
			{
				return false;
			}
			return getPattern().matcher(word).matches();
		}

		private synchronized Pattern getPattern()
		{
			if (pattern == null)
			{
				pattern = Pattern.compile("\\s*(?:(-)|(?:(-\\s*)?(((\\w+\\s*)|(<<)):)))\\s*"); //$NON-NLS-1$
			}
			return pattern;
		}
	}

	private final class YAMLNumberRule extends ExtendedWordRule
	{
		private Pattern pattern;

		private YAMLNumberRule(IWordDetector detector, IToken defaultToken, boolean ignoreCase)
		{
			super(detector, defaultToken, ignoreCase);
		}

		protected boolean wordOK(String word, ICharacterScanner scanner)
		{
			return getPattern().matcher(word).matches();
		}

		private synchronized Pattern getPattern()
		{
			if (pattern == null)
			{
				pattern = Pattern
						.compile("(\\+|-)?((0(x|X|o|O)[0-9a-fA-F]*)|(([0-9]+\\.?[0-9]*)|(\\.[0-9]+))((e|E)(\\+|-)?[0-9]+)?)(L|l|UL|ul|u|U|F|f)?"); //$NON-NLS-1$
			}
			return pattern;
		}
	}

	private final class YAMLDateRule extends ExtendedWordRule
	{
		private Pattern pattern;

		private YAMLDateRule(IWordDetector detector, IToken defaultToken, boolean ignoreCase)
		{
			super(detector, defaultToken, ignoreCase);
		}

		protected boolean wordOK(String word, ICharacterScanner scanner)
		{
			if (word.length() != 10)
			{
				return false;
			}
			return getPattern().matcher(word).matches();
		}

		private synchronized Pattern getPattern()
		{
			if (pattern == null)
			{
				pattern = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})"); //$NON-NLS-1$
			}
			return pattern;
		}
	}

	private static final class YAMLUnquotedWordDetector implements IWordDetector
	{
		public boolean isWordStart(char c)
		{
			return Character.isLetterOrDigit(c);
		}

		public boolean isWordPart(char c)
		{
			return isWordStart(c) || c == '-' || c == '_' || c == '/' || c == '.';
		}
	}

	/**
	 * Word detector for YAML variable references.
	 * 
	 * @author cwilliams
	 */
	private static class YAMLVariableDetector implements IWordDetector
	{

		public boolean isWordStart(char c)
		{
			return c == '&' || c == '*';
		}

		public boolean isWordPart(char c)
		{
			return Character.isLetterOrDigit(c) || c == '_' || c == '-';
		}
	}

	/**
	 * Word detector for YAML property keys.
	 * 
	 * @author cwilliams
	 */
	private static class YAMLKeyDetector implements IWordDetector
	{
		boolean stop = false;

		public boolean isWordStart(char c)
		{
			stop = false;
			return c == ' ' || c == '\t' || Character.isLetterOrDigit(c) || c == '<';
		}

		public boolean isWordPart(char c)
		{
			if (stop)
			{
				stop = false;
				return false;
			}
			if (c == ':')
			{
				stop = true;
				return true;
			}
			return c == ' ' || c == '\t' || Character.isLetterOrDigit(c) || c == '<' || c == '-' || c == '_';
		}

	}

	/**
	 * Word detector for YAML numbers.
	 * 
	 * @author cwilliams
	 */
	private static class YAMLNumberDetector implements IWordDetector
	{
		public boolean isWordStart(char c)
		{
			return Character.isDigit(c) || c == '.' || c == '-' || c == '+';
		}

		public boolean isWordPart(char c)
		{
			if (isWordStart(c))
			{
				return true;
			}
			c = Character.toLowerCase(c);
			return c == 'x' || c == 'a' || c == 'b' || c == 'c' || c == 'd' || c == 'e' || c == 'f' || c == 'l'
					|| c == 'u' || c == 'o';
		}
	}

	/**
	 * Word detector for YAML dates.
	 * 
	 * @author cwilliams
	 */
	private static class YAMLDateDetector implements IWordDetector
	{
		public boolean isWordStart(char c)
		{
			return Character.isDigit(c);
		}

		public boolean isWordPart(char c)
		{
			return isWordStart(c) || c == '-';
		}
	}
}