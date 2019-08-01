package com.intellij.idea.plugin.hybris.tools.remote.console

import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.execution.console.ConsoleRootType
import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.idea.plugin.hybris.common.HybrisConstants
import com.intellij.idea.plugin.hybris.common.HybrisConstants.*
import com.intellij.idea.plugin.hybris.common.HybrisConstants.IMPEX.CATALOG_VERSION_ONLINE
import com.intellij.idea.plugin.hybris.common.HybrisConstants.IMPEX.CATALOG_VERSION_STAGED
import com.intellij.idea.plugin.hybris.flexibleSearch.FlexibleSearchLanguage
import com.intellij.idea.plugin.hybris.impex.ImpexLanguage
import com.intellij.idea.plugin.hybris.settings.HybrisProjectSettingsComponent
import com.intellij.idea.plugin.hybris.settings.HybrisRemoteConnectionSettings
import com.intellij.idea.plugin.hybris.statistics.StatsCollector
import com.intellij.idea.plugin.hybris.tools.remote.console.preprocess.HybrisConsolePreProcessor
import com.intellij.idea.plugin.hybris.tools.remote.console.preprocess.HybrisConsolePreProcessorCatalogVersion
import com.intellij.idea.plugin.hybris.tools.remote.http.HybrisHacHttpClient
import com.intellij.idea.plugin.hybris.tools.remote.http.SolrHttpClient
import com.intellij.idea.plugin.hybris.tools.remote.http.impex.HybrisHttpResult
import com.intellij.idea.plugin.hybris.tools.remote.http.monitorImpexFiles
import com.intellij.idea.plugin.hybris.tools.remote.http.solr.SolrQueryObject
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.io.FileUtil.toCanonicalPath
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import org.apache.batik.ext.swing.GridBagConstants
import org.apache.commons.lang.StringUtils
import org.jetbrains.plugins.groovy.GroovyLanguage
import java.awt.*
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.jvm.internal.Intrinsics

/**
 * @author Nosov Aleksandr <nosovae.dev@gmail.com>
 */
abstract class HybrisConsole(project: Project, title: String, language: Language) : LanguageConsoleImpl(project, title, language) {
    init {
        this.printDefaultText()
    }

    abstract fun execute(query: String): HybrisHttpResult

    open fun preProcessors(): List<HybrisConsolePreProcessor> = listOf()

    open fun printDefaultText() {
        setInputText("")
    }

    open fun connectionType(): HybrisRemoteConnectionSettings.Type {
        return HybrisRemoteConnectionSettings.Type.Hybris
    }

    abstract fun collectStatistics()
}

class HybrisImpexConsole(project: Project) : HybrisConsole(project, IMPEX_CONSOLE_TITLE, ImpexLanguage.getInstance()) {
    override fun collectStatistics() {
        StatsCollector.getInstance().collectStat(StatsCollector.ACTIONS.IMPEX_CONSOLE)
    }

    object MyConsoleRootType : ConsoleRootType("hybris.impex.shell", null)

    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    private val catalogVersionLabel = JBLabel("Catalog Version")
    val catalogVersionComboBox = ComboBox(arrayOf(
        CatalogVersionOption("doesn't change", StringUtils.EMPTY),
        CatalogVersionOption("changes to $CATALOG_VERSION_STAGED", CATALOG_VERSION_STAGED),
        CatalogVersionOption("changes to $CATALOG_VERSION_ONLINE", CATALOG_VERSION_ONLINE)
    ))

    private val legacyModeCheckbox = JBCheckBox()
    private val legacyModeLabel = JBLabel("Legacy mode: ")

    override fun preProcessors() = listOf(HybrisConsolePreProcessorCatalogVersion())

    init {
        createUI()
        ConsoleHistoryController(MyConsoleRootType, "hybris.impex.shell", this).install()
    }

    private fun createUI() {
        catalogVersionComboBox.renderer = object : ListCellRendererWrapper<CatalogVersionOption>() {
            override fun customize(list: JList<*>?, value: CatalogVersionOption, index: Int, selected: Boolean, hasFocus: Boolean) {
                setText(value.name)
            }
        }
        catalogVersionComboBox.addItemListener {
            preProcessors().forEach { processor ->
                ApplicationManager.getApplication().invokeLater { this.setInputText(processor.process(this)) }
            }
        }
        catalogVersionLabel.border = EmptyBorder(0, 10, 0, 5)
        panel.add(catalogVersionLabel)
        panel.add(catalogVersionComboBox)
        legacyModeLabel.border = EmptyBorder(0, 10, 0, 5)
        legacyModeCheckbox.border = EmptyBorder(0, 0, 0, 5)
        panel.add(legacyModeLabel)
        panel.add(legacyModeCheckbox)

        add(panel, BorderLayout.NORTH)
        isEditable = true
    }

