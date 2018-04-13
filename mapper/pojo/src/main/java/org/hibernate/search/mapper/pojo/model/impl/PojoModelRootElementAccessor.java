/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;

/**
 * @param <T> The type of the root element.
 */
class PojoModelRootElementAccessor<T> implements PojoModelElementAccessor<T> {

	PojoModelRootElementAccessor() {
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, this accessor will only be passed PojoElement returning type T
	public T read(PojoElement bridgedElement) {
		return (T) ((PojoElementImpl) bridgedElement).get();
	}

}