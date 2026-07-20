package io.surprise.ciphertun.update

sealed class UpdateCheckException : Exception() {
    class TrackNotSupported : UpdateCheckException()
}
