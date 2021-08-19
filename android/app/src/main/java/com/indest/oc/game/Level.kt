package com.indest.oc.game

import androidx.gridlayout.widget.GridLayout
import com.google.gson.annotations.Expose

data class Level(
    @Expose val puzzle:String,
    @Expose val category:String,
    var grid:GridLayout)
