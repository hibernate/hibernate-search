/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;


public interface LuceneIndexSchemaNodeCollector {
	void collectObjectFieldNode(String absolutePath, LuceneIndexSchemaObjectFieldNode node);

	void collectFieldNode(String absoluteFieldPath, LuceneIndexSchemaFieldNode<?> node);

	void collect(LuceneIndexSchemaObjectFieldTemplate template);

	void collect(LuceneIndexSchemaFieldTemplate template);
}
