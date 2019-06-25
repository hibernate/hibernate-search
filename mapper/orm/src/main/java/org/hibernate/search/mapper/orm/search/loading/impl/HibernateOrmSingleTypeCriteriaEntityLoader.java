/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class HibernateOrmSingleTypeCriteriaEntityLoader<E> implements HibernateOrmComposableEntityLoader<EntityReference, E> {
	private final Session session;
	private final Class<? extends E> entityType;
	private final SingularAttribute<? super E, ?> documentIdSourceProperty;
	private final MutableEntityLoadingOptions loadingOptions;

	public HibernateOrmSingleTypeCriteriaEntityLoader(
			Session session,
			Class<? extends E> entityType,
			SingularAttribute<? super E, ?> documentIdSourceProperty,
			MutableEntityLoadingOptions loadingOptions) {
		this.session = session;
		this.entityType = entityType;
		this.documentIdSourceProperty = documentIdSourceProperty;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public List<E> loadBlocking(List<EntityReference> references) {
		// Load all references
		Map<EntityReference, E> objectsByReference = new HashMap<>();
		loadBlocking( references, objectsByReference );

		// Re-create the list of objects in the same order
		List<E> result = new ArrayList<>( references.size() );
		for ( EntityReference reference : references ) {
			result.add( objectsByReference.get( reference ) );
		}
		return result;
	}

	@Override
	public void loadBlocking(List<EntityReference> references, Map<? super EntityReference, ? super E> objectsByReference) {
		Map<Object, EntityReference> documentIdSourceValueToReference = new HashMap<>();
		for ( EntityReference reference : references ) {
			documentIdSourceValueToReference.put( reference.getId(), reference );
		}

		List<EntityLoadingResult> loadingResults = loadEntities( documentIdSourceValueToReference.keySet() );

		for ( EntityLoadingResult loadingResult : loadingResults ) {
			Object documentIdSourceValue = loadingResult.documentIdSourceValue;
			EntityReference reference = documentIdSourceValueToReference.get( documentIdSourceValue );

			@SuppressWarnings("unchecked") // Safe because "root" has the type "? extends E"
			E loadedEntity = (E) loadingResult.loadedEntity;

			objectsByReference.put( reference, loadedEntity );
		}
	}

	private List<EntityLoadingResult> loadEntities(Collection<Object> documentIdSourceValues) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<EntityLoadingResult> criteriaQuery = criteriaBuilder.createQuery( EntityLoadingResult.class );

		Root<? extends E> root = criteriaQuery.from( entityType );
		Path<?> documentIdSourcePropertyInRoot = root.get( documentIdSourceProperty );

		/*
		 * Hack to get the result type we want.
		 * This is ugly, but safe, because "root" has the type "? extends E" and the second constructor parameter
		 */
		criteriaQuery.select( criteriaBuilder.construct(
				EntityLoadingResult.class,
				documentIdSourcePropertyInRoot, root
		) );
		criteriaQuery.where( documentIdSourcePropertyInRoot.in( documentIdSourceValues ) );

		return session.createQuery( criteriaQuery )
				.setFetchSize( loadingOptions.getFetchSize() )
				.getResultList();
	}

	private static class EntityLoadingResult {
		private final Object documentIdSourceValue;
		private final Object loadedEntity;

		public EntityLoadingResult(Object documentIdSourceValue, Object loadedEntity) {
			this.documentIdSourceValue = documentIdSourceValue;
			this.loadedEntity = loadedEntity;
		}
	}
}
