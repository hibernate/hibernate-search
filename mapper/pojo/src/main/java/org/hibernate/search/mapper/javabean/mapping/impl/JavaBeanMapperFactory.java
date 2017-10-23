/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.model.impl.JavaBeanIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;


/**
 * @author Yoann Rodiere
 */
public final class JavaBeanMapperFactory extends PojoMapperFactory<JavaBeanMappingImpl> {

	private static final JavaBeanMapperFactory INSTANCE = new JavaBeanMapperFactory();

	private JavaBeanMapperFactory() {
		super( JavaBeanIntrospector.get(), false );
	}

	public static JavaBeanMapperFactory get() {
		return INSTANCE;
	}

	@Override
	protected JavaBeanMappingImpl createMapping(PojoMappingDelegate mappingDelegate) {
		return new JavaBeanMappingImpl( mappingDelegate );
	}
}
