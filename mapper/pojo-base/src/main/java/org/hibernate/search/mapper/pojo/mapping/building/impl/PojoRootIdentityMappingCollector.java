/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.PropertyIdentifierMapping;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ProvidedIdentifierMapping;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoRootIdentityMappingCollector<E> implements PojoIdentityMappingCollector {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final PojoMappingHelper mappingHelper;
	private final IndexedEntityBindingContext bindingContext;

	private final BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge;
	private final BeanResolver beanResolver;

	IdentifierMappingImplementor<?, E> identifierMapping;
	Optional<PojoPropertyModel<?>> documentIdSourceProperty;
	BoundRoutingBridge<E> routingBridge;

	PojoRootIdentityMappingCollector(PojoRawTypeModel<E> typeModel,
			PojoMappingHelper mappingHelper,
			IndexedEntityBindingContext bindingContext,
			BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge,
			BoundRoutingBridge<E> routingBridge,
			BeanResolver beanResolver) {
		this.typeModel = typeModel;
		this.mappingHelper = mappingHelper;
		this.bindingContext = bindingContext;
		this.providedIdentifierBridge = providedIdentifierBridge;
		this.beanResolver = beanResolver;
		this.routingBridge = routingBridge;
	}

	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
			closer.push( RoutingBridge::close, routingBridge, BoundRoutingBridge::getBridge );
			closer.push( BeanHolder::close, routingBridge, BoundRoutingBridge::getBridgeHolder );
		}
	}

	@Override
	public <T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder) {
		BoundIdentifierBridge<T> boundIdentifierBridge = mappingHelper.indexModelBinder()
				.bindIdentifier( bindingContext, modelPath, binder );
		PojoPropertyModel<T> propertyModel = modelPath.getPropertyModel();
		this.identifierMapping = new PropertyIdentifierMapping<>(
				propertyModel.typeModel().rawType().caster(),
				propertyModel.handle(),
				boundIdentifierBridge.getBridgeHolder()
		);
		this.documentIdSourceProperty = Optional.of( propertyModel );
	}

	void applyDefaults() {
		if ( identifierMapping != null ) {
			return;
		}

		// Assume a provided ID if requested
		if ( providedIdentifierBridge != null ) {
			identifierMapping = ProvidedIdentifierMapping.get( beanResolver.resolve( providedIdentifierBridge ) );
			documentIdSourceProperty = Optional.empty();
			return;
		}

		// Fall back to the entity ID if possible
		Optional<BoundPojoModelPathPropertyNode<E, ?>> entityIdPropertyPath = mappingHelper.indexModelBinder()
				.createEntityIdPropertyPath( typeModel );
		if ( entityIdPropertyPath.isPresent() ) {
			identifierBridge( entityIdPropertyPath.get(), null );
			return;
		}

		throw log.missingIdentifierMapping( typeModel );
	}

}
