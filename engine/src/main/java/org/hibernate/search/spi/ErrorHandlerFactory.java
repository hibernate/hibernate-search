/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Factory of {@link org.hibernate.search.exception.ErrorHandler}.
 * Some helper methods are exposed so that some integrating frameworks can wrap the
 * ErrorHandler with some custom decoration, while still leaving it
 * up to the user to choose which ErrorHandler to use within the decorator.
 *
 * @since 5.6
 */
public final class ErrorHandlerFactory {

	private ErrorHandlerFactory() {
		//Not to be invoked: we only expose static methods.
	}

	/**
	 * @return Default ErrorHandler in case none is explicitly configured.
	 */
	public static ErrorHandler getDefault() {
		return new LogErrorHandler();
	}

	/**
	 * @param searchConfiguration The configuration that may specify an ErrorHandler
	 * @return ErrorHandler specified in the {@link SearchConfiguration} or the default one in case not specified.
	 */
	public static ErrorHandler createErrorHandler(SearchConfiguration searchConfiguration) {
		Object configuredErrorHandler = searchConfiguration.getProperties().get( Environment.ERROR_HANDLER );

		if ( configuredErrorHandler == null ) {
			return getDefault();
		}
		if ( configuredErrorHandler instanceof String ) {
			return createErrorHandlerFromString(
					(String) configuredErrorHandler,
					searchConfiguration.getClassLoaderService()
			);
		}
		else if ( configuredErrorHandler instanceof ErrorHandler ) {
			return (ErrorHandler) configuredErrorHandler;
		}
		else {
			throw LoggerFactory.make().unsupportedErrorHandlerConfigurationValueType( configuredErrorHandler.getClass() );
		}
	}

	private static ErrorHandler createErrorHandlerFromString(
			String errorHandlerClassName,
			ClassLoaderService classLoaderService) {
		if ( StringHelper.isEmpty( errorHandlerClassName ) || ErrorHandler.LOG.equals( errorHandlerClassName.trim() ) ) {
			return getDefault();
		}
		else {
			Class<?> errorHandlerClass = classLoaderService.classForName( errorHandlerClassName );
			return ClassLoaderHelper.instanceFromClass(
					ErrorHandler.class,
					errorHandlerClass,
					"Error Handler"
			);
		}
	}

}
