import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class ValueImpl extends Value {
	private volatile Object valueObject = null;
	private final AtomicReference< Map< String, ValueVector > > children = new AtomicReference<>();

	protected ValueImpl() {}

	protected ValueImpl( Object object ) {
		valueObject = object;
	}

	protected ValueImpl( Value val ) {
		valueObject = val.valueObject();
	}

	@Override
	public void setValueObject( Object object ) {
		valueObject = object;
	}

	@Override
	public ValueVector getChildren( String childId ) {
		return children().computeIfAbsent( childId, k -> ValueVector.create() );
	}

	@Override
	public boolean hasChildren() {
		Map< String, ValueVector > c = children.get();
		return (c != null && !c.isEmpty());
	}

	@Override
	public boolean hasChildren( String childId ) {
		Map< String, ValueVector > c = children.get();
		return (c != null && c.containsKey( childId ));
	}

	@Override
	public Map< String, ValueVector > children() {
		// Create the map if not present
		children.getAndUpdate(
			v -> v == null ? new ConcurrentHashMap<>( RootValueImpl.INITIAL_CAPACITY, RootValueImpl.LOAD_FACTOR ) : v );
		return children.get();
	}

	@Override
	public Object valueObject() {
		return valueObject;
	}

	@Override
	public boolean isEqualTo( Value v ) {
		return this.equals( v );
	}
}


class RootValueImpl extends Value {
	protected final static int INITIAL_CAPACITY = 8;
	protected final static float LOAD_FACTOR = 0.75f;

	private final Map< String, ValueVector > children =
		new ConcurrentHashMap<>( INITIAL_CAPACITY, LOAD_FACTOR );

	protected RootValueImpl() {}

	@Override
	public void setValueObject( Object object ) {}

	@Override
	public ValueVector getChildren( String childId ) {
		return children.computeIfAbsent( childId, k -> ValueVector.create() );
	}

	@Override
	public Map< String, ValueVector > children() {
		return children;
	}

	@Override
	public boolean hasChildren() {
		return children.isEmpty() == false;
	}

	@Override
	public boolean hasChildren( String childId ) {
		return children.containsKey( childId );
	}

	@Override
	public Object valueObject() {
		return null;
	}

	@Override
	public boolean isEqualTo( Value v ) {
		return this.equals( v );
	}
}

public abstract class Value {
	public static Value createRootValue() {
		return new RootValueImpl();
	}

	public static Value create() {
		return new ValueImpl();
	}

	public static Value create( Boolean bool ) {
		return new ValueImpl( bool );
	}

	public static Value create( String str ) {
		return new ValueImpl( str );
	}

	public static Value create( Integer i ) {
		return new ValueImpl( i );
	}

	public static Value create( Value value ) {
		return new ValueImpl( value );
	}

	public abstract Map< String, ValueVector > children();

	public abstract Object valueObject();

	protected abstract void setValueObject( Object object );

	public abstract boolean hasChildren();

	public abstract boolean hasChildren( String childId );

	public abstract ValueVector getChildren( String childId );

	public abstract boolean isEqualTo( Value v );

	public final Value getNewChild( String childId ) {
		final ValueVector vec = getChildren( childId );
		Value retVal = new ValueImpl();
		vec.add( retVal );

		return retVal;
	}

	public final Value getFirstChild( String childId ) {
		return getChildren( childId ).get( 0 );
	}

	public final void setValue( Object object ) {
		setValueObject( object );
	}

	public final void setValue( String object ) {
		setValueObject( object );
	}

	public final void setValue( Boolean object ) {
		setValueObject( object );
	}

	public final void setValue( Integer object ) {
		setValueObject( object );
	}

	@Override
	public final synchronized boolean equals( Object obj ) {
		if( !(obj instanceof Value) )
			return false;
		final Value val = (Value) obj;

		boolean r = false;
		if( val.isDefined() ) {
			if( isString() || val.isString() ) {
				r = strValue().equals( val.strValue() );
			} else if( isInt() || val.isInt() ) {
				r = intValue() == val.intValue();
			} else if( isBool() || val.isBool() ) {
				r = boolValue() == val.boolValue();
			} else if( valueObject() != null ) {
				r = valueObject().equals( val.valueObject() );
			}
		} else {
			// undefined == undefined
			r = !isDefined();
		}
		return r;
	}

	@Override
	public int hashCode() {
		return !isDefined() ? super.hashCode()
			: valueObject().hashCode();
	}

	public final boolean isInt() {
		return (valueObject() instanceof Integer);
	}

	public final boolean isBool() {
		return (valueObject() instanceof Boolean);
	}

	public final boolean isString() {
		return (valueObject() instanceof String);
	}

	public final boolean isDefined() {
		return (valueObject() != null);
	}

	public String strValue() {
		try {
			return strValueStrict();
		} catch( TypeCastingException e ) {
			return "";
		}
	}

	public final String strValueStrict()
		throws TypeCastingException {
		Object o = valueObject();
		if( o == null ) {
			throw new TypeCastingException();
		} else if( o instanceof String ) {
			return (String) o;
		}
		return o.toString();
	}

	public int intValue() {
		try {
			return intValueStrict();
		} catch( TypeCastingException e ) {
			return 0;
		}
	}

	public final int intValueStrict()
		throws TypeCastingException {
		int r = 0;
		Object o = valueObject();
		if( o == null ) {
			throw new TypeCastingException();
		} else if( o instanceof Integer ) {
			r = ((Integer) o).intValue();
		} else if( o instanceof Boolean ) {
			r = (((Boolean) o).booleanValue() == true) ? 1 : 0;
		} else if( o instanceof String ) {
			try {
				r = Integer.parseInt( ((String) o).trim() );
			} catch( NumberFormatException nfe ) {
				throw new TypeCastingException();
			}
		}
		return r;
	}

	public boolean boolValue() {
		try {
			return boolValueStrict();
		} catch( TypeCastingException e ) {
			return false;
		}
	}

	public final boolean boolValueStrict()
		throws TypeCastingException {
		boolean r = false;
		Object o = valueObject();
		if( o == null ) {
			throw new TypeCastingException();
		} else if( o instanceof Boolean ) {
			r = ((Boolean) o).booleanValue();
		} else if( o instanceof Number ) {
			if( ((Number) o).intValue() > 0 ) {
				r = true;
			}
		} else if( o instanceof String ) {
			r = Boolean.parseBoolean( ((String) o).trim() );
		}

		return r;
	}
}
