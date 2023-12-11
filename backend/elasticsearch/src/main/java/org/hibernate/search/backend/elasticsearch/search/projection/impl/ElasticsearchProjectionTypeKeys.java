/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class ElasticsearchProjectionTypeKeys {

	private ElasticsearchProjectionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<?> JSON_HIT = of( "projection:json-hit" );
	public static final SearchQueryElementTypeKey<?> SOURCE = of( "projection:source" );
	public static final SearchQueryElementTypeKey<?> EXPLANATION = of( "projection:explanation" );

}
