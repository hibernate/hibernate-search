/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl.es2;

import org.hibernate.search.elasticsearch.dialect.impl.DialectIndependentGsonProvider;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.nulls.impl.Elasticsearch2MissingValueStrategy;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch2SchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.Elasticsearch2SchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaAccessor;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.work.impl.factory.Elasticsearch2WorkFactory;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch2Dialect implements ElasticsearchDialect {

	@Override
	public GsonProvider createGsonProvider() {
		return DialectIndependentGsonProvider.INSTANCE;
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
	public MissingValueStrategy createMissingValueStrategy() {
		return Elasticsearch2MissingValueStrategy.INSTANCE;
	}

}
