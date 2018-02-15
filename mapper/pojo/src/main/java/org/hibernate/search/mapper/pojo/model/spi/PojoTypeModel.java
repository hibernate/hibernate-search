/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.TypeModel;

public interface PojoTypeModel<T> extends TypeModel {

	@Override
	Stream<? extends PojoTypeModel<? super T>> getAscendingSuperTypes();

	@Override
	Stream<? extends PojoTypeModel<? super T>> getDescendingSuperTypes();

	/**
	 * @return The Java {@link Class} for this type. The type is {@code Class<? super T>} and not {@code Class<T>}
	 * on purpose: some type models can refer to parameterized types such as {@code Collection<Integer>},
	 * in which case this method will return {@code Collection.class} (the raw type),
	 * because that's the best possible representation of this parameterized type as a {@link Class}.
	 */
	Class<? super T> getJavaClass();

	<U> Optional<PojoTypeModel<U>> getSuperType(Class<U> superClassCandidate);

	<A extends Annotation> Optional<A> getAnnotationByType(Class<A> annotationType);

	<A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType);

	Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType);

	PojoPropertyModel<?> getProperty(String propertyName);

	Stream<PojoPropertyModel<?>> getDeclaredProperties();

	T cast(Object instance);
}
