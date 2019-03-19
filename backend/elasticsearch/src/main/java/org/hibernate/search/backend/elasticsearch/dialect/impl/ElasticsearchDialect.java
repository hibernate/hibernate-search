/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryContextProvider;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * An entry point to all operations that may be implemented differently depending
 * on the Elasticsearch version running on the Elasticsearch cluster.
 * <p>
 * Add more methods here as necessary to implement dialect-specific behavior.
 * <p>
 * This interface should only expose methods to be called during bootstrap,
 * and should not be depended upon in every part of the code.
 * Thus, most methods defined here should be about creating an instance of an interface defined in another package,
 * that will be passed to the part of the code that needs it.
 * <p>
 * For example, if a particular predicate has a different syntax in its JSON form depending on the
 * Elasticsearch version, we could have a createPredicateFormattingStrategy() methods that returns
 * a strategy to be plugged into the predicate builder factory.
 */
public interface ElasticsearchDialect {

	GsonBuilder createGsonBuilderBase();

	ElasticsearchWorkBuilderFactory createWorkBuilderFactory(GsonProvider gsonProvider);

	ElasticsearchSearchResultExtractorFactory createSearchResultExtractorFactory();

	ElasticsearchIndexFieldTypeFactoryContextProvider createIndexTypeFieldFactoryContextProvider(Gson userFacingGson);

}
