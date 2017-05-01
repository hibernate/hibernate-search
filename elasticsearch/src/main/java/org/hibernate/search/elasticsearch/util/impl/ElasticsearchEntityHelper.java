/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;

/**
 * @author Yoann Rodiere
 */
public final class ElasticsearchEntityHelper {

	private ElasticsearchEntityHelper() {
		// private constructor
	}

	public static boolean isMappedToElasticsearch(ExtendedSearchIntegrator integrator, IndexedTypeIdentifier entityType) {
		IndexedTypeSet entityTypesWithSubTypes = integrator.getIndexedTypesPolymorphic( entityType.asTypeSet() );
		return hasElasticsearchIndexManager( integrator, entityTypesWithSubTypes );
	}

	public static boolean isAnyMappedToElasticsearch(ExtendedSearchIntegrator integrator, IndexedTypeSet entityTypes) {
		IndexedTypeSet entityTypesWithSubTypes = integrator.getIndexedTypesPolymorphic( entityTypes );
		return hasElasticsearchIndexManager( integrator, entityTypesWithSubTypes );
	}

	private static boolean hasElasticsearchIndexManager(ExtendedSearchIntegrator integrator, IndexedTypeSet entityTypes) {
		for ( IndexedTypeIdentifier entityType : entityTypes ) {
			EntityIndexBinding binding = integrator.getIndexBinding( entityType );
			if ( binding == null ) {
				continue;
			}

			IndexManagerType indexManagerType = binding.getIndexManagerType();

			if ( ElasticsearchIndexManagerType.INSTANCE.equals( indexManagerType ) ) {
				return true;
			}
		}

		return false;
	}

}
