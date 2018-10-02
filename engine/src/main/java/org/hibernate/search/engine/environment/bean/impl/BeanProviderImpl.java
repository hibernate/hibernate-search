/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.StringHelper;

public class BeanProviderImpl implements BeanProvider {

	private final BeanResolver beanResolver;

	public BeanProviderImpl(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
	}

	@Override
	public <T> T getBean(Class<?> typeReference, Class<T> expectedClass) {
		if ( typeReference == null ) {
			throw new SearchException( "Got an empty bean reference (type is null)" );
		}
		return beanResolver.resolve( typeReference, expectedClass );
	}

	@Override
	public <T> T getBean(String nameReference, Class<T> expectedClass) {
		if ( StringHelper.isEmpty( nameReference ) ) {
			throw new SearchException( "Got an empty bean reference (name is null or empty)" );
		}
		return beanResolver.resolve( nameReference, expectedClass );
	}

	@Override
	public <T> T getBean(BeanReference reference, Class<T> expectedClass) {
		if ( reference == null ) {
			throw new SearchException( "Got an empty bean reference (reference is null)" );
		}

		String nameReference = reference.getName();
		Class<?> typeReference = reference.getType();
		boolean nameProvided = StringHelper.isNotEmpty( nameReference );
		boolean typeProvided = typeReference != null;

		if ( nameProvided && typeProvided ) {
			return beanResolver.resolve( nameReference, typeReference, expectedClass );
		}
		else if ( nameProvided ) {
			return beanResolver.resolve( nameReference, expectedClass );
		}
		else if ( typeProvided ) {
			return beanResolver.resolve( typeReference, expectedClass );
		}
		else {
			throw new SearchException( "Got an empty bean reference (no name, no type)" );
		}
	}
}
