/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.annotations;

import java.lang.annotation.Retention;

import org.hibernate.search.db.IdConverter;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Contains Information about where the Ids are stored and how to build a Java object from it again
 *
 * @author Martin Braun
 * @hsearch.experimental
 */
@Retention(RUNTIME)
public @interface IdInfo {

	/**
	 * the Java type of the entity this corresponds to.  If this equals {@code void.class} on the Class level
	 * the Class's type is assumed (this does not work on the Member level)
	 */
	Class<?> entity() default void.class;

	/**
	 * the id-columns (in table from the {@link UpdateInfo}
	 * this {@link IdInfo} is hosted in)
	 * for the entity this corresponds to
	 */
	IdColumn[] columns();

	/**
	 * used for custom column types and multi valued keys
	 */
	Class<? extends IdConverter> idConverter() default IdConverter.class;

	/**
	 * hints for the entity-provider used while updating
	 * (this is currently only used if you provide your own) (GenericJPA only)
	 */
	// genericjpa
	Hint[] hints() default {};

}
