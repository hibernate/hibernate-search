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
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchManagerImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;

public class JavaBeanMappingImpl extends AbstractPojoMappingImplementor<JavaBeanMapping> implements CloseableJavaBeanMapping {

	private final JavaBeanMappingContext mappingContext;

	JavaBeanMappingImpl(PojoMappingDelegate mappingDelegate) {
		super( mappingDelegate );
		this.mappingContext = new JavaBeanMappingContext();
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
		return new JavaBeanSearchManagerImpl.JavaBeanSearchManagerBuilderImpl( getDelegate(), mappingContext );
	}
}
