/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin;

// FIXME Remove this type, it can be replaced with string references, which are more flexible
public enum BuiltinContainerExtractor {

	ARRAY( BuiltinContainerExtractors.ARRAY ),
	COLLECTION( BuiltinContainerExtractors.COLLECTION ),
	ITERABLE( BuiltinContainerExtractors.ITERABLE ),
	MAP_KEY( BuiltinContainerExtractors.MAP_KEY ),
	MAP_VALUE( BuiltinContainerExtractors.MAP_VALUE ),
	OPTIONAL_DOUBLE( BuiltinContainerExtractors.OPTIONAL_DOUBLE ),
	OPTIONAL_INT( BuiltinContainerExtractors.OPTIONAL_INT ),
	OPTIONAL_LONG( BuiltinContainerExtractors.OPTIONAL_LONG ),
	OPTIONAL_VALUE( BuiltinContainerExtractors.OPTIONAL ),

	/**
	 * Used as a default value in annotations.
	 * @deprecated Should not be used explicitly. This only exists for default annotation values.
 	 */
	UNDEFINED( null );

	private final String name;

	BuiltinContainerExtractor(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
