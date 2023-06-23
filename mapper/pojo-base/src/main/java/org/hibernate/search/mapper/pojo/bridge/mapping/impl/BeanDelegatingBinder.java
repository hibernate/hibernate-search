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
import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

/**
 * A binder that upon building retrieves a delegate binder from the bean provider,
 * then delegates to that binder.
 */
public final class BeanDelegatingBinder
		implements TypeBinder, PropertyBinder, RoutingBinder,
		MarkerBinder, IdentifierBinder, ValueBinder {

	private final BeanReference<?> delegateReference;

	public BeanDelegatingBinder(BeanReference<?> delegateReference) {
		this.delegateReference = delegateReference;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[delegateReference=" + delegateReference + "]";
	}

	@Override
	public void bind(TypeBindingContext context) {
		try ( BeanHolder<? extends TypeBinder> delegateHolder =
				createDelegate( context.beanResolver(), TypeBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	@Override
	public void bind(PropertyBindingContext context) {
		try ( BeanHolder<? extends PropertyBinder> delegateHolder =
				createDelegate( context.beanResolver(), PropertyBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	@Override
	public void bind(RoutingBindingContext context) {
		try ( BeanHolder<? extends RoutingBinder> delegateHolder =
				createDelegate( context.beanResolver(), RoutingBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	@Override
	public void bind(MarkerBindingContext context) {
		try ( BeanHolder<? extends MarkerBinder> delegateHolder =
				createDelegate( context.beanResolver(), MarkerBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	@Override
	public void bind(IdentifierBindingContext<?> context) {
		try ( BeanHolder<? extends IdentifierBinder> delegateHolder =
				createDelegate( context.beanResolver(), IdentifierBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	@Override
	public void bind(ValueBindingContext<?> context) {
		try ( BeanHolder<? extends ValueBinder> delegateHolder =
				createDelegate( context.beanResolver(), ValueBinder.class ) ) {
			delegateHolder.get().bind( context );
		}
	}

	private <B> BeanHolder<? extends B> createDelegate(BeanResolver beanResolver, Class<B> expectedType) {
		return delegateReference.asSubTypeOf( expectedType ).resolve( beanResolver );
	}

}
