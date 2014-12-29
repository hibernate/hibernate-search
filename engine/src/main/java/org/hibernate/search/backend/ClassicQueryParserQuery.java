/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.apache.lucene.util.Version;

/**
 * EXPERT: allows for deletion of queries that are parsed from a Lucene QueryParser. this could yield problems when used
 * in a distributed context as the behaviour of QueryParser changes from version to version and if you upgrade one node
 * and it doesn't understand the queries correctly your indexes can get out of sync. If you are using this to delete
 * from the index you should reindex all indices after you have upgraded all your nodes to the newest version of
 * Hibernate-Search.
 *
 * @author Martin Braun
 */
public class ClassicQueryParserQuery implements DeletionQuery {

	// TODO: maybe replace this with a more modular approach to let users
	// specify their own queries so we won't be repsonsible for them shooting
	// in their own feet

	public static final int QUERY_KEY = 2;
	public static final Version LUCENE_VERSION = Version.LUCENE_4_10_2;

	private final Version version;
	private final String queryString;

	public ClassicQueryParserQuery(Version version, String queryString) {
		this.version = version;
		this.queryString = queryString;
	}

	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	public Version getVersion() {
		return version;
	}

	public String getQuery() {
		return queryString;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( queryString == null ) ? 0 : queryString.hashCode() );
		result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		ClassicQueryParserQuery other = (ClassicQueryParserQuery) obj;
		if ( queryString == null ) {
			if ( other.queryString != null ) {
				return false;
			}
		}
		else if ( !queryString.equals( other.queryString ) ) {
			return false;
		}
		if ( version == null ) {
			if ( other.version != null ) {
				return false;
			}
		}
		else if ( !version.equals( other.version ) ) {
			return false;
		}
		return true;
	}

}
