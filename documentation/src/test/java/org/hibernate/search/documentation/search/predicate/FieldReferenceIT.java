/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.mapper.scope.SearchScopeProvider;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.reference.RootReferenceScope;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.projection.FieldProjectionFieldReference;
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
		entityManagerFactory = setupHelper.start().setup( EntityA.class, EntityB.class, EntityC.class,
				Entity2A.class, Entity2B.class, Entity2C.class );
		initData();
	}

	@Test
	void smoke() {
		withinSearchSession( searchSession -> {
			assertThat(
					searchSession.search( EntityA_.scope )
							.select( f -> f.field( EntityA_.stringA ) )
							.where( f -> f.match().field( EntityA_.stringA ).matching( "a" ) )
							.fetchHits( 20 )
			).containsOnly( "a" );

			assertThat(
					searchSession.search( EntityC_.scope )
							.select( f -> f.field( EntityC_.stringA ) )
							.where( f -> f.match().field( EntityC_.stringC ).matching( "c" ) )
							.fetchHits( 20 )
			).containsOnly( "c" );

			SearchScope<EntityB_union_EntityC_, EntityB> scope = EntityB_union_EntityC_.scope.create( searchSession );

			SearchPredicate searchPredicate =
					scope.predicate().match().field( EntityB_union_EntityC_.stringA ).matching( "b" ).toPredicate();
			SearchPredicate searchPredicate2 =
					scope.predicate().match().field( EntityB_union_EntityC_.stringB ).matching( "c" ).toPredicate();

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( EntityB_union_EntityC_.stringA ) )
							.where( searchPredicate )
							.fetchHits( 20 )
			).containsOnly( "b" );

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( EntityB_union_EntityC_.stringA ) )
							.where( searchPredicate2 )
							.fetchHits( 20 )
			).containsOnly( "c" );

		} );
	}

	@Test
	void smoke2() {
		withinSearchSession( searchSession -> {

			SearchScope<Entity2A_union_Entity2B_, Object> scope = Entity2A_union_Entity2B_.scope.create( searchSession );
			// while path is there, sure, the EntityB isn't in the scope
			SearchPredicate searchPredicate =
					scope.predicate().match().field( Entity2A_union_Entity2B_.stringA ).matching( "a" ).toPredicate();
			SearchPredicate searchPredicate2 =
					scope.predicate().match().field( Entity2A_union_Entity2B_.stringA ).matching( "b" ).toPredicate();

			assertThat(
					searchSession.search( scope )
							// also wrong type here
							.select( f -> f.field( Entity2A_union_Entity2B_.stringA ) )
							.where( searchPredicate )
							.fetchHits( 20 )
			).containsOnly( "a" );

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( Entity2A_union_Entity2B_.stringA ) )
							.where( searchPredicate2 )
							.fetchHits( 20 )
			).containsOnly( "b" );

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

			Entity2A a2 = new Entity2A();
			a2.id = 2L;
			a2.stringA = "a";

			Entity2B b2 = new Entity2B();
			b2.id = 20L;
			b2.stringA = "b";
			b2.stringB = "b";

			Entity2C c2 = new Entity2C();
			c2.id = 200L;
			c2.stringA = "c";
			c2.stringC = "c";

			entityManager.persist( a2 );
			entityManager.persist( b2 );
			entityManager.persist( c2 );
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
		public static ValueFieldReference1<EntityA_, String, String, String> stringA;

		public static RootReferenceScope<EntityA_, EntityA> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityA_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( EntityA_.class, EntityA.class );
		}
	}

	public static class EntityB_ {
		public static ValueFieldReference1<EntityB_, String, String, String> stringA;
		public static ValueFieldReference1<EntityB_, String, String, String> stringB;

		public static RootReferenceScope<EntityB_, EntityB> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityB_.class, String.class, String.class, String.class );
			stringB = ValueFieldReference1.of( "stringB", EntityB_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( EntityB_.class, EntityB.class );
		}
	}

	public static class EntityC_ {
		public static ValueFieldReference1<EntityC_, String, String, String> stringA;
		public static ValueFieldReference1<EntityC_, String, String, String> stringB;
		public static ValueFieldReference1<EntityC_, String, String, String> stringC;

		public static RootReferenceScope<EntityC_, EntityC> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityC_.class, String.class, String.class, String.class );
			stringB = ValueFieldReference1.of( "stringB", EntityC_.class, String.class, String.class, String.class );
			stringC = ValueFieldReference1.of( "stringC", EntityC_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( EntityC_.class, EntityC.class );
		}
	}

	public static class EntityB_union_EntityC_ {
		public static ValueFieldReference1<EntityB_union_EntityC_, String, String, String> stringA;
		public static ValueFieldReference1<EntityB_union_EntityC_, String, String, String> stringB;

		public static RootReferenceScope<EntityB_union_EntityC_, EntityB> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", EntityB_union_EntityC_.class, String.class, String.class,
					String.class );
			stringB = ValueFieldReference1.of( "stringB", EntityB_union_EntityC_.class, String.class, String.class,
					String.class );

			scope = RootReferenceScopeImpl.of( EntityB_union_EntityC_.class, EntityB.class, EntityC.class );
		}
	}

	@Entity
	@Indexed
	public static class Entity2A {
		@Id
		Long id;

		@FullTextField(projectable = Projectable.YES)
		String stringA;
	}

	@Entity
	@Indexed
	public static class Entity2B {
		@Id
		Long id;

		@FullTextField(projectable = Projectable.YES)
		String stringA;

		@FullTextField(projectable = Projectable.YES)
		String stringB;
	}

	@Entity
	@Indexed
	public static class Entity2C {
		@Id
		Long id;

		@FullTextField(projectable = Projectable.YES)
		String stringA;

		@FullTextField(projectable = Projectable.YES)
		String stringC;
	}

	public static class Entity2A_union_Entity2B_ {
		public static ValueFieldReference1<Entity2A_union_Entity2B_, String, String, String> stringA;

		public static RootReferenceScope<Entity2A_union_Entity2B_, Object> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2A_union_Entity2B_.class, String.class, String.class,
					String.class );
			scope = RootReferenceScopeImpl.of( Entity2A_union_Entity2B_.class, Entity2A.class, Entity2B.class );
		}
	}

	public static class Entity2A_ {
		public static ValueFieldReference1<Entity2A_, String, String, String> stringA;

		public static RootReferenceScope<Entity2A_, Entity2A> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2A_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( Entity2A_.class, Entity2A.class );
		}
	}

	public static class Entity2B_ {
		public static ValueFieldReference1<Entity2B_, String, String, String> stringA;
		public static ValueFieldReference1<Entity2B_, String, String, String> stringB;

		public static RootReferenceScope<Entity2B_, Entity2B> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2B_.class, String.class, String.class, String.class );
			stringB = ValueFieldReference1.of( "stringB", Entity2B_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( Entity2B_.class, Entity2B.class );
		}
	}

	public static class Entity2C_ {
		public static ValueFieldReference1<Entity2C_, String, String, String> stringA;
		public static ValueFieldReference1<Entity2C_, String, String, String> stringC;

		public static RootReferenceScope<Entity2C_, Entity2C> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2C_.class, String.class, String.class, String.class );
			stringC = ValueFieldReference1.of( "stringC", Entity2C_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( Entity2C_.class, Entity2C.class );
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

	public static class TypedFieldReference1<SR, T, P>
			implements FieldProjectionFieldReference<SR, P>,
			MatchPredicateFieldReference<SR, T> {

		private final String absolutePath;
		private final ValueConvert valueConvert;
		private final Class<SR> containing;
		private final Class<T> input;
		private final Class<P> projection;

		public TypedFieldReference1(String absolutePath, ValueConvert valueConvert, Class<SR> containing, Class<T> input,
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
		public Class<SR> scopeRootType() {
			return containing;
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

	}

	private static class RootReferenceScopeImpl<SR, E> implements RootReferenceScope<SR, E> {

		private final Class<SR> rootReferenceType;
		private final Class<? extends E>[] entityClass;

		static <SR, E> RootReferenceScope<SR, E> of(Class<SR> rootReferenceType, Class<? extends E>... entityClass) {
			return new RootReferenceScopeImpl<>( rootReferenceType, entityClass );
		}

		private RootReferenceScopeImpl(Class<SR> rootReferenceType, Class<? extends E>... entityClass) {
			this.rootReferenceType = rootReferenceType;
			this.entityClass = entityClass;
		}

		@Override
		public Class<SR> rootReferenceType() {
			return rootReferenceType;
		}

		@Override
		public <
				ER extends EntityReference,
				S extends org.hibernate.search.engine.mapper.scope.SearchScope<SR, E, ER>,
				P extends SearchScopeProvider<ER>> S create(P scopeProvider) {
			return (S) scopeProvider.scope( Arrays.asList( entityClass ) );
		}
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}
}
