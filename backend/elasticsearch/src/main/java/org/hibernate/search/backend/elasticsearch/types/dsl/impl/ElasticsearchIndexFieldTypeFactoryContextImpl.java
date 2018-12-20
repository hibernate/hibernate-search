/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchJsonStringIndexFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexFieldTypeFactoryContextImpl
		implements ElasticsearchIndexFieldTypeFactoryContext, ElasticsearchIndexSchemaNodeContributor<PropertyMapping>,
		IndexSchemaBuildContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexSchemaRootNodeBuilder rootNodeBuilder;
	private final String relativeFieldName;
	private final String absoluteFieldPath;

	private ElasticsearchIndexSchemaNodeContributor<PropertyMapping> delegate;

	public ElasticsearchIndexFieldTypeFactoryContextImpl(ElasticsearchIndexSchemaRootNodeBuilder rootNodeBuilder,
			String parentAbsoluteFieldPath, String relativeFieldName) {
		this.rootNodeBuilder = rootNodeBuilder;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = ElasticsearchFields.compose( parentAbsoluteFieldPath, relativeFieldName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F> StandardIndexFieldTypeContext<?, F> as(Class<F> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asInteger();
		}
		else if ( Long.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLong();
		}
		else if ( Boolean.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asBoolean();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLocalDate();
		}
		else if ( Instant.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asInstant();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw log.cannotGuessFieldType( inputType, getEventContext() );
		}
	}

	@Override
	public StringIndexFieldTypeContext<?> asString() {
		return setDelegate( new ElasticsearchStringIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return setDelegate( new ElasticsearchIntegerIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return setDelegate( new ElasticsearchLongIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return setDelegate( new ElasticsearchBooleanIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return setDelegate( new ElasticsearchLocalDateIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return setDelegate( new ElasticsearchInstantIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return setDelegate( new ElasticsearchGeoPointIndexFieldTypeContext( this, relativeFieldName ) );
	}

	@Override
	public ElasticsearchJsonStringIndexFieldTypeContext asJsonString(String mappingJsonString) {
		return setDelegate( new org.hibernate.search.backend.elasticsearch.types.dsl.impl.ElasticsearchJsonStringIndexFieldTypeContext( this, relativeFieldName, mappingJsonString ) );
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		// TODO error if delegate is null
		return delegate.contribute( collector, parentNode );
	}

	@Override
	public EventContext getEventContext() {
		return rootNodeBuilder.getIndexEventContext()
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
