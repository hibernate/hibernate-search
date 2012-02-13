/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
/**
 * Specifies that an association (@*To*, @Embedded, @CollectionOfEmbedded) is to be indexed
 * in the root entity index
 * It allows queries involving associated objects restrictions
 */
public @interface IndexedEmbedded {

	/**
	 * Default value for {@link #indexNullAs} parameter. Indicates that {@code null} values should not be indexed.
	 */
	public static final String DO_NOT_INDEX_NULL = "__DO_NOT_INDEX_NULL__";

	/**
	 * Value for {@link #indexNullAs} parameter indicating that {@code null} values should not indexed using the
	 */
	public static final String DEFAULT_NULL_TOKEN = "__DEFAULT_NULL_TOKEN__";

	/**
	 * Field name prefix
	 * Default to 'propertyname.'
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
	 */
	String[] includePaths() default {};

	/**
	 * <p>Stop indexing embedded elements when depth is reached
	 * depth=1 means the associated element is indexed, but not its embedded elements.</p>
	 * 
	 * <p>The default value depends on the value of the {@code includePaths} attribute: if no paths
	 * are defined, the default is {@code Integer.MAX_VALUE}; if any {@code includePaths} are
	 * defined, the default {@code depth} is interpreted as 0 if not specified to a different value
	 * than it's default.</p>
	 * 
	 * <p>Note that when defining any path to the {@code includePaths} attribute the default is zero also
	 * when explicitly set to {@code Integer.MAX_VALUE}.</p>
	 */
	int depth() default Integer.MAX_VALUE;

	/**
	 * Overrides the type of an association. If a collection, overrides the type of the collection generics
	 */
	Class<?> targetElement() default void.class;

	/**
	 * @return Returns the value to be used for indexing {@code null}. Per default
	 *         {@code IndexedEmbedded.DO_NOT_INDEX_NULL} is
	 *         returned indicating that null values are not indexed.
	 */
	String indexNullAs() default DO_NOT_INDEX_NULL;
}
