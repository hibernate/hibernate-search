/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.identifiertovalue.impl.IdentifierBinderToValueBinderAdapter;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIndexedEmbeddedIdentityMappingCollector<E> implements PojoIdentityMappingCollector {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final PojoMappingHelper mappingHelper;

	private BoundPojoModelPathPropertyNode<?, ?> identifierModelPath;
	private IdentifierBinder identifierBinder;

	PojoIndexedEmbeddedIdentityMappingCollector(PojoRawTypeModel<E> typeModel, PojoMappingHelper mappingHelper) {
		this.typeModel = typeModel;
		this.mappingHelper = mappingHelper;
	}

	@Override
	public <T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder) {
		this.identifierModelPath = modelPath;
		this.identifierBinder = binder;
	}

	public void contributeIdentifierField(AbstractPojoIndexingProcessorTypeNodeBuilder<?, ?> embeddedTypeNodeBuilder) {
		if ( identifierModelPath == null ) {
			// Fall back to the entity ID if possible
			Optional<BoundPojoModelPathPropertyNode<E, ?>> entityIdPropertyPath = mappingHelper.indexModelBinder()
					.createEntityIdPropertyPath( typeModel );
			if ( entityIdPropertyPath.isPresent() ) {
				identifierBridge( entityIdPropertyPath.get(), null );
			}
			else {
				throw log.missingIdentifierMapping( typeModel );
			}
		}

		embeddedTypeNodeBuilder.property( identifierModelPath.getPropertyModel().name() )
				.value( ContainerExtractorPath.defaultExtractors() )
				.valueBinder( identifierBinder == null ? null : new IdentifierBinderToValueBinderAdapter( identifierBinder ),
						null,
						context -> context.standardTypeOptionsStep().searchable( Searchable.YES )
								.projectable( Projectable.YES ) );
	}
}
