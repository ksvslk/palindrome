package com.example.benchmark

import android.os.Build
import androidx.annotation.RequiresApi
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
class LargeStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()


    @RequiresApi(Build.VERSION_CODES.N)
    @Test
    fun startupBaselineProfile() {
        startup(CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Test
    fun startupNoCompilation() {
        startup(CompilationMode.None())
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