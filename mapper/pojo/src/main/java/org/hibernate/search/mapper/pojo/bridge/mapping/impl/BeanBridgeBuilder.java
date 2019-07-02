/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * A bridge builder that simply retrieves the bridge as a bean from the bean provider.
 */
public final class BeanBridgeBuilder
		implements TypeBridgeBuilder<Annotation>, PropertyBridgeBuilder<Annotation>,
				RoutingKeyBridgeBuilder<Annotation>,
				IdentifierBridgeBuilder, ValueBridgeBuilder {

	private final BeanReference<?> beanReference;

	public BeanBridgeBuilder(BeanReference<?> beanReference) {
		this.beanReference = beanReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + beanReference + "]";
	}

	@Override
	public void initialize(Annotation annotation) {
		throw new AssertionFailure( "This method should not be called on this object." );
	}

	@Override
	public BeanHolder<? extends TypeBridge> buildForType(BridgeBuildContext buildContext) {
		return doBuild( buildContext, TypeBridge.class );
	}

	@Override
	public BeanHolder<? extends PropertyBridge> buildForProperty(BridgeBuildContext buildContext) {
		return doBuild( buildContext, PropertyBridge.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public BeanHolder<? extends IdentifierBridge<?>> buildForIdentifier(BridgeBuildContext buildContext) {
		return doBuild( buildContext, (Class<? extends IdentifierBridge<?>>) (Class) IdentifierBridge.class );
	}

	@Override
	public BeanHolder<? extends RoutingKeyBridge> buildForRoutingKey(BridgeBuildContext buildContext) {
		return doBuild( buildContext, RoutingKeyBridge.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public BeanHolder<? extends ValueBridge<?, ?>> buildForValue(BridgeBuildContext buildContext) {
		return doBuild( buildContext, (Class<? extends ValueBridge<?, ?>>) (Class) ValueBridge.class );
	}

	private <T> BeanHolder<? extends T> doBuild(BridgeBuildContext buildContext, Class<T> expectedType) {
		BeanResolver beanResolver = buildContext.getBeanResolver();
		return beanReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}
}
