import java.util.List;

public class SelectRelativePathTest {
	public static void main( String[] args ) {
		// Create structure: a.b.c = 5

		Value a = Value.create();
		a.getChildren( "b" ).first()
			.getChildren( "c" ).first().setValue( 5 );

		System.out.println( "Structure: a.b.c = 5\n" );

		// Test: SELECT $.b.c FROM a WHERE . = 5
		System.out.println( "Test: SELECT $.b.c FROM a WHERE . = 5" );
		List<String> results = new SelectBuilder()
			.select( "$.b.c" )
			.from( a, "a" )
			.where( ". = 5" )
			.exec();

		System.out.println( "Results: " + results );
		System.out.println( "Expected: [a.b.c]" );

		if( results.size() == 1 && results.contains( "a.b.c" ) ) {
			System.out.println( "✓ TEST PASSED" );
		} else {
			System.out.println( "✗ TEST FAILED" );
			System.out.println( "Got: " + results );
			System.exit( 1 );
		}
	}
}
