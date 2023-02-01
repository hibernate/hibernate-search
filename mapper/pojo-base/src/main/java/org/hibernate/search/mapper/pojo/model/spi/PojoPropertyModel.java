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

public interface PojoPropertyModel<T> {

	/**
	 * @return The name of this property, without the {@code get}/{@code set} prefixes of getters/setters.
	 */
	String name();

	/**
	 * @return All annotations on this property.
	 */
	Stream<Annotation> annotations();

	/**
	 * @return A model of this property's type. Implementations may decide to implement their own,
	 * but could also simply use {@link GenericContextAwarePojoGenericTypeModel}.
	 */
	PojoTypeModel<T> typeModel();

	/**
	 * @return A handle to read the value of this property on a instance of its hosting type.
	 */
	ValueReadHandle<T> handle();

}
