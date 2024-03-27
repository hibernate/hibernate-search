/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.alternative;

import java.util.EnumMap;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinderDelegate;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeValueBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;

public class LanguageAlternativeBinderDelegate implements AlternativeBinderDelegate<Language, String> {

	private final String name;
	private final Projectable projectable;

	public LanguageAlternativeBinderDelegate(String name, Projectable projectable) {
		this.name = name;
		this.projectable = projectable;
	}

	@Override
	public AlternativeValueBridge<Language, String> bind(IndexSchemaElement indexSchemaElement,
			PojoModelProperty fieldValueSource) {
		EnumMap<Language, IndexFieldReference<String>> fields = new EnumMap<>( Language.class );
		String fieldNamePrefix = ( name != null ? name : fieldValueSource.name() ) + "_";

		for ( Language language : Language.values() ) {
			IndexFieldReference<String> field = indexSchemaElement.field(
					fieldNamePrefix + language.code,
					f -> f.asString().analyzer( "text_" + language.code ).projectable( projectable )
			)
					.toReference();
			fields.put( language, field );
		}

		return new Bridge( fields );
	}

	private static class Bridge implements AlternativeValueBridge<Language, String> {
		private final EnumMap<Language, IndexFieldReference<String>> fields;

		private Bridge(EnumMap<Language, IndexFieldReference<String>> fields) {
			this.fields = fields;
		}

		@Override
		public void write(DocumentElement target, Language discriminator, String bridgedElement) {
			target.addValue( fields.get( discriminator ), bridgedElement );
		}
	}
}
