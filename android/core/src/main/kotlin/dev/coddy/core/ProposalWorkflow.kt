package dev.coddy.core
import kotlinx.serialization.encodeToString
object ProposalWorkflow {
 fun validate(p: MemoryProposal): MemoryProposal {
  require(memoryId.matches(p.id) && p.category in CATEGORIES) { "Choose a supported memory category." }
  validateText(p.title,p.content); validTime(p.proposed_at)
  require(p.source.isNotBlank() && p.source.length<=240 && !credentialPattern.containsMatchIn(p.source)) { "Add a source without credentials (up to 240 characters)." }
  require(p.importance in 1..5 && p.confidence.isFinite() && p.confidence in 0.0..1.0) { "Check importance and confidence." }
  return p
 }
 fun review(p: MemoryProposal): MemoryProposal { require(p.state!=ProposalState.REJECTED){"A rejected proposal cannot be reviewed."};return validate(p).copy(state=ProposalState.REVIEWED) }
 fun reject(p: MemoryProposal) = p.copy(state=ProposalState.REJECTED)
 fun export(proposals: List<MemoryProposal>, at: String): String {
  validTime(at)
  val reviewed=proposals.filter{it.state==ProposalState.REVIEWED}.map{validate(it)}
  require(reviewed.size<=2000 && reviewed.map{it.id}.toSet().size==reviewed.size){"Invalid proposal export."}
  val text=CoddyJson.encodeToString(ProposalBundle(exported_at=at,proposals=reviewed))
  require(text.toByteArray().size<=MAX_SNAPSHOT_BYTES){"Export exceeds 8 MiB."}
  return text
 }
}
