/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi;

import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Suggested base class to create custom SearchConfiguration implementations.
 * We might need to add new methods to the {@link org.hibernate.search.cfg.spi.SearchConfiguration} interface,
 * in that case we can add default implementations here to avoid breaking
 * integration code.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public abstract class SearchConfigurationBase implements SearchConfiguration {

	/**
	 * {@inheritDoc}
	 * <p>
	 * In most cases it is safest to default to {@code true}.
	 * </p>
	 */
	@Override
	public boolean isTransactionManagerExpected() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In most cases it is safest to default to {@code false}.
	 * </p>
	 */
	@Override
	public boolean isIndexMetadataComplete() {
		return false;
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return false;
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return DefaultInstanceInitializer.DEFAULT_INITIALIZER;
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return false;
	}
}
