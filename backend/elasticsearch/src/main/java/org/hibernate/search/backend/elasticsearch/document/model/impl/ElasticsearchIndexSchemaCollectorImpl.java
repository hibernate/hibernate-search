/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.spi.IndexObjectReference;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RoutingType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSchemaCollectorImpl<T extends TypeMapping>
		implements ElasticsearchIndexSchemaElement, IndexSchemaCollector,
		ElasticsearchIndexSchemaNodeContributor<T> {

	private static final Supplier<TypeMapping> TYPE_MAPPING_FACTORY = TypeMapping::new;
	private static final Supplier<PropertyMapping> PROPERTY_MAPPING_FACTORY = () -> {
		PropertyMapping mapping = new PropertyMapping();
		mapping.setType( DataType.OBJECT );
		return mapping;
	};

	public static ElasticsearchIndexSchemaCollectorImpl<TypeMapping> root() {
		return new ElasticsearchIndexSchemaCollectorImpl<>( JsonAccessor.root(), TYPE_MAPPING_FACTORY,
				IndexSchemaNestingContext.includeAll() );
	}

	private final JsonObjectAccessor accessor;
	private final Supplier<T> mappingFactory;
	private final Map<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> propertyContributors;
	private RoutingType routing = null;

	private final IndexSchemaNestingContext filter;

	private ElasticsearchIndexSchemaCollectorImpl(JsonObjectAccessor accessor, Supplier<T> mappingFactory,
			IndexSchemaNestingContext filter) {
		this.accessor = accessor;
		this.mappingFactory = mappingFactory;
		this.propertyContributors = new HashMap<>();
		this.filter = filter;
	}

	private ElasticsearchIndexSchemaCollectorImpl(ElasticsearchIndexSchemaCollectorImpl<T> original,
			IndexSchemaNestingContext filter) {
		// Share the same state as the original regarding the model itself
		this.accessor = original.accessor;
		this.mappingFactory = original.mappingFactory;
		this.propertyContributors = original.propertyContributors;
		// ... but use a different filter
		this.filter = filter;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "accessor=" ).append( accessor )
				.append( ",propertyContributors=" ).append( propertyContributors )
				.append( ",filter=" ).append( filter )
				.append( "]" )
				.toString();
	}

	@Override
	public ElasticsearchIndexSchemaCollectorImpl<?> withContext(IndexSchemaNestingContext context) {
		/*
		 * Note: this erases the previous filter, but that's alright since
		 * filter composition is handled in the engine.
		 */
		return new ElasticsearchIndexSchemaCollectorImpl<>( this, context );
	}

	@Override
	public FieldModelContext field(String relativeName) {
		UnknownTypeJsonAccessor propertyAccessor = accessor.property( relativeName );
		ElasticsearchFieldModelContextImpl fieldContext =
				new ElasticsearchFieldModelContextImpl( propertyAccessor );

		// Only take the contributor into account if the field is included
		filter.applyIfIncluded( relativeName, name -> {
			addPropertyContributor( name, fieldContext );
			return null;
		} );

		return fieldContext;
	}

	@Override
	public ElasticsearchIndexSchemaCollectorImpl<?> childObject(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();

		// Only take the contributor into account if the child is included
		return filter.applyIfIncluded( relativeName, (name, filter) -> {
					ElasticsearchIndexSchemaCollectorImpl<PropertyMapping> childCollector =
							new ElasticsearchIndexSchemaCollectorImpl<>( propertyAccessor, PROPERTY_MAPPING_FACTORY, filter );
					addPropertyContributor( name, childCollector );
					return childCollector;
				} )
				.orElseGet( () -> new ElasticsearchIndexSchemaCollectorImpl<>( propertyAccessor, PROPERTY_MAPPING_FACTORY,
						IndexSchemaNestingContext.excludeAll() ) );
	}

	private void addPropertyContributor(String name, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> contributor) {
		Object previous = propertyContributors.putIfAbsent( name, contributor );
		if ( previous != null ) {
			// TODO more explicit error message
			throw new SearchException( "The index model node '" + name + "' was added twice at path '" + accessor + "'."
					+ " Multiple bridges may be trying to access the same index field, "
					+ " or two indexedEmbeddeds may have prefixes that end up mixing fields together,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it." );
		}
	}

	@Override
	public void explicitRouting() {
		if ( !JsonAccessor.root().equals( accessor ) ) {
			throw new AssertionFailure( "explicitRouting() was called on a non-root model collector; this should never happen." );
		}
		this.routing = RoutingType.REQUIRED;
	}

	@Override
	public IndexObjectReference asReference() {
		// TODO Object reference
		throw new UnsupportedOperationException( "object reference not implemented yet" );
	}

	@Override
	public T contribute(ElasticsearchFieldModelCollector collector) {
		T mapping = mappingFactory.get();
		if ( routing != null ) {
			mapping.setRouting( routing );
		}
		for ( Map.Entry<String, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> entry : propertyContributors.entrySet() ) {
			String propertyName = entry.getKey();
			ElasticsearchIndexSchemaNodeContributor<PropertyMapping> propertyContributor = entry.getValue();
			PropertyMapping propertyMapping = propertyContributor.contribute( collector );
			mapping.addProperty( propertyName, propertyMapping );
		}
		return mapping;
	}

}
