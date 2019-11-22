/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

abstract class AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeTypeContext<E>, HibernateOrmListenerTypeContext,
				HibernateOrmSessionTypeContext<E> {
	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final EntityTypeDescriptor<E> entityType;

	AbstractHibernateOrmTypeContext(SessionFactoryImplementor sessionFactory,
			PojoRawTypeIdentifier<E> typeIdentifier, String hibernateOrmEntityName) {
		this.typeIdentifier = typeIdentifier;
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		this.entityType = metamodel.entity( hibernateOrmEntityName );
	}

	@Override
	public PojoRawTypeIdentifier<E> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String getEntityName() {
		return entityType.getName();
	}

	public EntityTypeDescriptor<E> getEntityType() {
		return entityType;
	}
}
