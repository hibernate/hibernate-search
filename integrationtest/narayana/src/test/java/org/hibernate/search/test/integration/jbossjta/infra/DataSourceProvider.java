/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

	@Override
	public XADataSource getDataSource(String dbName) throws SQLException {
		return dynamicClass.getDataSource( dbName );
	}
}
