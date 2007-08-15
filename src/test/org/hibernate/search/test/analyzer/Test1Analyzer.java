//$Id$
package org.hibernate.search.test.analyzer;

/**
 * @author Emmanuel Bernard
 */
public class Test1Analyzer extends AbstractTestAnalyzer {
	private final String[] tokens = { "alarm", "dog", "performance" };

	protected String[] getTokens() {
		return tokens;
	}
}
