package dev.coddy.core
import java.text.Normalizer
import java.util.Locale
object MemorySearch {
 private fun normalized(value: String) = Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)
 fun search(notes: List<MemoryNote>, query: String, limit: Int = 20): List<MemoryNote> {
  val terms=Regex("[\\p{L}\\p{N}]+").findAll(normalized(query)).map{it.value}.toSet()
  if(terms.isEmpty()) return notes.sortedByDescending{it.created_at}.take(limit.coerceIn(0,2000))
  return notes.map{note->
   val title=normalized(note.title);val body=normalized(note.content)
   note to terms.sumOf{term->(if(title.contains(term)) 3 else 0)+(if(body.contains(term)) 1 else 0)}
  }.filter{it.second>0}.sortedWith(compareByDescending<Pair<MemoryNote,Int>>{it.second}.thenBy{it.first.id}).take(limit.coerceIn(0,2000)).map{it.first}
 }
}
