/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.impl;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.bridge.builtin.impl.DefaultIntegerFunctionBridge;
import org.hibernate.search.engine.bridge.builtin.impl.DefaultIntegerIdentifierBridge;
import org.hibernate.search.engine.bridge.builtin.impl.DefaultLocalDateFunctionBridge;
import org.hibernate.search.engine.bridge.builtin.impl.DefaultStringFunctionBridge;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeBeanReference;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeMapping;
import org.hibernate.search.engine.bridge.spi.Bridge;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public final class BridgeReferenceResolver {

	private static final Log log = LoggerFactory.make( Log.class );

	private final Map<Class<?>, BeanReference<? extends IdentifierBridge<?>>> defaultIdentifierBridgeBySourceType = new HashMap<>();
	private final Map<Class<?>, BeanReference<? extends FunctionBridge<?, ?>>> defaultFunctionBridgeBySourceType = new HashMap<>();

	public BridgeReferenceResolver() {
		// TODO add and using point to override these maps, or at least to add defaults for other types
		// TODO add defaults for other types (byte, char, Characeter, Double, double, boolean, Boolean, ...)

		defaultIdentifierBridgeBySourceType.put( Integer.class, new ImmutableBeanReference<>( DefaultIntegerIdentifierBridge.class ) );

		defaultFunctionBridgeBySourceType.put( Integer.class, new ImmutableBeanReference<>( DefaultIntegerFunctionBridge.class ) );
		defaultFunctionBridgeBySourceType.put( String.class, new ImmutableBeanReference<>( DefaultStringFunctionBridge.class ) );
		defaultFunctionBridgeBySourceType.put( LocalDate.class, new ImmutableBeanReference<>( DefaultLocalDateFunctionBridge.class ) );
	}

	public <T> BeanReference<? extends IdentifierBridge<T>> resolveIdentifierBridgeForType(Class<T> sourceType) {
		@SuppressWarnings("unchecked")
		BeanReference<? extends IdentifierBridge<T>> result =
				(BeanReference<? extends IdentifierBridge<T>>) defaultIdentifierBridgeBySourceType.get( sourceType );
		if ( result == null ) {
			throw log.unableToResolveDefaultIdentifierBridgeFromSourceType( sourceType );
		}
		return result;
	}

	public <T> BeanReference<? extends FunctionBridge<? super T, ?>> resolveFunctionBridgeForType(Class<T> sourceType) {
		@SuppressWarnings("unchecked")
		BeanReference<? extends FunctionBridge<? super T, ?>> result =
				(BeanReference<? extends FunctionBridge<? super T, ?>>) defaultFunctionBridgeBySourceType.get( sourceType );
		if ( result == null ) {
			throw log.unableToResolveDefaultIdentifierBridgeFromSourceType( sourceType );
		}
		return result;
	}

	public <A extends Annotation> BeanReference<? extends Bridge<?>> resolveBridgeForAnnotationType(Class<A> annotationType) {
		// TODO add a cache for annotation => metaAnnotation?
		BridgeMapping metaAnnotation = annotationType.getAnnotation( BridgeMapping.class );
		if ( metaAnnotation == null ) {
			throw log.unableToResolveBridgeFromAnnotationType( annotationType, BridgeMapping.class );
		}

		BridgeBeanReferenceWrapper reference = new BridgeBeanReferenceWrapper( metaAnnotation.implementation() );
		return reference;
	}

	private static class BridgeBeanReferenceWrapper implements BeanReference<Bridge<?>> {
		private final BridgeBeanReference delegate;

		public BridgeBeanReferenceWrapper(BridgeBeanReference delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getName() {
			return delegate.name();
		}

		@Override
		public Class<? extends Bridge<?>> getType() {
			return delegate.type();
		}

	}

}
