/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;

// FIXME: This is a hack. Remove this when we split the index field type and index field DSLs in the next commits.
public interface LuceneIndexSchemaFieldDslBackReference<F> {

	IndexFieldAccessor<F> onCreateAccessor(LuceneIndexFieldType<F> type);

}
