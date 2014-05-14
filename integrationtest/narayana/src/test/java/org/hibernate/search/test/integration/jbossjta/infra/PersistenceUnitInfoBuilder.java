/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
