/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoaderHelper;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.util.ResourceLoader;

/**
 * Hibernate Search specific implementation of Lucene's {@code ResourceLoader} interface.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Hardy Ferentschik
 */
final class HibernateSearchResourceLoader implements ResourceLoader {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ClassResolver classResolver;
	private final ResourceResolver resourceResolver;

	HibernateSearchResourceLoader(ClassResolver classResolver, ResourceResolver resourceResolver) {
		this.classResolver = classResolver;
		this.resourceResolver = resourceResolver;
	}

	@Override
	public InputStream openResource(String resource) {
		InputStream inputStream = resourceResolver.locateResourceStream( resource );

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
				classResolver
		);
	}

	@Override
	public <T> T newInstance(String className, Class<T> expectedType) {
		return ClassLoaderHelper.instanceFromName(
				expectedType,
				className,
				classResolver
		);
	}

}