    override fun execute(query: String): HybrisHttpResult {
        val settings = mutableMapOf(
            "scriptContent" to query,
            "validationEnum" to "IMPORT_STRICT",
            "encoding" to "UTF-8",
            "maxThreads" to "4",
            "_legacyMode" to "on"
        )
        if (legacyModeCheckbox.isSelected) {
            settings["legacyMode"] = "true"
        }
        return HybrisHacHttpClient.getInstance(project).importImpex(project, settings)
    }

    fun validate(text: String): HybrisHttpResult {
        val settings = mutableMapOf(
            "scriptContent" to text,
            "validationEnum" to "IMPORT_STRICT",
            "encoding" to "UTF-8",
            "maxThreads" to "4",
            "_legacyMode" to "on"
        )
        if (legacyModeCheckbox.isSelected) {
            settings["legacyMode"] = "true"
        }
        return HybrisHacHttpClient.getInstance(project).validateImpex(project, settings)
    }
}

class HybrisFSConsole(project: Project) : HybrisConsole(project, FLEXIBLE_SEARCH_CONSOLE_TITLE, FlexibleSearchLanguage.getInstance()) {

    private val commitCheckbox = JBCheckBox()
    private val commitLabel = JBLabel("Commit mode: ")
    private val isPlainSQLCheckbox = JBCheckBox()
    private val isPlainSQLLabel = JBLabel("SQL: ")
    private val jSpinner = JSpinner(SpinnerNumberModel(10, 0, 1000, 1) as SpinnerModel)
    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

    override fun execute(query: String): HybrisHttpResult {
        return HybrisHacHttpClient.getInstance(project).executeFlexibleSearch(project, commitCheckbox.isSelected, isPlainSQLCheckbox.isSelected, jSpinner.value.toString(), query)
    }

    override fun collectStatistics() {
        StatsCollector.getInstance().collectStat(StatsCollector.ACTIONS.FLEXIBLE_SEARCH_CONSOLE)
    }
    object MyConsoleRootType : ConsoleRootType("hybris.fs.shell", null)

    private fun createUI(){
        commitLabel.border = EmptyBorder(0, 10, 0, 3)
        commitCheckbox.border = EmptyBorder(0, 0, 0, 5)
        panel.add(commitLabel)
        panel.add(commitCheckbox)
        isPlainSQLLabel.border = EmptyBorder(0, 10, 0, 3)
        isPlainSQLCheckbox.border = EmptyBorder(0, 0, 0, 5)
        panel.add(isPlainSQLLabel)
        panel.add(isPlainSQLCheckbox)
        panel.add(jSpinner)
        add(panel, BorderLayout.NORTH)
        isEditable = true
    }

    init {
        createUI()
        ConsoleHistoryController(MyConsoleRootType, "hybris.fs.shell", this).install()
    }
}

class HybrisGroovyConsole(project: Project) : HybrisConsole(project, GROOVY_CONSOLE_TITLE, GroovyLanguage) {
    override fun collectStatistics() {
        StatsCollector.getInstance().collectStat(StatsCollector.ACTIONS.GROOVY_CONSOLE)
    }

    object MyConsoleRootType : ConsoleRootType("hybris.groovy.shell", null)

    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    private val commitCheckbox = JBCheckBox()
    private val commitLabel = JBLabel("Commit mode: ")

    init {
        createUI()
        ConsoleHistoryController(MyConsoleRootType, "hybris.groovy.shell", this).install()
    }

    private fun createUI() {
        commitLabel.border = EmptyBorder(0, 10, 0, 3)
        commitCheckbox.border = EmptyBorder(0, 0, 0, 5)
        panel.add(commitLabel)
        panel.add(commitCheckbox)
        add(panel, BorderLayout.NORTH)
        isEditable = true
    }

    override fun execute(query: String): HybrisHttpResult {
        return HybrisHacHttpClient.getInstance(project).executeGroovyScript(project, query, commitCheckbox.isSelected)
    }
}


class HybrisImpexMonitorConsole(project: Project) : HybrisConsole(project, IMPEX_MONITOR_CONSOLE_TITLE, ImpexLanguage.getInstance()) {
    override fun collectStatistics() {
        StatsCollector.getInstance().collectStat(StatsCollector.ACTIONS.IMPEX_MONITOR)
    }

    object MyConsoleRootType : ConsoleRootType("hybris.impex.monitor.shell", null)

    private val panel = JPanel()
    private val timeComboBox = ComboBox(arrayOf(
        TimeOption("in the last 5 minutes", 5, TimeUnit.MINUTES),
        TimeOption("in the last 10 minutes", 10, TimeUnit.MINUTES),
        TimeOption("in the last 15 minutes", 15, TimeUnit.MINUTES),
        TimeOption("in the last 30 minutes", 30, TimeUnit.MINUTES),
        TimeOption("in the last 1 hour", 1, TimeUnit.HOURS)
    ))
    private val workingDirLabel = JBLabel("Hybris Data Folder: ${obtainDataFolder(project)}")
    private val timeOptionLabel = JBLabel("Imported Impex")

