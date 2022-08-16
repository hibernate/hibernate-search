/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.annotation.Annotation;
import java.util.Collection;
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

	/**
	 * @return The main constructor of this type.
	 * The main constructor only exists if this type defines a single constructor.
	 * @throws org.hibernate.search.util.common.SearchException If there is no main constructor for this type.
	 */
	PojoConstructorModel<T> mainConstructor();

	/**
	 * @param parameterTypes The type of parameters to the returned constructor.
	 * @return The constructor of this type whose parameters have the given {@code parameterTypes}.
	 * @throws org.hibernate.search.util.common.SearchException If there is no constructor with parameters of the given types.
	 */
	PojoConstructorModel<T> constructor(Class<?> ... parameterTypes);

	/**
	 * @return All accessible constructors of this type.
	 */
	Collection<PojoConstructorModel<T>> declaredConstructors();

	Collection<PojoPropertyModel<?>> declaredProperties();

	/**
	 * @param other The type to cast to this type.
	 * @return A new type model, representing the given type cast to this type.
	 * If casting is not possible, returns {@code this}.
	 * If casting is possible, the returned type model
	 * will retain as much contextual type information as possible (type arguments, ...),
	 * so casting {@code List<Integer>} to {@code Collection} for example would return {@code Collection<Integer>}.
	 */
	PojoTypeModel<? extends T> cast(PojoTypeModel<?> other);

	PojoCaster<T> caster();

}
