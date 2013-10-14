/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
		dsw.setProperty( TransactionalDriver.userName, user);
		dsw.setProperty( TransactionalDriver.password, password );
		return dsw;
	}
}
