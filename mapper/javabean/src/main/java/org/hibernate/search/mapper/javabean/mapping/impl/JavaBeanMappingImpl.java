/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanBackendMappingContext;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSession;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;

public class JavaBeanMappingImpl extends AbstractPojoMappingImplementor<SearchMapping> implements
		CloseableSearchMapping {

	private final JavaBeanBackendMappingContext backendMappingContext;
	private final JavaBeanTypeContextContainer typeContextContainer;

	JavaBeanMappingImpl(PojoMappingDelegate mappingDelegate, JavaBeanTypeContextContainer typeContextContainer) {
		super( mappingDelegate );
		this.backendMappingContext = new JavaBeanBackendMappingContext();
		this.typeContextContainer = typeContextContainer;
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

	private SearchSessionBuilder createSearchManagerBuilder() {
		return new JavaBeanSearchSession.JavaBeanSearchSessionBuilder(
				getDelegate(), backendMappingContext, typeContextContainer
		);
	}
}
