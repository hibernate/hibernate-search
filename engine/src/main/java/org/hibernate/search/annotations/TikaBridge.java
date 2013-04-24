/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a bridge for integrating with <a href="http://tika.apache.org">Apache Tika</a>.
 * <p>
 * The bridge supports the following data types:
 * <ul>
 * <li>{@code String} - where the string value is interpreted as a file path</li>
 * <li>{@code URI} - where the URI is interpreted as a resource URI</li>
 * <li>{@code byte[]}</li>
 * <li>{@code java.sql.Blob}</li>
 * </ul>
 * </p>
 *
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface TikaBridge {

	/**
	 * @return Class used for optional Tika metadata pre- and post-processing
	 */
	Class<?> metadataProcessor() default void.class;

	/**
	 * @return Class used for optionally providing a Tika parsing context
	 */
	Class<?> parseContextProvider() default void.class;
}
