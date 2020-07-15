/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;


public class ElasticsearchIndexSchemaFieldNode<F> extends AbstractElasticsearchIndexSchemaFieldNode
		implements IndexValueFieldDescriptor, ElasticsearchSearchFieldContext<F> {

	private final List<String> nestedPathHierarchy;

	private final ElasticsearchIndexFieldType<F> type;

	public ElasticsearchIndexSchemaFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, boolean multiValued, ElasticsearchIndexFieldType<F> type) {
		super( parent, relativeFieldName, inclusion, multiValued );
		this.nestedPathHierarchy = parent.nestedPathHierarchy();
		this.type = type;
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
	public ElasticsearchIndexSchemaObjectFieldNode toObjectField() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public ElasticsearchIndexSchemaFieldNode<F> toValueField() {
		return this;
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	public ElasticsearchIndexFieldType<F> type() {
		return type;
	}

	@Override
	public EventContext eventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchContext searchContext) {
		ElasticsearchSearchFieldQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
	}

	@SuppressWarnings("unchecked")
	public <T> ElasticsearchIndexSchemaFieldNode<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.valueClass().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.valueClass(), expectedSubType,
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
