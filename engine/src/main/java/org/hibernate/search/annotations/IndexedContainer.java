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
 * Specifies that a field on a container property (array, {@link java.lang.Iterable} or {@link java.util.Map})
 * is to be indexed by passing *elements* of the container to the field bridge, instead of the container itself.
 * <p>
 * Use this when indexing a {@code List<String>}, for instance, so that each element of the list will be
 * added as another field value.
 * <p>
 * Note that this annotation differs from {@code @IndexedEmbedded}: it will apply a single field bridge
 * to each element of the container and add the resulting fields to the root type, where {@code @IndexedEmbedded}
 * will embed every field of each element to the root type.
 * <p>
 * Simply put, {@code @IndexedContainer} will work in conjunction with an {@code @Field} annotation,
 * while {@code @IndexedEmbedded} will work with annotations defined on the element type.
 *
 * @author Yoann Rodiere
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface IndexedContainer {

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
	 * @return the name of the field whose field bridge to apply on the container elements. Can be omitted in case only
	 * a single field exists for the annotated property.
	 */
	String forField() default "";

	/**
	 * By default, null containers are considered empty and not indexed. Use {@link #indexNullAs()} if you want
	 * null containers to be indexed with the given value.
	 *
	 * @return Returns the value to be used for indexing {@code null}. Per default
	 *         {@code IndexedContainer.DO_NOT_INDEX_NULL} is
	 *         returned indicating that null values are not indexed.
	 */
	String indexNullAs() default DO_NOT_INDEX_NULL;

}
