import java.util.List;

public class SelectWildcardTest {
	public static void main( String[] args ) {
		// Create test structure
		Value root = Value.create();
		Value a = root.getChildren( "a" ).first();

		// a.b[0] = "ciao"
		a.getChildren( "b" ).get( 0 ).setValue( "ciao" );
		// a.b[1] = "hello"
		a.getChildren( "b" ).get( 1 ).setValue( "hello" );
		// a.c[0] = "ciao"
		a.getChildren( "c" ).first().setValue( "ciao" );

		System.out.println( "Structure:" );
		System.out.println( "  a.b[0] = " + a.getChildren( "b" ).get( 0 ).strValue() );
		System.out.println( "  a.b[1] = " + a.getChildren( "b" ).get( 1 ).strValue() );
		System.out.println( "  a.c[0] = " + a.getChildren( "c" ).first().strValue() );

		// Test 1: $.a.* where . = ciao (should return field names)
		System.out.println( "\n=== Test 1: $.a.* WHERE . = ciao ===" );
		System.out.println( "Expected: [a.b, a.c] - field names where ANY element = ciao" );
		List< String > results1 = new SelectBuilder()
			.select( "a.*" )
			.from( root )
			.where( ". = ciao" )
			.exec();

		System.out.println( "Results:" );
		for( String path : results1 ) {
			System.out.println( "  " + path );
		}

		boolean test1 = results1.size() == 2 &&
			results1.contains( "a.b" ) &&
			results1.contains( "a.c" );
		System.out.println( test1 ? "✓ Test 1 PASSED" : "✗ Test 1 FAILED" );

		// Test 2: $.a.*[*] where . = ciao (should return specific array indices)
		System.out.println( "\n=== Test 2: $.a.*[*] WHERE . = ciao ===" );
		System.out.println( "Expected: [a.b[0], a.c[0]] - specific indices that = ciao" );
		List< String > results2 = new SelectBuilder()
			.select( "a.*[*]" )
			.from( root )
			.where( ". = ciao" )
			.exec();

		System.out.println( "Results:" );
		for( String path : results2 ) {
			System.out.println( "  " + path );
		}

		boolean test2 = results2.size() == 2 &&
			results2.contains( "a.b[0]" ) &&
			results2.contains( "a.c[0]" );
		System.out.println( test2 ? "✓ Test 2 PASSED" : "✗ Test 2 FAILED" );

		// Test 3: $.a.*[*] where . = hello
		System.out.println( "\n=== Test 3: $.a.*[*] WHERE . = hello ===" );
		System.out.println( "Expected: [a.b[1]] - only b[1] = hello" );
		List< String > results3 = new SelectBuilder()
			.select( "a.*[*]" )
			.from( root )
			.where( ". = hello" )
			.exec();

		System.out.println( "Results:" );
		for( String path : results3 ) {
			System.out.println( "  " + path );
		}

		boolean test3 = results3.size() == 1 &&
			results3.contains( "a.b[1]" );
		System.out.println( test3 ? "✓ Test 3 PASSED" : "✗ Test 3 FAILED" );

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
