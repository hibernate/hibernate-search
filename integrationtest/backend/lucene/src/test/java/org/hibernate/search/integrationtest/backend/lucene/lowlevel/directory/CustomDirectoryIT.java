/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Optional;

import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
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
		setup( CustomDirectoryProvider.class, c -> c.expectCustomBeans()
				.withBackendProperty( "directory." + CustomDirectoryProvider.CONFIGURATION_PROPERTY_KEY_RADICAL,
						CustomDirectoryProvider.CONFIGURATION_PROPERTY_EXPECTED_VALUE ) );

		assertThat( staticCounters.get( CustomDirectoryProvider.CONSTRUCTOR_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.CREATE_DIRECTORY_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_START_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_CLOSE_COUNTER_KEY ) ).isEqualTo( 0 );

		checkIndexingAndQuerying();
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_GET_COUNTER_KEY ) ).isGreaterThan( 1 );

		mapping.close();

		assertThat( staticCounters.get( CustomDirectoryProvider.CONSTRUCTOR_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.CREATE_DIRECTORY_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_START_COUNTER_KEY ) ).isEqualTo( 1 );
		assertThat( staticCounters.get( CustomDirectoryProvider.DIRECTORY_HOLDER_CLOSE_COUNTER_KEY ) ).isEqualTo( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3440")
	public void invalid() {
		String invalidDirectoryType = "someInvalidDirectoryType";
		assertThatThrownBy( () -> setup( "someInvalidDirectoryType", c -> c.expectCustomBeans() ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.indexContext( index.name() )
						.failure(
								"Invalid value for configuration property 'hibernate.search.backend.directory.type': '"
										+ invalidDirectoryType + "'",
								"No beans defined for type '" + DirectoryProvider.class.getName()
										+ "' and name '" + invalidDirectoryType
										+ "' in Hibernate Search's internal registry",
								"Unable to load class '" + invalidDirectoryType + "'"
						) );
	}

	public static class CustomDirectoryProvider implements DirectoryProvider {

		private static final String CONFIGURATION_PROPERTY_KEY_RADICAL = "myConfigurationProperty";
		private static final String CONFIGURATION_PROPERTY_EXPECTED_VALUE = "someValue";

		private static final StaticCounters.Key CONSTRUCTOR_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key CREATE_DIRECTORY_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key DIRECTORY_HOLDER_START_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key DIRECTORY_HOLDER_GET_COUNTER_KEY = StaticCounters.createKey();
		private static final StaticCounters.Key DIRECTORY_HOLDER_CLOSE_COUNTER_KEY = StaticCounters.createKey();

		public CustomDirectoryProvider() {
			StaticCounters.get().increment( CONSTRUCTOR_COUNTER_KEY );
		}

		@Override
		@SuppressWarnings("unchecked") // Workaround for assertThat(Optional) not taking wildcard type into account like assertThat(Collection) does
		public DirectoryHolder createDirectoryHolder(DirectoryCreationContext context) {
			StaticCounters.get().increment( CREATE_DIRECTORY_COUNTER_KEY );
			assertThat( context ).isNotNull();
			assertThat( context.indexName() ).isEqualTo( index.name() );
			Optional<?> actualConfigurationPropertyValue = context.configurationPropertySource()
					.get( CONFIGURATION_PROPERTY_KEY_RADICAL );
			assertThat( (Optional<Object>) actualConfigurationPropertyValue )
					.contains( CONFIGURATION_PROPERTY_EXPECTED_VALUE );
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
