/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManagerType;

/**
 * @author Yoann Rodiere
 */
public final class ElasticsearchEntityHelper {

	private ElasticsearchEntityHelper() {
		// private constructor
	}

	public static boolean isMappedToElasticsearch(ExtendedSearchIntegrator integrator, Class<?> entityType) {
		Set<Class<?>> entityTypesWithSubTypes = integrator.getIndexedTypesPolymorphic( new Class<?>[] { entityType } );
		return hasElasticsearchIndexManager( integrator, entityTypesWithSubTypes );
	}

	public static boolean isAnyMappedToElasticsearch(ExtendedSearchIntegrator integrator, Collection<Class<?>> entityTypes) {
		Set<Class<?>> entityTypesWithSubTypes = integrator.getIndexedTypesPolymorphic(
				entityTypes.toArray( new Class<?>[entityTypes.size()] ) );
		return hasElasticsearchIndexManager( integrator, entityTypesWithSubTypes );
	}

	private static boolean hasElasticsearchIndexManager(ExtendedSearchIntegrator integrator, Collection<Class<?>> entityTypes) {
		for ( Class<?> entityType : entityTypes ) {
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
