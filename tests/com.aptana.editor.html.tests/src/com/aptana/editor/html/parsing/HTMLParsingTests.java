/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html.parsing;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class HTMLParsingTests extends TestCase
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite(HTMLParsingTests.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(HTMLUtilsTest.class);
		// $JUnit-END$
		return suite;
	}

}
