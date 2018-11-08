/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Date;

import org.hibernate.search.backend.elasticsearch.document.model.dsl.JsonStringIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchBooleanIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchUtilDateIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchLongIndexSchemaFieldContextImpl;
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
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSchemaFieldContextImpl
		implements ElasticsearchIndexSchemaFieldContext, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>,
				IndexSchemaContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
		else if ( Long.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asLong();
		}
		else if ( Boolean.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asBoolean();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asLocalDate();
		}
		else if ( Date.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asUtilDate();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw log.cannotGuessFieldType( inputType, getEventContext() );
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
	public StandardIndexSchemaFieldTypedContext<?, Long> asLong() {
		return setDelegate( new ElasticsearchLongIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Boolean> asBoolean() {
		return setDelegate( new ElasticsearchBooleanIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, LocalDate> asLocalDate() {
		return setDelegate( new ElasticsearchLocalDateIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Date> asUtilDate() {
		return setDelegate( new ElasticsearchUtilDateIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, GeoPoint> asGeoPoint() {
		return setDelegate( new ElasticsearchGeoPointIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public JsonStringIndexSchemaFieldTypedContext asJsonString(String mappingJsonString) {
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
			throw log.tryToSetFieldTypeMoreThanOnce( getEventContext() );
		}
		delegate = context;
		return context;
	}

}
