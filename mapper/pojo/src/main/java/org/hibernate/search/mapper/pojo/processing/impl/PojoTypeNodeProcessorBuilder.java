/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelRootElement;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class PojoTypeNodeProcessorBuilder<T> extends AbstractPojoNodeProcessorBuilder<T>
		implements PojoTypeNodeMappingCollector {

	private final PojoTypeModel<T> typeModel;
	private final PojoModelRootElement pojoModelRootElement;

	private final PojoTypeNodeIdentityMappingCollector identityMappingCollector;

	private final Collection<TypeBridge> bridges = new ArrayList<>();
	private final Map<PropertyHandle, PojoPropertyNodeProcessorBuilder<? super T, ?>> propertyProcessorBuilders =
			new HashMap<>();

	public PojoTypeNodeProcessorBuilder(
			AbstractPojoNodeProcessorBuilder<?> parent, PojoTypeModel<T> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		super( parent, contributorProvider, indexModelBinder, bindingContext );
		this.typeModel = typeModel;

		// FIXME do something more with the indexable model, to be able to use it in containedIn processing in particular
		this.pojoModelRootElement = new PojoModelRootElement( typeModel, contributorProvider );

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	public void bridge(BridgeBuilder<? extends TypeBridge> builder) {
		bridges.add( indexModelBinder.addTypeBridge( bindingContext, pojoModelRootElement, builder ) );
	}

	@Override
	public void routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder) {
		RoutingKeyBridge bridge = indexModelBinder.addRoutingKeyBridge( bindingContext, pojoModelRootElement, builder );
		identityMappingCollector.routingKeyBridge( bridge );
	}

	@Override
	public PojoPropertyNodeMappingCollector property(PropertyHandle propertyHandle) {
		return propertyProcessorBuilders.computeIfAbsent( propertyHandle, this::createPropertyProcessorBuilder );
	}

	private PojoPropertyNodeProcessorBuilder<? super T, ?> createPropertyProcessorBuilder(PropertyHandle propertyHandle) {
		return new PojoPropertyNodeProcessorBuilder<>(
				this, typeModel, typeModel.getProperty( propertyHandle.getName() ), propertyHandle,
				contributorProvider, indexModelBinder, bindingContext, identityMappingCollector
		);
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "type " ).append( typeModel.getJavaClass().getSimpleName() );
	}

	@Override
	public PojoTypeNodeProcessor<T> build() {
		return new PojoTypeNodeProcessor<>(
				bindingContext.getParentIndexObjectAccessors(), bridges, propertyProcessorBuilders.values()
		);
	}

}
