/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.impl;

import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.search.engine.service.named.spi.NamedResolver;

/**
 * Implementation of NamedResolver to delegate to Hibernate ORM's
 * JNDI lookup service  {@literal org.hibernate.engine.jndi.spi.JndiService}.
 */
final class DelegatingNamedResolver implements NamedResolver {

	private final JndiService namingService;

	public DelegatingNamedResolver(JndiService namingService) {
		this.namingService = namingService;
	}

	@Override
	public Object locate(String jndiName) {
		return namingService.locate( jndiName );
	}

}
