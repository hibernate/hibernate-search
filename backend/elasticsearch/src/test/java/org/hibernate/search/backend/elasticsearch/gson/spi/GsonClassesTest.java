/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.jar.JandexAnnotationUtils.extractClass;
import static org.hibernate.search.util.impl.test.jar.JandexAnnotationUtils.findRuntimeAnnotations;
import static org.hibernate.search.util.impl.test.jar.JandexIndexingUtils.indexJarOrDirectory;
import static org.hibernate.search.util.impl.test.jar.JarUtils.determineJarOrDirectoryLocation;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.util.impl.test.logging.Log;

import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import com.google.gson.Gson;
import com.google.gson.TypeAdapterFactory;

public class GsonClassesTest {

	private static Index gsonIndex;
	private static Index backendElasticsearchIndex;

	@BeforeClass
	public static void index() throws IOException {
		gsonIndex = indexJarOrDirectory( determineJarOrDirectoryLocation( Gson.class, "Gson" ) );
		backendElasticsearchIndex = indexJarOrDirectory( determineJarOrDirectoryLocation(
				ElasticsearchExtension.class, "hibernate-search-backend-elasticsearch" ) );
	}

	@Test
	public void testNoMissingGsonAnnotatedClass() {
		Set<DotName> gsonAnnotations = findRuntimeAnnotations( gsonIndex );

		Set<DotName> annotatedClasses = new HashSet<>();
		Set<String> annotatedClassesAndSubclasses = new TreeSet<>();
		for ( DotName gsonAnnotation : gsonAnnotations ) {
			for ( AnnotationInstance annotationInstance : backendElasticsearchIndex.getAnnotations( gsonAnnotation ) ) {
				DotName className = extractClass( annotationInstance.target() ).name();
				annotatedClasses.add( className );
				annotatedClassesAndSubclasses.add( className.toString() );
			}
		}

		for ( DotName annotatedClass : annotatedClasses ) {
			for ( ClassInfo subclass : backendElasticsearchIndex.getAllKnownSubclasses( annotatedClass ) ) {
				annotatedClassesAndSubclasses.add( subclass.name().toString() );
			}
		}

		Log.INSTANCE.infof( "GSON-annotated classes and subclasses: %s", annotatedClassesAndSubclasses );
		assertThat( annotatedClassesAndSubclasses ).isNotEmpty();
		assertThat( GsonClasses.typesRequiringReflection() ).containsAll( annotatedClassesAndSubclasses );
	}

	@Test
	public void testNoMissingGsonContractImplementations() {
		List<DotName> gsonContracts = Collections.singletonList(
				DotName.createSimple( TypeAdapterFactory.class.getName() ) );

		Set<String> classes = new TreeSet<>();
		for ( DotName gsonContract : gsonContracts ) {
			for ( ClassInfo implementor : backendElasticsearchIndex.getAllKnownImplementors( gsonContract ) ) {
				classes.add( implementor.name().toString() );
			}
		}

		Log.INSTANCE.infof( "Gson contract implementations: %s", classes );
		assertThat( classes ).isNotEmpty();
		assertThat( GsonClasses.typesRequiringReflection() ).containsAll( classes );
	}

}