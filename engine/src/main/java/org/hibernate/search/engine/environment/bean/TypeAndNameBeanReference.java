/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

final class TypeAndNameBeanReference implements BeanReference {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static BeanReference createLenient(Class<?> type, String name) {
		boolean nameProvided = StringHelper.isNotEmpty( name );
		boolean typeProvided = type != null;

		if ( nameProvided && typeProvided ) {
			return new TypeAndNameBeanReference( type, name );
		}
		else if ( nameProvided ) {
			return new NameBeanReference( name );
		}
		else if ( typeProvided ) {
			return new TypeBeanReference( type );
		}
		else {
			throw log.invalidBeanReferenceTypeIsNullAndNameNullOrEmpty();
		}
	}

	private final Class<?> type;
	private final String name;

	private TypeAndNameBeanReference(Class<?> type, String name) {
		this.type = type;
		this.name = name;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[type=" + type + ", name=" + name + "]";
	}

	@Override
	public <T> T getBean(BeanProvider beanProvider, Class<T> expectedType) {
		return beanProvider.getBean( expectedType, type, name );
	}

}
