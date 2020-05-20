/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public class HibernateOrmCriteriaEntityLoader<E> implements HibernateOrmComposableEntityLoader<E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <E> EntityLoaderFactory factory(EntityTypeDescriptor<E> entityType,
			SingularAttribute<? super E,?> documentIdSourceAttribute,
			ValueReadHandle<?> documentIdSourceHandle) {
		return new Factory<>( entityType, documentIdSourceAttribute, documentIdSourceHandle );
	}

	private final EntityTypeDescriptor<? extends E> entityType;
	private final SingularAttribute<? super E, ?> documentIdSourceAttribute;
	private final ValueReadHandle<?> documentIdSourceHandle;
	private final Session session;
	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmCriteriaEntityLoader(
			EntityTypeDescriptor<? extends E> entityType,
			SingularAttribute<? super E, ?> documentIdSourceAttribute,
			ValueReadHandle<?> documentIdSourceHandle,
			Session session,
			MutableEntityLoadingOptions loadingOptions) {
		this.entityType = entityType;
		this.documentIdSourceAttribute = documentIdSourceAttribute;
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
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<? extends E> criteriaQuery = criteriaBuilder.createQuery( entityType.getJavaType() );

		Root<? extends E> root = criteriaQuery.from( entityType );
		Path<?> documentIdSourcePropertyInRoot = root.get( documentIdSourceAttribute );

		criteriaQuery.where( documentIdSourcePropertyInRoot.in( documentIdSourceValues ) );

		Query<? extends E> query = session.createQuery( criteriaQuery );

		query.setFetchSize( loadingOptions.fetchSize() );

		return query.getResultList();
	}

	private static class Factory<E> implements EntityLoaderFactory {

		private final EntityTypeDescriptor<E> entityType;
		private final SingularAttribute<? super E, ?> documentIdSourceAttribute;
		private final ValueReadHandle<?> documentIdSourceHandle;

		private Factory(EntityTypeDescriptor<E> entityType,
				SingularAttribute<? super E, ?> documentIdSourceAttribute,
				ValueReadHandle<?> documentIdSourceHandle) {
			this.entityType = entityType;
			this.documentIdSourceAttribute = documentIdSourceAttribute;
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
			return entityType.equals( other.entityType )
					&& documentIdSourceAttribute.equals( other.documentIdSourceAttribute )
					&& documentIdSourceHandle.equals( other.documentIdSourceHandle );
		}

		@Override
		public int hashCode() {
			return Objects.hash( entityType, documentIdSourceAttribute, documentIdSourceHandle );
		}

		@Override
		public <E2> HibernateOrmComposableEntityLoader<E2> create(
				HibernateOrmLoadingIndexedTypeContext targetEntityTypeContext,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			return doCreate( targetEntityTypeContext, session, cacheLookupStrategy, loadingOptions );
		}

		@Override
		public <E2> HibernateOrmComposableEntityLoader<? extends E2> create(
				List<HibernateOrmLoadingIndexedTypeContext> targetEntityTypeContexts,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			if ( targetEntityTypeContexts.size() != 1 ) {
				throw new AssertionFailure(
						"Attempt to use a criteria-based entity loader with multiple target entity types."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected entity name: " + entityType.getName()
								+ " Targeted entity names: "
								+ targetEntityTypeContexts.stream()
										.map( HibernateOrmLoadingIndexedTypeContext::jpaEntityName )
										.collect( Collectors.toList() )
				);
			}

			return doCreate( targetEntityTypeContexts.get( 0 ), session, cacheLookupStrategy, loadingOptions );
		}

		private <E2> HibernateOrmComposableEntityLoader<E2> doCreate(
				HibernateOrmLoadingIndexedTypeContext targetEntityTypeContext,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy,
				MutableEntityLoadingOptions loadingOptions) {
			if ( !entityType.getName().equals( targetEntityTypeContext.jpaEntityName() ) ) {
				throw new AssertionFailure(
						"Attempt to use a criteria-based entity loader with an unexpected target entity type."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected entity name: " + entityType.getName()
								+ " Targeted entity name: " + targetEntityTypeContext.jpaEntityName()
				);
			}

			/*
			 * We checked just above that "entityType" is equal to "targetEntityType",
			 * so E = (? extends E2), so this cast is safe.
			 */
			@SuppressWarnings("unchecked")
			HibernateOrmComposableEntityLoader<E2> result =
					(HibernateOrmComposableEntityLoader<E2>) new HibernateOrmCriteriaEntityLoader<>(
							entityType, documentIdSourceAttribute, documentIdSourceHandle,
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
						entityType.getName(), cacheLookupStrategy
				);
			}

			return result;
		}
	}
}
