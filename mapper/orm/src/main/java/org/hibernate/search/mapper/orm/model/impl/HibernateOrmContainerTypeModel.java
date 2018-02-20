/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoContainerTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class HibernateOrmContainerTypeModel<E> implements PojoContainerTypeModel<E> {

	private final Class<?> containerClass;
	private final PojoTypeModel<E> elementTypeModel;

	HibernateOrmContainerTypeModel(Class<?> containerClass, PojoTypeModel<E> elementTypeModel) {
		this.containerClass = containerClass;
		this.elementTypeModel = elementTypeModel;
	}

	@Override
	public boolean isSubTypeOf(Class<?> other) {
		return other.isAssignableFrom( containerClass );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public PojoTypeModel<E> getElementTypeModel() {
		return elementTypeModel;
	}
}
