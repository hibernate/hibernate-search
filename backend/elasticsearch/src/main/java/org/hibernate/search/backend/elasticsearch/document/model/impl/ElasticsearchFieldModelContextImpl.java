/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.TerminalFieldModelContext;
import org.hibernate.search.engine.backend.document.model.TypedFieldModelContext;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchFieldModelContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchFieldModelContextImpl
		implements ElasticsearchFieldModelContext, ElasticsearchIndexSchemaNodeContributor<PropertyMapping> {

	private final String relativeName;

	private ElasticsearchIndexSchemaNodeContributor<PropertyMapping> delegate;

	public ElasticsearchFieldModelContextImpl(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> TypedFieldModelContext<T> as(Class<T> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (TypedFieldModelContext<T>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (TypedFieldModelContext<T>) asInteger();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (TypedFieldModelContext<T>) asLocalDate();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (TypedFieldModelContext<T>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw new SearchException( "Cannot guess field type for input type " + inputType );
		}
	}

	@Override
	public TypedFieldModelContext<String> asString() {
		return setDelegate( new StringFieldModelContext( relativeName ) );
	}

	@Override
	public TypedFieldModelContext<Integer> asInteger() {
		return setDelegate( new IntegerFieldModelContext( relativeName ) );
	}

	@Override
	public TypedFieldModelContext<LocalDate> asLocalDate() {
		return setDelegate( new LocalDateFieldModelContext( relativeName ) );
	}

	@Override
	public TypedFieldModelContext<GeoPoint> asGeoPoint() {
		return setDelegate( new GeoPointFieldModelContext( relativeName ) );
	}

	@Override
	public TerminalFieldModelContext<String> asJsonString(String mappingJsonString) {
		return setDelegate( new JsonStringFieldModelContext( relativeName, mappingJsonString ) );
	}

	@Override
	public PropertyMapping contribute(ElasticsearchFieldModelCollector collector,
			ElasticsearchObjectNodeModel parentModel) {
		// TODO error if delegate is null
		return delegate.contribute( collector, parentModel );
	}

	private <T extends ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}

}
