/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.CloseableJavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManagerBuilder;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContextImpl;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingImplementor;

public class JavaBeanMappingImpl extends PojoMappingImplementor<JavaBeanMapping> implements CloseableJavaBeanMapping {

	private final JavaBeanMappingContextImpl mappingContext;

	JavaBeanMappingImpl(PojoMappingDelegate mappingDelegate) {
		super( mappingDelegate );
		this.mappingContext = new JavaBeanMappingContextImpl();
	}

	@Override
	public JavaBeanMapping toAPI() {
		return this;
	}

	@Override
	public JavaBeanSearchManager createSearchManager() {
		return createSearchManagerBuilder().build();
	}

	@Override
	public JavaBeanSearchManagerBuilder createSearchManagerWithOptions() {
		return createSearchManagerBuilder();
	}

	private JavaBeanSearchManagerBuilder createSearchManagerBuilder() {
		return new JavaBeanSearchManagerImpl.Builder( getDelegate(), mappingContext );
	}
}
