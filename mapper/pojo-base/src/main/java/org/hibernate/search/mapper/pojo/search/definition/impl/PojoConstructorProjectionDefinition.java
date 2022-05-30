/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingCollectorConstructorNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;

public final class PojoConstructorProjectionDefinition<T>
		implements CompositeProjectionDefinition<T>, ToStringTreeAppendable {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ValueCreateHandle<? extends T> valueCreateHandle;
	private final List<InnerProjectionDefinition> innerDefinitions;

	private PojoConstructorProjectionDefinition(ValueCreateHandle<? extends T> valueCreateHandle,
			List<InnerProjectionDefinition> innerDefinitions) {
		this.valueCreateHandle = valueCreateHandle;
		this.innerDefinitions = innerDefinitions;
	}

	@Override
	public String toString() {
		return "PojoConstructorProjectionDefinition["
				+ "valueCreateHandle=" + valueCreateHandle
				+ ']';
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "valueCreateHandle", valueCreateHandle )
				.attribute( "innerDefinitions", innerDefinitions );
	}

	@Override
	public CompositeProjectionValueStep<?, T> apply(SearchProjectionFactory<?, ?> projectionFactory,
			CompositeProjectionInnerStep initialStep) {
		SearchProjection<?>[] innerProjections = new SearchProjection<?>[innerDefinitions.size()];
		for ( int i = 0; i < innerDefinitions.size(); i++ ) {
			innerProjections[i] = innerDefinitions.get( i ).create( projectionFactory );
		}
		return initialStep.from( innerProjections ).asArray( valueCreateHandle );
	}

	static class ConstructorNode<T> implements PojoSearchMappingCollectorConstructorNode {
		private final PojoMappingHelper mappingHelper;
		private final ConstructorParameterTypeNode<?> parent;
		private final String relativeFieldPath;
		private final String absoluteFieldPath;
		private final PojoConstructorModel<T> constructor;
		private final Consumer<PojoConstructorProjectionDefinition<T>> collector;

		ConstructorNode(PojoMappingHelper mappingHelper,
				PojoConstructorModel<T> constructor, Consumer<PojoConstructorProjectionDefinition<T>> collector) {
			this( mappingHelper, null, constructor, collector );
		}

		ConstructorNode(PojoMappingHelper mappingHelper, ConstructorParameterTypeNode<?> parent,
				PojoConstructorModel<T> constructor, Consumer<PojoConstructorProjectionDefinition<T>> collector) {
			this.mappingHelper = mappingHelper;
			this.parent = parent;
			this.relativeFieldPath = parent == null ? null : parent.relativeFieldPath;
			this.absoluteFieldPath = FieldPaths.compose( parent == null ? null : parent.parent.parent.absoluteFieldPath,
					relativeFieldPath );
			this.constructor = constructor;
			this.collector = collector;
		}

		@Override
		public ContextualFailureCollector failureCollector() {
			if ( parent == null ) {
				return mappingHelper.failureCollector()
						.withContext( PojoEventContexts.fromType( constructor.typeModel() ) )
						.withContext( PojoEventContexts.fromConstructor( constructor ) );
			}
			else {
				return parent.failureCollector()
						.withContext( PojoEventContexts.fromConstructor( constructor ) );
			}
		}

		@Override
		public void projectionConstructor() {
			if ( constructor.typeModel().isAbstract() ) {
				throw log.invalidAbstractTypeForProjectionConstructor( constructor.typeModel() );
			}
			if ( parent != null ) {
				String path = parent.parent.parent.getPathFromSameProjectionConstructor( constructor );
				if ( path != null ) {
					throw log.infiniteRecursionForProjectionConstructor( constructor, FieldPaths.compose( path,
							relativeFieldPath
					) );
				}
			}
			List<InnerProjectionDefinition> innerDefinitions = new ArrayList<>();
			for ( PojoMethodParameterModel<?> parameter : constructor.declaredParameters() ) {
				ConstructorParameterNode<?> parameterNode =
						new ConstructorParameterNode<>( mappingHelper, this, parameter );
				innerDefinitions.add( parameterNode.inferInnerProjection() );
			}
			collector.accept( new PojoConstructorProjectionDefinition<>( constructor.handle(), innerDefinitions ) );
		}

		private String getPathFromSameProjectionConstructor(PojoConstructorModel<?> constructorToMatch) {
			if ( this.constructor.equals( constructorToMatch ) ) {
				return "";
			}
			else if ( parent != null ) {
				String path = parent.parent.parent.getPathFromSameProjectionConstructor( constructorToMatch );
				return path == null ? null : FieldPaths.compose( path, relativeFieldPath );
			}
			else {
				// This is the root.
				// If we reach this point, no definition involving the given projection constructor was found.
				return null;
			}
		}
	}

	private static class ConstructorParameterNode<P> implements PojoMappingCollector {
		private final PojoMappingHelper mappingHelper;
		private final ConstructorNode<?> parent;
		private final PojoMethodParameterModel<P> parameter;

		public ConstructorParameterNode(PojoMappingHelper mappingHelper, ConstructorNode<?> parent,
				PojoMethodParameterModel<P> parameter) {
			this.mappingHelper = mappingHelper;
			this.parent = parent;
			this.parameter = parameter;
		}

		@Override
		public ContextualFailureCollector failureCollector() {
			return parent.failureCollector()
					.withContext( PojoEventContexts.fromMethodParameter( parameter ) );
		}

		InnerProjectionDefinition inferInnerProjection() {
			if ( parameter.isImplicit() ) {
				// Inner class: the constructor has an implicit parameter for the instance of the surrounding class.
				// Let's ignore that, because there isn't much we can do about it anyway.
				return NullInnerProjectionDefinition.INSTANCE;
			}
			PojoTypeModel<P> parameterType = parameter.typeModel();
			BoundContainerExtractorPath<?, ?> boundParameterElementPath = mappingHelper.indexModelBinder()
					.bindExtractorPath( parameter.typeModel(), ContainerExtractorPath.defaultExtractors() );
			List<String> boundParameterElementExtractorNames =
					boundParameterElementPath.getExtractorPath().explicitExtractorNames();

			boolean multi;
			if ( boundParameterElementExtractorNames.isEmpty() ) {
				multi = false;
			}
			else {
				if ( boundParameterElementExtractorNames.size() > 1
						|| ! ( BuiltinContainerExtractors.COLLECTION.equals( boundParameterElementExtractorNames.get( 0 ) )
										|| BuiltinContainerExtractors.ITERABLE.equals( boundParameterElementExtractorNames.get( 0 ) ) )
						|| !mappingHelper.introspector().typeModel( List.class ).isSubTypeOf( parameterType.rawType().rawType() ) ) {
					throw log.invalidMultiValuedParameterTypeForProjectionConstructor( parameterType,
							PojoEventContexts.fromMethodParameter( parameter ) );
				}
				multi = true;
			}

			PojoRawTypeModel<?> boundParameterElementRawType = boundParameterElementPath.getExtractedType().rawType();
			Optional<String> paramName = parameter.name();
			if ( !paramName.isPresent() ) {
				throw log.missingParameterNameForProjectionConstructor( parent.constructor.typeModel(),
						PojoEventContexts.fromMethodParameter( parameter ) );
			}
			String innerRelativeFieldPath = paramName.get();
			String innerAbsoluteFieldPath = FieldPaths.compose( parent.absoluteFieldPath, innerRelativeFieldPath );
			ConstructorParameterTypeNode<?> parameterTypeNode = new ConstructorParameterTypeNode<>( mappingHelper,
					this, innerRelativeFieldPath, boundParameterElementRawType );
			for ( PojoTypeMetadataContributor contributor : mappingHelper.contributorProvider().get( boundParameterElementRawType ) ) {
				contributor.contributeSearchMapping( parameterTypeNode );
			}

			if ( parameterTypeNode.constructorProjectionDefinition != null ) {
				return new ObjectInnerProjectionDefinition( innerAbsoluteFieldPath, multi, parameterTypeNode.constructorProjectionDefinition );
			}
			else {
				// No projection constructor mapping for this type; assume it's a projection on value field
				return new ValueInnerProjectionDefinition( innerAbsoluteFieldPath, multi,
						boundParameterElementRawType.typeIdentifier().javaClass() );
			}
		}
	}

	private static class ConstructorParameterTypeNode<T>
			implements PojoSearchMappingCollectorTypeNode, Consumer<PojoConstructorProjectionDefinition<T>> {
		private final PojoMappingHelper mappingHelper;
		private final ConstructorParameterNode<?> parent;
		private final String relativeFieldPath;
		private final PojoRawTypeModel<T> type;

		private PojoConstructorProjectionDefinition<T> constructorProjectionDefinition;

		public ConstructorParameterTypeNode(PojoMappingHelper mappingHelper, ConstructorParameterNode<?> parent,
				String relativeFieldPath, PojoRawTypeModel<T> type) {
			this.mappingHelper = mappingHelper;
			this.parent = parent;
			this.relativeFieldPath = relativeFieldPath;
			this.type = type;
		}

		@Override
		public ContextualFailureCollector failureCollector() {
			return parent.failureCollector()
					.withContext( PojoEventContexts.fromType( type ) );
		}

		@Override
		public PojoRawTypeIdentifier<?> typeIdentifier() {
			return type.typeIdentifier();
		}

		@Override
		public PojoSearchMappingCollectorConstructorNode constructor(Class<?>[] parameterTypes) {
			return new ConstructorNode<>( mappingHelper, this, type.constructor( parameterTypes ), this );
		}

		@Override
		public void accept(PojoConstructorProjectionDefinition<T> constructorProjectionDefinition) {
			if ( this.constructorProjectionDefinition != null ) {
				throw log.multipleProjectionConstructorsForType( type.typeIdentifier().javaClass() );
			}
			this.constructorProjectionDefinition = constructorProjectionDefinition;
		}
	}

}
