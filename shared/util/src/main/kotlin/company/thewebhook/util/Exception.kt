package company.thewebhook.util

class NotConnectedException : Exception()

class MessageTooLargeException : Exception()

class EmptyResultException : Exception()

class ConfigException(message: String) : Exception(message)
