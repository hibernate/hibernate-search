/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.hibernate.cfg.JdbcSettings;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ulimit;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

/*
 * Suppress "Resource leak: '<unassigned Closeable value>' is never closed". Testcontainers take care of closing
 * these resources in the end.
 */
@SuppressWarnings("resource")
public final class DatabaseContainer {

	private static final Duration REGULAR_TIMEOUT = Duration.ofMinutes( 5 );
	private static final Duration EXTENDED_TIMEOUT = Duration.ofMinutes( 10 );

	private DatabaseContainer() {
	}

	private static final SupportedDatabase DATABASE;
	private static final HibernateSearchJdbcDatabaseContainer DATABASE_CONTAINER;


	static {
		String name = System.getProperty( "org.hibernate.search.integrationtest.orm.database.kind", "" );
		Path containers = Path.of( System.getProperty( "org.hibernate.search.integrationtest.container.directory", "" ) );
		DATABASE = SupportedDatabase.from( name );

		DATABASE_CONTAINER = DATABASE.container(
				containers.resolve( "database" ).resolve( name + ".Dockerfile" ),
				name
		);
	}

	public static Configuration configuration() {
		// Let's see if an external DB connection was provided:
		String url = System.getProperty( "jdbc.url" );
		if ( url != null && !url.trim().isEmpty() ) {
			// -Dhibernate.dialect=${db.dialect}
			// -Dhibernate.connection.driver_class=${jdbc.driver}
			// -Dhibernate.connection.url=${jdbc.url}
			// -Dhibernate.connection.username=${jdbc.user}
			// -Dhibernate.connection.password=${jdbc.pass}
			// -Dhibernate.connection.isolation=${jdbc.isolation}
			return DATABASE.configuration( url, DATABASE_CONTAINER )
					.withDialect( System.getProperty( "db.dialect" ) )
					.withDriver( System.getProperty( "jdbc.driver" ) )
					.withUser( System.getProperty( "jdbc.user" ) )
					.withPass( System.getProperty( "jdbc.pass" ) )
					.withIsolation( System.getProperty( "jdbc.isolation" ) );
		}
		else {
			if ( DATABASE_CONTAINER != null && !DATABASE_CONTAINER.isRunning() ) {
				synchronized (DATABASE_CONTAINER) {
					if ( !DATABASE_CONTAINER.isRunning() ) {
						DATABASE_CONTAINER.start();
					}
				}
			}
			return DATABASE.configuration( DATABASE_CONTAINER );
		}
	}

