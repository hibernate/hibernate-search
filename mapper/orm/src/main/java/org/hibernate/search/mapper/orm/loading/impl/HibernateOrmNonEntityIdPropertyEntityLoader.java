/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.jpa.QueryHints;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * An entity loader for indexed entities whose document ID is not the entity ID,
 * but another property.
 *
 * @param <E> The type of loaded entities.
 */
class HibernateOrmNonEntityIdPropertyEntityLoader<E> implements PojoLoader<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME = "docId";

	private final EntityPersister entityPersister;
	private final LoadingIndexedTypeContext<E> targetEntityTypeContext;
	private final TypeQueryFactory<E, ?> queryFactory;
	private final String documentIdSourcePropertyName;
	private final ValueReadHandle<?> documentIdSourceHandle;
	private final LoadingSessionContext sessionContext;
	private final MutableEntityLoadingOptions loadingOptions;
	private final boolean singleConcreteTypeInHierarchy;

	HibernateOrmNonEntityIdPropertyEntityLoader(EntityPersister entityPersister,
			LoadingIndexedTypeContext<E> targetEntityTypeContext,
			TypeQueryFactory<E, ?> queryFactory,
			String documentIdSourcePropertyName,
			ValueReadHandle<?> documentIdSourceHandle,
			LoadingSessionContext sessionContext,
			MutableEntityLoadingOptions loadingOptions) {
		this.entityPersister = entityPersister;
		this.targetEntityTypeContext = targetEntityTypeContext;
		this.queryFactory = queryFactory;
		this.documentIdSourcePropertyName = documentIdSourcePropertyName;
		this.documentIdSourceHandle = documentIdSourceHandle;
		this.sessionContext = sessionContext;
		this.loadingOptions = loadingOptions;
		this.singleConcreteTypeInHierarchy =
				entityPersister.getEntityMetamodel().getSubclassEntityNames().size() == 1;
	}

	@Override
	public List<E> loadBlocking(List<?> identifiers, Deadline deadline) {
		Map<Object, E> loadedEntities;
		Long timeout = deadline == null ? null : deadline.remainingTimeMillis();
		try {
			loadedEntities = doLoadEntities( identifiers, timeout );
		}
		catch (QueryTimeoutException | javax.persistence.QueryTimeoutException | LockTimeoutException |
				javax.persistence.LockTimeoutException e) {
			if ( deadline == null ) {
				// ORM-initiated timeout: just propagate the exception.
				throw e;
			}
			throw deadline.forceTimeoutAndCreateException( e );
		}

		List<E> result = new ArrayList<>( identifiers.size() );
		for ( Object identifier : identifiers ) {
			result.add( loadedEntities.get( identifier ) );
		}
		return result;
	}

	@Override
	// The cast is safe because we use reflection to check it.
	@SuppressWarnings("unchecked")
	public <E2 extends E> E2 castToExactTypeOrNull(PojoRawTypeIdentifier<E2> expectedType, Object loadedObject) {
		if ( singleConcreteTypeInHierarchy ) {
			// The loaded object will always be an instance of the exact same type,
			// and we can only get passed that exact type.
			return (E2) loadedObject;
		}
		else if ( expectedType.equals( sessionContext.runtimeIntrospector().detectEntityType( loadedObject ) ) ) {
			return (E2) loadedObject;
		}
		else {
			return null;
		}
	}

	private Map<Object, E> doLoadEntities(Collection<?> allIds, Long timeout) {
		int fetchSize = loadingOptions.fetchSize();

		Map<Object, E> result = CollectionHelper.newHashMap( allIds.size() );

		List<Object> ids = new ArrayList<>( fetchSize );
		for ( Object documentIdSourceValue : allIds ) {
			ids.add( documentIdSourceValue );
			if ( ids.size() >= fetchSize ) {
				// Don't reuse the query; see https://hibernate.atlassian.net/browse/HHH-14439
				Query<? extends E> query = createQuery( fetchSize, timeout );
				query.setParameterList( DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME, ids );
				addResults( result, query.getResultList() );
				ids.clear();
			}
		}
		if ( !ids.isEmpty() ) {
			Query<? extends E> query = createQuery( fetchSize, timeout );
			query.setParameterList( DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME, ids );
			addResults( result, query.getResultList() );
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
				throw log.foundMultipleEntitiesForDocumentId( targetEntityTypeContext.jpaEntityName(),
						documentIdSourcePropertyName, documentIdSourceValue );
			}
		}
	}

	private Query<? extends E> createQuery(int fetchSize, Long timeout) {
		Query<E> query = queryFactory.createQueryForLoadByUniqueProperty( sessionContext.session(),
				DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME );

		query.setFetchSize( fetchSize );
		if ( timeout != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, Math.toIntExact( timeout ) );
		}

		EntityGraphHint<?> entityGraphHint = loadingOptions.entityGraphHintOrNullForType( entityPersister );
		if ( entityGraphHint != null ) {
			query.applyGraph( entityGraphHint.graph, entityGraphHint.semantic );
		}

		return query;
	}

}
