/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping;

import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMapperImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMapperImpl;


/**
 * @author Yoann Rodiere
 */
public final class JavaBeanMapper extends PojoMapperImpl {

	private static final JavaBeanMapper INSTANCE = new JavaBeanMapper();

	public static JavaBeanMapper get() {
		return INSTANCE;
	}

	private JavaBeanMapper() {
		super( JavaBeanMapperImplementor.get() );
	}

}
