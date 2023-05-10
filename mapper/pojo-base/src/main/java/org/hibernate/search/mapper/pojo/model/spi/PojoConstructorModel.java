/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;

public interface PojoConstructorModel<T> {

	/**
	 * @return All annotations on this constructor.
	 */
	Stream<Annotation> annotations();

	/**
	 * @return A model of this constructor's constructed type.
	 */
	PojoRawTypeModel<T> typeModel();

	/**
	 * @return A handle to call this constructor.
	 */
	ValueCreateHandle<T> handle();

	/**
	 * @param index The index of a {@link #declaredParameters() declared parameter} in this constructor.
	 * @return A representation of the parameter with the given index.
	 * @throws org.hibernate.search.util.common.SearchException If there is no parameter with the given index in this constructor.
	 */
	PojoMethodParameterModel<?> parameter(int index);

	/**
	 * @return All declared parameters of this constructor.
	 */
	List<PojoMethodParameterModel<?>> declaredParameters();

	/**
	 * @return An array containing the Java types of all {@link #declaredParameters() declared parameters} of this constructor.
	 */
	Class<?>[] parametersJavaTypes();
}
