/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.test.testsupport.V5MigrationHelperOrmSetupHelper;
import org.hibernate.search.test.util.BackendTestHelper;
import org.hibernate.search.test.util.TestConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.store.Directory;

/**
 * Base class for Hibernate Search tests using Hibernate ORM and Junit 4.
 *
 * @author Hardy Ferentschik
 */
public abstract class SearchTestBase implements TestResourceManager, TestConfiguration {

	@RegisterExtension
	public final V5MigrationHelperOrmSetupHelper setupHelper = V5MigrationHelperOrmSetupHelper.create();

	// access only via getter, since instance gets lazily initialized
	private DefaultTestResourceManager testResourceManager;

	// access only via getter, since instance gets lazily initialized
	private BackendTestHelper backendTestHelper;

	@BeforeEach
	protected void setUp() throws Exception {
		getTestResourceManager().openSessionFactory();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		getTestResourceManager().defaultTearDown();
	}

	@Override
	public final void openSessionFactory() {
		getTestResourceManager().openSessionFactory();
	}

	@Override
	public final void closeSessionFactory() {
		getTestResourceManager().closeSessionFactory();
	}

	@Override
	public final SessionFactory getSessionFactory() {
		return getTestResourceManager().getSessionFactory();
	}

	@Override
	public final SearchFactory getSearchFactory() {
		return getTestResourceManager().getSearchFactory();
	}

	@Override
	public final Session openSession() {
		return getTestResourceManager().openSession();
	}

	@Override
	public final Session getSession() {
		return getTestResourceManager().getSession();
	}

	@Override
	public void ensureIndexesAreEmpty() throws IOException {
		getTestResourceManager().ensureIndexesAreEmpty();
	}

	@Override
	public Path getBaseIndexDir() {
		return getTestResourceManager().getBaseIndexDir();
	}

	@Override
	public void configure(Map<String, Object> settings) {
		//Empty by default
	}

	@Override
	public Set<String> multiTenantIds() {
		//Empty by default; specify more than one tenant to enable multi-tenancy
		return Collections.emptySet();
	}

	protected Directory openDirectoryForIndex(String indexName) {
		return getBackendTestHelper().openDirectoryForIndex( indexName );
	}

	protected int getNumberOfDocumentsInIndex(String indexName) {
		return getBackendTestHelper().getNumberOfDocumentsInIndex( indexName );
	}

	protected int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		return getBackendTestHelper().getNumberOfDocumentsInIndexByQuery(
				indexName, fieldName, value
		);
	}

	// synchronized due to lazy initialization
	private synchronized DefaultTestResourceManager getTestResourceManager() {
		if ( testResourceManager == null ) {
			testResourceManager = new DefaultTestResourceManager( this, setupHelper );
		}
		return testResourceManager;
	}

	private BackendTestHelper getBackendTestHelper() {
		if ( backendTestHelper == null ) {
			backendTestHelper = BackendTestHelper.getInstance( getTestResourceManager() );
		}

		return backendTestHelper;
	}
}
