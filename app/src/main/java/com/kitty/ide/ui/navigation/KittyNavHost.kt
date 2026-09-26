package com.kitty.ide.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kitty.ide.ui.screen.EditorScreen
import com.kitty.ide.ui.screen.StartScreen

object KittyRoute {
    const val START = "start"
    const val EDITOR = "editor/{projectUri}"

    fun editor(projectUri: String): String {
        // 对 URI 进行编码，避免斜杠等特殊字符导致导航解析失败
        return "editor/${Uri.encode(projectUri)}"
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
                onOpenProject = { projectUri ->
                    navController.navigate(KittyRoute.editor(projectUri))
                }
            )
        }
        composable(KittyRoute.EDITOR) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("projectUri") ?: ""
            val projectUri = Uri.decode(encodedUri)
            EditorScreen(
                projectUri = projectUri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}