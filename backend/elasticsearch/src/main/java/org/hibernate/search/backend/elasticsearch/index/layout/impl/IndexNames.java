/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		return name == null ? null : URLEncodedString.fromString( normalizeName( name ) );
	}

	private final String hibernateSearch;
	private final URLEncodedString write;
	private final boolean writeIsAlias;
	private final URLEncodedString read;
	private final boolean readIsAlias;

	public IndexNames(String hibernateSearch, URLEncodedString write, boolean writeIsAlias,
			URLEncodedString read, boolean readIsAlias) {
		this.hibernateSearch = hibernateSearch;
		this.write = write;
		this.writeIsAlias = writeIsAlias;
		this.read = read;
		this.readIsAlias = readIsAlias;
	}

	@Override
	public String toString() {
		return "IndexNames[" +
				"hibernateSearch=" + hibernateSearch +
				", read=" + read +
				", write=" + write +
				", readIsAlias=" + readIsAlias +
				", writeIsAlias=" + writeIsAlias +
				"]";
	}

	/**
	 * @return The Hibernate Search index name,
	 * i.e. the name that Hibernate Search uses internally to designate that index, for example in configuration files.
	 */
	public String hibernateSearchIndex() {
		return hibernateSearch;
	}

	/**
	 * @return The write name,
	 * i.e. the name that Hibernate Search is supposed to use when indexing or purging the index.
	 */
	public URLEncodedString write() {
		return write;
	}

	/**
	 * @return Whether the {@link #write write name} is an alias ({@code true}) or not ({@code false}).
	 */
	public boolean writeIsAlias() {
		return writeIsAlias;
	}

	/**
	 * @return The read name,
	 * i.e. the name that Hibernate Search is supposed to use when executing searches on the index.
	 */
	public URLEncodedString read() {
		return read;
	}

	/**
	 * @return Whether the {@link #write write name} is an alias ({@code true}) or not ({@code false}).
	 */
	public boolean readIsAlias() {
		return readIsAlias;
	}
}
