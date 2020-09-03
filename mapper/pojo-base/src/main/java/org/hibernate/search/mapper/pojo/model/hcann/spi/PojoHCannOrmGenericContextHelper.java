/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.reflect.Type;

import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class PojoHCannOrmGenericContextHelper implements GenericContextAwarePojoGenericTypeModel.Helper {
	private final AbstractPojoHCAnnBootstrapIntrospector introspector;

	public PojoHCannOrmGenericContextHelper(AbstractPojoHCAnnBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public <T> PojoRawTypeModel<T> rawTypeModel(Class<T> clazz) {
		return introspector.typeModel( clazz );
	}

	@Override
	public Object propertyCacheKey(PojoPropertyModel<?> rawPropertyModel) {
		return rawPropertyModel; // Properties are instantiated only once per type model
	}

	@Override
	public Type propertyGenericType(PojoPropertyModel<?> rawPropertyModel) {
		AbstractPojoHCAnnPropertyModel<?, ?> hcannPropertyModel = (AbstractPojoHCAnnPropertyModel<?, ?>) rawPropertyModel;
		return hcannPropertyModel.getterGenericReturnType();
	}
}
