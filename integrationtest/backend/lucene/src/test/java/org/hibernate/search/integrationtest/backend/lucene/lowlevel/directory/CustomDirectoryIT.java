/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProviderInitializationContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

public class CustomDirectoryIT extends AbstractDirectoryIT {

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	@PortedFromSearch5(original = "org.hibernate.search.test.directoryProvider.DirectoryLifecycleTest.testLifecycle")
	public void valid() {
		setup( CustomDirectoryProvider.class, c -> c.withBackendProperty(
				"directory." + CustomDirectoryProvider.CONFIGURATION_PROPERTY_KEY_RADICAL,
				CustomDirectoryProvider.CONFIGURATION_PROPERTY_EXPECTED_VALUE
		) );

		assertThat( staticCounters.get( CustomDirectoryProvider.CONSTRUCTOR_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.INITIALIZE_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.CREATE_DIRECTORY_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_START_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_CLOSE_COUNTER_KEY ) ).isEqualTo( 0 );
		assertThat( staticCounters.get( CustomDirectoryProvider.CLOSE_COUNTER_KEY ) ).isEqualTo( 0 );

		checkIndexingAndQuerying();
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_GET_COUNTER_KEY ) ).isGreaterThan( 1 );

		searchIntegration.close();

		assertThat( staticCounters.get( CustomDirectoryProvider.CONSTRUCTOR_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.INITIALIZE_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.CREATE_DIRECTORY_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_START_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_CLOSE_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.CLOSE_COUNTER_KEY ) ).isEqualTo( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void invalid() {
		String invalidDirectoryType = "someInvalidDirectoryType";
		Assertions.assertThatThrownBy( () ->
				setup( "someInvalidDirectoryType", c -> c )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.backendContext( BACKEND_NAME )
						.failure(
								"Unable to convert configuration property 'hibernate.search.backends." + BACKEND_NAME + ".directory.type'"
										+ " with value '" + invalidDirectoryType + "'",
								"Unable to find " + DirectoryProvider.class.getName() + " implementation class: "
										+ invalidDirectoryType
						)
						.build()
				);
	}

	public static class CustomDirectoryProvider implements DirectoryProvider {

		private static String CONFIGURATION_PROPERTY_KEY_RADICAL = "myConfigurationProperty";
		private static String CONFIGURATION_PROPERTY_EXPECTED_VALUE = "someValue";

		private static final StaticCounters.Key CONSTRUCTOR_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key INITIALIZE_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key CREATE_DIRECTORY_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key DIRECTORY_HOLDER_START_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key DIRECTORY_HOLDER_GET_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key DIRECTORY_HOLDER_CLOSE_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key CLOSE_COUNTER_KEY = StaticCounters.createKey();

		public CustomDirectoryProvider() {
			StaticCounters.get().increment( CONSTRUCTOR_COUNTER_KEY );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void initialize(DirectoryProviderInitializationContext context) {
			StaticCounters.get().increment( INITIALIZE_COUNTER_KEY );
			assertThat( context ).isNotNull();
			Optional<?> actualConfigurationPropertyValue = context.configurationPropertySource()
					.get( CONFIGURATION_PROPERTY_KEY_RADICAL );
			assertThat( (Optional) actualConfigurationPropertyValue )
					.contains( CONFIGURATION_PROPERTY_EXPECTED_VALUE );
		}

		@Override
		public void close() {
			StaticCounters.get().increment( CLOSE_COUNTER_KEY );
		}

		@Override
		public DirectoryHolder createDirectoryHolder(DirectoryCreationContext context) {
			StaticCounters.get().increment( CREATE_DIRECTORY_COUNTER_KEY );
			assertThat( context ).isNotNull();
			assertThat( context.indexName() ).isEqualTo( index.name() );
			return new DirectoryHolder() {
				Directory directory;
				@Override
				public void start() {
					StaticCounters.get().increment( DIRECTORY_HOLDER_START_COUNTER_KEY );
					directory = new ByteBuffersDirectory();
				}

				@Override
				public void close() throws IOException {
					StaticCounters.get().increment( DIRECTORY_HOLDER_CLOSE_COUNTER_KEY );
					directory.close();
				}

				@Override
				public Directory get() {
					StaticCounters.get().increment( DIRECTORY_HOLDER_GET_COUNTER_KEY );
					return directory;
				}
			};
		}
	}
}
