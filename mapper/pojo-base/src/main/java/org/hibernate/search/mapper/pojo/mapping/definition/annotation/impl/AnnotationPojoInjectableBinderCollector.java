/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.models.UnknownClassException;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoInjectableBinderModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoInjectablePropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadWriteHandle;

public final class AnnotationPojoInjectableBinderCollector {

	private final PojoBootstrapIntrospector introspector;
	private List<PojoInjectableBinderModel<?>> injectableBinderModels;
	private Set<Class<?>> alreadyAddedBinders = new HashSet<>();
	private Map<Class<?>, List<InjectableBinderFieldInjector>> injectors = new HashMap<>();

	public AnnotationPojoInjectableBinderCollector(PojoBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	void processDiscoveredBinders() {
		if ( hasContent() ) {
			for ( PojoInjectableBinderModel<?> model : injectableBinderModels ) {
				List<InjectableBinderFieldInjector> injectorsPerModel = new ArrayList<>();
				for ( PojoInjectablePropertyModel<?> property : model.declaredProperties() ) {
					InjectableBinderFieldInjector injector = null;
					for ( Annotation annotation : property.annotations().toList() ) {
						Optional<InjectableBinderAnnotationProcessor<Annotation>> processor =
								getAnnotationProcessor( annotation );
						if ( processor.isPresent() ) {
							if ( injector != null ) {
								throw new IllegalStateException(
										"Found more than one Search annotations on a single injectable field" );
							}
							injector = processor.get().process( property, annotation );
							injectorsPerModel.add( injector );
						}
					}
				}
				injectors.put( model.rawType().typeIdentifier().javaClass(),
						Collections.unmodifiableList( injectorsPerModel ) );
			}
			injectableBinderModels = null;
		}
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> Optional<InjectableBinderAnnotationProcessor<A>> getAnnotationProcessor(A annotation) {
		if ( annotation instanceof FullTextField ) {
			return Optional
					.of( (InjectableBinderAnnotationProcessor<A>) FullTextFieldInjectableBinderAnnotationProcessor.INSTANCE );
		}
		else if ( annotation instanceof KeywordField ) {
			return Optional
					.of( (InjectableBinderAnnotationProcessor<A>) KeywordFieldInjectableBinderAnnotationProcessor.INSTANCE );
		}
		else {
			return Optional.empty();
		}
	}

	public void add(PropertyBinder binder) {
		Class<?> binderType = null;
		if ( binder instanceof BeanDelegatingBinder delegatingBinder ) {
			binderType = delegatingBinder.getDelegateType();
		}
		else {
			binderType = binder.getClass();
		}
		if ( alreadyAddedBinders.add( binderType ) ) {
			try {
				add( introspector.injectableBinderModel( binderType ) );
			}
			catch (UnknownClassException e) {
				// assume it's a lambda and we don't have what to collect from it anyway
				MappingLog.INSTANCE.cannotLoadClassDetailsButIgnore( binderType, e.getMessage() );
			}
		}
	}

	public AnnotationPojoInjectableBinderCollector add(PojoInjectableBinderModel<?> model) {
		initChildren();
		this.injectableBinderModels.add( model );
		return this;
	}

	public boolean hasContent() {
		return injectableBinderModels != null && !injectableBinderModels.isEmpty();
	}

	private void initChildren() {
		if ( this.injectableBinderModels == null ) {
			this.injectableBinderModels = new ArrayList<>();
		}
	}

	List<InjectableBinderFieldInjector> injector(Class<?> classType) {
		return injectors.getOrDefault( classType, List.of() );
	}


	interface InjectableBinderAnnotationProcessor<A extends Annotation> {
		InjectableBinderFieldInjector process(PojoInjectablePropertyModel<?> property, A annotation);
	}


	interface InjectableBinderFieldInjector {
		// todo: create some context class instead of this list of params...
		void inject(Object binder, IndexSchemaElement indexSchemaElement, NamedValues params);
	}

	private static class FullTextFieldInjectableBinderAnnotationProcessor
			implements InjectableBinderAnnotationProcessor<FullTextField> {

		static final FullTextFieldInjectableBinderAnnotationProcessor INSTANCE =
				new FullTextFieldInjectableBinderAnnotationProcessor();

		@Override
		public InjectableBinderFieldInjector process(PojoInjectablePropertyModel<?> property, FullTextField annotation) {
			String name = annotation.name();
			if ( name == null || name.isBlank() ) {
				name = property.name();
			}
			return new InjectableBinderFieldInjectorImpl<>( property, name, f -> {
				StringIndexFieldTypeOptionsStep<?> step = f.asString();

				Projectable projectable = annotation.projectable();
				if ( !Projectable.DEFAULT.equals( projectable ) ) {
					step.projectable( projectable );
				}

				Searchable searchable = annotation.searchable();
				if ( !Searchable.DEFAULT.equals( searchable ) ) {
					step.searchable( searchable );
				}

				if ( !annotation.searchAnalyzer().isEmpty() ) {
					step.searchAnalyzer( annotation.searchAnalyzer() );
				}

				Norms norms = annotation.norms();
				if ( !Norms.DEFAULT.equals( norms ) ) {
					step.norms( norms );
				}

				TermVector termVector = annotation.termVector();
				if ( !TermVector.DEFAULT.equals( termVector ) ) {
					step.termVector( termVector );
				}
				Highlightable[] highlightable = annotation.highlightable();
				if ( !( highlightable.length == 1 && Highlightable.DEFAULT.equals( highlightable[0] ) ) ) {
					step.highlightable(
							highlightable.length == 0 ? Collections.emptyList() : Arrays.asList( highlightable )
					);
				}

				return step;
			} );
		}
	}

	private static class KeywordFieldInjectableBinderAnnotationProcessor
			implements InjectableBinderAnnotationProcessor<KeywordField> {

		static final KeywordFieldInjectableBinderAnnotationProcessor INSTANCE =
				new KeywordFieldInjectableBinderAnnotationProcessor();

		@Override
		public InjectableBinderFieldInjector process(PojoInjectablePropertyModel<?> property, KeywordField annotation) {
			String name = annotation.name();
			if ( name == null || name.isBlank() ) {
				name = property.name();
			}
			return new InjectableBinderFieldInjectorImpl<>( property, name, f -> {
				StringIndexFieldTypeOptionsStep<?> step = f.asString();

				Projectable projectable = annotation.projectable();
				if ( !Projectable.DEFAULT.equals( projectable ) ) {
					step.projectable( projectable );
				}

				Searchable searchable = annotation.searchable();
				if ( !Searchable.DEFAULT.equals( searchable ) ) {
					step.searchable( searchable );
				}

				String normalizer = annotation.normalizer();
				if ( !normalizer.isEmpty() ) {
					step.normalizer( annotation.normalizer() );
				}

				Norms norms = annotation.norms();
				if ( !Norms.DEFAULT.equals( norms ) ) {
					step.norms( norms );
				}

				return step;
			} );
		}
	}

	private static class InjectableBinderFieldInjectorImpl<F> implements InjectableBinderFieldInjector {
		private final ValueReadWriteHandle<?> writeHandle;
		private final String relativeFieldName;
		private final Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<F>> typeContributor;

		private InjectableBinderFieldInjectorImpl(PojoInjectablePropertyModel<?> property, String relativeFieldName,
				Function<? super IndexFieldTypeFactory, ? extends IndexFieldTypeFinalStep<F>> typeContributor) {
			this.writeHandle = property.handle();
			this.relativeFieldName = relativeFieldName;
			this.typeContributor = typeContributor;
		}

		@Override
		public void inject(Object binder, IndexSchemaElement indexSchemaElement, NamedValues params) {
			IndexFieldReference<F> reference = indexSchemaElement.field( relativeFieldName, typeContributor ).toReference();
			writeHandle.set( binder, reference );
		}
	}
}
