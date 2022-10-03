/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingCollectorConstructorNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoSearchMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoSearchQueryElementRegistryBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMappingHelper mappingHelper;
	private final Map<Class<?>, CompositeProjectionDefinition<?>> projectionDefinitions =
			new LinkedHashMap<>();

	public PojoSearchQueryElementRegistryBuilder(PojoMappingHelper mappingHelper) {
		this.mappingHelper = mappingHelper;
	}

	public <T> PojoSearchMappingCollectorTypeNode type(PojoRawTypeModel<T> type) {
		return new TypeNode<>( type );
	}

	public PojoSearchQueryElementRegistry build() {
		return new PojoSearchQueryElementRegistry( projectionDefinitions );
	}

	private class TypeNode<T> implements PojoSearchMappingCollectorTypeNode {
		private final PojoRawTypeModel<T> type;

		public TypeNode(PojoRawTypeModel<T> type) {
			this.type = type;
		}

		@Override
		public ContextualFailureCollector failureCollector() {
			return mappingHelper.failureCollector()
					.withContext( PojoEventContexts.fromType( type ) );
		}

		@Override
		public PojoRawTypeIdentifier<?> typeIdentifier() {
			return type.typeIdentifier();
		}

		@Override
		public PojoSearchMappingCollectorConstructorNode constructor(Class<?>[] parameterTypes) {
			return new ConstructorNode<>( type.constructor( parameterTypes ) );
		}
	}

	private class ConstructorNode<T> implements PojoSearchMappingCollectorConstructorNode {
		private final PojoConstructorModel<T> constructor;

		public ConstructorNode(PojoConstructorModel<T> constructor) {
			this.constructor = constructor;
		}

		@Override
		public ContextualFailureCollector failureCollector() {
			return mappingHelper.failureCollector()
					.withContext( PojoEventContexts.fromType( constructor.typeModel().rawType() ) )
					.withContext( PojoEventContexts.fromConstructor( constructor ) );
		}

		@Override
		public void projectionConstructor() {
			new PojoConstructorProjectionDefinition.ConstructorNode<>( mappingHelper, constructor, definition -> {
				PojoRawTypeModel<T> typeModel = constructor.typeModel();
				Class<T> instantiatedJavaClass = typeModel.typeIdentifier().javaClass();
				CompositeProjectionDefinition<?> existing =
						projectionDefinitions.putIfAbsent( instantiatedJavaClass, definition );
				log.constructorProjection( typeModel, definition );
				if ( existing != null ) {
					throw log.multipleProjectionConstructorsForType( instantiatedJavaClass );
				}
			} )
					.projectionConstructor();
		}
	}
}
