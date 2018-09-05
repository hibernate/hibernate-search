/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jbossjta.infra;

import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.jdbc.TransactionalDriver;

public class JBossTADataSourceBuilder {
	private String user;
	private String password;
	//by default no timeout
	private int timeout = 0;
	private XADataSource xaDataSource;

	public JBossTADataSourceBuilder setUser(String user) {
		this.user = user;
		return this;
	}

	public JBossTADataSourceBuilder setPassword(String password) {
		this.password = password;
		return this;
	}

	public JBossTADataSourceBuilder setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public JBossTADataSourceBuilder setXADataSource(XADataSource xaDataSource) {
		this.xaDataSource = xaDataSource;
		return this;
	}

	public DataSource createDataSource() throws SQLException {
		TxControl.setDefaultTimeout( timeout );
		DataSourceProvider.initialize( xaDataSource );
		DataSourceProvider dsProvider = new DataSourceProvider();
		final XADataSource dataSource = dsProvider.getDataSource( dsProvider.getDataSourceName() );
		XADataSourceWrapper dsw = new XADataSourceWrapper(
				dsProvider.getDataSourceName(),
				dataSource
		);
		dsw.setProperty( TransactionalDriver.dynamicClass, DataSourceProvider.class.getName() );
		dsw.setProperty( TransactionalDriver.userName, user );
		dsw.setProperty( TransactionalDriver.password, password );
		return dsw;
	}
}
