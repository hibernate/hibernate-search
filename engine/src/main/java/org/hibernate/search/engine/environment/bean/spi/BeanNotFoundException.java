/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.spi;


import org.hibernate.search.util.common.SearchException;

/**
 * Indicates a bean was not found by a {@link BeanProvider}.
 */
public class BeanNotFoundException extends SearchException {
	public BeanNotFoundException(String message) {
		super( message );
	}

	public BeanNotFoundException(String message, Throwable cause) {
		super( message, cause );
	}
}


