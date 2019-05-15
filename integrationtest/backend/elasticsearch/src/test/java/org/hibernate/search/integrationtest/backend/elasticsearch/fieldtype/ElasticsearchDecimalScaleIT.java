/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.fieldtype;

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

public class ElasticsearchDecimalScaleIT {

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
						ctx -> this.scaleIndexMapping = new ScaleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( scaleIndexMapping.scaled, BigDecimal.valueOf( 739.11111 ) ) );
		workPlan.execute().join();

		// decimal scale has been set to 3
		// decimal scale affects search precision, since 739.11111 will be treated as 739.111
		SearchQuery<DocumentReference> query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( BigDecimal.valueOf( 739.11 ) ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, "1" );

		query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( BigDecimal.valueOf( 739.111 ) ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasNoHits();
	}

	@Test
	public void defaultDecimalScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.defaultScaleIndexMapping = new DefaultScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( defaultScaleIndexMapping.scaled, BigDecimal.valueOf( 739.11111 ) ) );
		workPlan.execute().join();

		// default decimal scale is 2
		// decimal scale affects search precision, since 739.11111 will be treated as 739.11
		SearchQuery<DocumentReference> query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( BigDecimal.valueOf( 739.1 ) ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, "1" );

		query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( BigDecimal.valueOf( 739.11 ) ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasNoHits();
	}

	@Test
	public void decimalScale_andDefaultDecimalScale() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.bothScaleIndexMapping = new BothScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( bothScaleIndexMapping.scaled, BigDecimal.valueOf( 739.11111 ) ) );
		workPlan.execute().join();

		// default decimal scale is 2
		// decimal scale has been set to 3, overriding the default
		// decimal scale affects search precision, since 739.11111 will be treated as 739.111
		SearchQuery<DocumentReference> query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( BigDecimal.valueOf( 739.11 ) ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasDocRefHitsExactOrder( INDEX_NAME, "1" );

		query = indexManager.createSearchScope()
				.query().asReference()
				.predicate( p -> p.range().onField( "scaled" ).above( BigDecimal.valueOf( 739.111 ) ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasNoHits();
	}

	@Test
	public void decimalScale_doesNotAffectProjections() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.bothScaleIndexMapping = new BothScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( bothScaleIndexMapping.scaled, BigDecimal.valueOf( 739.11111 ) ) );
		workPlan.execute().join();

		// default decimal scale is 2
		// decimal scale has been set to 3, overriding the default
		// decimal scale should not have any effects on stored valued for projections
		SearchQuery<Object> query = indexManager.createSearchScope().query()
				.asProjection( p -> p.field( "scaled" ) )
				.predicate( p -> p.matchAll() )
				.toQuery();
		assertThat( query ).hasHitsExactOrder( BigDecimal.valueOf( 739.11111 ) );
	}

	@Test
	public void decimalScale_doesNotAffectProjections_exceedingZeros() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.bothScaleIndexMapping = new BothScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( bothScaleIndexMapping.scaled, BigDecimal.valueOf( 739.11111000 ) ) );
		workPlan.execute().join();

		// default decimal scale is 2
		// decimal scale has been set to 3, overriding the default
		// decimal scale should not have any effects on stored valued for projections
		// any exceeding decimal value zero will be ignored
		SearchQuery<Object> query = indexManager.createSearchScope().query()
				.asProjection( p -> p.field( "scaled" ) )
				.predicate( p -> p.matchAll() )
				.toQuery();
		assertThat( query ).hasHitsExactOrder( BigDecimal.valueOf( 739.11111 ) );
	}

	private static class ScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		ScaleIndexMapping(IndexSchemaElement root) {
			scaled = root.field( "scaled", f -> f.asBigDecimal().projectable( Projectable.YES ).decimalScale( 3 ) ).toReference();
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