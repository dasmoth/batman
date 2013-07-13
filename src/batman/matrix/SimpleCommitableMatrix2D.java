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

package batman.matrix;

/**
 * Matrix object which supports commit and rollback operations.
 * 
 * <p>
 * <strong>Not threadsafe</strong>
 * </p>
 *
 * @author Thomas Down
 */

public class SimpleCommitableMatrix2D implements CommitableMatrix2D {
    private static final int EDIT_LIST_SIZE = 5;
    
    private final int rows;
    private final int columns;
    private double[] background;
    private double[] foreground;
    
    private int editCount;
    private int[] editIndex;
    
    public SimpleCommitableMatrix2D(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        int cells = rows * columns;
        this.background = new double[cells];
        this.foreground = new double[cells];
        editCount = 0;
        editIndex = new int[EDIT_LIST_SIZE];
    }
    
    public SimpleCommitableMatrix2D(Matrix2D m) {
        this(m.rows(), m.columns());
        for (int i = 0; i < m.rows(); ++i) {
            for (int j = 0; j < m.columns(); ++j) {
                set(i, j, m.get(i, j));
            }
        }
        commit();
    }
    
    public SimpleCommitableMatrix2D(int rows, int columns, double v) {
        this(rows, columns);
        for (int i = 0; i < rows * columns; ++i) {
            background[i] = foreground[i] = v;
        }
    }
    
    public int rows() {
        return rows;
    }
    
    public int columns() {
        return columns;
    }
    
    public double get(int row, int col) {
        return foreground[row * columns + col];
    }
    
    public double getCommitted(int row, int col) {
        return background[row * columns + col];
    }
    
    public synchronized void set(int row, int col, double d) {
        int cell = row * columns + col;
        foreground[cell] = d;
        if (editCount < EDIT_LIST_SIZE) {
            editIndex[editCount] = cell;
        }
        ++editCount;
    }
    
    public void commit() {
        sync(background, foreground);
    }
    
    public void rollback() {
        sync(foreground, background);
    }
    
    public boolean isDirty() {
        return editCount > 0;
    }
    
    private synchronized void sync(double[] to, double[] from) {
        if (editCount <= EDIT_LIST_SIZE) {
            for (int e = 0; e < editCount; ++e) {
                int i = editIndex[e];
                to[i] = from[i];
            }
        } else {
            System.arraycopy(from, 0, to, 0, to.length);
        }
        editCount = 0;
    }
    
    public double[] getRaw() {
        return foreground;
    }
}
