/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexObjectAccessor;
import org.hibernate.search.engine.backend.document.model.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSchemaElementImpl
		implements ElasticsearchIndexSchemaElement {

	protected final JsonObjectAccessor accessor;
	private final AbstractIndexSchemaCompositeNodeBuilder<?> nodeBuilder;
	private final IndexSchemaNestingContext nestingContext;

	ElasticsearchIndexSchemaElementImpl(JsonObjectAccessor accessor,
			AbstractIndexSchemaCompositeNodeBuilder<?> nodeBuilder, IndexSchemaNestingContext nestingContext) {
		this.accessor = accessor;
		this.nodeBuilder = nodeBuilder;
		this.nestingContext = nestingContext;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "accessor=" ).append( accessor )
				.append( ",nestingContext=" ).append( nestingContext )
				.append( "]" )
				.toString();
	}

	@Override
	public FieldModelContext field(String relativeName) {
		UnknownTypeJsonAccessor propertyAccessor = accessor.property( relativeName );
		ElasticsearchFieldModelContextImpl fieldContext =
				new ElasticsearchFieldModelContextImpl( propertyAccessor );

		// Only take the contributor into account if the field is included
		nestingContext.applyIfIncluded( relativeName, name -> {
			nodeBuilder.putProperty( name, fieldContext );
			return null;
		} );

		return fieldContext;
	}

	@Override
	public ElasticsearchIndexSchemaElement objectField(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();
		IndexSchemaObjectPropertyNodeBuilder nestedNodeBuilder =
				new IndexSchemaObjectPropertyNodeBuilder( propertyAccessor );

		// Only take the contributor into account if the child is included
		return nestingContext.applyIfIncluded( relativeName, (name, filter) -> {
					nodeBuilder.putProperty( name, nestedNodeBuilder );
					return new ElasticsearchIndexSchemaElementImpl( propertyAccessor, nestedNodeBuilder, filter );
				} )
				.orElseGet( () -> new ElasticsearchIndexSchemaElementImpl( propertyAccessor,
						nestedNodeBuilder, IndexSchemaNestingContext.excludeAll() ) );
	}

	@Override
	public IndexObjectAccessor createAccessor() {
		// TODO Object accessors
		throw new UnsupportedOperationException( "object accessors not implemented yet" );
	}
}
