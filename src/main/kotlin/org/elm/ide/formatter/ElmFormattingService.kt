package org.elm.ide.formatter

import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.commandLineTools.ElmFormatCLI.ElmFormatResult
import org.elm.workspace.elmToolchain

@Suppress("UnstableApiUsage")
class ElmFormattingService : AsyncDocumentFormattingService() {

    private val VirtualFile.document: Document?
        get() = FileDocumentManager.getInstance().getDocument(this)

    override fun getFeatures(): Set<FormattingService.Feature> =
        setOf()

    override fun canFormat(file: PsiFile): Boolean {
        ElmFormatCLI.getElmVersion(file) ?: return false
        file.project.elmToolchain.elmFormatCLI ?: return false
        return true
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val context = request.context
        val project = context.project
        val file = context.virtualFile ?: return null
        val document = file.document ?: return null
        val elmVersion = ElmFormatCLI.getElmVersion(project, file) ?: return null
        val elmFormat = project.elmToolchain.elmFormatCLI ?: return null

        return object : FormattingTask {

            private val indicator: ProgressIndicatorBase = ProgressIndicatorBase()

            override fun run() {
                when (val result = elmFormat.format(document, elmVersion)) {
                    is ElmFormatResult.BadSyntax ->
                        request.onTextReady(result.msg)

                    is ElmFormatResult.FailedToStart ->
                        request.onTextReady(result.msg)

                    is ElmFormatResult.UnknownFailure ->
                        request.onTextReady(result.msg)

                    is ElmFormatResult.Success ->
                        request.onTextReady(result.formatted)
                }
            }

            override fun cancel(): Boolean {
                indicator.cancel()
                return true
            }
        }
    }

    override fun getNotificationGroupId(): String {
        return "Elm Plugin"
    }

    override fun getName(): String {
        return "elm-format"
    }
}
