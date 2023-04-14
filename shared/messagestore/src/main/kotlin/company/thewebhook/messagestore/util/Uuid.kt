package company.thewebhook.messagestore.util

import java.nio.ByteBuffer
import java.util.*

fun generateBase64Uuid(): String {
    val uuid = UUID.randomUUID()
    val byteBuffer = ByteBuffer.wrap(ByteArray(16))
    byteBuffer.putLong(uuid.mostSignificantBits)
    byteBuffer.putLong(uuid.leastSignificantBits)
    return Base64.getEncoder().encodeToString(byteBuffer.array())
}
