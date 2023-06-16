/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoRootIdentityMappingCollector<E> implements PojoIdentityMappingCollector {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final PojoMappingHelper mappingHelper;
	private final Optional<IndexedEntityBindingContext> bindingContext;

	private final BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge;

	private IdentifierMappingImplementor<?, E> identifierMapping;
	private PojoPropertyModel<?> documentIdSourceProperty;

	public PojoRootIdentityMappingCollector(PojoRawTypeModel<E> typeModel,
			PojoMappingHelper mappingHelper,
			Optional<IndexedEntityBindingContext> bindingContext,
			BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge) {
		this.typeModel = typeModel;
		this.mappingHelper = mappingHelper;
		this.bindingContext = bindingContext;
		this.providedIdentifierBridge = providedIdentifierBridge;
	}

	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( IdentifierMappingImplementor::close, identifierMapping );
		}
	}

	public PojoIndexMappingCollectorTypeNode toMappingCollectorRootNode() {
		return new IdentityMappingCollectorTypeNode<>( BoundPojoModelPath.root( typeModel ), mappingHelper, this );
	}

	@Override
	public <T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder, Map<String, Object> params) {
		BoundIdentifierBridge<T> boundIdentifierBridge = mappingHelper.indexModelBinder()
				.bindIdentifier( bindingContext, modelPath, binder, params );
		PojoPropertyModel<T> propertyModel = modelPath.getPropertyModel();
		this.identifierMapping = new PropertyIdentifierMapping<>(
				propertyModel.typeModel().rawType().caster(),
				propertyModel.handle(),
				boundIdentifierBridge.getBridgeHolder()
		);
		this.documentIdSourceProperty = propertyModel;
	}

	public IdentifierMappingImplementor<?, E> buildAndContributeTo(
			PojoTypeExtendedMappingCollector extendedMappingCollector, IdentityMappingMode mode) {
		applyDefaults( mode );

		if ( documentIdSourceProperty != null ) {
			extendedMappingCollector.documentIdSourceProperty( documentIdSourceProperty );
		}

		extendedMappingCollector.identifierMapping( identifierMapping );

		return identifierMapping;
	}

	private void applyDefaults(IdentityMappingMode mode) {
		if ( identifierMapping != null ) {
			return;
		}

		// Assume a provided ID if requested
		if ( providedIdentifierBridge != null ) {
			BoundIdentifierBridge<Object> boundIdentifierBridge = mappingHelper.indexModelBinder()
					.bindIdentifier( bindingContext, mappingHelper.introspector().typeModel( Object.class ),
							new BeanBinder( providedIdentifierBridge ),
							Collections.emptyMap() );
			identifierMapping = ProvidedIdentifierMapping.get( boundIdentifierBridge.getBridgeHolder() );
			return;
		}

		// Fall back to the entity ID if possible
		Optional<BoundPojoModelPathPropertyNode<E, ?>> entityIdPropertyPath = mappingHelper.indexModelBinder()
				.createEntityIdPropertyPath( typeModel );
		if ( IdentityMappingMode.REQUIRED.equals( mode ) ) {
			if ( entityIdPropertyPath.isPresent() ) {
				identifierBridge( entityIdPropertyPath.get(), null, Collections.emptyMap() );
			}
			else {
				throw log.missingIdentifierMapping( typeModel );
			}
		}
		else {
			if ( entityIdPropertyPath.isPresent() ) {
				identifierMapping = unmappedIdentifier( entityIdPropertyPath.get() );
			}
			else {
				identifierMapping = new UnconfiguredIdentifierMapping<>( typeModel.typeIdentifier() );
			}
		}
	}

	private <T> IdentifierMappingImplementor<T, E> unmappedIdentifier(BoundPojoModelPathPropertyNode<?, T> modelPath) {
		PojoPropertyModel<T> propertyModel = modelPath.getPropertyModel();
		return new UnmappedPropertyIdentifierMapping<>(
				typeModel.typeIdentifier(),
				propertyModel.typeModel().rawType().caster(),
				propertyModel.handle()
		);
	}
}
