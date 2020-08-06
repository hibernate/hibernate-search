/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoRoutingIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoRoutingIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class RoutingBindingContextImpl<E> extends AbstractCompositeBindingContext
		implements RoutingBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;

	private final PojoRawTypeModel<E> indexedEntityType;
	private final PojoModelTypeRootElement<E> pojoModelTypeRootElement;
	private final PojoRoutingIndexingDependencyConfigurationContextImpl<E> dependencyContext;

	private BeanHolder<? extends RoutingBridge<? super E>> routingBridgeHolder;

	public RoutingBindingContextImpl(BeanResolver beanResolver, PojoBootstrapIntrospector introspector,
			PojoRawTypeModel<E> indexedEntityType, PojoModelTypeRootElement<E> pojoModelTypeRootElement,
			PojoRoutingIndexingDependencyConfigurationContextImpl<E> dependencyContext) {
		super( beanResolver );
		this.introspector = introspector;
		this.indexedEntityType = indexedEntityType;
		this.pojoModelTypeRootElement = pojoModelTypeRootElement;
		this.dependencyContext = dependencyContext;
	}

	@Override
	public <E2> void bridge(Class<E2> expectedType, RoutingBridge<E2> bridge) {
		bridge( expectedType, BeanHolder.of( bridge ) );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <E2> void bridge(Class<E2> expectedType, BeanHolder<? extends RoutingBridge<E2>> bridgeHolder) {
		PojoRawTypeModel<E2> expectedTypeModel = introspector.typeModel( expectedType );
		if ( !indexedEntityType.isSubTypeOf( expectedTypeModel ) ) {
			throw log.invalidInputTypeForRoutingBridge( bridgeHolder.get(), indexedEntityType );
		}
		routingBridgeHolder = (BeanHolder<? extends RoutingBridge<? super E>>) bridgeHolder;
	}

	@Override
	public PojoModelType bridgedElement() {
		return pojoModelTypeRootElement;
	}

	@Override
	public PojoRoutingIndexingDependencyConfigurationContext dependencies() {
		return dependencyContext;
	}

	public BoundRoutingBridge<E> applyBinder(RoutingBinder binder) {
		binder.bind( this );

		if ( routingBridgeHolder == null ) {
			throw log.missingBridgeForBinder( binder );
		}

		checkBridgeDependencies( pojoModelTypeRootElement, dependencyContext );

		return new BoundRoutingBridge<>( routingBridgeHolder, pojoModelTypeRootElement, dependencyContext );
	}
}
