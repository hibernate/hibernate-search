/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import static org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils.getServiceOrEmpty;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.models.spi.ModelsConfiguration;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.provider.Arguments;

public abstract class AbstractHibernateOrmBootstrapIntrospectorPerReflectionStrategyTest {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( ValueHandleFactory.usingJavaLangReflect() ),
				Arguments.of( ValueHandleFactory.usingMethodHandle( MethodHandles.publicLookup() ) )
		);
	}

	private final List<AutoCloseable> toClose = new ArrayList<>();

	@AfterEach
	void cleanup() throws Exception {
		try ( Closer<Exception> closer = new Closer<>() ) {
			closer.pushAll( AutoCloseable::close, toClose );
		}
	}

	@SuppressWarnings("deprecation") // There's no other way to access the reflection manager
	final HibernateOrmBootstrapIntrospector createIntrospector(ValueHandleFactory valueHandleFactory,
			Class<?>... entityClasses) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		// Some properties that are not relevant to our test, but necessary to create the Metadata
		registryBuilder.applySetting( AvailableSettings.DIALECT, H2Dialect.class );
		StandardServiceRegistry serviceRegistry = registryBuilder.build();
		toClose.add( serviceRegistry );

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		for ( Class<?> entityClass : entityClasses ) {
			metadataSources.addAnnotatedClass( entityClass );
		}
		Metadata metadata = metadataSources.buildMetadata();

		MetadataImplementor metadataImplementor = (MetadataImplementor) metadata;
		var context = createModelBuildingContext( metadataImplementor.getTypeConfiguration()
				.getMetadataBuildingContext().getBootstrapContext() );
		HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider =
				HibernateOrmBasicTypeMetadataProvider.create( metadata );

		return HibernateOrmBootstrapIntrospector.create( basicTypeMetadataProvider, context.getClassDetailsRegistry(),
				valueHandleFactory
		);
	}

	public static ModelsContext createModelBuildingContext(BootstrapContext bootstrapContext) {
		ClassLoaderService classLoaderService =
				getServiceOrEmpty( bootstrapContext.getServiceRegistry(), ClassLoaderService.class )
						.orElseThrow();
		return new ModelsConfiguration()
				.setClassLoading( new ClassLoaderServiceLoading( classLoaderService ) )
				.setRegistryPrimer( ModelsHelper::preFillRegistries )
				.bootstrap();
	}
}
