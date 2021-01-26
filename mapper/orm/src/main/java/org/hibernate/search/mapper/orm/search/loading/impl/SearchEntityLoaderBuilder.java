/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

public class SearchEntityLoaderBuilder<E> {

	private final SessionImplementor session;
	private final Set<? extends SearchLoadingIndexedTypeContext> concreteIndexedTypes;

	private EntityLoadingCacheLookupStrategy cacheLookupStrategy;

	public SearchEntityLoaderBuilder(SearchLoadingMappingContext mappingContext,
			SearchLoadingSessionContext sessionContext,
			Set<? extends SearchLoadingIndexedTypeContext> concreteIndexedTypes) {
		this.session = sessionContext.session();
		this.concreteIndexedTypes = concreteIndexedTypes;
		this.cacheLookupStrategy = mappingContext.cacheLookupStrategy();
	}

	public void cacheLookupStrategy(EntityLoadingCacheLookupStrategy cacheLookupStrategy) {
		this.cacheLookupStrategy = cacheLookupStrategy;
	}

	public EntityLoader<EntityReference, ? extends E> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedTypes.size() == 1 ) {
			SearchLoadingIndexedTypeContext typeContext = concreteIndexedTypes.iterator().next();
			return createForSingleType( typeContext, mutableLoadingOptions );
		}

		/*
		 * First, group the types by their loading strategy.
		 * If multiple types are in the same entity hierarchy and are loaded the same way,
		 * this will allow running a single query to load entities of all these types,
		 * instead of one query per type.
		 */
		Map<SearchEntityLoadingStrategy, List<SearchLoadingIndexedTypeContext>> typesBySearchEntityLoadingStrategy =
				new HashMap<>( concreteIndexedTypes.size() );
		for ( SearchLoadingIndexedTypeContext typeContext : concreteIndexedTypes ) {
			SearchEntityLoadingStrategy loadingStrategyForType = typeContext.loadingStrategy();
			typesBySearchEntityLoadingStrategy.computeIfAbsent( loadingStrategyForType, ignored -> new ArrayList<>() )
					.add( typeContext );
		}

		/*
		 * Then create the loaders.
		 */
		if ( typesBySearchEntityLoadingStrategy.size() == 1 ) {
			// Optimization: we only need one loader, so skip the "by type" wrapper.
			Map.Entry<SearchEntityLoadingStrategy, List<SearchLoadingIndexedTypeContext>> entry =
					typesBySearchEntityLoadingStrategy.entrySet().iterator().next();
			SearchEntityLoadingStrategy loadingStrategy = entry.getKey();
			List<SearchLoadingIndexedTypeContext> types = entry.getValue();
			return createForMultipleTypes( loadingStrategy, types, mutableLoadingOptions );
		}
		else {
			Map<String, HibernateOrmComposableSearchEntityLoader<? extends E>> delegateByEntityName =
					new HashMap<>( concreteIndexedTypes.size() );
			for ( Map.Entry<SearchEntityLoadingStrategy, List<SearchLoadingIndexedTypeContext>> entry :
					typesBySearchEntityLoadingStrategy.entrySet() ) {
				SearchEntityLoadingStrategy loadingStrategy = entry.getKey();
				List<SearchLoadingIndexedTypeContext> types = entry.getValue();
				HibernateOrmComposableSearchEntityLoader<? extends E> loader =
						createForMultipleTypes( loadingStrategy, types, mutableLoadingOptions );
				for ( SearchLoadingIndexedTypeContext type : types ) {
					delegateByEntityName.put( type.jpaEntityName(), loader );
				}
			}
			return new HibernateOrmByTypeEntityLoader<>( delegateByEntityName );
		}
	}

	private HibernateOrmComposableSearchEntityLoader<? extends E> createForSingleType(
			SearchLoadingIndexedTypeContext typeContext,
			MutableEntityLoadingOptions mutableLoadingOptions) {
		return typeContext.loadingStrategy().createLoader(
				typeContext,
				session,
				cacheLookupStrategy,
				mutableLoadingOptions
		);
	}

	private HibernateOrmComposableSearchEntityLoader<? extends E> createForMultipleTypes(
			SearchEntityLoadingStrategy loadingStrategy,
			List<SearchLoadingIndexedTypeContext> types,
			MutableEntityLoadingOptions mutableLoadingOptions) {
		return loadingStrategy.createLoader(
				types,
				session,
				cacheLookupStrategy,
				mutableLoadingOptions
		);
	}
}
