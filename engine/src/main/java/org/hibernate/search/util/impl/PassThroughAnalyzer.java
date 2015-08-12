/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

/**
 * Analyzer that applies no operation whatsoever to the flux
 * This is useful for queries operating on non tokenized fields.
 * <p>
 * TODO there is probably a way to make that much more efficient by
 * reimplementing TokenStream to take the Reader and pass through the flux as a single token
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public final class PassThroughAnalyzer extends Analyzer {

	public static final PassThroughAnalyzer INSTANCE = new PassThroughAnalyzer();

	/**
	 * Create a new PassThroughAnalyzer.
	 */
	private PassThroughAnalyzer() {
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		return new TokenStreamComponents( new KeywordTokenizer() );
	}

}

