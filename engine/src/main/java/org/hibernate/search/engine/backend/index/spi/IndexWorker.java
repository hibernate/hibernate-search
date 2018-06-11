/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

/**
 * @author Yoann Rodiere
 */
public interface IndexWorker<D> {

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

}
