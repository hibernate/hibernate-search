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

	private final LuceneFieldFormatter<?> formatter;

	private final LuceneFieldCodec<T> codec;

	private final LuceneFieldQueryFactory queryFactory;

	private final LuceneFieldSortContributor sortContributor;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String fieldName, LuceneFieldFormatter<?> formatter, LuceneFieldCodec<T> codec,
			LuceneFieldQueryFactory queryFactory, LuceneFieldSortContributor sortContributor) {
		this.parent = parent;
		this.fieldName = fieldName;
		this.absoluteFieldPath = parent.getAbsolutePath( fieldName );
		this.formatter = formatter;
		this.codec = codec;
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

	public LuceneFieldFormatter<?> getFormatter() {
		return formatter;
	}

	public LuceneFieldCodec<T> getEncoding() {
		return codec;
	}

	public LuceneFieldQueryFactory getQueryFactory() {
		return queryFactory;
	}

	public LuceneFieldSortContributor getSortContributor() {
		return sortContributor;
	}

	public boolean isCompatibleWith(LuceneIndexSchemaFieldNode<?> other) {
		if ( !Objects.equals( formatter, other.formatter )
				|| !Objects.equals( codec, other.codec )
				|| !Objects.equals( queryFactory, other.queryFactory )
				|| !Objects.equals( sortContributor, other.sortContributor ) ) {
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
				.append( ", codec=" ).append( codec )
				.append( ", queryFactory=" ).append( queryFactory )
				.append( ", sortContributor" ).append( sortContributor )
				.append( "]" );
		return sb.toString();
	}
}
