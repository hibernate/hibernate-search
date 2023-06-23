/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel.mapper;

import java.util.List;
import java.util.Optional;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Basic tests for value field type descriptor features, for every relevant field type.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3589")
public class IndexValueFieldTypeDescriptorBaseIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<FieldTypeDescriptor<?>> parameters() {
		return FieldTypeDescriptor.getAll();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private final FieldTypeDescriptor<?> fieldType;

	public IndexValueFieldTypeDescriptorBaseIT(FieldTypeDescriptor<?> fieldType) {
		this.fieldType = fieldType;
	}

	@Before
	public void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@Test
	public void isSearchable() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( true, IndexValueFieldTypeDescriptor::searchable );
		assertThat( getTypeDescriptor( "searchable" ) )
				.returns( true, IndexValueFieldTypeDescriptor::searchable );
		assertThat( getTypeDescriptor( "nonSearchable" ) )
				.returns( false, IndexValueFieldTypeDescriptor::searchable );
	}

	@Test
	public void isSortable() {
		boolean projectable = TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault();

		assertThat( getTypeDescriptor( "default" ) )
				.returns( GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) ? projectable : false,
						IndexValueFieldTypeDescriptor::sortable );
		if ( isSortSupported() ) {
			assertThat( getTypeDescriptor( "sortable" ) )
					.returns( true, IndexValueFieldTypeDescriptor::sortable );
		}
		assertThat( getTypeDescriptor( "nonSortable" ) )
				.returns( GeoPointFieldTypeDescriptor.INSTANCE.equals( fieldType ) ? projectable : false,
						IndexValueFieldTypeDescriptor::sortable );
	}

	@Test
	public void isProjectable() {
		boolean projectable = TckConfiguration.get().getBackendFeatures().fieldsProjectableByDefault();
		assertThat( getTypeDescriptor( "default" ) )
				.returns( projectable, IndexValueFieldTypeDescriptor::projectable );
		assertThat( getTypeDescriptor( "projectable" ) )
				.returns( true, IndexValueFieldTypeDescriptor::projectable );
		assertThat( getTypeDescriptor( "nonProjectable" ) )
				.returns( false, IndexValueFieldTypeDescriptor::projectable );
	}

	@Test
	public void isAggregable() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( false, IndexValueFieldTypeDescriptor::aggregable );
		if ( isAggregationSupported() ) {
			assertThat( getTypeDescriptor( "aggregable" ) )
					.returns( true, IndexValueFieldTypeDescriptor::aggregable );
		}
		assertThat( getTypeDescriptor( "nonAggregable" ) )
				.returns( false, IndexValueFieldTypeDescriptor::aggregable );
	}

	@Test
	public void dslArgumentClass() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::dslArgumentClass );
		assertThat( getTypeDescriptor( "dslConverter" ) )
				.returns( ValueWrapper.class, IndexValueFieldTypeDescriptor::dslArgumentClass );
		assertThat( getTypeDescriptor( "projectionConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::dslArgumentClass );
	}

	@Test
	public void projectedValueClass() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::projectedValueClass );
		assertThat( getTypeDescriptor( "dslConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::projectedValueClass );
		assertThat( getTypeDescriptor( "projectionConverter" ) )
				.returns( ValueWrapper.class, IndexValueFieldTypeDescriptor::projectedValueClass );
	}

	@Test
	public void valueClass() {
		assertThat( getTypeDescriptor( "default" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::valueClass );
		assertThat( getTypeDescriptor( "dslConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::valueClass );
		assertThat( getTypeDescriptor( "projectionConverter" ) )
				.returns( fieldType.getJavaType(), IndexValueFieldTypeDescriptor::valueClass );
	}

	@Test
	public void searchAnalyzerName() {
		IndexValueFieldTypeDescriptor typeDescriptor = getTypeDescriptor( "default" );

		Optional<String> searchAnalyzerName = typeDescriptor.searchAnalyzerName();

		if ( fieldType instanceof AnalyzedStringFieldTypeDescriptor ) {
			assertThat( searchAnalyzerName ).contains( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name );
		}
		else {
			assertThat( searchAnalyzerName ).isEmpty();
		}
	}

	@Test
	public void normalizerName() {
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

	private boolean isSortSupported() {
		return fieldType.isFieldSortSupported();
	}

	private boolean isAggregationSupported() {
		return TermsAggregationDescriptor.INSTANCE.getSingleFieldAggregationExpectations( fieldType ).isSupported();
	}

	private class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			mapper( fieldType, ignored -> {} ).map( root, "default" );

			mapper( fieldType, c -> c.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() ) )
					.map( root, "dslConverter" );
			mapper( fieldType, c -> c.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() ) )
					.map( root, "projectionConverter" );

			mapper( fieldType, c -> c.searchable( Searchable.YES ) ).map( root, "searchable" );
			mapper( fieldType, c -> c.searchable( Searchable.NO ) ).map( root, "nonSearchable" );
			if ( isSortSupported() ) {
				mapper( fieldType, c -> c.sortable( Sortable.YES ) ).map( root, "sortable" );
			}
			mapper( fieldType, c -> c.sortable( Sortable.NO ) ).map( root, "nonSortable" );
			mapper( fieldType, c -> c.projectable( Projectable.YES ) ).map( root, "projectable" );
			mapper( fieldType, c -> c.projectable( Projectable.NO ) ).map( root, "nonProjectable" );
			if ( isAggregationSupported() ) {
				mapper( fieldType, c -> c.aggregable( Aggregable.YES ) ).map( root, "aggregable" );
			}
			mapper( fieldType, c -> c.aggregable( Aggregable.NO ) ).map( root, "nonAggregable" );
		}
	}
}
