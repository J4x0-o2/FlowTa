package com.j4x.flowta.core.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase

fun createDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(FlowTaDatabase.Schema, context, "flowta.db")
