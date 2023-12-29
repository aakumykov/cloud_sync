package com.github.aakumykov.cloud_writer

import java.io.File
import java.io.IOException

class LocalCloudWriter : CloudWriter {

    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun createDir(path: String) {
        File(path).mkdir()
    }

    @Throws(IOException::class, CloudWriter.UnsuccessfulResponseException::class)
    override fun putFile(file: File, targetDirPath: String, overwriteIfExists: Boolean) {

        val fullTargetPath = "${targetDirPath}/${file.name}".replace(Regex("[/]+"),"/")
        val targetFile = File(fullTargetPath)

        val isMoved = file.renameTo(targetFile)

        if (!isMoved)
            throw IOException("File cannot be not moved from '${file.absolutePath}' to '${fullTargetPath}'")
    }
}