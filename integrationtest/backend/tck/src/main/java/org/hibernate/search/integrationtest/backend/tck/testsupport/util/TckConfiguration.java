/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
