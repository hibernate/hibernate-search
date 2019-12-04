/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.reflect.Type;

import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class HibernateOrmGenericContextHelper implements GenericContextAwarePojoGenericTypeModel.Helper {
	private final HibernateOrmBootstrapIntrospector introspector;

	public HibernateOrmGenericContextHelper(HibernateOrmBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public <T> PojoRawTypeModel<T> getRawTypeModel(Class<T> clazz) {
		return introspector.getTypeModel( clazz );
	}

	@Override
	public Object getPropertyCacheKey(PojoPropertyModel<?> rawPropertyModel) {
		return rawPropertyModel; // Properties are instantiated only once per type model
	}

	@Override
	public Type getPropertyGenericType(PojoPropertyModel<?> rawPropertyModel) {
		HibernateOrmClassPropertyModel<?> ormPropertyModel = (HibernateOrmClassPropertyModel<?>) rawPropertyModel;
		return ormPropertyModel.getGetterGenericReturnType();
	}
}
