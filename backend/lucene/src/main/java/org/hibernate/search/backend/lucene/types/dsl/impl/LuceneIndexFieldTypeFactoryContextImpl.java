/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaBuildContext;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * FIXME: This class is a bit of a hack: it is the bridge between the index field type DSL and the index field DSL.
 * If you look at it and find it ugly and confusing, it's just because it is.
 * We will clean it up when we properly split the index field type and index field DSLs in the next commits.
 *
 * @author Guillaume Smet
 */
public class LuceneIndexFieldTypeFactoryContextImpl
		implements LuceneIndexFieldTypeFactoryContext, LuceneIndexFieldTypeBuildContext,
		LuceneIndexSchemaNodeContributor, LuceneIndexSchemaBuildContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneIndexSchemaRootNodeBuilder root;
	private final String relativeFieldName;
	private final String absoluteFieldPath;

	private LuceneIndexSchemaNodeContributor delegate;

	public LuceneIndexFieldTypeFactoryContextImpl(LuceneIndexSchemaRootNodeBuilder root,
			String parentAbsoluteFieldPath, String relativeFieldName) {
		this.root = root;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = LuceneFields.compose( parentAbsoluteFieldPath, relativeFieldName );
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
		return new LuceneStringIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return new LuceneIntegerIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return new LuceneLongIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return new LuceneBooleanIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return new LuceneLocalDateIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return new LuceneInstantIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return new LuceneGeoPointIndexFieldTypeContext( this, initDelegate() );
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		Contracts.assertNotNull( delegate, "delegate" );

		delegate.contribute( collector, parentNode );
	}

	@Override
	public <F> IndexSchemaFieldTerminalContext<F> asLuceneField(Class<F> indexFieldType,
			LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor) {
		return new LuceneFieldIndexFieldTypeContext<>(
				indexFieldType, fieldContributor, fieldValueExtractor,
				initDelegate()
		);
	}

	@Override
	public EventContext getEventContext() {
		return getRoot().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return getRoot().getAnalysisDefinitionRegistry();
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRoot() {
		return root;
	}

	private <F> LuceneIndexSchemaFieldDslBackReference<F> initDelegate() {
		if ( delegate != null ) {
			throw log.tryToSetFieldTypeMoreThanOnce( getEventContext() );
		}
		IndexSchemaFieldDslAdapter<F> adapter = new IndexSchemaFieldDslAdapter<>();
		this.delegate = adapter;
		return adapter;
	}

	private class IndexSchemaFieldDslAdapter<F>
			implements LuceneIndexSchemaNodeContributor, LuceneIndexSchemaFieldDslBackReference<F> {
		private final IndexSchemaFieldDefinitionHelper<F> helper;
		private LuceneIndexFieldType<F> type;

		private IndexSchemaFieldDslAdapter() {
			this.helper = new IndexSchemaFieldDefinitionHelper<>(
					LuceneIndexFieldTypeFactoryContextImpl.this
			);
		}

		@Override
		public IndexFieldAccessor<F> onCreateAccessor(LuceneIndexFieldType<F> type) {
			this.type = type;
			return helper.createAccessor();
		}

		@Override
		public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
			IndexFieldAccessor<F> accessor = null;
			// FIXME this is weird, but we need it to pass the tests. It will disappear in the next commit.
			if ( type != null ) {
				accessor = type.addField( collector, parentNode, relativeFieldName );
			}
			helper.initialize( accessor );
		}
	}
}
