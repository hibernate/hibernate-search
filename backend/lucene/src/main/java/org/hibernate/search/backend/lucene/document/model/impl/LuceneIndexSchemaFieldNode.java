/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;


public class LuceneIndexSchemaFieldNode<F> {

	private final String relativeFieldName;

	private final LuceneIndexSchemaObjectNode parent;

	private final String absoluteFieldPath;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final LuceneIndexFieldType<F> type;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String relativeFieldName,
			boolean multiValued,
			LuceneIndexFieldType<F> type) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = parent.getAbsolutePath( relativeFieldName );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.multiValued = multiValued;
		this.type = type;
	}

	public LuceneIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsoluteFieldPath() {
		return absoluteFieldPath;
	}

	public String getNestedDocumentPath() {
		return ( nestedPathHierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	/**
	 * @return {@code true} if this node is multi-valued in its parent object.
	 */
	public boolean isMultiValued() {
		return multiValued;
	}

	public LuceneIndexFieldType<F> getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", relativeFieldName=" ).append( relativeFieldName )
				.append( ", type=" ).append( type )
				.append( "]" );
		return sb.toString();
	}
}
