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

	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}

	public String getPersistenceProviderClassName() {
		return persistenceProviderClassName;
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public DataSource getJtaDataSource() {
		return jtaDataSource;
	}

	public DataSource getNonJtaDataSource() {
		return null;
	}

	public List<String> getMappingFileNames() {
		return Collections.EMPTY_LIST;
	}

	public List<URL> getJarFileUrls() {
		return Collections.EMPTY_LIST;
	}

	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitRootUrl;
	}

	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	public boolean excludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public Properties getProperties() {
		return properties;
	}

	public String getPersistenceXMLSchemaVersion() {
		return persistenceXMLSchemaVersion;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void addTransformer(ClassTransformer transformer) {
	}

	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}
