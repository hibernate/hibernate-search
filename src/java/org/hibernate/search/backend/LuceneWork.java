//$Id$
package org.hibernate.search.backend;

import java.io.Serializable;

import org.apache.lucene.document.Document;

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
	private final Class entityClass;
	private final Serializable id;
	
	/**
	 * Flag indicating if this lucene work has to be indexed in batch mode.
	 */
	private final boolean batch;
	private final String idInString;

	public LuceneWork(Serializable id, String idInString, Class entity) {
		this( id, idInString, entity, null );
	}

	public LuceneWork(Serializable id, String idInString, Class entity, Document document) {
		this( id, idInString, entity, document, false );
	}

	public LuceneWork(Serializable id, String idInString, Class entity, Document document, boolean batch) {
		this.id = id;
		this.idInString = idInString;
		this.entityClass = entity;
		this.document = document;
		this.batch = batch;
	}

	public boolean isBatch() {
		return batch;
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
	
	public abstract <T> T getWorkDelegate(WorkVisitor<T> visitor);

}
