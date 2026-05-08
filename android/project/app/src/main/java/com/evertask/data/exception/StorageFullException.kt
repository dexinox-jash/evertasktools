package com.evertask.data.exception

/**
 * Exception thrown when a database write fails because device storage is full.
 */
class StorageFullException(message: String) : Exception(message)
