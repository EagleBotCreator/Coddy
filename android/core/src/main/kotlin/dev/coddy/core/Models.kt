package dev.coddy.core

import kotlinx.serialization.Serializable

const val MAX_SNAPSHOT_BYTES = 8 * 1024 * 1024
val CATEGORIES = setOf("fact", "preference", "decision", "experience", "project", "lesson")

@Serializable data class MemoryNote(val id: String, val category: String, val title: String, val content: String, val source: String, val created_at: String)
@Serializable data class MemorySnapshot(val format: String = "coddy.snapshot", val version: Int = 1, val exported_at: String, val notes: List<MemoryNote>)
@Serializable enum class ProposalState { DRAFT, REVIEWED, REJECTED }
@Serializable data class MemoryProposal(val id: String, val category: String, val title: String, val content: String, val source: String, val importance: Int, val confidence: Double, val proposed_at: String, val state: ProposalState = ProposalState.DRAFT)
@Serializable data class ProposalBundle(val format: String = "coddy.proposals", val version: Int = 1, val exported_at: String, val proposals: List<MemoryProposal>)
@Serializable data class ChatMessage(val id: String, val role: String, val content: String, val created_at: String, val sources: List<String> = emptyList(), val stopped: Boolean = false)
data class PromptMessage(val role: String, val content: String)
@Serializable enum class ChatProvider { LOCAL, DESKTOP }
@Serializable data class ConnectionSettings(val baseUrl: String = "", val workspace: String = "", val provider: ChatProvider = ChatProvider.LOCAL)
