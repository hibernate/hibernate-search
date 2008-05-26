package org.hibernate.search.test.configuration;

import java.util.Properties;

import org.hibernate.search.backend.configuration.MaskedProperty;

/**
 * @author Sanne Grinovero
 */
public class MaskedPropertiesTest extends junit.framework.TestCase {

	public void testConfigurationParsingPrecedence() {
		Properties cfg = new Properties();
		cfg.put( "hibernate.search.Animals.transaction.indexwriter.max_merge_docs", "1" );
		cfg.put( "hibernate.search.Animals.2.transaction.indexwriter.max_merge_docs", "2" );
		cfg.put( "hibernate.search.Animals.2.transaction.max_merge_docs", "3" );
		cfg.put( "hibernate.search.Animals.transaction.max_merge_docs", "5" );
		cfg.put( "hibernate.search.default.transaction.max_merge_docs", "6" );
		cfg.put( "hibernate.search.default.transaction.indexwriter.max_field_length", "7" );

		//this is more a "concept demo" than a test:
		Properties root = new MaskedProperty( cfg, "hibernate.search" );
		//only keys starting as "hibernate.search.default" are exposed:
		Properties common = new MaskedProperty( root, "default" );
		//now as "hibernate.search.Animals" or "hibernate.search.default" if first fails:
		Properties dirProvider = new MaskedProperty( root, "Animals", common );
		//this narrows visibility to "hibernate.search.<providername|default>.transaction":
		Properties transaction = new MaskedProperty( dirProvider, "transaction" );
		Properties shard2 = new MaskedProperty( dirProvider, "2", dirProvider );
		Properties transactionInShard2 = new MaskedProperty( shard2, "transaction", transaction );
		Properties newStyleTransaction = new MaskedProperty( transaction, "indexwriter", transaction );
		Properties newStyleTransactionInShard2 = new MaskedProperty(
				transactionInShard2, "indexwriter", transactionInShard2 );
		
		assertEquals( "7" , newStyleTransaction.getProperty( "max_field_length" ) );
		assertEquals( "7" , newStyleTransactionInShard2.getProperty( "max_field_length" ) );
		assertEquals( "5" , transaction.getProperty( "max_merge_docs" ) );
	}
	
}
