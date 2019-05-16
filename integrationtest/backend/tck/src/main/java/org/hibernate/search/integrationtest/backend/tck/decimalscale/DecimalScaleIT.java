/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.decimalscale;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.math.BigDecimal;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class DecimalScaleIT {

	private static final String INDEX_NAME = "indexname";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private ScaleIndexMapping scaleIndexMapping;
	private DefaultScaleIndexMapping defaultScaleIndexMapping;
	private BothScaleIndexMapping bothScaleIndexMapping;

	private StubMappingIndexManager indexManager;

	@Test
	public void noDecimalScale() {
		SubTest.expectException( () ->
				setupHelper.withDefaultConfiguration()
						.withIndex( INDEX_NAME, ctx -> ctx.getSchemaElement().field( "noScaled", f -> f.asBigDecimal() ).toReference() )
						.setup()
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Impossible to detect a decimal scale to use for this field." );
	}

	@Test
	public void decimalScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// decimal scale is 3, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.111
		matchAbove( "739.11" );
		doNotMatchAbove( "739.111" );
	}

	@Test
	public void decimalScale_zeroScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// decimal scale is 0, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739
		matchAbove( "738" );
		doNotMatchAbove( "739" );
	}

	@Test
	public void decimalScale_negativeScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), -3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "11111.11111" ) ) );
		workPlan.execute().join();

		// decimal scale is -3, affecting the search precision
		// so the provided value 11111.11111 will be treated as if it were 11000
		matchAbove( "10000" );
		doNotMatchAbove( "11000" );
	}

	@Test
	public void decimalScale_largeScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						// 16 seems the MAX here. A 17-bases similar test would fail for Elasticsearch. Lucene would work.
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 16 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		BigDecimal providedValue = new BigDecimal(
				// 20 decimal ciphers
				"1.11111111111111111111"
		);

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, providedValue ) );
		workPlan.execute().join();

		// decimal scale is 16, affecting the search precision
		// so the provided value (20 decimals) will be treated as if it had 16 decimals
		matchAbove(
				// 15 decimal ciphers
				"1.111111111111111"
		);
		doNotMatchAbove(
				// 16 decimal ciphers
				"1.1111111111111111"
		);
	}

	@Test
	public void decimalScale_negativeScale_largeScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), -78 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		BigDecimal providedValue = new BigDecimal(
				"11111111111111111111111111111111111111111111111111111111111111111111111111111111"
		);

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, providedValue ) );
		workPlan.execute().join();

		// decimal scale is -78, affecting the search precision
		// so the provided value of 80 of 1 digits will be treated as if it had 2 of 1 digits followed by 78 of 0 digits
		matchAbove(
				"10000000000000000000000000000000000000000000000000000000000000000000000000000000"
		);
		doNotMatchAbove(
				"11000000000000000000000000000000000000000000000000000000000000000000000000000000"
		);
	}

	@Test
	public void decimalScale_rounding() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.114999" ) ) );
		workPlan.add( referenceProvider( "2" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.115" ) ) );
		workPlan.add( referenceProvider( "3" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.11" ) ) );
		workPlan.add( referenceProvider( "4" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.12" ) ) );
		workPlan.execute().join();

		// RoundingMode.HALF_UP expected on both values:
		match( "739.11", "1", "3" );
		match( "739.12", "2", "4" );

		// and parameters:
		match( "739.114999", "1", "3" );
		match( "739.115", "2", "4" );
	}

	@Test
	public void decimalScale_largeDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// That seems a limit for ES. Even if new BigDecimal( "2" ).pow( 54 ) << Long.MAX_VALUE
		// If the exponent were 54, the test would fail for Elasticsearch, whereas it would work for Lucene backend.
		BigDecimal largeDecimal = new BigDecimal( "2" ).pow( 53 );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, largeDecimal ) );
		workPlan.execute().join();

		// the precision is supposed to be preserved
		matchAbove( largeDecimal.subtract( BigDecimal.ONE ) );
		doNotMatchAbove( largeDecimal );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE ).multiply( BigDecimal.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, tooLargeDecimal ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is too large to be indexed." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that if it were multiplied by 100, could not be represented as a long
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE ).divide( BigDecimal.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, tooLargeDecimal ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "is too large to be indexed." );
	}

	@Test
	public void defaultDecimalScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.defaultScaleIndexMapping = new DefaultScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( defaultScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// default decimal scale is 2, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.11
		matchAbove( "739.1" );
		doNotMatchAbove( "739.11" );
	}

	@Test
	public void decimalScale_andDefaultDecimalScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.bothScaleIndexMapping = new BothScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( bothScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// default decimal scale is 2
		// decimal scale has been set to 3, overriding the default and affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.111
		matchAbove( "739.11" );
		doNotMatchAbove( "739.111" );
	}

	@Test
	public void decimalScale_doesNotAffectProjections() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement(), 3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// even though decimal scale is 3, projected values wont be affected to
		projection( "739.11111" );
	}

	private void matchAbove(String value) {
		matchAbove( new BigDecimal( value ) );
	}

	public void doNotMatchAbove(String value) {
		doNotMatchAbove( new BigDecimal( value ) );
	}

	private void matchAbove(BigDecimal value) {
		SearchQuery<DocumentReference> query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( value ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, "1" );
	}

	public void doNotMatchAbove(BigDecimal value) {
		SearchQuery<DocumentReference> query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( value ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasNoHits();
	}

	public void projection(String value) {
		SearchQuery<Object> query = indexManager.createSearchScope().query()
				.asProjection( p -> p.field( "scaled" ) )
				.predicate( p -> p.matchAll() )
				.toQuery();
		assertThat( query ).hasHitsExactOrder( new BigDecimal( value ) );
	}

	private void match(String matching, String match1, String match2 ) {
		SearchQuery<DocumentReference> query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.match().onField( "scaled" ).matching( new BigDecimal( matching ) ) )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, match1, match2 );
	}

	private static class ScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		ScaleIndexMapping(IndexSchemaElement root, int decimalScale) {
			scaled = root.field( "scaled", f -> f.asBigDecimal().projectable( Projectable.YES ).decimalScale( decimalScale ) ).toReference();
		}
	}

	private static class DefaultScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		DefaultScaleIndexMapping(IndexedEntityBindingContext ctx) {
			scaled = ctx.getSchemaElement()
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( 2 ) ).asBigDecimal().projectable( Projectable.YES ) ).toReference();
		}
	}

	private static class BothScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		BothScaleIndexMapping(IndexedEntityBindingContext ctx) {
			scaled = ctx.getSchemaElement()
					// setting both default decimal scale
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( 2 ) )
							// and the not-default decimal scale
							.asBigDecimal().decimalScale( 3 ).projectable( Projectable.YES ) ).toReference();
		}
	}
}