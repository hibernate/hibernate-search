/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.indexes.spi.IndexFamilyImplementor;
import org.hibernate.search.util.impl.Closer;

/**
 * @author Yoann Rodiere
 */
public class ImmutableSearchIntegration implements SearchIntegration {

	private final IndexFamilyImplementor indexFamily;

	private final AnalyzerRegistry analyzerRegistry;

	private final NormalizerRegistry normalizerRegistry;

	public ImmutableSearchIntegration(IndexFamilyImplementor indexFamily,
			AnalyzerRegistry analyzerRegistry,
			NormalizerRegistry normalizerRegistry) {
		this.indexFamily = indexFamily;
		this.analyzerRegistry = new ImmutableAnalyzerRegistry( analyzerRegistry );
		this.normalizerRegistry = new ImmutableNormalizerRegistry( normalizerRegistry );
	}

	@Override
	public IndexFamilyImplementor getIndexFamily() {
		return indexFamily;
	}

	@Override
	public AnalyzerRegistry getAnalyzerRegistry() {
		return analyzerRegistry;
	}

	@Override
	public NormalizerRegistry getNormalizerRegistry() {
		return normalizerRegistry;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IndexFamilyImplementor::close, indexFamily );
			closer.push( AnalyzerRegistry::close, analyzerRegistry );
			closer.push( NormalizerRegistry::close, normalizerRegistry );
		}
	}

}
