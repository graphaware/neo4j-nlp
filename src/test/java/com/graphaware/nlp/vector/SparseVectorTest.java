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

import com.graphaware.nlp.ml.similarity.VectorProcessLogic;
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
    public void testDot2() {
        List<Float> a = Arrays.asList(36.0f, 480.0f, 2547.0f, 3268.0f, 3520.0f,
                4149.0f, 8418.0f, 12152.0f, 12301.0f, 12305.0f, 12391.0f,
                12429.0f, 12766.0f, 12823.0f, 13040.0f, 13172.0f, 13686.0f,
                13692.0f, 13932.0f, 15357.0f, 17535.0f, 30705.0f, 31238.0f,
                35337.0f, 42717.0f, 48192.0f, 51166.0f, 60242.0f, 73681.0f,
                115140.0f, 115150.0f, 115151.0f, 115168.0f, 115169.0f, 115170.0f,
                115171.0f, 115172.0f, 1.8579924f, 2.450645f, 1.489563f, 5.8959875f,
                1.2966125f, 1.6582532f, 0.75541353f, 1.5338151f, 1.3244672f, 1.7394404f,
                1.6663334f, 1.6522903f, 2.4761992f, 0.9694071f, 1.6367781f, 1.1489949f,
                1.2922825f, 2.155864f, 2.8807697f, 1.9735237f, 1.5814133f, 2.3226151f,
                2.994713f, 2.149615f, 3.091623f, 2.6145017f, 2.2787097f, 3.392653f,
                2.5633492f, 1.945495f, 3.149615f, 3.994713f, 3.994713f, 3.295743f,
                3.994713f, 1.4977833f);
        List<Float> b = Arrays.asList(4.0f, 3520.0f, 17535.0f, 22446.0f, 115150.0f, 3.9306583f, 1.9735237f, 3.392653f, 1.945495f);
        SparseVector aV = SparseVector.fromList(a);
        SparseVector bV = SparseVector.fromList(b);
        float dot = aV.dot(bV);
        System.out.println("Dot: " + dot);
        float similarity = VectorProcessLogic.getSimilarity(a, b);
        System.out.println("similarity: " + similarity);

//        assertEquals(13f, aV.dot(bV), 0.0f);
//        assertEquals(13f, bV.dot(aV), 0.0f);
//        List<Float> c = Arrays.asList(7.0f, 0f, 2f, 4f, 5f, 6f, 7f, 8f, 1f, 10f, 20.0f, 3f, 1f, 1f, 1f);
//        List<Float> d = Arrays.asList(4.0f, 1f, 2f, 5f, 8f, 1f, 1f, 1f, 4f);
//        SparseVector cV = SparseVector.fromList(c);
//        SparseVector dV = SparseVector.fromList(d);
//        assertEquals(17f, cV.dot(dV), 0.0f);
//        assertEquals(17f, dV.dot(cV), 0.0f);
    }

    @Test
    public void testNorm() {
    }

}
