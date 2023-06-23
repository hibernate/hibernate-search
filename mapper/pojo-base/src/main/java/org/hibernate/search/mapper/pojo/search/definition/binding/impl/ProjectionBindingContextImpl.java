/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeNestingContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ConstantProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ObjectProjectionDefinition;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.PojoModelConstructorParameter;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelConstructorParameterRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingMultiContext;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.FieldProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.builtin.ObjectProjectionBinder;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ProjectionBindingContextImpl<P> implements ProjectionBindingContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final BiFunction<MappingElement, String, SearchException> CYCLIC_RECURSION_EXCEPTION_FACTORY =
			(mappingElement, cyclicRecursionPath) -> log.objectProjectionCyclicRecursion( mappingElement,
					mappingElement.eventContext(), cyclicRecursionPath );

	private final PojoMappingHelper mappingHelper;
	final ProjectionConstructorParameterBinder<P> parameterBinder;
	private final Map<String, Object> params;
	private final PojoTypeModel<?> parameterTypeModel;
	private final PojoModelConstructorParameterRootElement<P> parameterRootElement;

	private MappingElement mappingElement;
	private PartialBinding<P> partialBinding;

	public ProjectionBindingContextImpl(ProjectionConstructorParameterBinder<P> parameterBinder,
			Map<String, Object> params) {
		this.mappingHelper = parameterBinder.mappingHelper;
		this.parameterBinder = parameterBinder;
		this.params = params;
		this.parameterTypeModel = parameterBinder.parameter.typeModel();
		this.parameterRootElement = parameterBinder.parameterRootElement;
	}

	@Override
	public BeanResolver beanResolver() {
		return mappingHelper.beanResolver();
	}

	@Override
	public <T> T param(String name, Class<T> paramType) {
		Contracts.assertNotNull( name, "name" );
		Contracts.assertNotNull( paramType, "paramType" );

		Object value = params.get( name );
		if ( value == null ) {
			throw log.paramNotDefined( name );
		}

		return paramType.cast( value );
	}

	@Override
	public <T> Optional<T> paramOptional(String name, Class<T> paramType) {
		Contracts.assertNotNull( name, "name" );
		Contracts.assertNotNull( paramType, "paramType" );

		return Optional.ofNullable( params.get( name ) ).map( paramType::cast );
	}

	@Override
	public <P2> void definition(Class<P2> expectedValueType, ProjectionDefinition<? extends P2> definition) {
		definition( expectedValueType, BeanHolder.of( definition ) );
	}

	@Override
	public <P2> void definition(Class<P2> expectedValueType,
			BeanHolder<? extends ProjectionDefinition<? extends P2>> definitionHolder) {
		checkAndBind( definitionHolder, mappingHelper.introspector().typeModel( expectedValueType ) );
	}

	@Override
	public Optional<MultiContextImpl<?>> multi() {
		BoundContainerExtractorPath<?, ?> boundParameterElementPath = mappingHelper.indexModelBinder()
				.bindExtractorPath( parameterTypeModel, ContainerExtractorPath.defaultExtractors() );
		List<String> boundParameterElementExtractorNames =
				boundParameterElementPath.getExtractorPath().explicitExtractorNames();

		if ( boundParameterElementExtractorNames.isEmpty() ) {
			return Optional.empty();
		}
		else {
			if ( boundParameterElementExtractorNames.size() > 1
					|| !( BuiltinContainerExtractors.COLLECTION.equals( boundParameterElementExtractorNames.get( 0 ) )
							|| BuiltinContainerExtractors.ITERABLE.equals( boundParameterElementExtractorNames.get( 0 ) ) )
					|| !mappingHelper.introspector().typeModel( List.class ).isSubTypeOf( parameterTypeModel.rawType() ) ) {
				throw log.invalidMultiValuedParameterTypeForProjectionConstructor( parameterTypeModel );
			}
			return Optional.of( new MultiContextImpl<>( boundParameterElementPath.getExtractedType() ) );
		}
	}

	@Override
	public PojoModelConstructorParameter constructorParameter() {
		return parameterRootElement;
	}

	@Override
	public <T> BeanHolder<? extends ProjectionDefinition<T>> createObjectDefinition(String fieldPath,
			Class<T> projectedType, TreeFilterDefinition filter) {
		Contracts.assertNotNull( fieldPath, "fieldPath" );
		Contracts.assertNotNull( projectedType, "projectedType" );
		Contracts.assertNotNull( filter, "filter" );
		Optional<BeanHolder<? extends ProjectionDefinition<T>>> objectProjection = nestObjectProjection(
				fieldPath, filter,
				nestingContext -> {
					CompositeProjectionDefinition<T> composite =
							createCompositeProjectionDefinition( projectedType, nestingContext );
					try {
						return BeanHolder.ofCloseable( new ObjectProjectionDefinition.SingleValued<>(
								fieldPath, composite ) );
					}
					catch (RuntimeException e) {
						new SuppressingCloser( e )
								.push( composite );
						throw e;
					}
				}
		);
		return objectProjection.orElse( ConstantProjectionDefinition.nullValue() );
	}

	@Override
	public <T> BeanHolder<? extends ProjectionDefinition<List<T>>> createObjectDefinitionMulti(String fieldPath,
			Class<T> projectedType, TreeFilterDefinition filter) {
		Contracts.assertNotNull( fieldPath, "fieldPath" );
		Contracts.assertNotNull( projectedType, "projectedType" );
		Contracts.assertNotNull( filter, "filter" );
		Optional<BeanHolder<? extends ProjectionDefinition<List<T>>>> objectProjection = nestObjectProjection(
				fieldPath, filter,
				nestingContext -> {
					CompositeProjectionDefinition<T> composite =
							createCompositeProjectionDefinition( projectedType, nestingContext );
					try {
						return BeanHolder.ofCloseable( new ObjectProjectionDefinition.MultiValued<>(
								fieldPath, composite ) );
					}
					catch (RuntimeException e) {
						new SuppressingCloser( e )
								.push( composite );
						throw e;
					}
				}
		);
		return objectProjection.orElse( ConstantProjectionDefinition.emptyList() );
	}

	private <T> Optional<T> nestObjectProjection(String fieldPath, TreeFilterDefinition filter,
			Function<TreeNestingContext, T> contextBuilder) {
		return parameterBinder.parent.nestingContext.nestComposed(
				mappingElement,
				fieldPath + ".", filter,
				mappingHelper.getOrCreatePathTracker( mappingElement, filter ),
				new TreeNestingContext.NestedContextBuilder<T>() {
					@Override
					public void appendObject(String objectName) {
						// Ignore
					}

					@Override
					public T build(TreeNestingContext nestingContext) {
						return contextBuilder.apply( nestingContext );
					}
				},
				CYCLIC_RECURSION_EXCEPTION_FACTORY
		);
	}

	@Override
	public <T> BeanHolder<? extends ProjectionDefinition<T>> createCompositeDefinition(Class<T> projectedType) {
		return BeanHolder.ofCloseable( createCompositeProjectionDefinition( projectedType,
				parameterBinder.parent.nestingContext ) );
	}

	private <T> CompositeProjectionDefinition<T> createCompositeProjectionDefinition(Class<T> projectedType,
			TreeNestingContext nestingContext) {
		PojoConstructorModel<T> projectionConstructor = parameterBinder.findProjectionConstructorOrNull(
				mappingHelper.introspector().typeModel( projectedType ) );
		if ( projectionConstructor == null ) {
			throw log.invalidObjectClassForProjection( projectedType );
		}
		return new ProjectionConstructorBinder<>( mappingHelper, projectionConstructor, this, nestingContext )
				.bind();
	}

	@Override
	public boolean isIncluded(String fieldPath) {
		return parameterBinder.parent.nestingContext.nest( fieldPath,
				// Ideally, for consistency, we'd expose an API relying on lambdas, like nest(...) does,
				// but I must admit that's not exactly obvious to work with.
				// Also the only reason that (rather convoluted) lambda-based SPI exists
				// is because of @IndexedEmbedded.prefix, which may alters the name of nested fields...
				// but we don't have an equivalent for @IndexedEmbedded.prefix in the case of @ObjectProjection.
				// So we'll just use a more traditional API that simply returns a boolean.
				(prefixedRelativeName, inclusion) -> TreeNodeInclusion.INCLUDED.equals( inclusion ) );
	}

	public BeanHolder<? extends ProjectionDefinition<? extends P>> applyBinder(ProjectionBinder binder) {
		try {
			this.mappingElement = new PojoConstructorParameterProjectionMappingElement( parameterBinder.parent.constructor,
					parameterBinder.parameter, binder );
			// This call should set the partial binding
			binder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingProjectionDefinitionForBinder( binder );
			}

			return partialBinding.complete();
		}
		catch (RuntimeException e) {
			if ( partialBinding != null ) {
				partialBinding.abort( new SuppressingCloser( e ) );
			}
			throw e;
		}
		finally {
			partialBinding = null;
			this.mappingElement = null;
		}
	}

	private <P2> void checkAndBind(BeanHolder<? extends ProjectionDefinition<? extends P2>> definitionHolder,
			PojoRawTypeModel<P2> expectedValueType) {
		if ( !expectedValueType.isSubTypeOf( parameterTypeModel.rawType() ) ) {
			throw log.invalidOutputTypeForProjectionDefinition( definitionHolder.get(), parameterTypeModel, expectedValueType );
		}

		@SuppressWarnings("unchecked") // We check that P2 extends P explicitly using reflection (see above)
		BeanHolder<? extends ProjectionDefinition<? extends P>> castDefinitionHolder =
				(BeanHolder<? extends ProjectionDefinition<? extends P>>) definitionHolder;

		this.partialBinding = new PartialBinding<>( castDefinitionHolder );
	}

	public BeanHolder<? extends ProjectionDefinition<?>> applyDefaultProjection() {
		Optional<? extends ProjectionBindingContextImpl<?>.MultiContextImpl<?>> multi = multi();
		PojoConstructorModel<?> constructorModelOrNull;
		if ( multi.isPresent() ) {
			constructorModelOrNull = parameterBinder.findProjectionConstructorOrNull(
					multi.get().parameterContainerElementTypeModel.rawType() );
		}
		else {
			constructorModelOrNull = parameterBinder.findProjectionConstructorOrNull(
					parameterTypeModel.rawType() );
		}
		Optional<String> paramName = parameterRootElement.name();
		if ( !paramName.isPresent() ) {
			throw log.missingParameterNameForInferredProjection();
		}
		// We reuse projection binders instead of return projection definitions directly
		// so that we take advantage of their handling of nesting filters.
		if ( constructorModelOrNull != null ) {
			return applyBinder( ObjectProjectionBinder.create( paramName.get() ) );
		}
		else {
			return applyBinder( FieldProjectionBinder.create( paramName.get() ) );
		}
	}

	private static class PartialBinding<P> {
		private final BeanHolder<? extends ProjectionDefinition<? extends P>> definitionHolder;

		private PartialBinding(BeanHolder<? extends ProjectionDefinition<? extends P>> definitionHolder) {
			this.definitionHolder = definitionHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( BeanHolder::close, definitionHolder );
		}

		BeanHolder<? extends ProjectionDefinition<? extends P>> complete() {
			return definitionHolder;
		}
	}

	public class MultiContextImpl<PV> implements ProjectionBindingMultiContext {
		public final PojoTypeModel<PV> parameterContainerElementTypeModel;
		private final PojoModelValue<PV> parameterContainerElementRootElement;

		public MultiContextImpl(PojoTypeModel<PV> parameterContainerElementTypeModel) {
			this.parameterContainerElementTypeModel = parameterContainerElementTypeModel;
			this.parameterContainerElementRootElement = new PojoModelValueElement<>( mappingHelper.introspector(),
					parameterContainerElementTypeModel );
		}

		@Override
		public <P2> void definition(Class<P2> expectedValueType,
				ProjectionDefinition<? extends List<? extends P2>> definition) {
			definition( expectedValueType, BeanHolder.of( definition ) );
		}

		@Override
		public <P2> void definition(Class<P2> expectedValueType,
				BeanHolder<? extends ProjectionDefinition<? extends List<? extends P2>>> definitionHolder) {
			checkAndBind( definitionHolder, mappingHelper.introspector().typeModel( expectedValueType ) );
		}

		@Override
		public PojoModelValue<?> containerElement() {
			return parameterContainerElementRootElement;
		}

		private <P2> void checkAndBind(
				BeanHolder<? extends ProjectionDefinition<? extends List<? extends P2>>> definitionHolder,
				PojoRawTypeModel<P2> expectedValueType) {
			if ( !expectedValueType.isSubTypeOf( parameterContainerElementTypeModel.rawType() ) ) {
				throw log.invalidOutputTypeForMultiValuedProjectionDefinition( definitionHolder.get(), parameterTypeModel,
						expectedValueType );
			}

			@SuppressWarnings("unchecked") // We check that P2 extends PV explicitly using reflection (see above) and we've already checked that P = List<? extends PV>
			BeanHolder<? extends ProjectionDefinition<? extends P>> castDefinitionHolder =
					(BeanHolder<? extends ProjectionDefinition<? extends P>>) definitionHolder;

			partialBinding = new PartialBinding<>( castDefinitionHolder );
		}
	}
}
