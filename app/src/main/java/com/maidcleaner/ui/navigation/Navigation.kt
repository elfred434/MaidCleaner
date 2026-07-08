package com.maidcleaner.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.maidcleaner.ui.appcleaner.AppCleanerScreen
import com.maidcleaner.ui.appcontrol.AppControlScreen
import com.maidcleaner.ui.corpsefinder.CorpseFinderScreen
import com.maidcleaner.ui.dashboard.DashboardScreen
import com.maidcleaner.ui.duplicatefinder.DuplicateFinderScreen
import com.maidcleaner.ui.optimizer.OptimizerScreen
import com.maidcleaner.ui.scheduler.SchedulerScreen
import com.maidcleaner.ui.storageanalyzer.StorageAnalyzerScreen
import com.maidcleaner.ui.systemcleaner.SystemCleanerScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val CORPSE_FINDER = "corpse_finder"
    const val SYSTEM_CLEANER = "system_cleaner"
    const val APP_CLEANER = "app_cleaner"
    const val APP_CONTROL = "app_control"
    const val STORAGE_ANALYZER = "storage_analyzer"
    const val DUPLICATE_FINDER = "duplicate_finder"
    const val OPTIMIZER = "optimizer"
    const val SCHEDULER = "scheduler"
}

@Composable
fun MaidCleanerNavHost(
    navController: NavHostController
) {
    val animationDuration = 300

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        enterTransition = { fadeIn(animationSpec = tween(animationDuration)) },
        exitTransition = { fadeOut(animationSpec = tween(animationDuration)) }
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToCorpseFinder = { navController.navigate(Routes.CORPSE_FINDER) },
                onNavigateToSystemCleaner = { navController.navigate(Routes.SYSTEM_CLEANER) },
                onNavigateToAppCleaner = { navController.navigate(Routes.APP_CLEANER) },
                onNavigateToStorageAnalyzer = { navController.navigate(Routes.STORAGE_ANALYZER) },
                onNavigateToDuplicateFinder = { navController.navigate(Routes.DUPLICATE_FINDER) },
                onNavigateToAppControl = { navController.navigate(Routes.APP_CONTROL) },
                onNavigateToScheduler = { navController.navigate(Routes.SCHEDULER) },
                onNavigateToOptimizer = { navController.navigate(Routes.OPTIMIZER) }
            )
        }

        composable(
            Routes.CORPSE_FINDER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            CorpseFinderScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.SYSTEM_CLEANER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            SystemCleanerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.APP_CLEANER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            AppCleanerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.APP_CONTROL,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            AppControlScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.STORAGE_ANALYZER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            StorageAnalyzerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.DUPLICATE_FINDER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            DuplicateFinderScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.OPTIMIZER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            OptimizerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.SCHEDULER,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(animationDuration)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(animationDuration)
                )
            }
        ) {
            SchedulerScreen(onBack = { navController.popBackStack() })
        }
    }
}
