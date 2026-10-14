package com.barrioahorro.app.ui.screens.business

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoDraftTest {

    private val server = listOf(PhotoUi(1, "u1"), PhotoUi(2, "u2"), PhotoUi(3, "u3"))
    private val draft = PhotoDraft.fromServer(server)

    @Test
    fun `a draft built from the server has no changes`() {
        assertFalse(draft.hasChanges)
        assertEquals(emptyList<PhotoOperation>(), draft.pendingOperations())
        assertEquals(listOf("u1", "u2", "u3"), draft.photos.map { it.url })
    }

    @Test
    fun `addPhoto appends a local photo that renders its uri`() {
        val updated = draft.addPhoto("content://new")

        assertEquals(4, updated.photos.size)
        val added = updated.photos.last()
        assertNull(added.serverId)
        assertEquals("content://new", added.url)
        assertTrue(updated.hasChanges)
    }

    @Test
    fun `addPhoto ignores photos over the limit`() {
        var full = PhotoDraft.fromServer(emptyList())
        repeat(MAX_BUSINESS_PHOTOS) { full = full.addPhoto("content://$it") }

        assertEquals(full, full.addPhoto("content://extra"))
    }

    @Test
    fun `replacePhoto shows the new image in the same position`() {
        val key = draft.photos[1].key
        val updated = draft.replacePhoto(key, "content://x")

        assertEquals(listOf("u1", "content://x", "u3"), updated.photos.map { it.url })
        assertEquals(2L, updated.photos[1].serverId)
        assertTrue(updated.hasChanges)
    }

    @Test
    fun `removePhoto drops a server photo and marks it for deletion`() {
        val updated = draft.removePhoto(draft.photos[0].key)

        assertEquals(listOf(2L, 3L), updated.photos.map { it.serverId })
        assertEquals(listOf<PhotoOperation>(PhotoOperation.Delete(1)), updated.pendingOperations())
    }

    @Test
    fun `removing a newly added photo leaves no pending changes`() {
        val withNew = draft.addPhoto("content://new")
        val updated = withNew.removePhoto(withNew.photos.last().key)

        assertFalse(updated.hasChanges)
        assertEquals(emptyList<PhotoOperation>(), updated.pendingOperations())
    }

    @Test
    fun `setAsCover moves the photo to the first position`() {
        val updated = draft.setAsCover(draft.photos[2].key)

        assertEquals(listOf(3L, 1L, 2L), updated.photos.map { it.serverId })
        assertTrue(updated.hasChanges)
        assertEquals(
            listOf<PhotoOperation>(PhotoOperation.Reorder(updated.photos.map { it.key })),
            updated.pendingOperations(),
        )
    }

    @Test
    fun `setAsCover on the current cover changes nothing`() {
        assertEquals(draft, draft.setAsCover(draft.photos[0].key))
    }

    @Test
    fun `pendingOperations orders deletes, replacements, additions and then the final order`() {
        val updated = draft
            .removePhoto(draft.photos[0].key)
            .replacePhoto(draft.photos[2].key, "content://r")
            .addPhoto("content://a")
        val newKey = updated.photos.last().key
        val withCover = updated.setAsCover(newKey)

        assertEquals(
            listOf(
                PhotoOperation.Delete(1),
                PhotoOperation.Replace(key = draft.photos[2].key, photoId = 3, uri = "content://r"),
                PhotoOperation.Add(key = newKey, uri = "content://a"),
                PhotoOperation.Reorder(listOf(newKey, draft.photos[1].key, draft.photos[2].key)),
            ),
            withCover.pendingOperations(),
        )
    }

    @Test
    fun `no reorder is needed when new photos stay at the end`() {
        val updated = draft.addPhoto("content://a")

        assertEquals(
            listOf<PhotoOperation>(PhotoOperation.Add(updated.photos.last().key, "content://a")),
            updated.pendingOperations(),
        )
    }

    @Test
    fun `rebase after a full save leaves no pending changes`() {
        val updated = draft
            .removePhoto(draft.photos[0].key)
            .replacePhoto(draft.photos[1].key, "content://r")
            .addPhoto("content://a")
        val addKey = updated.photos.last().key
        val replaceKey = draft.photos[1].key
        val reloaded = listOf(PhotoUi(20, "u20"), PhotoUi(3, "u3"), PhotoUi(40, "u40"))

        val rebased = updated.rebase(reloaded, uploadedIds = mapOf(replaceKey to 20L, addKey to 40L))

        assertFalse(rebased.hasChanges)
        assertEquals(listOf(20L, 3L, 40L), rebased.photos.map { it.serverId })
        assertEquals(listOf("u20", "u3", "u40"), rebased.photos.map { it.url })
    }

    @Test
    fun `rebase after a partial failure keeps what did not apply as pending`() {
        val updated = draft
            .removePhoto(draft.photos[0].key)
            .addPhoto("content://ok")
            .addPhoto("content://fail")
        val okKey = updated.photos[2].key
        val failKey = updated.photos[3].key
        // The delete of photo 1 failed, the first upload worked and the second one did not.
        val reloaded = listOf(PhotoUi(1, "u1"), PhotoUi(2, "u2"), PhotoUi(3, "u3"), PhotoUi(50, "u50"))

        val rebased = updated.rebase(reloaded, uploadedIds = mapOf(okKey to 50L))

        assertTrue(rebased.hasChanges)
        assertEquals(listOf("u2", "u3", "u50", "content://fail"), rebased.photos.map { it.url })
        assertEquals(
            listOf(PhotoOperation.Delete(1), PhotoOperation.Add(failKey, "content://fail")),
            rebased.pendingOperations(),
        )
    }

    @Test
    fun `rebase keeps a failed reorder pending`() {
        val updated = draft.setAsCover(draft.photos[1].key)

        val rebased = updated.rebase(server, uploadedIds = emptyMap())

        assertEquals(listOf(2L, 1L, 3L), rebased.photos.map { it.serverId })
        assertTrue(rebased.pendingOperations().single() is PhotoOperation.Reorder)
    }
}
