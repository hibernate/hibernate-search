/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Hibernate Search specific implementation of Lucene's {@code ResourceLoader} interface.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public class HibernateSearchResourceLoader implements ResourceLoader {
	private static final Log log = LoggerFactory.make();
	private final ServiceManager serviceManager;

	public HibernateSearchResourceLoader(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	@Override
	public InputStream openResource(String resource) throws IOException {
		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		InputStream inputStream;
		try {
			inputStream = classLoaderService.locateResourceStream( resource );
		}
		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}

		if ( inputStream == null ) {
			throw log.unableToLoadResource( resource );
		}
		else {
			return inputStream;
		}
	}

	@Override
	public <T> Class<? extends T> findClass(String className, Class<T> expectedType) {
		return ClassLoaderHelper.classForName(
				expectedType,
				className,
				describeComponent( className ),
				serviceManager
		);
	}

	@Override
	public <T> T newInstance(String className, Class<T> expectedType) {
		return ClassLoaderHelper.instanceFromName(
				expectedType,
				className,
				describeComponent( className ),
				serviceManager
		);
	}

	private static String describeComponent(final String className) {
		return "Lucene Analyzer component " + className;
	}

}
