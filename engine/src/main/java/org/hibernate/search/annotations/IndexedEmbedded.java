/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that an association ({@code @*To*}, {@code @Embedded}, {@code @CollectionOfEmbedded}) is to be indexed in
 * the root entity index. This allows queries involving associated objects properties.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface IndexedEmbedded {

	/**
	 * Default value for the {@link #indexNullAs} parameter. Indicates that {@code null} values should not be indexed.
	 */
	String DO_NOT_INDEX_NULL = "__DO_NOT_INDEX_NULL__";

	/**
	 * Value for the {@link #indexNullAs} parameter indicating that {@code null} values should be indexed using the null
	 * token given through the {@link org.hibernate.search.cfg.Environment#DEFAULT_NULL_TOKEN} configuration property.
	 * If no value is given for that property, the token {@code _null_} will be used.
	 */
	String DEFAULT_NULL_TOKEN = "__DEFAULT_NULL_TOKEN__";

	/**
	 * Field name prefix, defaults to {@code propertyname.}.
	 *
	 * @return the field name prefix. Default to "."
	 */
	String prefix() default ".";

	/**
	 * <p>List which <em>paths</em> of the object graph should be included
	 * in the index, and need to match the field names used to store them in the index, so they will
	 * also match the field names used to specify full text queries.</p>
	 *
	 * <p>Defined paths are going to be indexed even if they exceed the {@code depth} threshold.
	 * When {@code includePaths} is not empty, the default value for {@code depth} is 0.</p>
	 *
	 * <p>Defined paths are implicitly prefixed with the {@link IndexedEmbedded#prefix()}.
	 *
	 * @return the paths to include. Default to empty array
	 */
	String[] includePaths() default { };

	/**
	 * Stop indexing embedded elements when {@code depth} is reached.
	 * {@code depth=1} means the associated element is indexed, but not its embedded elements.
	 *
	 * <p>The default value depends on the value of the {@code includePaths} attribute: if no paths
	 * are defined, the default is {@code Integer.MAX_VALUE}; if any {@code includePaths} are
	 * defined, the default {@code depth} is interpreted as 0 if not specified to a different value
	 * than it's default.</p>
	 *
	 * <p>Note that when defining any path to the {@code includePaths} attribute the default is zero also
	 * when explicitly set to {@code Integer.MAX_VALUE}.</p>
	 *
	 * @return the depth size. Default value is {@link Integer#MAX_VALUE}
	 */
	int depth() default Integer.MAX_VALUE;

	/**
	 * Overrides the target type of an association, in case a collection overrides the type of the collection generics.
	 * @return the target type of the association. Default to {@code void.class}
	 */
	Class<?> targetElement() default void.class;

	/**
	 * @return Returns the value to be used for indexing {@code null}. Per default
	 *         {@code IndexedEmbedded.DO_NOT_INDEX_NULL} is
	 *         returned indicating that null values are not indexed.
	 */
	String indexNullAs() default DO_NOT_INDEX_NULL;

	/**
	 * Returns {@code true}, if the id of the embedded object should be included into the index,
	 * {@code false} otherwise. The default is {@code false}.
	 *
	 * <p><b>Note</b><br>:
	 * If the id property is explicitly listed via {@link #includePaths()}, then the id is included even if this value
	 * is {@code false}.
	 * </p>
	 *
	 * @return Returns {@code true}, if the id of the embedded object should be included into the index,
	 * {@code false} otherwise.
	 */
	boolean includeEmbeddedObjectId() default false;
}
