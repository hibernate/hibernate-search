package org.hibernate.search.engine.search.reference.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TraitReferenceMappingTest {

	@MethodSource("traitNames")
	@ParameterizedTest
	void allTraitsPresent(String traitName) {
		assertThat( TraitReferenceMapping.instance().reference( traitName ) )
				.isNotNull();
	}

	private static Stream<Arguments> traitNames() {
		Set<String> traitNames = new HashSet<>();
		traitNames.addAll( traitNames( IndexFieldTraits.Predicates.class ) );
		traitNames.addAll( traitNames( IndexFieldTraits.Projections.class ) );
		traitNames.addAll( traitNames( IndexFieldTraits.Sorts.class ) );
		traitNames.addAll( traitNames( IndexFieldTraits.Aggregations.class ) );
		return traitNames.stream().map( Arguments::of );
	}

	private static Set<String> traitNames(Class<?> clazz) {
		Set<String> traits = new HashSet<>();
		for ( Field field : clazz.getDeclaredFields() ) {
			if ( java.lang.reflect.Modifier.isStatic( field.getModifiers() ) ) {
				try {
					traits.add( (String) field.get( null ) );
				}
				catch (IllegalAccessException e) {
					fail( "Unexpected exception: " + e );
				}
			}
		}
		return traits;
	}
}
