package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

import org.junit.jupiter.api.Test;

class ElasticsearchDisjunctionMaxPredicateTest {
	@Test
	void clausesAndModifiersAreWrittenWithTheNativeJsonTree() {
		var scope = mock( ElasticsearchSearchIndexScope.class );
		when( scope.hibernateSearchIndexNames() ).thenReturn( Set.of( "index" ) );
		var builder = new ElasticsearchDisjunctionMaxPredicate.Builder( scope );
		builder.disjunct( new ElasticsearchMatchNonePredicate.Builder( scope ).build() );
		builder.disjunct( new ElasticsearchMatchNonePredicate.Builder( scope ).build() );
		builder.tieBreaker( 0.5f );
		builder.boost( 2f );
		var query = ( (ElasticsearchSearchPredicate) builder.build() ).toJsonQuery( null ).get( "dis_max" );
		assertThat( query.get( "queries" ).size() ).isEqualTo( 2 );
		assertThat( query.get( "queries" ).get( 0 ).has( "match_none" ) ).isTrue();
		assertThat( query.get( "queries" ).get( 1 ).has( "match_none" ) ).isTrue();
		assertThat( query.get( "tie_breaker" ).asDouble() ).isEqualTo( 0.5 );
		assertThat( query.get( "boost" ).asDouble() ).isEqualTo( 2 );
		var empty = new ElasticsearchDisjunctionMaxPredicate.Builder( scope );
		assertThat( ( (ElasticsearchSearchPredicate) empty.build() ).toJsonQuery( null ).has( "match_none" ) ).isTrue();
	}
}
