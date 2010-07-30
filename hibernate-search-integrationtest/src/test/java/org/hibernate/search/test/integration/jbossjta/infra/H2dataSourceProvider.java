package org.hibernate.search.test.integration.jbossjta.infra;

import java.sql.SQLException;
import javax.sql.XADataSource;

import com.arjuna.ats.internal.jdbc.DynamicClass;
import org.h2.jdbcx.JdbcDataSource;

/**
 * Bind a H2 DataSource to the name "h2"
 *
 * @author Emmanuel Bernard
 */
public class H2dataSourceProvider implements DynamicClass {
	private static String DATASOURCE_NAME = "h2";
	private static DynamicClass dynamicClass;

	static {
		final JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL( "jdbc:h2:file:h2db" );
		dataSource.setUser( "sa" );
		dataSource.setPassword( "" );
		dynamicClass = new DataSourceByNameProvider( DATASOURCE_NAME, dataSource );
	}

	public String getDataSourceName() {
		return DATASOURCE_NAME;
	}

	public XADataSource getDataSource(String dbName) throws SQLException {
		return dynamicClass.getDataSource( dbName );
	}
}
