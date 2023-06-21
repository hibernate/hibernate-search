/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

/**
 * Useful paths to compose Elasticsearch URLs.
 */
public final class Paths {

	private Paths() {
		// Private constructor, do not use.
	}

	public static final URLEncodedString _EXPLAIN = URLEncodedString.fromString( "_explain" );
	public static final URLEncodedString _FLUSH = URLEncodedString.fromString( "_flush" );
	public static final URLEncodedString _DELETE_BY_QUERY = URLEncodedString.fromString( "_delete_by_query" );
	public static final URLEncodedString _FORCEMERGE = URLEncodedString.fromString( "_forcemerge" );
	public static final URLEncodedString _COUNT = URLEncodedString.fromString( "_count" );
	public static final URLEncodedString _CLOSE = URLEncodedString.fromString( "_close" );
	public static final URLEncodedString _SETTINGS = URLEncodedString.fromString( "_settings" );
	public static final URLEncodedString _OPEN = URLEncodedString.fromString( "_open" );
	public static final URLEncodedString _MAPPING = URLEncodedString.fromString( "_mapping" );
	public static final URLEncodedString _REFRESH = URLEncodedString.fromString( "_refresh" );
	public static final URLEncodedString _SEARCH = URLEncodedString.fromString( "_search" );
	public static final URLEncodedString _CLUSTER = URLEncodedString.fromString( "_cluster" );
	public static final URLEncodedString _BULK = URLEncodedString.fromString( "_bulk" );
	public static final URLEncodedString _DOC = URLEncodedString.fromString( "_doc" );
	public static final URLEncodedString _ALIASES = URLEncodedString.fromString( "_aliases" );
	public static final URLEncodedString SCROLL = URLEncodedString.fromString( "scroll" );
	public static final URLEncodedString HEALTH = URLEncodedString.fromString( "health" );

}
