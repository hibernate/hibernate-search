/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoContainerTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

class JavaBeanContainerTypeModel<E> implements PojoContainerTypeModel<E> {

	private final Class<?> rawContainerType;
	private final PojoTypeModel<E> elementTypeModel;

	JavaBeanContainerTypeModel(Class<?> rawContainerType, PojoTypeModel<E> elementTypeModel) {
		this.rawContainerType = rawContainerType;
		this.elementTypeModel = elementTypeModel;
	}

	@Override
	public boolean isSubTypeOf(Class<?> other) {
		return other.isAssignableFrom( rawContainerType );
	}

	@Override
	public PojoTypeModel<E> getElementTypeModel() {
		return elementTypeModel;
	}
}
