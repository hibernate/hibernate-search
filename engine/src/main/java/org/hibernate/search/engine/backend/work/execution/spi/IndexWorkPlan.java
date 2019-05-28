/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.concurrent.CompletableFuture;

/**
 * A set of works to be executed on an index.
 * <p>
 * Works are accumulated when methods such as {@link #add(DocumentReferenceProvider, DocumentContributor)}
 * or {@link #update(DocumentReferenceProvider, DocumentContributor)} are called,
 * and executed only when {@link #execute()} is called.
 * <p>
 * Relative ordering of works within a work plan will be preserved.
 * <p>
 * Implementations may not be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface IndexWorkPlan<D> {

	/**
	 * Add a document to the index, assuming that the document is absent from the index.
	 *
	 * @param documentReferenceProvider A source of information about the identity of the document to add.
	 * @param documentContributor A contributor to the document, adding fields to the indexed document.
	 */
	void add(DocumentReferenceProvider documentReferenceProvider, DocumentContributor<D> documentContributor);

	/**
	 * Update a document in the index, or add it if it's absent from the index.
	 *
	 * @param documentReferenceProvider A source of information about the identity of the document to update.
	 * @param documentContributor A contributor to the document, adding fields to the indexed document.
	 */
	void update(DocumentReferenceProvider documentReferenceProvider, DocumentContributor<D> documentContributor);

	/**
	 * Delete a document from the index.
	 *
	 * @param documentReferenceProvider A source of information about the identity of the document to delete.
	 */
	void delete(DocumentReferenceProvider documentReferenceProvider);

	/**
	 * Prepare the work plan execution, i.e. execute as much as possible without writing to the index.
	 * <p>
	 * Calling this method is optional: the {@link #execute()} method
	 * will perform the preparation if necessary.
	 */
	void prepare();

	/**
	 * Start executing all the works in this plan, and clear the plan so that it can be re-used.
	 *
	 * @return A {@link CompletableFuture} that will be completed when all the works are complete.
	 */
	CompletableFuture<?> execute();

}
