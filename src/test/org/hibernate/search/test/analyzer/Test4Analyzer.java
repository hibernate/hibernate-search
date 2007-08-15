//$Id$
package org.hibernate.search.test.analyzer;

/**
 * @author Emmanuel Bernard
 */
public class Test4Analyzer extends AbstractTestAnalyzer {
	private final String[] tokens = { "noise", "mouse", "light" };

	protected String[] getTokens() {
		return tokens;
	}
}
