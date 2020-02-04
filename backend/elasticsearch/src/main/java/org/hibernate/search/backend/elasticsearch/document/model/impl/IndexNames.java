/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

public final class IndexNames {

	private final String hibernateSearch;
	private final URLEncodedString primary;
	private final URLEncodedString write;
	private final URLEncodedString read;

	public IndexNames(String hibernateSearch, URLEncodedString primary,
			URLEncodedString write, URLEncodedString read) {
		this.hibernateSearch = hibernateSearch;
		this.primary = primary;
		this.write = write;
		this.read = read;
	}

	@Override
	public String toString() {
		return "IndexNames[" +
				"hibernateSearch=" + hibernateSearch +
				", primary=" + primary +
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
	 * The primary index name,
	 * i.e. the name that Hibernate Search is supposed to use when creating/deleting the index.
	 * <p>
	 * This is also the name used in {@link org.hibernate.search.backend.elasticsearch.mapping.impl.IndexNameTypeNameMapping}.
	 */
	public URLEncodedString getPrimary() {
		return primary;
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
