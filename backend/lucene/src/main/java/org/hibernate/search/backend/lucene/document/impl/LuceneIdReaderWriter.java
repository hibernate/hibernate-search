/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

/**
 * Implementations decide to which fields to write IDs to and how, later, to read them back from the index.
 */
public interface LuceneIdReaderWriter extends LuceneIdWriter, LuceneIdReader {

}
