package com.amiralibg.panelix.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

data class PickedFolder(val uri: Uri, val flags: Int)

class SafFolderPicker : ActivityResultContract<Unit?, PickedFolder?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PickedFolder? {
        if (resultCode != Activity.RESULT_OK || intent?.data == null) return null
        return PickedFolder(intent.data!!, intent.flags)
    }
}
