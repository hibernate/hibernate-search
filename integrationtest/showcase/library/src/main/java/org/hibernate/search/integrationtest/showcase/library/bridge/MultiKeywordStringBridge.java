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
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class MultiKeywordStringBridge implements PropertyBridge {

	public static final String SEPARATOR_PATTERN_DEFAULT = ",";

	public static class Builder implements AnnotationBridgeBuilder<PropertyBridge, org.hibernate.search.integrationtest.showcase.library.bridge.annotation.MultiKeywordStringBridge> {
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
		public BeanHolder<PropertyBridge> build(BridgeBuildContext buildContext) {
			if ( fieldName == null || fieldName.isEmpty() ) {
				throw new IllegalArgumentException( "fieldName is a mandatory parameter" );
			}
			return BeanHolder.of( new MultiKeywordStringBridge( this ) );
		}
	}

	private final String fieldName;
	private final Pattern separatorPattern;

	private IndexFieldReference<String> valueFieldReference;

	private MultiKeywordStringBridge(Builder builder) {
		this.fieldName = builder.fieldName;
		this.separatorPattern = builder.separatorPattern;
	}

	@Override
	public void bind(PropertyBridgeBindingContext context) {
		context.getDependencies().useRootOnly();
		valueFieldReference = context.getIndexSchemaElement().field(
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

}
