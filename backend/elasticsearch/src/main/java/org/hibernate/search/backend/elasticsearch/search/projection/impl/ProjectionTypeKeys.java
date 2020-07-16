/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;

public final class ProjectionTypeKeys {

	private ProjectionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<ElasticsearchFieldProjection.TypeSelector<?>> FIELD = key( "field" );
	public static final SearchQueryElementTypeKey<DistanceToFieldProjectionBuilder> DISTANCE = key( "distance" );

	private static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "projection", name );
	}

}
