/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

public interface PojoPropertyModel<T> {

	String getName();

	<A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType);

	Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType);

	/**
	 * @return A model of this property's type. Implementations may decide to return a model of the raw type,
	 * or to retain at least some generics information, allowing for more precise results
	 * in {@link PojoGenericTypeModel#getTypeArgument(Class, int)} for example.
	 *
	 * @see ErasingPojoGenericTypeModel
	 */
	PojoGenericTypeModel<T> getTypeModel();

	PropertyHandle getHandle();

}
