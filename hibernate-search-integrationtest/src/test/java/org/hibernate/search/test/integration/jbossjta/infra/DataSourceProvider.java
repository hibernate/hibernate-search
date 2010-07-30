package org.hibernate.search.test.integration.jbossjta.infra;

import java.sql.SQLException;
import javax.sql.XADataSource;

import com.arjuna.ats.internal.jdbc.DynamicClass;

/**
 * Bind statically a DataSource to the name "datasource"
 *
 * @author Emmanuel Bernard
 */
public class DataSourceProvider implements DynamicClass {
	private static String DATASOURCE_NAME = "datasource";
	private static DynamicClass dynamicClass;

	static void initialize(XADataSource dataSource) {
		dynamicClass = new DataSourceByNameProvider( DATASOURCE_NAME, dataSource );
	}

	public String getDataSourceName() {
		return DATASOURCE_NAME;
	}

	public XADataSource getDataSource(String dbName) throws SQLException {
		return dynamicClass.getDataSource( dbName );
	}
}
