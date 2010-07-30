package org.hibernate.search.test.integration.jbossjta.infra;

import java.sql.SQLException;
import javax.sql.XADataSource;

import com.arjuna.ats.internal.jdbc.DynamicClass;

/**
 * @author Emmanuel Bernard
 */
class DataSourceByNameProvider implements DynamicClass {
	private final XADataSource datasource;
	private final String name;

	public DataSourceByNameProvider(String name, XADataSource datasource) {
		this.name = name;
		this.datasource = datasource;
	}

	public XADataSource getDataSource(String dbName) throws SQLException {
		if ( name.equals( dbName ) ) {
			return datasource;
		}
		else {
			throw new IllegalArgumentException( "Datasource not found: " + dbName );
		}
	}
}