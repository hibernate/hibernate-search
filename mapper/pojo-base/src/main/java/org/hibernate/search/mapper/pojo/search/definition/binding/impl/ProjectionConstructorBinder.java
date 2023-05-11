/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.engine.search.projection.definition.spi.ConstantProjectionDefinition;
import org.hibernate.search.mapper.pojo.search.definition.impl.PojoConstructorProjectionDefinition;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

public class ProjectionConstructorBinder<T> implements EventContextProvider {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMappingHelper mappingHelper;
	private final ProjectionConstructorParameterBinder<?> parent;
	private final String relativeFieldPath;
	final PojoConstructorModel<T> constructor;

	public ProjectionConstructorBinder(PojoMappingHelper mappingHelper, PojoConstructorModel<T> constructor) {
		this( mappingHelper, constructor, null, null );
	}

	public ProjectionConstructorBinder(PojoMappingHelper mappingHelper, PojoConstructorModel<T> constructor,
			ProjectionConstructorParameterBinder<?> parent, String relativeFieldPath) {
		this.mappingHelper = mappingHelper;
		this.parent = parent;
		this.relativeFieldPath = relativeFieldPath;
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

	public PojoConstructorProjectionDefinition<T> bind() {
		if ( constructor.typeModel().isAbstract() ) {
			throw log.invalidAbstractTypeForProjectionConstructor(
					constructor.typeModel() );
		}
		if ( parent != null ) {
			String path = parent.parent.getPathFromSameProjectionConstructor( constructor );
			if ( path != null ) {
				throw log.infiniteRecursionForProjectionConstructor(
						constructor,
						FieldPaths.compose( path, relativeFieldPath )
				);
			}
		}
		List<BeanHolder<? extends ProjectionDefinition<?>>> parameterDefinitions = new ArrayList<>();
		for ( PojoMethodParameterModel<?> parameter : constructor.declaredParameters() ) {
			ProjectionConstructorParameterBinder<?> parameterBinder =
					new ProjectionConstructorParameterBinder<>( mappingHelper, this, parameter );
			BeanHolder<? extends ProjectionDefinition<?>> parameterDefinition;
			try {
				parameterDefinition = parameterBinder.bind();
			}
			catch (RuntimeException e) {
				mappingHelper.failureCollector()
						.withContext( parameterBinder.eventContext() )
						.add( e );
				// Here the result no longer matters, bootstrap will fail anyway.
				// We just pick a neutral value that won't trigger cascading failures.
				parameterDefinition = ConstantProjectionDefinition.nullValue();
			}
			parameterDefinitions.add( parameterDefinition );
		}
		return new PojoConstructorProjectionDefinition<>( constructor, parameterDefinitions );
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
