/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.spi.IndexObjectReference;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSchemaElementImpl<T extends TypeMapping>
		implements ElasticsearchIndexSchemaElement, ElasticsearchIndexSchemaNodeContributor<T> {

	static final Supplier<TypeMapping> TYPE_MAPPING_FACTORY = TypeMapping::new;
	static final Supplier<PropertyMapping> PROPERTY_MAPPING_FACTORY = () -> {
		PropertyMapping mapping = new PropertyMapping();
		mapping.setType( DataType.OBJECT );
		return mapping;
	};

	private final JsonObjectAccessor accessor;
	private final Supplier<T> mappingFactory;
	private final ElasticsearchIndexSchemaPropertyNodeContributorMap propertyContributors;
	private final IndexSchemaNestingContext nestingContext;

	ElasticsearchIndexSchemaElementImpl(JsonObjectAccessor accessor, Supplier<T> mappingFactory,
			ElasticsearchIndexSchemaPropertyNodeContributorMap propertyContributors,
			IndexSchemaNestingContext nestingContext) {
		this.accessor = accessor;
		this.mappingFactory = mappingFactory;
		this.propertyContributors = propertyContributors;
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
			propertyContributors.put( name, fieldContext );
			return null;
		} );

		return fieldContext;
	}

	@Override
	public ElasticsearchIndexSchemaElementImpl<?> childObject(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();
		ElasticsearchIndexSchemaPropertyNodeContributorMap nestedPropertyContributors =
				new ElasticsearchIndexSchemaPropertyNodeContributorMap( accessor );

		// Only take the contributor into account if the child is included
		return nestingContext.applyIfIncluded( relativeName, (name, filter) -> {
					ElasticsearchIndexSchemaElementImpl<PropertyMapping> childCollector =
							new ElasticsearchIndexSchemaElementImpl<>( propertyAccessor, PROPERTY_MAPPING_FACTORY,
									nestedPropertyContributors, filter );
					propertyContributors.put( name, childCollector );
					return childCollector;
				} )
				.orElseGet( () -> new ElasticsearchIndexSchemaElementImpl<>( propertyAccessor, PROPERTY_MAPPING_FACTORY,
						nestedPropertyContributors, IndexSchemaNestingContext.excludeAll() ) );
	}

	@Override
	public IndexObjectReference asReference() {
		// TODO Object reference
		throw new UnsupportedOperationException( "object reference not implemented yet" );
	}

	@Override
	public T contribute(ElasticsearchFieldModelCollector collector) {
		T mapping = mappingFactory.get();
		propertyContributors.contribute( collector, mapping );
		return mapping;
	}

}
