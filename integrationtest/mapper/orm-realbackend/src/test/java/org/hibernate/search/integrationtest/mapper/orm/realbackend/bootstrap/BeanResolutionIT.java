/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class BeanResolutionIT {

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Mock
	private BeanContainer beanContainerMock;

	/**
	 * Checks that Hibernate Search will not rely on CDI or Spring for default beans.
	 * This is important because there may be bugs in CDI or Spring
	 * (e.g. https://github.com/spring-projects/spring-framework/issues/25111)
	 * and we don't want those bugs to affect users who don't explicitly ask the retrieval of custom beans.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4096")
	void noExplicitBean_noCallToBeanContainer() {
		setupHelper.start()
				.withProperty( AvailableSettings.BEAN_CONTAINER, beanContainerMock )
				.setup( IndexedEntity.class );

		// Check that NO bean was retrieved from the BeanContainer
		verifyNoInteractions( beanContainerMock );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4096")
	void explicitBean_callToBeanContainer() {
		when( beanContainerMock
				.getBean( eq( "myConfigurer" ), eq( HibernateOrmSearchMappingConfigurer.class ), any(), any() ) )
				.thenReturn( new StubContainedBean<>( new HibernateOrmSearchMappingConfigurer() {
					@Override
					public void configure(HibernateOrmMappingConfigurationContext context) {
						context.programmaticMapping().type( IndexedEntity.class )
								.property( "integers" ).genericField( "integers2" );
					}
				} ) );

		SessionFactory sessionFactory = setupHelper.start()
				.withProperty( AvailableSettings.BEAN_CONTAINER, beanContainerMock )
				.withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, "myConfigurer" )
				.setup( IndexedEntity.class );

		// Check the bean was retrieved from the BeanContainer
		verify( beanContainerMock ).getBean(
				eq( "myConfigurer" ), eq( HibernateOrmSearchMappingConfigurer.class ), any(), any() );

		// Check the bean was actually used
		assertThat( Search.mapping( sessionFactory ).indexedEntity( IndexedEntity.class )
				.indexManager().descriptor().field( "integers2" ) )
				.isPresent();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static final class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		private Integer id;

		@FullTextField
		private String text;

		@ElementCollection
		@GenericField
		@OrderColumn
		private List<String> integers;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public List<String> getIntegers() {
			return integers;
		}

		public void setIntegers(List<String> integers) {
			this.integers = integers;
		}
	}

	private static class StubContainedBean<T> implements ContainedBean<T> {
		private final T bean;

		private StubContainedBean(T bean) {
			this.bean = bean;
		}

		@Override
		public T getBeanInstance() {
			return bean;
		}
	}
}
