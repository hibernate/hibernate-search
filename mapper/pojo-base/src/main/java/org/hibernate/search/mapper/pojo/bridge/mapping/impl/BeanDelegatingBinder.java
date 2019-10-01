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
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

/**
 * A binder that upon building retrieves a delegate binder from the bean provider,
 * then delegates to that binder.
 */
public final class BeanDelegatingBinder
		implements IdentifierBinder, ValueBinder {

	private final BeanReference<?> delegateReference;

	public BeanDelegatingBinder(BeanReference<?> delegateReference) {
		this.delegateReference = delegateReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + "]";
	}

	@Override
	public void bind(IdentifierBindingContext<?> context) {
		try ( BeanHolder<? extends IdentifierBinder> delegateHolder =
				createDelegate( context.getBeanResolver(), IdentifierBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	@Override
	public void bind(ValueBindingContext<?> context) {
		try ( BeanHolder<? extends ValueBinder> delegateHolder =
				createDelegate( context.getBeanResolver(), ValueBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	private <B> BeanHolder<? extends B> createDelegate(BeanResolver beanResolver, Class<B> expectedType) {
		return delegateReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}

}
