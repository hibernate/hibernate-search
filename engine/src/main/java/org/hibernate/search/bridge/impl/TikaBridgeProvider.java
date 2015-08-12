/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * @author Emmanuel Bernard
 */
class TikaBridgeProvider extends ExtendedBridgeProvider {

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
