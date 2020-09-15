/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;

/**
 * A binder that simply retrieves the bridge as a bean from the bean provider.
 */
public final class BeanBinder
		implements IdentifierBinder, ValueBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanReference<?> beanReference;

	public BeanBinder(BeanReference<?> beanReference) {
		this.beanReference = beanReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + beanReference + "]";
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void bind(IdentifierBindingContext<?> context) {
		BeanHolder<? extends IdentifierBridge> bridgeHolder = doBuild( context.beanResolver(), IdentifierBridge.class );
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
	@SuppressWarnings({"unchecked"})
	public void bind(ValueBindingContext<?> context) {
		BeanHolder<? extends ValueBridge> bridgeHolder = doBuild( context.beanResolver(), ValueBridge.class );
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
		@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
		Class<I> bridgeParameterType = bridgeTypeContext.resolveTypeArgument( IdentifierBridge.class, 0 )
				.map( type -> {
					if ( type instanceof Class ) {
						return (Class<I>) type;
					}
					else {
						throw log.invalidGenericParameterToInferIdentifierType( bridge, type );
					}
				} )
				.orElseThrow( () -> new AssertionFailure(
						"Could not auto-detect the input type for identifier bridge '"
						+ bridge + "'."
						+ " There is a bug in Hibernate Search, please report it."
				) );
		context.bridge( bridgeParameterType, bridge );
	}

	private <B extends ValueBridge<V, F>, V, F> void doBind(BeanHolder<B> bridgeHolder, ValueBindingContext<?> context) {
		ValueBridge<V, F> bridge = bridgeHolder.get();
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
		Class<V> bridgeParameterType = bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 0 )
				.map( type -> {
					if ( type instanceof Class ) {
						return (Class<V>) type;
					}
					else {
						throw log.invalidGenericParameterToInferValueType( bridge, type );
					}
				} )
				.orElseThrow( () -> new AssertionFailure(
						"Could not auto-detect the input type for value bridge '"
						+ bridge + "'."
						+ " There is a bug in Hibernate Search, please report it."
				) );
		context.bridge( bridgeParameterType, bridge );
	}

	private <T> BeanHolder<? extends T> doBuild(BeanResolver beanResolver, Class<T> expectedType) {
		return beanReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}
}
