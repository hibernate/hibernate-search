/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.bridge.spi.Bridge;
import org.hibernate.search.engine.bridge.spi.FunctionBridge;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;

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

	public <A extends Annotation> Bridge<A> createBridge(BeanReference<? extends Bridge<?>> reference, A annotation) {
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

}
