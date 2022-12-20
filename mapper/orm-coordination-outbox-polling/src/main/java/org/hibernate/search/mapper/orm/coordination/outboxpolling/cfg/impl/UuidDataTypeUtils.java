/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg.impl;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;

public final class UuidDataTypeUtils {
	private UuidDataTypeUtils() {
	}

	public static final String DEFAULT = "default";
	public static final String UUID_BINARY = "uuid-binary";
	public static final String UUID_CHAR = "uuid-char";

	/**
	 * @return In case of {@code value == "default" } a database specific result will be returned.
	 * Some databases might not work correctly with a particular representation of UUID hence the
	 * default provides a sensible value depending on which particular database is used. By default, a character representation
	 * is used as characters should be overall supported by databases.
	 * <p>
	 * But for some databases, a different value is used. For instance, in the case of Microsoft SQL Server,
	 * using a character representation might lead to undesired behavior in the form of deadlocks
	 * so a binary representation is used by default.
	 * <p>
	 * Otherwise, when {@code value != "default" } a user passed value will be used.
	 */
	public static String uuidType(String value, Dialect dialect) {
		if ( DEFAULT.equalsIgnoreCase( value ) ) {
			// See HSEARCH-4749 why MSSQL is treated differently
			if ( dialect instanceof SQLServerDialect ) {
				return UUID_BINARY;
			}
			return UUID_CHAR;
		}
		return value;
	}
}
