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
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchJsonStringIndexFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexFieldTypeFactoryContextImpl
		implements ElasticsearchIndexFieldTypeFactoryContext, ElasticsearchIndexFieldTypeBuildContext,
				ElasticsearchIndexSchemaNodeContributor, IndexSchemaBuildContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexSchemaRootNodeBuilder rootNodeBuilder;
	private final String relativeFieldName;
	private final String absoluteFieldPath;

	private ElasticsearchIndexSchemaNodeContributor delegate;

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
		return new ElasticsearchStringIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return new ElasticsearchIntegerIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return new ElasticsearchLongIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return new ElasticsearchBooleanIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return new ElasticsearchLocalDateIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return new ElasticsearchInstantIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return new ElasticsearchGeoPointIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public ElasticsearchJsonStringIndexFieldTypeContext asJsonString(String mappingJsonString) {
		return new ElasticsearchJsonStringIndexFieldTypeContextImpl( this, mappingJsonString, initDelegate() );
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode, AbstractTypeMapping parentMapping) {
		// TODO error if delegate is null
		delegate.contribute( collector, parentNode, parentMapping );
	}

	@Override
	public EventContext getEventContext() {
		return rootNodeBuilder.getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	private <F> ElasticsearchIndexSchemaFieldDslBackReference<F> initDelegate() {
		if ( delegate != null ) {
			throw log.tryToSetFieldTypeMoreThanOnce( getEventContext() );
		}
		IndexSchemaFieldDslAdapter<F> adapter = new IndexSchemaFieldDslAdapter<>();
		this.delegate = adapter;
		return adapter;
	}

	private class IndexSchemaFieldDslAdapter<F>
			implements ElasticsearchIndexSchemaNodeContributor, ElasticsearchIndexSchemaFieldDslBackReference<F> {
		private final IndexSchemaFieldDefinitionHelper<F> helper;
		private ElasticsearchIndexFieldType<F> type;

		private IndexSchemaFieldDslAdapter() {
			this.helper = new IndexSchemaFieldDefinitionHelper<>(
					ElasticsearchIndexFieldTypeFactoryContextImpl.this
			);
		}

		@Override
		public IndexFieldAccessor<F> onCreateAccessor(ElasticsearchIndexFieldType<F> type) {
			this.type = type;
			return helper.createAccessor();
		}

		@Override
		public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
				ElasticsearchIndexSchemaObjectNode parentNode, AbstractTypeMapping parentMapping) {
			IndexFieldAccessor<F> accessor = null;
			// FIXME this is weird, but we need it to pass the tests. It will disappear in the next commit.
			if ( type != null ) {
				accessor = type.addField( collector, parentNode, parentMapping, relativeFieldName );
			}
			helper.initialize( accessor );
		}
	}
}
