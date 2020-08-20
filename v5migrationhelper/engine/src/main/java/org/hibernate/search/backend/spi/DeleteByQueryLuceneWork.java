/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.spi.IndexedTypeIdentifier;


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

	public DeleteByQueryLuceneWork(IndexedTypeIdentifier typeIdentifier, DeletionQuery deletionQuery) {
		this( null, typeIdentifier, deletionQuery );
	}

	public DeleteByQueryLuceneWork(String tenantId, IndexedTypeIdentifier typeIdentifier, DeletionQuery deletionQuery) {
		super( tenantId, null, null, typeIdentifier );
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
		return "DeleteByQueryLuceneWork: " + this.getEntityType().getName() + ": " + this.deletionQuery.toString();
	}

}
