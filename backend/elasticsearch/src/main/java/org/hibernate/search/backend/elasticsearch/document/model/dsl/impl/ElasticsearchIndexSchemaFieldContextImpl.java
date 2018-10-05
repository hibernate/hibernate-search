/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.ElasticsearchIndexSchemaFieldContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchGeoPointIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchIntegerIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.JsonStringIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchLocalDateIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchStringIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
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
	public <F> StandardIndexSchemaFieldTypedContext<?, F> as(Class<F> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asInteger();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asLocalDate();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw new SearchException( "Cannot guess field type for input type " + inputType );
		}
	}

	@Override
	public StringIndexSchemaFieldTypedContext<?> asString() {
		return setDelegate( new ElasticsearchStringIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Integer> asInteger() {
		return setDelegate( new ElasticsearchIntegerIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, LocalDate> asLocalDate() {
		return setDelegate( new ElasticsearchLocalDateIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, GeoPoint> asGeoPoint() {
		return setDelegate( new ElasticsearchGeoPointIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<?, String> asJsonString(String mappingJsonString) {
		return setDelegate( new JsonStringIndexSchemaFieldContextImpl( this, relativeFieldName, mappingJsonString ) );
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		// TODO error if delegate is null
		return delegate.contribute( collector, parentNode );
	}

	@Override
	public EventContext getEventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	private <T extends ElasticsearchIndexSchemaNodeContributor<PropertyMapping>> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}

}
