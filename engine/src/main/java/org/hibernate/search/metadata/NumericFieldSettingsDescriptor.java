/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.metadata;

/**
 * Metadata related to a numeric field
 *
 * @author Hardy Ferentschik
 */
public interface NumericFieldSettingsDescriptor extends FieldSettingsDescriptor {

	/**
	 * @return the numeric precision step for this numeric field.
	 */
	int precisionStep();

	/**
	 * @return the type of numeric field
	 */
	NumericEncodingType encodingType();

	/**
	 * Defines different logical field types
	 */
	public enum NumericEncodingType {
		/**
		 * An integer encoded numeric field
		 */
		INTEGER,

		/**
		 * An long encoded numeric field
		 */
		LONG,

		/**
		 * An float encoded numeric field
		 */
		FLOAT,

		/**
		 * An double encoded numeric field
		 */
		DOUBLE,

		/**
		 * The encoding type of the numeric field is not known due to the use of a custom bridge
		 */
		UNKNOWN
	}
}


