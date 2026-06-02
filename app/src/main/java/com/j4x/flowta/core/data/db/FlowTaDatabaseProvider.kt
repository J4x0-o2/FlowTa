package com.j4x.flowta.core.data.db

import android.content.Context
import com.j4x.flowta.database.FlowTaDatabase

fun createDatabase(context: Context): FlowTaDatabase =
    FlowTaDatabase(createDriver(context))
