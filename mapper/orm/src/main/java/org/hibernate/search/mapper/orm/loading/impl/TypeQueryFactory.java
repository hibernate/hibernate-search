/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

interface TypeQueryFactory<E> {

	static TypeQueryFactory<?> create(SessionFactoryImplementor sessionFactory, EntityPersister entityPersister,
			String uniquePropertyName) {
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		EntityTypeDescriptor<?> typeDescriptorOrNull = metamodel.entity( entityPersister.getEntityName() );
		if ( typeDescriptorOrNull != null ) {
			return CriteriaTypeQueryFactory.create( typeDescriptorOrNull, uniquePropertyName );
		}
		else {
			// Most likely this is a dynamic-map entity; they don't have a representation in the JPA metamodel
			// and can't be queried using the Criteria API.
			// Use HQL queries instead, even if it feels a bit dirty.
			return new HqlTypeQueryFactory<>( entityPersister, uniquePropertyName );
		}
	}

	Query<E> createQueryForLoadByUniqueProperty(SessionImplementor session, String parameterName);

}
