/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that users attempting to use unsupported features are duly warned.
 *
 * @author Yoann Rodiere
 */
public class ElasticsearchUnsupportedFeaturesIT extends SearchInitializationTestBase {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void dynamicBoosting() throws Exception {
		logged.expectMessage( "HSEARCH400032", "@DynamicBoost", DynamicBoostingEntity.class.getSimpleName() );

		init( DynamicBoostingEntity.class );
	}

	@Indexed
	@Entity
	@DynamicBoost(impl = CustomBoostStrategy.class)
	public static class DynamicBoostingEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private Float boost;

		@Field
		private String data;

		public Integer getId() {
			return id;
		}

		public Float getBoost() {
			return boost;
		}

		public void setBoost(Float boost) {
			this.boost = boost;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

	}

	public static class CustomBoostStrategy implements BoostStrategy {
		@Override
		public float defineBoost(Object value) {
			DynamicBoostingEntity entity = (DynamicBoostingEntity) value;
			return entity.getBoost();
		}
	}

}
