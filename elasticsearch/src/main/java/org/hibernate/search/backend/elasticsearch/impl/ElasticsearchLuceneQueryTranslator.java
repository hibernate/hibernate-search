/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Properties;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.impl.LuceneQueryTranslator;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.CollectionHelper;

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
	public boolean conversionRequired(Class<?>... entities) {
		Set<Class<?>> queriedEntityTypes = getQueriedEntityTypes( entities );
		Set<Class<?>> queriedEntityTypesWithSubTypes = extendedIntegrator.getIndexedTypesPolymorphic( queriedEntityTypes.toArray( new Class<?>[queriedEntityTypes.size()] ) );

		for ( Class<?> queriedEntityType : queriedEntityTypesWithSubTypes ) {
			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( queriedEntityType );

			if ( binding == null ) {
				continue;
			}

			IndexManager[] indexManagers = binding.getIndexManagers();

			for ( IndexManager indexManager : indexManagers ) {
				if ( indexManager instanceof ElasticsearchIndexManager ) {
					return true;
				}
			}
		}

		return false;
	}

	private Set<Class<?>> getQueriedEntityTypes(Class<?>... indexedTargetedEntities) {
		if ( indexedTargetedEntities == null || indexedTargetedEntities.length == 0 ) {
			return extendedIntegrator.getIndexBindings().keySet();
		}
		else {
			return CollectionHelper.asSet( indexedTargetedEntities );
		}
	}
}
