package com.kitty.ide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kitty.ide.ui.screen.EditorScreen
import com.kitty.ide.ui.screen.StartScreen

object KittyRoute {
    const val START  = "start"
    const val EDITOR = "editor"
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
                onNewProject  = { navController.navigate(KittyRoute.EDITOR) },
                onOpenProject = { navController.navigate(KittyRoute.EDITOR) }
            )
        }
        composable(KittyRoute.EDITOR) {
            EditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}