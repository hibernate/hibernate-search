/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Optional;

/**
 * A {@link PojoTypeModel} representing a parameterized type,
 * where generic type arguments are known.
 * <p>
 * This type model offers additional reflection capabilities,
 * such as the ability to retrieve the type argument for a given generic supertype.
 *
 * @see GenericContextAwarePojoGenericTypeModel the default implementation
 *
 * @param <T> The type represented by this model
 */
public interface PojoGenericTypeModel<T> extends PojoTypeModel<T> {

	/**
	 * @param rawSuperType The supertype to resolve type parameters for
	 * @param typeParameterIndex The index of the type parameter to resolve
	 * @return The model for the type argument for the type parameter defined in {@code rawSuperType}
	 * at index {@code typeParameterIndex}, or an empty optional if the current type
	 * does not extend {@code rawSuperType}.
	 * Implementations may decide to return a model of the raw type argument, or to retain generics information.
	 */
	Optional<? extends PojoGenericTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex);

	/**
	 * @return The model for the array element type, or an empty optional if the current type
	 * is not an array type.
	 * Implementations may decide to return a model of the raw array element type, or to retain generics information.
	 */
	Optional<? extends PojoGenericTypeModel<?>> arrayElementType();

	/**
	 * @param target The type to cast to.
	 * @param <U> The type to cast to.
	 * @return A new type model, representing the current type cast to the given type,
	 * or {@link Optional#empty()} if casting is not supported.
	 * The type model will retain as much contextual type information as possible (type arguments, ...),
	 * so casting {@code List<Integer>} to {@code Collection} for example would return {@code Collection<Integer>}.
	 */
	<U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target);
}
