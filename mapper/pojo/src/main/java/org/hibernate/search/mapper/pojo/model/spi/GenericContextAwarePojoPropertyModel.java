/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

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
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return rawPropertyModel.getAnnotationsByType( annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(
			Class<? extends Annotation> metaAnnotationType) {
		return rawPropertyModel.getAnnotationsByMetaAnnotationType( metaAnnotationType );
	}

	@Override
	public PojoGenericTypeModel<T> getTypeModel() {
		return genericPropertyTypeModel;
	}

	@Override
	@SuppressWarnings("unchecked") // We know that, in the current generic context, this cast is legal
	public PropertyHandle<T> getHandle() {
		return (PropertyHandle<T>) rawPropertyModel.getHandle();
	}
}
