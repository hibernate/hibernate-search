/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.reporting.failurehandler;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FailureHandlerIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@RegisterExtension
	public StaticCounters staticCounters = StaticCounters.create();

	@Test
	void smoke() {
		assertThat( staticCounters.get( MyFailureHandler.INSTANCES ) ).isEqualTo( 0 );

		setupHelper.start()
				.withProperties( "/reporting/failurehandler.properties" )
				.setup( IndexedEntity.class );

		assertThat( staticCounters.get( MyFailureHandler.INSTANCES ) ).isEqualTo( 1 );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	static class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		@GeneratedValue
		private Integer id;

		@KeywordField
		private String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
