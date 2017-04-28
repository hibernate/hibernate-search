/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

/**
 * Useful paths to compose Elasticsearch URLs.
 */
public interface Paths {

	URLEncodedString _EXPLAIN = URLEncodedString.fromString( "_explain" );
	URLEncodedString _QUERY = URLEncodedString.fromString( "_query" );
	URLEncodedString _FLUSH = URLEncodedString.fromString( "_flush" );
	URLEncodedString _OPTIMIZE = URLEncodedString.fromString( "_optimize" );
	URLEncodedString _DELETE_BY_QUERY = URLEncodedString.fromString( "_delete_by_query" );
	URLEncodedString _FORCEMERGE = URLEncodedString.fromString( "_forcemerge" );
	URLEncodedString _COUNT = URLEncodedString.fromString( "_count" );
	URLEncodedString _CLOSE = URLEncodedString.fromString( "_close" );
	URLEncodedString _SETTINGS = URLEncodedString.fromString( "_settings" );
	URLEncodedString _OPEN = URLEncodedString.fromString( "_open" );
	URLEncodedString _MAPPING = URLEncodedString.fromString( "_mapping" );
	URLEncodedString _REFRESH = URLEncodedString.fromString( "_refresh" );
	URLEncodedString _SEARCH = URLEncodedString.fromString( "_search" );
	URLEncodedString _TEMPLATE = URLEncodedString.fromString( "_template" );
	URLEncodedString _CLUSTER = URLEncodedString.fromString( "_cluster" );
	URLEncodedString _BULK = URLEncodedString.fromString( "_bulk" );

	URLEncodedString SCROLL = URLEncodedString.fromString( "scroll" );
	URLEncodedString HEALTH = URLEncodedString.fromString( "health" );

}
