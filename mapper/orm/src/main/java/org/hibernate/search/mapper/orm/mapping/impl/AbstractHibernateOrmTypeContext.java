/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

abstract class AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeTypeContext<E>, HibernateOrmListenerTypeContext {
	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;

	AbstractHibernateOrmTypeContext(PojoRawTypeIdentifier<E> typeIdentifier, String entityName) {
		this.typeIdentifier = typeIdentifier;
		this.entityName = entityName;
	}

	@Override
	public PojoRawTypeIdentifier<E> getTypeIdentifier() {
		return typeIdentifier;
	}

	public String getEntityName() {
		return entityName;
	}
}
