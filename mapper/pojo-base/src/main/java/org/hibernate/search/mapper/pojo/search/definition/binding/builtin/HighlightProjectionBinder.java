/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.AbstractProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.mapper.pojo.logging.impl.ProjectionLog;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * Binds a constructor parameter to a projection to highlights,
 * i.e. sequences of text that matched the query, extracted from the given field's value.
 *
 * @see TypedSearchProjectionFactory#highlight(String)
 * @see org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep#highlighter(String)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.HighlightProjection
 */
public final class HighlightProjectionBinder implements ProjectionBinder {

	/**
	 * Creates a {@link HighlightProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 * <p>
	 * This method requires the projection constructor class to be compiled with the {@code -parameters} flag
	 * to infer the field path from the name of the constructor parameter being bound.
	 * If this compiler flag is not used,
	 * use {@link #create(String)} instead and pass the field path explicitly.
	 *
	 * @return The binder.
	 */
	public static HighlightProjectionBinder create() {
		return create( null );
	}

	/**
	 * Creates a {@link FieldProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @param fieldPath The <a href="../../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose highlights will be extracted.
	 * When {@code null}, defaults to the name of the constructor parameter being bound,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise a null {@code fieldPath} will lead to a failure).
	 * @return The binder.
	 */
	public static HighlightProjectionBinder create(String fieldPath) {
		return new HighlightProjectionBinder( fieldPath );
	}

	private final String fieldPathOrNull;
	private String highlighterName;

	private HighlightProjectionBinder(String fieldPathOrNull) {
		this.fieldPathOrNull = fieldPathOrNull;
	}

	/**
	 * @param highlighterName The name of a highlighter
	 * {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#highlighter(String, Function) defined on the query},
	 * or {@code null} to use the default highlighter.
	 * @return {@code this}, for method chaining.
	 * @see org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep#highlighter(String)
	 */
	public HighlightProjectionBinder highlighter(String highlighterName) {
		this.highlighterName = highlighterName;
		return this;
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		String fieldPath = fieldPathOrFail( context );
		Optional<PojoModelValue<?>> containerElementOptional = context.containerElement();

		var collector = context.projectionCollectorProviderFactory()
				.projectionCollectorProvider(
						// if there's no container element, there's no container hence we are working with a "nullable" collector:
						containerElementOptional.isPresent()
								? context.constructorParameter().rawType()
								: null,
						String.class );

		context.definition( String.class, new Definition<>( fieldPath, highlighterName, collector ) );
	}

	private String fieldPathOrFail(ProjectionBindingContext context) {
		if ( fieldPathOrNull != null ) {
			return fieldPathOrNull;
		}
		Optional<String> paramName = context.constructorParameter().name();
		if ( paramName.isEmpty() ) {
			throw ProjectionLog.INSTANCE.missingParameterNameForHighlightProjectionInProjectionConstructor();
		}
		return paramName.get();
	}

	private static class Definition<T> extends AbstractProjectionDefinition<T> {
		private final String fieldPath;
		private final String highlighterName;
		private final ProjectionCollector.Provider<String, T> collector;

		private Definition(String fieldPath, String highlighterName, ProjectionCollector.Provider<String, T> collector) {
			this.fieldPath = fieldPath;
			this.highlighterName = highlighterName;
			this.collector = collector;
		}

		@Override
		protected String type() {
			return "highlight";
		}

		@Override
		public SearchProjection<T> create(ProjectionDefinitionContext context) {
			return context.projection().highlight( fieldPath )
					.highlighter( highlighterName )
					.collector( collector )
					.toProjection();
		}

		@Override
		public void appendTo(ToStringTreeAppender appender) {
			super.appendTo( appender );
			appender.attribute( "fieldPath", fieldPath );
			appender.attribute( "highlighter", highlighterName );
		}
	}
}
