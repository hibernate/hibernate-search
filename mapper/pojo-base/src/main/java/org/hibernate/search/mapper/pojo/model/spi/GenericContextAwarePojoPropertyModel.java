/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

final class GenericContextAwarePojoPropertyModel<T> implements PojoPropertyModel<T> {

	private final PojoPropertyModel<? super T> rawPropertyModel;
	private final GenericContextAwarePojoGenericTypeModel<T> genericPropertyTypeModel;

	GenericContextAwarePojoPropertyModel(
			PojoPropertyModel<? super T> rawPropertyModel,
			GenericContextAwarePojoGenericTypeModel<T> genericPropertyTypeModel) {
		this.rawPropertyModel = rawPropertyModel;
		this.genericPropertyTypeModel = genericPropertyTypeModel;
	}

	@Override
	public String getName() {
		return rawPropertyModel.getName();
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return rawPropertyModel.getAnnotations();
	}

	@Override
	public PojoGenericTypeModel<T> getTypeModel() {
		return genericPropertyTypeModel;
	}

	@Override
	@SuppressWarnings("unchecked") // We know that, in the current generic context, this cast is legal
	public ValueReadHandle<T> getHandle() {
		return (ValueReadHandle<T>) rawPropertyModel.getHandle();
	}
}
