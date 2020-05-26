/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * A {@link PojoTypeModel} representing a raw type,
 * where generic type parameters are ignored.
 * <p>
 * This excludes in particular parameterized types such as {@code ArrayList<Integer>},
 * because we cannot tell the difference between instances of such types and instances of the same type
 * with different parameters, such as {@code ArrayList<String>}.
 * Thus the mapper would be unable to find which mapping to use when indexing such an instance,
 * and it would be impossible to target the index from the {@link Class} only.
 * <p>
 *
 * @see PojoTypeModel
 * @param <T> The pojo type
 */
public interface PojoRawTypeModel<T> extends PojoTypeModel<T>, MappableTypeModel {

	/**
	 * @return {@code this}.
	 */
	@Override
	default PojoRawTypeModel<T> rawType() {
		return this;
	}

	/**
	 * @return The identifier for this type.
	 */
	PojoRawTypeIdentifier<T> typeIdentifier();

	/**
	 * @return The supertypes of the current type, in ascending order.
	 */
	@Override
	Stream<? extends PojoRawTypeModel<? super T>> ascendingSuperTypes();

	/**
	 * @return The supertypes of the current type, in descending order.
	 */
	@Override
	Stream<? extends PojoRawTypeModel<? super T>> descendingSuperTypes();

	Stream<Annotation> annotations();

	Stream<PojoPropertyModel<?>> declaredProperties();

	PojoCaster<T> caster();

}
