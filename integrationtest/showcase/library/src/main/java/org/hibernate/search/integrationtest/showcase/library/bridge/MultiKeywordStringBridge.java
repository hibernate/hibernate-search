/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import java.util.regex.Pattern;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class MultiKeywordStringBridge implements PropertyBridge<String> {

	public static final String SEPARATOR_PATTERN_DEFAULT = ",";

	private final Pattern separatorPattern;

	private final IndexFieldReference<String> valueFieldReference;

	private MultiKeywordStringBridge(String fieldName, Pattern separatorPattern,
			IndexSchemaElement indexSchemaElement) {
		this.separatorPattern = separatorPattern;
		this.valueFieldReference = indexSchemaElement.field(
				fieldName, f -> f.asString()
		)
				.multiValued()
				.toReference();
	}

	@Override
	public void write(DocumentElement target, String sourceValue, PropertyBridgeWriteContext context) {
		if ( sourceValue != null ) {
			String[] items = separatorPattern.split( sourceValue );
			for ( String item : items ) {
				target.addValue( valueFieldReference, item );
			}
		}
	}

	public static class Binder implements PropertyBinder {
		private String fieldName;
		private Pattern separatorPattern = Pattern.compile( SEPARATOR_PATTERN_DEFAULT );

		public Binder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		public Binder separatorPattern(Pattern separatorPattern) {
			this.separatorPattern = separatorPattern;
			return this;
		}

		@Override
		public void bind(PropertyBindingContext context) {
			if ( fieldName == null || fieldName.isEmpty() ) {
				throw new IllegalArgumentException( "fieldName is a mandatory parameter" );
			}
			context.dependencies().useRootOnly();

			context.bridge( String.class,
					new MultiKeywordStringBridge(
							fieldName,
							separatorPattern,
							context.indexSchemaElement()
					)
			);
		}
	}

}
