/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollectorImplementor;
import org.hibernate.search.engine.backend.document.model.spi.IndexModelNestingContext;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.engine.backend.document.spi.IndexObjectReference;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexModelCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.engine.backend.projection.spi.Projection;
import org.hibernate.search.engine.mapper.model.spi.IndexableReference;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexModelCollectorImpl<T extends TypeMapping>
		implements ElasticsearchIndexModelCollector, IndexModelCollectorImplementor,
				ElasticsearchIndexModelNodeContributor<T> {

	private static final Supplier<TypeMapping> TYPE_MAPPING_FACTORY = TypeMapping::new;
	private static final Supplier<PropertyMapping> PROPERTY_MAPPING_FACTORY = () -> {
		PropertyMapping mapping = new PropertyMapping();
		mapping.setType( DataType.OBJECT );
		return mapping;
	};

	public static ElasticsearchIndexModelCollectorImpl<TypeMapping> root() {
		return new ElasticsearchIndexModelCollectorImpl<>( JsonAccessor.root(), TYPE_MAPPING_FACTORY,
				IndexModelNestingContext.includeAll() );
	}

	private final JsonObjectAccessor accessor;
	private final Supplier<T> mappingFactory;
	private final Map<String, ElasticsearchIndexModelNodeContributor<PropertyMapping>> propertyContributors;
	private final IndexModelNestingContext filter;

	private ElasticsearchIndexModelCollectorImpl(JsonObjectAccessor accessor, Supplier<T> mappingFactory,
			IndexModelNestingContext filter) {
		this.accessor = accessor;
		this.mappingFactory = mappingFactory;
		this.propertyContributors = new HashMap<>();
		this.filter = filter;
	}

	private ElasticsearchIndexModelCollectorImpl(ElasticsearchIndexModelCollectorImpl<T> original,
			IndexModelNestingContext filter) {
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
	public ElasticsearchIndexModelCollectorImpl<?> withContext(IndexModelNestingContext context) {
		/*
		 * Note: this erases the previous filter, but that's alright since
		 * filter composition is handled in the engine.
		 */
		return new ElasticsearchIndexModelCollectorImpl<>( this, context );
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
	public ElasticsearchIndexModelCollectorImpl<?> childObject(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();

		// Only take the contributor into account if the child is included
		return filter.applyIfIncluded( relativeName, (name, filter) -> {
					ElasticsearchIndexModelCollectorImpl<PropertyMapping> childCollector =
							new ElasticsearchIndexModelCollectorImpl<>( propertyAccessor, PROPERTY_MAPPING_FACTORY, filter );
					addPropertyContributor( name, childCollector );
					return childCollector;
				} )
				.orElseGet( () -> new ElasticsearchIndexModelCollectorImpl<>( propertyAccessor, PROPERTY_MAPPING_FACTORY,
						IndexModelNestingContext.excludeAll() ) );
	}

	private void addPropertyContributor(String name, ElasticsearchIndexModelNodeContributor<PropertyMapping> contributor) {
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
	public void projection(Set<IndexFieldReference<?>> requiredFields, Projection projection) {
		// TODO Projections
		throw new UnsupportedOperationException( "Projections not implemented yet" );
	}

	@Override
	public void projection(String relativeName, Set<IndexableReference<?>> requiredFields, Projection projection) {
		// TODO Projections
		throw new UnsupportedOperationException( "Projections not implemented yet" );
	}

	@Override
	public IndexObjectReference asReference() {
		// TODO Object reference
		throw new UnsupportedOperationException( "object reference not implemented yet" );
	}

	@Override
	public <T2 extends IndexModelCollector> Optional<T2> unwrap(Class<T2> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchIndexModelCollector.class ) ) {
			return Optional.of( clazz.cast( this ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public T contribute(ElasticsearchFieldModelCollector collector) {
		T mapping = mappingFactory.get();
		for ( Map.Entry<String, ElasticsearchIndexModelNodeContributor<PropertyMapping>> entry : propertyContributors.entrySet() ) {
			String propertyName = entry.getKey();
			ElasticsearchIndexModelNodeContributor<PropertyMapping> propertyContributor = entry.getValue();
			PropertyMapping propertyMapping = propertyContributor.contribute( collector );
			mapping.addProperty( propertyName, propertyMapping );
		}
		return mapping;
	}

}
