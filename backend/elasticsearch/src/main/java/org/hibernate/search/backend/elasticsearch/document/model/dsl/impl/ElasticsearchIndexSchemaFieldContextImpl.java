/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
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
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.engine.logging.spi.FailureContextElement;
import org.hibernate.search.engine.logging.spi.FailureContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSchemaFieldContextImpl
		implements ElasticsearchIndexSchemaFieldContext, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>,
				IndexSchemaContext {

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;

	private ElasticsearchIndexSchemaNodeContributor<PropertyMapping> delegate;

	ElasticsearchIndexSchemaFieldContextImpl(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent, String relativeFieldName) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = ElasticsearchFields.compose( parent.getAbsolutePath(), relativeFieldName );
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
		return setDelegate( new StringIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<Integer> asInteger() {
		return setDelegate( new IntegerIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return setDelegate( new LocalDateIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return setDelegate( new GeoPointIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTerminalContext<String> asJsonString(String mappingJsonString) {
		return setDelegate( new JsonStringIndexSchemaFieldContext( this, relativeFieldName, mappingJsonString ) );
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		// TODO error if delegate is null
		return delegate.contribute( collector, parentNode );
	}

	@Override
	public List<FailureContextElement> getFailureContext() {
		return Arrays.asList(
				parent.getRootNodeBuilder().getIndexFailureContextElement(),
				FailureContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	private <T extends ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}

}
