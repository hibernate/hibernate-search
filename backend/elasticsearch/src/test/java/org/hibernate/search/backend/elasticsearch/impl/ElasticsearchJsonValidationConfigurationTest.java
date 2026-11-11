package org.hibernate.search.backend.elasticsearch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ElasticsearchJsonValidationConfigurationTest {
	@Test
	void configuredValuePrecedesSystemPropertyAndDefault() {
		String key = JsonTreeValidation.INTERNAL_VALIDATION_PROPERTY;
		String utilityKey = JsonTreeValidation.INTERNAL_VALIDATION_SYSTEM_PROPERTY;
		String previous = System.getProperty( key );
		String previousUtility = System.getProperty( utilityKey );
		try {
			System.clearProperty( key );
			System.clearProperty( utilityKey );
			assertThat( resolve( Map.of() ).enabled() ).isFalse();
			assertThat( resolve( Map.of( "hibernate.search.backend.json.internal_validation", true ) ).enabled() )
					.isFalse();
			assertThat( resolve( Map.of( key, true ) ).enabled() ).isTrue();
			assertThat( resolve( Map.of( key, false ) ).enabled() ).isFalse();
			System.setProperty( key, "true" );
			assertThat( resolve( Map.of() ).enabled() ).isTrue();
			assertThat( resolve( Map.of( key, "false" ) ).enabled() ).isFalse();
			System.setProperty( key, "false" );
			assertThat( resolve( Map.of() ).enabled() ).isFalse();
			assertThat( resolve( Map.of( key, "true" ) ).enabled() ).isTrue();
			System.clearProperty( key );
			System.setProperty( utilityKey, "true" );
			assertThat( resolve( Map.of() ).enabled() ).isTrue();
			assertThat( JsonValidation.systemDefault().enabled() ).isTrue();
			assertThat( resolve( Map.of( key, false ) ).enabled() ).isFalse();
			System.setProperty( utilityKey, "false" );
			assertThat( resolve( Map.of() ).enabled() ).isFalse();
			assertThat( resolve( Map.of( key, true ) ).enabled() ).isTrue();
		}
		finally {
			restoreProperty( key, previous );
			restoreProperty( utilityKey, previousUtility );
		}
	}

	@Test
	void configuredInvalidValueReportsSearchConfigurationError() {
		assertThatThrownBy( () -> resolve( Map.of( JsonTreeValidation.INTERNAL_VALIDATION_PROPERTY, "invalid" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( JsonTreeValidation.INTERNAL_VALIDATION_PROPERTY );
	}

	@Test
	void independentContextsKeepTheirPoliciesAcrossProviderServices() {
		var disabled = resolve( Map.of( JsonTreeValidation.INTERNAL_VALIDATION_PROPERTY, false ) );
		var enabled = resolve( Map.of( JsonTreeValidation.INTERNAL_VALIDATION_PROPERTY, true ) );
		var disabledProvider = JsonProviderHelper.create( false, disabled );
		var enabledProvider = JsonProviderHelper.create( true, enabled );
		assertThat( disabledProvider.getCodec().validation().enabled() ).isFalse();
		assertThat( disabledProvider.getCodecNoSerializeNulls().validation().enabled() ).isFalse();
		assertThat( enabledProvider.getCodec().validation().enabled() ).isTrue();
		assertThat( enabledProvider.getCodecNoSerializeNulls().validation().enabled() ).isTrue();
		assertThat( JsonProviderHelper.createUserFacingCodec( disabled ).validation().enabled() ).isFalse();
		assertThat( JsonProviderHelper.createUserFacingCodec( enabled ).validation().enabled() ).isTrue();
		assertThat( disabledProvider.getCodec().validation().enabled() ).isFalse();
	}

	private static JsonValidation resolve(Map<String, ?> properties) {
		return ElasticsearchBackendFactory.resolveJsonValidation(
				AllAwareConfigurationPropertySource.fromMap( properties ).withMask( "hibernate.search" ) );
	}

	private static void restoreProperty(String key, String previous) {
		if ( previous == null ) {
			System.clearProperty( key );
		}
		else {
			System.setProperty( key, previous );
		}
	}
}
