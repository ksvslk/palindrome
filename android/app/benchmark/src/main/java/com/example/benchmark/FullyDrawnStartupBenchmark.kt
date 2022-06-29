package com.example.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FullyDrawnStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() {
        startup(CompilationMode.None())
    }

    @Test
    fun startupBaselineProfile() {
        startup(CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ))
    }

    fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = "com.indest.hah",
        metrics = listOf(StartupTimingMetric()),
        iterations = 15,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode
    ) {
        pressHome()
        startActivityAndWait()
    }
}