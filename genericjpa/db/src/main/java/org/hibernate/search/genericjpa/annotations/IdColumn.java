/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.annotations;

import java.lang.annotation.Retention;

import org.hibernate.search.genericjpa.db.ColumnType;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Martin on 21.07.2015.
 */
@Retention(RUNTIME)
public @interface IdColumn {

	/**
	 * the column-name in the original table
	 *
	 * @return
	 */
	String column();

	/**
	 * the column-name in the update table
	 */
	String updateTableColumn() default "";

	ColumnType columnType();

	/**
	 * used to specify manual DDL code for the {@link IdColumn#updateTableColumn()}
	 */
	String columnDefinition() default "";

}
