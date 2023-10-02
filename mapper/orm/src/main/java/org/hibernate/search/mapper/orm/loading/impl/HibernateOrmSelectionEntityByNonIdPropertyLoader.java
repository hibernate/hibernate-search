/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * An entity loader for indexed entities whose document ID is not the entity ID,
 * but another property.
 *
 * @param <E> The type of loaded entities.
 */
class HibernateOrmSelectionEntityByNonIdPropertyLoader<E> extends AbstractHibernateOrmSelectionEntityLoader<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoLoadingTypeContext<E> targetEntityTypeContext;
	private final String documentIdSourcePropertyName;
	private final ValueReadHandle<?> documentIdSourceHandle;

	HibernateOrmSelectionEntityByNonIdPropertyLoader(EntityMappingType entityMappingType,
			PojoLoadingTypeContext<E> targetEntityTypeContext,
			TypeQueryFactory<E, ?> queryFactory,
			String documentIdSourcePropertyName,
			ValueReadHandle<?> documentIdSourceHandle,
			HibernateOrmLoadingSessionContext sessionContext,
			MutableEntityLoadingOptions loadingOptions) {
		super( entityMappingType, queryFactory, sessionContext, loadingOptions );
		this.targetEntityTypeContext = targetEntityTypeContext;
		this.documentIdSourcePropertyName = documentIdSourcePropertyName;
		this.documentIdSourceHandle = documentIdSourceHandle;
	}

	@Override
	protected List<E> doLoadEntities(List<?> allIds, Long timeout) {
		Map<Object, E> entityById = CollectionHelper.newHashMap( allIds.size() );

		int fetchSize = loadingOptions.fetchSize();
		Query<E> query = createQuery( fetchSize, timeout );

		List<Object> ids = new ArrayList<>( fetchSize );
		for ( Object documentIdSourceValue : allIds ) {
			ids.add( documentIdSourceValue );
			if ( ids.size() >= fetchSize ) {
				query.setParameterList( IDS_PARAMETER_NAME, ids );
				addResults( entityById, query.getResultList() );
				ids.clear();
			}
		}
		if ( !ids.isEmpty() ) {
			query.setParameterList( IDS_PARAMETER_NAME, ids );
			addResults( entityById, query.getResultList() );
		}

		List<E> result = new ArrayList<>( allIds.size() );
		for ( Object identifier : allIds ) {
			result.add( entityById.get( identifier ) );
		}
		return result;
	}

	private void addResults(Map<Object, E> resultMap, List<? extends E> loadedEntities) {
		for ( E loadedEntity : loadedEntities ) {
			// The handle may point to a field, in which case it won't work on a proxy. Unproxy first.
			Object unproxied = Hibernate.unproxy( loadedEntity );
			Object documentIdSourceValue = documentIdSourceHandle.get( unproxied );
			Object previous = resultMap.put( documentIdSourceValue, loadedEntity );
			if ( previous != null ) {
				throw log.foundMultipleEntitiesForDocumentId( targetEntityTypeContext.entityName(),
						documentIdSourcePropertyName, documentIdSourceValue );
			}
		}
	}

}
