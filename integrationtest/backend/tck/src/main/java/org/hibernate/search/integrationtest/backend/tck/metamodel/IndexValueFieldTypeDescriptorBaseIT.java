/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.TermsAggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.NormalizedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.VectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Basic tests for value field type descriptor features, for every relevant field type.
 */
@TestForIssue(jiraKey = "HSEARCH-3589")
class IndexValueFieldTypeDescriptorBaseIT {

	public static List<? extends Arguments> params() {
		return FieldTypeDescriptor.getAll().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private SimpleMappedIndex<IndexBinding> index;

	public void init(FieldTypeDescriptor<?, ?> fieldType) {
		index = SimpleMappedIndex.of(
				root -> new IndexBinding( root, fieldType ) );
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isSearchable(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		assertThat( getTypeDescriptor( "default" ) )
				.returns( true, IndexValueFieldTypeDescriptor::searchable );
		assertThat( getTypeDescriptor( "searchable" ) )
				.returns( true, IndexValueFieldTypeDescriptor::searchable );
		assertThat( getTypeDescriptor( "nonSearchable" ) )
				.returns( false, IndexValueFieldTypeDescriptor::searchable );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isSortable(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		boolean projectable = TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault();

		assertThat( getTypeDescriptor( "default" ) )
				.returns( GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) ? projectable : false,
						IndexValueFieldTypeDescriptor::sortable );
		if ( isSortSupported( fieldType ) ) {
			assertThat( getTypeDescriptor( "sortable" ) )
					.returns( true, IndexValueFieldTypeDescriptor::sortable );
		}
		assertThat( getTypeDescriptor( "nonSortable" ) )
				.returns( GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) ? projectable : false,
						IndexValueFieldTypeDescriptor::sortable );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isProjectable(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		boolean projectable = TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault();
		assertThat( getTypeDescriptor( "default" ) )
				.returns( projectable, IndexValueFieldTypeDescriptor::projectable );
		assertThat( getTypeDescriptor( "projectable" ) )
				.returns( true, IndexValueFieldTypeDescriptor::projectable );
		assertThat( getTypeDescriptor( "nonProjectable" ) )
				.returns( false, IndexValueFieldTypeDescriptor::projectable );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void isAggregable(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		assertThat( getTypeDescriptor( "default" ) )
				.returns( false, IndexValueFieldTypeDescriptor::aggregable );
		if ( isAggregationSupported( fieldType ) ) {
			assertThat( getTypeDescriptor( "aggregable" ) )
					.returns( true, IndexValueFieldTypeDescriptor::aggregable );
		}
		assertThat( getTypeDescriptor( "nonAggregable" ) )
				.returns( false, IndexValueFieldTypeDescriptor::aggregable );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void dslArgumentClass(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		assertThat( getTypeDescriptor( "default" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::dslArgumentClass );
		assertThat( getTypeDescriptor( "dslConverter" ) )
				.returns( ValueWrapper.class, IndexValueFieldTypeDescriptor::dslArgumentClass );
		assertThat( getTypeDescriptor( "projectionConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::dslArgumentClass );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void projectedValueClass(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		assertThat( getTypeDescriptor( "default" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::projectedValueClass );
		assertThat( getTypeDescriptor( "dslConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::projectedValueClass );
		assertThat( getTypeDescriptor( "projectionConverter" ) )
				.returns( ValueWrapper.class, IndexValueFieldTypeDescriptor::projectedValueClass );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void valueClass(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		assertThat( getTypeDescriptor( "default" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::valueClass );
		assertThat( getTypeDescriptor( "dslConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::valueClass );
		assertThat( getTypeDescriptor( "projectionConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::valueClass );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void searchAnalyzerName(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		IndexValueFieldTypeDescriptor typeDescriptor = getTypeDescriptor( "default" );

		Optional<String> searchAnalyzerName = typeDescriptor.searchAnalyzerName();

		if ( fieldType instanceof AnalyzedStringFieldTypeDescriptor ) {
			assertThat( searchAnalyzerName ).contains( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name );
		}
		else {
			assertThat( searchAnalyzerName ).isEmpty();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void normalizerName(FieldTypeDescriptor<?, ?> fieldType) {
		init( fieldType );
		IndexValueFieldTypeDescriptor typeDescriptor = getTypeDescriptor( "default" );

		Optional<String> normalizerName = typeDescriptor.normalizerName();

		if ( fieldType instanceof NormalizedStringFieldTypeDescriptor ) {
			assertThat( normalizerName ).contains( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name );
		}
		else {
			assertThat( normalizerName ).isEmpty();
		}
	}

	private IndexValueFieldTypeDescriptor getTypeDescriptor(String fieldName) {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexValueFieldDescriptor fieldDescriptor = indexDescriptor.field( fieldName ).get().toValueField();
		return fieldDescriptor.type();
	}

	private boolean isSortSupported(FieldTypeDescriptor<?, ?> fieldType) {
		return fieldType.isFieldSortSupported();
	}

	private boolean isAggregationSupported(FieldTypeDescriptor<?, ?> fieldType) {
		return TermsAggregationDescriptor.INSTANCE.getSingleFieldAggregationExpectations( fieldType ).isSupported();
	}

	private class IndexBinding {
		IndexBinding(IndexSchemaElement root, FieldTypeDescriptor<?, ?> fieldType) {
			mapper( fieldType, ignored -> {} ).map( root, "default" );

			mapper( fieldType, c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) )
					.map( root, "dslConverter" );
			mapper( fieldType, c -> c.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() ) )
					.map( root, "projectionConverter" );

			if ( fieldType instanceof StandardFieldTypeDescriptor<?> ) {
				var standardFieldType = (StandardFieldTypeDescriptor<?>) fieldType;
				mapper( standardFieldType, c -> c.searchable( Searchable.YES ) ).map( root, "searchable" );
				mapper( standardFieldType, c -> c.searchable( Searchable.NO ) ).map( root, "nonSearchable" );
				if ( isSortSupported( standardFieldType ) ) {
					mapper( standardFieldType, c -> c.sortable( Sortable.YES ) ).map( root, "sortable" );
				}
				mapper( standardFieldType, c -> c.sortable( Sortable.NO ) ).map( root, "nonSortable" );
				mapper( standardFieldType, c -> c.projectable( Projectable.YES ) ).map( root, "projectable" );
				mapper( standardFieldType, c -> c.projectable( Projectable.NO ) ).map( root, "nonProjectable" );
				if ( isAggregationSupported( standardFieldType ) ) {
					mapper( standardFieldType, c -> c.aggregable( Aggregable.YES ) ).map( root, "aggregable" );
				}
				mapper( standardFieldType, c -> c.aggregable( Aggregable.NO ) ).map( root, "nonAggregable" );
			}
			else {
				var vectorFieldType = (VectorFieldTypeDescriptor<?>) fieldType;
				mapper( vectorFieldType, c -> c.searchable( Searchable.YES ) ).map( root, "searchable" );
				mapper( vectorFieldType, c -> c.searchable( Searchable.NO ) ).map( root, "nonSearchable" );
				mapper( vectorFieldType, c -> c.projectable( Projectable.YES ) ).map( root, "projectable" );
				mapper( vectorFieldType, c -> c.projectable( Projectable.NO ) ).map( root, "nonProjectable" );

				// use defaults, that should be NO to make use of the tests:
				mapper( vectorFieldType ).map( root, "nonSortable" );
				mapper( vectorFieldType ).map( root, "nonAggregable" );

			}
		}
	}
}
