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
 * @author Emmanuel Bernard
 */
class DataSourceByNameProvider implements DynamicClass {
	private final XADataSource datasource;
	private final String name;

	public DataSourceByNameProvider(String name, XADataSource datasource) {
		this.name = name;
		this.datasource = datasource;
	}

	@Override
	public XADataSource getDataSource(String dbName) throws SQLException {
		if ( name.equals( dbName ) ) {
			return datasource;
		}
		else {
			throw new IllegalArgumentException( "Datasource not found: " + dbName );
		}
	}
}
