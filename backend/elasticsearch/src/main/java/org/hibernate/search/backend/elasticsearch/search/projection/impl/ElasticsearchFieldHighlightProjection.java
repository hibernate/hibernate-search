/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.search.highlighter.impl.ElasticsearchSearchHighlighter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ElasticsearchFieldHighlightProjection<T> implements ElasticsearchSearchProjection<T> {

	private static final JsonObjectAccessor REQUEST_HIGHLIGHT_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "highlight" ).asObject().property( "fields" ).asObject();

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String[] absoluteFieldPathComponents;

	private final String highlighterName;
	private final ElasticsearchSearchIndexValueFieldTypeContext<?> typeContext;
	private final ProjectionAccumulator.Provider<String, T> accumulatorProvider;

	private ElasticsearchFieldHighlightProjection(Builder builder,
			ProjectionAccumulator.Provider<String, T> accumulatorProvider) {
		this( builder.scope, builder.field, builder.highlighterName(), accumulatorProvider );
	}

	private ElasticsearchFieldHighlightProjection(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<?> field,
			String highlighterName,
			ProjectionAccumulator.Provider<String, T> accumulatorProvider) {
		this.indexNames = scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = field.absolutePath();
		this.absoluteFieldPathComponents = field.absolutePathComponents();
		this.highlighterName = highlighterName;
		this.typeContext = field.type();
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ "highlighterName=" + highlighterName
				+ "]";
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public Extractor<?, T> request(JsonObject requestBody, ProjectionRequestContext context) {
		if ( context.absoluteCurrentFieldPath() != null ) {
			throw log.cannotHighlightInNestedContext(
					context.absoluteCurrentFieldPath(),
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}

		ProjectionRequestContext innerContext = context.forField( absoluteFieldPath, absoluteFieldPathComponents );
		ElasticsearchSearchHighlighter highlighter = context.root().highlighter( highlighterName );

		SearchHighlighterType highlighterType = highlighter.type();
		if ( highlighterType == null ) {
			// this can happen if field highlighter has no configuration or is using a default highlighter,
			// if so let's try to get the type from a global config:
			ElasticsearchSearchHighlighter queryHighlighter = context.root().queryHighlighter();
			highlighterType = queryHighlighter != null ? queryHighlighter.type() : null;
			highlighterType = highlighterType == null ? SearchHighlighterType.UNIFIED : highlighterType;
		}
		if ( !typeContext.highlighterTypeSupported( highlighterType ) ) {
			throw log.highlighterTypeNotSupported( highlighterType, absoluteFieldPath );
		}
		if ( !context.root().isCompatibleHighlighter( highlighterName, accumulatorProvider ) ) {
			throw log.highlighterIncompatibleCardinality();
		}

		highlighter.applyToField(
				absoluteFieldPath,
				REQUEST_HIGHLIGHT_FIELDS_ACCESSOR.getOrCreate( requestBody, JsonObject::new )
		);

		return new FieldHighlightExtractor<>( innerContext.absoluteCurrentFieldPath(), accumulatorProvider.get() );
	}

	private class FieldHighlightExtractor<A> implements Extractor<A, T> {
		private final JsonArrayAccessor highlightAccessor;
		private final ProjectionAccumulator<String, String, A, T> accumulator;

		private FieldHighlightExtractor(String fieldPath, ProjectionAccumulator<String, String, A, T> accumulator) {
			this.highlightAccessor = JsonAccessor.root().property( "highlight" ).property( fieldPath ).asArray();
			this.accumulator = accumulator;
		}

		@Override
		public A extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit, JsonObject source,
				ProjectionExtractContext context) {
			A initial = accumulator.createInitial();
			Optional<JsonArray> highlights = highlightAccessor.get( hit );
			if ( highlights.isPresent() ) {
				for ( JsonElement element : highlights.get() ) {
					initial = accumulator.accumulate( initial, element.getAsString() );
				}
			}
			return initial;
		}

		@Override
		public T transform(LoadingResult<?> loadingResult, A extractedData, ProjectionTransformContext context) {
			return accumulator.finish( extractedData );
		}
	}

	public static class Factory<F>
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<HighlightProjectionBuilder, F> {
		@Override
		public HighlightProjectionBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			if ( field.nestedDocumentPath() != null ) {
				// see HSEARCH-4841 to remove this limitation.
				throw log.cannotHighlightFieldFromNestedObjectStructure(
						EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() )
				);
			}
			return new Builder( scope, field );
		}
	}

	public static class Builder extends HighlightProjectionBuilder {
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<?> field;

		public Builder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<?> field) {
			super( field.absolutePath() );
			this.scope = scope;
			this.field = field;
		}

		protected String highlighterName() {
			return highlighterName;
		}

		@Override
		public <V> SearchProjection<V> build(ProjectionAccumulator.Provider<String, V> accumulatorProvider) {
			return new ElasticsearchFieldHighlightProjection<>( this, accumulatorProvider );
		}
	}
}
