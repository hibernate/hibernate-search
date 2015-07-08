/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;

/**
 * @author Hardy Ferentschik
 */
public final class FooAnalyzer extends Analyzer {

	public FooAnalyzer() {
		super();
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		//Not particularly important, but at least it's a fully functional Analyzer:
		return new TokenStreamComponents( new LowerCaseTokenizer() );
	}

}
