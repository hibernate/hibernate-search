/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import static org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys.key;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class LuceneProjectionTypeKeys {

	private LuceneProjectionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<?> EXPLANATION = key( "explanation" );
	public static final SearchQueryElementTypeKey<?> DOCUMENT = key( "document" );

}
