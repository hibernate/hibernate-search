/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the metadata_writes_async configuration properties is applied to the Infinispan Directory.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 * @since 5.0
 */
@RunWith(BMUnitRunner.class)
@TestForIssue(jiraKey = "HSEARCH-1728")
public class AsyncMetadataConfigurationTest {

	@Test
	@BMRule(targetClass = "org.infinispan.lucene.impl.DirectoryBuilderImpl",
			targetMethod = "create",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "assertBooleanValue($0.writeFileListAsync, true); countInvocation();",
			name = "verifyAsyncMetadataOptionApplied")
	public void verifyAsyncMetadataOptionApplied() throws Exception {
		buildSearchFactoryWithAsyncOption( true );
	}

	@Test
	@BMRule(targetClass = "org.infinispan.lucene.impl.DirectoryBuilderImpl",
			targetMethod = "create",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "assertBooleanValue($0.writeFileListAsync, false); countInvocation();",
			name = "verifyAsyncMetadataDisabledByDefault")
	public void verifyAsyncMetadataDisabledByDefault() throws Exception {
		buildSearchFactoryWithAsyncOption( null );
	}


	@Test
	@BMRule(targetClass = "org.infinispan.lucene.impl.DirectoryBuilderImpl",
			targetMethod = "create",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "assertBooleanValue($0.writeFileListAsync, false); countInvocation();",
			name = "verifyAsyncMetadataOptionExplicitDisabled")
	public void verifyAsyncMetadataOptionExplicitDisabled() throws Exception {
		buildSearchFactoryWithAsyncOption( false );
	}

	private void buildSearchFactoryWithAsyncOption(Boolean async) {
		SearchConfigurationForTest configuration = new HibernateManualConfiguration()
				.addClass( SimpleEmail.class )
				.addProperty( "hibernate.search.default.directory_provider", "infinispan" )
				.addProperty( "hibernate.search.infinispan.configuration_resourcename", "localonly-infinispan.xml" );
		if ( async != null ) {
			configuration.addProperty( "hibernate.search.default.write_metadata_async", async.toString() );
		}

		new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
		assertEquals( "The directory provider was not started", 1, BytemanHelper.getAndResetInvocationCount() );
	}
}
