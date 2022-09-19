/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchHttpClientConfigurerIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	@Rule
	public StaticCounters staticCounters = new StaticCounters();

	@Test
	public void smoke() {
		assertThat( staticCounters.get( HttpClientConfigurer.INSTANCES ) ).isZero();

		setupHelper.start()
				.withProperties( "/configuration/http-client-configurer.properties" )
				.setup( IndexedEntity.class );

		assertThat( staticCounters.get( HttpClientConfigurer.INSTANCES ) ).isEqualTo( 1 );
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
