/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.writer.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;

import org.junit.jupiter.api.Test;

class TraitReferenceDetailsTest {
	@Test
	void combine() {
		TraitReferenceMapping instance = TraitReferenceMapping.instance();

		TraitReferenceDetails match = instance.reference( IndexFieldTraits.Predicates.MATCH );
		TraitReferenceDetails field = instance.reference( IndexFieldTraits.Projections.FIELD );

		TypedFieldReferenceDetails details = TypedFieldReferenceDetails.of( List.of( match, field ) );
		String referenceClassText = details.formatted();
		assertThat( referenceClassText ).contains( "TypedFieldReferenceP2R1",
				"MatchPredicateFieldReference", "FieldProjectionFieldReference" );

		assertThat( details.constructorCall( "valueModel", "in", "out" ) )
				.isEqualTo( "new TypedFieldReferenceP2R1<>(absolutePath, scopeRootType, valueModel, in, out);" );

		assertThat( new ValueFieldReferenceDetails( details ).formatted() ).contains( "ValueFieldReferenceP2R1",
				"TypedFieldReferenceP2R1", "mapping", "raw", "string" );
	}
}
