/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.mapping.SearchMappingBuilder;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public final class JavaBeanMappingSetupHelper
		extends MappingSetupHelper<JavaBeanMappingSetupHelper.SetupContext, SearchMappingBuilder, CloseableSearchMapping> {

	/**
	 * @param lookup A {@link MethodHandles.Lookup} with private access to the test method,
	 * to be passed to mapping builders created by {@link SetupContext#setup(Class[])} or {@link SetupContext#setup()}
	 * so that the javabean mapper will be able to inspect classes defined in the test methods.
	 * @param backendMock A backend mock.
	 */
	public static JavaBeanMappingSetupHelper withBackendMock(MethodHandles.Lookup lookup, BackendMock backendMock) {
		return new JavaBeanMappingSetupHelper( lookup, BackendSetupStrategy.withBackendMocks( backendMock ) );
	}

	public static JavaBeanMappingSetupHelper withBackendMocks(MethodHandles.Lookup lookup,
			BackendMock defaultBackendMock, BackendMock ... otherBackendMocks) {
		return new JavaBeanMappingSetupHelper(
				lookup,
				BackendSetupStrategy.withBackendMocks( defaultBackendMock, otherBackendMocks )
		);
	}

	private final MethodHandles.Lookup lookup;

	private JavaBeanMappingSetupHelper(MethodHandles.Lookup lookup, BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
		this.lookup = lookup;
	}

	@Override
	protected SetupContext createSetupContext() {
		return new SetupContext();
	}

	@Override
	protected void close(CloseableSearchMapping toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends MappingSetupHelper<SetupContext, SearchMappingBuilder, CloseableSearchMapping>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<String, Object> properties = new LinkedHashMap<>();

		SetupContext() {
			// Ensure overridden properties will be applied
			withConfiguration( builder -> properties.forEach( builder::setProperty ) );
		}

		@Override
		public SetupContext withProperty(String key, Object value) {
			properties.put( key, value );
			return thisAsC();
		}

		public SetupContext withAnnotatedEntityTypes(Class<?> ... annotatedEntityTypes) {
			return withAnnotatedEntityTypes( CollectionHelper.asLinkedHashSet( annotatedEntityTypes ) );
		}

		public SetupContext withAnnotatedEntityTypes(Set<Class<?>> annotatedEntityTypes) {
			return withConfiguration( builder -> {
				builder.addEntityTypes( annotatedEntityTypes );
				builder.annotationMapping().add( annotatedEntityTypes );
			} );
		}

		public SetupContext withAnnotatedTypes(Class<?> ... annotatedTypes) {
			return withAnnotatedTypes( CollectionHelper.asLinkedHashSet( annotatedTypes ) );
		}

		public SetupContext withAnnotatedTypes(Set<Class<?>> annotatedTypes) {
			return withConfiguration( builder -> builder.annotationMapping().add( annotatedTypes ) );
		}

		public SearchMapping setup(Class<?> ... annotatedEntityTypes) {
			return withAnnotatedEntityTypes( annotatedEntityTypes ).setup();
		}

		@Override
		protected SearchMappingBuilder createBuilder() {
			return SearchMapping.builder( lookup ).setProperties( properties );
		}

		@Override
		protected CloseableSearchMapping build(SearchMappingBuilder builder) {
			return builder.build();
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}
}
