/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.mapper.javabean.search.JavaBeanSearchScope;
import org.hibernate.search.mapper.javabean.search.dsl.query.JavaBeanQueryResultDefinitionContext;
import org.hibernate.search.mapper.javabean.search.dsl.query.impl.JavaBeanQueryResultDefinitionContextImpl;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class JavaBeanSearchScopeImpl implements JavaBeanSearchScope {

	private final PojoSearchScopeDelegate<?, PojoReference> delegate;

	public JavaBeanSearchScopeImpl(PojoSearchScopeDelegate<?, PojoReference> delegate) {
		this.delegate = delegate;
	}

	@Override
	public JavaBeanQueryResultDefinitionContext search() {
		return new JavaBeanQueryResultDefinitionContextImpl( delegate );
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
	public SearchProjectionFactoryContext<PojoReference, PojoReference> projection() {
		return delegate.projection();
	}
}
