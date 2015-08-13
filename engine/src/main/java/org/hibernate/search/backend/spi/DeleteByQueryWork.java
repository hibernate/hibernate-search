/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import org.hibernate.search.backend.impl.DeleteByQuerySupport;

/**
 * Delete via a serializable query
 *
 * @hsearch.experimental
 *
 * @author Martin Braun
 */
public class DeleteByQueryWork extends Work {

	// Design note: this is a subclass of Work because I didn't want
	// to pollute the Work class with more fields that
	// don't have a meaning for the other WorkTypes.

	private final DeletionQuery deleteByQuery;

	public DeleteByQueryWork(Class<?> entityType, DeletionQuery deletionQuery) {
		this( null, entityType, deletionQuery );
	}

	/**
	 * Creates a DeleteByWork
	 *
	 * @param tenantId the tenant identifier
	 * @param entityType the class to operate on
	 * @param deletionQuery the query to delete by
	 * @throws IllegalArgumentException if a unsupported SerializableQuery is passed
	 */
	public DeleteByQueryWork(String tenantId, Class<?> entityType, DeletionQuery deletionQuery) {
		super( tenantId, entityType, null, WorkType.DELETE_BY_QUERY );
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
