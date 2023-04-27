/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
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
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.search.definition.impl.InnerProjectionDefinition;
import org.hibernate.search.mapper.pojo.search.definition.impl.NullInnerProjectionDefinition;
import org.hibernate.search.mapper.pojo.search.definition.impl.ObjectInnerProjectionDefinition;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoConstructorProjectionDefinition;
import org.hibernate.search.mapper.pojo.search.definition.impl.ValueInnerProjectionDefinition;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

class ProjectionConstructorParameterBinder<P> implements EventContextProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final PojoMappingHelper mappingHelper;
	final ProjectionConstructorBinder<?> parent;
	private final PojoMethodParameterModel<P> parameter;

	ProjectionConstructorParameterBinder(PojoMappingHelper mappingHelper, ProjectionConstructorBinder<?> parent,
			PojoMethodParameterModel<P> parameter) {
		this.mappingHelper = mappingHelper;
		this.parent = parent;
		this.parameter = parameter;
	}

	@Override
	public EventContext eventContext() {
		return parent.eventContext().append( PojoEventContexts.fromMethodParameter( parameter ) );
	}

	InnerProjectionDefinition bind() {
		if ( parameter.isEnclosingInstance() ) {
			// Let's ignore this parameter, because we are not able to provide a surrounding instance,
			// and it's often useful to be able to declare a method-local type for projections
			// (those types have a "surrounding instance" parameter in their constructor
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
					|| !( BuiltinContainerExtractors.COLLECTION.equals( boundParameterElementExtractorNames.get( 0 ) )
					|| BuiltinContainerExtractors.ITERABLE.equals( boundParameterElementExtractorNames.get( 0 ) ) )
					|| !mappingHelper.introspector().typeModel( List.class ).isSubTypeOf(
					parameterType.rawType().rawType() ) ) {
				throw log.invalidMultiValuedParameterTypeForProjectionConstructor(
						parameterType );
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
					boundParameterElementRawType.typeIdentifier().javaClass()
			);
		}
	}

	<T> PojoConstructorProjectionDefinition<T> createConstructorProjectionDefinitionOrNull(
			PojoRawTypeModel<T> parameterType,
			String relativeFieldPath) {
		PojoConstructorProjectionDefinition<T> result = null;
		for ( PojoTypeMetadataContributor contributor : mappingHelper.contributorProvider()
				// Constructor mapping is not inherited
				.getIgnoringInheritance( parameterType ) ) {
			for ( PojoSearchMappingConstructorNode constructorMapping : contributor.constructors().values() ) {
				if ( constructorMapping.isProjectionConstructor() ) {
					if ( result != null ) {
						throw log.multipleProjectionConstructorsForType(
								parameterType.typeIdentifier().javaClass() );
					}
					PojoConstructorModel<T> constructor = parameterType.constructor(
							constructorMapping.parametersJavaTypes() );
					result = new ProjectionConstructorBinder<>( mappingHelper, constructor, this, relativeFieldPath )
							.bind();
				}
			}
		}
		return result;
	}
}
