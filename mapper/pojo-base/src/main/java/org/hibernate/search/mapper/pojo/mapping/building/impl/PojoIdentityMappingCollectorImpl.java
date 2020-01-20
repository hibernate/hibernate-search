/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.PropertyIdentifierMapping;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ProvidedIdentifierMapping;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.RoutingKeyBridgeRoutingKeyProvider;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.RoutingKeyProvider;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoEntityTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoIdentityMappingCollectorImpl<E> implements PojoIdentityMappingCollector {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final PojoMappingHelper mappingHelper;
	private final IndexedEntityBindingContext bindingContext;

	private final BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge;
	private final BeanResolver beanResolver;
	private final BoundPojoModelPathPropertyNode<?, ?> entityIdPropertyPath;

	IdentifierMappingImplementor<?, E> identifierMapping;
	Optional<PojoPropertyModel<?>> documentIdSourceProperty;
	RoutingKeyProvider<E> routingKeyProvider;

	PojoIdentityMappingCollectorImpl(PojoRawTypeModel<E> typeModel,
			PojoEntityTypeAdditionalMetadata entityTypeMetadata,
			PojoMappingHelper mappingHelper,
			IndexedEntityBindingContext bindingContext,
			BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge,
			BeanResolver beanResolver) {
		this.typeModel = typeModel;
		this.mappingHelper = mappingHelper;
		this.bindingContext = bindingContext;
		this.providedIdentifierBridge = providedIdentifierBridge;
		this.beanResolver = beanResolver;

		Optional<String> entityIdPropertyName = entityTypeMetadata.getEntityIdPropertyName();
		if ( entityIdPropertyName.isPresent() ) {
			this.entityIdPropertyPath = BoundPojoModelPath.root( typeModel ).property( entityIdPropertyName.get() );
		}
		else {
			this.entityIdPropertyPath = null;
		}
	}

	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( RoutingKeyProvider::close, routingKeyProvider );
		}
	}

	@Override
	public <T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder) {
		BoundIdentifierBridge<T> boundIdentifierBridge = mappingHelper.getIndexModelBinder()
				.bindIdentifier( bindingContext, modelPath, binder );
		PojoPropertyModel<T> propertyModel = modelPath.getPropertyModel();
		this.identifierMapping = new PropertyIdentifierMapping<>(
				propertyModel.getTypeModel().getRawType().getCaster(),
				propertyModel.getHandle(),
				boundIdentifierBridge.getBridgeHolder()
		);
		this.documentIdSourceProperty = Optional.of( propertyModel );
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> routingKeyBridge(BoundPojoModelPathTypeNode<T> modelPath,
			RoutingKeyBinder binder) {
		BoundRoutingKeyBridge<T> boundRoutingKeyBridge = mappingHelper.getIndexModelBinder()
				.bindRoutingKey( bindingContext, modelPath, binder );
		this.routingKeyProvider = new RoutingKeyBridgeRoutingKeyProvider<>( boundRoutingKeyBridge.getBridgeHolder() );
		return boundRoutingKeyBridge;
	}

	void applyDefaults() {
		if ( identifierMapping == null ) {
			// Assume a provided ID if requested
			if ( providedIdentifierBridge != null ) {
				identifierMapping = ProvidedIdentifierMapping.get( beanResolver.resolve( providedIdentifierBridge ) );
				documentIdSourceProperty = Optional.empty();
			}
			// Fall back to the entity ID if possible
			else if ( entityIdPropertyPath != null ) {
				identifierBridge( entityIdPropertyPath, null );
			}
			else {
				throw log.missingIdentifierMapping( typeModel );
			}
		}

		if ( routingKeyProvider == null ) {
			routingKeyProvider = RoutingKeyProvider.alwaysNull();
		}
	}

}
