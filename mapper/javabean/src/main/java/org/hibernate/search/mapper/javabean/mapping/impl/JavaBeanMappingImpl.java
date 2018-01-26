/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingImpl;

public class JavaBeanMappingImpl extends PojoMappingImpl implements JavaBeanMapping {

	JavaBeanMappingImpl(PojoMappingDelegate mappingDelegate) {
		super( mappingDelegate );
	}

	@Override
	public PojoSearchManager createSearchManager() {
		return createSearchManagerBuilder().build();
	}

	@Override
	public JavaBeanSearchManagerBuilder createSearchManagerWithOptions() {
		return createSearchManagerBuilder();
	}

	private JavaBeanSearchManagerBuilder createSearchManagerBuilder() {
		return new JavaBeanSearchManagerImpl.Builder( getDelegate() );
	}
}
