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
 * Represent a Serializable Lucene unit work
 *
 * WARNING: This class aims to be serializable and passed in an asynchronous way across VMs
 *          any non backward compatible serialization change should be done with great care
 *          and publically announced. Specifically, new versions of Hibernate Search should be
 *          able to handle changes produced by older versions of Hibernate Search if reasonably possible.
 *          That is why each subclass susceptible to be pass along have a magic serialization number.
 *          NOTE: we are relying on Lucene's Document to play nice unfortunately
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public abstract class LuceneWork implements Serializable {

	private final Document document;
	private final Class<?> entityClass;
	private final Serializable id;
	private final String idInString;

	public LuceneWork(Serializable id, String idInString, Class<?> entity) {
		this( id, idInString, entity, null );
	}

	public LuceneWork(Serializable id, String idInString, Class<?> entity, Document document) {
		this.id = id;
		this.idInString = idInString;
		this.entityClass = entity;
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public Serializable getId() {
		return id;
	}

	public String getIdInString() {
		return idInString;
	}

	public abstract <T> T getWorkDelegate(WorkVisitor<T> visitor);

	public Map<String, String> getFieldToAnalyzerMap() {
		return null;
	}

}
