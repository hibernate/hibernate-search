/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;

/**
 * @author Yoann Rodiere
 */
public class ScopedElasticsearchAnalyzerReference extends ElasticsearchAnalyzerReference implements ScopedAnalyzerReference {

	public ScopedElasticsearchAnalyzerReference(ScopedElasticsearchAnalyzer analyzer) {
		super( analyzer );
	}

	@Override
	public ScopedElasticsearchAnalyzer getAnalyzer() {
		return (ScopedElasticsearchAnalyzer) super.getAnalyzer();
	}

	@Override
	public <T extends AnalyzerReference> boolean is(Class<T> analyzerType) {
		return analyzerType.isAssignableFrom( ScopedElasticsearchAnalyzerReference.class );
	}

}
