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
