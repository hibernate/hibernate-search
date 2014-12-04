/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

/**
 * Determines how to encode the indexed value.
 *
 * @author Hardy Ferentschik
 */
public enum EncodingType {
	/**
	 * Values are indexed as strings
	 */
	STRING,

	/**
	 * Values are indexed numerically
	 */
	NUMERIC
}
