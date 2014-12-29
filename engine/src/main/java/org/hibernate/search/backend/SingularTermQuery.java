package org.hibernate.search.backend;

/**
 * DeleteByQuery equivalent to {@link org.apache.lucene.search.TermQuery} and supports only
 * Strings as values
 * 
 * @author Martin Braun
 */
public class SingularTermQuery implements DeletionQuery {

	public static final int QUERY_KEY = 1;

	private final String fieldName;
	private final String value;

	public SingularTermQuery(String key, String value) {
		this.fieldName = key;
		this.value = value;
	}

	public String getFieldName() {
		return fieldName;
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
		return "SingularTermQuery: +" + fieldName + ":" + value;
	}

}
