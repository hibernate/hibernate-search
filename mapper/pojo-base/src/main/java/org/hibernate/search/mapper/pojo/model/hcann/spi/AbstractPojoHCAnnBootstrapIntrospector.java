/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.util.common.impl.StreamHelper;

public abstract class AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final ReflectionManager reflectionManager;

	private final PojoXClassOrdering typeOrdering;

	public AbstractPojoHCAnnBootstrapIntrospector(ReflectionManager reflectionManager) {
		this.reflectionManager = reflectionManager;
		this.typeOrdering = new PojoXClassOrdering( reflectionManager );
	}

	public Stream<Annotation> annotations(XAnnotatedElement xAnnotated) {
		return Arrays.stream( xAnnotated.getAnnotations() );
	}

	public XClass toXClass(Class<?> type) {
		return reflectionManager.toXClass( type );
	}

	public Map<String, XProperty> declaredFieldAccessXPropertiesByName(XClass xClass) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return xClass.getDeclaredProperties( XClass.ACCESS_FIELD ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	public Map<String, XProperty> declaredMethodAccessXPropertiesByName(XClass xClass) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return xClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	public Stream<Class<?>> ascendingSuperClasses(XClass xClass) {
		return typeOrdering.ascendingSuperTypes( xClass ).map( this::toClass );
	}

	public Stream<Class<?>> descendingSuperClasses(XClass xClass) {
		return typeOrdering.descendingSuperTypes( xClass ).map( this::toClass );
	}

	private Class<?> toClass(XClass xClass) {
		return reflectionManager.toClass( xClass );
	}

	private Collector<XProperty, ?, Map<String, XProperty>> xPropertiesByNameNoDuplicate() {
		return StreamHelper.toMap(
				XProperty::getName, Function.identity(),
				TreeMap::new // Sort properties by name for deterministic iteration
		);
	}
}
