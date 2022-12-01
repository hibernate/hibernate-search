/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.time.Duration;
import java.util.Collections;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

/*
 * Suppress "Resource leak: '<unassigned Closeable value>' is never closed". Testcontainers take care of closing
 * these resources in the end.
 */
@SuppressWarnings("resource")
public final class DatabaseContainer {

	private DatabaseContainer() {
	}

	private static final SupportedDatabase DATABASE;
	private static final JdbcDatabaseContainer<?> DATABASE_CONTAINER;


	static {
		String database = System.getProperty( "org.hibernate.search.integrationtest.orn.database" );
		DATABASE = SupportedDatabase.from( database );

		DATABASE_CONTAINER = DATABASE.container();
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
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return null;
			}
		},
		POSTGRESQL {
			@Override
			String dialect() {
				return org.hibernate.dialect.PostgreSQL10Dialect.class.getName();
			}

			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "postgres" ).withTag( "13.1" ) ) {
					@Override
					public String getDriverClassName() {
						return "org.postgresql.Driver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:postgresql://" + getHost() + ":" + getMappedPort( 5432 ) + "/hibernate_orm_test";
					}

					@Override
					public String getUsername() {
						return "hibernate_orm_test";
					}

					@Override
					public String getPassword() {
						return "hibernate_orm_test";
					}

					@Override
					protected String getTestQueryString() {
						return "select 1";
					}
				}
						.withExposedPorts( 5432 )
						.withEnv( "POSTGRES_USER", "hibernate_orm_test" )
						.withEnv( "POSTGRES_PASSWORD", "hibernate_orm_test" )
						.withEnv( "POSTGRES_DB", "hibernate_orm_test" )
						.withStartupTimeout( Duration.ofMinutes( 2 ) )
						.withReuse( true );
			}
		},
		MARIADB {
			@Override
			String dialect() {
				return org.hibernate.dialect.MariaDB103Dialect.class.getName();
			}
			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "mariadb" ).withTag( "10.5.8" ) ) {
					@Override
					public String getDriverClassName() {
						return "org.mariadb.jdbc.Driver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:mariadb://" + getHost() + ":" + getMappedPort( 3306 ) + "/hibernate_orm_test";
					}

					@Override
					public String getUsername() {
						return "hibernate_orm_test";
					}

					@Override
					public String getPassword() {
						return "hibernate_orm_test";
					}

					@Override
					protected String getTestQueryString() {
						return "select 1";
					}
				}
						.withExposedPorts( 3306 )
						.withCommand( "--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci" )
						.withEnv( "MYSQL_USER", "hibernate_orm_test" )
						.withEnv( "MYSQL_PASSWORD", "hibernate_orm_test" )
						.withEnv( "MYSQL_DATABASE", "hibernate_orm_test" )
						.withEnv( "MYSQL_RANDOM_ROOT_PASSWORD", "true" )
						.withTmpFs( Collections.singletonMap( "/var/lib/mysql", "" ) )
						.withStartupTimeout( Duration.ofMinutes( 2 ) )
						.withReuse( true );
			}
		},
		MYSQL {
			@Override
			String dialect() {
				return org.hibernate.dialect.MySQL8Dialect.class.getName();
			}

			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "mysql" ).withTag( "8.0.22" ) ) {
					@Override
					public String getDriverClassName() {
						return "com.mysql.jdbc.Driver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:mysql://" + getHost() + ":" + getMappedPort( 3306 ) + "/hibernate_orm_test";
					}

					@Override
					public String getUsername() {
						return "hibernate_orm_test";
					}

					@Override
					public String getPassword() {
						return "hibernate_orm_test";
					}

					@Override
					protected String getTestQueryString() {
						return "select 1";
					}
				}
						.withExposedPorts( 3306 )
						.withCommand( "--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci" )
						.withEnv( "MYSQL_USER", "hibernate_orm_test" )
						.withEnv( "MYSQL_PASSWORD", "hibernate_orm_test" )
						.withEnv( "MYSQL_DATABASE", "hibernate_orm_test" )
						.withEnv( "MYSQL_RANDOM_ROOT_PASSWORD", "true" )
						.withTmpFs( Collections.singletonMap( "/var/lib/mysql", "" ) )
						.withStartupTimeout( Duration.ofMinutes( 2 ) )
						.withReuse( true );
			}
		},
		DB2 {
			@Override
			String dialect() {
				return org.hibernate.dialect.DB297Dialect.class.getName();
			}

			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "ibmcom/db2" ).withTag( "11.5.8.0" ) ) {
					@Override
					public String getDriverClassName() {
						return "com.ibm.db2.jcc.DB2Driver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:db2://" + getHost() + ":" + getMappedPort( 50000 ) + "/hreact";
					}

					@Override
					public String getUsername() {
						return "hreact";
					}

					@Override
					public String getPassword() {
						return "hreact";
					}

					@Override
					protected String getTestQueryString() {
						return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
					}
				}
						.withExposedPorts( 50000 )
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
						.withStartupTimeout( Duration.ofMinutes( 15 ) )
						.withReuse( true );
			}
		},
		ORACLE {
			@Override
			String dialect() {
				return org.hibernate.dialect.Oracle12cDialect.class.getName();
			}

			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "gvenzl/oracle-xe" ).withTag( "21-slim-faststart" ) ) {
					@Override
					public String getDriverClassName() {
						return "oracle.jdbc.OracleDriver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:oracle:thin:@" + getHost() + ":" + getMappedPort( 1521 ) + "/XE";
					}

					@Override
					public String getUsername() {
						return "SYSTEM";
					}

					@Override
					public String getPassword() {
						return "hibernate_orm_test";
					}

					@Override
					protected String getTestQueryString() {
						return "select 1 from dual";
					}
				}
						.withExposedPorts( 1521 )
						.withEnv( "ORACLE_PASSWORD", "hibernate_orm_test" )
						.withStartupTimeout( Duration.ofMinutes( 10 ) )
						.withReuse( true );
			}
		},
		MSSQL {
			@Override
			String dialect() {
				return org.hibernate.dialect.SQLServer2016Dialect.class.getName();
			}

			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "mcr.microsoft.com/mssql/server" ).withTag( "2019-CU8-ubuntu-16.04" ) ) {
					@Override
					public String getDriverClassName() {
						return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:sqlserver://" + getHost() + ":" + getMappedPort( 1433 ) + ";databaseName=tempdb";
					}

					@Override
					public String getUsername() {
						return "SA";
					}

					@Override
					public String getPassword() {
						return "ActuallyRequired11Complexity";
					}

					@Override
					protected String getTestQueryString() {
						return "select 1";
					}
				}
						.withExposedPorts( 1433 )
						.withEnv( "ACCEPT_EULA", "Y" )
						.withEnv( "SA_PASSWORD", "ActuallyRequired11Complexity" )
						.withStartupTimeout( Duration.ofMinutes( 10 ) )
						.withReuse( true );
			}
		},
		COCKROACHDB {
			@Override
			String dialect() {
				return org.hibernate.dialect.CockroachDB201Dialect.class.getName();
			}

			@Override
			<SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container() {
				return new JdbcDatabaseContainer<SELF>( DockerImageName.parse( "cockroachdb/cockroach" ).withTag( "v22.1.4" ) ) {
					@Override
					public String getDriverClassName() {
						return "org.postgresql.Driver";
					}

					@Override
					public String getJdbcUrl() {
						return "jdbc:postgresql://" + getHost() + ":" + getMappedPort( 26257 ) + "/defaultdb?sslmode=disable";
					}

					@Override
					public String getUsername() {
						return "root";
					}

					@Override
					public String getPassword() {
						return "";
					}

					@Override
					protected String getTestQueryString() {
						return "select 1";
					}
				}
						.withExposedPorts( 26257 )
						.withCommand( "start-single-node --insecure" )
						.withStartupTimeout( Duration.ofMinutes( 5 ) )
						.withReuse( true );
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

		abstract <SELF extends JdbcDatabaseContainer<SELF>> JdbcDatabaseContainer<SELF> container();

		static SupportedDatabase from(String name) {
			for ( SupportedDatabase database : values() ) {
				if ( database.name().equalsIgnoreCase( name ) ) {
					return database;
				}
			}
			throw new IllegalStateException( "Unsupported database requested: " + name );
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
