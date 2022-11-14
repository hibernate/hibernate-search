/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public enum UuidDataType {
	/**
	 * Database specific. Some databases might not work correctly with a particular representation of UUID hence the
	 * default provides a sensible value depending on which particular database is used. By default, a character representation
	 * is used as characters should be overall supported by databases.
	 * <p>
	 * But for some databases, a different value is used. For instance, in the case of Microsoft SQL Server,
	 * using a character representation might lead to undesired behavior in the form of deadlocks
	 * so a binary representation is used by default.
	 */
	DEFAULT( "default" ) {
		@Override
		public String type(Dialect dialect) {
			// See HSEARCH-4749 why MSSQL is treated differently
			if ( dialect instanceof SQLServerDialect ) {
				return BINARY.type( dialect );
			}
			return CHAR.type( dialect );
		}
	},

	UUID( "uuid" ),

	BINARY( "uuid-binary" ),

	CHAR( "uuid-char" );

	private final String externalRepresentation;

	UuidDataType(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public String externalRepresentation() {
		return externalRepresentation;
	}

	public String type(Dialect dialect) {
		return externalRepresentation();
	}

	public static String uuidType(String value, Dialect dialect) {
		for ( UuidDataType dataType : UuidDataType.values() ) {
			if ( dataType.externalRepresentation().equals( value ) ) {
				return dataType.type( dialect );
			}
		}
		return value;
	}
}
