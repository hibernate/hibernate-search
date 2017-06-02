/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl.es52;

import org.hibernate.search.elasticsearch.analyzer.impl.Elasticsearch52AnalyzerStrategy;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerStrategyFactory;
import org.hibernate.search.elasticsearch.dialect.impl.es50.Elasticsearch50Dialect;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch52SchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch52SchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaAccessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.engine.service.spi.ServiceManager;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch52Dialect extends Elasticsearch50Dialect {

	@Override
	public ElasticsearchSchemaTranslator createSchemaTranslator() {
		return new Elasticsearch52SchemaTranslator();
	}

	@Override
	public ElasticsearchSchemaValidator createSchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		return new Elasticsearch52SchemaValidator( schemaAccessor );
	}

	@Override
	public ElasticsearchAnalyzerStrategyFactory createAnalyzerStrategyFactory(ServiceManager serviceManager) {
		return configuration -> new Elasticsearch52AnalyzerStrategy( serviceManager, configuration );
	}

}
