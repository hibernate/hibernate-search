/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.bridge.mapping;

import java.lang.annotation.Annotation;

/**
 * @author Yoann Rodiere
 */
public interface BridgeDefinition<A extends Annotation> {

	/*
	 * FIXME bridge definitions
 	 * TODO add support for annotation parameters (?)
 	 * TODO add support for multi-valued parameters (?)
	 * see https://github.com/hibernate/hibernate-validator/blob/master/engine/src/main/java/org/hibernate/validator/cfg/AnnotationDef.java
	 * see https://github.com/hibernate/hibernate-validator/blob/master/engine/src/main/java/org/hibernate/validator/cfg/ConstraintDef.java
	 * see NotNullDef in https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/?v=5.4#section-programmatic-api
	 */

	A get();

}
