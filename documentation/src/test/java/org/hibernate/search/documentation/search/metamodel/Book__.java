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
public final class Book__
		implements
		org.hibernate.search.mapper.orm.scope.HibernateOrmRootReferenceScope<Book__, Book> {

	public static final Book__ INDEX = new Book__();

	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Book__, String, String, String, String> comment;
	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Book__, String, String, String, String> description;
	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Book__, Genre, Genre, String, String> genre;
	public final ValueFieldReferenceP0P13P2P6P7P9<Book__, Integer, Integer, Integer, Integer> pageCount;
	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<Book__, String, String, String, String> title;
	public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Book__, String, String, String, String> title_autocomplete;
	public final Book__authors__ authors;

	private Book__() {
		// simple value field references:
		this.comment = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "comment", Book__.class, String.class, String.class,
				String.class, String.class );
		this.description = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "description", Book__.class, String.class,
				String.class, String.class, String.class );
		this.genre = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "genre", Book__.class, Genre.class, Genre.class,
				String.class, String.class );
		this.pageCount = new ValueFieldReferenceP0P13P2P6P7P9<>( "pageCount", Book__.class, Integer.class, Integer.class,
				Integer.class, Integer.class );
		this.title = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<>( "title", Book__.class, String.class, String.class,
				String.class, String.class );
		this.title_autocomplete = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "title_autocomplete", Book__.class,
				String.class, String.class, String.class, String.class );
		// various object field references:
		this.authors = new Book__authors__();
	}

	@Override
	public Class<Book__> rootReferenceType() {
		return Book__.class;
	}

	@Override
	public org.hibernate.search.mapper.orm.scope.SearchScope<Book__, Book> scope(
			org.hibernate.search.mapper.orm.scope.SearchScopeProvider scopeProvider) {
		return scopeProvider.scope( Book.class );
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

	public static class ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, I, O, T, R>
			extends TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, I, O> {

		private final TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, T, T> mapping;
		private final TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, R, R> raw;
		private final TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, String, String> string;

		public ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2(
				String absolutePath,
				Class<SR> scopeRootType,
				Class<I> inputType,
				Class<O> outputType,
				Class<T> indexType,
				Class<R> rawType
		) {
			super( absolutePath, scopeRootType, org.hibernate.search.engine.search.common.ValueModel.MAPPING, inputType,
					outputType );
			this.mapping = new TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.INDEX, indexType, indexType );
			this.raw = new TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.RAW, rawType, rawType );
			this.string = new TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.STRING, String.class, String.class );
		}

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, T, T> mapping() {
			return mapping;
		}

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, R, R> raw() {
			return raw;
		}

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, String, String> string() {
			return string;
		}

	}

	public static class TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2<SR, I, O>
			implements org.hibernate.search.engine.search.reference.predicate.ExistsPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.TermsPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.WildcardPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.PhrasePredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.PrefixPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.QueryStringPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.RegexpPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.projection.FieldProjectionFieldReference<SR, O>,
			org.hibernate.search.engine.search.reference.projection.HighlightProjectionFieldReference<SR> {

		private final String absolutePath;
		private final Class<SR> scopeRootType;
		private final org.hibernate.search.engine.search.common.ValueModel valueModel;
		private final Class<I> predicateType;
		private final Class<O> projectionType;

		public TypedFieldReferenceP0P13P14P2P4P5P6P7P8P9R1R2(
				String absolutePath,
				Class<SR> scopeRootType,
				org.hibernate.search.engine.search.common.ValueModel valueModel,
				Class<I> predicateType,
				Class<O> projectionType) {
			this.absolutePath = absolutePath;
			this.scopeRootType = scopeRootType;
			this.valueModel = valueModel;
			this.predicateType = predicateType;
			this.projectionType = projectionType;
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

		public Class<O> projectionType() {
			return this.projectionType;
		}
	}

	public static class ValueFieldReferenceP0P13P2P6P7P9<SR, I, O, T, R> extends TypedFieldReferenceP0P13P2P6P7P9<SR, I> {

		private final TypedFieldReferenceP0P13P2P6P7P9<SR, T> mapping;
		private final TypedFieldReferenceP0P13P2P6P7P9<SR, R> raw;
		private final TypedFieldReferenceP0P13P2P6P7P9<SR, String> string;

		public ValueFieldReferenceP0P13P2P6P7P9(
				String absolutePath,
				Class<SR> scopeRootType,
				Class<I> inputType,
				Class<O> outputType,
				Class<T> indexType,
				Class<R> rawType
		) {
			super( absolutePath, scopeRootType, org.hibernate.search.engine.search.common.ValueModel.MAPPING, inputType );
			this.mapping = new TypedFieldReferenceP0P13P2P6P7P9<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.INDEX, indexType );
			this.raw = new TypedFieldReferenceP0P13P2P6P7P9<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.RAW, rawType );
			this.string = new TypedFieldReferenceP0P13P2P6P7P9<>( absolutePath, scopeRootType,
					org.hibernate.search.engine.search.common.ValueModel.STRING, String.class );
		}

		public TypedFieldReferenceP0P13P2P6P7P9<SR, T> mapping() {
			return mapping;
		}

		public TypedFieldReferenceP0P13P2P6P7P9<SR, R> raw() {
			return raw;
		}

		public TypedFieldReferenceP0P13P2P6P7P9<SR, String> string() {
			return string;
		}

	}

	public static class TypedFieldReferenceP0P13P2P6P7P9<SR, I>
			implements org.hibernate.search.engine.search.reference.predicate.ExistsPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.TermsPredicateFieldReference<SR>,
			org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.QueryStringPredicateFieldReference<SR, I>,
			org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference<SR, I> {

		private final String absolutePath;
		private final Class<SR> scopeRootType;
		private final org.hibernate.search.engine.search.common.ValueModel valueModel;
		private final Class<I> predicateType;

		public TypedFieldReferenceP0P13P2P6P7P9(
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


	public static class Book__authors__
			implements org.hibernate.search.engine.search.reference.object.NestedFieldReference<Book__> {

		public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Book__, String, String, String, String> firstName;
		public final ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<Book__, String, String, String, String> lastName;

		private Book__authors__() {
			// simple value field references:
			this.firstName = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "authors.firstName", Book__.class, String.class,
					String.class, String.class, String.class );
			this.lastName = new ValueFieldReferenceP0P13P14P2P4P5P6P7P8P9<>( "authors.lastName", Book__.class, String.class,
					String.class, String.class, String.class );
			// various object field references:

		}

		@Override
		public String absolutePath() {
			return "authors";
		}

		@Override
		public Class<Book__> scopeRootType() {
			return Book__.class;
		}

	}

}
