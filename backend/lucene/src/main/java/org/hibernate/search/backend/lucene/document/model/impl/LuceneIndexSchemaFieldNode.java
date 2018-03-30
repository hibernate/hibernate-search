/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Objects;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexSchemaFieldNode<T> {

	private final String fieldName;

	private final LuceneIndexSchemaObjectNode parent;

	private final String absoluteFieldPath;

	private final LuceneFieldFormatter<T> formatter;

	private final LuceneFieldQueryFactory queryFactory;

	private final LuceneFieldSortContributor sortContributor;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String fieldName, LuceneFieldFormatter<T> formatter,
			LuceneFieldQueryFactory queryFactory, LuceneFieldSortContributor sortContributor) {
		this.parent = parent;
		this.fieldName = fieldName;
		this.absoluteFieldPath = parent.getAbsolutePath( fieldName );
		this.formatter = formatter;
		this.queryFactory = queryFactory;
		this.sortContributor = sortContributor;
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

	public LuceneFieldQueryFactory getQueryFactory() {
		return queryFactory;
	}

	public LuceneFieldSortContributor getSortContributor() {
		return sortContributor;
	}

	public boolean isCompatibleWith(LuceneIndexSchemaFieldNode<?> other) {
		if ( !Objects.equals( sortContributor, other.sortContributor ) || !Objects.equals( formatter, other.formatter )
				|| !Objects.equals( queryFactory, other.queryFactory ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", fieldName=" ).append( fieldName )
				.append( ", formatter=" ).append( formatter )
				.append( ", queryFactory=" ).append( queryFactory )
				.append( ", sortContributor" ).append( sortContributor )
				.append( "]" );
		return sb.toString();
	}
}
