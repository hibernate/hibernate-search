package org.hibernate.search.integrationtest.backend.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.json.JsonHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.gson.JsonElement;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchNativeFieldCardinalityIT {
	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> singleIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, false ) ).name( "native-single" );
	private final SimpleMappedIndex<IndexBinding> multiIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, true ) ).name( "native-multi" );

	private final SimpleMappedIndex<IndexBinding> splitIndex =
			SimpleMappedIndex.of( root -> new IndexBinding( root, true, false ) ).name( "native-split" );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndexes( singleIndex, multiIndex, splitIndex ).setup();
	}

	@Test
	@SuppressWarnings("unused")
	void multipleObjectsProjectIndependentlyWithObjectAndGeneralHandlers() {
		ObjectNode first = JacksonJsonHandlers.object().fromJson( "{\"value\":1,\"nested\":null}" );
		ObjectNode second = JacksonJsonHandlers.object().fromJson( "{\"value\":2}" );
		multiIndex.bulkIndexer().add( "two", document -> {
			document.addValue( multiIndex.binding().objects, first );
			document.addValue( multiIndex.binding().objects, second );
			document.addValue( multiIndex.binding().values, first );
			document.addValue( multiIndex.binding().values, second );
		} ).add( "one", document -> document.addValue( multiIndex.binding().objects, first ) )
				.add( "missing", document -> {} )
				.add( "null", document -> document.addValue( multiIndex.binding().objects, null ) ).join();
		var scope = multiIndex.createScope();
		assertThat( scope.query().select( f -> f.field( "objects", ObjectNode.class ).list() )
				.where( f -> f.id().matching( "two" ) ).fetchSingleHit().orElseThrow() ).containsExactly( first, second );
		assertThat( scope.query().select( f -> f.field( "values", JsonNode.class ).list() )
				.where( f -> f.id().matching( "two" ) ).fetchSingleHit().orElseThrow() ).containsExactly( first, second );
		assertThat( scope.query().select( f -> f.field( "objects", ObjectNode.class ).list() )
				.where( f -> f.id().matching( "one" ) ).fetchSingleHit().orElseThrow() ).containsExactly( first );
		assertThat( scope.query().select( f -> f.field( "objects", ObjectNode.class ).list() )
				.where( f -> f.id().matching( "missing" ) ).fetchSingleHit().orElseThrow() ).isEmpty();
		assertThat( scope.query().select( f -> f.field( "objects", ObjectNode.class ).list() )
				.where( f -> f.id().matching( "null" ) ).fetchSingleHit().orElseThrow() ).containsExactly( (ObjectNode) null );
	}

	@Test
	void atomicArraysSurviveSingleAndMultiValuedIndexingAndMixedIndexSearch() {
		JsonNode value = JacksonJsonHandlers.value().fromJson( "[1,null,{\"nested\":null}]" );
		JsonNode empty = JacksonJsonHandlers.value().fromJson( "[]" );
		singleIndex.bulkIndexer().add( "single", document -> document.addValue( singleIndex.binding().values, value ) ).join();
		multiIndex.bulkIndexer().add( "multi", document -> {
			document.addValue( multiIndex.binding().values, value );
			document.addValue( multiIndex.binding().values, value );
			document.addValue( multiIndex.binding().values, empty );
		} ).join();
		assertThat( value ).hasSize( 3 );
		assertThat( singleIndex.createScope().query().select( f -> f.field( "values", JsonNode.class ) )
				.where( f -> f.matchAll() ).fetchSingleHit().orElseThrow() ).isEqualTo( value );
		assertThat( multiIndex.createScope().query().select( f -> f.field( "values", JsonNode.class ).list() )
				.where( f -> f.matchAll() ).fetchSingleHit().orElseThrow() ).containsExactly( value, value, empty );
		assertThat( singleIndex.createScope( multiIndex ).query().select( f -> f.field( "values", JsonNode.class ).list() )
				.where( f -> f.matchAll() ).fetchAllHits() )
				.containsExactlyInAnyOrder( List.of( value ), List.of( value, value, empty ) );
	}

	@Test
	void defaultNativeFieldsPreserveLegacyCardinalityAndRejectDifferentModesAcrossIndexes() {
		var jackson = JacksonJsonHandlers.value().fromJson( "[1,null,2]" );
		var gson = GsonJsonHandlers.value().fromJson( "[1,null,2]" );
		splitIndex.bulkIndexer().add( "arrays", document -> {
			document.addValue( splitIndex.binding().values, jackson );
			document.addValue( splitIndex.binding().gson, gson );
			document.addValue( splitIndex.binding().legacy, gson );
		} ).add( "empty", document -> {
			document.addValue( splitIndex.binding().values, JacksonJsonHandlers.value().fromJson( "[]" ) );
			document.addValue( splitIndex.binding().gson, GsonJsonHandlers.value().fromJson( "[]" ) );
			document.addValue( splitIndex.binding().legacy, GsonJsonHandlers.value().fromJson( "[]" ) );
		} ).join();
		var scope = splitIndex.createScope();
		assertThat( scope.query().select( f -> f.field( "values", JsonNode.class ).list() )
				.where( f -> f.id().matching( "arrays" ) ).fetchSingleHit().orElseThrow() )
				.containsExactly( JacksonJsonHandlers.value().fromJson( "1" ), null,
						JacksonJsonHandlers.value().fromJson( "2" ) );
		for ( String name : List.of( "gson", "legacy" ) ) {
			assertThat( scope.query().select( f -> f.field( name, JsonElement.class ).list() )
					.where( f -> f.id().matching( "arrays" ) ).fetchSingleHit().orElseThrow() )
					.containsExactly( GsonJsonHandlers.value().fromJson( "1" ), null,
							GsonJsonHandlers.value().fromJson( "2" ) );
			assertThat( scope.query().select( f -> f.field( name, JsonElement.class ).list() )
					.where( f -> f.id().matching( "empty" ) ).fetchSingleHit().orElseThrow() ).isEmpty();
		}
		assertThat( scope.query().select( f -> f.field( "values", JsonNode.class ).list() )
				.where( f -> f.id().matching( "empty" ) ).fetchSingleHit().orElseThrow() ).isEmpty();
		assertThatThrownBy( () -> splitIndex.createScope( multiIndex ).query()
				.select( f -> f.field( "values", JsonNode.class ).list() ).where( f -> f.matchAll() ).fetchAllHits() )
				.isInstanceOf( org.hibernate.search.util.common.SearchException.class );
	}

	private static class IndexBinding {
		final IndexFieldReference<ObjectNode> objects;
		final IndexFieldReference<JsonNode> values;
		final IndexFieldReference<JsonElement> gson;
		final IndexFieldReference<JsonElement> legacy;

		IndexBinding(IndexSchemaElement root, boolean multiValued) {
			this( root, multiValued, true );
		}

		@SuppressWarnings("deprecation")
		IndexBinding(IndexSchemaElement root, boolean multiValued, boolean atomic) {
			objects = field( root, "objects", JacksonJsonHandlers.object(), multiValued );
			var valuesStep = root.field( "values", f -> {
				var step = f.extension( ElasticsearchExtension.get() ).asNative( JacksonJsonHandlers.value() )
						.mapping( "{\"type\":\"object\",\"enabled\":false}" );
				return atomic ? step.arrayAsValue() : step;
			} );
			if ( multiValued ) {
				valuesStep.multiValued();
			}
			values = valuesStep.toReference();
			gson = root.field( "gson", f -> f.extension( ElasticsearchExtension.get() )
					.asNative( GsonJsonHandlers.value() ).mapping( "{\"type\":\"object\",\"enabled\":false}" ) )
					.multiValued().toReference();
			legacy = root.field( "legacy", f -> f.extension( ElasticsearchExtension.get() )
					.asNative().mapping( "{\"type\":\"object\",\"enabled\":false}" ) )
					.multiValued().toReference();
		}

		private static <T> IndexFieldReference<T> field(IndexSchemaElement root, String name,
				JsonHandler<T> handler, boolean multiValued) {
			var field = root.field( name, f -> nativeType( f, handler ) );
			if ( multiValued ) {
				field.multiValued();
			}
			return field.toReference();
		}

		private static <T> IndexFieldTypeFinalStep<T> nativeType(
				IndexFieldTypeFactory factory, JsonHandler<T> handler) {
			return factory.extension( ElasticsearchExtension.get() ).asNative( handler )
					.mapping( "{\"type\":\"object\",\"enabled\":false}" ).arrayAsValue();
		}
	}
}