    init {
        createUI()
        ConsoleHistoryController(MyConsoleRootType, "hybris.impex.monitor.shell", this).install()
    }

    private fun createUI() {
        timeComboBox.renderer = object : ListCellRendererWrapper<TimeOption>() {
            override fun customize(list: JList<*>?, value: TimeOption, index: Int, selected: Boolean, hasFocus: Boolean) {
                setText(value.name)
            }
        }
        isConsoleEditorEnabled = false
        panel.layout = GridBagLayout()
        val constraints = GridBagConstraints()
        constraints.weightx = 0.0
        timeOptionLabel.border = EmptyBorder(0, 10, 0, 5)
        panel.add(timeOptionLabel)
        panel.add(timeComboBox, constraints)
        constraints.weightx = 1.0
        constraints.fill = GridBagConstants.HORIZONTAL
        workingDirLabel.border = EmptyBorder(0, 10, 0, 10)
        panel.add(workingDirLabel, constraints)
        add(panel, BorderLayout.NORTH)
        isEditable = true
    }

    private fun obtainDataFolder(project: Project): String {
        val settings = HybrisProjectSettingsComponent.getInstance(project).state
        return toCanonicalPath("${project.basePath}${File.separatorChar}${settings.hybrisDirectory}${File.separatorChar}${HybrisConstants.HYBRIS_DATA_DIRECTORY}")
    }

    private fun timeOption() = (timeComboBox.selectedItem as TimeOption)

    private fun workingDir() = obtainDataFolder(project)

    override fun execute(query: String): HybrisHttpResult {
        return monitorImpexFiles(timeOption().value, timeOption().unit, workingDir())
    }
}

class HybrisSolrConsole(project: Project) : HybrisConsole(project, SOLR_CONSOLE_TITLE, PlainTextLanguage.INSTANCE), SolrConsole  {
    override val coresComboBox = ComboBox(SolrHttpClient.getInstance(project).listOfCores(project), 300)
    private val jPanel = JPanel(FlowLayout(0, 0, 0))
    private val rowsLabel = JBLabel("Rows:")
    private val jSpinner = JSpinner(SpinnerNumberModel(10, 0, 1000, 1))
    private val availableCoresLabel = JBLabel("Available cores one")

    private fun createUI() {
        availableCoresLabel.border = EmptyBorder(0, 10, 0, 5)
        jPanel.add(availableCoresLabel)
        jPanel.add(coresComboBox)
        rowsLabel.border = EmptyBorder(0, 10, 0, 0)
        jPanel.add(rowsLabel)
        jPanel.add(jSpinner)
        add(jPanel, "North")
        prompt = " q="
        setPromptAttributes(ConsoleViewContentType.LOG_INFO_OUTPUT)
        Intrinsics.checkExpressionValueIsNotNull(consoleEditor, "consoleEditor")
        consoleEditor.isOneLineMode = true
    }

    init {
        Intrinsics.checkParameterIsNotNull(project, "project")
        val plainTextLanguage = PlainTextLanguage.INSTANCE
        Intrinsics.checkExpressionValueIsNotNull(plainTextLanguage, "PlainTextLanguage.INSTANCE")
        createUI()
        ConsoleHistoryController(MyConsoleRootType, "hybris.impex.shell", this).install()
    }

    override fun printDefaultText() {
        this.setInputText("*:*")
    }

    override fun collectStatistics() {
        StatsCollector.getInstance().collectStat(StatsCollector.ACTIONS.SOLR_CONSOLE)
    }

    object MyConsoleRootType : ConsoleRootType("hybris.solr.shell", null)

    override fun execute(query: String): HybrisHttpResult {

        Intrinsics.checkParameterIsNotNull(project, "query")
        if (this.coresComboBox.selectedItem != null) {
            val selectedCore = this.coresComboBox.selectedItem
            if (selectedCore == null) {
                throw TypeCastException("null cannot be cast to non-null type kotlin.String")
            } else {
                val selectedCoreString = selectedCore as String
                val spinnerValue = jSpinner.value
                if (spinnerValue == null) {
                    throw TypeCastException("null cannot be cast to non-null type kotlin.Int")
                } else {
                    val solrQueryObject = SolrQueryObject(query, selectedCoreString, spinnerValue as Int)
                    val client = SolrHttpClient.getInstance(project)
                    return client.executeSolrQuery(project, solrQueryObject)
                }
            }
        } else {
            val hybrisHttpResult = HybrisHttpResult.HybrisHttpResultBuilder.createResult().errorMessage("Cannot connect to a Solr server. Please, check your configuration").httpCode(500).build()
            Intrinsics.checkExpressionValueIsNotNull(hybrisHttpResult, "HybrisHttpResult.HybrisH…\n                .build()")
            return hybrisHttpResult
        }
    }
}

data class TimeOption(val name: String, val value: Int, val unit: TimeUnit)
data class CatalogVersionOption(val name: String, val value: String)