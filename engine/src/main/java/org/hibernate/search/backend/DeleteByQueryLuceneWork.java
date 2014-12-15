package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * @author Martin Braun
 */
public class DeleteByQueryLuceneWork extends LuceneWork implements Serializable {

	private static final long serialVersionUID = -5384359572337884952L;
	
	private final SerializableQuery query;

	public DeleteByQueryLuceneWork(Class<?> entity, SerializableQuery query) {
		super(null, null, entity);
		this.query = query;
	}
	
	public SerializableQuery getQuery() {
		return this.query;
	}

	@Override
	public <T> T getWorkDelegate(WorkVisitor<T> visitor) {
		return visitor.getDelegate(this);
	}

}
