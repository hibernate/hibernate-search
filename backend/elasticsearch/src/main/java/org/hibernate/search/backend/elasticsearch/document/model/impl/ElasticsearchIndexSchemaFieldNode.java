/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;


public class ElasticsearchIndexSchemaFieldNode<F> {

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final String absolutePath;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final ElasticsearchIndexFieldType<F> type;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			boolean multiValued, ElasticsearchIndexFieldType<F> type) {
		this.parent = parent;
		this.absolutePath = parent.getAbsolutePath( relativeFieldName );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.type = type;
		this.multiValued = multiValued;
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getNestedPath() {
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

	public ElasticsearchIndexFieldType<F> getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( "parent=" ).append( parent )
				.append( ", type=" ).append( type )
				.append( "]" );
		return sb.toString();
	}
}
