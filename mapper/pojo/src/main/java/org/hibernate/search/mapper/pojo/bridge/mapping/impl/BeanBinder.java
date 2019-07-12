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
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;

/**
 * A binder that simply retrieves the bridge as a bean from the bean provider.
 */
public final class BeanBinder
		implements TypeBinder<Annotation>, PropertyBinder<Annotation>,
		RoutingKeyBinder<Annotation>,
		IdentifierBinder, ValueBinder {

	private final BeanReference<?> beanReference;

	public BeanBinder(BeanReference<?> beanReference) {
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
	public void bind(TypeBindingContext context) {
		BeanHolder<? extends TypeBridge> bridgeHolder = doBuild( context.getBeanResolver(), TypeBridge.class );
		try {
			context.setBridge( bridgeHolder );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( bridgeHolder );
			throw e;
		}
	}

	@Override
	public void bind(PropertyBindingContext context) {
		BeanHolder<? extends PropertyBridge> bridgeHolder = doBuild( context.getBeanResolver(), PropertyBridge.class );
		try {
			context.setBridge( bridgeHolder );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( bridgeHolder );
			throw e;
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void bind(IdentifierBindingContext<?> context) {
		BeanHolder<? extends IdentifierBridge> bridgeHolder = doBuild( context.getBeanResolver(), IdentifierBridge.class );
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

	@Override
	public void bind(RoutingKeyBindingContext context) {
		BeanHolder<? extends RoutingKeyBridge> bridgeHolder = doBuild( context.getBeanResolver(), RoutingKeyBridge.class );
		try {
			context.setBridge( bridgeHolder );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( bridgeHolder );
			throw e;
		}
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

	private <B extends IdentifierBridge<I>, I> void doBind(BeanHolder<B> bridgeHolder, IdentifierBindingContext<?> context) {
		IdentifierBridge<I> bridge = bridgeHolder.get();
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		// TODO HSEARCH-3243 We're assuming the field type is raw here, maybe we should enforce it?
		@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
		Class<I> bridgeParameterType = (Class<I>) bridgeTypeContext.resolveTypeArgument( IdentifierBridge.class, 0 )
				.map( ReflectionUtils::getRawType )
				.orElseThrow( () -> new AssertionFailure(
						"Could not auto-detect the input type for identifier bridge '"
						+ bridge + "'."
						+ " There is a bug in Hibernate Search, please report it."
				) );
		context.setBridge( bridgeParameterType, bridge );
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
}
