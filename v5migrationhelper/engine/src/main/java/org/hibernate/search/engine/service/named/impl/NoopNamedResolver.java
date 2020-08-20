/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.service.named.impl;

import org.hibernate.search.engine.service.named.spi.NamedResolver;
import org.hibernate.search.exception.SearchException;

/**
 * This implementation of NamedResolver is not functional:
 * integrator code is expected to provide a custom implementation
 * to replace this.
 * This implementation is provided to generate an appropriate
 * error when no replacement is provided.
 */
public class NoopNamedResolver implements NamedResolver {

	@Override
	public Object locate(String jndiName) {
		throw new SearchException( "The internal NamedResolver service was not overridden: not possible to lookup components by name. Provide a NamedResolver Service implementation during bootstrap." );
	}

}
