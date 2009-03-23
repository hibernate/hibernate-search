package org.hibernate.search.impl;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.beans.Introspector;

import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.ReflectionUtil;
import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.EntityDescriptor;
import org.hibernate.search.cfg.PropertyDescriptor;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Fields;

/**
 * @author Emmanuel Bernard
 */
public class MappingModelMetadataProvider implements MetadataProvider {

	private static final Filter FILTER = new Filter() {
		public boolean returnStatic() {
			return false;
		}

		public boolean returnTransient() {
			return true;
		}
	};

	private final MetadataProvider delegate;
	private final SearchMapping mapping;
	private final Map<AnnotatedElement, AnnotationReader> cache = new HashMap<AnnotatedElement, AnnotationReader>(100);

	public MappingModelMetadataProvider(MetadataProvider delegate, SearchMapping mapping) {
		this.delegate = delegate;
		this.mapping = mapping;
	}
	public Map<Object, Object> getDefaults() {
		return delegate.getDefaults();
	}

	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		AnnotationReader reader = cache.get(annotatedElement);
		if (reader == null) {
			reader = new MappingModelAnnotationReader( mapping, delegate, annotatedElement);
			cache.put( annotatedElement, reader );
		}
		return reader;
	}

	private static class MappingModelAnnotationReader implements AnnotationReader {
		private AnnotationReader delegate;
		private SearchMapping mapping;
		private transient Annotation[] annotationsArray;
		private transient Map<Class<? extends Annotation>, Annotation> annotations;
		private Class<?> entityType;
		private ElementType elementType;
		private String propertyName;

		public MappingModelAnnotationReader(SearchMapping mapping, MetadataProvider delegate, AnnotatedElement el) {
			this.delegate = delegate.getAnnotationReader( el );
			this.mapping = mapping;
			if ( el instanceof Class ) {
				entityType = (Class) el;
			}
			else if ( el instanceof Field ) {
				Field field = (Field) el;
				entityType = field.getDeclaringClass();
				propertyName = field.getName();
				elementType = ElementType.FIELD;
			}
			else if ( el instanceof Method ) {
				Method method = (Method) el;
				entityType = method.getDeclaringClass();
				propertyName = method.getName();
				if ( ReflectionUtil.isProperty(
						method,
						null, //this is yukky!! we'd rather get the TypeEnvironment()
						FILTER
				) ) {
					if ( propertyName.startsWith( "get" ) ) {
						propertyName = Introspector.decapitalize( propertyName.substring( "get".length() ) );
					}
					else if ( propertyName.startsWith( "is" ) ) {
						propertyName = Introspector.decapitalize( propertyName.substring( "is".length() ) );
					}
					else {
						throw new RuntimeException( "Method " + propertyName + " is not a property getter" );
					}
					elementType = ElementType.METHOD;
				}
				else {
					throw new SearchException( "Error in programmatic mapping. Method " + propertyName + " is not a property getter" );
				}
			}
			else {
				entityType = null;
				propertyName = null;
			}
		}

		/**
		 * Consider the class to be free of Hibernate Search annotations. Does nto attempt to merge
		 * data.
		 * TODO do merge data? or safe-guard against errors
		 */
		private void initAnnotations() {
			if ( annotationsArray == null ) {
				annotations = new HashMap<Class<? extends Annotation>, Annotation>();
				delegatesAnnotationReading();
				if (entityType != null) {
					final EntityDescriptor entity = mapping.getEntityDescriptor( entityType );
					if (entity != null) {
						if (propertyName == null) {
							//entityType overriding
							createIndexed( entity );
						}
						else {
							final PropertyDescriptor property = entity.getPropertyDescriptor( propertyName, elementType );
							if (property != null) {
								// property name overriding
								createFields( property );
							}
						}
					}
				}
				else {
					delegatesAnnotationReading();
				}

				populateAnnotationArray();
			}
		}

		private void createFields(PropertyDescriptor property) {
			final Set<Map<String,Object>> fields = property.getFields();
			List<org.hibernate.search.annotations.Field> fieldAnnotations =
					new ArrayList<org.hibernate.search.annotations.Field>( fields.size() );
			for(Map<String, Object> field : fields) {
				AnnotationDescriptor fieldAnnotation = new AnnotationDescriptor( org.hibernate.search.annotations.Field.class );
				for ( Map.Entry<String, Object> entry : field.entrySet() ) {
					if ( entry.getKey().equals( "analyzer" ) ) {
						AnnotationDescriptor analyzerAnnotation = new AnnotationDescriptor( Analyzer.class );
						@SuppressWarnings( "unchecked" )
						Map<String, Object> analyzer = (Map<String, Object>) entry.getValue();
						for( Map.Entry<String, Object> analyzerEntry : analyzer.entrySet() ) {
							analyzerAnnotation.setValue( analyzerEntry.getKey(), analyzerEntry.getValue() );
						}
						fieldAnnotation.setValue( "analyzer", AnnotationFactory.create( analyzerAnnotation ) );
					}
					else {
						fieldAnnotation.setValue( entry.getKey(), entry.getValue() );
					}
				}
				fieldAnnotations.add( (org.hibernate.search.annotations.Field) AnnotationFactory.create( fieldAnnotation ) );
			}
			AnnotationDescriptor fieldsAnnotation = new AnnotationDescriptor( Fields.class );

			final org.hibernate.search.annotations.Field[] fieldArray =
					new org.hibernate.search.annotations.Field[fieldAnnotations.size()];
			fieldsAnnotation.setValue( "value", fieldAnnotations.toArray( fieldArray ));
			annotations.put( Fields.class, AnnotationFactory.create( fieldsAnnotation ) );
		}

		private void createIndexed(EntityDescriptor entity) {
			Class<? extends Annotation> annotationType = Indexed.class;
			AnnotationDescriptor annotation = new AnnotationDescriptor( annotationType );
			for ( Map.Entry<String, Object> entry : entity.getIndexed().entrySet() ) {
				annotation.setValue( entry.getKey(), entry.getValue() );
			}
			annotations.put( annotationType, AnnotationFactory.create( annotation ) );
		}

		private void populateAnnotationArray() {
			annotationsArray = new Annotation[ annotations.size() ];
			int index = 0;
			for( Annotation ann: annotations.values() ) {
				annotationsArray[index] = ann;
				index++;
			}
		}

		private void delegatesAnnotationReading() {
			for ( Annotation a : delegate.getAnnotations() ) {
				annotations.put( a.annotationType(), a );
			}
		}

		@SuppressWarnings( "unchecked" )
		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			initAnnotations();
			return (T) annotations.get( annotationType );
		}

		@SuppressWarnings( "unchecked" )
		public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
			initAnnotations();
			return (T) annotations.get( annotationType ) != null;
		}

		public Annotation[] getAnnotations() {
			initAnnotations();
			return new Annotation[0];  //To change body of implemented methods use File | Settings | File Templates.
		}
	}
}
