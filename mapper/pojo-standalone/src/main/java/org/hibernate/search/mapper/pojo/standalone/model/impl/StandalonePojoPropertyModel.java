/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.model.impl;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class StandalonePojoPropertyModel<T> extends AbstractPojoHCAnnPropertyModel<T, StandalonePojoBootstrapIntrospector> {

	StandalonePojoPropertyModel(StandalonePojoBootstrapIntrospector introspector,
			StandalonePojoRawTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties,
			List<Member> members) {
		super( introspector, holderTypeModel, name, declaredXProperties, members );
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReadHandle<T> createHandle(Member member) throws IllegalAccessException {
		return (ValueReadHandle<T>) introspector.createValueReadHandle( member );
	}
}
