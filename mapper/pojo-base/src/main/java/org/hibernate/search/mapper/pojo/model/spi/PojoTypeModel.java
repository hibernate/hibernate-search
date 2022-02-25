/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.util.Optional;

/**
 * A model representing a POJO type: its structure (properties and their type),
 * its name, ...
 * <p>
 * Most of the time, type models represent a Java class,
 * either {@link PojoRawTypeModel raw} or {@link GenericContextAwarePojoGenericTypeModel parameterized}.
 * However, it is also possible that a given type model
 * represents a <strong>subset</strong> of all instances of a given Java class,
 * which all follow a common convention regarding their structure.
 * <p>
 * For example, a type model could represent a Map-based type
 * where properties are defined by the map entries
 * and where all instances are required to have a value of type {@code Integer} for the key {@code "age"}.
 *
 * @param <T> The pojo type
 */
public interface PojoTypeModel<T> {

	/**
	 * @return A human-readable name for this type.
	 */
	String name();

	/**
	 * @return A representation of the closest parent Java {@link Class} for this type.
	 */
	PojoRawTypeModel<? super T> rawType();

	PojoPropertyModel<?> property(String propertyName);

	/**
	 * @param target The type to cast to.
	 * @param <U> The type to cast to.
	 * @return A new type model, representing the current type cast to the given type,
	 * or {@link Optional#empty()} if casting is not supported.
	 * The type model will retain as much contextual type information as possible (type arguments, ...),
	 * so casting {@code List<Integer>} to {@code Collection} for example would return {@code Collection<Integer>}.
	 */
	<U> Optional<PojoTypeModel<? extends U>> castTo(Class<U> target);

	/**
	 * @param rawSuperType The supertype to resolve type parameters for
	 * @param typeParameterIndex The index of the type parameter to resolve
	 * @return The model for the type argument for the type parameter defined in {@code rawSuperType}
	 * at index {@code typeParameterIndex}, or an empty optional if the current type
	 * does not extend {@code rawSuperType}.
	 * Implementations may decide to return a model of the raw type argument, or to retain generics information.
	 */
	Optional<? extends PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex);

	/**
	 * @return The model for the array element type, or an empty optional if the current type
	 * is not an array type.
	 * Implementations may decide to return a model of the raw array element type, or to retain generics information.
	 */
	Optional<? extends PojoTypeModel<?>> arrayElementType();

}
