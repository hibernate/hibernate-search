/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * A unit of work triggering an optimize operation.
 * This work does not propagate to a cluster: it should be filtered before being sent to
 * the network.
 *
 * @author Andrew Hahn
 * @author Emmanuel Bernard
 */
public class OptimizeLuceneWork extends LuceneWork {

	public static final OptimizeLuceneWork INSTANCE = new OptimizeLuceneWork();

	/**
	 * Optimizes the index(es) of a specific entity
	 * @param entityType the entity type
	 */
	public OptimizeLuceneWork(IndexedTypeIdentifier entityType) {
		super( null, null, null, entityType );
	}

	/**
	 * Optimizes any index
	 */
	private OptimizeLuceneWork() {
		super( null, null, null, null );
	}

	@Override
	public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
		return visitor.visitOptimizeWork( this, p );
	}

	@Override
	public String toString() {
		IndexedTypeIdentifier entityClass = this.getEntityType();
		if ( entityClass == null ) {
			return "OptimizeLuceneWork: global";
		}
		else {
			return "OptimizeLuceneWork: " + entityClass.getName();
		}
	}

}
