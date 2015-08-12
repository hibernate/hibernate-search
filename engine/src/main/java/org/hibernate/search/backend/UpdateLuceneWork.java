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

/**
 * Carries a Lucene update operation from the engine to the backend
 *
 * @since 4.0
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class UpdateLuceneWork extends LuceneWork {

	private static final long serialVersionUID = -5267394465359187688L;

	private final Map<String, String> fieldToAnalyzerMap;

	public UpdateLuceneWork(Serializable id, String idInString, Class<?> entity, Document document) {
		this( null, id, idInString, entity, document, null );
	}

	public UpdateLuceneWork(String tenantId, Serializable id, String idInString, Class<?> entity, Document document) {
		this( tenantId, id, idInString, entity, document, null );
	}

	public UpdateLuceneWork(Serializable id, String idInString, Class<?> entity, Document document, Map<String, String> fieldToAnalyzerMap) {
		this( null, id, idInString, entity, document );
	}

	public UpdateLuceneWork(String tenantId, Serializable id, String idInString, Class<?> entity, Document document, Map<String, String> fieldToAnalyzerMap) {
		super( tenantId, id, idInString, entity, document );
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	@Override
	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitUpdateWork( this, p );
	}

	@Override
	public String toString() {
		String tenant = getTenantId() == null ? "" : " [" + getTenantId() + "] ";
		return "UpdateLuceneWork" + tenant + ": " + this.getEntityClass().getName() + "#" + this.getIdInString();
	}

}
