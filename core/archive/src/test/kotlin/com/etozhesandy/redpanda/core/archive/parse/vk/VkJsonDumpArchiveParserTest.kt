package com.etozhesandy.redpanda.core.archive.parse.vk

import com.etozhesandy.redpanda.core.archive.parse.ParseSink
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.ProfileDetails
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The same dumper ships in two builds that differ only in what its files are called, and for a long
 * time only the English one was readable. The Russian one names the dialogs directory
 * `conversations`, the profile `страница.json`, the friend list `друзья.json`, writes plain JSON
 * where the English build writes a `let dialogjson = …` assignment, and keeps `companions.json`
 * *inside* the dialogs directory — which is why dialog files have to be picked by name.
 */
class VkJsonDumpArchiveParserTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val parser = VkJsonDumpArchiveParser(
        ioDispatcher = Dispatchers.Unconfined,
        defaultDispatcher = Dispatchers.Unconfined,
    )

    private val dialog = """
        {"messages":[{"id":1,"from":7,"text":"Привет","time":1624266625},
                     {"id":2,"from":1000,"text":"И тебе","time":1624266682}],
         "profiles":[{"firstName":"Контакт","lastName":"Первый","id":7,"gender":2}]}
    """.trimIndent()

    @Test
    fun `reads the english layout`() {
        val root = newRoot()
        val json = File(root, "json").apply { mkdirs() }
        File(json, "profile.json").writeText("""let profilejson = {"id":1000,"firstName":"Профиль","lastName":"Первый"}""")
        File(json, "friends.json").writeText("let friendsjson = []")
        File(json, "companions.json").writeText("""let companionsjson = [{"id":7,"firstName":"Контакт","lastName":"Первый"}]""")
        File(json, "dialogs").mkdirs()
        File(json, "dialogs/7.json").writeText("let dialogjson = $dialog")

        val sink = parse(root)

        assertEquals(1, sink.dialogsDiscovered)
        assertEquals(listOf("Привет", "И тебе"), sink.messages.sortedBy { it.timestampEpoch }.map { it.text })
        assertEquals("Профиль Первый", sink.displayName)
    }

    @Test
    fun `reads the russian layout`() {
        val root = newRoot()
        val json = File(root, "json").apply { mkdirs() }
        File(json, "страница.json").writeText("""{"id":1000,"firstName":"Профиль","lastName":"Второй"}""")
        File(json, "друзья.json").writeText("[]")
        File(json, "conversations").mkdirs()
        File(json, "conversations/7.json").writeText(dialog)
        File(json, "conversations/companions.json").writeText("""[{"id":7,"firstName":"Контакт","lastName":"Первый"}]""")

        val sink = parse(root)

        assertEquals(1, sink.dialogsDiscovered)
        assertEquals(listOf("Привет", "И тебе"), sink.messages.sortedBy { it.timestampEpoch }.map { it.text })
        assertEquals("Профиль Второй", sink.displayName)
        assertEquals(listOf("Контакт Первый"), sink.dialogs.map { it.peerName })
    }

    /**
     * `companions.json` lives beside the dialogs in the Russian layout. Taking every `.json` file
     * in that directory invented an empty conversation called `companions` and reported one dialog
     * more than the export holds.
     */
    @Test
    fun `does not read the companions file as a dialog`() {
        val root = newRoot()
        val json = File(root, "json").apply { mkdirs() }
        File(json, "conversations").mkdirs()
        File(json, "conversations/7.json").writeText(dialog)
        File(json, "conversations/companions.json").writeText("""[{"id":7,"firstName":"Контакт","lastName":"Первый"}]""")

        val sink = parse(root)

        assertEquals(1, sink.dialogsDiscovered)
        assertEquals(listOf("7"), sink.dialogs.map { it.peerId })
    }

    /** A dump whose profile file went missing still has every message in it. */
    @Test
    fun `reads a dump with no profile file`() {
        val root = newRoot()
        val json = File(root, "json").apply { mkdirs() }
        File(json, "dialogs").mkdirs()
        File(json, "dialogs/7.json").writeText(dialog)

        assertEquals(2, parse(root).messages.size)
    }

    private fun newRoot(): File = temporaryFolder.newFolder(temporaryFolder.root.list()!!.size.toString())

    private fun parse(root: File): RecordingSink =
        RecordingSink().also { sink -> runBlocking { parser.parse(root, "p1", sink) } }

    private class RecordingSink : ParseSink {
        val dialogs = mutableListOf<ChatDialog>()
        val messages = mutableListOf<Message>()
        val friends = mutableListOf<Friend>()
        var displayName: String? = null
        var dialogsDiscovered: Int = 0

        override suspend fun onDisplayName(name: String) { displayName = name }
        override suspend fun onDialogsDiscovered(total: Int) { dialogsDiscovered = total }
        override suspend fun onDialog(dialog: ChatDialog) { dialogs += dialog }
        override suspend fun onMessages(batch: List<Message>) { messages += batch }
        override suspend fun onAttachments(batch: List<Attachment>) = Unit
        override suspend fun onProfileDetails(details: ProfileDetails) = Unit
        override suspend fun onFriends(batch: List<Friend>) { friends += batch }
    }
}
