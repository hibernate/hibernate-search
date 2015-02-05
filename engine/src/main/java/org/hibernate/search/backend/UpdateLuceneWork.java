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

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * Carries a Lucene update operation from the engine to the backend
 *
 * @since 4.0
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class UpdateLuceneWork extends LuceneWork {

	private static final long serialVersionUID = -5267394465359187688L;

	private final Map<String, String> fieldToAnalyzerMap;

	public UpdateLuceneWork(Serializable id, String idInString, Class<?> entity, Document document) {
		this( id, idInString, entity, document, null );
	}

	public UpdateLuceneWork(Serializable id, String idInString, Class<?> entity, Document document, Map<String, String> fieldToAnalyzerMap) {
		super( id, idInString, entity, document );
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	@Override
	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

	@Override
	public String toString() {
		return "UpdateLuceneWork: " + this.getEntityClass().getName() + "#" + this.getIdInString();
	}

}
