/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.impl;

import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingFactory;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingInitiatorImpl;

public class JavaBeanMappingInitiator extends PojoMappingInitiatorImpl<JavaBeanMapping> {

	private final JavaBeanTypeConfigurationContributor typeConfigurationContributor;

	public JavaBeanMappingInitiator(JavaBeanBootstrapIntrospector introspector) {
		super(
				new JavaBeanMappingFactory(),
				introspector
		);
		typeConfigurationContributor = new JavaBeanTypeConfigurationContributor( introspector );
		addConfigurationContributor( typeConfigurationContributor );
	}

	public void addEntityType(Class<?> type) {
		typeConfigurationContributor.addEntityType( type );
	}
}
