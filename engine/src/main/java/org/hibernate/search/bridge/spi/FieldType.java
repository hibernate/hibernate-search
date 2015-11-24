/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

/**
 * The type of an indexed field.
 *
 * @author Gunnar Morling
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing users.
 */
public enum FieldType {

	/**
	 * A String field.
	 */
	STRING,

	/**
	 * A boolean field, mapped to the String "true" or "false" in Lucene.
	 */
	BOOLEAN,

	/**
	 * A date, mapped to a formatted String in Lucene.
	 */
	DATE,

	/**
	 * An integer field.
	 */
	INTEGER,

	/**
	 * An long field.
	 */
	LONG,

	/**
	 * An float field.
	 */
	FLOAT,

	/**
	 * An double field.
	 */
	DOUBLE
}
