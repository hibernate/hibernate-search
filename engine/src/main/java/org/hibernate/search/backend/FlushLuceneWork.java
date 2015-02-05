/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

import org.hibernate.search.backend.impl.WorkVisitor;

/**
 * Used to flush and commit asynchronous and other pending operations on the Indexes.
 * Generally not needed, this is mainly used at the end of mass indexing operations.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @since 4.1
 */
public class FlushLuceneWork extends LuceneWork {

	public static final FlushLuceneWork INSTANCE = new FlushLuceneWork();

	/**
	 * Flushes all index operations for a specific entity.
	 *
	 * @param entity the entity type for which to flush the index
	 */
	public FlushLuceneWork(Class<?> entity) {
		super( null, null, entity );
	}

	/**
	 * Flushes all index operations
	 */
	private FlushLuceneWork() {
		super( null, null, null );
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

	@Override
	public String toString() {
		Class entityClass = this.getEntityClass();
		if ( entityClass == null ) {
			return "FlushLuceneWork: global";
		}
		else {
			return "FlushLuceneWork: " + this.getEntityClass().getName();
		}
	}
}
