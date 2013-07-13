package batman.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Collects {
	private Collects() {}
	
	public static <K,V> void pushOntoMap(Map<K,List<V>> m, K k, V v)
	{
		List<V> l = m.get(k);
		if (l == null) {
			l = new ArrayList<V>();
			m.put(k, l);
		}
		l.add(v);
	}
	
	public static <K,V> void pushNewOntoMap(Map<K,List<V>> m, K k, V v)
	{
		List<V> l = m.get(k);
		if (l == null) {
			l = new ArrayList<V>();
			m.put(k, l);
		}
		if (!l.contains(v)) {
			l.add(v);
		}
	}
	
}
