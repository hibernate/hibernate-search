/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Martin on 20.07.2015.
 */
@Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)
@Repeatable( UpdateInfos.class )
public @interface UpdateInfo {

	/**
	 * the name of the original table
	 */
	String tableName();

	/**
	 * the name of the table the events are stored in
	 */
	String updateTableName() default "";

	/**
	 * the name for the update table's id column
	 * (unrelated to your domain model)
	 */
	String updateTableIdColumn() default "";

	/**
	 * the name for the update table's event column
	 */
	String updateTableEventTypeColumn()  default "";

	IdInfo[] idInfos();

}
