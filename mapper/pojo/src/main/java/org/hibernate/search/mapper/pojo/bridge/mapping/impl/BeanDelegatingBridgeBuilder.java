/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;

/**
 * A bridge builder that upon building retrieves a delegate bridge builder from the bean provider,
 * then delegates to that bridge builder.
 */
public final class BeanDelegatingBridgeBuilder
		implements IdentifierBridgeBuilder, ValueBridgeBuilder {

	private final BeanReference<?> delegateReference;

	public BeanDelegatingBridgeBuilder(BeanReference<?> delegateReference) {
		this.delegateReference = delegateReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + "]";
	}

	@Override
	public BeanHolder<? extends IdentifierBridge<?>> buildForIdentifier(BridgeBuildContext buildContext) {
		try ( BeanHolder<? extends IdentifierBridgeBuilder> delegateHolder =
				createDelegate( buildContext, IdentifierBridgeBuilder.class ) ) {
			return delegateHolder.get().buildForIdentifier( buildContext );
		}
	}

	@Override
	public void bind(ValueBindingContext<?> context) {
		try ( BeanHolder<? extends ValueBridgeBuilder> delegateHolder =
				createDelegate( context.getBeanResolver(), ValueBridgeBuilder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	private <B> BeanHolder<? extends B> createDelegate(BeanResolver beanResolver, Class<B> expectedType) {
		return delegateReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}

	private <B> BeanHolder<? extends B> createDelegate(BridgeBuildContext buildContext, Class<B> expectedType) {
		return createDelegate( buildContext.getBeanResolver(), expectedType );
	}

}
