/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;


/**
 * This interface is used to mark the bridges we want to keep safe from the analyzers.
 *
 * @author Guillaume Smet
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * breaking existing users.
 */
public interface IgnoreAnalyzerBridge {

}
