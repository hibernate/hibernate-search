/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.reference.traits.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.traits.projection.FieldProjectionFieldReference;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FieldReferenceIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( EntityA.class, EntityB.class, EntityC.class );
		initData();
	}

	@Test
	void smoke() {
		withinSearchSession( searchSession -> {
			assertThat(
					searchSession.search( EntityA.class )
							.select( f -> f.field( EntityA_.stringA ) )
							.where( f -> f.match().field( EntityA_.stringA ).matching( "a" ) )
							.fetchHits( 20 )
			).containsOnly( "a" );

			assertThat(
					searchSession.search( EntityA.class )
							.select( f -> f.field( EntityC_.stringA ) )
							.where( f -> f.match().field( EntityC_.stringC ).matching( "c" ) )
							.fetchHits( 20 )
			).containsOnly( "c" );

			SearchScope<EntityB> scope = searchSession.scope( List.of( EntityB.class, EntityC.class ) );
			SearchPredicate searchPredicate = scope.predicate().match().field( EntityB_.stringA ).matching( "b" ).toPredicate();
			SearchPredicate searchPredicate2 =
					scope.predicate().match().field( EntityC_.stringC ).matching( "c" ).toPredicate();

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( EntityC_.stringA ) )
							.where( searchPredicate )
							.fetchHits( 20 )
			).containsOnly( "b" );

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( EntityC_.stringA ) )
							.where( searchPredicate2 )
							.fetchHits( 20 )
			).containsOnly( "c" );

		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			EntityA a = new EntityA();
			a.id = 1L;
			a.stringA = "a";

			EntityB b = new EntityB();
			b.id = 10L;
			b.stringA = "b";
			b.stringB = "b";

			EntityC c = new EntityC();
			c.id = 100L;
			c.stringA = "c";
			c.stringB = "c";
			c.stringC = "c";

			entityManager.persist( a );
			entityManager.persist( b );
			entityManager.persist( c );
		} );
	}

	@Indexed
	@Entity
	public static class EntityA {
		@Id
		Long id;

		@FullTextField(projectable = Projectable.YES)
		String stringA;

	}

	@Entity
	public static class EntityB extends EntityA {

		@FullTextField(projectable = Projectable.YES)
		String stringB;

	}

	@Entity
	public static class EntityC extends EntityB {

		@FullTextField(projectable = Projectable.YES)
		String stringC;

	}


	public static class EntityA_ {
		public static ValueFieldReference1<EntityA, String, String, String> stringA;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityA.class, String.class, String.class, String.class );
		}
	}

	public static class EntityB_ {
		public static ValueFieldReference1<EntityB, String, String, String> stringA;
		public static ValueFieldReference1<EntityB, String, String, String> stringB;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityB.class, String.class, String.class, String.class );
			stringB = ValueFieldReference1.of( "stringB", EntityB.class, String.class, String.class, String.class );
		}
	}

	public static class EntityC_ {
		public static ValueFieldReference1<EntityC, String, String, String> stringA;
		public static ValueFieldReference1<EntityC, String, String, String> stringB;
		public static ValueFieldReference1<EntityC, String, String, String> stringC;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityC.class, String.class, String.class, String.class );
			stringB = ValueFieldReference1.of( "stringB", EntityC.class, String.class, String.class, String.class );
			stringC = ValueFieldReference1.of( "stringC", EntityC.class, String.class, String.class, String.class );
		}
	}


	public static class ValueFieldReference1<E, T, V, P> extends TypedFieldReference1<E, T, P> {

		public static <E, T, V, P> ValueFieldReference1<E, T, V, P> of(
				String path,
				Class<E> documentReferenceClass,
				Class<T> t,
				Class<V> v,
				Class<P> p) {
			return new ValueFieldReference1<>( path, documentReferenceClass, t, v, p );
		}

		private final TypedFieldReference1<E, V, V> noConverter;
		private final TypedFieldReference1<E, String, String> string;

		public ValueFieldReference1(String absolutePath, Class<E> containing, Class<T> inputType, Class<V> indexType,
				Class<P> projectionType) {
			super( absolutePath, ValueConvert.YES, containing, inputType, projectionType );
			this.noConverter = new TypedFieldReference1<>( absolutePath, ValueConvert.NO, containing, indexType, indexType );
			this.string =
					new TypedFieldReference1<>( absolutePath, ValueConvert.PARSE, containing, String.class, String.class );
		}

		public TypedFieldReference1<E, V, V> noConverter() {
			return noConverter;
		}


		public TypedFieldReference1<E, String, String> asString() {
			return string;
		}

	}

	public static class TypedFieldReference1<E, T, P>
			implements FieldProjectionFieldReference<E, P>,
			MatchPredicateFieldReference<E, T> {

		private final String absolutePath;
		private final ValueConvert valueConvert;
		private final Class<E> containing;
		private final Class<T> input;
		private final Class<P> projection;

		public TypedFieldReference1(String absolutePath, ValueConvert valueConvert, Class<E> containing, Class<T> input,
				Class<P> projection) {
			this.absolutePath = absolutePath;
			this.valueConvert = valueConvert;
			this.containing = containing;
			this.input = input;
			this.projection = projection;
		}

		@Override
		public String absolutePath() {
			return absolutePath;
		}

		@Override
		public Class<T> predicateType() {
			return input;
		}

		@Override
		public ValueConvert valueConvert() {
			return valueConvert;
		}

		@Override
		public Class<P> projectionType() {
			return projection;
		}

		@Override
		public Class<E> containing() {
			return containing;
		}
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}
}
