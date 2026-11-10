package org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.json.JsonHandler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ObjectNode;

class JavaModulePathIT {

	@BeforeAll
	@AfterAll
	static void clear() {
		SimulatedDatastore.clear();
	}

	/*
	 * Test that the service successfully uses Hibernate Search in the module path.
	 * We don't really care about the features themselves,
	 * but the easiest way to check this is to just use Hibernate Search features and see if it works.
	 */
	@Test
	void test() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );
		checkIsInModulePath( SearchMapping.class );

		try ( AuthorService service = new AuthorService() ) {
			service.add( "foo" );
			service.add( "bar" );
			service.add( "foo bar" );
			assertThat( service.search( "foo" ) ).hasSize( 2 );
		}
	}

	@Test
	void jsonApiIsTransitiveAndImplementationIsEncapsulated() {
		checkIsInModulePath( JsonApiExample.class );
		checkIsInModulePath( ObjectNode.class );
		checkIsInModulePath( JsonHandler.class );
		assertThat( JsonApiExample.requestValue() ).isEqualTo( 42L );
		assertThat( JsonApiExample.deprecatedGsonValue() ).isEqualTo( 42L );
		ObjectNode custom = JsonApiExample.customHandlerResult();
		assertThat( custom.get( "custom" ).booleanValue() ).isTrue();
		assertThat( custom.get( "name" ).asString() ).isEqualTo( "custom source" );
		assertThat( Double.doubleToRawLongBits( JsonApiExample.nativeScalar().doubleValue() ) )
				.isEqualTo( Double.doubleToRawLongBits( -0.0d ) );
		assertThat( JsonHandler.class.getModule().getDescriptor().requires() )
				.anySatisfy( requirement -> {
					assertThat( requirement.name() ).isEqualTo( "tools.jackson.databind" );
					assertThat( requirement.modifiers() )
							.doesNotContain( ModuleDescriptor.Requires.Modifier.TRANSITIVE );
				} );
		assertThat( JsonHandler.class.getModule().isExported( "org.hibernate.search.util.json.internal.spi",
				JsonApiExample.class.getModule() ) ).isFalse();
		assertThat( JsonHandler.class.getModule().getDescriptor().opens() ).isEmpty();
		assertThat( ElasticsearchRequest.class.getModule()
				.isExported( "org.hibernate.search.backend.elasticsearch.json.impl", JsonApiExample.class.getModule() ) )
				.isFalse();
		assertThat( JsonHandler.class.getModule().getDescriptor().requires() )
				.anySatisfy( requirement -> {
					assertThat( requirement.name() ).isEqualTo( "tools.jackson.core" );
					assertThat( requirement.modifiers() )
							.doesNotContain( ModuleDescriptor.Requires.Modifier.TRANSITIVE );
				} );
	}

	private void checkIsInModulePath(Class<?> clazz) {
		assertThat( clazz.getModule().isNamed() )
				.as( clazz + " should be part of a named module - there is a problem in test setup" )
				.isTrue();
	}
}
