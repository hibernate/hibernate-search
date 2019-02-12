/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeTerminalContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


/**
 * @author Guillaume Smet
 */
public class LuceneIndexFieldTypeFactoryContextImpl
		implements LuceneIndexFieldTypeFactoryContext, LuceneIndexFieldTypeBuildContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	public LuceneIndexFieldTypeFactoryContextImpl(EventContext eventContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.eventContext = eventContext;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
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
		else if ( Character.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asCharacter();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asLocalDate();
		}
		else if ( Instant.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asInstant();
		}
		else if ( ZonedDateTime.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asZonedDateTime();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asGeoPoint();
		}
		else if ( URI.class.equals( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asUri();
		}
		else {
			// TODO implement other types
			throw log.cannotGuessFieldType( inputType, getEventContext() );
		}
	}

	@Override
	public StringIndexFieldTypeContext<?> asString() {
		return new LuceneStringIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return new LuceneIntegerIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return new LuceneLongIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return new LuceneBooleanIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Character> asCharacter() {
		return new LuceneCharacterIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return new LuceneLocalDateIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return new LuceneInstantIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, ZonedDateTime> asZonedDateTime() {
		// TODO implement this on backend commits within the same HSEARCH-3047 issue
		return null;
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return new LuceneGeoPointIndexFieldTypeContext( this );
	}

	@Override
	public StandardIndexFieldTypeContext<?, URI> asUri() {
		// TODO implement this on backend commits within the same HSEARCH-3047 issue
		return null;
	}

	@Override
	public <F> IndexFieldTypeTerminalContext<F> asLuceneField(Class<F> indexFieldType,
			LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor) {
		return new LuceneFieldIndexFieldTypeContext<>(
				indexFieldType, fieldContributor, fieldValueExtractor
		);
	}

	@Override
	public EventContext getEventContext() {
		return eventContext;
	}

	@Override
	public LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return analysisDefinitionRegistry;
	}
}
