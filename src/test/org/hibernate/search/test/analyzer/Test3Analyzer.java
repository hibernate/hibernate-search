//$Id$
package org.hibernate.search.test.analyzer;

/**
 * @author Emmanuel Bernard
 */
public class Test3Analyzer extends AbstractTestAnalyzer {
	private final String[] tokens = { "music", "elephant", "energy" };

	protected String[] getTokens() {
		return tokens;
	}
}
