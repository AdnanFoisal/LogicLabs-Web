package com.logiclabs.core.data.persistence

import com.logiclabs.core.bridge.circuit.BreadboardCircuit
import com.logiclabs.core.bridge.model.WireColor
import com.logiclabs.core.bridge.topology.AD200Topology
import com.logiclabs.core.data.progress.ProgressRepository
import com.logiclabs.core.data.session.SessionKind
import com.logiclabs.core.data.session.SessionRepository
import com.logiclabs.core.data.settings.SettingsRepository
import com.logiclabs.core.data.settings.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DesktopPersistenceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun resolveDataDir_usesWindowsAppDataByDefault() {
        val appDir = tempFolder.newFolder("app")
        val fakeAppData = tempFolder.newFolder("AppDataRoaming")
        val resolved = DesktopAppPaths.resolveDataDir(
            appDir = appDir,
            osName = "Windows 10",
            env = mapOf("APPDATA" to fakeAppData.absolutePath),
            userHome = tempFolder.root.absolutePath
        )
        assertEquals(File(fakeAppData, "LogicLabs"), resolved)
        assertFalse(DesktopAppPaths.isPortableMode(appDir))
    }

    @Test
    fun resolveDataDir_switchesToPortableWhenMarkerPresent() {
        val appDir = tempFolder.newFolder("portableApp")
        File(appDir, DesktopAppPaths.PORTABLE_MARKER).writeText("portable")
        val resolved = DesktopAppPaths.resolveDataDir(
            appDir = appDir,
            osName = "Windows 10",
            env = mapOf("APPDATA" to tempFolder.newFolder("ignored").absolutePath),
            userHome = tempFolder.root.absolutePath
        )
        assertEquals(File(appDir, "data"), resolved)
        assertTrue(DesktopAppPaths.isPortableMode(appDir))
    }

    @Test
    fun repositories_persistAcrossRestart() = runBlocking {
        val dataDir = tempFolder.newFolder("data")

        val settings1 = SettingsRepository(dataDir)
        settings1.setThemeMode(ThemeMode.AMBER_CRT)
        settings1.setDefaultRoutingManhattan(true)
        settings1.setSoundEnabled(false)
        settings1.flush()

        val settings2 = SettingsRepository(dataDir)
        assertEquals(ThemeMode.AMBER_CRT, settings2.current.themeMode)
        assertTrue(settings2.current.defaultRoutingManhattan)
        assertFalse(settings2.current.soundEnabled)

        val progress1 = ProgressRepository(dataDir)
        val badges = progress1.markLabVerified("lab1_not", 12)
        assertTrue(badges.any { it.id == "truth_seeker" })
        val progress2 = ProgressRepository(dataDir)
        assertTrue(progress2.markLabVerified("lab1_not", 12).isEmpty())

        val projects = ProjectsRepository(dataDir)
        val circuit = BreadboardCircuit()
        circuit.addChip("7408", 1, 10)
        circuit.addWire(AD200Topology.TERM_SW0, AD200Topology.terminalSocket(1, 10, 0), WireColor.YELLOW)
        val saved = projects.save(ProjectPersistence.serialize(circuit, "Win10 AND Test"))
        assertTrue(saved.isNew)
        assertEquals(1, projects.list().size)
        val loaded = projects.load(saved.project.id)
        assertNotNull(loaded)
        assertEquals("Win10 AND Test", loaded!!.title)

        val session = SessionRepository(dataDir)
        session.recordProject(saved.project.id)
        assertEquals(SessionKind.PROJECT, session.current()?.kind)
    }
}
