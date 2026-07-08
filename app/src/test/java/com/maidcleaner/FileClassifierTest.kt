package com.maidcleaner

import com.maidcleaner.data.model.FileType
import com.maidcleaner.util.FileClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class FileClassifierTest {

    @Test
    fun classifyVideo() {
        assertEquals(FileType.VIDEO, FileClassifier.classify("movie.mp4"))
        assertEquals(FileType.VIDEO, FileClassifier.classify("clip.mkv"))
    }

    @Test
    fun classifyAudio() {
        assertEquals(FileType.AUDIO, FileClassifier.classify("song.mp3"))
        assertEquals(FileType.AUDIO, FileClassifier.classify("audio.flac"))
    }

    @Test
    fun classifyImage() {
        assertEquals(FileType.IMAGE, FileClassifier.classify("photo.jpg"))
        assertEquals(FileType.IMAGE, FileClassifier.classify("pic.png"))
        assertEquals(FileType.IMAGE, FileClassifier.classify("shot.webp"))
    }

    @Test
    fun classifyDocument() {
        assertEquals(FileType.DOCUMENT, FileClassifier.classify("report.pdf"))
        assertEquals(FileType.DOCUMENT, FileClassifier.classify("sheet.xlsx"))
    }

    @Test
    fun classifyArchive() {
        assertEquals(FileType.ARCHIVE, FileClassifier.classify("backup.zip"))
        assertEquals(FileType.ARCHIVE, FileClassifier.classify("data.tar.gz"))
    }

    @Test
    fun classifyApk() {
        assertEquals(FileType.APK, FileClassifier.classify("app.apk"))
    }

    @Test
    fun classifyOther() {
        assertEquals(FileType.OTHER, FileClassifier.classify("data.bin"))
        assertEquals(FileType.OTHER, FileClassifier.classify("unknown.xyz"))
    }

    @Test
    fun classifyCaseInsensitive() {
        assertEquals(FileType.IMAGE, FileClassifier.classify("Photo.JPG"))
        assertEquals(FileType.VIDEO, FileClassifier.classify("Movie.MP4"))
    }

    @Test
    fun isJunkFile() {
        assert(FileClassifier.isJunkFile("temp.tmp"))
        assert(FileClassifier.isJunkFile("cache.cache"))
        assert(FileClassifier.isJunkFile("debug.log"))
        assert(!FileClassifier.isJunkFile("photo.jpg"))
    }
}
