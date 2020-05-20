/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * An entity loader for indexed entities whose document ID is not the entity ID,
 * but another property.
 *
 * @param <E> The type of loaded entities.
 */
public class HibernateOrmNonEntityIdPropertyEntityLoader<E> implements HibernateOrmComposableEntityLoader<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME = "docId";

	public static EntityLoaderFactory factory(EntityPersister entityPersister,
			String documentIdSourcePropertyName, ValueReadHandle<?> documentIdSourceHandle) {
		return new Factory( entityPersister, documentIdSourcePropertyName, documentIdSourceHandle );
	}

	private final EntityPersister entityPersister;
	private final String documentIdSourcePropertyName;
	private final ValueReadHandle<?> documentIdSourceHandle;
	private final SessionImplementor session;
	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmNonEntityIdPropertyEntityLoader(
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
	public void loadBlocking(List<EntityReference> references, Map<? super EntityReference, ? super E> entitiesByReference) {
		Map<Object, EntityReference> documentIdSourceValueToReference = new HashMap<>();
		for ( EntityReference reference : references ) {
			documentIdSourceValueToReference.put( reference.id(), reference );
		}

		List<? extends E> loadedEntities = loadEntities( documentIdSourceValueToReference.keySet() );

		for ( E loadedEntity : loadedEntities ) {
			// The handle may point to a field, in which case it won't work on a proxy. Unproxy first.
			Object unproxied = Hibernate.unproxy( loadedEntity );
			Object documentIdSourceValue = documentIdSourceHandle.get( unproxied );

			EntityReference reference = documentIdSourceValueToReference.get( documentIdSourceValue );

			entitiesByReference.put( reference, loadedEntity );
		}
	}

	private List<? extends E> loadEntities(Collection<Object> documentIdSourceValues) {
		int fetchSize = loadingOptions.fetchSize();
		Query<? extends E> query = createQuery( fetchSize );

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

	@SuppressWarnings("unchecked") // Cast is safe because entityPersister represents type E. See Factory.doCreate().
	private Query<? extends E> createQuery(int fetchSize) {
		Query<? extends E> query = (Query<? extends E>) HibernateOrmUtils.createQueryForLoadByUniqueProperty(
				session, entityPersister, documentIdSourcePropertyName,
				DOCUMENT_ID_SOURCE_PROPERTY_PARAMETER_NAME
		);

		query.setFetchSize( fetchSize );

		EntityGraphHint<?> entityGraphHint = loadingOptions.entityGraphHintOrNullForType( entityPersister );
		if ( entityGraphHint != null ) {
			query.applyGraph( entityGraphHint.graph, entityGraphHint.semantic );
		}

		return query;
	}

	private static class Factory implements EntityLoaderFactory {

		private final EntityPersister entityPersister;
		private final String documentIdSourcePropertyName;
		private final ValueReadHandle<?> documentIdSourceHandle;

		private Factory(EntityPersister entityPersister,
				String documentIdSourcePropertyName,
				ValueReadHandle<?> documentIdSourceHandle) {
			this.entityPersister = entityPersister;
			this.documentIdSourcePropertyName = documentIdSourcePropertyName;
			this.documentIdSourceHandle = documentIdSourceHandle;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
				return false;
			}
			Factory other = (Factory) obj;
			// If the entity type is different,
			// the factories work in separate ID spaces and should be used separately.
			return entityPersister.equals( other.entityPersister )
					&& documentIdSourcePropertyName.equals( other.documentIdSourcePropertyName )
					&& documentIdSourceHandle.equals( other.documentIdSourceHandle );
		}

		@Override
		public int hashCode() {
			return Objects.hash( entityPersister, documentIdSourcePropertyName, documentIdSourceHandle );
		}

		@Override
		public <E> HibernateOrmComposableEntityLoader<E> create(
				HibernateOrmLoadingIndexedTypeContext targetEntityTypeContext,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			return doCreate( targetEntityTypeContext, session, cacheLookupStrategy, loadingOptions );
		}

		@Override
		public <E> HibernateOrmComposableEntityLoader<? extends E> create(
				List<HibernateOrmLoadingIndexedTypeContext> targetEntityTypeContexts,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			if ( targetEntityTypeContexts.size() != 1 ) {
				throw new AssertionFailure(
						"Attempt to use a criteria-based entity loader with multiple target entity types."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected entity name: " + entityPersister.getEntityName()
								+ " Targeted entity names: "
								+ targetEntityTypeContexts.stream()
										.map( HibernateOrmLoadingIndexedTypeContext::hibernateOrmEntityName )
										.collect( Collectors.toList() )
				);
			}

			return doCreate( targetEntityTypeContexts.get( 0 ), session, cacheLookupStrategy, loadingOptions );
		}

		private <E> HibernateOrmComposableEntityLoader<E> doCreate(
				HibernateOrmLoadingIndexedTypeContext targetEntityTypeContext,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy,
				MutableEntityLoadingOptions loadingOptions) {
			if ( !entityPersister.equals( targetEntityTypeContext.entityPersister() ) ) {
				throw new AssertionFailure(
						"Attempt to use a criteria-based entity loader with an unexpected target entity type."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected entity name: " + entityPersister.getEntityName()
								+ " Targeted entity name: " + targetEntityTypeContext.hibernateOrmEntityName()
				);
			}

			/*
			 * We checked just above that "entityPersister" is equal to "targetEntityTypeContext.entityPersister()",
			 * so this loader will actually return entities of type E.
			 */
			HibernateOrmComposableEntityLoader<E> result = new HibernateOrmNonEntityIdPropertyEntityLoader<>(
					entityPersister, documentIdSourcePropertyName, documentIdSourceHandle,
					session, loadingOptions
			);

			if ( !EntityLoadingCacheLookupStrategy.SKIP.equals( cacheLookupStrategy ) ) {
				/*
				 * We can't support preliminary cache lookup with this strategy,
				 * because document IDs are not entity IDs.
				 * However, we can't throw an exception either,
				 * because this setting may still be relevant for other entity types targeted by the same query.
				 * Let's log something, at least.
				 */
				log.skippingPreliminaryCacheLookupsForNonEntityIdEntityLoader(
						targetEntityTypeContext.jpaEntityName(), cacheLookupStrategy
				);
			}

			return result;
		}
	}
}
