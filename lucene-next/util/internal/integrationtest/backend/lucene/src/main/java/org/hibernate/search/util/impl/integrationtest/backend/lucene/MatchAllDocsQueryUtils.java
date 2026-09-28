package org.hibernate.search.util.impl.integrationtest.backend.lucene;

import org.apache.lucene.search.MatchAllDocsQuery;

public final class MatchAllDocsQueryUtils {

	private MatchAllDocsQueryUtils() {
	}

	public static MatchAllDocsQuery matchAllDocsQuery() {
		return MatchAllDocsQuery.INSTANCE;
	}

}
