/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.util.rule;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.mapper.javabean.CloseableJavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.JavaBeanMappingBuilder;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public class JavaBeanMappingSetupHelper
		extends MappingSetupHelper<JavaBeanMappingSetupHelper.SetupContext, JavaBeanMappingBuilder, CloseableJavaBeanMapping> {

	private final MethodHandles.Lookup lookup;

	public JavaBeanMappingSetupHelper() {
		this( MethodHandles.lookup() );
	}

	/**
	 * @param lookup A {@link MethodHandles.Lookup} with private access to the test method,
	 * to be passed to initiators created by {@link SetupContext#setup(Class[])} or {@link SetupContext#setup(Set, Set)}
	 * so that the javabean mapper will be able to inspect classes defined in the test methods.
	 */
	public JavaBeanMappingSetupHelper(MethodHandles.Lookup lookup) {
		this.lookup = lookup;
	}

	@Override
	protected SetupContext createSetupContext(ConfigurationPropertySource propertySource) {
		return new SetupContext( propertySource );
	}

	@Override
	protected void close(CloseableJavaBeanMapping toClose) {
		toClose.close();
	}

	public class SetupContext
			extends MappingSetupHelper<SetupContext, JavaBeanMappingBuilder, CloseableJavaBeanMapping>.SetupContext {

		private final ConfigurationPropertySource propertySource;

		SetupContext(ConfigurationPropertySource propertySource) {
			this.propertySource = propertySource;
		}

		public JavaBeanMapping setup(Class<?> ... annotatedEntityTypes) {
			Set<Class<?>> classesSet = CollectionHelper.asLinkedHashSet( annotatedEntityTypes );
			return setup( classesSet, classesSet );
		}

		public JavaBeanMapping setup(Set<Class<?>> entityTypes, Set<Class<?>> annotatedTypes) {
			return withConfiguration( builder -> {
				builder.addEntityTypes( entityTypes );
				builder.annotationMapping().add( annotatedTypes );
			} )
					.setup();
		}

		@Override
		protected JavaBeanMappingBuilder createBuilder() {
			return JavaBeanMapping.builder( propertySource, lookup );
		}

		@Override
		protected void setProperty(JavaBeanMappingBuilder builder, String key, String value) {
			builder.setProperty( key, value );
		}

		@Override
		protected CloseableJavaBeanMapping build(JavaBeanMappingBuilder builder) {
			return builder.build();
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}
}