	private enum SupportedDatabase {
		H2 {
			@Override
			String dialect() {
				return org.hibernate.dialect.H2Dialect.class.getName();
			}

			@Override
			Configuration configuration(JdbcDatabaseContainer<?> container) {
				return new Configuration(
						dialect(),
						"org.h2.Driver",
						"jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1",
						"sa",
						"sa",
						""
				);
			}

			@Override
			Configuration configuration(String jdbcUrl, JdbcDatabaseContainer<?> container) {
				return configuration( container ).withUrl( jdbcUrl );
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return null;
			}
		},
		POSTGRES {
			@Override
			String dialect() {
				return org.hibernate.dialect.PostgreSQLDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"org.postgresql.Driver",
						"jdbc:postgresql://%s:%d/hibernate_orm_test",
						5432,
						"hibernate_orm_test",
						"hibernate_orm_test",
						"select 1",
						new LogMessageWaitStrategy()
								.withRegEx( ".*database system is ready to accept connections.*\\s" )
								.withTimes( 2 )
				).withEnv( "POSTGRES_USER", "hibernate_orm_test" )
						.withEnv( "POSTGRES_PASSWORD", "hibernate_orm_test" )
						.withEnv( "POSTGRES_DB", "hibernate_orm_test" );
			}
		},
		MARIADB {
			@Override
			String dialect() {
				return org.hibernate.dialect.MariaDBDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"org.mariadb.jdbc.Driver",
						"jdbc:mariadb://%s:%d/hibernate_orm_test",
						3306,
						"hibernate_orm_test",
						"hibernate_orm_test",
						"select 1"
				).withCommand( "--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci" )
						.withEnv( "MYSQL_USER", "hibernate_orm_test" )
						.withEnv( "MYSQL_PASSWORD", "hibernate_orm_test" )
						.withEnv( "MYSQL_DATABASE", "hibernate_orm_test" )
						.withEnv( "MYSQL_RANDOM_ROOT_PASSWORD", "true" )
						.withTmpFs( Collections.singletonMap( "/var/lib/mysql", "" ) );
			}
		},
		MYSQL {
			@Override
			String dialect() {
				return org.hibernate.dialect.MySQLDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"com.mysql.jdbc.Driver",
						"jdbc:mysql://%s:%d/hibernate_orm_test",
						3306,
						"hibernate_orm_test",
						"hibernate_orm_test",
						"select 1"
				).withCommand( "--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci" )
						.withEnv( "MYSQL_USER", "hibernate_orm_test" )
						.withEnv( "MYSQL_PASSWORD", "hibernate_orm_test" )
						.withEnv( "MYSQL_DATABASE", "hibernate_orm_test" )
						.withEnv( "MYSQL_RANDOM_ROOT_PASSWORD", "true" )
						.withTmpFs( Collections.singletonMap( "/var/lib/mysql", "" ) );
			}
		},
		DB2 {
			@Override
			String dialect() {
				return org.hibernate.dialect.DB2Dialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {

				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"com.ibm.db2.jcc.DB2Driver",
						"jdbc:db2://%s:%d/hreact:sslConnection=false;",
						50000,
						"hreact",
						"hreact",
						"SELECT 1 FROM SYSIBM.SYSDUMMY1",
						new LogMessageWaitStrategy()
								.withRegEx( ".*Setup has completed\\..*" )
				).withPrivilegedMode( true )
						.withNetworkMode( "bridge" )
						.withEnv( "DB2INSTANCE", "hreact" )
						.withEnv( "DB2INST1_PASSWORD", "hreact" )
						.withEnv( "DBNAME", "hreact" )
						.withEnv( "LICENSE", "accept" )
						// These help the DB2 container start faster
						.withEnv( "AUTOCONFIG", "false" )
						.withEnv( "ARCHIVE_LOGS", "false" )
						.withEnv( "PERSISTENT_HOME", "false" )
						// because it takes ages to start and select query will just get tired to retry
						// this is a JDBCContainer specific setting
						.withStartupTimeoutSeconds( 600 )
						.withStartupTimeout( EXTENDED_TIMEOUT );
			}
		},
		ORACLE {
			@Override
			String dialect() {
				return org.hibernate.dialect.OracleDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"oracle.jdbc.OracleDriver",
						"jdbc:oracle:thin:@%s:%d/FREEPDB1",
						1521,
						"SYSTEM",
						"hibernate_orm_test",
						"select 1 from dual",
						new LogMessageWaitStrategy()
								.withRegEx( ".*DATABASE IS READY TO USE!.*\\s" )
								.withTimes( 1 )
				).withEnv( "ORACLE_PASSWORD", "hibernate_orm_test" );
			}
		},
		MSSQL {
			@Override
			String dialect() {
				return org.hibernate.dialect.SQLServerDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"com.microsoft.sqlserver.jdbc.SQLServerDriver",
						"jdbc:sqlserver://%s:%d;databaseName=tempdb;encrypt=true;trustServerCertificate=true;",
						1433,
						"SA",
						"ActuallyRequired11Complexity",
						"select 1"
				).withEnv( "ACCEPT_EULA", "Y" )
						.withEnv( "MSSQL_SA_PASSWORD", "ActuallyRequired11Complexity" )
						.withStartupTimeout( EXTENDED_TIMEOUT );
			}
		},
		COCKROACHDB {
			@Override
			String dialect() {
				return org.hibernate.dialect.CockroachDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name) {
				return new HibernateSearchJdbcDatabaseContainer(
						dockerfile, name,
						"org.postgresql.Driver",
						"jdbc:postgresql://%s:%d/defaultdb?sslmode=disable",
						26257,
						"root",
						"",
						"select 1",
						new HttpWaitStrategy()
								.forPath( "/health" )
								.forPort( 8080 )
								.forStatusCode( 200 ),
						8080
				).withCommand( "start-single-node --insecure" )
						.withStartupTimeout( EXTENDED_TIMEOUT )
						.withCreateContainerCmdModifier( cmd -> {
							HostConfig hostConfig = cmd.getHostConfig();
							if ( hostConfig == null ) {
								throw new IllegalStateException( "Host config is `null`. Cannot redefine the ulimits!" );
							}
							hostConfig.withUlimits( List.of( new Ulimit( "nofile", 1956L, 1956L ) ) );
						} );
			}
		};

		Configuration configuration(JdbcDatabaseContainer<?> container) {
			return new Configuration(
					dialect(),
					container.getDriverClassName(),
					container.getJdbcUrl(),
					container.getUsername(),
					container.getPassword(),
					""
			);
		}

		Configuration configuration(String jdbcUrl, JdbcDatabaseContainer<?> container) {
			return new Configuration(
					dialect(),
					container.getDriverClassName(),
					jdbcUrl,
					container.getUsername(),
					container.getPassword(),
					""
			);
		}

		abstract String dialect();

		abstract HibernateSearchJdbcDatabaseContainer container(Path dockerfile, String name);

		static SupportedDatabase from(String name) {
			for ( SupportedDatabase database : values() ) {
				if ( name.toLowerCase( Locale.ROOT ).contains( database.name().toLowerCase( Locale.ROOT ) ) ) {
					return database;
				}
			}
			throw new IllegalStateException( "Unsupported database requested: " + name );
		}
	}

	private static class HibernateSearchJdbcDatabaseContainer
			extends JdbcDatabaseContainer<HibernateSearchJdbcDatabaseContainer> {

		private final String driverClassName;
		private final String jdbcUrlPattern;
		private final int port;
		private final String username;
		private final String password;
		private final String testQueryString;
		private final Optional<WaitStrategy> customWaitStrategy;

		public HibernateSearchJdbcDatabaseContainer(Path dockerfile, String name, String driverClassName, String jdbcUrlPattern,
				int port, String username, String password, String testQueryString) {
			this( dockerfile, name, driverClassName, jdbcUrlPattern, port, username, password, testQueryString, null );
		}

		public HibernateSearchJdbcDatabaseContainer(Path dockerfile, String name, String driverClassName, String jdbcUrlPattern,
				int port, String username, String password, String testQueryString, WaitStrategy waitStrategy,
				Integer... additionalExposedPorts) {
			// IMPORTANT: we do not want to delete the image on exit as then we cannot use container reuse.
			// (these two options are somewhat mutually exclusive).
			super( new ImageFromDockerfile( "hibernate-search-" + name, false ).withDockerfile( dockerfile ) );
			this.driverClassName = driverClassName;
			this.jdbcUrlPattern = jdbcUrlPattern;
			this.port = port;
			this.username = username;
			this.password = password;
			this.testQueryString = testQueryString;
			this.customWaitStrategy = Optional.ofNullable( waitStrategy );

			if ( additionalExposedPorts.length == 0 ) {
				withExposedPorts( port );
			}
			else {
				Integer[] ports = Arrays.copyOf( additionalExposedPorts, additionalExposedPorts.length + 1 );
				ports[additionalExposedPorts.length] = port;
				withExposedPorts( ports );
			}
			withReuse( true );
			withStartupTimeout( REGULAR_TIMEOUT );
		}

		@Override
		protected void waitUntilContainerStarted() {
			if ( customWaitStrategy.isPresent() ) {
				customWaitStrategy.get().waitUntilReady( this );
			}
			else {
				super.waitUntilContainerStarted();
			}
		}

		@Override
		public HibernateSearchJdbcDatabaseContainer withStartupTimeout(Duration startupTimeout) {
			if ( customWaitStrategy.isPresent() ) {
				customWaitStrategy.get().withStartupTimeout( startupTimeout );
			}
			return super.withStartupTimeout( startupTimeout );
		}

		@Override
		public String getDriverClassName() {
			return driverClassName;
		}

		@Override
		public String getJdbcUrl() {
			return String.format( Locale.ROOT, jdbcUrlPattern, getHost(), getMappedPort( port ) );
		}

		@Override
		public String getUsername() {
			return username;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		protected String getTestQueryString() {
			return testQueryString;
		}
	}


	public static class Configuration {
		private final String dialect;
		private final String driver;
		private final String url;
		private final String user;
		private final String pass;
		private final String isolation;

		private Configuration(String dialect, String driver, String url, String user, String pass, String isolation) {
			this.dialect = dialect;
			this.driver = driver;
			this.url = url;
			this.user = user;
			this.pass = pass;
			this.isolation = isolation;
		}

		public String dialect() {
			return dialect;
		}

		public String driver() {
			return driver;
		}

		public String url() {
			return url;
		}

		public String user() {
			return user;
		}

		public String pass() {
			return pass;
		}

		public String isolation() {
			return isolation;
		}

		@SuppressWarnings("deprecation") // since DialectContext is using the deprecated properties we cannot switch to JAKARTA_* for now...
		public void add(Map<String, Object> map) {
			map.put( JdbcSettings.DIALECT, this.dialect );
			map.put( JdbcSettings.DRIVER, this.driver );
			map.put( JdbcSettings.URL, this.url );
			map.put( JdbcSettings.USER, this.user );
			map.put( JdbcSettings.PASS, this.pass );
			map.put( JdbcSettings.ISOLATION, this.isolation );
		}

		public void addAsSpring(BiConsumer<String, String> consumer) {
			consumer.accept( "spring.datasource.driver-class-name", this.driver );
			consumer.accept( "spring.datasource.url", this.url );
			consumer.accept( "spring.datasource.username", this.user );
			consumer.accept( "spring.datasource.password", this.pass );
		}

		private Configuration withDialect(String dialect) {
			if ( dialect == null ) {
				return this;
			}
			return new Configuration( dialect, driver, url, user, pass, isolation );
		}

		private Configuration withDriver(String driver) {
			if ( driver == null ) {
				return this;
			}
			return new Configuration( dialect, driver, url, user, pass, isolation );
		}

		private Configuration withUrl(String url) {
			if ( url == null ) {
				return this;
			}
			return new Configuration( dialect, driver, url, user, pass, isolation );
		}

		private Configuration withUser(String user) {
			if ( user == null ) {
				return this;
			}
			return new Configuration( dialect, driver, url, user, pass, isolation );
		}

		private Configuration withPass(String pass) {
			if ( pass == null ) {
				return this;
			}
			return new Configuration( dialect, driver, url, user, pass, isolation );
		}

		private Configuration withIsolation(String isolation) {
			if ( isolation == null ) {
				return this;
			}
			return new Configuration( dialect, driver, url, user, pass, isolation );
		}
	}
}
