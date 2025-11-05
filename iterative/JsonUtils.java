import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 * Utilities for parsing JSON into Jolie Value structures.
 * Extracted from jolie.js.JsUtils
 */
public class JsonUtils {
	/**
	 * Jolie introduces a "$" named attribute for JSON objects with a root value.
	 */
	private static final String ROOT_SIGN = "$";

	/**
	 * Multi-dimensional arrays in JSON become nested arrays with "_" key.
	 */
	public static final String JSONARRAY_KEY = "_";

	/**
	 * Parse JSON from a Reader into a Jolie Value.
	 *
	 * @param reader the Reader containing JSON data
	 * @param value the Value to populate with parsed data
	 * @param strictEncoding if true, JSON arrays become nested with "_" key; if false, arrays map directly
	 * @throws IOException if parsing fails
	 */
	public static void parseJsonIntoValue( Reader reader, Value value, boolean strictEncoding )
		throws IOException {
		try {
			Object obj = JSONValue.parseWithException( reader );
			if( obj instanceof JSONArray ) {
				value.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
			} else if( obj instanceof JSONObject ) {
				jsonObjectToValue( (JSONObject) obj, value, strictEncoding );
			} else {
				objectToBasicValue( obj, value );
			}
		} catch( ParseException | ClassCastException e ) {
			throw new IOException( e );
		}
	}

	private static void jsonObjectToValue( JSONObject obj, Value value, boolean strictEncoding ) {
		@SuppressWarnings( "unchecked" )
		Map< String, Object > map = (Map< String, Object >) obj;
		ValueVector vec;
		for( Map.Entry< String, Object > entry : map.entrySet() ) {
			if( entry.getKey().equals( ROOT_SIGN ) ) {
				objectToBasicValue( entry.getValue(), value );
			} else {
				vec = jsonObjectToValueVector( entry.getValue(), strictEncoding );
				value.children().put( entry.getKey(), vec );
			}
		}
	}

	private static ValueVector jsonObjectToValueVector( Object obj, boolean strictEncoding ) {
		ValueVector vec = ValueVector.create();
		if( obj instanceof JSONObject ) {
			Value val = Value.create();
			jsonObjectToValue( (JSONObject) obj, val, strictEncoding );
			vec.add( val );
		} else if( obj instanceof JSONArray && strictEncoding ) {
			Value arrayValue = Value.create();
			vec.add( arrayValue );
			arrayValue.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) obj, strictEncoding ) );
		} else if( obj instanceof JSONArray && !strictEncoding ) {
			vec = jsonArrayToValueVector( (JSONArray) obj, strictEncoding );
		} else {
			Value val = Value.create();
			objectToBasicValue( obj, val );
			vec.add( val );
		}
		return vec;
	}

	private static ValueVector jsonArrayToValueVector( JSONArray array, boolean strictEncoding ) {
		ValueVector vec = ValueVector.create();
		for( Object element : array ) {
			Value value = Value.create();
			if( element instanceof JSONArray ) {
				value.children().put( JSONARRAY_KEY, jsonArrayToValueVector( (JSONArray) element, strictEncoding ) );
			} else if( element instanceof JSONObject ) {
				jsonObjectToValue( (JSONObject) element, value, strictEncoding );
			} else {
				objectToBasicValue( element, value );
			}
			vec.add( value );
		}
		return vec;
	}

	private static void objectToBasicValue( Object obj, Value val ) {
		if( obj instanceof String ) {
			val.setValue( (String) obj );
		} else if( obj instanceof Double ) {
			val.setValue( (Double) obj );
		} else if( obj instanceof Long ) {
			long lval = (Long) obj;
			if( lval > Integer.MAX_VALUE || lval < Integer.MIN_VALUE ) {
				val.setValue( lval );
			} else {
				val.setValue( (int) lval );
			}
		} else if( obj instanceof Boolean ) {
			val.setValue( (Boolean) obj );
		} else if( obj != null ) {
			val.setValue( obj.toString() );
		}
	}
}
