import java.util.List;

public class SelectArrayTest {
	public static void main( String[] args ) {
		// Create Jolie tree structure with arrays
		Value root = Value.create();
		Value apiData = root.getChildren( "apiData" ).first();

		// apiData.operations[0]
		Value op0 = apiData.getChildren( "operations" ).get( 0 );
		op0.getChildren( "path" ).first().setValue( "/users" );
		op0.getChildren( "method" ).first().setValue( "get" );
		op0.getChildren( "tags" ).get( 0 ).setValue( "users" );
		op0.getChildren( "tags" ).get( 1 ).setValue( "admin" );
		op0.getChildren( "requiresAuth" ).first().setValue( true );

		// apiData.operations[1]
		Value op1 = apiData.getChildren( "operations" ).get( 1 );
		op1.getChildren( "path" ).first().setValue( "/public/posts" );
		op1.getChildren( "method" ).first().setValue( "get" );
		op1.getChildren( "tags" ).get( 0 ).setValue( "posts" );
		op1.getChildren( "tags" ).get( 1 ).setValue( "public" );
		op1.getChildren( "requiresAuth" ).first().setValue( false );

		// apiData.operations[2]
		Value op2 = apiData.getChildren( "operations" ).get( 2 );
		op2.getChildren( "path" ).first().setValue( "/admin/secrets" );
		op2.getChildren( "method" ).first().setValue( "post" );
		op2.getChildren( "tags" ).get( 0 ).setValue( "admin" );
		op2.getChildren( "tags" ).get( 1 ).setValue( "sensitive" );
		op2.getChildren( "requiresAuth" ).first().setValue( true );

		// apiData.operations[3]
		Value op3 = apiData.getChildren( "operations" ).get( 3 );
		op3.getChildren( "path" ).first().setValue( "/admin/logs" );
		op3.getChildren( "method" ).first().setValue( "get" );
		op3.getChildren( "tags" ).get( 0 ).setValue( "admin" );
		op3.getChildren( "tags" ).get( 1 ).setValue( "monitoring" );
		op3.getChildren( "requiresAuth" ).first().setValue( true );

		// Verify structure
		System.out.println( "Operations created: " + apiData.getChildren( "operations" ).size() );
		System.out.println( "op[0].path = " + op0.getChildren( "path" ).first().strValue() );
		System.out.println( "op[0].tags[1] = " + op0.getChildren( "tags" ).get( 1 ).strValue() );

		// Test 1: Select all operations with method="get"
		System.out.println( "\n=== Test 1: SELECT $.apiData.operations[*] WHERE .method = get ===" );
		List< String > results1 = new SelectBuilder()
			.select( "apiData.operations[*]" )
			.from( root )
			.where( ".method = get" )
			.exec();

		System.out.println( "Results:" );
		for( String path : results1 ) {
			System.out.println( "  " + path );
		}

		// Expected: operations[0], operations[1], operations[3]
		boolean test1 = results1.size() == 3;
		if( test1 ) {
			System.out.println( "✓ Test 1 PASSED" );
		} else {
			System.out.println( "✗ Test 1 FAILED: Expected 3 results, got " + results1.size() );
		}

		// Test 2: Select operations with requiresAuth=true
		System.out.println( "\n=== Test 2: SELECT $.apiData.operations[*] WHERE .requiresAuth = true ===" );
		List< String > results2 = new SelectBuilder()
			.select( "apiData.operations[*]" )
			.from( root )
			.where( ".requiresAuth = true" )
			.exec();

		System.out.println( "Results:" );
		for( String path : results2 ) {
			System.out.println( "  " + path );
		}

		// Expected: operations[0], operations[2], operations[3]
		boolean test2 = results2.size() == 3;
		if( test2 ) {
			System.out.println( "✓ Test 2 PASSED" );
		} else {
			System.out.println( "✗ Test 2 FAILED: Expected 3 results, got " + results2.size() );
		}

		// Test 3: Select operations with descendant tags="admin"
		System.out.println( "\n=== Test 3: SELECT $.apiData.operations[*] WHERE ..tags = admin ===" );
		List< String > results3 = new SelectBuilder()
			.select( "apiData.operations[*]" )
			.from( root )
			.where( "..tags = admin" )
			.exec();

		System.out.println( "Results:" );
		for( String path : results3 ) {
			System.out.println( "  " + path );
		}

		// Expected: operations[0], operations[2], operations[3]
		boolean test3 = results3.size() == 3;
		if( test3 ) {
			System.out.println( "✓ Test 3 PASSED" );
		} else {
			System.out.println( "✗ Test 3 FAILED: Expected 3 results, got " + results3.size() );
		}

		// Exit with appropriate code
		if( test1 && test2 && test3 ) {
			System.out.println( "\n✓ ALL TESTS PASSED" );
			System.exit( 0 );
		} else {
			System.out.println( "\n✗ SOME TESTS FAILED" );
			System.exit( 1 );
		}
	}
}
