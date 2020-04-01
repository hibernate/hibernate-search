/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.util.Collections;
import java.util.Map;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.FilterBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;
import org.hibernate.search.mapper.pojo.bridge.binding.FilterBindingContext;

/**
 * A binder that simply retrieves the bridge as a bean from the bean provider.
 */
public final class BeanBinder
	implements IdentifierBinder, ValueBinder, FilterBinder {

	private final BeanReference<?> beanReference;
	private Map<String, Object> params;

	public BeanBinder(BeanReference<?> beanReference) {
		this( beanReference, Collections.emptyMap() );
	}

	public BeanBinder(BeanReference<?> beanReference, Map<String, Object> params) {
		this.beanReference = beanReference;
		this.params = params;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + beanReference + "]";
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

	@Override
	public void bind(FilterBindingContext<? extends FilterFactory> context) {
		BeanHolder<? extends FilterFactory> bridgeHolder = doBuild( context.getBeanResolver(), FilterFactory.class );
		try {
			doBind( bridgeHolder, context );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
				.push( bridgeHolder );
			throw e;
		}
	}

	private <B extends IdentifierBridge<I>, I> void doBind(BeanHolder<B> bridgeHolder, IdentifierBindingContext<?> context) {
		IdentifierBridge<I> bridge = bridgeHolder.get();
		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		// TODO HSEARCH-3243 We're assuming the field type is raw here, maybe we should enforce it?
		@SuppressWarnings("unchecked") // We ensure this cast is safe through reflection
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
		@SuppressWarnings("unchecked") // We ensure this cast is safe through reflection
		Class<V> bridgeParameterType = (Class<V>) bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 0 )
			.map( ReflectionUtils::getRawType )
			.orElseThrow( () -> new AssertionFailure(
			"Could not auto-detect the input type for value bridge '"
			+ bridge + "'."
			+ " There is a bug in Hibernate Search, please report it."
		) );
		context.setBridge( bridgeParameterType, bridge );
	}

	private <T extends FilterFactory> void doBind(BeanHolder<T> bridgeHolder, FilterBindingContext context) {
		T factory = bridgeHolder.get();
		context.setFactory( factory, params );
	}

	private <T> BeanHolder<? extends T> doBuild(BeanResolver beanResolver, Class<T> expectedType) {
		return beanReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}
}
