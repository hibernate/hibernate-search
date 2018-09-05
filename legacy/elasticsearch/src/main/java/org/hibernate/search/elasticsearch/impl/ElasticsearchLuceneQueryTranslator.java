/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.Properties;

import org.apache.lucene.search.Query;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchEntityHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.query.engine.impl.LuceneQueryTranslator;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.impl.IndexedTypeSets;

import com.google.gson.JsonObject;

/**
 * Translates Lucene queries into ES queries.
 * <p>
 * Extra-experimental ;)
 *
 * @author Gunnar Morling
 */
public class ElasticsearchLuceneQueryTranslator implements LuceneQueryTranslator, Startable {

	private ExtendedSearchIntegrator extendedIntegrator;

	@Override
	public void start(Properties properties, BuildContext context) {
		extendedIntegrator = context.getUninitializedSearchIntegrator();
	}

	@Override
	public QueryDescriptor convertLuceneQuery(Query luceneQuery) {
		JsonObject convertedQuery = ToElasticsearch.fromLuceneQuery( luceneQuery );

		JsonObject query = new JsonObject();
		query.add( "query", convertedQuery );

		return new ElasticsearchJsonQueryDescriptor( query );
	}

	@Override
	public boolean conversionRequired(IndexedTypeSet entities) {
		IndexedTypeSet queriedEntityTypes = getQueriedEntityTypes( entities );
		return ElasticsearchEntityHelper.isAnyMappedToElasticsearch( extendedIntegrator, queriedEntityTypes );
	}

	private IndexedTypeSet getQueriedEntityTypes(IndexedTypeSet indexedTargetedEntities) {
		if ( indexedTargetedEntities == null || indexedTargetedEntities.isEmpty() ) {
			return extendedIntegrator.getIndexBindings().keySet();
		}
		else {
			return IndexedTypeSets.fromIdentifiers( indexedTargetedEntities );
		}
	}

}
