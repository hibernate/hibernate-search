/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.mapper.javabean.search.JavaBeanSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class JavaBeanSearchTargetImpl implements JavaBeanSearchTarget {

	private final PojoSearchTargetDelegate<?> delegate;

	public JavaBeanSearchTargetImpl(PojoSearchTargetDelegate<?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryResultDefinitionContext<PojoReference, PojoReference> query() {
		return delegate.query( ObjectLoader.identity() );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortContainerContext sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactoryContext projection() {
		return delegate.projection();
	}
}
