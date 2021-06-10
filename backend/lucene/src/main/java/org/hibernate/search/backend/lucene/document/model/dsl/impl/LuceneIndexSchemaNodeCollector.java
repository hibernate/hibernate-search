/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;


import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueField;

public interface LuceneIndexSchemaNodeCollector {
	void collect(String absolutePath, LuceneIndexObjectField node);

	void collect(String absoluteFieldPath, LuceneIndexValueField<?> node);

	void collect(LuceneIndexSchemaObjectFieldTemplate template);

	void collect(LuceneIndexSchemaValueFieldTemplate template);
}
