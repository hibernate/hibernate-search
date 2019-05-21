/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin;

public class BuiltinContainerExtractors {

	private BuiltinContainerExtractors() {
	}

	public static final String ARRAY = "array";
	public static final String COLLECTION = "collection";
	public static final String ITERABLE = "iterable";
	public static final String MAP_KEY = "map-key";
	public static final String MAP_VALUE = "map-value";
	public static final String OPTIONAL_DOUBLE = "optional-double";
	public static final String OPTIONAL_INT = "optional-int";
	public static final String OPTIONAL_LONG = "optional-long";
	public static final String OPTIONAL = "optional";

}
