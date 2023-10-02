/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.work;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * The entry point for explicit, large-scale index operations.
 * <p>
 * A {@link SearchWorkspace} targets a pre-defined set of indexed types (and their indexes),
 * filtered to only affect a single tenant, if relevant.
 * <p>
 * While {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED indexing listeners}
 * generally take care of indexing entities as they are persisted/deleted in the database,
 * there are cases where massive operations must be applied to the index,
 * such as completely purging the index.
 * This is where the {@link SearchWorkspace} comes in.
 */
public interface SearchWorkspace {

	/**
	 * Delete all documents from indexes targeted by this workspace.
	 * <p>
	 * With multi-tenancy enabled, only documents of the current tenant will be removed:
	 * the tenant that was targeted by the session from where this workspace originated.
	 */
	void purge();

	/**
	 * Asynchronous version of {@link #purge()}, returning as soon as the operation is queued.
	 *
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 * @see #purge()
	 */
	CompletionStage<?> purgeAsync();

	/**
	 * Delete documents from indexes targeted by this workspace
	 * that were indexed with any of the given routing keys.
	 * <p>
	 * With multi-tenancy enabled, only documents of the current tenant will be removed:
	 * the tenant that was targeted by the session from where this workspace originated.
	 *
	 * @param routingKeys The set of routing keys.
	 * If non-empty, only documents that were indexed with these routing keys will be deleted.
	 * If empty, documents will be deleted regardless of their routing key.
	 */
	void purge(Set<String> routingKeys);

	/**
	 * Asynchronous version of {@link #purge(Set)}, returning as soon as the operation is queued.
	 *
	 * @param routingKeys The set of routing keys.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 * @see #purge(Set)
	 */
	CompletionStage<?> purgeAsync(Set<String> routingKeys);

	/**
	 * Flush to disk the changes to indexes that were not committed yet.
	 * In the case of backends with a transaction log (Elasticsearch),
	 * also apply operations from the transaction log that were not applied yet.
	 * <p>
	 * This is generally not useful as Hibernate Search commits changes automatically.
	 * Only to be used by experts fully aware of the implications.
	 * <p>
	 * Note that some operations may still be waiting in a queue when {@link #flush()} is called,
	 * in particular operations queued as part of processing an indexing plan before a transaction
	 * is committed.
	 * These operations will not be applied immediately just because a call to {@link #flush()} is issued:
	 * the "flush" here is a very low-level operation handled by the backend.
	 */
	void flush();

	/**
	 * Asynchronous version of {@link #flush()}, returning as soon as the operation is queued.
	 *
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 * @see #flush()
	 */
	CompletionStage<?> flushAsync();

	/**
	 * Refresh the indexes so that all changes executed so far will be visible in search queries.
	 * <p>
	 * This is generally not useful as indexes are refreshed automatically,
	 * either after every change (default for the Lucene backend)
	 * or periodically (default for the Elasticsearch backend,
	 * possible for the Lucene backend by setting a refresh interval).
	 * Only to be used by experts fully aware of the implications.
	 * <p>
	 * Note that some operations may still be waiting in a queue when {@link #refresh()} is called,
	 * in particular operations queued as part of processing an indexing plan before a transaction is committed.
	 * These operations will not be applied immediately just because a call to {@link #refresh()} is issued:
	 * the "refresh" here is a very low-level operation handled by the backend.
	 */
	void refresh();

	/**
	 * Asynchronous version of {@link #refresh()}, returning as soon as the operation is queued.
	 *
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 * @see #refresh()
	 */
	CompletionStage<?> refreshAsync();

	/**
	 * Merge all segments of the indexes targeted by this workspace into a single one.
	 * <p>
	 * Note this operation may affect performance positively as well as negatively.
	 * See the reference documentation for more information.
	 */
	void mergeSegments();

	/**
	 * Asynchronous version of {@link #mergeSegments()}, returning as soon as the operation is queued.
	 * <p>
	 * Note this operation may affect performance positively as well as negatively.
	 * See the reference documentation for more information.
	 *
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 * @see #mergeSegments()
	 */
	CompletionStage<?> mergeSegmentsAsync();

}
