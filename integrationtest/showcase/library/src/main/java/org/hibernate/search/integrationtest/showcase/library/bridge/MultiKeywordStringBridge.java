/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import java.util.regex.Pattern;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class MultiKeywordStringBridge implements PropertyBridge {

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
	public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
		String sourceValue = (String) bridgedElement;
		if ( sourceValue != null ) {
			String[] items = separatorPattern.split( sourceValue );
			for ( String item : items ) {
				target.addValue( valueFieldReference, item );
			}
		}
	}

	public static class Builder implements PropertyBridgeBuilder<org.hibernate.search.integrationtest.showcase.library.bridge.annotation.MultiKeywordStringBridge> {
		private String fieldName;
		private Pattern separatorPattern = Pattern.compile( SEPARATOR_PATTERN_DEFAULT );

		@Override
		public void initialize(org.hibernate.search.integrationtest.showcase.library.bridge.annotation.MultiKeywordStringBridge annotation) {
			fieldName( annotation.fieldName() );
			separatorPattern( Pattern.compile( annotation.separatorPattern() ) );
		}

		public Builder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		public Builder separatorPattern(Pattern separatorPattern) {
			this.separatorPattern = separatorPattern;
			return this;
		}

		@Override
		public void bind(PropertyBindingContext context) {
			if ( fieldName == null || fieldName.isEmpty() ) {
				throw new IllegalArgumentException( "fieldName is a mandatory parameter" );
			}
			context.getDependencies().useRootOnly();

			context.setBridge(
					new MultiKeywordStringBridge(
							fieldName,
							separatorPattern,
							context.getIndexSchemaElement()
					)
			);
		}
	}

}
