/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.annotations;

import java.lang.annotation.Retention;

import org.hibernate.search.db.ColumnType;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Martin Braun
 * @hsearch.experimental
 */
@Retention(RUNTIME)
public @interface IdColumn {

	/**
	 * the column-name in the original table
	 */
	String column();

	/**
	 * the column-name in the update table
	 */
	String updateTableColumn() default "";

	/**
	 * If you need something else than String, Integer or Long
	 * you have to specify CUSTOM and also give a suitable {@link org.hibernate.search.db.IdConverter} in the
	 * {@link IdInfo} and manually give the columnDefinition in {@link #columnDefinition()}
	 */
	ColumnType columnType();

	/**
	 * used to specify manual DDL code for the {@link IdColumn#updateTableColumn()}
	 */
	String columnDefinition() default "";

}
