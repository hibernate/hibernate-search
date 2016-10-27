/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl.es5;

import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.nulls.impl.Elasticsearch5MissingValueStrategy;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch5SchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch5SchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaAccessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.model.FieldDataType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.util.impl.gson.ES5FieldDataTypeJsonAdapter;
import org.hibernate.search.elasticsearch.util.impl.gson.ES5IndexTypeJsonAdapter;
import org.hibernate.search.elasticsearch.work.impl.factory.Elasticsearch5WorkFactory;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;

import com.google.gson.GsonBuilder;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch5Dialect implements ElasticsearchDialect {

	@Override
	public GsonProvider createGsonProvider() {
		return DefaultGsonProvider.create( () -> {
			return new GsonBuilder()
					.registerTypeAdapter( IndexType.class, new ES5IndexTypeJsonAdapter().nullSafe() )
					.registerTypeAdapter( FieldDataType.class, new ES5FieldDataTypeJsonAdapter().nullSafe() );
		} );
	}

	@Override
	public ElasticsearchWorkFactory createWorkFactory(GsonProvider gsonProvider) {
		return new Elasticsearch5WorkFactory( gsonProvider );
	}

	@Override
	public ElasticsearchSchemaTranslator createSchemaTranslator() {
		return new Elasticsearch5SchemaTranslator();
	}

	@Override
	public ElasticsearchSchemaValidator createSchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		return new Elasticsearch5SchemaValidator( schemaAccessor );
	}

	@Override
	public MissingValueStrategy createMissingValueStrategy() {
		return Elasticsearch5MissingValueStrategy.INSTANCE;
	}

}
