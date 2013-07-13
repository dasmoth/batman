/*
 * NestedMICA Motif Inference Toolkit
 *
 * Copyright (c) 2004-2007: Genome Research Ltd.
 *
 * NestedMICA is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * or see the on-line version at http://www.gnu.org/copyleft/lgpl.txt
 *
 */

package batman.utils;

import java.util.*;

/**
 * Utilities for working with collections.
 *
 * @author Thomas Down
 */

public class CollectTools {
    public static int[] toIntArray(Collection<? extends Number> l) {
        int[] a = new int[l.size()];
        int i = 0;
        for (Iterator<? extends Number> j = l.iterator(); j.hasNext(); ) {
            a[i++] = j.next().intValue();
        }
        return a;
    }
    
    public static double[] toDoubleArray(Collection<? extends Number> l) {
        double[] a = new double[l.size()];
        int i = 0;
        for (Iterator<? extends Number> j = l.iterator(); j.hasNext(); ) {
            a[i++] = j.next().doubleValue();
        }
        return a;
    }
    
    public static <X extends Object> X randomPick(Collection<X> col) {
        Object[] objs = col.toArray(new Object[col.size()]);
        return (X) objs[(int) Math.floor(Math.random() * objs.length)];
    }
}