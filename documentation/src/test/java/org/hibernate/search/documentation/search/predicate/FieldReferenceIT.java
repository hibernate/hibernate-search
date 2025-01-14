/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.object.ObjectFieldReference;
import org.hibernate.search.engine.search.reference.predicate.ExistsPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.QueryStringPredicateFieldReference;
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
		entityManagerFactory = setupHelper.start().setup(
				MappedSuperclassThing.class, ContainingA.class, ContainingB.class, EmbeddedThing1.class, EmbeddedThing2.class,
				EmbeddedThing3.class, ContainingASub1.class, ContainingASub2.class
		);
		initData();
	}

	@Test
	void smoke() {
		withinSearchSession( searchSession -> {

			SearchScope<ContainingA__, ContainingA> scope = ContainingA__.INDEX.scope( searchSession );

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( ContainingA__.INDEX.a ) )
							.where( f -> f.bool()
									.should( utilMethodForPredicate( f, ContainingA__.INDEX ) )
									.should( utilMethodForPredicate( f, ContainingA__.INDEX.e1 ) )
									.should( utilMethodForPredicate( f, ContainingA__.INDEX.e2 ) )
							)
							.fetchHits( 20 )
			).containsOnly( "a" );

			assertThat(
					searchSession.search( ContainingA__.INDEX )
							.select( f -> f.field( ContainingA__.INDEX.a ) )
							.where( f -> f.match().field( ContainingA__.INDEX.sub1 ).matching( "a1" ) )
							.fetchHits( 20 )
			).containsOnly( "a1" );

			assertThat(
					searchSession.search( ContainingA__.INDEX )
							.select( f -> f.field( ContainingA__.INDEX.a ) )
							.where( f -> utilMethodForPredicateNoProjection( f, ContainingA__.INDEX.e3 ) )
							.fetchHits( 20 )
			).containsOnly( "a" );

		} );
	}


	public static <SR extends ContainingA_e1_e2_Intersection<SR>> PredicateFinalStep utilMethodForPredicate(
			SearchPredicateFactory<SR> factory, ContainingA_e1_e2_Intersection<SR> reference) {
		return factory.match().field( reference.a() ).matching( "a" );
	}


	public static <SR extends ContainingA__> PredicateFinalStep utilMethodForPredicateNoProjection(
			SearchPredicateFactory<SR> factory, ContainingA_e1_e2_e3_Intersection<SR> reference) {
		return factory.match().field( reference.a() ).matching( "a" );
	}

	@Test
	void smoke2() {
		withinSearchSession( searchSession -> {

			SearchScope<Object, MappedSuperclassThing> scope = searchSession.scope( List.of( ContainingA.class ) );

			assertThat(
					searchSession.search( scope )
							.select( f -> f.field( "a" ) )
							.where( f -> f.exists().field( "commonSub" ) )
							.fetchHits( 20 )
			).containsOnly( "a1", "a2" );

		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			ContainingA a = new ContainingA();
			a.id = 1L;
			a.a = "a";
			a.e1 = new EmbeddedThing1();
			a.e2 = new EmbeddedThing1();
			a.e3 = new EmbeddedThing2();
			a.e1.a = "a";
			a.e1.b = "b";

			a.e2.a = "a";
			a.e2.b = "b";

			a.e3.a = "a";

			// -------------------
			ContainingASub1 a1 = new ContainingASub1();
			a1.id = 2L;
			a1.a = "a1";
			a1.e1 = new EmbeddedThing1();
			a1.e2 = new EmbeddedThing1();
			a1.e1.a = "a1";
			a1.e1.b = "b1";

			a1.e2.a = "a1";
			a1.e2.b = "b1";

			a1.commonSub = "a1";
			a1.sub1 = "a1";

			// -------------------
			ContainingASub2 a2 = new ContainingASub2();
			a2.id = 3L;
			a2.a = "a2";
			a2.e1 = new EmbeddedThing1();
			a2.e2 = new EmbeddedThing1();
			a2.e1.a = "a2";
			a2.e1.b = "b2";

			a2.e2.a = "a2";
			a2.e2.b = "b2";

			a2.commonSub = "a2";
			a2.sub2 = "a2";

			// -------------------
			ContainingB b = new ContainingB();
			b.id = 10L;
			b.a = "b";
			b.e2 = new EmbeddedThing3();
			b.e2.a = -100;
			b.e3 = new EmbeddedThing3();
			b.e3.a = -10;


			entityManager.persist( a );
			entityManager.persist( a1 );
			entityManager.persist( a2 );
			entityManager.persist( b );
		} );
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
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER)
	public static class ContainingA extends MappedSuperclassThing {
		@IndexedEmbedded
		@Embedded
		EmbeddedThing1 e1;
		@IndexedEmbedded
		@Embedded
		EmbeddedThing1 e2;
		@IndexedEmbedded
		@Embedded
		EmbeddedThing2 e3;
	}

	@Embeddable
	public static class EmbeddedThing1 {
		@FullTextField(projectable = Projectable.YES)
		String a;
		@FullTextField(projectable = Projectable.YES)
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
	public static class ContainingASub1 extends ContainingA {
		@FullTextField(projectable = Projectable.YES)
		String sub1;
		@FullTextField(projectable = Projectable.YES)
		String commonSub;
	}

	@Entity
	public static class ContainingASub2 extends ContainingA {
		@FullTextField(projectable = Projectable.YES)
		String sub2;
		@FullTextField(projectable = Projectable.YES)
		String commonSub;
	}

	@Entity
	@Indexed
	public static class ContainingB extends MappedSuperclassThing {
		@IndexedEmbedded
		EmbeddedThing3 e2;
		@IndexedEmbedded
		EmbeddedThing3 e3;
	}

	@Embeddable
	public static class EmbeddedThing3 {
		@GenericField
		Integer a;
		// some other fields maybe different from EmbeddedThing1/EmbeddedThing2
	}


	// IMPL_NOTE: note cannot use the EntityClassName_ since ORM picks it up and tries to do its thing...
	// so we'd need to come up with a different naming strategy...
	public static class ContainingA__
			implements HibernateOrmRootReferenceScope<ContainingA__, ContainingA>,
			ContainingA_e1_e2_Intersection<ContainingA__> {

		// IMPL_NOTE: Maybe let's use the INDEX name?
		// also I'm thinking we can make it configurable and let the user decide how to call this variable...
		public static final ContainingA__ INDEX = new ContainingA__();

		public final ValueFieldReference1<ContainingA__, String, String, String> a;
		public final e1_ e1;
		public final e2_ e2;
		public final e3_ e3;

		// ContainingASub1
		public final ValueFieldReference1<ContainingA__, String, String, String> sub1;

		// ContainingASub2
		public final ValueFieldReference1<ContainingA__, String, String, String> sub2;

		public final ValueFieldReference1<ContainingA__, String, String, String> commonSub;


		public ContainingA__() {
			a = ValueFieldReference1.of( "e2.a", ContainingA__.class, String.class, String.class, String.class );
			e1 = new e1_();
			e2 = new e2_();
			e3 = new e3_();
			sub1 = ValueFieldReference1.of( "sub1", ContainingA__.class, String.class, String.class, String.class );
			sub2 = ValueFieldReference1.of( "sub2", ContainingA__.class, String.class, String.class, String.class );
			commonSub = ValueFieldReference1.of( "commonSub", ContainingA__.class, String.class, String.class, String.class );
		}

		@Override
		public SearchScope<ContainingA__, ContainingA> scope(SearchScopeProvider scopeProvider) {
			return scopeProvider.scope( ContainingA.class );
		}

		@Override
		public Class<ContainingA__> rootReferenceType() {
			return ContainingA__.class;
		}

		@Override
		public ValueFieldReference1<ContainingA__, String, String, String> a() {
			return a;
		}

		public static class e1_
				implements ObjectFieldReference<ContainingA__>,
				ContainingA_e1_e2_Intersection<ContainingA__> {

			public final ValueFieldReference1<ContainingA__, String, String, String> a;
			public final ValueFieldReference1<ContainingA__, String, String, String> b;

			public e1_() {
				a = ValueFieldReference1.of( "e1.a", ContainingA__.class, String.class, String.class, String.class );
				b = ValueFieldReference1.of( "e1.b", ContainingA__.class, String.class, String.class, String.class );
			}

			@Override
			public String absolutePath() {
				return "e1";
			}

			@Override
			public Class<ContainingA__> scopeRootType() {
				return ContainingA__.class;
			}

			@Override
			public ValueFieldReference1<ContainingA__, String, String, String> a() {
				return a;
			}
		}

		public static class e2_
				implements ObjectFieldReference<ContainingA__>,
				ContainingA_e1_e2_Intersection<ContainingA__>,
				ContainingA_e1_e2_e3_Intersection<ContainingA__> {

			public final ValueFieldReference1<ContainingA__, String, String, String> a;
			public final ValueFieldReference1<ContainingA__, String, String, String> b;

			public e2_() {
				a = ValueFieldReference1.of( "e2.a", ContainingA__.class, String.class, String.class, String.class );
				b = ValueFieldReference1.of( "e2.b", ContainingA__.class, String.class, String.class, String.class );
			}

			@Override
			public String absolutePath() {
				return "e2";
			}

			@Override
			public Class<ContainingA__> scopeRootType() {
				return ContainingA__.class;
			}

			@Override
			public ValueFieldReference1<ContainingA__, String, String, String> a() {
				return a;
			}
		}

		public static class e3_
				implements ObjectFieldReference<ContainingA__>,
				ContainingA_e1_e2_e3_Intersection<ContainingA__> {

			public final ValueFieldReference2<ContainingA__, String, String> a;

			public e3_() {
				a = ValueFieldReference2.of( "e3.a", ContainingA__.class, String.class, String.class );
			}

			@Override
			public String absolutePath() {
				return "e3";
			}

			@Override
			public Class<ContainingA__> scopeRootType() {
				return ContainingA__.class;
			}

			@Override
			public ValueFieldReference2<ContainingA__, String, String> a() {
				return a;
			}
		}
	}

	public interface ContainingA_e1_e2_e3_Intersection<SR> {

		TraitsIntersection<SR, String> a();

		interface TraitsIntersection<SR, T>
				extends MatchPredicateFieldReference<SR, T>,
				ExistsPredicateFieldReference<SR>,
				QueryStringPredicateFieldReference<SR, T> {

			@Override
			default ValueModel valueModel() {
				return ValueModel.MAPPING;
			}
		}
	}

	// the same field with the same traits:
	public interface ContainingA_e1_e2_Intersection<SR> {

		TraitsIntersection<SR, String, String> a();

		interface TraitsIntersection<SR, T, P>
				extends FieldProjectionFieldReference<SR, P>,
				MatchPredicateFieldReference<SR, T>,
				ExistsPredicateFieldReference<SR>,
				QueryStringPredicateFieldReference<SR, T> {

			@Override
			default ValueModel valueModel() {
				return ValueModel.MAPPING;
			}
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
			super( absolutePath, ValueModel.MAPPING, containing, inputType, projectionType );
			this.noConverter = new TypedFieldReference1<>( absolutePath, ValueModel.INDEX, containing, indexType, indexType );
			this.string =
					new TypedFieldReference1<>( absolutePath, ValueModel.STRING, containing, String.class, String.class );
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
			MatchPredicateFieldReference<SR, T>,
			ExistsPredicateFieldReference<SR>,
			QueryStringPredicateFieldReference<SR, T>,
			ContainingA_e1_e2_Intersection.TraitsIntersection<SR, T, P>,
			ContainingA_e1_e2_e3_Intersection.TraitsIntersection<SR, T> {

		private final String absolutePath;
		private final ValueModel valueModel;
		private final Class<SR> containing;
		private final Class<T> input;
		private final Class<P> projection;

		public TypedFieldReference1(String absolutePath, ValueModel valueModel, Class<SR> containing, Class<T> input,
				Class<P> projection) {
			this.absolutePath = absolutePath;
			this.valueModel = valueModel;
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
		public ValueModel valueModel() {
			return valueModel;
		}

		@Override
		public Class<P> projectionType() {
			return projection;
		}

	}

	public static class ValueFieldReference2<E, T, V> extends TypedFieldReference2<E, T> {

		public static <E, T, V> ValueFieldReference2<E, T, V> of(
				String path,
				Class<E> documentReferenceClass,
				Class<T> t,
				Class<V> v) {
			return new ValueFieldReference2<>( path, documentReferenceClass, t, v );
		}

		private final TypedFieldReference2<E, V> noConverter;
		private final TypedFieldReference2<E, String> string;

		public ValueFieldReference2(String absolutePath, Class<E> containing, Class<T> inputType, Class<V> indexType) {
			super( absolutePath, ValueModel.MAPPING, containing, inputType );
			this.noConverter = new TypedFieldReference2<>( absolutePath, ValueModel.INDEX, containing, indexType );
			this.string =
					new TypedFieldReference2<>( absolutePath, ValueModel.STRING, containing, String.class );
		}

		public TypedFieldReference2<E, V> noConverter() {
			return noConverter;
		}


		public TypedFieldReference2<E, String> asString() {
			return string;
		}

	}

	public static class TypedFieldReference2<SR, T>
			implements MatchPredicateFieldReference<SR, T>,
			ExistsPredicateFieldReference<SR>,
			QueryStringPredicateFieldReference<SR, T>,
			ContainingA_e1_e2_e3_Intersection.TraitsIntersection<SR, T> {

		private final String absolutePath;
		private final ValueModel valueModel;
		private final Class<SR> containing;
		private final Class<T> input;

		public TypedFieldReference2(String absolutePath, ValueModel valueModel, Class<SR> containing, Class<T> input) {
			this.absolutePath = absolutePath;
			this.valueModel = valueModel;
			this.containing = containing;
			this.input = input;
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
		public ValueModel valueModel() {
			return valueModel;
		}
	}

	private void withinSearchSession(Consumer<SearchSession> action) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			action.accept( searchSession );
		} );
	}
}
