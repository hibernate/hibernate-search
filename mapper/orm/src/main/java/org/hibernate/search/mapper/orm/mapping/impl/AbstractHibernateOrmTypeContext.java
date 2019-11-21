/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import javax.persistence.metamodel.EntityType;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRuntimeIntrospectorTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.AssertionFailure;

abstract class AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeTypeContext<E>, HibernateOrmListenerTypeContext,
				HibernateOrmRuntimeIntrospectorTypeContext {
	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final EntityTypeDescriptor<E> entityType;

	@SuppressWarnings("unchecked")
	AbstractHibernateOrmTypeContext(SessionFactoryImplementor sessionFactory,
			PojoRawTypeIdentifier<E> typeIdentifier, String entityName) {
		this.typeIdentifier = typeIdentifier;
		this.entityType = (EntityTypeDescriptor<E>) getEntityTypeByJpaEntityName( sessionFactory, entityName );
	}

	@Override
	public PojoRawTypeIdentifier<E> getTypeIdentifier() {
		return typeIdentifier;
	}

	public String getEntityName() {
		return entityType.getName();
	}

	public EntityTypeDescriptor<E> getEntityType() {
		return entityType;
	}

	private static EntityTypeDescriptor<?> getEntityTypeByJpaEntityName(
			SessionFactoryImplementor sessionFactory, String jpaEntityName) {
		// This is ugly, but there is no other way to get the entity type from its JPA entity name...
		for ( EntityType<?> entity : sessionFactory.getMetamodel().getEntities() ) {
			if ( jpaEntityName.equals( entity.getName() ) ) {
				return (EntityTypeDescriptor<?>) entity;
			}
		}
		throw new AssertionFailure(
				"Could not find the entity type with name '" + jpaEntityName + "'."
						+ " There is a bug in Hibernate Search, please report it."
		);
	}
}
