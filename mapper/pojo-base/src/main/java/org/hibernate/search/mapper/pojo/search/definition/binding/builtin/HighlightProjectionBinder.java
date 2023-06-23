/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.definition.spi.AbstractProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingMultiContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

/**
 * Binds a constructor parameter to a projection to highlights,
 * i.e. sequences of text that matched the query, extracted from the given field's value.
 *
 * @see SearchProjectionFactory#highlight(String)
 * @see org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep#highlighter(String)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.HighlightProjection
 */
public final class HighlightProjectionBinder implements ProjectionBinder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
		Class<?> rawType = context.constructorParameter().rawType();
		if ( !rawType.isAssignableFrom( List.class ) ) {
			throw log.invalidParameterTypeForHighlightProjectionInProjectionConstructor( rawType );
		}
		Optional<? extends ProjectionBindingMultiContext> multiOptional = context.multi();
		if ( !multiOptional.isPresent() ) {
			throw log.invalidParameterTypeForHighlightProjectionInProjectionConstructor( rawType );
		}
		ProjectionBindingMultiContext multi = multiOptional.get();
		multi.definition( String.class, new Definition( fieldPath, highlighterName ) );
	}

	private String fieldPathOrFail(ProjectionBindingContext context) {
		if ( fieldPathOrNull != null ) {
			return fieldPathOrNull;
		}
		Optional<String> paramName = context.constructorParameter().name();
		if ( !paramName.isPresent() ) {
			throw log.missingParameterNameForHighlightProjectionInProjectionConstructor();
		}
		return paramName.get();
	}

	private static class Definition extends AbstractProjectionDefinition<List<String>> {
		private final String fieldPath;
		private final String highlighterName;

		private Definition(String fieldPath, String highlighterName) {
			this.fieldPath = fieldPath;
			this.highlighterName = highlighterName;
		}

		@Override
		protected String type() {
			return "highlight";
		}

		@Override
		public void appendTo(ToStringTreeAppender appender) {
			super.appendTo( appender );
			appender.attribute( "fieldPath", fieldPath );
			appender.attribute( "highlighter", highlighterName );
		}

		@Override
		public SearchProjection<List<String>> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.highlight( fieldPath ).highlighter( highlighterName ).toProjection();
		}
	}
}
