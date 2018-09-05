/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.engine.impl.SimpleInitializer;

/**
 * Provides SPI level access to the default {@code InstanceInitializer} singleton.
 *
 * @since 5.0
 */
public final class DefaultInstanceInitializer {

	public static final InstanceInitializer DEFAULT_INITIALIZER = SimpleInitializer.INSTANCE;

	private DefaultInstanceInitializer() {
		// not meant to be initialized
	}


}
