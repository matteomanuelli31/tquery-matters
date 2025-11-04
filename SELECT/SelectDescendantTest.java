import java.util.List;

public class SelectDescendantTest {
	public static void main( String[] args ) {
		// Create structure:
		// root.x.value = 10
		// root.y.z.value = 10

		Value root = Value.create();
		root.getChildren( "x" ).first()
			.getChildren( "value" ).first().setValue( 10 );
		root.getChildren( "y" ).first()
			.getChildren( "z" ).first()
			.getChildren( "value" ).first().setValue( 10 );

		System.out.println( "Structure: root.x.value=10, root.y.z.value=10\n" );

		// Test: SELECT $..value FROM root WHERE . = 10
		System.out.println( "Test: SELECT $..value FROM root WHERE . = 10" );
		List<String> results = new SelectBuilder()
			.select( "$..value" )
			.from( root, "root" )
			.where( ". = 10" )
			.exec();

		System.out.println( "Results: " + results );
		System.out.println( "Expected: [root.x.value, root.y.z.value]" );

		if( results.size() == 2 &&
		    results.contains( "root.x.value" ) &&
		    results.contains( "root.y.z.value" ) ) {
			System.out.println( "✓ TEST PASSED" );
		} else {
			System.out.println( "✗ TEST FAILED" );
			System.out.println( "Got: " + results );
			System.exit( 1 );
		}
	}
}
