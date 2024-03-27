/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IdentifierProjectionBaseIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
			.name( "my-index" );

	private final SimpleMappedIndex<IndexBinding> compatibleIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "my-compatible-index" );

	private final StubMappedIndex incompatibleIndex = StubMappedIndex.ofAdvancedNonRetrievable( ctx -> ctx
			.idProjectionConverter( Integer.class, INCOMPATIBLE_ID_CONVERTER ) );

	private final String[] ids = new String[7];
	private final String[] names = new String[7];
	@SuppressWarnings("unchecked")
	private final List<String>[] duplicates = new List[7];

	private final String[] compatibleIndexIds = new String[3];
	private final String[] compatibleIndexNames = new String[3];

	public IdentifierProjectionBaseIT() {
		initValues();
	}

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( index, compatibleIndex, incompatibleIndex ).setup();
		initData();
	}

	@Test
	void simple() {
		StubMappingScope scope = index.createScope();
		SearchQuery<String> query = scope.query()
				.select( f -> f.id( String.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<String> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( ids );
	}

	@Test
	void noClass() {
		StubMappingScope scope = index.createScope();
		SearchQuery<Object> query = scope.query()
				.select( f -> f.id() )
				.where( f -> f.matchAll() )
				.toQuery();

		List<Object> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( (Object[]) ids );
	}

	@Test
	void duplicated() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f -> f.composite( f.id( String.class ), f.id( String.class ) ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<List<?>> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( duplicates );
	}

	@Test
	void validSuperClass() {
		StubMappingScope scope = index.createScope();
		SearchQuery<CharSequence> query = scope.query()
				.select( f -> f.id( CharSequence.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<CharSequence> result = query.fetchHits( 30 );
		assertThat( result ).containsExactlyInAnyOrder( ids );
	}

	@Test
	void invalidProjectionType() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection().id( Integer.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type for returned values: 'java.lang.Integer'. Expected 'java.lang.String' or a supertype"
				);
	}

	@Test
	void multiIndex_withCompatibleIndex() {
		StubMappingScope scope = index.createScope( compatibleIndex );
		SearchQuery<String> query = scope.query()
				.select( f -> f.id( String.class ) )
				.where( f -> f.matchAll() )
				.toQuery();

		List<String> result = query.fetchHits( 30 );

		String[] allIds = Stream.concat( Arrays.stream( ids ), Arrays.stream( compatibleIndexIds ) )
				.toArray( String[]::new );

		assertThat( result ).containsExactlyInAnyOrder( allIds );
	}

	@Test
	void multiIndex_withIncompatibleIndex() {
		StubMappingScope scope = index.createScope( incompatibleIndex );

		assertThatThrownBy( () -> scope.projection().id() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for identifier in a search query across multiple indexes",
						"Attribute 'projectionConverter' differs"
				);
	}

	@Test
	void nullClass() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.projection().id( (Class<?>) null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll(
						"must not be null", "requestedIdentifierType"
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
		for ( int i = 0; i < 3; i++ ) {
			int suffix = i + 8;
			compatibleIndexIds[i] = "ID-" + suffix;
			compatibleIndexNames[i] = "my-name-" + suffix;
		}
	}

	private void initData() {
		index.bulkIndexer()
				.add( 7, i -> documentProvider(
						ids[i],
						document -> document.addValue( index.binding().name, names[i] )
				) )
				.join();
		compatibleIndex.bulkIndexer()
				.add( 3, i -> documentProvider(
						compatibleIndexIds[i],
						document -> document.addValue( compatibleIndex.binding().name, compatibleIndexNames[i] )
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

	private static final FromDocumentValueConverter<String, Integer> INCOMPATIBLE_ID_CONVERTER =
			new FromDocumentValueConverter<String, Integer>() {
				@Override
				public Integer fromDocumentValue(String value, FromDocumentValueConvertContext context) {
					return value.hashCode();
				}
			};
}
