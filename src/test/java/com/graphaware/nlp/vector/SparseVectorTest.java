/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class SparseVectorTest {

    public SparseVectorTest() {
    }

    @Test
    public void testFromMap() {
        Map<Long, Float> map = new HashMap<>();
        map.put(10L, 1.0f);
        map.put(1L, 1.1f);
        map.put(100L, 1.2f);
        map.put(5L, 1.3f);

        SparseVector vector = SparseVector.fromMap(map);
        System.out.println("> " + vector);
        assertEquals(4, vector.getCardinality().intValue());
        assertEquals(1l, vector.getIndex().get(0).longValue());
        assertEquals(5l, vector.getIndex().get(1).longValue());
        assertEquals(10l, vector.getIndex().get(2).longValue());
        assertEquals(100l, vector.getIndex().get(3).longValue());

        assertEquals(1.1f, vector.getValues().get(0), 0.0f);
        assertEquals(1.3f, vector.getValues().get(1), 0.0f);
        assertEquals(1.0f, vector.getValues().get(2), 0.0f);
        assertEquals(1.2f, vector.getValues().get(3), 0.0f);
    }

    @Test
    public void testFromList() {
        List<Float> list = Arrays.asList(3.0f, 11f, 12f, 100f, 0.1f, 0.2f, 3.0f);
        SparseVector vector = SparseVector.fromList(list);
        System.out.println("> " + vector);
        assertEquals(3, vector.getCardinality().intValue());
        assertEquals(11l, vector.getIndex().get(0).longValue());
        assertEquals(12l, vector.getIndex().get(1).longValue());
        assertEquals(100l, vector.getIndex().get(2).longValue());

        assertEquals(0.1f, vector.getValues().get(0), 0.0f);
        assertEquals(0.2f, vector.getValues().get(1), 0.0f);
        assertEquals(3.0f, vector.getValues().get(2), 0.0f);

        assertFalse(list.retainAll(vector.getList()));
    }

    @Test
    public void testGetList() {
        List<Float> list = Arrays.asList(4.0f, 10f, 11f, 12f, 100f, 0.1f, 0.2f, 3.0f, 0.2f);
        SparseVector vector = SparseVector.fromList(list);
        System.out.println("> " + vector);
        assertFalse(list.retainAll(vector.getList()));
    }

    @Test
    public void testDot() {
        List<Float> a = Arrays.asList(4.0f, 0f, 2f, 4f, 5f, 1f, 10f, 20.0f, 3f);
        List<Float> b = Arrays.asList(3.0f, 1f, 2f, 5f, 1f, 1f, 1f);
        SparseVector aV = SparseVector.fromList(a);
        SparseVector bV = SparseVector.fromList(b);
        assertEquals(13f, aV.dot(bV), 0.0f);
        assertEquals(13f, bV.dot(aV), 0.0f);
        List<Float> c = Arrays.asList(7.0f, 0f, 2f, 4f, 5f, 6f, 7f, 8f, 1f, 10f, 20.0f, 3f, 1f, 1f, 1f);
        List<Float> d = Arrays.asList(4.0f, 1f, 2f, 5f, 8f, 1f, 1f, 1f, 4f);
        SparseVector cV = SparseVector.fromList(c);
        SparseVector dV = SparseVector.fromList(d);
        assertEquals(17f, cV.dot(dV), 0.0f);
        assertEquals(17f, dV.dot(cV), 0.0f);
    }

    @Test
    public void testNorm() {
    }

}
