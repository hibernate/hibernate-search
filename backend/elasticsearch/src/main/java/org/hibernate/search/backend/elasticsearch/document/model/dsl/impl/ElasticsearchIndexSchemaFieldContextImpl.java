/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.GeoPointIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.IntegerIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.JsonStringIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.LocalDateIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.StringIndexSchemaFieldContext;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSchemaFieldContextImpl
		implements ElasticsearchIndexSchemaFieldContext, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> {

	private final String relativeName;

	private ElasticsearchIndexSchemaNodeContributor<PropertyMapping> delegate;

	public ElasticsearchIndexSchemaFieldContextImpl(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> IndexSchemaFieldTypedContext<T> as(Class<T> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asInteger();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asLocalDate();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw new SearchException( "Cannot guess field type for input type " + inputType );
		}
	}

	@Override
	public IndexSchemaFieldTypedContext<String> asString() {
		return setDelegate( new StringIndexSchemaFieldContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<Integer> asInteger() {
		return setDelegate( new IntegerIndexSchemaFieldContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return setDelegate( new LocalDateIndexSchemaFieldContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return setDelegate( new GeoPointIndexSchemaFieldContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTerminalContext<String> asJsonString(String mappingJsonString) {
		return setDelegate( new JsonStringIndexSchemaFieldContext( relativeName, mappingJsonString ) );
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		// TODO error if delegate is null
		return delegate.contribute( collector, parentNode );
	}

	private <T extends ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}

}
