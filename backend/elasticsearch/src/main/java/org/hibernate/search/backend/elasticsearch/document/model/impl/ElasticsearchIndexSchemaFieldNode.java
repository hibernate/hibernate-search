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
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonElement;


public class ElasticsearchIndexSchemaFieldNode<F> implements IndexValueFieldDescriptor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexSchemaObjectNode parent;
	private final String absolutePath;
	private final String relativeName;
	private final JsonAccessor<JsonElement> relativeAccessor;
	private final IndexFieldInclusion inclusion;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final ElasticsearchIndexFieldType<F> type;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, boolean multiValued, ElasticsearchIndexFieldType<F> type) {
		this.parent = parent;
		this.absolutePath = parent.absolutePath( relativeFieldName );
		this.relativeName = relativeFieldName;
		this.relativeAccessor = JsonAccessor.root().property( relativeFieldName );
		this.inclusion = inclusion;
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.type = type;
		this.multiValued = multiValued;
	}

	@Override
	public boolean isObjectField() {
		return false;
	}

	@Override
	public boolean isValueField() {
		return true;
	}

	@Override
	public IndexObjectFieldDescriptor toObjectField() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public IndexValueFieldDescriptor toValueField() {
		return this;
	}

	@Override
	public ElasticsearchIndexSchemaObjectNode parent() {
		return parent;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public String relativeName() {
		return relativeName;
	}

	public JsonAccessor<JsonElement> getRelativeAccessor() {
		return relativeAccessor;
	}

	public IndexFieldInclusion getInclusion() {
		return inclusion;
	}

	public String getNestedPath() {
		return ( nestedPathHierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	public boolean isMultiValued() {
		return multiValued;
	}

	@Override
	public ElasticsearchIndexFieldType<F> type() {
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
