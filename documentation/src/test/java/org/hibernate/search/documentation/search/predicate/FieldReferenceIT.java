/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.function.Consumer;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.object.ObjectFieldReference;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.projection.FieldProjectionFieldReference;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.HibernateOrmRootReferenceScope;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.scope.SearchScopeProvider;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

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
				Entity2A.class, Entity2B.class, Entity2C.class,
				ContainingA.class, ContainingB.class, EmbeddedThing1.class, EmbeddedThing2.class, EmbeddedThing3.class
		);
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

			assertThat(
					searchSession.search( EntityC_.scope )
							.select( f -> f.field( EntityC_.stringA ) )
							.where( f -> f.match().field( EntityC_.stringA ).matching( "c" ) )
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

	@Test
	void smoke3() {

		withinSearchSession( searchSession -> {

			SearchScope<ContainingA_, ContainingA> scope = ContainingA_.scope.create( searchSession );

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( ContainingA_.a ) )
							.where( f -> utilMethodForPredicate( f ) )
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

	public static class EntityA_ implements Properties_EntityA_ {
		public static HibernateOrmRootReferenceScope<EntityA_, EntityA> scope;

		static {
			scope = RootReferenceScopeImpl.of( EntityA_.class, EntityA.class );
		}

	}

	public interface Properties_EntityA_ {
		ValueFieldReference1<EntityA_, String, String, String> stringA =
				ValueFieldReference1.of( "stringA", EntityA_.class, String.class, String.class, String.class );
	}

	public static class EntityB_ extends EntityA_ {
		public static ValueFieldReference1<EntityB_, String, String, String> stringB;

		public static HibernateOrmRootReferenceScope<EntityB_, EntityB> scope;

		static {
			stringB = ValueFieldReference1.of( "stringB", EntityB_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( EntityB_.class, EntityB.class );
		}
	}

	public static class EntityC_ extends EntityB_ {
		public static ValueFieldReference1<EntityC_, String, String, String> stringC;

		public static HibernateOrmRootReferenceScope<EntityC_, EntityC> scope;

		static {
			stringC = ValueFieldReference1.of( "stringC", EntityC_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( EntityC_.class, EntityC.class );
		}
	}

	public static class EntityB_union_EntityC_ {
		public static ValueFieldReference1<EntityB_union_EntityC_, String, String, String> stringA;
		public static ValueFieldReference1<EntityB_union_EntityC_, String, String, String> stringB;

		public static HibernateOrmRootReferenceScope<EntityB_union_EntityC_, EntityB> scope;

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

		public static HibernateOrmRootReferenceScope<Entity2A_union_Entity2B_, Object> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2A_union_Entity2B_.class, String.class, String.class,
					String.class );
			scope = RootReferenceScopeImpl.of( Entity2A_union_Entity2B_.class, Entity2A.class, Entity2B.class );
		}
	}

	public static class Entity2A_ {
		public static ValueFieldReference1<Entity2A_, String, String, String> stringA;

		public static HibernateOrmRootReferenceScope<Entity2A_, Entity2A> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2A_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( Entity2A_.class, Entity2A.class );
		}
	}

	public static class Entity2B_ {
		public static ValueFieldReference1<Entity2B_, String, String, String> stringA;
		public static ValueFieldReference1<Entity2B_, String, String, String> stringB;

		public static HibernateOrmRootReferenceScope<Entity2B_, Entity2B> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2B_.class, String.class, String.class, String.class );
			stringB = ValueFieldReference1.of( "stringB", Entity2B_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( Entity2B_.class, Entity2B.class );
		}
	}

	public static class Entity2C_ {
		public static ValueFieldReference1<Entity2C_, String, String, String> stringA;
		public static ValueFieldReference1<Entity2C_, String, String, String> stringC;

		public static HibernateOrmRootReferenceScope<Entity2C_, Entity2C> scope;

		static {
			stringA = ValueFieldReference1.of( "stringA", Entity2C_.class, String.class, String.class, String.class );
			stringC = ValueFieldReference1.of( "stringC", Entity2C_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( Entity2C_.class, Entity2C.class );
		}
	}

	@MappedSuperclass
	public static class MappedSuperclassThing {
		@Id
		Long id;

		@FullTextField(projectable = Projectable.YES)
		String a;
	}

	@Indexed
	@Entity
	public static class ContainingA extends MappedSuperclassThing {
		@IndexedEmbedded
		EmbeddedThing1 e1;
		@IndexedEmbedded
		EmbeddedThing1 e2;
	}

	@Embeddable
	public static class EmbeddedThing1 {
		@FullTextField
		String a;
		@FullTextField
		String b;
		// some other fields
	}

	@Embeddable
	public static class EmbeddedThing2 {
		@FullTextField
		String a;
		// some other fields maybe different from EmbeddedThing1
	}

	@Entity
	@Indexed
	public static class ContainingB extends MappedSuperclassThing {
		@IndexedEmbedded
		EmbeddedThing3 e3;
	}

	@Embeddable
	public static class EmbeddedThing3 {
		@GenericField
		Integer a;
		// some other fields maybe different from EmbeddedThing1/EmbeddedThing2
	}


	interface Property_String_a_ {
		ValueFieldReference1<Property_String_a_, String, String, String> a =
				ValueFieldReference1.of( "a", Property_String_a_.class, String.class, String.class, String.class );
	}

	interface Property_String_b_ {
		ValueFieldReference1<Property_String_b_, String, String, String> b =
				ValueFieldReference1.of( "b", Property_String_b_.class, String.class, String.class, String.class );
	}

	interface Property_Integer_a_ {
		ValueFieldReference1<Property_Integer_a_, Integer, Integer, Integer> a =
				ValueFieldReference1.of( "a", Property_Integer_a_.class, Integer.class, Integer.class, Integer.class );
	}

	public static class MappedSuperclassThing_ implements Property_String_a_ {

		public static HibernateOrmRootReferenceScope<MappedSuperclassThing_, MappedSuperclassThing> scope;

		static {
			//a = ValueFieldReference1.of( "a", MappedSuperclassThing_.class, String.class, String.class, String.class );

			scope = RootReferenceScopeImpl.of( MappedSuperclassThing_.class, MappedSuperclassThing.class );
		}
	}

	public static class ContainingA_ extends MappedSuperclassThing_ {
		public static e1_.Absolute e1;
		public static e2_.Absolute e2;

		public static HibernateOrmRootReferenceScope<ContainingA_, ContainingA> scope;

		static {
			e1 = new e1_.Absolute();
			e2 = new e2_.Absolute();

			scope = RootReferenceScopeImpl.of( ContainingA_.class, ContainingA.class );
		}

		public static class e1_ implements ObjectFieldReference<ContainingA_>, Property_String_a_, Property_String_b_ {

			@Override
			public String absolutePath() {
				return "e1";
			}

			@Override
			public Class<ContainingA_> scopeRootType() {
				return ContainingA_.class;
			}

			static class Absolute extends e1_ {
				public static ValueFieldReference1<ContainingA_, String, String, String> a;
				public static ValueFieldReference1<ContainingA_, String, String, String> b;

				static {
					a = ValueFieldReference1.of( "e1.a", ContainingA_.class, String.class, String.class, String.class );
					b = ValueFieldReference1.of( "e1.b", ContainingA_.class, String.class, String.class, String.class );
				}
			}
		}

		public static class e2_ implements ObjectFieldReference<ContainingA_>, Property_String_a_, Property_String_b_ {

			@Override
			public String absolutePath() {
				return "e2";
			}

			@Override
			public Class<ContainingA_> scopeRootType() {
				return ContainingA_.class;
			}

			static class Absolute extends e2_ {
				public static ValueFieldReference1<ContainingA_, String, String, String> a;
				public static ValueFieldReference1<ContainingA_, String, String, String> b;

				static {
					a = ValueFieldReference1.of( "e2.a", ContainingA_.class, String.class, String.class, String.class );
					b = ValueFieldReference1.of( "e2.b", ContainingA_.class, String.class, String.class, String.class );
				}
			}
		}
	}

	public static class ContainingB_ extends MappedSuperclassThing_ {
		public static e3_.Absolute e3;

		public static HibernateOrmRootReferenceScope<ContainingB_, ContainingB> scope;

		static {
			e3 = new e3_.Absolute();

			scope = RootReferenceScopeImpl.of( ContainingB_.class, ContainingB.class );
		}

		public static class e3_ implements ObjectFieldReference<ContainingB_>, Property_Integer_a_ {

			@Override
			public String absolutePath() {
				return "e3";
			}

			@Override
			public Class<ContainingB_> scopeRootType() {
				return ContainingB_.class;
			}

			public static class Absolute extends e3_ {
				public static ValueFieldReference1<ContainingB_, Integer, Integer, Integer> a;

				static {
					a = ValueFieldReference1.of( "e3.a", ContainingB_.class, Integer.class, Integer.class, Integer.class );
				}
			}
		}
	}

	public static PredicateFinalStep utilMethodForPredicate(SearchPredicateFactory<? extends Property_String_a_> factory) {
		return factory.match().field( Property_String_a_.a ).matching( "a" );
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

	private static class RootReferenceScopeImpl<SR, E> implements HibernateOrmRootReferenceScope<SR, E> {

		private final Class<SR> rootReferenceType;
		private final Class<? extends E>[] entityClass;

		static <SR, E> HibernateOrmRootReferenceScope<SR, E> of(Class<SR> rootReferenceType,
				Class<? extends E>... entityClass) {
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
		public SearchScope<SR, E> create(SearchScopeProvider scopeProvider) {
			return scopeProvider.scope( Arrays.asList( entityClass ) );
		}
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}
}
