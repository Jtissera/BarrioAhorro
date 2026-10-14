package com.barrioahorro.app.ui.screens.business

/**
 * One photo as shown on the "Fotos del comercio" screen while editing.
 *
 * @property key stable identity inside the draft ("s<id>" for server photos, "n<n>" for new ones).
 * @property serverId id on the server, or null for a photo added in this draft.
 * @property url what the gallery renders: the server url, or the local uri string of a pending upload.
 * @property localUri image that still has to be uploaded (a new photo or the replacement of a server one).
 */
data class DraftPhoto(
    val key: String,
    val serverId: Long?,
    val url: String,
    val localUri: String? = null,
) {
    companion object {
        fun fromServer(photo: PhotoUi) = DraftPhoto(key = serverKey(photo.id), serverId = photo.id, url = photo.url)

        private fun serverKey(id: Long) = "s$id"
    }
}

/** A server call needed to turn the server state into the draft. */
sealed interface PhotoOperation {
    data class Delete(val photoId: Long) : PhotoOperation
    data class Replace(val key: String, val photoId: Long, val uri: String) : PhotoOperation
    data class Add(val key: String, val uri: String) : PhotoOperation

    /** Final order of the draft, by key; keys of uploaded photos resolve to their new server ids. */
    data class Reorder(val keys: List<String>) : PhotoOperation
}

/**
 * Local, unsaved photo edits. Nothing reaches the server until the user taps "Guardar cambios";
 * then [pendingOperations] says what to send and [rebase] folds the result back in.
 */
data class PhotoDraft(
    val original: List<PhotoUi>,
    val photos: List<DraftPhoto>,
    private val nextLocalId: Int = 0,
) {
    val hasChanges: Boolean get() = pendingOperations().isNotEmpty()

    val canAdd: Boolean get() = photos.size < MAX_BUSINESS_PHOTOS

    fun addPhoto(uri: String): PhotoDraft {
        if (!canAdd) return this
        val photo = DraftPhoto(key = "n$nextLocalId", serverId = null, url = uri, localUri = uri)
        return copy(photos = photos + photo, nextLocalId = nextLocalId + 1)
    }

    fun replacePhoto(key: String, uri: String): PhotoDraft =
        copy(photos = photos.map { if (it.key == key) it.copy(url = uri, localUri = uri) else it })

    fun removePhoto(key: String): PhotoDraft = copy(photos = photos.filterNot { it.key == key })

    /** Makes the photo the cover, which is always the first one. */
    fun setAsCover(key: String): PhotoDraft {
        val index = photos.indexOfFirst { it.key == key }
        if (index <= 0) return this
        return copy(photos = listOf(photos[index]) + photos.filterIndexed { i, _ -> i != index })
    }

    /** Deletes, then replacements, then additions, then the final order (only when it differs from what the server will hold). */
    fun pendingOperations(): List<PhotoOperation> {
        val keptIds = photos.mapNotNull { it.serverId }.toSet()
        val deletes = original.filter { it.id !in keptIds }.map { PhotoOperation.Delete(it.id) }
        val replaces = photos.mapNotNull { photo ->
            val id = photo.serverId ?: return@mapNotNull null
            photo.localUri?.let { PhotoOperation.Replace(photo.key, id, it) }
        }
        val newPhotos = photos.filter { it.serverId == null }
        val adds = newPhotos.mapNotNull { photo -> photo.localUri?.let { PhotoOperation.Add(photo.key, it) } }

        // The server keeps the original order and appends new photos at the end.
        val positionById = original.withIndex().associate { (index, photo) -> photo.id to index }
        val serverOrder = photos.filter { it.serverId != null }.sortedBy { positionById[it.serverId] ?: Int.MAX_VALUE } + newPhotos
        val reorder = if (serverOrder.map { it.key } != photos.map { it.key }) {
            listOf(PhotoOperation.Reorder(photos.map { it.key }))
        } else {
            emptyList()
        }
        return deletes + replaces + adds + reorder
    }

    /**
     * Rebuilds the draft on top of the photos reloaded from the server after a save.
     * [uploadedIds] maps the key of every upload that succeeded (add or replace) to its server id;
     * everything that did not apply stays pending.
     */
    fun rebase(server: List<PhotoUi>, uploadedIds: Map<String, Long>): PhotoDraft {
        val serverById = server.associateBy { it.id }
        val rebased = photos.mapNotNull { photo ->
            val uploadedId = uploadedIds[photo.key]
            when {
                uploadedId != null -> serverById[uploadedId]?.let(DraftPhoto::fromServer)
                // A failed upload keeps its local image; a replacement whose photo is gone becomes an addition.
                photo.localUri != null ->
                    if (photo.serverId == null || photo.serverId in serverById) photo else photo.copy(serverId = null)
                else -> serverById[photo.serverId]?.let(DraftPhoto::fromServer)
            }
        }
        // Photos the draft never knew about (added elsewhere) are shown too; failed deletes stay removed.
        val known = original.map { it.id }.toSet() + rebased.mapNotNull { it.serverId }
        val unknown = server.filter { it.id !in known }.map(DraftPhoto::fromServer)
        return copy(original = server, photos = rebased + unknown)
    }

    companion object {
        fun fromServer(photos: List<PhotoUi>) = PhotoDraft(original = photos, photos = photos.map(DraftPhoto::fromServer))
    }
}
