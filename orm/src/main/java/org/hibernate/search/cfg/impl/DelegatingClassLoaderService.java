/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.cfg.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;

/**
 * A Search class loader implementation which delegates to the ORM class loader service.
 *
 * @author Hardy Ferentschik
 */
public class DelegatingClassLoaderService implements ClassLoaderService {
	private final org.hibernate.boot.registry.classloading.spi.ClassLoaderService hibernateClassLoaderService;

	public DelegatingClassLoaderService(org.hibernate.boot.registry.classloading.spi.ClassLoaderService hibernateClassLoaderService) {
		this.hibernateClassLoaderService = hibernateClassLoaderService;
	}

	@Override
	public <T> Class<T> classForName(String className) {
		try {
			return hibernateClassLoaderService.classForName( className );
		}
		catch (ClassLoadingException e) {
			throw new org.hibernate.search.engine.service.classloading.spi.ClassLoadingException( e.getMessage() );
		}
	}

	@Override
	public URL locateResource(String name) {
		return hibernateClassLoaderService.locateResource( name );
	}

	@Override
	public InputStream locateResourceStream(String name) {
		return hibernateClassLoaderService.locateResourceStream( name );
	}

	@Override
	public <T> LinkedHashSet<T> loadJavaServices(Class<T> serviceContract) {
		return hibernateClassLoaderService.loadJavaServices( serviceContract );
	}
}


