/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl.es2;

import org.hibernate.search.elasticsearch.analyzer.impl.Elasticsearch2AnalyzerStrategy;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerStrategyFactory;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.query.impl.Elasticsearch2QueryFactory;
import org.hibernate.search.elasticsearch.query.impl.ElasticsearchQueryFactory;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch2SchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch2SchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaAccessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.model.FieldDataType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.NormsType;
import org.hibernate.search.elasticsearch.util.impl.gson.ES2FieldDataTypeJsonAdapter;
import org.hibernate.search.elasticsearch.util.impl.gson.ES2IndexTypeJsonAdapter;
import org.hibernate.search.elasticsearch.util.impl.gson.ES2NormsTypeJsonAdapter;
import org.hibernate.search.elasticsearch.work.impl.factory.Elasticsearch2WorkFactory;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.service.spi.ServiceManager;

import com.google.gson.GsonBuilder;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch2Dialect implements ElasticsearchDialect {

	@Override
	public GsonBuilder createGsonBuilderBase() {
		return new GsonBuilder()
					.registerTypeAdapter( IndexType.class, new ES2IndexTypeJsonAdapter().nullSafe() )
					.registerTypeAdapter( FieldDataType.class, new ES2FieldDataTypeJsonAdapter().nullSafe() )
					.registerTypeAdapter( NormsType.class, new ES2NormsTypeJsonAdapter().nullSafe() );
	}

	@Override
	public ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider) {
		return new Elasticsearch2WorkFactory( gsonProvider );
	}

	@Override
	public ElasticsearchSchemaTranslator createSchemaTranslator() {
		return new Elasticsearch2SchemaTranslator();
	}

	@Override
	public ElasticsearchSchemaValidator createSchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		return new Elasticsearch2SchemaValidator( schemaAccessor );
	}

	@Override
	public ElasticsearchAnalyzerStrategyFactory createAnalyzerStrategyFactory(ServiceManager serviceManager) {
		return configuration -> new Elasticsearch2AnalyzerStrategy( serviceManager, configuration );
	}

	@Override
	public ElasticsearchQueryFactory createQueryFactory() {
		return new Elasticsearch2QueryFactory();
	}

}
