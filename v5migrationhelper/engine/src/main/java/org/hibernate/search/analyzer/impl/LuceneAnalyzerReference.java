/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;

/**
 * A reference to an {@link Analyzer}.
 *
 * @author Davide D'Alto
 */
public abstract class LuceneAnalyzerReference implements AnalyzerReference {

	public abstract Analyzer getAnalyzer();

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( getClass() );
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		return analyzerType.cast( this );
	}
}
