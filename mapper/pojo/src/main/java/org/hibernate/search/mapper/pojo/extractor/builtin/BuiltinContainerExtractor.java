/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

public enum BuiltinContainerExtractor {

	ARRAY( ArrayElementExtractor.class ),
	COLLECTION( CollectionElementExtractor.class ),
	ITERABLE( IterableElementExtractor.class ),
	MAP_KEY( MapKeyExtractor.class ),
	MAP_VALUE( MapValueExtractor.class ),
	OPTIONAL_DOUBLE( OptionalDoubleValueExtractor.class ),
	OPTIONAL_INT( OptionalIntValueExtractor.class ),
	OPTIONAL_LONG( OptionalLongValueExtractor.class ),
	OPTIONAL_VALUE( OptionalValueExtractor.class ),

	/*
	 * Indicates that the container extractor should be selected automatically based on the type of the container.
	 */
	AUTOMATIC( null );

	private Class<? extends ContainerExtractor> type;

	BuiltinContainerExtractor(Class<? extends ContainerExtractor> type) {
		this.type = type;
	}

	public Class<? extends ContainerExtractor> getType() {
		return type;
	}

}
