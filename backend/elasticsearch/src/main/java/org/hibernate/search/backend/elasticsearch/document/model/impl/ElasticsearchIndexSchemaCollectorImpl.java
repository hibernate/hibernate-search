/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexSchemaElement;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RoutingType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.util.AssertionFailure;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSchemaCollectorImpl<T extends TypeMapping>
		implements IndexSchemaCollector, ElasticsearchIndexSchemaNodeContributor<T> {

	public static ElasticsearchIndexSchemaCollectorImpl<TypeMapping> root() {
		return new ElasticsearchIndexSchemaCollectorImpl<>( JsonAccessor.root(),
				ElasticsearchIndexSchemaElementImpl.TYPE_MAPPING_FACTORY );
	}

	private final JsonObjectAccessor accessor;
	private final Supplier<T> mappingFactory;
	private final ElasticsearchIndexSchemaPropertyNodeContributorMap propertyContributors;
	private RoutingType routing = null;

	private ElasticsearchIndexSchemaCollectorImpl(JsonObjectAccessor accessor, Supplier<T> mappingFactory) {
		this( accessor, mappingFactory, new ElasticsearchIndexSchemaPropertyNodeContributorMap( accessor ) );
	}

	private ElasticsearchIndexSchemaCollectorImpl(JsonObjectAccessor accessor, Supplier<T> mappingFactory,
			ElasticsearchIndexSchemaPropertyNodeContributorMap propertyContributors) {
		this.accessor = accessor;
		this.mappingFactory = mappingFactory;
		this.propertyContributors = propertyContributors;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "accessor=" ).append( accessor )
				.append( ",propertyContributors=" ).append( propertyContributors )
				.append( "]" )
				.toString();
	}

	@Override
	public ElasticsearchIndexSchemaElement withContext(IndexSchemaNestingContext context) {
		/*
		 * Note: this ignores any previous nesting context, but that's alright since
		 * nesting context composition is handled in the engine.
		 */
		return new ElasticsearchIndexSchemaElementImpl<>( accessor, mappingFactory, propertyContributors, context );
	}

	@Override
	public ElasticsearchIndexSchemaCollectorImpl<?> childObject(String relativeName) {
		JsonObjectAccessor propertyAccessor = accessor.property( relativeName ).asObject();
		ElasticsearchIndexSchemaPropertyNodeContributorMap nestedPropertyContributors =
				new ElasticsearchIndexSchemaPropertyNodeContributorMap( propertyAccessor );

		ElasticsearchIndexSchemaCollectorImpl<PropertyMapping> childCollector =
				new ElasticsearchIndexSchemaCollectorImpl<>( propertyAccessor,
						ElasticsearchIndexSchemaElementImpl.PROPERTY_MAPPING_FACTORY, nestedPropertyContributors );
		propertyContributors.put( relativeName, childCollector );
		return childCollector;
	}

	@Override
	public void explicitRouting() {
		if ( !JsonAccessor.root().equals( accessor ) ) {
			throw new AssertionFailure( "explicitRouting() was called on a non-root model collector; this should never happen." );
		}
		this.routing = RoutingType.REQUIRED;
	}

	@Override
	public T contribute(ElasticsearchFieldModelCollector collector) {
		T mapping = mappingFactory.get();
		if ( routing != null ) {
			mapping.setRouting( routing );
		}
		propertyContributors.contribute( collector, mapping );
		return mapping;
	}

}
