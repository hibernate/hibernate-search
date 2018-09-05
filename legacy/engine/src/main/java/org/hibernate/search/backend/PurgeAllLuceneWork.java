/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * A unit of work used to purge an entire index.
 *
 * @author John Griffin
 */
public class PurgeAllLuceneWork extends LuceneWork {

	public PurgeAllLuceneWork(IndexedTypeIdentifier type) {
		this( null, type );
	}

	public PurgeAllLuceneWork(String tenantId, IndexedTypeIdentifier type) {
		super( tenantId, null, null, type, null );
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitPurgeAllWork( this, p );
	}

	@Override
	public String toString() {
		String tenant = getTenantId() == null ? "" : " [" + getTenantId() + "] ";
		return "PurgeAllLuceneWork" + tenant + ": " + this.getEntityType().getName();
	}

}
