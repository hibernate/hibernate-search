/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes;

import org.apache.lucene.index.IndexReader;

/**
 * The {@code IndexReaderAccessor} exposes {@link org.apache.lucene.index.IndexReader}s directly, making it possible to query the Lucene
 * indexes directly bypassing Hibernate Search.
 * <p>
 * The returned IndexReader instances are always read-only and must be closed
 * using the {@link #close(IndexReader)} method on this same instance.
 * </p>
 * <P>
 * <b>Note:</b> this API is intended for power users intending to extract information directly.
 * </p>
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public interface IndexReaderAccessor {

	/**
	 * Opens an IndexReader on all indexes containing the entities passed as parameter.
	 * In the simplest case passing a single entity will map to a single index; if the entity
	 * uses a sharding strategy or if multiple entities using different index names are selected,
	 * the single IndexReader will act as a MultiReader on the aggregate of these indexes.
	 * This MultiReader is not filtered by Hibernate Search, so it might contain information
	 * relevant to different types as well.
	 * <p>
	 * The returned IndexReader is read only; writing directly to the index is discouraged. If you
	 * need to write to the index use the
	 * {@link org.hibernate.search.spi.SearchIntegrator#getWorker()} to queue change operations to the backend.
	 * </p>
	 * <p>
	 * The IndexReader should not be closed in other ways except being returned to this instance via
	 * {@link #close(IndexReader)}.
	 * </p>
	 *
	 * @param entities the entity types for which to return a (multi)reader
	 * @return an IndexReader containing at least all listed entities
	 * @throws java.lang.IllegalArgumentException if one of the specified classes is not indexed
	 */
	IndexReader open(Class<?>... entities);

	/**
	 * Opens an IndexReader on all named indexes.
	 * A single name can be provided, or multiple. In the case of multiple names it
	 * still returns a single IndexReader instance, but this will make it possible to run
	 * queries on the combination of each index.
	 *
	 * @param indexNames At least one IndexManager name.
	 * @return an IndexReader instance.
	 * @throws org.hibernate.search.exception.SearchException if the index manager to which the named index belongs failed to start
	 */
	IndexReader open(String... indexNames);

	/**
	 * Closes IndexReader instances obtained using {@link #open(Class...)}
	 *
	 * @param indexReader the IndexReader to be closed
	 */
	void close(IndexReader indexReader);
}
