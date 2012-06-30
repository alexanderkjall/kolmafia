package net.java.dev.spellcast.utilities;

import java.util.ListIterator;

public class ListModelIterator<E extends Comparable<? super E>>
        implements ListIterator<E>
{
    private LockableListModel<E> data;

    private int nextIndex, previousIndex;
    private boolean isIncrementing;

    public ListModelIterator(LockableListModel<E> data)
    {
        this( 0, data );
    }

    public ListModelIterator( final int initialIndex, LockableListModel<E> data)
    {
        this.data = data;
        this.nextIndex = 0;
        this.previousIndex = -1;
        this.isIncrementing = true;
    }

    public boolean hasPrevious()
    {
        return this.previousIndex > 0;
    }

    public boolean hasNext()
    {
        return this.nextIndex < data.size();
    }

    public E next()
    {
        this.isIncrementing = true;
        E nextObject = data.get( this.nextIndex );
        ++this.nextIndex;
        ++this.previousIndex;
        return nextObject;
    }

    public E previous()
    {
        this.isIncrementing = false;
        E previousObject = data.get( this.previousIndex );
        --this.nextIndex;
        --this.previousIndex;
        return previousObject;
    }

    public int nextIndex()
    {
        return this.nextIndex;
    }

    public int previousIndex()
    {
        return this.previousIndex;
    }

    public void add( final E o )
    {
        data.add( this.nextIndex, o );
        ++this.nextIndex;
        ++this.previousIndex;
    }

    public void remove()
    {
        if ( this.isIncrementing )
        {
            --this.nextIndex;
            --this.previousIndex;
            data.remove( this.nextIndex );
        }
        else
        {
            ++this.nextIndex;
            ++this.previousIndex;
            data.remove( this.previousIndex );
        }
    }

    public void set( final E o )
    {
        data.set( this.isIncrementing ? this.nextIndex - 1 : this.previousIndex + 1, o );
    }
}

