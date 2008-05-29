//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

import org.apache.lucene.document.Document;

/**
 * Represent a Serializable Lucene unit work
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class LuceneWork implements Serializable {

	private final Document document;
	private final Class entityClass;
	private final Serializable id;
	
	/**
	 * Flag indicating if this lucene work has to be indexed in batch mode.
	 */
	private boolean batch = false;
	private final String idInString;

	public LuceneWork(Serializable id, String idInString, Class entity) {
		this( id, idInString, entity, null );
	}

	public LuceneWork(Serializable id, String idInString, Class entity, Document document) {
		this.id = id;
		this.idInString = idInString;
		this.entityClass = entity;
		this.document = document;
	}

	public boolean isBatch() {
		return batch;
	}

	//TODO move to final field, or enable synchronization?
	public void setBatch(boolean batch) {
		this.batch = batch;
	}

	public Document getDocument() {
		return document;
	}

	public Class getEntityClass() {
		return entityClass;
	}

	public Serializable getId() {
		return id;
	}

	public String getIdInString() {
		return idInString;
	}
}
