package com.softwareverde.bitcoin.jni;

public class NativeUnspentTransactionOutputCache {
    public static native void _init();
    public static native void _destroy();

    public static native int _createCache();
    public static native void _deleteCache(int instanceId);
    public static native void _cacheUnspentTransactionOutputId(int instanceId, byte[] transactionHash, int transactionOutputIndex, long transactionOutputId);
    public static native long _getCachedUnspentTransactionOutputId(int instanceId, byte[] transactionHash, int transactionOutputIndex);
    public static native void _setMasterCache(int instanceId, int masterCacheId);
    public static native void _invalidateUnspentTransactionOutputId(int instanceId, byte[] transactionHash, int transactionOutputIndex);
    public static native void _commit(int instanceId, int masterCacheId);
    public static native void _commit(int instanceId);

    public static native void _setMaxItemCount(int instanceId, long maxItemCount);

    // ~53,750,000 items.  11GB loaded, 4GB disabled. ~139 bytes per item
    // 711,238,950
}