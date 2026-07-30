package io.surprise.ciphertun.bg;

import android.os.ParcelFileDescriptor;
import io.surprise.ciphertun.bg.IBridgeSession;
import io.surprise.ciphertun.bg.INeighborTableCallback;
import io.surprise.ciphertun.bg.IRootShellSession;
import io.surprise.ciphertun.bg.ParceledListSlice;

interface IRootService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    ParceledListSlice getInstalledPackages(int flags, int userId) = 1;

    void installPackage(in ParcelFileDescriptor apk, long size, int userId) = 2;

    String exportDebugInfo(String outputPath) = 3;

    void registerNeighborTableCallback(in INeighborTableCallback callback) = 4;

    oneway void unregisterNeighborTableCallback(in INeighborTableCallback callback) = 5;

    IRootShellSession openShellSession(String user, String command, in String[] env, String term, int rows, int cols) = 6;

    String lookupSFTPServer() = 7;

    IBridgeSession openBridge(String bridgeName, int mtu, String inet4Port, String inet6Port, int ruleIndex, int routeTable) = 8;
}
