package io.surprise.ciphertun.bg;

import io.surprise.ciphertun.bg.ParceledListSlice;

interface INeighborTableCallback {
    oneway void onNeighborTableUpdated(in ParceledListSlice entries);
}
