/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.embedded.depth.PersonWithBrokenSocialSecurityNumber;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.junit.Test;

/**
 * The order in which the XClass methods will be listed varies depending on the platform and JVM,
 * in particular we had an issue with annotations {@code @Id} and {@code @DocumentId}
 * when encountering them in unexpected order. This test verifies iteration order doesn't affect
 * our capability to startup correctly.
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1048")
public class UnorderedIdScanTest {

	@Test
	public void naturalSortAnnotationsRead() {
		ReflectionManager reflectionManager = new JavaReflectionManager();
		XClass mappedXClass = reflectionManager.toXClass( PersonWithBrokenSocialSecurityNumber.class );
		tryCreatingDocumentBuilder( mappedXClass, reflectionManager );
		//No assertions needed: we just verify the previous statements won't throw an exception
	}

	@Test
	public void invertedSortAnnotationsRead() {
		JavaReflectionManager reflectionManager = new TrickedJavaReflectionManager<PersonWithBrokenSocialSecurityNumber>( PersonWithBrokenSocialSecurityNumber.class );
		XClass mappedXClass = reflectionManager.toXClass( PersonWithBrokenSocialSecurityNumber.class );
		XClass reverted = new DeclaredMethodsReverter( mappedXClass );
		tryCreatingDocumentBuilder( reverted, reflectionManager );
		//No assertions needed: we just verify the previous statements won't throw an exception
	}

	private static void tryCreatingDocumentBuilder(XClass mappedXClass, ReflectionManager reflectionManager) {
		SearchConfiguration searchConfiguration = new HibernateManualConfiguration();
		ConfigContext context = new ConfigContext( searchConfiguration, new BuildContextForTest( searchConfiguration ) );
		MetadataProvider metadataProvider = new AnnotationMetadataProvider( reflectionManager, context );
		Class mappedClazz = reflectionManager.toClass( mappedXClass );
		IndexedTypeIdentifier type = new PojoIndexedTypeIdentifier( mappedClazz );
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( type,
				LuceneEmbeddedIndexManagerType.INSTANCE );
		new DocumentBuilderIndexedEntity( mappedXClass,
				typeMetadata,
				context,
				reflectionManager,
				Collections.<XClass>emptySet(),
				DefaultInstanceInitializer.DEFAULT_INITIALIZER );
	}

	/**
	 * The real JavaReflectionManager would throw an error when the toClass method is invoked on an XClass
	 * instance it didn't create, so we trick that.
	 */
	static class TrickedJavaReflectionManager<T> extends JavaReflectionManager {

		private final Class<T> class1;

		public TrickedJavaReflectionManager(Class<T> class1) {
			this.class1 = class1;
		}

		@Override
		public Class toClass(XClass xClazz) {
			try {
				return super.toClass( xClazz );
			}
			catch (IllegalArgumentException e) {
				return class1;
			}
		}

		@Override
		public boolean equals(XClass class1, Class class2) {
			if ( class1 instanceof DeclaredMethodsReverter ) {
				DeclaredMethodsReverter wrapper = (DeclaredMethodsReverter) class1;
				return super.equals( wrapper.delegate, class2 );
			}
			else {
				return super.equals( class1, class2 );
			}
		}

	}

	/**
	 * Delegate all methods to a wrapped XClass instance but revert the order
	 * of properties when listing them.
	 */
	static class DeclaredMethodsReverter implements XClass {

		private final XClass delegate;
		DeclaredMethodsReverter(XClass delegate) {
			this.delegate = delegate;
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> arg0) {
			return delegate.getAnnotation( arg0 );
		}

		@Override
		public Annotation[] getAnnotations() {
			return delegate.getAnnotations();
		}

		@Override
		public <T extends Annotation> boolean isAnnotationPresent(Class<T> arg0) {
			return delegate.isAnnotationPresent( arg0 );
		}

		@Override
		public List<XMethod> getDeclaredMethods() {
			List<XMethod> declaredMethods = delegate.getDeclaredMethods();
			Collections.reverse( declaredMethods );
			return declaredMethods;
		}

		@Override
		public List<XProperty> getDeclaredProperties(String arg0) {
			List<XProperty> declaredProperties = delegate.getDeclaredProperties( arg0 );
			Collections.reverse( declaredProperties );
			return declaredProperties;
		}

		@Override
		public List<XProperty> getDeclaredProperties(String arg0, Filter arg1) {
			List<XProperty> declaredProperties = delegate.getDeclaredProperties( arg0, arg1 );
			Collections.reverse( declaredProperties );
			return declaredProperties;
		}

		@Override
		public XClass[] getInterfaces() {
			return delegate.getInterfaces();
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public XClass getSuperclass() {
			return delegate.getSuperclass();
		}

		@Override
		public boolean isAbstract() {
			return delegate.isAbstract();
		}

		@Override
		public boolean isAssignableFrom(XClass arg0) {
			return delegate.isAssignableFrom( arg0 );
		}

		@Override
		public boolean isEnum() {
			return delegate.isEnum();
		}

		@Override
		public boolean isInterface() {
			return delegate.isInterface();
		}

		@Override
		public boolean isPrimitive() {
			return delegate.isPrimitive();
		}

	}

}
