/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;

import org.apache.lucene.document.Document;

public interface LuceneIdWriter {
	/**
	 * Writes an ID to the document. Implementations chose which filed name to use
	 * and how many fields are used to represent the ID.
	 * @param id The id to add to the document.
	 * @param document The document to write to.
	 * @see LuceneBackendSettings#SCHEMA_ID_STRATEGY
	 */
	void write(String id, Document document);
}
