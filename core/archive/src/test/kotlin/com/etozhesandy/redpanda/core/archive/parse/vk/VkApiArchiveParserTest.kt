package com.etozhesandy.redpanda.core.archive.parse.vk

import com.etozhesandy.redpanda.core.archive.parse.ParseSink
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.ProfileDetails
import com.etozhesandy.redpanda.core.model.Sex
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Covers the JSON shapes a real VK API dump uses that a naive reader gets wrong.
 *
 * The theme running through most of these: these exports write an absent field as an explicit
 * `null` rather than by leaving the key out. In kotlinx.serialization that is `JsonNull`, an
 * ordinary `JsonElement` — so `element?.jsonObject` does not short-circuit on it the way it does
 * on a Kotlin `null`, it throws. Because [VkApiArchiveParser] parses peers inside `async`, one
 * such throw propagates out of `awaitAll` and fails the whole import with zero dialogs saved
 * rather than degrading to a single bad message.
 */
class VkApiArchiveParserTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val parser = VkApiArchiveParser(
        ioDispatcher = Dispatchers.Unconfined,
        defaultDispatcher = Dispatchers.Unconfined,
    )

    private val owner = """{"id":"1000","first_name":"Николай","last_name":"Андросов"}"""

    /** Builds a dump with one peer directory and returns the content root. */
    private fun dump(
        page: String,
        conversation: String = """{"user":{"id":7,"first_name":"Иван","last_name":"Петров"}}""",
        profile: String = owner,
        data: String = """{"jsons_count":1}""",
        peerId: String = "7",
    ): File {
        val root = temporaryFolder.newFolder(temporaryFolder.root.list()!!.size.toString())
        File(root, "profile.json").writeText(profile)
        val messages = File(root, "messages").apply { mkdirs() }
        File(messages, "conversations.json").writeText("""conversations={"$peerId":$conversation}""")
        val peer = File(messages, peerId).apply { mkdirs() }
        File(peer, "data.json").writeText("data=$data")
        File(peer, "1.json").writeText("messages=$page")
        return root
    }

    private fun parse(root: File): RecordingSink =
        RecordingSink().also { sink -> runBlocking { parser.parse(root, "p1", sink) } }

    /**
     * The regression this file exists for: one real export writes `"reply_message": null` on all
     * 517 136 of its messages, so the very first message of the very first dialog threw and the
     * import finished with no dialogs at all.
     */
    @Test
    fun `reply_message written as an explicit null still parses the dialog`() {
        val sink = parse(
            dump("""[{"id":1,"from_id":7,"date":100,"text":"привет","out":false,"reply_message":null}]"""),
        )

        assertEquals(1, sink.dialogs.size)
        assertEquals("Иван Петров", sink.dialogs.single().peerName)
        assertEquals(listOf("привет"), sink.messages.map { it.text })
    }

    @Test
    fun `fwd_messages written as an explicit null still parses the dialog`() {
        val sink = parse(
            dump("""[{"id":1,"from_id":7,"date":100,"text":"привет","fwd_messages":null}]"""),
        )

        assertEquals(1, sink.dialogs.size)
        assertEquals(listOf("привет"), sink.messages.map { it.text })
    }

    /** `"user": null` on a group conversation must fall through to `group`, not throw. */
    @Test
    fun `a conversation whose user is null falls back to the group`() {
        val sink = parse(
            dump(
                page = """[{"id":1,"from_id":5,"date":100,"text":"эй"}]""",
                conversation = """{"user":null,"group":{"id":5,"name":"Сообщество"}}""",
                peerId = "5",
            ),
        )

        assertEquals("Сообщество", sink.dialogs.single().peerName)
    }

    @Test
    fun `an explicit null avatar leaves the dialog without one`() {
        val sink = parse(
            dump(
                page = """[{"id":1,"from_id":7,"date":100,"text":"эй"}]""",
                conversation = """{"user":{"id":7,"first_name":"Иван","photo_100":null,"photo_50":null}}""",
            ),
        )

        assertNull(sink.dialogs.single().peerAvatarPath)
    }

    /** A stray non-object in a page array costs that one message, never the dialog. */
    @Test
    fun `a page entry that is not an object is skipped`() {
        val sink = parse(
            dump("""[{"id":1,"from_id":7,"date":100,"text":"a"},42,{"id":2,"from_id":7,"date":200,"text":"b"}]"""),
        )

        assertEquals(listOf("a", "b"), sink.messages.map { it.text })
        assertEquals(2, sink.dialogs.single().messageCount)
    }

    /** Guarding the nulls must not quietly disable the feature the guards sit in front of. */
    @Test
    fun `a real reply and forward are still folded into the message text`() {
        val sink = parse(
            dump(
                """
                [{"id":1,"from_id":1000,"date":100,"out":true,"text":"ответ",
                  "reply_message":{"from_id":7,"text":"вопрос"},
                  "fwd_messages":[{"from_id":7,"text":"переслано"}]}]
                """.trimIndent(),
            ),
        )

        assertEquals(
            "ответ\n> вопрос\n[Пересланное от Иван Петров]: переслано",
            sink.messages.single().text,
        )
    }

    /** VK writes a place as a bare string in some exports and as `{id, title}` in others. */
    @Test
    fun `country and city written as objects are read from their title`() {
        val sink = parse(
            dump(
                page = """[{"id":1,"from_id":7,"date":100,"text":"эй"}]""",
                profile = """
                    {"id":"1000","first_name":"Николай","last_name":"Андросов","sex":2,
                     "country":{"id":1,"title":"Россия"},"city":{"id":73,"title":"Калуга"}}
                """.trimIndent(),
            ),
        )

        val details = requireNotNull(sink.details)
        assertEquals("Россия", details.country)
        assertEquals("Калуга", details.city)
        assertEquals(Sex.MALE, details.sex)
    }

    @Test
    fun `a bare string country and city are still read`() {
        val sink = parse(
            dump(
                page = """[{"id":1,"from_id":7,"date":100,"text":"эй"}]""",
                profile = """
                    {"id":"1000","first_name":"Николай","country":"Россия","city":"Калуга"}
                """.trimIndent(),
            ),
        )

        val details = requireNotNull(sink.details)
        assertEquals("Россия", details.country)
        assertEquals("Калуга", details.city)
    }

    /** `out` decides direction when present; without it the owner id does. */
    @Test
    fun `message direction comes from the out flag`() {
        val sink = parse(
            dump(
                """
                [{"id":1,"from_id":1000,"date":100,"text":"мой","out":true},
                 {"id":2,"from_id":7,"date":200,"text":"его","out":false},
                 {"id":3,"from_id":1000,"date":300,"text":"без флага"}]
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(true, false, true), sink.messages.map { it.isOutgoing })
    }

    /** Every peer directory is reported up front so progress can be shown as "N of M". */
    @Test
    fun `the dialog total is announced before parsing`() {
        val sink = parse(dump("""[{"id":1,"from_id":7,"date":100,"text":"эй"}]"""))

        assertEquals(1, sink.dialogsDiscovered)
        assertEquals("Николай Андросов", sink.displayName)
    }

    /** An unreadable page loses its messages, never the dialog it belongs to. */
    @Test
    fun `a page of invalid json does not lose the dialog`() {
        val sink = parse(dump("""[{"id":1,"from_id":7,"""))

        assertEquals(1, sink.dialogs.size)
        assertTrue(sink.messages.isEmpty())
    }

    private class RecordingSink : ParseSink {
        val dialogs = mutableListOf<ChatDialog>()
        val messages = mutableListOf<Message>()
        val attachments = mutableListOf<Attachment>()
        var details: ProfileDetails? = null
        var displayName: String? = null
        var dialogsDiscovered: Int = 0

        override suspend fun onDisplayName(name: String) { displayName = name }
        override suspend fun onDialogsDiscovered(total: Int) { dialogsDiscovered = total }
        override suspend fun onDialog(dialog: ChatDialog) { dialogs += dialog }
        override suspend fun onMessages(batch: List<Message>) { messages += batch }
        override suspend fun onAttachments(batch: List<Attachment>) { attachments += batch }
        override suspend fun onProfileDetails(details: ProfileDetails) { this.details = details }
    }
}
