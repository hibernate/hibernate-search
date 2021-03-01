/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IdentifierSearchProjectionBaseIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
			.name( "my-index" );

	private final SimpleMappedIndex<IndexBinding> compatibleIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "my-compatible-index" );

	private final StubMappedIndex incompatibleIndex = StubMappedIndex.ofAdvancedNonRetrievable( ctx -> ctx
			.idDslConverter( INCOMPATIBLE_ID_CONVERTER ) );

	private final String[] ids = new String[7];
	private final String[] names = new String[7];
	private final List<String>[] duplicates = new List[7];

	public IdentifierSearchProjectionBaseIT() {
		initValues();
	}

	@Before
	public void setup() {
		setupHelper.start().withIndexes( index, compatibleIndex, incompatibleIndex ).setup();
		initData();
	}

	@Test
	public void simple() {
		StubMappingScope scope = index.createScope();
		SearchQuery<String> query = scope.query()
				.select( f -> f.id( String.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<String> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( ids );
	}

	@Test
	public void noClass() {
		StubMappingScope scope = index.createScope();
		SearchQuery<Object> query = scope.query()
				.select( f -> f.id() )
				.where( f -> f.matchAll() )
				.toQuery();

		List<Object> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( (Object[]) ids );
	}

	@Test
	public void duplicated() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f -> f.composite( f.id( String.class ), f.id( String.class ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<List<?>> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( duplicates );
	}

	@Test
	public void validSuperClass() {
		StubMappingScope scope = index.createScope();
		SearchQuery<CharSequence> query = scope.query()
				.select( f -> f.id( CharSequence.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<CharSequence> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( ids );
	}

	@Test
	public void invalidProjectionType() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection().id( Integer.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"The required identifier type 'java.lang.Integer' does not match the actual identifier type 'java.lang.String'",
						"the required identifier must be a superclass of the actual identifier"
				);
	}

	@Test
	public void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = index.createScope( compatibleIndex );
		SearchQuery<String> query = scope.query()
				.select( f -> f.id( String.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<String> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( ids );
	}

	@Test
	public void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		assertThatThrownBy( () -> scope.projection().id() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for the identifier in a search query across multiple indexes",
						"converter differs"
				);
	}

	@Test
	public void nullClass() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection().id( (Class<?>) null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"must not be null", "identifierType"
				);
	}

	private void initValues() {
		for ( int i = 0; i < 7; i++ ) {
			int suffix = i + 1;
			ids[i] = "ID-" + suffix;
			names[i] = "my-name-" + suffix;

			ArrayList<String> duplicated = new ArrayList<>( 2 );
			duplicated.add( ids[i] );
			duplicated.add( ids[i] );
			duplicates[i] = duplicated;
		}
	}

	private void initData() {
		index.bulkIndexer()
				.add( 7, i -> documentProvider(
						ids[i],
						document -> document.addValue( index.binding().name, names[i] )
				) )
				.join();
	}

	private static class IndexBinding {
		private final IndexFieldReference<String> name;

		IndexBinding(IndexSchemaElement root) {
			// this field is irrelevant for the test
			name = root.field( "name", f -> f.asString() ).toReference();
		}
	}

	private static final DocumentIdentifierValueConverter<Integer> INCOMPATIBLE_ID_CONVERTER =
			new DocumentIdentifierValueConverter<Integer>() {
				@Override
				public String convertToDocument(Integer value, ToDocumentIdentifierValueConvertContext context) {
					throw new UnsupportedOperationException( "Should not be called" );
				}

				@Override
				public String convertToDocumentUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
					throw new UnsupportedOperationException( "Should not be called" );
				}

				@Override
				public Integer convertToSource(String documentId, FromDocumentIdentifierValueConvertContext context) {
					return documentId.hashCode();
				}
			};
}
