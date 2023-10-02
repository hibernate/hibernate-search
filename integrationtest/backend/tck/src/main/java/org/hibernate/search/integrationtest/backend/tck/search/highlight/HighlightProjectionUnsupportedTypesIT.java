/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests basic behavior of highlight projections common to all unsupported types,
 * i.e. error messages.
 */

class HighlightProjectionUnsupportedTypesIT<F> {

	private static Stream<FieldTypeDescriptor<?,
			? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> unsupportedTypeDescriptors() {
		return FieldTypeDescriptor.getAll().stream()
				.filter( typeDescriptor -> !AnalyzedStringFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) );
	}

	public static List<? extends Arguments> params() {
		List<Arguments> parameters = new ArrayList<>();
		unsupportedTypeDescriptors().forEach( fieldTypeDescriptor -> parameters.add( Arguments.of( fieldTypeDescriptor ) ) );
		return parameters;
	}

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void trait(FieldTypeDescriptor<F, ?> fieldTypeDescriptor) {
		String fieldPath = getFieldPath( fieldTypeDescriptor );

		assertThat( index.toApi().descriptor().field( fieldPath ) )
				.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
						.as( "traits of field '" + fieldPath + "'" )
						.doesNotContain( IndexFieldTraits.Projections.HIGHLIGHT ) );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void use(FieldTypeDescriptor<F, ?> fieldTypeDescriptor) {
		StubMappingScope scope = index.createScope();
		String absoluteFieldPath = getFieldPath( fieldTypeDescriptor );

		assertThatThrownBy(
				() -> scope.query().select( f -> f.highlight( absoluteFieldPath ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:highlight' on field '" + absoluteFieldPath + "'",
						"'projection:highlight' is not available for fields of this type"
				);
	}

	private String getFieldPath(FieldTypeDescriptor<F, ?> fieldTypeDescriptor) {
		return index.binding().fieldModels.get( fieldTypeDescriptor ).relativeFieldName;
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( unsupportedTypeDescriptors(), root, "", c -> {
				if ( c instanceof StringIndexFieldTypeOptionsStep<?> ) {
					( (StringIndexFieldTypeOptionsStep<?>) c ).highlightable( Collections.singleton( Highlightable.ANY ) );
				}
			} );
		}
	}

}
