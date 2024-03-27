/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONParser;

class JsonPropertyIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		// Oracle: Caused by: java.sql.SQLException: ORA-43853: JSON type cannot be used in non-automatic segment space management tablespace "SYSTEM"
		assumeFalse(
				DatabaseContainer.configuration().driver().toLowerCase( Locale.ROOT ).contains( "oracle" ),
				"This test will not work on Oracle DB in our current configuration."
		);

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "json", b2 -> b2
						.field( "content", String.class, b3 -> b3.analyzerName( AnalyzerNames.DEFAULT ) )
						.field( "keyword", String.class )
						.field( "numbers", Integer.class, b3 -> b3.multiValued( true ) )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.type.json_format_mapper", new FormatMapper() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
						if ( !javaType.getJavaTypeClass().isAssignableFrom( JsonThing.class ) ) {
							throw new IllegalArgumentException();
						}
						if ( charSequence == null ) {
							return null;
						}
						try {
							JsonThing jsonThing = new JsonThing();
							JSONObject object = (JSONObject) JSONParser.parseJSON( charSequence.toString() );

							jsonThing.setContent( object.getString( "content" ) );
							jsonThing.setKeyword( object.getString( "keyword" ) );
							jsonThing.setNumbers( Arrays.stream( object.getJSONArray( "numbers" ).join( "," ).split( "," ) )
									.map( Integer::parseInt )
									.collect( Collectors.toList() ) );
							return (T) jsonThing;
						}
						catch (JSONException e) {
							throw new RuntimeException( e );
						}
					}

					@Override
					public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
						if ( !javaType.getJavaTypeClass().isAssignableFrom( JsonThing.class ) ) {
							throw new IllegalArgumentException();
						}
						return String.format( Locale.ROOT, "{" +
								"  \"content\": \"%2$s\"," +
								"  \"keyword\": \"%1$s\"," +
								"  \"numbers\": %3$s" +
								"}",
								( (JsonThing) value ).keyword,
								( (JsonThing) value ).content,
								( (JsonThing) value ).numbers
						);
					}
				} )
				.setup(
						IndexedEntity.class,
						JsonThing.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			JsonThing json = new JsonThing();
			json.setKeyword( "keyword" );
			json.setNumbers( Arrays.asList( 1, 2, 3 ) );
			json.setContent( "some text content" );
			entity1.setJson( json );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "json", b2 -> b2
									.field( "content", json.getContent() )
									.field( "keyword", json.getKeyword() )
									.field( "numbers", json.getNumbers().get( 0 ) )
									.field( "numbers", json.getNumbers().get( 1 ) )
									.field( "numbers", json.getNumbers().get( 2 ) )
							)
					);
		} );
		with( sessionFactory ).runInTransaction( session -> {
			// not if json does not have equals -- will result in an index operation!
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			assertThat( entity1.getJson().getKeyword() ).isEqualTo( "keyword" );
		} );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			JsonThing json = entity1.getJson();
			json.setKeyword( "updatedKeyword" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "json", b2 -> b2
									.field( "content", json.getContent() )
									.field( "keyword", json.getKeyword() )
									.field( "numbers", json.getNumbers().get( 0 ) )
									.field( "numbers", json.getNumbers().get( 1 ) )
									.field( "numbers", json.getNumbers().get( 2 ) )
							)
					);
		} );
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@IndexedEmbedded
		@JdbcTypeCode(SqlTypes.JSON)
		private JsonThing json;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public JsonThing getJson() {
			return json;
		}

		public void setJson(JsonThing json) {
			this.json = json;
		}
	}

	public static class JsonThing {
		@KeywordField
		private String keyword;

		@FullTextField
		private String content;

		@GenericField
		private List<Integer> numbers = new ArrayList<>();

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public List<Integer> getNumbers() {
			return numbers;
		}

		public void setNumbers(List<Integer> numbers) {
			this.numbers = numbers;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( object == null || getClass() != object.getClass() ) {
				return false;
			}
			JsonThing jsonThing = (JsonThing) object;
			return Objects.equals( keyword, jsonThing.keyword )
					&& Objects.equals(
							content, jsonThing.content )
					&& Objects.equals( numbers, jsonThing.numbers );
		}

		@Override
		public int hashCode() {
			return Objects.hash( keyword, content, numbers );
		}
	}

}
