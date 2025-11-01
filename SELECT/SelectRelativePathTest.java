import java.util.List;

public class SelectRelativePathTest {
	public static void main( String[] args ) {
		// Create structure: a.b.c.d.e = 5
		Value root = Value.create();
		Value a = root.getChildren( "a" ).first();
		Value b = a.getChildren( "b" ).first();
		Value c = b.getChildren( "c" ).first();
		Value d = c.getChildren( "d" ).first();
		d.getChildren( "e" ).first().setValue( 5 );

		System.out.println( "Structure: a.b.c.d.e = 5\n" );

		// Test 1: Select from root
		System.out.println( "Test 1: SELECT a.b.* FROM root WHERE ..e = 5" );
		List< String > results1 = new SelectBuilder()
			.select( "a.b.*" )
			.from( root )
			.where( "..e = 5" )
			.exec();
		System.out.println( "Result: " + results1 );
		boolean test1 = results1.size() == 1 && results1.contains( "a.b.c" );
		System.out.println( test1 ? "✓ PASSED" : "✗ FAILED" );

		// Test 2: Select from sub-node (relative)
		System.out.println( "\nTest 2: SELECT b.* FROM node-a WHERE ..e = 5" );
		List< String > results2 = new SelectBuilder()
			.select( "b.*" )
			.from( a )
			.where( "..e = 5" )
			.exec();
		System.out.println( "Result: " + results2 );
		System.out.println( "Expected: [b.c]" );
		boolean test2 = results2.size() == 1 && results2.contains( "b.c" );
		System.out.println( test2 ? "✓ PASSED" : "✗ FAILED" );

		if( test1 && test2 ) {
			System.out.println( "\n✓ ALL TESTS PASSED" );
			System.exit( 0 );
		} else {
			System.out.println( "\n✗ SOME TESTS FAILED" );
			System.exit( 1 );
		}
	}
}
