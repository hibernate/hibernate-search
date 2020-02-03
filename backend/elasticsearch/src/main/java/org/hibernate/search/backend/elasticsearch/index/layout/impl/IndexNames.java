/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.layout.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class IndexNames {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static String normalizeName(String indexName) {
		String esIndexName = indexName.toLowerCase( Locale.ENGLISH );
		if ( !esIndexName.equals( indexName ) ) {
			log.debugf( "Normalizing index name from '%1$s' to '%2$s'", indexName, esIndexName );
		}
		return esIndexName;
	}

	public static URLEncodedString encodeName(String name) {
		return URLEncodedString.fromString( normalizeName( name ) );
	}

	private final String hibernateSearch;
	private final URLEncodedString write;
	private final URLEncodedString read;

	public IndexNames(String hibernateSearch, URLEncodedString write, URLEncodedString read) {
		this.hibernateSearch = hibernateSearch;
		this.write = write;
		this.read = read;
	}

	@Override
	public String toString() {
		return "IndexNames[" +
				"hibernateSearch=" + hibernateSearch +
				", read=" + read +
				", write=" + write +
				"]";
	}

	/**
	 * The Hibernate Search index name,
	 * i.e. the name that Hibernate Search uses internally to designate that index, for example in configuration files.
	 */
	public String getHibernateSearch() {
		return hibernateSearch;
	}

	/**
	 * The write name,
	 * i.e. the name that Hibernate Search is supposed to use when indexing or purging the index.
	 */
	public URLEncodedString getWrite() {
		return write;
	}

	/**
	 * The read name,
	 * i.e. the name that Hibernate Search is supposed to use when executing searches on the index.
	 */
	public URLEncodedString getRead() {
		return read;
	}
}
