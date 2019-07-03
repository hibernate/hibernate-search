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
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;

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
	@SuppressWarnings({"unchecked"})
	public void bind(ValueBindingContext<?> context) {
		BeanHolder<? extends ValueBridge> bridgeHolder = doBuild( context.getBeanResolver(), ValueBridge.class );
		try {
			doBind( bridgeHolder, context );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( bridgeHolder );
			throw e;
		}
	}

	private <B extends ValueBridge<V, F>, V, F> void doBind(BeanHolder<B> bridgeHolder, ValueBindingContext<?> context) {
		ValueBridge<V, F> bridge = bridgeHolder.get();
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		// TODO HSEARCH-3243 We're assuming the field type is raw here, maybe we should enforce it?
		@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
		Class<V> bridgeParameterType = (Class<V>) bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 0 )
				.map( ReflectionUtils::getRawType )
				.orElseThrow( () -> new AssertionFailure(
						"Could not auto-detect the input type for value bridge '"
						+ bridge + "'."
						+ " There is a bug in Hibernate Search, please report it."
				) );
		context.setBridge( bridgeParameterType, bridge );
	}

	private <T> BeanHolder<? extends T> doBuild(BeanResolver beanResolver, Class<T> expectedType) {
		return beanReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}

	private <T> BeanHolder<? extends T> doBuild(BridgeBuildContext buildContext, Class<T> expectedType) {
		return doBuild( buildContext.getBeanResolver(), expectedType );
	}
}
