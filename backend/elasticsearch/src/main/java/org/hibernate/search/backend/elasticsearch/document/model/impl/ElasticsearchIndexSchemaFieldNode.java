/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonElement;


public class ElasticsearchIndexSchemaFieldNode<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final String absolutePath;
	private final JsonAccessor<JsonElement> relativeAccessor;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final ElasticsearchIndexFieldType<F> type;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			boolean multiValued, ElasticsearchIndexFieldType<F> type) {
		this.parent = parent;
		this.absolutePath = parent.getAbsolutePath( relativeFieldName );
		this.relativeAccessor = JsonAccessor.root().property( relativeFieldName );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.type = type;
		this.multiValued = multiValued;
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public JsonAccessor<JsonElement> getRelativeAccessor() {
		return relativeAccessor;
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

	@SuppressWarnings("unchecked")
	public <T> ElasticsearchIndexSchemaFieldNode<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.getValueType().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.getValueType(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (ElasticsearchIndexSchemaFieldNode<? super T>) this;
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
