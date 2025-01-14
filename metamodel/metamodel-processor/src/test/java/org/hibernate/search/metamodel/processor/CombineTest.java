/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;

import java.util.List;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.metamodel.processor.writer.impl.TraitReferenceDetails;
import org.hibernate.search.metamodel.processor.writer.impl.TraitReferenceMapping;
import org.hibernate.search.metamodel.processor.writer.impl.TypedFieldReferenceDetails;
import org.hibernate.search.metamodel.processor.writer.impl.ValueFieldReferenceDetails;

import org.junit.jupiter.api.Test;

public class CombineTest {

	@Test
	void name() {
		TraitReferenceMapping instance = TraitReferenceMapping.instance();

		TraitReferenceDetails match = instance.reference( IndexFieldTraits.Predicates.MATCH );
		//TraitReferenceDetails field = instance.reference( IndexFieldTraits.Projections.FIELD );

		TypedFieldReferenceDetails details = TypedFieldReferenceDetails.of( List.of( match ) );
		System.err.println( details.formatted() );
		System.err.println( details.constructorCall( "valueModel", "in", "out" ) );

		System.err.println( new ValueFieldReferenceDetails( details ).formatted() );
	}

}
