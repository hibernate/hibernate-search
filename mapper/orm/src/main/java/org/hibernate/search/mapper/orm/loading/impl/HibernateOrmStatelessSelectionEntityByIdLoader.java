/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.SelectionQuery;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;

/**
 * An entity loader for indexed entities whose document ID is the entity ID,
 * using a {@link org.hibernate.StatelessSession}.
 * <p>
 * Unlike the stateful variant, this loader uses query results directly
 * instead of relying on the persistence context, since {@code StatelessSession}
 * does not maintain a meaningful persistence context.
 *
 * @param <E> A common supertype of loaded entities.
 */
class HibernateOrmStatelessSelectionEntityByIdLoader<E> extends AbstractHibernateOrmSelectionEntityLoader<E> {

	HibernateOrmStatelessSelectionEntityByIdLoader(EntityMappingType rootEntityMappingType,
			TypeQueryFactory<E, ?> queryFactory, HibernateOrmLoadingSessionContext sessionContext,
			MutableEntityLoadingOptions loadingOptions) {
		super( rootEntityMappingType, queryFactory, sessionContext, loadingOptions );
	}

	@Override
	protected List<E> doLoadEntities(List<?> allIds, Long timeout) {
		int fetchSize = loadingOptions.fetchSize();
		SelectionQuery<E> query = createQuery( fetchSize, timeout );
		List<E> loadedEntities = createListContainingNulls( allIds.size() );

		Map<Object, Integer> idToPosition = new LinkedHashMap<>( fetchSize );
		for ( int i = 0; i < allIds.size(); i++ ) {
			idToPosition.put( allIds.get( i ), i );
			if ( idToPosition.size() >= fetchSize ) {
				loadBatch( query, idToPosition, loadedEntities );
			}
		}
		if ( !idToPosition.isEmpty() ) {
			loadBatch( query, idToPosition, loadedEntities );
		}

		return loadedEntities;
	}

	private void loadBatch(SelectionQuery<E> query, Map<Object, Integer> idToPosition, List<E> loadedEntities) {
		query.setParameterList( IDS_PARAMETER_NAME, idToPosition.keySet() );
		List<E> results = query.getResultList();
		for ( E entity : results ) {
			Object entityId = entityMappingType.getIdentifierMapping().getIdentifier( entity );
			Integer position = idToPosition.get( entityId );
			if ( position != null ) {
				loadedEntities.set( position, entity );
			}
		}
		idToPosition.clear();
	}

}
