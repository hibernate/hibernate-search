/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.engine.service.spi.ServiceManager;

/**
 * An {@link AnalyzerStrategy} for Elasticsearch 5.2:
 * normalizers are actually translated as normalizers in ES,
 * instead of being translated as analyzers like in {@link Elasticsearch2AnalyzerStrategy}.
 *
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch52AnalyzerStrategy extends Elasticsearch2AnalyzerStrategy {

	public Elasticsearch52AnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		super( serviceManager, cfg );
	}

	@Override
	protected ElasticsearchAnalysisDefinitionRegistry wrapForAdditions(ElasticsearchAnalysisDefinitionRegistry registry) {
		/*
		 * We don't need the same trick as with ES 2.x/5.0/5.1: Elasticsearch 5.2+
		 * actually defines separate namespaces for normalizers and analyzers.
		 */
		return registry;
	}

	@Override
	public ElasticsearchAnalyzerReference createNamedNormalizerReference(String name) {
		return new NamedElasticsearch52NormalizerReference( name );
	}

}
