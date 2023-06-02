package company.thewebhook.util

import java.nio.ByteBuffer
import java.util.*

fun UUID.toBase64(): String {
    val byteBuffer = ByteBuffer.wrap(ByteArray(16))
    byteBuffer.putLong(this.mostSignificantBits)
    byteBuffer.putLong(this.leastSignificantBits)
    return Base64.getEncoder().encodeToString(byteBuffer.array())
}

fun String.toUUID(): UUID {
    val byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(this))
    return UUID(byteBuffer.long, byteBuffer.long)
}
