/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import org.apache.lucene.util.Version;

/**
 * @author Emmanuel Bernard
 */
public final class AnalyzerForTests4 extends AbstractTestAnalyzer {
	private final String[] tokens = { "noise", "mouse", "light" };

	public AnalyzerForTests4(Version version) {
	}

	@Override
	protected String[] getTokens() {
		return tokens;
	}
}
