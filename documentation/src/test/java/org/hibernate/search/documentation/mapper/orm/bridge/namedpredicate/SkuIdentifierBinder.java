/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.namedpredicate;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

//tag::binder[]
/**
 * A binder for Stock Keeping Unit (SKU) identifiers, i.e. Strings with a specific format.
 */
public class SkuIdentifierBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies().useRootOnly();

		IndexSchemaObjectField skuIdObjectField = context.indexSchemaElement()
				.objectField( context.bridgedElement().name() );

		IndexFieldType<String> skuIdPartType = context.typeFactory()
				.asString().normalizer( "lowercase" ).toIndexFieldType();

		context.bridge( String.class, new Bridge(
				skuIdObjectField.toReference(),
				skuIdObjectField.field( "departmentCode", skuIdPartType ).toReference(),
				skuIdObjectField.field( "collectionCode", skuIdPartType ).toReference(),
				skuIdObjectField.field( "itemCode", skuIdPartType ).toReference()
		) );

		skuIdObjectField.namedPredicate( // <1>
				"skuIdMatch", // <2>
				new SkuIdentifierMatchPredicateDefinition() // <3>
		);
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class SkuIdentifierBinder (continued)

	private static class Bridge implements PropertyBridge<String> { // <1>

		private final IndexObjectFieldReference skuIdObjectField;
		private final IndexFieldReference<String> departmentCodeField;
		private final IndexFieldReference<String> collectionCodeField;
		private final IndexFieldReference<String> itemCodeField;

		private Bridge(IndexObjectFieldReference skuIdObjectField,
				IndexFieldReference<String> departmentCodeField,
				IndexFieldReference<String> collectionCodeField,
				IndexFieldReference<String> itemCodeField) {
			this.skuIdObjectField = skuIdObjectField;
			this.departmentCodeField = departmentCodeField;
			this.collectionCodeField = collectionCodeField;
			this.itemCodeField = itemCodeField;
		}

		@Override
		public void write(DocumentElement target, String skuId, PropertyBridgeWriteContext context) {
			DocumentElement skuIdObject = target.addObject( this.skuIdObjectField );// <2>

			// An SKU identifier is formatted this way: "<department code>.<collection code>.<item code>".
			String[] skuIdParts = skuId.split( "\\." );
			skuIdObject.addValue( this.departmentCodeField, skuIdParts[0] ); // <3>
			skuIdObject.addValue( this.collectionCodeField, skuIdParts[1] ); // <3>
			skuIdObject.addValue( this.itemCodeField, skuIdParts[2] ); // <3>
		}
	}

	// ... class continues below
	//end::bridge[]
	//tag::predicate-definition[]
	// ... class SkuIdentifierBinder (continued)

	private static class SkuIdentifierMatchPredicateDefinition implements PredicateDefinition { // <1>
		@Override
		public SearchPredicate create(PredicateDefinitionContext context) {
			SearchPredicateFactory f = context.predicate(); // <2>

			String pattern = (String) context.param( "pattern" ); // <3>

			return f.and().with( and -> { // <4>
				// An SKU identifier pattern is formatted this way: "<department code>.<collection code>.<item code>".
				// Each part supports * and ? wildcards.
				String[] patternParts = pattern.split( "\\." );
				if ( patternParts.length > 0 ) {
					and.add( f.wildcard()
							.field( "departmentCode" ) // <5>
							.matching( patternParts[0] ) );
				}
				if ( patternParts.length > 1 ) {
					and.add( f.wildcard()
							.field( "collectionCode" )
							.matching( patternParts[1] ) );
				}
				if ( patternParts.length > 2 ) {
					and.add( f.wildcard()
							.field( "itemCode" )
							.matching( patternParts[2] ) );
				}
			} ).toPredicate(); // <6>
		}
	}
}
//end::predicate-definition[]
