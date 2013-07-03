/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.serialization.spi;

import java.util.Properties;

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.indexes.serialization.impl.PluggableSerializationLuceneWorkSerializer;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Provides a Serializer of LuceneWork instances consuming a SerializerProvider
 * This is commonly used to delegate indexing work to a different JVM.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SerializerService implements ServiceProvider<LuceneWorkSerializer> {

	private static final Log log = LoggerFactory.make();

	private PluggableSerializationLuceneWorkSerializer workSerializer;
	private ServiceManager serviceManager;

	@Override
	public void start(Properties properties, BuildContext buildContext) {
		serviceManager = buildContext.getServiceManager();
		SerializationProvider serializationProvider = serviceManager.requestService( SerializationProviderService.class, buildContext );
		try {
			workSerializer = new PluggableSerializationLuceneWorkSerializer(
					serializationProvider,
					buildContext.getUninitializedSearchFactory()
			);
		}
		catch (RuntimeException e) {
			if ( e instanceof SearchException ) {
				throw e;
			}
			else {
				throw log.unableToStartSerializationLayer( e );
			}
		}
	}

	@Override
	public LuceneWorkSerializer getService() {
		return workSerializer;
	}

	@Override
	public void stop() {
		if ( workSerializer != null ) {
			serviceManager.releaseService( SerializationProviderService.class );
		}
	}

}
