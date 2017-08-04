/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.model.impl.JavaBeanIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;


/**
 * @author Yoann Rodiere
 */
public final class JavaBeanMapperImplementor extends PojoMapperImplementor {

	private static final JavaBeanMapperImplementor INSTANCE = new JavaBeanMapperImplementor();

	private JavaBeanMapperImplementor() {
		super( JavaBeanIntrospector.get(), PojoProxyIntrospector.noProxy(), false );
	}

	public static JavaBeanMapperImplementor get() {
		return INSTANCE;
	}

}
