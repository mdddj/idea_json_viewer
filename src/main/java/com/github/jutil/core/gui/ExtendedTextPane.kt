package com.github.jutil.core.gui

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import javax.swing.text.BadLocationException

class ExtendedTextPane : RSyntaxTextArea() {
    /**
     * method to set caret position at start of the specified line
     *
     * @param line
     */
    @Throws(BadLocationException::class)
    fun setCaretLineNumber(line: Int) {
        val rootElement = document.defaultRootElement
        if (line < 1 || line > rootElement.elementCount) {
            throw BadLocationException("invalid line number", line)
        }
        val offset = rootElement.getElement(line - 1).startOffset
        caretPosition = offset
    }

    /**
     * Method to get line count in current textpane
     *
     */
    override fun getLineCount(): Int {
        return document.defaultRootElement.elementCount
    }

    companion object {
        private const val serialVersionUID = -2461865660916776135L
    }
}