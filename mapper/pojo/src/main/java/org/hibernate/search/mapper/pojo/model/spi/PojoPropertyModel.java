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

	Class<? super T> getJavaClass();

	<A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType);

	Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType);

	PojoTypeModel<T> getTypeModel();

	PropertyHandle getHandle();

}
