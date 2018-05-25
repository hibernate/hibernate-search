/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingFactory;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingKey;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingInitiatorImpl;

public final class JavaBeanMappingInitiator extends PojoMappingInitiatorImpl<JavaBeanMapping> {

	public JavaBeanMappingInitiator(SearchMappingRepositoryBuilder mappingRepositoryBuilder) {
		this( mappingRepositoryBuilder, true );
	}

	public JavaBeanMappingInitiator(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			boolean annotatedTypeDiscoveryEnabled) {
		this( mappingRepositoryBuilder, MethodHandles.publicLookup(), annotatedTypeDiscoveryEnabled, false );
	}

	public JavaBeanMappingInitiator(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			boolean annotatedTypeDiscoveryEnabled, boolean multiTenancyEnabled) {
		this( mappingRepositoryBuilder, MethodHandles.publicLookup(), annotatedTypeDiscoveryEnabled, multiTenancyEnabled );
	}

	public JavaBeanMappingInitiator(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			MethodHandles.Lookup lookup, boolean annotatedTypeDiscoveryEnabled, boolean multiTenancyEnabled) {
		this( mappingRepositoryBuilder, new JavaBeanBootstrapIntrospector( lookup ), annotatedTypeDiscoveryEnabled, multiTenancyEnabled );
	}

	private JavaBeanMappingInitiator(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			JavaBeanBootstrapIntrospector introspector, boolean annotatedTypeDiscoveryEnabled, boolean multiTenancyEnabled) {
		super(
				mappingRepositoryBuilder, new JavaBeanMappingKey(),
				new JavaBeanMappingFactory(),
				introspector, false,
				annotatedTypeDiscoveryEnabled, multiTenancyEnabled
		);
	}
}
