//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.document.Document;

/**
 * @author Emmanuel Bernard
 */
public class AddLuceneWork extends LuceneWork implements Serializable {

	private static final long serialVersionUID = -2450349312813297371L;

	private final Map<String, String> fieldToAnalyzerMap;

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document) {
		this( id, idInString, entity, document, false );
	}

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document, boolean batch) {
		this( id, idInString, entity, document, null, batch );
	}

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document, Map<String, String> fieldToAnalyzerMap) {
		this( id, idInString, entity, document, fieldToAnalyzerMap, false );
	}

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document, Map<String, String> fieldToAnalyzerMap, boolean batch) {
		super( id, idInString, entity, document, batch );
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

}
