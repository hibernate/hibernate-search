/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexSchemaFieldNode<T> {

	private final String fieldName;

	private final LuceneIndexSchemaObjectNode parent;

	private final String absoluteFieldPath;

	private final LuceneFieldFormatter<T> formatter;

	private final LuceneFieldQueryFactory queryBuilder;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String fieldName, LuceneFieldFormatter<T> formatter,
			LuceneFieldQueryFactory queryBuilder) {
		this.parent = parent;
		this.fieldName = fieldName;
		this.absoluteFieldPath = parent.getAbsolutePath( fieldName );
		this.formatter = formatter;
		this.queryBuilder = queryBuilder;
	}

	public LuceneIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getAbsoluteFieldPath() {
		return absoluteFieldPath;
	}

	public LuceneFieldFormatter<T> getFormatter() {
		return formatter;
	}

	public LuceneFieldQueryFactory getQueryBuilder() {
		return queryBuilder;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[parent=" + parent + ", fieldName=" + fieldName + "]";
	}
}
