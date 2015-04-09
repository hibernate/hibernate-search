/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * Representation of deleteByQuery(...) from Lucene. Currently not all functionality of Lucene is supported but can be
 * added
 *
 * @author Martin Braun
 */
public class DeleteByQueryLuceneWork extends LuceneWork {

	private final DeletionQuery deletionQuery;

	public DeleteByQueryLuceneWork(Class<?> entity, DeletionQuery deletionQuery) {
		this( null, entity, deletionQuery );
	}

	public DeleteByQueryLuceneWork(String tenantId, Class<?> entity, DeletionQuery deletionQuery) {
		super( tenantId, null, null, entity );
		this.deletionQuery = deletionQuery;
	}

	public DeletionQuery getDeletionQuery() {
		return this.deletionQuery;
	}

	@Override
	public <T> T getWorkDelegate(WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

	@Override
	public String toString() {
		return "DeleteByQueryLuceneWork: " + this.getEntityClass().getName() + ": " + this.deletionQuery.toString();
	}

}
