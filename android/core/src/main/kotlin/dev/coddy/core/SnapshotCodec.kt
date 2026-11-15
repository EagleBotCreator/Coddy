package dev.coddy.core
import java.time.Instant
import kotlinx.serialization.json.Json

val CoddyJson = Json { prettyPrint = true; ignoreUnknownKeys = false; encodeDefaults = true }
internal val memoryId = Regex("[a-f0-9]{32}")
internal val credentialPattern = Regex("sk-[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{20,}|-----BEGIN .*PRIVATE KEY-----|(?:api[_-]?key|password|access[_-]?token)\\s*[:=]\\s*\\S{8,}", RegexOption.IGNORE_CASE)
internal fun validTime(value: String) { require(runCatching { Instant.parse(value) }.isSuccess) { "Use an ISO timestamp with a timezone." } }
internal fun validateText(title: String, content: String) {
 require(title.isNotBlank() && title.length <= 120 && title.none { it.isISOControl() }) { "Title must be 1–120 characters on one line." }
 require(content.isNotBlank() && content.length <= 12000) { "Memory must be 1–12,000 characters." }
 require(!credentialPattern.containsMatchIn(title + "\n" + content)) { "Credentials cannot be stored as memories." }
}
object SnapshotCodec {
 fun decode(text: String): MemorySnapshot {
  require(text.toByteArray(Charsets.UTF_8).size <= MAX_SNAPSHOT_BYTES) { "Snapshot exceeds 8 MiB." }
  val snapshot = try { CoddyJson.decodeFromString<MemorySnapshot>(text) } catch(e: Exception) { throw IllegalArgumentException("Choose a valid Coddy snapshot JSON file.", e) }
  require(snapshot.format=="coddy.snapshot" && snapshot.version==1) { "Unsupported snapshot version." }
  validTime(snapshot.exported_at)
  require(snapshot.notes.size<=2000) { "A snapshot can contain at most 2,000 memories." }
  require(snapshot.notes.map{it.id}.toSet().size==snapshot.notes.size) { "Duplicate memory IDs." }
  snapshot.notes.forEach { note ->
   require(memoryId.matches(note.id) && note.category in CATEGORIES) { "Invalid memory ID or category." }
   validateText(note.title,note.content); validTime(note.created_at)
   val source=note.source
   require(source.length<=320 && source.startsWith("01 Memories/") && source.endsWith(".md") && !source.contains('\\') && !source.contains(':') && source.none{it.isISOControl()} && source.split('/').none{it.isBlank()||it=="."||it==".."}) { "Unsafe memory source path." }
  }
  return snapshot
 }
}
