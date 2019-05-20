/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.builtin;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.ArrayElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.CollectionElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.IterableElementExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalDoubleValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalIntValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalLongValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.OptionalValueExtractor;

@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
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

	/**
	 * Used as a default value in annotations.
	 * @deprecated Should not be used explicitly. This only exists for default annotation values.
 	 */
	UNDEFINED( null );

	private final Class<? extends ContainerExtractor> type;

	BuiltinContainerExtractor(Class<? extends ContainerExtractor> type) {
		this.type = type;
	}

	public Class<? extends ContainerExtractor> getType() {
		return type;
	}

}
