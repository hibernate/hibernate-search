/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

final class HibernateOrmQueryUtils {

	private HibernateOrmQueryUtils() {
	}

	public static Query<?> createQueryForLoadByUniqueProperty(SessionImplementor session,
			EntityPersister persister, String uniquePropertyName, String parameterName) {
		MetamodelImplementor metamodel = session.getSessionFactory().getMetamodel();
		EntityTypeDescriptor<?> typeDescriptorOrNull = metamodel.entity( persister.getEntityName() );
		if ( typeDescriptorOrNull != null ) {
			return createQueryForLoadByUniqueProperty( session, typeDescriptorOrNull, uniquePropertyName, parameterName );
		}
		else {
			// Most likely this is a dynamic-map entity; they don't have a representation in the JPA metamodel
			// and can't be queried using the Criteria API.
			// Use a HQL query instead, even if it feels a bit dirty.
			return session.createQuery(
					"select e from " + persister.getEntityName()
							+ " e where " + uniquePropertyName + " in (:" + parameterName + ")",
					(Class<?>) persister.getMappedClass()
			);
		}
	}

	private static <E> Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session,
			EntityTypeDescriptor<E> typeDescriptor, String uniquePropertyName, String parameterName) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		ParameterExpression<Collection> idsParameter = criteriaBuilder.parameter( Collection.class, parameterName );
		CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( typeDescriptor.getJavaType() );
		Root<E> root = criteriaQuery.from( typeDescriptor );
		Path<?> uniquePropertyInRoot = root.get( typeDescriptor.getSingularAttribute( uniquePropertyName ) );
		criteriaQuery.where( uniquePropertyInRoot.in( idsParameter ) );
		return session.createQuery( criteriaQuery );
	}
}
