/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.impl;

import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;

/**
 * @param <T> The type used as a root element.
 */
public class PojoModelValueElement<T> implements PojoModelValue<T> {

	private final PojoGenericTypeModel<? extends T> typeModel;

	public PojoModelValueElement(PojoGenericTypeModel<? extends T> typeModel) {
		this.typeModel = typeModel;
	}

	@Override
	public String toString() {
		return "PojoModelValueElement[" + typeModel.toString() + "]";
	}

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		return typeModel.getRawType().isSubTypeOf( clazz );
	}

	@Override
	public Class<?> getRawType() {
		return typeModel.getRawType().getJavaClass();
	}
}