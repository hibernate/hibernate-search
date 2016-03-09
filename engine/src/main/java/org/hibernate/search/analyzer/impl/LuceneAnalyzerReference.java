/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;

/**
 * A reference to an {@link Analyzer}.
 *
 * @author Davide D'Alto
 */
public final class LuceneAnalyzerReference implements AnalyzerReference {

	private final Analyzer analyzer;

	public LuceneAnalyzerReference(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	public void close() {
		analyzer.close();
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return LuceneAnalyzerReference.class.isAssignableFrom( analyzerType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		return (T) this;
	}
}
