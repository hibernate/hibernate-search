/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

/**
 * A model representing a POJO type: its structure (properties and their type),
 * its name, ...
 * <p>
 * Most of the time, type models represent a Java class,
 * either {@link PojoRawTypeModel raw} or {@link PojoGenericTypeModel parameterized}.
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
}
