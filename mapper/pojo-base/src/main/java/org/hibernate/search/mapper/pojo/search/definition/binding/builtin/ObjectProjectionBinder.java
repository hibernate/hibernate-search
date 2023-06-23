/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingMultiContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Binds a constructor parameter to an object projection bound to a specific object field in the indexed document.
 * <p>
 * The content of the object projection is defined in the constructor parameter type
 * by another {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor}.
 * <p>
 * Compared to the basic {@link CompositeProjectionBinder composite projection},
 * an object projection is bound to a specific object field,
 * and thus it yields zero, one or many values, as many as there are objects in the targeted object field.
 * Therefore, you must take care of using a {@code List<...>} as your constructor parameter type
 * if the object field is multi-valued.
 *
 * @see org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory#object(String)
 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection
 */
public final class ObjectProjectionBinder implements ProjectionBinder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	/**
	 * Creates an {@link ObjectProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 * <p>
	 * This method requires the projection constructor class to be compiled with the {@code -parameters} flag
	 * to infer the field path from the name of the constructor parameter being bound.
	 * If this compiler flag is not used,
	 * use {@link #create(String)} instead and pass the field path explicitly.
	 *
	 * @return The binder.
	 */
	public static ObjectProjectionBinder create() {
		return create( null );
	}

	/**
	 * Creates an {@link ObjectProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @param fieldPath The <a href="../../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose value will be extracted.
	 * When {@code null}, defaults to the name of the constructor parameter being bound,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise a null {@code fieldPath} will lead to a failure).
	 * @return The binder.
	 */
	public static ObjectProjectionBinder create(String fieldPath) {
		return new ObjectProjectionBinder( fieldPath );
	}

	private final String fieldPathOrNull;

	private TreeFilterDefinition filter = TreeFilterDefinition.includeAll();

	private ObjectProjectionBinder(String fieldPathOrNull) {
		this.fieldPathOrNull = fieldPathOrNull;
	}

	@Override
	public String toString() {
		return "ObjectProjectionBinder(...)";
	}

	/**
	 * @param filter The filter to apply to determine which nested index field projections should be included in the projection.
	 * @return {@code this}, for method chaining.
	 * @see ObjectProjection#includePaths()
	 * @see ObjectProjection#excludePaths()
	 * @see ObjectProjection#includeDepth()
	 */
	public ObjectProjectionBinder filter(TreeFilterDefinition filter) {
		this.filter = filter;
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
		context.definition( constructorParameterType,
				context.createObjectDefinition( fieldPath, constructorParameterType, filter ) );
	}

	private <T> void bind(ProjectionBindingContext context, ProjectionBindingMultiContext multi,
			String fieldPath, Class<T> containerElementType) {
		multi.definition( containerElementType,
				context.createObjectDefinitionMulti( fieldPath, containerElementType, filter ) );
	}

	private String fieldPathOrFail(ProjectionBindingContext context) {
		if ( fieldPathOrNull != null ) {
			return fieldPathOrNull;
		}
		Optional<String> paramName = context.constructorParameter().name();
		if ( !paramName.isPresent() ) {
			throw log.missingParameterNameForObjectProjectionInProjectionConstructor();
		}
		return paramName.get();
	}
}
