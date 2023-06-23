/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueFieldTemplate;

public interface LuceneIndexNodeCollector {
	void collect(String absolutePath, LuceneIndexObjectField node);

	void collect(String absoluteFieldPath, LuceneIndexValueField<?> node);

	void collect(LuceneIndexObjectFieldTemplate template);

	void collect(LuceneIndexValueFieldTemplate template);
}
