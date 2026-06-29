package com.bridge.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bridge.calculator.core.elbow.ElbowCategory
import com.bridge.calculator.core.elbow.ElbowRegistry
import com.bridge.calculator.ui.screen.*
import com.bridge.calculator.ui.theme.BridgeCalculatorTheme

object NavRoutes {
    const val HOME = "home"
    const val CATEGORY_LIST = "category/{categoryName}"
    const val ELBOW_DETAIL = "elbow/{elbowId}"
    const val FORMULA = "formula"
    const val SETTINGS = "settings"
    fun categoryList(category: ElbowCategory) = "category/${category.name}"
    fun elbowDetail(elbowId: Int) = "elbow/$elbowId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BridgeCalculatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { BridgeCalculatorApp() }
            }
        }
    }
}

@Composable
fun BridgeCalculatorApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.HOME) { HomeScreen(onNavigateToCategory = { navController.navigate(NavRoutes.categoryList(it)) }, onNavigateToElbow = { navController.navigate(NavRoutes.elbowDetail(it.id)) }, onNavigateToFormula = { navController.navigate(NavRoutes.FORMULA) }, onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }) }
        composable(NavRoutes.CATEGORY_LIST) { backStackEntry -> val categoryName = backStackEntry.arguments?.getString("categoryName") ?: return@composable; val category = ElbowCategory.valueOf(categoryName); CategoryListScreen(category = category, onBack = { navController.popBackStack() }, onElbowClick = { navController.navigate(NavRoutes.elbowDetail(it.id)) }) }
        composable(NavRoutes.ELBOW_DETAIL) { backStackEntry -> val elbowId = backStackEntry.arguments?.getInt("elbowId") ?: return@composable; val elbow = ElbowRegistry.getById(elbowId) ?: return@composable; ElbowDetailScreen(elbowSpec = elbow, onBack = { navController.popBackStack() }) }
        composable(NavRoutes.FORMULA) { FormulaScreen(onBack = { navController.popBackStack() }) }
        composable(NavRoutes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
