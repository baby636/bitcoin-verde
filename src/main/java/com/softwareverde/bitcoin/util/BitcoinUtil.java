package com.softwareverde.bitcoin.util;

import com.softwareverde.bitcoin.type.bytearray.ByteArray;
import com.softwareverde.security.encoding.Base58;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BitcoinUtil {
    private final static char[] HEX_ALPHABET = "0123456789ABCDEF".toCharArray();

    /**
     * Returns an uppercase hex representation of the provided bytes without any prefix.
     */
    public static String toHexString(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j=0; j<bytes.length; ++j) {
            final int v = (bytes[j] & 0xFF);
            hexChars[j * 2] = HEX_ALPHABET[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ALPHABET[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static String toHexString(final ByteArray bytes) {
        return toHexString(bytes.getBytes());
    }

    /**
     * Returns the decoded bytes from an uppercase (or lowercase) hex representation without any prefix.
     */
    public static byte[] hexStringToByteArray(final String hexString) {
        final Integer stringLength = hexString.length();
        if (stringLength % 2 != 0) { return null; }

        final byte[] data = new byte[stringLength / 2];
        for (int i = 0; i < stringLength; i += 2) {
            data[i/2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] sha256(final byte[] data) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(data);
        }
        catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] ripemd160(final byte[] data) {
        final RIPEMD160Digest ripemd160Digest = new RIPEMD160Digest();
        ripemd160Digest.update(data, 0, data.length);
        final byte[] output = new byte[ripemd160Digest.getDigestSize()];
        ripemd160Digest.doFinal(output, 0);
        return output;
    }

    public static String toBase58String(final byte[] bytes) {
        return Base58.encode(bytes);
    }

    public static byte[] calculateChecksummedKey(final Byte addressPrefix, final byte[] keyData) {
        final Integer prefixByteCount = (addressPrefix == null ? 0 : 1);
        final Integer checksumByteCount = 4;

        final byte[] base58CheckEncoded = new byte[prefixByteCount + keyData.length + checksumByteCount];
        final byte[] versionPayload = new byte[prefixByteCount + keyData.length];
        {
            if (addressPrefix != null) {
                base58CheckEncoded[0] = addressPrefix;
                versionPayload[0] = addressPrefix;
            }
            for (int i = 0; i < keyData.length; ++i) {
                versionPayload[prefixByteCount + i] = keyData[i];
                base58CheckEncoded[prefixByteCount + i] = keyData[i];
            }
        }

        { // Calculate the checksum...
            final byte[] fullChecksum = BitcoinUtil.sha256(BitcoinUtil.sha256(versionPayload));
            for (int i = 0; i < checksumByteCount; ++i) {
                base58CheckEncoded[prefixByteCount + keyData.length + i] = fullChecksum[i];
            }
        }

        return base58CheckEncoded;
    }
}
