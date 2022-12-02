/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.Locale;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ulimit;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

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
		String name = System.getProperty( "org.hibernate.search.integrationtest.orm.database.image.name", "" );
		String tag = System.getProperty( "org.hibernate.search.integrationtest.orm.database.image.tag" );
		DATABASE = SupportedDatabase.from( name );

		DATABASE_CONTAINER = DATABASE.container( name, tag );
	}


	public static Configuration configuration() {
		if ( !SupportedDatabase.H2.equals( DATABASE ) ) {
			DATABASE_CONTAINER.start();
		}
		Configuration configuration = DATABASE.configuration( DATABASE_CONTAINER );
		configuration.addAsSystemProperties();
		return configuration;
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
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return null;
			}
		},
		POSTGRES {
			@Override
			String dialect() {
				return org.hibernate.dialect.PostgreSQLDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
						"org.postgresql.Driver",
						"jdbc:postgresql://%s:%d/hibernate_orm_test",
						5432,
						"hibernate_orm_test",
						"hibernate_orm_test",
						"select 1"
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
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
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
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
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
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
						"com.ibm.db2.jcc.DB2Driver",
						"jdbc:db2://%s:%d/hreact",
						50000,
						"hreact",
						"hreact",
						"SELECT 1 FROM SYSIBM.SYSDUMMY1"

				).withNetworkMode( "bridge" )
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
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
						"oracle.jdbc.OracleDriver",
						"jdbc:oracle:thin:@%s:%d/XE",
						1521,
						"SYSTEM",
						"hibernate_orm_test",
						"select 1 from dual"
				).withEnv( "ORACLE_PASSWORD", "hibernate_orm_test" )
						.withStartupTimeout( EXTENDED_TIMEOUT );
			}
		},
		MSSQL {
			@Override
			String dialect() {
				return org.hibernate.dialect.SQLServerDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
						"com.microsoft.sqlserver.jdbc.SQLServerDriver",
						"jdbc:sqlserver://%s:%d;databaseName=tempdb",
						1433,
						"SA",
						"ActuallyRequired11Complexity",
						"select 1"
				).withEnv( "ACCEPT_EULA", "Y" )
						.withEnv( "SA_PASSWORD", "ActuallyRequired11Complexity" )
						.withStartupTimeout( EXTENDED_TIMEOUT );
			}
		},
		COCKROACHDB {
			@Override
			String dialect() {
				return org.hibernate.dialect.CockroachDialect.class.getName();
			}

			@Override
			HibernateSearchJdbcDatabaseContainer container(String name, String tag) {
				return new HibernateSearchJdbcDatabaseContainer(
						DockerImageName.parse( name ).withTag( tag ),
						"org.postgresql.Driver",
						"jdbc:postgresql://%s:%d/defaultdb?sslmode=disable",
						26257,
						"root",
						"",
						"select 1"
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

		abstract String dialect();

		abstract HibernateSearchJdbcDatabaseContainer container(String name, String tag);

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

		public HibernateSearchJdbcDatabaseContainer(Future<String> image, String driverClassName, String jdbcUrlPattern,
				int port, String username, String password, String testQueryString) {
			super( image );
			this.driverClassName = driverClassName;
			this.jdbcUrlPattern = jdbcUrlPattern;
			this.port = port;
			this.username = username;
			this.password = password;
			this.testQueryString = testQueryString;
			posInit();
		}

		public HibernateSearchJdbcDatabaseContainer(DockerImageName dockerImageName, String driverClassName,
				String jdbcUrlPattern, int port, String username, String password, String testQueryString) {
			super( dockerImageName );
			this.driverClassName = driverClassName;
			this.jdbcUrlPattern = jdbcUrlPattern;
			this.port = port;
			this.username = username;
			this.password = password;
			this.testQueryString = testQueryString;
			posInit();
		}

		private void posInit() {
			withExposedPorts( port );
			withReuse( true );
			withStartupTimeout( REGULAR_TIMEOUT );
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

		public Configuration(String dialect, String driver, String url, String user, String pass, String isolation) {
			this.dialect = dialect;
			this.driver = driver;
			this.url = url;
			this.user = user;
			this.pass = pass;
			this.isolation = isolation;
		}

		private void addAsSystemProperties() {
			System.setProperty( "hibernate.dialect", this.dialect );
			System.setProperty( "hibernate.connection.driver_class", this.driver );
			System.setProperty( "hibernate.connection.url", this.url );
			System.setProperty( "hibernate.connection.username", this.user );
			System.setProperty( "hibernate.connection.password", this.pass );
			System.setProperty( "hibernate.connection.isolation", this.isolation );
		}
	}

}
