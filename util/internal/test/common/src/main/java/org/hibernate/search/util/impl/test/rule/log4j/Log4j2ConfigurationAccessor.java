/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.rule.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4j2ConfigurationAccessor {

	private final LoggerContext context;
	private final Configuration configuration;
	private final LoggerConfig logger;

	private Appender appender;
	private Level originalLoggerLevel;

	public Log4j2ConfigurationAccessor(String loggerName) {
		context = (LoggerContext) LogManager.getContext( false );
		configuration = context.getConfiguration();
		// Make sure the logger exists (this call is ignored if it already exists)
		configuration.addLogger( loggerName, new LoggerConfig() );
		logger = configuration.getLoggerConfig( loggerName );
	}

	public void addAppender(Appender appender) {
		this.appender = appender;
		originalLoggerLevel = logger.getLevel();

		logger.addAppender( appender, Level.ALL, null );
		logger.setLevel( Level.ALL );
		appender.start();
		context.updateLoggers();
	}

	public void removeAppender() {
		if ( appender == null ) {
			return;
		}

		appender.stop();
		logger.removeAppender( appender.getName() );
		logger.setLevel( originalLoggerLevel );
		context.updateLoggers();
	}
}
