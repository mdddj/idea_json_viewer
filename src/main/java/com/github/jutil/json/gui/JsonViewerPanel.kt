package com.github.jutil.json.gui

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.jutil.core.gui.AbstractPanel
import com.github.jutil.core.gui.ExtendedTextPane
import com.github.jutil.core.gui.GuiUtils
import com.github.jutil.gui.GuiConstants
import com.google.gson.JsonSyntaxException
import org.apache.commons.lang3.StringUtils
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.SearchContext
import org.fife.ui.rtextarea.SearchEngine
import org.slf4j.LoggerFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class JsonViewerPanel : AbstractPanel() {
    private val jsonIndicator = JLabel()
    private var validJsonIcon: Icon? = null
    private var invalidJsonIcon: Icon? = null
    private var textPane: ExtendedTextPane? = null
    private var searchField: JTextField? = null
    private val timer: Timer
    private val msgLabel = JLabel()
    private val msgPanel = JPanel()
    private var lastDirectory: File? = null

    init {
        init()
        timer = Timer(GuiConstants.DEFAULT_DELAY_MS) { e: ActionEvent? -> validateJson() }
        timer.isRepeats = false
    }

    private fun init() {
        size = maximumSize
        layout = BorderLayout()
        val settingPanel = JPanel()
        settingPanel.add(jsonIndicator)
        settingPanel.add(loadFileButton)
        settingPanel.add(formatButton)
        settingPanel.add(deformatButton)
        settingPanel.add(JLabel("Search"))
        settingPanel.add(getSearchField().also { searchField = it })
        settingPanel.add(msgLabel)
        msgPanel.add(msgLabel)
        val topPanel = JPanel(BorderLayout())
        topPanel.add(settingPanel, BorderLayout.NORTH)
        topPanel.add(msgPanel, BorderLayout.CENTER)
        add(topPanel, BorderLayout.NORTH)
        val scrollPane = GuiUtils.getScrollTextPane(SyntaxConstants.SYNTAX_STYLE_JSON)
        textPane = scrollPane.textArea as ExtendedTextPane
        textPane!!.isCodeFoldingEnabled = true
        textPane!!.highlightCurrentLine = true
        // textPane.setAutoIndentEnabled(true);
        // textPane.setHyperlinksEnabled(true);
        textPane!!.isBracketMatchingEnabled = true
        textPane!!.paintMatchedBracketPair = true
        GuiUtils.applyShortcut(textPane, KeyEvent.VK_L, "lineNumber", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                try {
                    textPane!!.caretLineNumber = JOptionPane.showInputDialog(
                        this@JsonViewerPanel, String.format("Enter Line Number (1, %d)", textPane!!.lineCount),
                        "Go to Line", JOptionPane.PLAIN_MESSAGE, null, null,
                        (textPane!!.caretLineNumber + 1).toString()
                    ) as Int
                } catch (ex: NumberFormatException) {
                    UIManager.getLookAndFeel().provideErrorFeedback(this@JsonViewerPanel)
                } catch (ex: Exception) {
                    UIManager.getLookAndFeel().provideErrorFeedback(this@JsonViewerPanel)
                    LOGGER.warn("excption while taking line number as input", ex)
                }
            }
        })
        textPane!!.document.addDocumentListener(object : DocumentListener {
            override fun removeUpdate(e: DocumentEvent) {
                timer.restart()
            }

            override fun insertUpdate(e: DocumentEvent) {
                timer.restart()
            }

            override fun changedUpdate(e: DocumentEvent) {}
        })
        add(scrollPane, BorderLayout.CENTER)
        validJsonIcon = ImageIcon(javaClass.getResource("/tick.png"))
        invalidJsonIcon = ImageIcon(javaClass.getResource("/error.png"))
    }

    private fun loadFile() {
        val fileChooser = JFileChooser(lastDirectory)
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDirectory = fileChooser.currentDirectory
            readFile(fileChooser.selectedFile)
        }
    }

    private fun validateJson() {
        val text = textPane!!.text
        if (StringUtils.isBlank(text)) {
            jsonIndicator.icon = null
            msgPanel.isVisible = false
            return
        }
        try {
            GuiUtils.validateJson(text)
            msgPanel.isVisible = false
            jsonIndicator.icon = validJsonIcon
        } catch (e: Exception) {
            jsonIndicator.icon = invalidJsonIcon
            popup(e)
        }
    }

    private fun toSimpleJson() {
        val text = textPane!!.text
        if (StringUtils.isBlank(text)) {
            return
        }
        try {
            textPane!!.text = GuiUtils.toSimpleJson(text)
            msgPanel.isVisible = false
        } catch (e: Exception) {
            popup(e)
        }
    }

    private fun toPrettyJson() {
        val text = textPane!!.text
        if (StringUtils.isBlank(text)) {
            return
        }
        try {
            textPane!!.text = GuiUtils.toPrettyJson(text)
            msgPanel.isVisible = false
        } catch (e: Exception) {
            popup(e)
        }
    }

    private fun readFile(selectedFile: File) {
        try {
            textPane!!.text = GuiUtils.readFile(selectedFile)
        } catch (e: Exception) {
            popup(e)
        }
    }

    private fun findInJson() {
        val findText = searchField!!.text
        val context = SearchContext(findText)
        if (!SearchEngine.find(textPane, context).wasFound()) {
            UIManager.getLookAndFeel().provideErrorFeedback(this@JsonViewerPanel)
        }
        RTextArea.setSelectedOccurrenceText(findText)
    }

    private val loadFileButton: JButton
        private get() {
            val loadFileButton = JButton("Open File")
            loadFileButton.toolTipText = "loads contents from file"
            GuiUtils.applyShortcut(loadFileButton, KeyEvent.VK_O, "Open", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    loadFile()
                }
            })
            loadFileButton.addActionListener { e: ActionEvent? -> loadFile() }
            return loadFileButton
        }
    private val formatButton: JButton
        private get() {
            val formatButton = JButton("Format")
            formatButton.toolTipText = "formats the input provided"
            GuiUtils.applyShortcut(formatButton, KeyEvent.VK_Q, "Format", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    toPrettyJson()
                }
            })
            formatButton.addActionListener { e: ActionEvent? -> toPrettyJson() }
            return formatButton
        }
    private val deformatButton: JButton
        get() {
            val deformatButton = JButton("DeFormat")
            deformatButton.toolTipText = "compresses json, should be used to send compressed data over networks"
            GuiUtils.applyShortcut(deformatButton, KeyEvent.VK_W, "Deformat", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    toSimpleJson()
                }
            })
            deformatButton.addActionListener { e: ActionEvent? -> toSimpleJson() }
            return deformatButton
        }

    private fun getSearchField(): JTextField {
        val searchField = JTextField(10)
        searchField.toolTipText = "CTRL+K (fwd) CTRL+SHIFT+K (bkd)"
        GuiUtils.applyShortcut(searchField, KeyEvent.VK_F, "Find", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                searchField.requestFocusInWindow()
            }
        })
        searchField.addActionListener { e: ActionEvent? -> findInJson() }
        return searchField
    }

    private fun popup(e: Exception) {
        LOGGER.warn("Exception occurred", e)
        val filteredMsg = StringBuilder()
        if (e is JsonProcessingException) {
            val jpe = e
            filteredMsg.append(jpe.originalMessage)
            val location = jpe.location
            val locationStr = String.format(" at line %d col %d", location.lineNr, location.columnNr)
            filteredMsg.append(locationStr)
        } else if (e is JsonSyntaxException) {
            val msg = StringUtils.substringAfter(e.message, "Exception: ")
            filteredMsg.append(msg)
        } else {
            filteredMsg.append(e.message)
        }
        msgLabel.text = filteredMsg.toString()
        msgLabel.foreground = Color.RED
        msgPanel.isVisible = true
    }

    companion object {
        private const val serialVersionUID = 7554118114747990205L
        private val LOGGER = LoggerFactory.getLogger(JsonViewerPanel::class.java)
        val instance = JsonViewerPanel()
    }
}