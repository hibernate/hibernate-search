package org.hibernate.search.backend;

import org.apache.lucene.util.Version;

/**
 * EXPERT: allows for deletion of queries that are parsed from a Lucene
 * QueryParser. this could yield problems when used in a distributed context as
 * the behaviour of QueryParser changes from version to version and if you
 * upgrade one node and it doesn't understand the queries correctly your indexes
 * can get out of sync.
 * 
 * If you are using this to delete from the index you should reindex all indices
 * after you have upgraded all your nodes to the newest version of
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

}
