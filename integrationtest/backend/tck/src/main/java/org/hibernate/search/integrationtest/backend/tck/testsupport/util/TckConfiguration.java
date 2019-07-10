/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Allows running the tests with different backend technologies,
 * relying on a {@link TckBackendHelper} to adapt to the particulars of each technology.
 */
public final class TckConfiguration {

	private static TckConfiguration instance;

	public static TckConfiguration get() {
		if ( instance == null ) {
			instance = new TckConfiguration();
		}
		return instance;
	}

	private final TckBackendHelper helper;

	private TckConfiguration() {
		Iterator<TckBackendHelper> featuresIterator = ServiceLoader.load( TckBackendHelper.class ).iterator();
		if ( !featuresIterator.hasNext() ) {
			throw new IllegalStateException( "No implementation for TckBackendHelper service in classpath" );
		}

		this.helper = featuresIterator.next();
		if ( featuresIterator.hasNext() ) {
			throw new IllegalStateException( "Multiple implementations for TckBackendHelper service in classpath" );
		}
	}

	public TckBackendHelper getBackendHelper() {
		return helper;
	}

	public TckBackendFeatures getBackendFeatures() {
		return helper.getBackendFeatures();
	}

}
