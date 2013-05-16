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
