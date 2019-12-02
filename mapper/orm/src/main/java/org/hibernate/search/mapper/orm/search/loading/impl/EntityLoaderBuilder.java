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

public class EntityLoaderBuilder<E> {

	private final SessionImplementor session;
	private final Set<? extends HibernateOrmLoadingIndexedTypeContext<? extends E>> concreteIndexedTypes;

	private EntityLoadingCacheLookupStrategy cacheLookupStrategy;

	public EntityLoaderBuilder(HibernateOrmLoadingMappingContext mappingContext,
			HibernateOrmLoadingSessionContext sessionContext,
			Set<? extends HibernateOrmLoadingIndexedTypeContext<? extends E>> concreteIndexedTypes) {
		this.session = sessionContext.getSession();
		this.concreteIndexedTypes = concreteIndexedTypes;
		this.cacheLookupStrategy = mappingContext.getCacheLookupStrategy();
	}

	public void cacheLookupStrategy(EntityLoadingCacheLookupStrategy cacheLookupStrategy) {
		this.cacheLookupStrategy = cacheLookupStrategy;
	}

	public EntityLoader<EntityReference, ? extends E> build(MutableEntityLoadingOptions mutableLoadingOptions) {
		if ( concreteIndexedTypes.size() == 1 ) {
			HibernateOrmLoadingIndexedTypeContext<? extends E> typeContext = concreteIndexedTypes.iterator().next();
			return createForSingleType( typeContext, mutableLoadingOptions );
		}

		/*
		 * First, group the types by their loader factory.
		 * If multiple types are in the same entity hierarchy and are loaded the same way,
		 * this will allow to run one query to load entities of all these types,
		 * instead of one query per type.
		 */
		Map<EntityLoaderFactory, List<HibernateOrmLoadingIndexedTypeContext<? extends E>>> typesByEntityLoaderFactory =
				new HashMap<>( concreteIndexedTypes.size() );
		for ( HibernateOrmLoadingIndexedTypeContext<? extends E> typeContext : concreteIndexedTypes ) {
			EntityLoaderFactory loaderFactoryForType = typeContext.getLoaderFactory();
			typesByEntityLoaderFactory.computeIfAbsent( loaderFactoryForType, ignored -> new ArrayList<>() )
					.add( typeContext );
		}

		/*
		 * Then create the loaders.
		 */
		if ( typesByEntityLoaderFactory.size() == 1 ) {
			// Optimization: we only need one loader, so skip the "by type" wrapper.
			Map.Entry<EntityLoaderFactory, List<HibernateOrmLoadingIndexedTypeContext<? extends E>>> entry =
					typesByEntityLoaderFactory.entrySet().iterator().next();
			EntityLoaderFactory loaderFactory = entry.getKey();
			List<HibernateOrmLoadingIndexedTypeContext<? extends E>> types = entry.getValue();
			return createForMultipleTypes( loaderFactory, types, mutableLoadingOptions );
		}
		else {
			Map<String, HibernateOrmComposableEntityLoader<? extends E>> delegateByEntityName =
					new HashMap<>( concreteIndexedTypes.size() );
			for ( Map.Entry<EntityLoaderFactory, List<HibernateOrmLoadingIndexedTypeContext<? extends E>>> entry :
					typesByEntityLoaderFactory.entrySet() ) {
				EntityLoaderFactory loaderFactory = entry.getKey();
				List<HibernateOrmLoadingIndexedTypeContext<? extends E>> types = entry.getValue();
				HibernateOrmComposableEntityLoader<? extends E> loader =
						createForMultipleTypes( loaderFactory, types, mutableLoadingOptions );
				for ( HibernateOrmLoadingIndexedTypeContext<? extends E> type : types ) {
					delegateByEntityName.put( type.getJpaEntityName(), loader );
				}
			}
			return new HibernateOrmByTypeEntityLoader<>( delegateByEntityName );
		}
	}

	private HibernateOrmComposableEntityLoader<? extends E> createForSingleType(
			HibernateOrmLoadingIndexedTypeContext<? extends E> typeContext,
			MutableEntityLoadingOptions mutableLoadingOptions) {
		return typeContext.getLoaderFactory().create(
				typeContext,
				session,
				cacheLookupStrategy,
				mutableLoadingOptions
		);
	}

	private HibernateOrmComposableEntityLoader<? extends E> createForMultipleTypes(
			EntityLoaderFactory loaderFactory,
			List<HibernateOrmLoadingIndexedTypeContext<? extends E>> types,
			MutableEntityLoadingOptions mutableLoadingOptions) {
		return loaderFactory.create(
				types,
				session,
				cacheLookupStrategy,
				mutableLoadingOptions
		);
	}

}
