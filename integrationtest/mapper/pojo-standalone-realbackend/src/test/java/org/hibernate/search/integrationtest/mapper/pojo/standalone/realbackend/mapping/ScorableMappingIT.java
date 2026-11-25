package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ScorableMappingIT {
	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withSingleBackend(
			MethodHandles.lookup(), BackendConfigurations.simple() );

	@Test
	void unsupportedScorableAnnotationRejectedAtBootstrap() {
		assertThatThrownBy( () -> setupHelper.start().setup( AnnotatedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Hibernate Search encountered failures during bootstrap",
						"Scorable.YES is not supported", "Only numeric fields", "path '.title'" );
	}

	@Test
	void unsupportedScorableProgrammaticMappingRejectedAtBootstrap() {
		assertThatThrownBy( () -> setupHelper.start()
				.withConfiguration( builder -> builder.programmaticMapping().type( ProgrammaticEntity.class )
						.property( "title" ).genericField().scorable( Scorable.YES ) )
				.setup( ProgrammaticEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Hibernate Search encountered failures during bootstrap",
						"Scorable.YES is not supported", "Only numeric fields", "path '.title'" );
	}

	@Indexed
	static class AnnotatedEntity {
		@GenericField(scorable = Scorable.YES)
		String title;
	}

	@Indexed
	static class ProgrammaticEntity {
		String title;
	}
}
