/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;

public final class ProjectionTypeKeys {

	private ProjectionTypeKeys() {
	}

	public static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "projection", name );
	}

	public static final SearchQueryElementTypeKey<FieldProjectionBuilder.TypeSelector> FIELD = key( "field" );
	public static final SearchQueryElementTypeKey<DistanceToFieldProjectionBuilder> DISTANCE = key( "distance" );
	public static final SearchQueryElementTypeKey<CompositeProjectionBuilder> OBJECT = key( "object" );
	public static final SearchQueryElementTypeKey<HighlightProjectionBuilder> HIGHLIGHT = key( "highlight" );

}
