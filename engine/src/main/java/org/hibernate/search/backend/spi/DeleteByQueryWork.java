/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.impl.DeleteByQuerySupport;

/**
 * Delete via a serializable query
 *
 * @author Martin Braun
 */
public class DeleteByQueryWork extends Work {

	// Design note: this is a subclass of Work because I didn't want
	// to pollute the Work class with more fields that
	// don't have a meaning for the other WorkTypes.

	private final DeletionQuery deleteByQuery;

	/**
	 * Creates a DeleteByWork
	 *
	 * @param entityType the class to operate on
	 * @param deleteByQuery the query to delete by
	 * @throws IllegalArgumentException if a unsupported SerializableQuery is passed
	 */
	public DeleteByQueryWork(Class<?> entityType, DeletionQuery deletionQuery) {
		super( entityType, null, WorkType.DELETE_BY_QUERY );
		if ( entityType == null ) {
			throw new IllegalArgumentException( "entityType must not be null" );
		}
		if ( DeleteByQuerySupport.isSupported( deletionQuery.getClass() ) ) {
			this.deleteByQuery = deletionQuery;
		}
		else {
			throw new IllegalArgumentException( "unsupported SerializableQuery passed. you can't supply your own custom class here!" );
		}
	}

	public DeletionQuery getDeleteByQuery() {
		return deleteByQuery;
	}

}
