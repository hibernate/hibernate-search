package org.hibernate.search.backend.spi;

import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.DeleteByQuerySupport;

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
	 * @param entityType
	 *            the class to operate on
	 * @param deleteByQuery
	 *            the query to delete by
	 * @throws IllegalArgumentException
	 *             if a unsupported SerializableQuery is passed
	 */
	public DeleteByQueryWork(Class<?> entityType,
			DeletionQuery deleteByQuery) {
		super(entityType, null, WorkType.DELETE_BY_QUERY);
		if (entityType == null) {
			throw new IllegalArgumentException("entityType must not be null");
		}
		Class<? extends DeletionQuery> clazz = DeleteByQuerySupport.SUPPORTED_TYPES
				.get(deleteByQuery.getQueryKey());
		if (clazz != null && clazz.equals(deleteByQuery.getClass())) {
			this.deleteByQuery = deleteByQuery;
		} else {
			throw new IllegalArgumentException(
					"unsupported SerializableQuery passed. you can't supply your own custom type here!");
		}
	}

	public DeletionQuery getDeleteByQuery() {
		return deleteByQuery;
	}

}
