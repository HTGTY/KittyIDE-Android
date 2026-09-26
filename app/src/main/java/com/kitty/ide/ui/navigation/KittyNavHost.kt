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
    const val EDITOR = "editor/{projectPath}"

    /** 构建编辑器页面的导航路由，对路径进行 Uri 编码，避免斜杠等特殊字符导致导航解析失败 */
    fun editor(projectPath: String): String {
        return "editor/${Uri.encode(projectPath)}"
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}