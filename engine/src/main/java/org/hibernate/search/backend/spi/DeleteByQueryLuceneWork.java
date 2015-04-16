/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.LuceneWork;


/**
 * Representation of deleteByQuery(...) from Lucene. Currently not all functionality of Lucene is supported but can be
 * added
 *
 * @hsearch.experimental
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
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitDeleteByQueryWork( this, p );
	}

	@Override
	public String toString() {
		return "DeleteByQueryLuceneWork: " + this.getEntityClass().getName() + ": " + this.deletionQuery.toString();
	}

}
