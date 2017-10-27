/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import org.hibernate.EntityMode;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;

class NonManagedTypeModel<T> implements TypeModel<T> {

	private final HibernateOrmIntrospector introspector;
	private final Class<T> type;

	NonManagedTypeModel(
			HibernateOrmIntrospector introspector,
			Class<T> type) {
		this.introspector = introspector;
		this.type = type;
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}

	@Override
	public PropertyModel<?> getProperty(String propertyName) {
		return introspector.createFallbackPropertyModel(
				this,
				null,
				EntityMode.POJO,
				propertyName
		);
	}
}
