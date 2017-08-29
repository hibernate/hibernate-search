/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl.es50;

import org.hibernate.search.elasticsearch.analyzer.impl.Elasticsearch2AnalyzerStrategy;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerStrategyFactory;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.query.impl.Elasticsearch5QueryFactory;
import org.hibernate.search.elasticsearch.query.impl.ElasticsearchQueryFactory;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch50SchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch50SchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaAccessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.model.FieldDataType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.NormsType;
import org.hibernate.search.elasticsearch.util.impl.gson.ES5FieldDataTypeJsonAdapter;
import org.hibernate.search.elasticsearch.util.impl.gson.ES5IndexTypeJsonAdapter;
import org.hibernate.search.elasticsearch.util.impl.gson.ES5NormsTypeJsonAdapter;
import org.hibernate.search.elasticsearch.work.impl.factory.Elasticsearch5WorkFactory;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.service.spi.ServiceManager;

import com.google.gson.GsonBuilder;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch50Dialect implements ElasticsearchDialect {

	@Override
	public GsonBuilder createGsonBuilderBase() {
		return new GsonBuilder()
				.registerTypeAdapter( IndexType.class, new ES5IndexTypeJsonAdapter().nullSafe() )
				.registerTypeAdapter( FieldDataType.class, new ES5FieldDataTypeJsonAdapter().nullSafe() )
				.registerTypeAdapter( NormsType.class, new ES5NormsTypeJsonAdapter().nullSafe() );
	}

	@Override
	public ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider) {
		return new Elasticsearch5WorkFactory( gsonProvider );
	}

	@Override
	public ElasticsearchSchemaTranslator createSchemaTranslator() {
		return new Elasticsearch50SchemaTranslator();
	}

	@Override
	public ElasticsearchSchemaValidator createSchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		return new Elasticsearch50SchemaValidator( schemaAccessor );
	}

	@Override
	public ElasticsearchAnalyzerStrategyFactory createAnalyzerStrategyFactory(ServiceManager serviceManager) {
		return configuration -> new Elasticsearch2AnalyzerStrategy( serviceManager, configuration );
	}

	@Override
	public ElasticsearchQueryFactory createQueryFactory() {
		return new Elasticsearch5QueryFactory();
	}

}
