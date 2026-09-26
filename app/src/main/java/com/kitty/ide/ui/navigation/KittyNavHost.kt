package com.kitty.ide.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kitty.ide.ui.screen.EditorScreen
import com.kitty.ide.ui.screen.PreviewScreen
import com.kitty.ide.ui.screen.StartScreen

object KittyRoute {
    const val START = "start"
    const val EDITOR = "editor/{projectPath}"
    const val PREVIEW = "preview/{htmlPath}"

    fun editor(projectPath: String): String {
        return "editor/${Uri.encode(projectPath)}"
    }

    fun preview(htmlPath: String): String {
        return "preview/${Uri.encode(htmlPath)}"
    }
}

@Composable
fun KittyNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = KittyRoute.START
    ) {
        composable(KittyRoute.START) {
            StartScreen(
                onOpenProject = { projectPath ->
                    navController.navigate(KittyRoute.editor(projectPath))
                }
            )
        }

        composable(KittyRoute.EDITOR) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = Uri.decode(encodedPath)
            EditorScreen(
                projectPath = projectPath,
                onBack = { navController.popBackStack() },
                onRun = { htmlPath ->
                    navController.navigate(KittyRoute.preview(htmlPath))
                }
            )
        }

        composable(KittyRoute.PREVIEW) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("htmlPath") ?: ""
            val htmlPath = Uri.decode(encodedPath)
            PreviewScreen(
                htmlPath = htmlPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}