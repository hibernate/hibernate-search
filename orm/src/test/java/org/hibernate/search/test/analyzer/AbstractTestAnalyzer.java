/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public abstract class AbstractTestAnalyzer extends Analyzer {

	protected abstract String[] getTokens();

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer tokenizer = new StreamWrappingTokenizer( getTokens() );
		return new TokenStreamComponents( tokenizer );
	}

}
