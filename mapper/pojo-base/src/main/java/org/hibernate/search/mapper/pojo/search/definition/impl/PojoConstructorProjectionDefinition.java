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

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
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
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingConstructorNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoConstructorProjectionDefinitionMessages;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

public final class PojoConstructorProjectionDefinition<T>
		implements CompositeProjectionDefinition<T>, ToStringTreeAppendable {

	public static <T> PojoConstructorProjectionDefinition<T> create(PojoMappingHelper mappingHelper,
			PojoConstructorModel<T> constructor) {
		return new ConstructorNode<>( mappingHelper, constructor ).createConstructorProjectionDefinition();
	}

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final PojoConstructorProjectionDefinitionMessages MESSAGES = PojoConstructorProjectionDefinitionMessages.INSTANCE;

	private final ConstructorIdentifier constructor;
	private final ValueCreateHandle<? extends T> valueCreateHandle;
	private final List<InnerProjectionDefinition> innerDefinitions;

	private PojoConstructorProjectionDefinition(PojoConstructorModel<T> constructor,
			List<InnerProjectionDefinition> innerDefinitions) {
		this.constructor = new ConstructorIdentifier( constructor );
		this.valueCreateHandle = constructor.handle();
		this.innerDefinitions = innerDefinitions;
	}

	@Override
	public String toString() {
		return "PojoConstructorProjectionDefinition["
				+ "valueCreateHandle=" + valueCreateHandle
				+ ']';
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "valueCreateHandle", valueCreateHandle )
				.attribute( "innerDefinitions", innerDefinitions );
	}

	@Override
	public CompositeProjectionValueStep<?, T> apply(SearchProjectionFactory<?, ?> projectionFactory,
			CompositeProjectionInnerStep initialStep) {
		int i = -1;
		try {
			SearchProjection<?>[] innerProjections = new SearchProjection<?>[innerDefinitions.size()];
			for ( i = 0; i < innerDefinitions.size(); i++ ) {
				innerProjections[i] = innerDefinitions.get( i ).create( projectionFactory );
			}
			return initialStep.from( innerProjections ).asArray( valueCreateHandle );
		}
		catch (ConstructorProjectionApplicationException e) {
			// We already know what prevented from applying a projection constructor correctly,
			// just add a parent constructor and re-throw:
			ProjectionConstructorPath path = new ProjectionConstructorPath( e.projectionConstructorPath(), i, constructor );
			throw log.errorApplyingProjectionConstructor(
					e.getCause().getMessage(), e, path
			);
		}
		catch (SearchException e) {
			ProjectionConstructorPath path = new ProjectionConstructorPath( constructor );
			throw log.errorApplyingProjectionConstructor( e.getMessage(), e, path );
		}
	}

	private static class ConstructorNode<T> implements EventContextProvider {
		private final PojoMappingHelper mappingHelper;
		private final ConstructorParameterNode<?> parent;
		private final String relativeFieldPath;
		private final String absoluteFieldPath;
		private final PojoConstructorModel<T> constructor;

		ConstructorNode(PojoMappingHelper mappingHelper, PojoConstructorModel<T> constructor) {
			this( mappingHelper, constructor, null, null );
		}

		ConstructorNode(PojoMappingHelper mappingHelper, PojoConstructorModel<T> constructor,
				ConstructorParameterNode<?> parent, String relativeFieldPath) {
			this.mappingHelper = mappingHelper;
			this.parent = parent;
			this.relativeFieldPath = relativeFieldPath;
			this.absoluteFieldPath = FieldPaths.compose( parent == null ? null : parent.parent.absoluteFieldPath,
					relativeFieldPath );
			this.constructor = constructor;
		}

		@Override
		public EventContext eventContext() {
			return EventContext.concat(
					parent == null ? null : parent.eventContext(),
					PojoEventContexts.fromType( constructor.typeModel() ),
					PojoEventContexts.fromConstructor( constructor )
			);
		}

		PojoConstructorProjectionDefinition<T> createConstructorProjectionDefinition() {
			if ( constructor.typeModel().isAbstract() ) {
				throw log.invalidAbstractTypeForProjectionConstructor( constructor.typeModel() );
			}
			if ( parent != null ) {
				String path = parent.parent.getPathFromSameProjectionConstructor( constructor );
				if ( path != null ) {
					throw log.infiniteRecursionForProjectionConstructor( constructor,
							FieldPaths.compose( path, relativeFieldPath ) );
				}
			}
			List<InnerProjectionDefinition> innerDefinitions = new ArrayList<>();
			for ( PojoMethodParameterModel<?> parameter : constructor.declaredParameters() ) {
				ConstructorParameterNode<?> parameterNode =
						new ConstructorParameterNode<>( mappingHelper, this, parameter );
				InnerProjectionDefinition innerProjection;
				try {
					innerProjection = parameterNode.inferInnerProjection();
				}
				catch (RuntimeException e) {
					mappingHelper.failureCollector()
							.withContext( parameterNode.eventContext() )
							.add( e );
					// Here the result no longer matters, bootstrap will fail anyway.
					// We just pick a neutral value that won't trigger cascading failures.
					innerProjection = NullInnerProjectionDefinition.INSTANCE;
				}
				innerDefinitions.add( innerProjection );
			}
			return new PojoConstructorProjectionDefinition<>( constructor, innerDefinitions );
		}

		private String getPathFromSameProjectionConstructor(PojoConstructorModel<?> constructorToMatch) {
			if ( this.constructor.equals( constructorToMatch ) ) {
				return "";
			}
			else if ( parent != null ) {
				String path = parent.parent.getPathFromSameProjectionConstructor( constructorToMatch );
				return path == null ? null : FieldPaths.compose( path, relativeFieldPath );
			}
			else {
				// This is the root.
				// If we reach this point, no definition involving the given projection constructor was found.
				return null;
			}
		}
	}

	private static class ConstructorParameterNode<P> implements EventContextProvider {
		private final PojoMappingHelper mappingHelper;
		private final ConstructorNode<?> parent;
		private final PojoMethodParameterModel<P> parameter;

		ConstructorParameterNode(PojoMappingHelper mappingHelper, ConstructorNode<?> parent,
				PojoMethodParameterModel<P> parameter) {
			this.mappingHelper = mappingHelper;
			this.parent = parent;
			this.parameter = parameter;
		}

		@Override
		public EventContext eventContext() {
			return parent.eventContext().append( PojoEventContexts.fromMethodParameter( parameter ) );
		}

		InnerProjectionDefinition inferInnerProjection() {
			if ( parameter.isEnclosingInstance() ) {
				// Let's ignore this parameter, because we are not able to provide an enclosing instance,
				// and it's often useful to be able to declare a method-local type for projections
				// (those types have a "enclosing instance" parameter in their constructor
				// even if they don't use it).
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
					throw log.invalidMultiValuedParameterTypeForProjectionConstructor( parameterType );
				}
				multi = true;
			}

			PojoRawTypeModel<?> boundParameterElementRawType = boundParameterElementPath.getExtractedType().rawType();
			Optional<String> paramName = parameter.name();
			if ( !paramName.isPresent() ) {
				throw log.missingParameterNameForProjectionConstructor();
			}
			String innerRelativeFieldPath = paramName.get();
			String innerAbsoluteFieldPath = FieldPaths.compose( parent.absoluteFieldPath, innerRelativeFieldPath );
			PojoConstructorProjectionDefinition<?> definition = createConstructorProjectionDefinitionOrNull(
					boundParameterElementRawType, innerRelativeFieldPath );
			if ( definition != null ) {
				return new ObjectInnerProjectionDefinition( innerAbsoluteFieldPath, multi, definition );
			}
			else {
				// No projection constructor for this type; assume it's a projection on value field
				return new ValueInnerProjectionDefinition( innerAbsoluteFieldPath, multi,
						boundParameterElementRawType.typeIdentifier().javaClass() );
			}
		}

		<T> PojoConstructorProjectionDefinition<T> createConstructorProjectionDefinitionOrNull(PojoRawTypeModel<T> parameterType,
				String relativeFieldPath) {
			PojoConstructorProjectionDefinition<T> result = null;
			for ( PojoTypeMetadataContributor contributor : mappingHelper.contributorProvider()
					// Constructor mapping is not inherited
					.getIgnoringInheritance( parameterType ) ) {
				for ( PojoSearchMappingConstructorNode constructorMapping : contributor.constructors().values() ) {
					if ( constructorMapping.isProjectionConstructor() ) {
						if ( result != null ) {
							throw log.multipleProjectionConstructorsForType( parameterType.typeIdentifier().javaClass() );
						}
						PojoConstructorModel<T> constructor = parameterType.constructor( constructorMapping.parametersJavaTypes() );
						result = new ConstructorNode<>( mappingHelper, constructor, this, relativeFieldPath )
								.createConstructorProjectionDefinition();
					}
				}
			}
			return result;
		}
	}

	public static class ConstructorIdentifier {
		private final String name;
		private final Class<?>[] parametersJavaTypes;

		public ConstructorIdentifier(PojoConstructorModel<?> constructor) {
			this.name = constructor.typeModel().name();
			this.parametersJavaTypes = constructor.parametersJavaTypes();
		}

		public String toHighlightedString(int position) {
			return name + "(" + CommaSeparatedClassesFormatter.formatHighlighted( parametersJavaTypes, position ) + ")";
		}

		@Override
		public String toString() {
			return toHighlightedString( -1 );
		}
	}

	public static class ProjectionConstructorPath {
		private final ProjectionConstructorPath child;
		private final int position;
		private final ConstructorIdentifier constructor;

		public ProjectionConstructorPath(ProjectionConstructorPath child, int position,
				ConstructorIdentifier constructor) {
			this.child = child;
			this.position = position;
			this.constructor = constructor;
		}

		public ProjectionConstructorPath(ConstructorIdentifier constructor) {
			this( null, -1, constructor );
		}

		public String toPrefixedString() {
			return "\n" + MESSAGES.executedConstructorPath() + "\n" + this;
		}

		@Override
		public String toString() {
			return child == null ? constructor.toString() :
					child + "\n\t\u2937 for parameter #" + position + " in " + constructor.toHighlightedString(
							position );
		}
	}
}
