/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanBackendMappingContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSessionMappingContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class JavaBeanMapping extends AbstractPojoMappingImplementor<SearchMapping>
		implements CloseableSearchMapping, JavaBeanSearchSessionMappingContext {

	private final JavaBeanBackendMappingContext backendMappingContext;
	private final JavaBeanTypeContextContainer typeContextContainer;

	JavaBeanMapping(PojoMappingDelegate mappingDelegate, JavaBeanTypeContextContainer typeContextContainer) {
		super( mappingDelegate );
		this.backendMappingContext = new JavaBeanBackendMappingContext();
		this.typeContextContainer = typeContextContainer;
	}

	@Override
	public SearchScope scope(Collection<? extends Class<?>> targetedTypes) {
		return createScope( targetedTypes );
	}

	@Override
	public SearchMapping toConcreteType() {
		return this;
	}

	@Override
	public SearchSession createSession() {
		return createSearchManagerBuilder().build();
	}

	@Override
	public SearchSessionBuilder createSessionWithOptions() {
		return createSearchManagerBuilder();
	}

	@Override
	public JavaBeanBackendMappingContext getBackendMappingContext() {
		return backendMappingContext;
	}

	@Override
	public SearchScopeImpl createScope(Collection<? extends Class<?>> classes) {
		List<PojoRawTypeIdentifier<?>> typeIdentifiers = new ArrayList<>( classes.size() );
		for ( Class<?> clazz : classes ) {
			typeIdentifiers.add( PojoRawTypeIdentifier.of( clazz ) );
		}

		return new SearchScopeImpl(
				getDelegate().createPojoScope(
						backendMappingContext,
						typeIdentifiers,
						// We don't load anything, so we don't need any additional type context
						ignored -> null
				)
		);
	}

	private SearchSessionBuilder createSearchManagerBuilder() {
		return new JavaBeanSearchSession.JavaBeanSearchSessionBuilder(
				getDelegate(), this, typeContextContainer
		);
	}
}
