/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.alternative.alternativebinder;

import java.util.EnumMap;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinderDelegate;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeValueBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;

//tag::include[]
public class LanguageAlternativeBinderDelegate implements AlternativeBinderDelegate<Language, String> { // <1>

	private final String name;

	public LanguageAlternativeBinderDelegate(String name) { // <2>
		this.name = name;
	}

	@Override
	public AlternativeValueBridge<Language, String> bind(IndexSchemaElement indexSchemaElement, // <3>
			PojoModelProperty fieldValueSource) {
		EnumMap<Language, IndexFieldReference<String>> fields = new EnumMap<>( Language.class );
		String fieldNamePrefix = ( name != null ? name : fieldValueSource.name() ) + "_";

		for ( Language language : Language.values() ) { // <4>
			String languageCode = language.code;
			IndexFieldReference<String> field = indexSchemaElement.field(
					fieldNamePrefix + languageCode, // <5>
					f -> f.asString().analyzer( "text_" + languageCode ) // <6>
			)
					.toReference();
			fields.put( language, field );
		}

		return new Bridge( fields ); // <7>
	}

	private static class Bridge // <8>
			implements AlternativeValueBridge<Language, String> { // <9>
		private final EnumMap<Language, IndexFieldReference<String>> fields;

		private Bridge(EnumMap<Language, IndexFieldReference<String>> fields) {
			this.fields = fields;
		}

		@Override
		public void write(DocumentElement target, Language discriminator, String bridgedElement) {
			target.addValue( fields.get( discriminator ), bridgedElement ); // <10>
		}
	}
}
//end::include[]
