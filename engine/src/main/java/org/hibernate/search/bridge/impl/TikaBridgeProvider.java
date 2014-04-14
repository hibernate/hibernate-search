/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.Blob;

import org.hibernate.search.annotations.TikaBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TikaBridgeProvider extends ExtendedBridgeProvider {
	private static final Log LOG = LoggerFactory.make();

	private final String TIKA_BRIDGE_NAME = "org.hibernate.search.bridge.builtin.TikaBridge";
	private final String TIKA_BRIDGE_METADATA_PROCESSOR_SETTER = "setMetadataProcessorClass";
	private final String TIKA_BRIDGE_PARSE_CONTEXT_SETTER = "setParseContextProviderClass";

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( annotatedElement.isAnnotationPresent( TikaBridge.class ) ) {
			Class<?> returnType = context.getReturnType();
			if ( ! Blob.class.isAssignableFrom( returnType )
					&& ! byte[].class.isAssignableFrom( returnType )
					&& ! String.class.isAssignableFrom( returnType )
					&& ! URI.class.isAssignableFrom( returnType ) ) {
				throw LOG.unsupportedTikaBridgeType( returnType );
			}
			TikaBridge tikaAnnotation = annotatedElement.getAnnotation( TikaBridge.class );
			return createTikaBridge( tikaAnnotation, context.getServiceManager() );
		}
		return null;
	}

	private FieldBridge createTikaBridge(org.hibernate.search.annotations.TikaBridge annotation, ServiceManager serviceManager) {
		Class<?> tikaBridgeClass;
		FieldBridge tikaBridge;
		try {
			tikaBridgeClass = ClassLoaderHelper.classForName( TIKA_BRIDGE_NAME, serviceManager );
			tikaBridge = ClassLoaderHelper.instanceFromClass( FieldBridge.class, tikaBridgeClass, "Tika bridge" );
		}
		catch (ClassLoadingException e) {
			throw new AssertionFailure( "Unable to find Tika bridge class: " + TIKA_BRIDGE_NAME );
		}

		Class<?> tikaMetadataProcessorClass = annotation.metadataProcessor();
		if ( tikaMetadataProcessorClass != void.class ) {
			configureTikaBridgeParameters(
					tikaBridgeClass,
					TIKA_BRIDGE_METADATA_PROCESSOR_SETTER,
					tikaBridge,
					tikaMetadataProcessorClass
			);
		}

		Class<?> tikaParseContextProviderClass = annotation.parseContextProvider();
		if ( tikaParseContextProviderClass != void.class ) {
			configureTikaBridgeParameters(
					tikaBridgeClass,
					TIKA_BRIDGE_PARSE_CONTEXT_SETTER,
					tikaBridge,
					tikaParseContextProviderClass
			);
		}

		return tikaBridge;
	}

	private void configureTikaBridgeParameters(Class<?> tikaBridgeClass, String setter, Object tikaBridge, Class<?> clazz) {
		try {
			Method m = tikaBridgeClass.getMethod( setter, Class.class );
			m.invoke( tikaBridge, clazz );
		}
		catch (Exception e) {
			throw LOG.unableToConfigureTikaBridge( TIKA_BRIDGE_NAME, e );
		}
	}
}
