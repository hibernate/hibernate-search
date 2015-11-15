/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.source;

/**
 * Builds {@link org.hibernate.search.manualsource.source.EntitySourceContext}
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface EntitySourceContextBuilder {

	/**
	 * Method executed when a WorkLoad is created to define the entity source context
	 * used by the work plan.
	 * Note that this can be overridden by the WorkPlanManager#createWorkPlan(EntitySourceContext)
	 * @return
	 */
	EntitySourceContext buildEntitySourceContextForWorkLoad();
}
