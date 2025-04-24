/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.metamodel;

/**
 * Note: this class was created by an annotation processor and copied to the sources,
 * so that we do not run the AP on an entire documentation module.
 */
@javax.annotation.processing.Generated(value = "org.hibernate.search.metamodel.processor.HibernateSearchMetamodelProcessor")
public final class Author__
		implements
		org.hibernate.search.mapper.orm.scope.HibernateOrmRootReferenceScope<Author__, Author> {

	public static final Author__ INDEX = new Author__();

	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Author__, String, String, String, String> firstName;
	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Author__, String, String, String, String> lastName;

	private Author__() {
		// simple value field references:
		this.firstName = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "firstName", Author__.class, String.class,
				String.class, String.class, String.class );
		this.lastName = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "lastName", Author__.class, String.class, String.class,
				String.class, String.class );
		// various object field references:

	}

	@Override
	public Class<Author__> rootReferenceType() {
		return Author__.class;
	}

	@Override
	public org.hibernate.search.mapper.orm.scope.SearchScope<Author__, Author> scope(
			org.hibernate.search.mapper.orm.scope.SearchScopeProvider scopeProvider) {
		return scopeProvider.scope( Author.class );
	}

	public static class ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, I, O, T, R>
			extends TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, I> {

		private final TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, T> mapping;
		private final TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, R> raw;
		private final TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, String> string;

		public ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9(
				String absolutePath,
				Class<SR> scopeRootType,
				Class<I> inputType,
				Class<O> outputType,
				Class<T> indexType,
				Class<R> rawType
		) {
			super( absolutePath, scopeRootType, org.hibernate.search.engine.search.common.ValueModel.MAPPING, inputType );
			this.mapping = new TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.INDEX, indexType );
			this.raw = new TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.RAW, rawType );
			this.string = new TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.STRING, String.class );
		}

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, T> mapping() {
			return mapping;
		}

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, R> raw() {
			return raw;
		}

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, String> string() {
			return string;
		}

	}

	public static class TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9<SR, I>
			implements org.hibernate.search.engine.search.reference.predicate.ExistsPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.TermsPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.WildcardPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.PhrasePredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.PrefixPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.QueryStringPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.RegexpPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference<SR, I> {

		private final String absolutePath;
		private final Class<SR> scopeRootType;
		private final org.hibernate.search.engine.search.common.ValueModel valueModel;
		private final Class<I> predicateType;

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9(
				String absolutePath,
				Class<SR> scopeRootType,
				org.hibernate.search.engine.search.common.ValueModel valueModel,
				Class<I> predicateType) {
			this.absolutePath = absolutePath;
			this.scopeRootType = scopeRootType;
			this.valueModel = valueModel;
			this.predicateType = predicateType;
		}

		public String absolutePath() {
			return this.absolutePath;
		}

		public Class<SR> scopeRootType() {
			return this.scopeRootType;
		}

		public org.hibernate.search.engine.search.common.ValueModel valueModel() {
			return this.valueModel;
		}

		public Class<I> predicateType() {
			return this.predicateType;
		}
	}

}
