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
public class ElasticsearchIndexModelCollectorImpl
		implements ElasticsearchIndexModelCollector, IndexModelCollectorImplementor,
				ElasticsearchMappingContributor<TypeMapping> {

	private final JsonObjectAccessor accessor;
	private final Map<String, ElasticsearchMappingContributor<PropertyMapping>> propertyContributors;
	private final IndexModelNestingContext filter;

	public ElasticsearchIndexModelCollectorImpl() {
		this( JsonAccessor.root(), IndexModelNestingContext.includeAll() );
	}

	private ElasticsearchIndexModelCollectorImpl(JsonObjectAccessor accessor, IndexModelNestingContext filter) {
		this.accessor = accessor;
		this.propertyContributors = new HashMap<>();
		this.filter = filter;
	}

	private ElasticsearchIndexModelCollectorImpl(ElasticsearchIndexModelCollectorImpl original,
			IndexModelNestingContext filter) {
		// Share the same state as the original regarding the model itself
		this.accessor = original.accessor;
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
	public ElasticsearchIndexModelCollectorImpl withContext(IndexModelNestingContext context) {
		/*
		 * Note: this erases the previous filter, but that's alright since
		 * filter composition is handled in the engine.
		 */
		return new ElasticsearchIndexModelCollectorImpl( this, context );
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
	public ElasticsearchIndexModelCollectorImpl childObject(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();

		// Only take the contributor into account if the child is included
		return filter.applyIfIncluded( relativeName, (name, filter) -> {
					ElasticsearchIndexModelCollectorImpl childCollector =
							new ElasticsearchIndexModelCollectorImpl( propertyAccessor, filter );
					addPropertyContributor( name, p -> {
						p.setType( DataType.OBJECT );
						childCollector.contribute( p );
					});
					return childCollector;
				} )
				.orElseGet( () -> new ElasticsearchIndexModelCollectorImpl( propertyAccessor, IndexModelNestingContext.excludeAll() ) );
	}

	private void addPropertyContributor(String name, ElasticsearchMappingContributor<PropertyMapping> contributor) {
		Object previous = propertyContributors.putIfAbsent( name, contributor );
		if ( previous != null ) {
			// TODO more explicit error message
			throw new SearchException( "The index model node '" + name + "' was added twice at path '" + accessor + "'."
					+ " Multiple bridges may be trying to access the same index field, "
					+ " or two indexedEmbeddeds may have prefixes that end up mixing fields together,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.");
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
	public <T extends IndexModelCollector> Optional<T> unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchIndexModelCollector.class ) ) {
			return Optional.of( clazz.cast( this ) );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public void contribute(TypeMapping mapping) {
		for ( Map.Entry<String, ElasticsearchMappingContributor<PropertyMapping>> entry
				: propertyContributors.entrySet() ) {
			PropertyMapping propertyMapping = new PropertyMapping();
			mapping.addProperty( entry.getKey(), propertyMapping );
			entry.getValue().contribute( propertyMapping );
		}
	}

}
