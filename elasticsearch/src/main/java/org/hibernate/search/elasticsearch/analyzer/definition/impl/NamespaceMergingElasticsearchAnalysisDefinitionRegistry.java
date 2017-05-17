/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A wrapper around an {@link ElasticsearchAnalysisDefinitionRegistry} that automatically
 * checks that newly inserted analyzers/normalizers do not introduce naming conflicts
 * between normalizers and analyzers, effectively merging the analyzer and normalizer
 * namespace.
 * <p>
 * For instance, a {@link SimpleElasticsearchAnalysisDefinitionRegistry} would allow
 * both an analyzer and a normalizer to be named "standard" at the same time.
 * Adding this wrapper around the registry would prevent this situation to ever occur:
 * the insertion of the analyzer or normalizer (whichever happens last) would trigger
 * an exception.
 *
 * @author Yoann Rodiere
 */
public final class NamespaceMergingElasticsearchAnalysisDefinitionRegistry extends ForwardingElasticsearchAnalysisDefinitionRegistry {

	private static final Log LOG = LoggerFactory.make( Log.class );

	public NamespaceMergingElasticsearchAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionRegistry delegate) {
		super( delegate );
	}

	@Override
	public void register(String name, AnalyzerDefinition definition) {
		if ( getNormalizerDefinition( name ) != null ) {
			throw LOG.analyzerNormalizerNamingConflict( name );
		}
		super.register( name, definition );
	}

	@Override
	public void register(String name, NormalizerDefinition definition) {
		if ( getAnalyzerDefinition( name ) != null ) {
			throw LOG.analyzerNormalizerNamingConflict( name );
		}
		super.register( name, definition );
	}

}
