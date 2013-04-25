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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

public class PersistenceUnitInfoBuilder {
	private String persistenceUnitName;
	private String persistenceProviderClassName;
	private PersistenceUnitTransactionType transactionType;
	private DataSource jtaDataSource;
	private URL persistenceUnitRootUrl;
	private List<String> managedClassNames;
	private boolean excludeUnlistedClasses;
	private SharedCacheMode sharedCacheMode;
	private ValidationMode validationMode;
	private Properties properties;
	private String persistenceXMLSchemaVersion;
	private ClassLoader classLoader;

	public PersistenceUnitInfoBuilder() {
		classLoader = Thread.currentThread().getContextClassLoader();
		persistenceUnitRootUrl = classLoader.getResource( "persistence.xml" );
		managedClassNames = new ArrayList<String>();
		properties = new Properties( );
	}

	public PersistenceUnitInfoBuilder setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
		return this;
	}

	public PersistenceUnitInfoBuilder setPersistenceProviderClassName(String persistenceProviderClassName) {
		this.persistenceProviderClassName = persistenceProviderClassName;
		return this;
	}

	public PersistenceUnitInfoBuilder setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
		return this;
	}

	public PersistenceUnitInfoBuilder setJtaDataSource(DataSource jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
		return this;
	}

	public PersistenceUnitInfoBuilder setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
		return this;
	}

	public PersistenceUnitInfoBuilder addManagedClassNames(String managedClassName) {
		this.managedClassNames.add( managedClassName );
		return this;
	}

	public PersistenceUnitInfoBuilder setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
		return this;
	}

	public PersistenceUnitInfoBuilder setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
		return this;
	}

	public PersistenceUnitInfoBuilder setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
		return this;
	}

	public PersistenceUnitInfoBuilder addProperty(String key, String value) {
		this.properties.setProperty( key, value );
		return this;
	}

	public PersistenceUnitInfoBuilder setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
		return this;
	}

	public PersistenceUnitInfoBuilder setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return this;
	}

	public ReadOnlyPersistenceUnitInfo create() {
		return new ReadOnlyPersistenceUnitInfo(
				persistenceUnitName,
				persistenceProviderClassName,
				transactionType,
				jtaDataSource,
				persistenceUnitRootUrl,
				managedClassNames,
				excludeUnlistedClasses,
				sharedCacheMode,
				validationMode,
				properties,
				persistenceXMLSchemaVersion,
				classLoader
		);
	}
}
