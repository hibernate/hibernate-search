/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.definition.spi.ConstantProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.FieldProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingMultiContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Binds a constructor parameter to a projection to the value of a field in the indexed document.
 *
 * @see SearchProjectionFactory#field(String, Class)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection
 */
public final class FieldProjectionBinder implements ProjectionBinder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * Creates a {@link FieldProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 * <p>
	 * This method requires the projection constructor class to be compiled with the {@code -parameters} flag
	 * to infer the field path from the name of the constructor parameter being bound.
	 * If this compiler flag is not used,
	 * use {@link #create(String)} instead and pass the field path explicitly.
	 *
	 * @return The binder.
	 */
	public static FieldProjectionBinder create() {
		return create( null );
	}

	/**
	 * Creates a {@link FieldProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @param fieldPath The <a href="../../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose value will be extracted.
	 * When {@code null}, defaults to the name of the constructor parameter being bound,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise a null {@code fieldPath} will lead to a failure).
	 * @return The binder.
	 */
	public static FieldProjectionBinder create(String fieldPath) {
		return new FieldProjectionBinder( fieldPath );
	}

	private final String fieldPathOrNull;
	private ValueConvert valueConvert = ValueConvert.YES;

	private FieldProjectionBinder(String fieldPathOrNull) {
		this.fieldPathOrNull = fieldPathOrNull;
	}

	/**
	 * @param valueConvert Controls how the data fetched from the backend should be converted.
	 * See {@link ValueConvert}.
	 * @return {@code this}, for method chaining.
	 * @see SearchProjectionFactory#field(String, Class, ValueConvert)
	 */
	public FieldProjectionBinder valueConvert(ValueConvert valueConvert) {
		this.valueConvert = valueConvert;
		return this;
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		Optional<? extends ProjectionBindingMultiContext> multiOptional = context.multi();
		String fieldPath = fieldPathOrFail( context );
		if ( multiOptional.isPresent() ) {
			ProjectionBindingMultiContext multi = multiOptional.get();
			bind( context, multi, fieldPath, multi.containerElement().rawType() );
		}
		else {
			bind( context, fieldPath, context.constructorParameter().rawType() );
		}
	}

	private <T> void bind(ProjectionBindingContext context, String fieldPath, Class<T> constructorParameterType) {
		context.definition( constructorParameterType, context.isIncluded( fieldPath )
				? BeanHolder
						.of( new FieldProjectionDefinition.SingleValued<>( fieldPath, constructorParameterType, valueConvert ) )
				: ConstantProjectionDefinition.nullValue() );
	}

	private <T> void bind(ProjectionBindingContext context, ProjectionBindingMultiContext multi, String fieldPath,
			Class<T> containerElementType) {
		multi.definition( containerElementType, context.isIncluded( fieldPath )
				? BeanHolder.of( new FieldProjectionDefinition.MultiValued<>( fieldPath, containerElementType, valueConvert ) )
				: ConstantProjectionDefinition.emptyList() );
	}

	private String fieldPathOrFail(ProjectionBindingContext context) {
		if ( fieldPathOrNull != null ) {
			return fieldPathOrNull;
		}
		Optional<String> paramName = context.constructorParameter().name();
		if ( !paramName.isPresent() ) {
			throw log.missingParameterNameForFieldProjectionInProjectionConstructor();
		}
		return paramName.get();
	}
}
