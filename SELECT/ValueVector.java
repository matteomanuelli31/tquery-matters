import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ValueVectorImpl extends ValueVector {
	private final ArrayList< Value > values;

	@Override
	protected List< Value > values() {
		return values;
	}

	@Override
	public synchronized int size() {
		return values().size();
	}

	@Override
	public Value get( int i ) {
		if( i >= values.size() ) {
			synchronized( this ) {
				if( i >= values.size() ) {
					values.ensureCapacity( i + 1 );
					for( int k = values.size(); k <= i; k++ ) {
						values.add( Value.create() );
					}
				}
			}
		}
		return values.get( i );
	}

	@Override
	public synchronized void set( int i, Value value ) {
		if( i >= values.size() ) {
			values.ensureCapacity( i + 1 );
			for( int k = values.size(); k < i; k++ ) {
				values.add( Value.create() );
			}
			values.add( value );
		} else {
			values.set( i, value );
		}
	}

	public ValueVectorImpl() {
		values = new ArrayList<>( 1 );
	}
}


public abstract class ValueVector implements Iterable< Value > {
	public static ValueVector create() {
		return new ValueVectorImpl();
	}

	public synchronized Value first() {
		return get( 0 );
	}

	public synchronized boolean isEmpty() {
		return values().isEmpty();
	}

	@Override
	public synchronized Iterator< Value > iterator() {
		return values().iterator();
	}

	public abstract Value get( int i );

	public abstract void set( int i, Value value );

	public abstract int size();

	public synchronized void add( Value value ) {
		values().add( value );
	}

	public synchronized void add( int index, Value value ) {
		values().add( index, value );
	}

	protected abstract List< Value > values();
}
