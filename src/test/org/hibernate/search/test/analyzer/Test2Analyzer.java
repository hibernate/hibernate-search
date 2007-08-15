//$Id$
package org.hibernate.search.test.analyzer;

/**
 * @author Emmanuel Bernard
 */
public class Test2Analyzer extends AbstractTestAnalyzer {
	private final String[] tokens = { "sound", "cat", "speed" };

	protected String[] getTokens() {
		return tokens;
	}
}
