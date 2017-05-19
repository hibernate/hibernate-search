/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerStrategyFactory;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.query.impl.ElasticsearchQueryFactory;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaCreator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaDropper;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaMigrator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.Service;


/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchService extends Service {

	GsonProvider getGsonProvider();

	ElasticsearchWorkFactory getWorkFactory();

	ElasticsearchWorkProcessor getWorkProcessor();

	ElasticsearchSchemaCreator getSchemaCreator();

	ElasticsearchSchemaDropper getSchemaDropper();

	ElasticsearchSchemaMigrator getSchemaMigrator();

	ElasticsearchSchemaValidator getSchemaValidator();

	ElasticsearchSchemaTranslator getSchemaTranslator();

	ElasticsearchAnalyzerStrategyFactory getAnalyzerStrategyFactory();

	MissingValueStrategy getMissingValueStrategy();

	ElasticsearchQueryFactory getQueryFactory();

	ElasticsearchQueryOptions getQueryOptions();

}
