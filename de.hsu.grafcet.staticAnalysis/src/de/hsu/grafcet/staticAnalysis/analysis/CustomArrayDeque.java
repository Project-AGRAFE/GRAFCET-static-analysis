package de.hsu.grafcet.staticAnalysis.analysis;

import java.util.ArrayDeque;
import java.util.Collection;

public class CustomArrayDeque<E> extends ArrayDeque<E> {
	
	/*
	 * Checks if item is already in the collection 
	 */
	@Override
	public boolean add(E e) {
		if (!this.contains(e)) {
			return super.add(e);
		}
		return false;
	}
	
	/*
	 * Checks if item is already in the collection 
	 */
	@Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }
	
	
}
