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

	Stream<Annotation> annotations();

	/**
	 * @return A model of this constructor's constructed type.
	 */
	PojoRawTypeModel<T> typeModel();

	ValueCreateHandle<T> handle();

	List<PojoMethodParameterModel<?>> declaredParameters();

	Class<?>[] parametersJavaTypes();

}
