/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to an {@link Analyzer}.
 *
 * @author Davide D'Alto
 */
public abstract class LuceneAnalyzerReference implements AnalyzerReference {

	private final Log log = LoggerFactory.make( MethodHandles.lookup() );

	public abstract Analyzer getAnalyzer();

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( getClass() );
	}

	@Override
	public <T extends AnalyzerReference> T unwrap(Class<T> analyzerType) {
		try {
			return analyzerType.cast( this );
		}
		catch (ClassCastException e) {
			if ( !LuceneAnalyzerReference.class.isAssignableFrom( analyzerType ) ) {
				// Not even the same technology, probably a user error
				throw log.invalidConversionFromLuceneAnalyzer( this, e );
			}
			else {
				// The other type uses the same technology... probably a bug?
				throw e;
			}
		}
	}
}
