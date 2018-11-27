/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneBooleanIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneFieldIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneGeoPointIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIntegerIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneLocalDateIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneLongIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneStringIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneInstantIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Guillaume Smet
 */
class LuceneIndexSchemaFieldContextImpl
		implements LuceneIndexSchemaFieldContext, LuceneIndexSchemaNodeContributor, LuceneIndexSchemaContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;

	private LuceneIndexSchemaNodeContributor delegate;

	LuceneIndexSchemaFieldContextImpl(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = LuceneFields.compose( parent.getAbsolutePath(), relativeFieldName );
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
		else if ( Instant.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<?, F>) asInstant();
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
		return setDelegate( new LuceneStringIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Integer> asInteger() {
		return setDelegate( new LuceneIntegerIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Long> asLong() {
		return setDelegate( new LuceneLongIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Boolean> asBoolean() {
		return setDelegate( new LuceneBooleanIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, LocalDate> asLocalDate() {
		return setDelegate( new LuceneLocalDateIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, Instant> asInstant() {
		return setDelegate( new LuceneInstantIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<?, GeoPoint> asGeoPoint() {
		return setDelegate( new LuceneGeoPointIndexSchemaFieldContext( this, relativeFieldName ) );
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
		return setDelegate( new LuceneFieldIndexSchemaFieldContext<>(
				this, relativeFieldName, indexFieldType, fieldContributor, fieldValueExtractor
		) );
	}

	@Override
	public EventContext getEventContext() {
		return getRoot().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRoot() {
		return parent.getRoot();
	}

	private <T extends LuceneIndexSchemaNodeContributor> T setDelegate(T context) {
		if ( delegate != null ) {
			throw log.tryToSetFieldTypeMoreThanOnce( getEventContext() );
		}
		delegate = context;
		return context;
	}
}
