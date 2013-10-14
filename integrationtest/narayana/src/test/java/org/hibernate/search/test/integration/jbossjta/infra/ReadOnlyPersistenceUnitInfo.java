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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

/**
 * @author Emmanuel Bernard
 */
class ReadOnlyPersistenceUnitInfo implements PersistenceUnitInfo {
	private final String persistenceUnitName;
	private final String persistenceProviderClassName;
	private final PersistenceUnitTransactionType transactionType;
	private final DataSource jtaDataSource;
	private final URL persistenceUnitRootUrl;
	private final List<String> managedClassNames;
	private final boolean excludeUnlistedClasses;
	private final SharedCacheMode sharedCacheMode;
	private final ValidationMode validationMode;
	private final Properties properties;
	private final String persistenceXMLSchemaVersion;
	private final ClassLoader classLoader;

	public ReadOnlyPersistenceUnitInfo(String persistenceUnitName, String persistenceProviderClassName, PersistenceUnitTransactionType transactionType, DataSource jtaDataSource, URL persistenceUnitRootUrl, List<String> managedClassNames, boolean excludeUnlistedClasses, SharedCacheMode sharedCacheMode, ValidationMode validationMode, Properties properties, String persistenceXMLSchemaVersion, ClassLoader classLoader) {
		this.persistenceUnitName = persistenceUnitName;
		this.persistenceProviderClassName = persistenceProviderClassName;
		this.transactionType = transactionType;
		this.jtaDataSource = jtaDataSource;
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
		this.managedClassNames = managedClassNames;
		this.excludeUnlistedClasses = excludeUnlistedClasses;
		this.sharedCacheMode = sharedCacheMode;
		this.validationMode = validationMode;
		this.properties = properties;
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
		this.classLoader = classLoader;
	}

	@Override
	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}

	@Override
	public String getPersistenceProviderClassName() {
		return persistenceProviderClassName;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	@Override
	public DataSource getJtaDataSource() {
		return jtaDataSource;
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return null;
	}

	@Override
	public List<String> getMappingFileNames() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitRootUrl;
	}

	@Override
	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ValidationMode getValidationMode() {
		return validationMode;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return persistenceXMLSchemaVersion;
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
