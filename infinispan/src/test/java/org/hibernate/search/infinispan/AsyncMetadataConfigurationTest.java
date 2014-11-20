/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan;

import java.lang.reflect.Field;

import org.apache.lucene.store.Directory;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.infinispan.impl.InfinispanDirectoryProvider;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;


/**
 * Verifies the metadata_writes_async configuration properties is applied to the Infinispan Directory.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 * @since 5.0
 */
@TestForIssue(jiraKey = "HSEARCH-1728")
public class AsyncMetadataConfigurationTest {

	@Rule
	public SearchFactoryHolder asyncMetadata = new SearchFactoryHolder( SimpleEmail.class )
			.withProperty( "hibernate.search.default.directory_provider", "infinispan" )
			.withProperty( "hibernate.search.infinispan.configuration_resourcename", "localonly-infinispan.xml" )
			.withProperty( "hibernate.search.default.metadata_writes_async", "true" );

	@Rule
	public SearchFactoryHolder defaultMetadata = new SearchFactoryHolder( SimpleEmail.class )
			.withProperty( "hibernate.search.default.directory_provider", "infinispan" )
			.withProperty( "hibernate.search.infinispan.configuration_resourcename", "localonly-infinispan.xml" );

	@Rule
	public SearchFactoryHolder syncMetadata = new SearchFactoryHolder( SimpleEmail.class )
			.withProperty( "hibernate.search.default.directory_provider", "infinispan" )
			.withProperty( "hibernate.search.infinispan.configuration_resourcename", "localonly-infinispan.xml" )
			.withProperty( "hibernate.search.default.metadata_writes_async", "false" );

	@Test
	public void verifyAsyncMetadataOptionApplied() throws Exception {
		verifyAsyncIs( asyncMetadata, true );
	}

	@Test
	public void verifyAsyncMetadataDisabledByDefault() throws Exception {
		verifyAsyncIs( defaultMetadata, false );
	}

	@Test
	public void verifyAsyncMetadataOptionExplicitDisabled() throws Exception {
		verifyAsyncIs( syncMetadata, false );
	}

	private static void verifyAsyncIs(SearchFactoryHolder metadata, boolean expectation) throws Exception {
		EntityIndexBinding indexBinding = metadata.getSearchFactory().getIndexBinding( SimpleEmail.class );
		IndexManager indexManager = indexBinding.getIndexManagers()[0];
		Assert.assertTrue( indexManager instanceof DirectoryBasedIndexManager );
		DirectoryBasedIndexManager directoryIm = (DirectoryBasedIndexManager) indexManager;
		DirectoryProvider directoryProvider = directoryIm.getDirectoryProvider();
		Assert.assertTrue( directoryProvider instanceof InfinispanDirectoryProvider );
		Directory infinispanDirectory = directoryProvider.getDirectory();
		//From here we need reflection as all needed references are not visible:
		expectClassType( "org.infinispan.lucene.impl.DirectoryLuceneV4", infinispanDirectory );
		Object directoryImplementor = getValueFromField( infinispanDirectory, "impl" );
		expectClassType( "org.infinispan.lucene.impl.DirectoryImplementor", directoryImplementor );
		Object fileOps = getValueFromField( directoryImplementor, "fileOps" );
		expectClassType( "org.infinispan.lucene.impl.FileListOperations", fileOps );
		Object writeAsyncValue = getValueFromField( fileOps, "writeAsync" );
		Assert.assertTrue( writeAsyncValue instanceof Boolean );
		Boolean value = (Boolean) writeAsyncValue;
		Assert.assertEquals( expectation, value.booleanValue() );
	}

	private static Object getValueFromField(Object instance, String fieldName) throws Exception {
		Field field = instance.getClass().getDeclaredField( fieldName );
		Assert.assertNotNull( field );
		field.setAccessible( true );
		Object object = field.get( instance );
		Assert.assertNotNull( object );
		return object;
	}

	private static void expectClassType(String expectedClassName, Object instance) {
		Assert.assertEquals( expectedClassName, instance.getClass().getName() );
	}

}
