/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.reference.impl.ValueFieldReferenceImpl;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * @param <T> The type of the attribute (dsl converter). {@link org.hibernate.search.engine.backend.types.converter.spi.DslConverter}s V
 * @param <V> The type of the index field. {@link org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter}s F or {@link org.hibernate.search.engine.backend.types.converter.spi.DslConverter}s F
 * @param <P> The type of the attribute (projection converter) {@link org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter}s V.
 */
@Incubating
public interface ValueFieldReference<T, V, P> extends SearchValueFieldReference<T, P> {

	static <T, V, P> ValueFieldReference<T, V, P> of(String absolutePath, Class<T> dslType, Class<V> indexType,
			Class<P> projectionType) {
		return new ValueFieldReferenceImpl<>( absolutePath, dslType, indexType, projectionType );
	}

	/**
	 * Applies {@link ValueConvert#NO}
	 */
	SearchValueFieldReference<V, V> noConverter();

	/**
	 * Applies {@link ValueConvert#PARSE}
	 */

	SearchValueFieldReference<String, String> asString();

	@Override
	default ValueConvert valueConvert() {
		return ValueConvert.YES;
	}
}
