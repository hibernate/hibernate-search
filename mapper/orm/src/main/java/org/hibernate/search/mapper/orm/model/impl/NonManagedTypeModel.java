/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;

class NonManagedTypeModel<T> extends AbstractHibernateOrmTypeModel<T> {

	NonManagedTypeModel(HibernateOrmIntrospector introspector, Class<T> type) {
		super( introspector, type );
	}

	@Override
	PropertyModel<?> createPropertyModel(String propertyName, List<XProperty> xProperties) {
		return introspector.createFallbackPropertyModel(
				this,
				null,
				EntityMode.POJO,
				propertyName,
				xProperties
		);
	}
}
