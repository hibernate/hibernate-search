package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldValueFactorFunction;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonObject;

import org.mockito.ArgumentCaptor;

class ElasticsearchFunctionScorePredicateTest {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void mixedRootAndNestedFieldsRejected(boolean rootFirst) {
		ElasticsearchSearchIndexScope<?> scope = mock( ElasticsearchSearchIndexScope.class );
		ElasticsearchFunctionScorePredicate.Builder builder = new ElasticsearchFunctionScorePredicate.Builder( scope );
		ElasticsearchFieldValueFactorFunction.Builder<?> root =
				mock( ElasticsearchFieldValueFactorFunction.Builder.class );
		when( root.nestedPathHierarchy() ).thenReturn( List.of() );
		ElasticsearchFieldValueFactorFunction.Builder<?> nested =
				mock( ElasticsearchFieldValueFactorFunction.Builder.class );
		when( nested.nestedPathHierarchy() ).thenReturn( List.of( "nested" ) );

		builder.addFieldValueFactorFunction( rootFirst ? root : nested, 1f, null );
		assertThatThrownBy( () -> builder.addFieldValueFactorFunction( rootFirst ? nested : root, 1f, null ) )
				.isInstanceOf( SearchException.class )
				.hasMessageStartingWith( "HSEARCH400696:" )
				.hasMessageContaining( "at different nested paths" );
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "nested", "nested.deeper" })
	void childrenUseResolvedNestedContext(String parentNestedPath) {
		ElasticsearchSearchIndexScope<?> scope = mock( ElasticsearchSearchIndexScope.class );
		when( scope.hibernateSearchIndexNames() ).thenReturn( Set.of( "index" ) );
		ElasticsearchSearchPredicate inner = child();
		ElasticsearchSearchPredicate filter = child();
		ElasticsearchFieldValueFactorFunction.Builder<?> function =
				mock( ElasticsearchFieldValueFactorFunction.Builder.class );
		when( function.nestedPathHierarchy() ).thenReturn( List.of( "nested", "nested.deeper" ) );
		when( function.fieldPath() ).thenReturn( "nested.deeper.rating" );
		when( function.toJson() ).thenReturn( new JsonObject() );
		ElasticsearchFunctionScorePredicate.Builder builder = new ElasticsearchFunctionScorePredicate.Builder( scope );
		builder.innerPredicate( inner );
		builder.addFieldValueFactorFunction( function, 1f, filter );
		PredicateRequestContext context = new PredicateRequestContext( null, scope, Set.of(), null );
		if ( parentNestedPath != null ) {
			context = context.withNestedPath( parentNestedPath );
		}

		( (ElasticsearchSearchPredicate) builder.build() ).toJsonQuery( context );

		ArgumentCaptor<PredicateRequestContext> innerContext = ArgumentCaptor.forClass( PredicateRequestContext.class );
		verify( inner ).toJsonQuery( innerContext.capture() );
		assertThat( innerContext.getValue().getNestedPath() ).isEqualTo( "nested.deeper" );
		ArgumentCaptor<PredicateRequestContext> filterContext = ArgumentCaptor.forClass( PredicateRequestContext.class );
		verify( filter ).toJsonQuery( filterContext.capture() );
		assertThat( filterContext.getValue().getNestedPath() ).isEqualTo( "nested.deeper" );
	}

	private ElasticsearchSearchPredicate child() {
		ElasticsearchSearchPredicate child = mock( ElasticsearchSearchPredicate.class );
		when( child.indexNames() ).thenReturn( Set.of( "index" ) );
		when( child.toJsonQuery( any() ) ).thenReturn( new JsonObject() );
		return child;
	}
}
