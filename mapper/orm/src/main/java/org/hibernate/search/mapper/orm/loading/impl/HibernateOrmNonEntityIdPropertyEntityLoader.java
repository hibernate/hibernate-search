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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.QueryTimeoutException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.jpa.QueryHints;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmComposableSearchEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityGraphHint;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * An entity loader for indexed entities whose document ID is not the entity ID,
 * but another property.
 *
 * @param <E> The type of loaded entities.
 */
class HibernateOrmNonEntityIdPropertyEntityLoader<E> implements HibernateOrmComposableSearchEntityLoader<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME = "docId";

	private final EntityPersister entityPersister;
	private final String documentIdSourcePropertyName;
	private final ValueReadHandle<?> documentIdSourceHandle;
	private final SessionImplementor session;
	private final MutableEntityLoadingOptions loadingOptions;

	HibernateOrmNonEntityIdPropertyEntityLoader(
			EntityPersister entityPersister,
			String documentIdSourcePropertyName,
			ValueReadHandle<?> documentIdSourceHandle,
			SessionImplementor session,
			MutableEntityLoadingOptions loadingOptions) {
		this.entityPersister = entityPersister;
		this.documentIdSourcePropertyName = documentIdSourcePropertyName;
		this.documentIdSourceHandle = documentIdSourceHandle;
		this.session = session;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public void loadBlocking(List<EntityReference> references,
			Map<? super EntityReference, ? super E> entitiesByReference, Deadline deadline) {
		Map<Object, EntityReference> documentIdSourceValueToReference = new HashMap<>();
		for ( EntityReference reference : references ) {
			documentIdSourceValueToReference.put( reference.id(), reference );
		}

		List<? extends E> loadedEntities;
		Long timeout = deadline == null ? null : deadline.remainingTimeMillis();
		try {
			loadedEntities = loadEntities( documentIdSourceValueToReference.keySet(), timeout );
		}
		catch (QueryTimeoutException | javax.persistence.QueryTimeoutException | LockTimeoutException |
				javax.persistence.LockTimeoutException e) {
			if ( deadline == null ) {
				// ORM-initiated timeout: just propagate the exception.
				throw e;
			}
			throw deadline.forceTimeoutAndCreateException( e );
		}

		for ( E loadedEntity : loadedEntities ) {
			// The handle may point to a field, in which case it won't work on a proxy. Unproxy first.
			Object unproxied = Hibernate.unproxy( loadedEntity );
			Object documentIdSourceValue = documentIdSourceHandle.get( unproxied );

			EntityReference reference = documentIdSourceValueToReference.get( documentIdSourceValue );

			Object previous = entitiesByReference.put( reference, loadedEntity );
			if ( previous != null ) {
				throw log.foundMultipleEntitiesForDocumentId( reference.name(), documentIdSourcePropertyName,
						reference.id() );
			}
		}
	}

	private List<? extends E> loadEntities(Collection<Object> documentIdSourceValues, Long timeout) {
		int fetchSize = loadingOptions.fetchSize();
		Query<? extends E> query = createQuery( fetchSize, timeout );

		if ( fetchSize >= documentIdSourceValues.size() ) {
			query.setParameterList( DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME, documentIdSourceValues );
			return query.getResultList();
		}
		else {
			List<E> result = new ArrayList<>( documentIdSourceValues.size() );

			List<Object> ids = new ArrayList<>( fetchSize );
			for ( Object documentIdSourceValue : documentIdSourceValues ) {
				ids.add( documentIdSourceValue );
				if ( ids.size() >= fetchSize ) {
					query.setParameterList( DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME, ids );
					result.addAll( query.getResultList() );
					ids.clear();
				}
			}
			if ( !ids.isEmpty() ) {
				query.setParameterList( DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME, ids );
				result.addAll( query.getResultList() );
			}

			return result;
		}
	}

	@SuppressWarnings("unchecked") // Cast is safe because entityPersister represents type E. See Strategy.doCreate().
	private Query<? extends E> createQuery(int fetchSize, Long timeout) {
		Query<? extends E> query = (Query<? extends E>) HibernateOrmQueryUtils.createQueryForLoadByUniqueProperty(
				session, entityPersister, documentIdSourcePropertyName,
				DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME
		);

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
