/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.controller.search;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Niels
 */
public class SearchControllerTest {

    @Test
    public void search() {

        String txHash = "00200f74e456b0bcca2cd9467d9178ba0b011970532bf0ef73b14fe9fa7a0b5b08f6";
        String blockHash = "00204508bbbac4aecea0e31100a03c5ae70ed3bca09d85311612e265f311fa353a07";
        String height = "12345";
        String address = "TTarYnUfsftmm7DrStandCEdd4SNiELS";
        System.out.println(txHash.length());
        System.out.println(address.length());
    }
}