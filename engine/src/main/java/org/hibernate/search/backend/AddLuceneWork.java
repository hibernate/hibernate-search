/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * @author Emmanuel Bernard
 */
public class AddLuceneWork extends LuceneWork {

	private final Map<String, String> fieldToAnalyzerMap;

	public AddLuceneWork(Serializable id, String idInString, IndexedTypeIdentifier entityType, Document document) {
		this( null, id, idInString, entityType, document, null );
	}

	public AddLuceneWork(String tenantId, Serializable id, String idInString, IndexedTypeIdentifier entityType, Document document) {
		this( tenantId, id, idInString, entityType, document, null );
	}

	public AddLuceneWork(Serializable id, String idInString, IndexedTypeIdentifier entityType, Document document, Map<String, String> fieldToAnalyzerMap) {
		this( null, id, idInString, entityType, document, fieldToAnalyzerMap );
	}

	public AddLuceneWork(String tenantId, Serializable id, String idInString, IndexedTypeIdentifier entityType, Document document, Map<String, String> fieldToAnalyzerMap) {
		super( tenantId, id, idInString, entityType, document );
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	@Override
	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitAddWork( this, p );
	}

	@Override
	public String toString() {
		String tenant = getTenantId() == null ? "" : " [" + getTenantId() + "] ";
		return "AddLuceneWork" + tenant + ": " + this.getEntityType().getName() + "#" + this.getIdInString();
	}

}
