/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.util.impl.ScopedLuceneAnalyzer;

/**
 * @author Yoann Rodiere
 */
public class ScopedLuceneAnalyzerReference extends LuceneAnalyzerReference implements ScopedAnalyzerReference {

	public ScopedLuceneAnalyzerReference(ScopedLuceneAnalyzer analyzer) {
		super( analyzer );
	}

	@Override
	public ScopedLuceneAnalyzer getAnalyzer() {
		return (ScopedLuceneAnalyzer) super.getAnalyzer();
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( ScopedLuceneAnalyzerReference.class );
	}

}
