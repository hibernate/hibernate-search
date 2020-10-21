/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4j2ConfigurationAccessor {

	private final LoggerContext context;
	private final Configuration configuration;
	private final LoggerConfig rootLogger;

	private Appender appender;
	private Level rootLoggerLevel;

	public Log4j2ConfigurationAccessor() {
		context = (LoggerContext) LogManager.getContext( false );
		configuration = context.getConfiguration();
		rootLogger = configuration.getRootLogger();
	}

	public void addAppender(Appender appender) {
		this.appender = appender;
		rootLoggerLevel = rootLogger.getLevel();

		configuration.addAppender( appender );
		rootLogger.addAppender( appender, Level.ALL, null );
		rootLogger.setLevel( Level.ALL );
		appender.start();
		context.updateLoggers();
	}

	public void removeAppender() {
		if ( appender == null ) {
			return;
		}

		appender.stop();
		rootLogger.removeAppender( appender.getName() );
		rootLogger.setLevel( rootLoggerLevel );
		configuration.getAppenders().remove( appender.getName() );
		context.updateLoggers();
	}
}
