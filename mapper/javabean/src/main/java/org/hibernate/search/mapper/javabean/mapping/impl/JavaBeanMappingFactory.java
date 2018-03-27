/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingFactory;

public final class JavaBeanMappingFactory implements PojoMappingFactory<JavaBeanMapping> {
	@Override
	public MappingImplementor<JavaBeanMapping> createMapping(ConfigurationPropertySource propertySource,
			PojoMappingDelegate mappingDelegate) {
		return new JavaBeanMappingImpl( mappingDelegate );
	}
}
