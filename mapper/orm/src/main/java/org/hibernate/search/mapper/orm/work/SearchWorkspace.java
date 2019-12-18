/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work;

import java.util.concurrent.CompletableFuture;

/**
 * The entry point for explicit index operations.
 * <p>
 * A {@link SearchWorkspace} targets a pre-defined set of indexed types (and their indexes),
 * filtered to only affect a single tenant, if relevant.
 * <p>
 * While {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#AUTOMATIC_INDEXING_STRATEGY automatic indexing}
 * generally takes care of indexing entities as they are persisted/deleted in the database,
 * there are cases where massive operations must be applied to the index,
 * such as completely purging the index.
 * This is where the {@link SearchWorkspace} comes in.
 */
public interface SearchWorkspace {

	/**
	 * Purge the data targeted by this workspaces, removing all documents.
	 * <p>
	 * When using multi-tenancy, only documents of one tenant will be removed:
	 * the tenant that was targeted by the session from where this workspace originated.
	 */
	void purge();

	/**
	 * Asynchronous version of {@link #purge()}, returning as soon as the operation is queued.
	 *
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 * @see #purge()
	 */
	CompletableFuture<?> purgeAsync();

	/**
	 * Flush to disk the changes to indexes that were not committed yet.
	 * In the case of backends with a transaction log (Elasticsearch),
	 * also apply operations from the transaction log that were not applied yet.
	 * <p>
	 * This is generally not useful as Hibernate Search commits changes automatically.
	 * Only to be used by experts fully aware of the implications.
	 * <p>
	 * Note that some operations may still be waiting in a queue when {@link #flush()} is called,
	 * in particular operations queued as part of automatic indexing before a transaction
	 * is committed.
	 * These operations will not be applied immediately just because  a call to {@link #flush()} is issued:
	 * the "flush" here is a very low-level operation managed by the backend.
	 */
	void flush();

	/**
	 * Asynchronous version of {@link #flush()}, returning as soon as the operation is queued.
	 *
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 * @see #flush()
	 */
	CompletableFuture<?> flushAsync();

	/**
	 * Merge all segments of the indexes targeted by this workspace into a single one.
	 * <p>
	 * Note this operation may affect performance positively as well as negatively.
	 * See the reference documentation for more information.
	 */
	void forceMerge();

	/**
	 * Asynchronous version of {@link #forceMerge()}, returning as soon as the operation is queued.
	 * <p>
	 * Note this operation may affect performance positively as well as negatively.
	 * See the reference documentation for more information.
	 *
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 * @see #forceMerge()
	 */
	CompletableFuture<?> forceMergeAsync();

}
