/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
