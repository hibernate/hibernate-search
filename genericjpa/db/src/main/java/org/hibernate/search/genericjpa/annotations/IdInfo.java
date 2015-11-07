/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.annotations;

import java.lang.annotation.Retention;

import org.hibernate.search.genericjpa.db.IdConverter;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Martin on 20.07.2015.
 */
@Retention(RUNTIME)
public @interface IdInfo {

	/**
	 * the Java type of the entity this corresponds to
	 */
	Class<?> entity() default void.class;

	/**
	 * the id-columns for the entity this corresponds to
	 */
	IdColumn[] columns();

	/**
	 * used for custom column types and multi valued keys
	 */
	Class<? extends IdConverter> idConverter() default IdConverter.class;

	/**
	 * hints for the entity-provider used while updating
	 */
	Hint[] hints() default {};

}
