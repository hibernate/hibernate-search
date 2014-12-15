package org.hibernate.search.backend;

import java.io.Serializable;

/**
 * serializable equivalent to {@link org.apache.lucene.search.TermQuery} and supports only
 * Strings as values
 * 
 * @author Martin Braun
 */
public class SingularTermQuery implements SerializableQuery, Serializable {

	public static final int QUERY_KEY = 1;
	private static final long serialVersionUID = 4044626299423006482L;

	private final String key;
	private final String value;

	public SingularTermQuery(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	@Override
	public String toString() {
		return "SingularTermQuery: +" + key + ":" + value;
	}

}
