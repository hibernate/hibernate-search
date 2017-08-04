/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.bridge.declaration.spi.BridgeBeanReference;
import org.hibernate.search.engine.bridge.declaration.spi.BridgeMapping;
import org.hibernate.search.engine.bridge.mapping.BridgeDefinition;
import org.hibernate.search.engine.bridge.spi.Bridge;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public final class BridgeFactory {

	private final BuildContext buildContext;
	private final BeanResolver beanResolver;

	public BridgeFactory(BuildContext buildContext, BeanResolver beanResolver) {
		this.buildContext = buildContext;
		this.beanResolver = beanResolver;
	}

	public IdentifierBridge<?> createIdentifierBridge(BeanReference<? extends IdentifierBridge<?>> reference) {
		return beanResolver.resolve( reference, IdentifierBridge.class );
	}

	public <A extends Annotation> Bridge<A> createBridge(BridgeDefinition<A> definition) {
		A annotation = definition.get();
		Class<?> annotationType = annotation.annotationType();
		// TODO add a cache for annotation => metaAnnotation?
		BridgeMapping metaAnnotation = annotationType.getAnnotation( BridgeMapping.class );
		if ( metaAnnotation == null ) {
			throw new SearchException( "A '" + annotationType + "' annotation was passed as a bridge definition,"
					+ " but this annotation type is missing the '" + BridgeMapping.class + "' meta-annotation." );
		}

		BridgeBeanReferenceWrapper reference = new BridgeBeanReferenceWrapper( metaAnnotation.implementation() );

		// TODO check that the implementation accepts annotations of type A
		Bridge<A> bridge = beanResolver.resolve( reference, Bridge.class );

		bridge.initialize( buildContext, annotation );

		return bridge;
	}

	public FunctionBridge<?, ?> createFunctionBridge(BeanReference<? extends FunctionBridge<?, ?>> reference) {
		FunctionBridge<?, ?> bridge = beanResolver.resolve( reference, FunctionBridge.class );
		bridge.initialize( buildContext );
		return bridge;
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
