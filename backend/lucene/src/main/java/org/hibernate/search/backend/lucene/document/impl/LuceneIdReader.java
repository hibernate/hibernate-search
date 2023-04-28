/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.LeafReader;

public interface LuceneIdReader {

	/**
	 * Creates a {@link BinaryDocValues} backed up by a specific field depending on a configured ID read/write strategy.
	 *
	 * @param reader The reader to retrieve IDs from.
	 * @return Returns {@link BinaryDocValues} for the ID field.
	 * @throws IOException On I/O error occurred while creating doc values.
	 * @see LuceneBackendSettings#SCHEMA_ID_STRATEGY
	 */
	BinaryDocValues idDocValues(LeafReader reader) throws IOException;
}
