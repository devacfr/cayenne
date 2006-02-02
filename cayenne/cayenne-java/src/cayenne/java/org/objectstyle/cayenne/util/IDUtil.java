/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * helper class to generate pseudo-GUID sequences.
 * 
 * @author Andrus Adamchik
 */
public class IDUtil {

    // this id sequence doesn't have to be very long... it only addresses the need to feel
    // the gap within the same timestamp millisecond
    private static volatile byte currentId = Byte.MIN_VALUE;
    private static MessageDigest md;
    private static byte[] ipAddress;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new CayenneRuntimeException("Can't initialize MessageDigest.", e);
        }

        try {
            ipAddress = java.net.InetAddress.getLocalHost().getAddress();
        }
        catch (UnknownHostException e) {
            // use loopback interface
            ipAddress = new byte[] {
                    127, 0, 0, 1
            };
        }
    }

    /**
     * Prints a byte value to a StringBuffer as a double digit hex value.
     * 
     * @since 1.2
     */
    public static void appendFormattedByte(StringBuffer buffer, byte byteValue) {
        final String digits = "0123456789ABCDEF";

        buffer.append(digits.charAt((byteValue >>> 4) & 0xF));
        buffer.append(digits.charAt(byteValue & 0xF));
    }

    /**
     * @param length the length of returned byte[]
     * @return A pseudo-unique byte array of the specified length. Length must be at least
     *         16 bytes, or an exception is thrown.
     * @since 1.0.2
     */
    public synchronized static byte[] pseudoUniqueByteSequence(int length) {
        if (length < 16) {
            throw new IllegalArgumentException(
                    "Can't generate unique byte sequence shorter than 16 bytes: "
                            + length);
        }

        if (length == 16) {
            return pseudoUniqueByteSequence16();
        }

        byte[] bytes = new byte[length];
        for (int i = 0; i <= length - 16; i += 16) {
            byte[] nextSequence = pseudoUniqueByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, i, 16);
        }

        // leftovers?
        int leftoverLen = length % 16;
        if (leftoverLen > 0) {
            byte[] nextSequence = pseudoUniqueByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, length - leftoverLen, leftoverLen);
        }

        return bytes;
    }

    public synchronized static byte[] pseudoUniqueSecureByteSequence(int length) {
        if (length < 16) {
            throw new IllegalArgumentException(
                    "Can't generate unique byte sequence shorter than 16 bytes: "
                            + length);
        }

        if (length == 16) {
            return pseudoUniqueSecureByteSequence16();
        }

        byte[] bytes = new byte[length];
        for (int i = 0; i <= length - 16; i += 16) {
            byte[] nextSequence = pseudoUniqueSecureByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, i, 16);
        }

        // leftovers?
        int leftoverLen = length % 16;
        if (leftoverLen > 0) {
            byte[] nextSequence = pseudoUniqueSecureByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, length - leftoverLen, leftoverLen);
        }

        return bytes;
    }

    public static final byte[] pseudoUniqueByteSequence8() {
        byte[] bytes = new byte[8];

        // bytes 0 - incrementing #
        // bytes 1..3 - timestamp high bytes
        // bytes 4..7 - IP address

        bytes[0] = nextId();

        long t = System.currentTimeMillis();
        
        // append 3 high bytes
        for (int i = 0; i < 3; ++i) {
            int off = (3 - i) * 8;
            bytes[i + 1] = (byte) ((t & (0xff << off)) >>> off);
        }

        System.arraycopy(ipAddress, 0, bytes, 4, 4);

        return bytes;
    }

    /**
     * @return A pseudo unique 16-byte array.
     */
    public static final byte[] pseudoUniqueByteSequence16() {
        byte[] bytes = new byte[16];

        // bytes 0..3 - incrementing #
        // bytes 4..11 - timestamp
        // bytes 12..15 - IP address

        appendIntBytes(bytes, 0, nextId());
        appendLongBytes(bytes, 4, System.currentTimeMillis());
        System.arraycopy(ipAddress, 0, bytes, 12, 4);

        return bytes;
    }

    /**
     * @return A pseudo unique digested 16-byte array.
     */
    public static byte[] pseudoUniqueSecureByteSequence16() {
        byte[] bytes = pseudoUniqueByteSequence16();

        synchronized (md) {
            return md.digest(bytes);
        }
    }

    private static final byte nextId() {
        if (currentId >= Byte.MAX_VALUE - 1) {
            currentId = Byte.MIN_VALUE;
        }

        return currentId++;
    }

    private static final void appendIntBytes(byte[] bytes, int offset, int value) {
        for (int i = 0; i < 4; ++i) {
            int off = (3 - i) * 8;
            bytes[i + offset] = (byte) ((value & (0xff << off)) >>> off);
        }
    }

    private static final void appendLongBytes(byte[] bytes, int offset, long value) {
        for (int i = 0; i < 8; ++i) {
            int off = (7 - i) * 8;
            bytes[i + offset] = (byte) ((value & (0xff << off)) >>> off);
        }
    }

    private IDUtil() {
    }
}
