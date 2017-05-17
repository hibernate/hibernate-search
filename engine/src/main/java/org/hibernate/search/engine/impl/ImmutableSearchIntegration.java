/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.engine.integration.impl.SearchIntegration;

/**
 * @author Yoann Rodiere
 */
public class ImmutableSearchIntegration implements SearchIntegration {

	private final AnalyzerRegistry analyzerRegistry;

	private final NormalizerRegistry normalizerRegistry;

	public ImmutableSearchIntegration(AnalyzerRegistry analyzerRegistry,
			NormalizerRegistry normalizerRegistry) {
		super();
		this.analyzerRegistry = new ImmutableAnalyzerRegistry( analyzerRegistry );
		this.normalizerRegistry = new ImmutableNormalizerRegistry( normalizerRegistry );
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
		analyzerRegistry.close();
	}

}
