package org.elm.ide.formatter

import com.intellij.util.ThrowableRunnable
import org.elm.workspace.ElmWorkspaceTestBase
import org.intellij.lang.annotations.Language

class ElmFormattingServiceTest : ElmWorkspaceTestBase() {

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        if (toolchain.elmFormatCLI == null) {
            // TODO in the future maybe we should install elm-format in the CI build environment
            System.err.println("SKIP $name: elm-format not found")
            return
        }
        super.runTestRunnable(testRunnable)
    }

    private val unformatted = """
                    module Main exposing (f)


                    f x = x

                """.trimIndent()

    private val expectedFormatted = """
                    module Main exposing (f)


                    f x =
                        x

                """.trimIndent()

    fun `test ElmFormatOnFileSaveComponent should work with elm 19`() {

        buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm", unformatted)
            }
        }

        testCorrectFormatting("src/Main.elm", expectedFormatted)
    }

    fun `test ElmFormatOnFileSaveComponent should not touch a file with the wrong ending like 'scala'`() {
        buildProject {
            project("elm.json", manifestElm19)
            dir("src") {
                elm("Main.elm")
                file("Main.scala", "blah")
            }
        }

        testCorrectFormatting("src/Main.scala", expected = "blah")
    }

    private fun testCorrectFormatting(fileWithCaret: String, expected: String) {
        myFixture.configureFromTempProjectFile(fileWithCaret)
        myFixture.performEditorAction("ReformatCode")
        myFixture.checkResult(expected)
    }
}


@Language("JSON")
private val manifestElm19 = """
        {
            "type": "application",
            "source-directories": [
                "src"
            ],
            "elm-version": "0.19.1",
            "dependencies": {
                "direct": {
                    "elm/core": "1.0.0",
                    "elm/json": "1.0.0"
                },
                "indirect": {}
            },
            "test-dependencies": {
                "direct": {},
                "indirect": {}
            }
        }
        """.trimIndent()
