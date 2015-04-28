/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi.impl;

import org.hibernate.search.cfg.spi.SearchConfiguration;

/**
 * Additional SearchConfiguration-Features that are not that commonly used are in here. These methods are in this
 * separate interface to not break exisiting integrator code
 *
 * @author Martin Braun
 */
public interface ExtendedSearchConfiguration extends SearchConfiguration {

	/**
	 * @return {@code true} if the value of all
	 * {@link org.hibernate.search.annotations.IndexedEmbedded#includeEmbeddedObjectId} should be overwritten to true
	 */
	boolean isEnforceIncludeEmbeddedObjectId();

}
