package org.hibernate.search.backend;

import java.io.Serializable;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * @author Martin Braun
 */
public class DeleteByQueryLuceneWork extends LuceneWork implements Serializable {

	private static final long serialVersionUID = -5384359572337884952L;

	private final DeletionQuery deletionQuery;

	public DeleteByQueryLuceneWork(Class<?> entity, DeletionQuery deletionQuery) {
		super(null, null, entity);
		this.deletionQuery = deletionQuery;
	}

	public DeletionQuery getDeletionQuery() {
		return this.deletionQuery;
	}

	@Override
	public <T> T getWorkDelegate(WorkVisitor<T> visitor) {
		return visitor.getDelegate(this);
	}

	@Override
	public String toString() {
		return "DeleteByQueryLuceneWork: " + this.getEntityClass().getName()
				+ ": " + this.deletionQuery.toString();
	}

}
